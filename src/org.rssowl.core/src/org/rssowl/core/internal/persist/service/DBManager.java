/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
 **   http://www.rssowl.org/                                                 **
 **                                                                          **
 **   All rights reserved                                                    **
 **                                                                          **
 **   This program and the accompanying materials are made available under   **
 **   the terms of the Eclipse Public License v1.0 which accompanies this    **
 **   distribution, and is available at:                                     **
 **   http://www.rssowl.org/legal/epl-v10.html                               **
 **                                                                          **
 **   A copy is found in the file epl-v10.html and important notices to the  **
 **   license from the team is found in the textfile LICENSE.txt distributed **
 **   in this package.                                                       **
 **                                                                          **
 **   This copyright notice MUST APPEAR in all copies of the file!           **
 **                                                                          **
 **   Contributors:                                                          **
 **     RSSOwl Development Team - initial API and implementation             **
 **                                                                          **
 **  **********************************************************************  */

package org.rssowl.core.internal.persist.service;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.internal.persist.AbstractEntity;
import org.rssowl.core.internal.persist.BookMark;
import org.rssowl.core.internal.persist.ConditionalGet;
import org.rssowl.core.internal.persist.Description;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.Folder;
import org.rssowl.core.internal.persist.Label;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.NewsBin;
import org.rssowl.core.internal.persist.Preference;
import org.rssowl.core.internal.persist.SearchFilter;
import org.rssowl.core.internal.persist.migration.MigrationResult;
import org.rssowl.core.internal.persist.migration.Migrations;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.NewsCounterItem;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.DiskFullException;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.persist.service.InsufficientFilePermissionException;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.LongOperationMonitor;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.config.Configuration;
import com.db4o.config.ObjectClass;
import com.db4o.config.ObjectField;
import com.db4o.config.QueryEvaluationMode;
import com.db4o.ext.DatabaseFileLockedException;
import com.db4o.query.Query;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBManager {
  private static final int MAX_BACKUPS_COUNT = 2;
  private static final String FORMAT_FILE_NAME = "format2"; //$NON-NLS-1$
  private static DBManager fInstance;

  /* Backup Times */
  private static final int ONLINE_BACKUP_INITIAL = 1000 * 60 * 30; //30 Minutes
  private static final int ONLINE_BACKUP_INTERVAL = 1000 * 60 * 60 * 8; //8 Hours
  private static final int OFFLINE_BACKUP_INTERVAL = 1000 * 60 * 60 * 24 * 7; //7 Days

  /* Defrag Tasks Work Ticks */
  private static final int DEFRAG_TOTAL_WORK = 10000000; //100% (but don't fill to 100% to leave room for backup)
  private static final int DEFRAG_SUB_WORK_LABELS = 100000; //1%
  private static final int DEFRAG_SUB_WORK_FOLDERS = 500000; //5%
  private static final int DEFRAG_SUB_WORK_BINS = 1000000; //10%
  private static final int DEFRAG_SUB_WORK_FEEDS = 3000000; //30%
  private static final int DEFRAG_SUB_WORK_DESCRIPTIONS = 3000000; //30%
  private static final int DEFRAG_SUB_WORK_PREFERENCES = 100000; //1%
  private static final int DEFRAG_SUB_WORK_FILTERS = 100000; //1%
  private static final int DEFRAG_SUB_WORK_CONDITIONAL_GET = 100000; //1%
  private static final int DEFRAG_SUB_WORK_COUNTERS = 100000; //1%
  private static final int DEFRAG_SUB_WORK_EVENTS = 100000; //1%
  private static final int DEFRAG_SUB_WORK_COMMITT_DESTINATION = 300000; //3%
  private static final int DEFRAG_SUB_WORK_CLOSE_DESTINATION = 100000; //1%
  private static final int DEFRAG_SUB_WORK_CLOSE_SOURCE = 100000; //1%
  private static final int DEFRAG_SUB_WORK_FINISH = 100000; //1%

  private ObjectContainer fObjectContainer;
  private final ReadWriteLock fLock = new ReentrantReadWriteLock();
  private final List<DatabaseListener> fEntityStoreListeners = new CopyOnWriteArrayList<DatabaseListener>();
  private IStatus startupStatus;

  /**
   * @return The Singleton Instance.
   */
  public static DBManager getDefault() {
    if (fInstance == null)
      fInstance = new DBManager();

    return fInstance;
  }

  /**
   * Load and initialize the contributed DataBase.
   *
   * @param monitor
   * @throws PersistenceException In case of an error while initializing and
   * loading the contributed DataBase.
   */
  public void startup(LongOperationMonitor monitor) throws PersistenceException {
    EventManager.getInstance();
    createDatabase(monitor);
  }

  public void addEntityStoreListener(DatabaseListener listener) {
    if (listener instanceof EventManager)
      fEntityStoreListeners.add(0, listener);
    else if (listener instanceof DB4OIDGenerator) {
      if (!fEntityStoreListeners.isEmpty() && fEntityStoreListeners.get(0) instanceof EventManager)
        fEntityStoreListeners.add(1, listener);
      else
        fEntityStoreListeners.add(0, listener);
    } else
      fEntityStoreListeners.add(listener);
  }

  private void fireDatabaseEvent(DatabaseEvent event, boolean storeOpened) {
    for (DatabaseListener listener : fEntityStoreListeners) {
      if (storeOpened) {
        listener.databaseOpened(event);
      } else {
        listener.databaseClosed(event);
      }
    }
  }

  private void createEmptyObjectContainer(Configuration config, IStatus status) {
    Activator.getDefault().getLog().log(status); //Log in case there's also an exception creating an empty object container
    fObjectContainer = Db4o.openFile(config, getDBFilePath());
  }

  private IStatus createObjectContainer(Configuration config) {
    IStatus status = null;

    /* Open the DB */
    try {
      fObjectContainer = Db4o.openFile(config, getDBFilePath());
      status = Status.OK_STATUS;
    }

    /* Error opening the DB - try to recover */
    catch (Throwable e) {
      if (!(e instanceof OutOfMemoryError))
        Activator.safeLogError(e.getMessage(), e);

      if (e instanceof Error)
        throw (Error) e;

      File file = new File(getDBFilePath());
      if (!file.exists())
        throw new DiskFullException("Failed to create an empty database. This seems to indicate that the disk is full. Please free some space on the disk and restart RSSOwl.", e); //$NON-NLS-1$

      if (!file.canRead() || (!file.canWrite()))
        throw new InsufficientFilePermissionException("Current user has no permission to read and/or write file: " + file + ". Please make sure to start RSSOwl with sufficient permissions.", null); //$NON-NLS-1$ //$NON-NLS-2$

      BackupService backupService = createOnlineBackupService();
      if (backupService == null || e instanceof DatabaseFileLockedException)
        throw new PersistenceException(e);

      BackupService scheduledBackupService = createScheduledBackupService(null);
      File currentDbCorruptedFile = backupService.getCorruptedFile(null);
      DBHelper.rename(backupService.getFileToBackup(), currentDbCorruptedFile);

      /*
       * There was no online back-up file. This could only happen if the problem
       * happened on the first start-up or if the user never used the
       * application for more than 10 minutes.
       */
      if (backupService.getBackupFile(0) == null) {
        status = Activator.getDefault().createErrorStatus("Database file is corrupted and no back-up could be found. The corrupted file has been saved to: " + currentDbCorruptedFile.getAbsolutePath(), e); //$NON-NLS-1$
        createEmptyObjectContainer(config, status);
      } else {
        status = restoreFromBackup(config, e, currentDbCorruptedFile, backupService, scheduledBackupService);
      }
    }

    Assert.isNotNull(status, "status"); //$NON-NLS-1$
    final BackupService backupService = createOnlineBackupService();
    Job job = new Job("Back-up service") { //$NON-NLS-1$
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        if (!Owl.isShuttingDown()) {
          try {
            backupService.backup(true, monitor);
          } catch (PersistenceException e) {
            Activator.safeLogError(e.getMessage(), e);
          }
          schedule(getOnlineBackupDelay(false));
        }

        return Status.OK_STATUS;
      }
    };
    job.setSystem(true);
    job.schedule(getOnlineBackupDelay(true));
    return status;
  }

  private void checkDirPermissions() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    if (!dir.canRead() || (!dir.canWrite()))
      throw new InsufficientFilePermissionException("Current user has no permission to read from and/or write to directory: " + dir + ". Please make sure to start RSSOwl with sufficient permissions.", null); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private IStatus restoreFromBackup(Configuration config, Throwable startupException, File currentDbCorruptedFile, BackupService... backupServices) {
    Assert.isNotNull(backupServices, "backupServices"); //$NON-NLS-1$
    Assert.isLegal(backupServices.length > 0, "backupServices should have at least one element"); //$NON-NLS-1$
    Assert.isNotNull(backupServices[0].getBackupFile(0), "backupServices[0] should contain at least one back-up"); //$NON-NLS-1$
    long lastModified = -1;
    boolean foundSuitableBackup = false;
    for (BackupService backupService : backupServices) {
      for (int i = 0;; ++i) {
        File backupFile = backupService.getBackupFile(i);
        /* Always false in first iteration */
        if (backupFile == null)
          break;

        lastModified = backupFile.lastModified();

        DBHelper.rename(backupFile, backupService.getFileToBackup());
        try {
          fObjectContainer = Db4o.openFile(config, getDBFilePath());
          foundSuitableBackup = true;
          break;
        } catch (Throwable e1) {
          Activator.getDefault().logError("Back-up database corrupted: " + backupFile, e1); //$NON-NLS-1$
          DBHelper.rename(new File(getDBFilePath()), backupService.getCorruptedFile(i));
        }
      }
      if (foundSuitableBackup)
        break;
    }

    if (foundSuitableBackup) {
      String message = createRecoveredFromCorruptedDatabaseMessage(currentDbCorruptedFile, lastModified);
      return Activator.getDefault().createErrorStatus(message, startupException);
    }

    IStatus status = Activator.getDefault().createErrorStatus("Database file and its back-ups are all corrupted. The corrupted database file has been saved to: " + currentDbCorruptedFile.getAbsolutePath(), startupException); //$NON-NLS-1$
    createEmptyObjectContainer(config, status);
    return status;
  }

  private String createRecoveredFromCorruptedDatabaseMessage(File corruptedFile, long lastModified) {
    String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(lastModified));
    return "There was a problem opening the database file. RSSOwl has reverted to the last working back-up (from " + date + "). The corrupted file has been saved to: " + corruptedFile.getAbsolutePath(); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private boolean shouldReindex(MigrationResult migrationResult, IStatus startupStatus) {
    boolean shouldReindex = migrationResult.isReindex() || (!startupStatus.isOK());
    if (shouldReindex) {
      System.setProperty("rssowl.reindex", "true"); //$NON-NLS-1$ //$NON-NLS-2$ //Let others know by setting property
      return true;
    }

    return Boolean.getBoolean("rssowl.reindex"); //$NON-NLS-1$
  }

  private long getOnlineBackupDelay(boolean initial) {
    if (initial)
      return ONLINE_BACKUP_INITIAL;

    return getLongProperty("rssowl.onlinebackup.interval", ONLINE_BACKUP_INTERVAL); //$NON-NLS-1$
  }

  private long getLongProperty(String propertyName, long defaultValue) {
    String backupIntervalText = System.getProperty(propertyName);

    if (backupIntervalText != null) {
      try {
        long backupInterval = Long.parseLong(backupIntervalText);
        if (backupInterval > 0) {
          return backupInterval;
        }
      } catch (NumberFormatException e) {
        /* Let it fall through and use default */
      }
    }
    return defaultValue;
  }

  private BackupService createOnlineBackupService() {
    File file = new File(getDBFilePath());

    /* No database file exists, so no back-up can exist */
    if (!file.exists())
      return null;

    BackupService backupService = new BackupService(file, ".onlinebak", 2); //$NON-NLS-1$
    backupService.setBackupStrategy(new BackupService.BackupStrategy() {
      public void backup(File originFile, File destinationFile, IProgressMonitor monitor) {
        File marker = getOnlineBackupMarkerFile();
        try {

          /* Create Marker that Onlinebackup is Performed */
          if (!marker.exists())
            safeCreate(marker);

          /* Relies on fObjectContainer being set before calling backup */
          fObjectContainer.ext().backup(destinationFile.getAbsolutePath());
        } catch (IOException e) {
          throw new PersistenceException(e);
        } finally {
          safeDelete(marker);
        }
      }
    });
    return backupService;
  }

  private void safeCreate(File file) {
    try {
      file.createNewFile();
    } catch (Exception e) {
      /* Ignore */
    }
  }

  private void safeDelete(File file) {
    try {
      file.delete();
    } catch (Exception e) {
      /* Ignore */
    }
  }

  /**
   * @return the File indicating whether defragment should be run or not.
   */
  public File getDefragmentFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    return new File(dir, "defragment"); //$NON-NLS-1$
  }

  /**
   * @return the File indicating whether the online backup terminated normally
   * or not.
   */
  public File getOnlineBackupMarkerFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    return new File(dir, "onlinebakmarker"); //$NON-NLS-1$
  }

  /**
   * @return the File indicating whether the reindexing of news terminated
   * normally or not.
   */
  public File getReindexMarkerFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    return new File(dir, "reindexmarker"); //$NON-NLS-1$
  }

  /**
   * Internal method, exposed for tests only.
   *
   * @return the path to the db file.
   */
  public static final String getDBFilePath() {
    String filePath = Activator.getDefault().getStateLocation().toOSString() + File.separator + "rssowl.db"; //$NON-NLS-1$
    return filePath;
  }

  private File getDBFormatFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File formatFile = new File(dir, FORMAT_FILE_NAME);
    return formatFile;
  }

  public void removeEntityStoreListener(DatabaseListener listener) {
    fEntityStoreListeners.remove(listener);
  }

  public void createDatabase(LongOperationMonitor progressMonitor) throws PersistenceException {

    /* Assert File Permissions */
    checkDirPermissions();

    /* Create Configuration and check for Migration */
    Configuration config = createConfiguration(false);
    int workspaceVersion = getWorkspaceFormatVersion();
    MigrationResult migrationResult = new MigrationResult(false, false, false);

    SubMonitor subMonitor = null;
    try {

      /* Log previously failing Online Backup */
      try {
        if (getOnlineBackupMarkerFile().exists()) {
          Activator.safeLogInfo("Detected an Online Backup that did not complete"); //$NON-NLS-1$
          safeDelete(getOnlineBackupMarkerFile());
        }
      } catch (Exception e) {
        /* Ignore */
      }

      /* Log previously failing Reindexing */
      try {
        if (getReindexMarkerFile().exists()) {
          Activator.safeLogInfo("Detected a Search Re-Indexing that did not complete"); //$NON-NLS-1$
          safeDelete(getReindexMarkerFile());
        }
      } catch (Exception e) {
        /* Ignore */
      }

      /* Perform Migration if necessary */
      if (workspaceVersion != getCurrentFormatVersion()) {
        progressMonitor.beginLongOperation(false);
        subMonitor = SubMonitor.convert(progressMonitor, Messages.DBManager_RSSOWL_MIGRATION, 100);

        //TODO Have a better way to allocate the ticks to the child. We need
        //to be able to do it dynamically based on whether a reindex is required or not.
        migrationResult = migrate(workspaceVersion, getCurrentFormatVersion(), subMonitor.newChild(70));
      }

      /* Perform Defrag if necessary */
      if (!defragmentIfNecessary(progressMonitor, subMonitor)) {

        /* Defragment */
        if (migrationResult.isDefragmentDatabase())
          defragment(progressMonitor, subMonitor);

        /*
         * We only run the time-based back-up if a defragment has not taken
         * place because we always back-up during defragment.
         */
        else
          scheduledBackup(progressMonitor);
      }

      /* Open the DB */
      startupStatus = createObjectContainer(config);

      /* Notify Listeners that DB is opened */
      if (startupStatus.isOK())
        fireDatabaseEvent(new DatabaseEvent(fObjectContainer, fLock), true);

      /* Re-Index Search Index if necessary */
      boolean shouldReindex = shouldReindex(migrationResult, startupStatus);
      if (subMonitor == null && shouldReindex) {
        progressMonitor.beginLongOperation(false);
        subMonitor = SubMonitor.convert(progressMonitor, Messages.DBManager_PROGRESS_WAIT, 20);
      }

      IModelSearch modelSearch = InternalOwl.getDefault().getPersistenceService().getModelSearch();
      if (!progressMonitor.isCanceled() && (shouldReindex || migrationResult.isOptimizeIndex())) {
        modelSearch.startup();
        if (shouldReindex && !progressMonitor.isCanceled()) {
          Activator.safeLogInfo("Start: Search Re-Indexing"); //$NON-NLS-1$

          File marker = getReindexMarkerFile();
          try {

            /* Create Marker that Reindexing is Performed */
            if (!marker.exists())
              safeCreate(marker);

            /* Reindex Search Index */
            modelSearch.reindexAll(subMonitor != null ? subMonitor.newChild(20) : new NullProgressMonitor());
          } finally {
            safeDelete(marker);
          }

          if (progressMonitor.isCanceled())
            Activator.safeLogInfo("Cancelled: Search Re-Indexing"); //$NON-NLS-1$
          else
            Activator.safeLogInfo("Finished: Search Re-Indexing"); //$NON-NLS-1$
        }

        /* Optimize Index if Necessary */
        if (migrationResult.isOptimizeIndex() && !progressMonitor.isCanceled())
          modelSearch.optimize();
      }
    } finally {
      if (subMonitor != null) //If we perform the migration, the subMonitor is not null. Otherwise we don't show progress.
        progressMonitor.done();
    }
  }

  private BackupService createScheduledBackupService(Long backupFrequency) {
    return new BackupService(new File(getDBFilePath()), ".backup", MAX_BACKUPS_COUNT, getDBLastBackUpFile(), backupFrequency); //$NON-NLS-1$
  }

  private void scheduledBackup(IProgressMonitor monitor) {
    if (!new File(getDBFilePath()).exists())
      return;

    long sevenDays = getLongProperty("rssowl.offlinebackup.interval", OFFLINE_BACKUP_INTERVAL); //$NON-NLS-1$
    try {
      createScheduledBackupService(sevenDays).backup(false, monitor);
    } catch (PersistenceException e) {
      Activator.safeLogError(e.getMessage(), e);
    }
  }

  public File getDBLastBackUpFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File lastBackUpFile = new File(dir, "lastbackup"); //$NON-NLS-1$
    return lastBackUpFile;
  }

  private MigrationResult migrate(final int workspaceFormat, int currentFormat, IProgressMonitor progressMonitor) {
    Activator.safeLogInfo(NLS.bind("Migrating RSSOwl (from version {0} to version {1}", workspaceFormat, currentFormat)); //$NON-NLS-1$

    ConfigurationFactory configFactory = new ConfigurationFactory() {
      public Configuration createConfiguration() {
        return DBManager.createConfiguration(false);
      }
    };
    Migration migration = new Migrations().getMigration(workspaceFormat, currentFormat);
    if (migration == null) {
      throw new PersistenceException("It was not possible to migrate your data to the current version of RSSOwl. Migrations are supported between final versions and between consecutive milestones. In other words, 2.0M7 to 2.0M8 and 2.0 to 2.1 are supported but 2.0M6 to 2.0M8 is not supported. In the latter case, you would need to launch 2.0M7 and then 2.0M8 to be able to use that version. Migration was attempted from originFormat: " + workspaceFormat + " to destinationFormat: " + currentFormat); //$NON-NLS-1$ //$NON-NLS-2$
    }

    final File dbFile = new File(getDBFilePath());
    final String backupFileSuffix = ".mig."; //$NON-NLS-1$

    /*
     * Copy the db file to a permanent back-up where the file name includes the
     * workspaceFormat number. This will only be deleted after another
     * migration.
     */
    final BackupService backupService = new BackupService(dbFile, backupFileSuffix + workspaceFormat, 1);
    backupService.setLayoutStrategy(new BackupService.BackupLayoutStrategy() {
      public List<File> findBackupFiles() {
        List<File> backupFiles = new ArrayList<File>(3);
        for (int i = workspaceFormat; i >= 0; --i) {
          File file = new File(dbFile.getAbsoluteFile() + backupFileSuffix + i);
          if (file.exists())
            backupFiles.add(file);
        }
        return backupFiles;
      }

      public void rotateBackups(List<File> backupFiles) {
        throw new UnsupportedOperationException("No rotation supported because maxBackupCount is 1"); //$NON-NLS-1$
      }
    });
    backupService.backup(true, new NullProgressMonitor());

    /* Create a copy of the db file to use for the migration */
    File migDbFile = backupService.getTempBackupFile();
    DBHelper.copyFileNIO(dbFile, migDbFile);

    /* Migrate the copy */
    MigrationResult migrationResult = migration.migrate(configFactory, migDbFile.getAbsolutePath(), progressMonitor);

    File dbFormatFile = getDBFormatFile();
    File migFormatFile = new File(dbFormatFile.getAbsolutePath() + ".mig.temp"); //$NON-NLS-1$
    try {
      if (!migFormatFile.exists()) {
        migFormatFile.createNewFile();
      }
      if (!dbFormatFile.exists()) {
        dbFormatFile.createNewFile();
      }
    } catch (IOException ioe) {
      throw new PersistenceException("Failed to migrate data", ioe); //$NON-NLS-1$
    }
    setFormatVersion(migFormatFile);

    DBHelper.rename(migFormatFile, dbFormatFile);

    /* Finally, rename the actual db file */
    DBHelper.rename(migDbFile, dbFile);

    //TODO Remove this after M9
    if (getOldDBFormatFile().exists())
      getOldDBFormatFile().delete();

    return migrationResult;
  }

  private File getOldDBFormatFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File formatFile = new File(dir, "format"); //$NON-NLS-1$
    return formatFile;
  }

  private int getWorkspaceFormatVersion() {
    boolean dbFileExists = new File(getDBFilePath()).exists();
    File formatFile = getDBFormatFile();
    boolean formatFileExists = formatFile.exists();

    //TODO Remove this after M9 release and change the code to assume that if
    //no format2 file exists, then the version is lower than M8
    if (!formatFileExists && getOldDBFormatFile().exists()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(getOldDBFormatFile()));
        String text = reader.readLine();
        DBHelper.writeToFile(formatFile, text);
        formatFileExists = true;
      } catch (IOException e) {
        throw new PersistenceException(e);
      } finally {
        DBHelper.closeQuietly(reader);
      }
    }

    if (dbFileExists) {
      /* Assume that it's M5a if no format file exists, but a db file exists */
      if (!formatFileExists)
        return 0;

      String versionText = DBHelper.readFirstLineFromFile(formatFile);
      try {
        int version = Integer.parseInt(versionText);
        return version;
      } catch (NumberFormatException e) {
        throw new PersistenceException("Format file does not contain a number as the version", e); //$NON-NLS-1$
      }
    }
    /*
     * In case there is no database file, we just set the version as the current
     * version.
     */
    if (!formatFileExists) {
      try {
        formatFile.createNewFile();
      } catch (IOException ioe) {
        throw new PersistenceException("Error creating database", ioe); //$NON-NLS-1$
      }
    }
    setFormatVersion(formatFile);
    return getCurrentFormatVersion();
  }

  private void setFormatVersion(File formatFile) {
    DBHelper.writeToFile(formatFile, String.valueOf(getCurrentFormatVersion()));
  }

  private int getCurrentFormatVersion() {
    return 5;
  }

  private boolean defragmentIfNecessary(LongOperationMonitor progressMonitor, SubMonitor subMonitor) {
    File defragmentFile = getDefragmentFile();
    if (!defragmentFile.exists()) {
      return false;
    }
    if (!defragmentFile.delete()) {
      Activator.getDefault().logError("Failed to delete defragment file", null); //$NON-NLS-1$
    }
    defragment(progressMonitor, subMonitor);
    return true;
  }

  private void defragment(LongOperationMonitor progressMonitor, SubMonitor subMonitor) {
    SubMonitor monitor;
    if (subMonitor == null) {
      progressMonitor.beginLongOperation(true);
      String monitorText = Messages.DBManager_PROGRESS_WAIT;
      subMonitor = SubMonitor.convert(progressMonitor, monitorText, DEFRAG_TOTAL_WORK);
      monitor = subMonitor.newChild(DEFRAG_TOTAL_WORK);

      /*
       * This should not be needed, but things don't work properly when it's not
       * called.
       */
      monitor.beginTask(monitorText, DEFRAG_TOTAL_WORK);
    } else {
      monitor = subMonitor.newChild(10);
      monitor.setWorkRemaining(100);
    }

    Activator.safeLogInfo("Start: Database Defragmentation"); //$NON-NLS-1$

    BackupService backupService = createScheduledBackupService(null);
    File database = new File(getDBFilePath());
    File defragmentedDatabase = backupService.getTempBackupFile();

    /* User might have cancelled the operation */
    if (monitor.isCanceled()) {
      Activator.safeLogInfo("Cancelled: Database Defragmentation"); //$NON-NLS-1$
      return;
    }

    /* Defrag */
    monitor.subTask(Messages.DBManager_IMPROVING_APP_PERFORMANCE);
    copyDatabase(database, defragmentedDatabase, monitor);

    /* User might have cancelled the operation */
    if (monitor.isCanceled()) {
      Activator.safeLogInfo("Cancelled: Database Defragmentation"); //$NON-NLS-1$
      defragmentedDatabase.delete();
      return;
    }

    /* Backup */
    monitor.subTask(Messages.DBManager_CREATING_DB_BACKUP);
    backupService.backup(true, monitor);

    /* User might have cancelled the operation */
    if (monitor.isCanceled()) {
      Activator.safeLogInfo("Cancelled: Database Defragmentation"); //$NON-NLS-1$
      defragmentedDatabase.delete();
      return;
    }

    /* Rename Defragmented DB to real DB */
    DBHelper.rename(defragmentedDatabase, database);

    /* Finished */
    monitor.done();
    Activator.safeLogInfo("Finished: Database Defragmentation"); //$NON-NLS-1$
  }

  /**
   * Internal method. Made public for testing. Creates a copy of the database
   * that has all essential data structures. At the moment, this means not
   * copying NewsCounter and IConditionalGets since they will be re-populated
   * eventually.
   *
   * @param source
   * @param destination
   * @param monitor
   */
  public final static void copyDatabase(File source, File destination, IProgressMonitor monitor) {
    ObjectContainer sourceDb = Db4o.openFile(createConfiguration(true), source.getAbsolutePath());
    ObjectContainer destinationDb = Db4o.openFile(createConfiguration(true), destination.getAbsolutePath());

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    /* Labels (keep in memory to avoid duplicate copies when cascading feed) */
    List<Label> labels = new ArrayList<Label>();
    ObjectSet<Label> allLabels = sourceDb.query(Label.class);
    int available = DEFRAG_SUB_WORK_LABELS;
    if (!allLabels.isEmpty()) {
      int chunk = available / allLabels.size();
      for (Label label : allLabels) {
        if (isCanceled(monitor, sourceDb, destinationDb))
          return;

        labels.add(label);
        sourceDb.activate(label, Integer.MAX_VALUE);
        destinationDb.ext().set(label, Integer.MAX_VALUE);
        monitor.worked(chunk);
      }
      allLabels = null;
    } else
      monitor.worked(available);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    /* Folders */
    ObjectSet<Folder> allFolders = sourceDb.query(Folder.class);
    available = DEFRAG_SUB_WORK_FOLDERS;
    if (!allFolders.isEmpty()) {
      int chunk = available / allFolders.size();
      for (Folder type : allFolders) {
        if (isCanceled(monitor, sourceDb, destinationDb))
          return;

        sourceDb.activate(type, Integer.MAX_VALUE);
        if (type.getParent() == null)
          destinationDb.ext().set(type, Integer.MAX_VALUE);

        monitor.worked(chunk);
      }
      allFolders = null;
    } else
      monitor.worked(available);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    /*
     * We use destinationDb for the query here because we have already copied
     * the NewsBins at this stage and we may need to fix the NewsBin in case it
     * contains stale news refs.
     */
    ObjectSet<NewsBin> allBins = destinationDb.query(NewsBin.class);
    available = DEFRAG_SUB_WORK_BINS;
    if (!allBins.isEmpty()) {
      int chunk = available / allBins.size();
      for (NewsBin newsBin : allBins) {
        if (isCanceled(monitor, sourceDb, destinationDb))
          return;

        destinationDb.activate(newsBin, Integer.MAX_VALUE);
        List<NewsReference> staleNewsRefs = new ArrayList<NewsReference>(0);
        for (NewsReference newsRef : newsBin.getNewsRefs()) {
          if (isCanceled(monitor, sourceDb, destinationDb))
            return;

          Query query = sourceDb.query();
          query.constrain(News.class);
          query.descend("fId").constrain(newsRef.getId()); //$NON-NLS-1$
          Iterator<?> newsIt = query.execute().iterator();
          if (!newsIt.hasNext()) {
            Activator.getDefault().logError("NewsBin " + newsBin + " has reference to news with id: " + newsRef.getId() + ", but that news does not exist.", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            staleNewsRefs.add(newsRef);
            continue;
          }
          Object news = newsIt.next();
          sourceDb.activate(news, Integer.MAX_VALUE);
          destinationDb.ext().set(news, Integer.MAX_VALUE);
        }

        if (!staleNewsRefs.isEmpty()) {
          if (isCanceled(monitor, sourceDb, destinationDb))
            return;

          newsBin.removeNewsRefs(staleNewsRefs);
          destinationDb.ext().set(newsBin, Integer.MAX_VALUE);
        }

        monitor.worked(chunk);
      }
      allBins = null;
    } else
      monitor.worked(available);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    /* Feeds */
    available = DEFRAG_SUB_WORK_FEEDS;
    int feedCounter = 0;
    NewsCounter newsCounter = new NewsCounter();
    ObjectSet<Feed> allFeeds = sourceDb.query(Feed.class);
    if (!allFeeds.isEmpty()) {
      int allFeedsSize = allFeeds.size();
      int chunk = available / allFeedsSize;

      int i = 1;
      for (Feed feed : allFeeds) {
        if (isCanceled(monitor, sourceDb, destinationDb))
          return;

        /* Introduce own label as feed copying can be very time consuming */
        monitor.subTask(NLS.bind(Messages.DBManager_OPTIMIZING_NEWSFEEDS, i, allFeedsSize));
        i++;

        sourceDb.activate(feed, Integer.MAX_VALUE);
        addNewsCounterItem(newsCounter, feed);
        destinationDb.ext().set(feed, Integer.MAX_VALUE);

        ++feedCounter;
        if (feedCounter % 40 == 0) {
          destinationDb.commit();
          System.gc();
        }

        monitor.worked(chunk);
      }
      allFeeds = null;
      destinationDb.commit();
      System.gc();
    } else
      monitor.worked(available);

    /* Back to normal subtask label */
    monitor.subTask(Messages.DBManager_IMPROVING_APP_PERFORMANCE);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    destinationDb.ext().set(newsCounter, Integer.MAX_VALUE);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    /* Description */
    available = DEFRAG_SUB_WORK_DESCRIPTIONS;
    int descriptionCounter = 0;
    ObjectSet<Description> allDescriptions = sourceDb.query(Description.class);
    if (!allDescriptions.isEmpty()) {
      int chunk = Math.max(1, available / allDescriptions.size());

      for (Description description : allDescriptions) {
        if (isCanceled(monitor, sourceDb, destinationDb))
          return;

        sourceDb.activate(description, Integer.MAX_VALUE);
        destinationDb.ext().set(description, Integer.MAX_VALUE);

        ++descriptionCounter;
        if (descriptionCounter % 600 == 0) {
          destinationDb.commit();
          System.gc();
        }

        monitor.worked(chunk);
      }

      allDescriptions = null;
      destinationDb.commit();
      System.gc();
    } else
      monitor.worked(available);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    /* Preferences */
    available = DEFRAG_SUB_WORK_PREFERENCES;
    ObjectSet<Preference> allPreferences = sourceDb.query(Preference.class);
    if (!allPreferences.isEmpty()) {
      int chunk = available / allPreferences.size();

      for (Preference pref : allPreferences) {
        if (isCanceled(monitor, sourceDb, destinationDb))
          return;

        sourceDb.activate(pref, Integer.MAX_VALUE);
        destinationDb.ext().set(pref, Integer.MAX_VALUE);

        monitor.worked(chunk);
      }

      allPreferences = null;
    } else
      monitor.worked(available);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    /* Filter */
    available = DEFRAG_SUB_WORK_FILTERS;
    ObjectSet<SearchFilter> allFilters = sourceDb.query(SearchFilter.class);
    if (!allFilters.isEmpty()) {
      int chunk = available / allFilters.size();

      for (ISearchFilter filter : allFilters) {
        if (isCanceled(monitor, sourceDb, destinationDb))
          return;

        sourceDb.activate(filter, Integer.MAX_VALUE);
        destinationDb.ext().set(filter, Integer.MAX_VALUE);
        monitor.worked(chunk);
      }

      allFilters = null;
    } else
      monitor.worked(available);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    /* Counter */
    List<Counter> counterSet = sourceDb.query(Counter.class);
    Counter counter = counterSet.iterator().next();
    sourceDb.activate(counter, Integer.MAX_VALUE);
    destinationDb.ext().set(counter, Integer.MAX_VALUE);

    monitor.worked(DEFRAG_SUB_WORK_COUNTERS);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    /* Entity Id By Event Type */
    EntityIdsByEventType entityIdsByEventType = sourceDb.query(EntityIdsByEventType.class).iterator().next();
    sourceDb.activate(entityIdsByEventType, Integer.MAX_VALUE);
    destinationDb.ext().set(entityIdsByEventType, Integer.MAX_VALUE);

    monitor.worked(DEFRAG_SUB_WORK_EVENTS);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    /* Conditional Get */
    available = DEFRAG_SUB_WORK_CONDITIONAL_GET;
    ObjectSet<ConditionalGet> allConditionalGets = sourceDb.query(ConditionalGet.class);
    if (!allConditionalGets.isEmpty()) {
      int chunk = available / allConditionalGets.size();
      for (ConditionalGet conditionalGet : allConditionalGets) {
        if (isCanceled(monitor, sourceDb, destinationDb))
          return;

        sourceDb.activate(conditionalGet, Integer.MAX_VALUE);
        destinationDb.ext().set(conditionalGet, Integer.MAX_VALUE);
        monitor.worked(chunk);
      }
      allConditionalGets = null;
    } else
      monitor.worked(available);

    /* User might have cancelled the operation */
    if (isCanceled(monitor, sourceDb, destinationDb))
      return;

    sourceDb.close();
    monitor.worked(DEFRAG_SUB_WORK_CLOSE_SOURCE);

    /* User might have cancelled the operation */
    if (monitor.isCanceled()) {
      destinationDb.close();
      return;
    }

    destinationDb.commit();
    monitor.worked(DEFRAG_SUB_WORK_COMMITT_DESTINATION);

    /* User might have cancelled the operation */
    if (monitor.isCanceled()) {
      destinationDb.close();
      return;
    }

    destinationDb.close();
    monitor.worked(DEFRAG_SUB_WORK_CLOSE_DESTINATION);

    /* User might have cancelled the operation */
    if (monitor.isCanceled())
      return;

    System.gc();
    monitor.worked(DEFRAG_SUB_WORK_FINISH);
  }

  private static boolean isCanceled(IProgressMonitor monitor, ObjectContainer source, ObjectContainer dest) {
    if (monitor.isCanceled()) {
      source.close();
      dest.close();
      return true;
    }

    return false;
  }

  private static void addNewsCounterItem(NewsCounter newsCounter, Feed feed) {
    Map<State, Integer> stateToCountMap = feed.getNewsCount();
    int unreadCount = getCount(stateToCountMap, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));
    Integer newCount = stateToCountMap.get(INews.State.NEW);
    newsCounter.put(feed.getLink().toString(), new NewsCounterItem(newCount, unreadCount, feed.getStickyCount()));
  }

  private static int getCount(Map<State, Integer> stateToCountMap, Set<State> states) {
    int count = 0;
    for (State state : states) {
      count += stateToCountMap.get(state);
    }
    return count;
  }

  /**
   * Internal method, exposed for tests only.
   *
   * @param forDefrag if <code>true</code> the configuration will be improved
   * for the defrag process and <code>false</code> otherwise to return a normal
   * configuration suitable for the application.
   * @return Configuration
   */
  public static final Configuration createConfiguration(boolean forDefrag) {
    Configuration config = Db4o.newConfiguration();
    //TODO We can use dbExists to configure our parameters for a more
    //efficient startup. For example, the following could be used. We'd have
    //to include a file when we need to evolve the schema or something similar
    //config.detectSchemaChanges(false)

    //    config.blockSize(8);
    //    config.bTreeCacheHeight(0);
    //    config.bTreeNodeSize(100);
    //    config.diagnostic().addListener(new DiagnosticListener() {
    //      public void onDiagnostic(Diagnostic d) {
    //        System.out.println(d);
    //      }
    //    });

    config.setOut(new PrintStream(new ByteArrayOutputStream()) {
      @Override
      public void write(byte[] buf, int off, int len) {
        if (buf != null && len >= 0 && off >= 0 && off <= buf.length - len)
          CoreUtils.appendLogMessage(new String(buf, off, len));
      }
    });

    config.lockDatabaseFile(false);
    config.queries().evaluationMode(forDefrag ? QueryEvaluationMode.LAZY : QueryEvaluationMode.IMMEDIATE);
    config.automaticShutDown(false);
    config.callbacks(false);
    config.activationDepth(2);
    config.flushFileBuffers(false);
    config.callConstructors(true);
    config.exceptionsOnNotStorable(true);
    configureAbstractEntity(config);
    config.objectClass(BookMark.class).objectField("fFeedLink").indexed(true); //$NON-NLS-1$
    config.objectClass(ConditionalGet.class).objectField("fLink").indexed(true); //$NON-NLS-1$
    configureFeed(config);
    configureNews(config);
    configureFolder(config);
    config.objectClass(Description.class).objectField("fNewsId").indexed(true); //$NON-NLS-1$
    config.objectClass(NewsCounter.class).cascadeOnDelete(true);
    config.objectClass(Preference.class).cascadeOnDelete(true);
    config.objectClass(Preference.class).objectField("fKey").indexed(true); //$NON-NLS-1$
    config.objectClass(SearchFilter.class).objectField("fActions").cascadeOnDelete(true); //$NON-NLS-1$

    if (isIBM_VM_1_6()) //See defect 733
      config.objectClass("java.util.MiniEnumSet").translate(new com.db4o.config.TSerializable()); //$NON-NLS-1$

    return config;
  }

  private static boolean isIBM_VM_1_6() {
    String javaVendor = System.getProperty("java.vendor"); //$NON-NLS-1$
    String javaVersion = System.getProperty("java.version"); //$NON-NLS-1$
    return javaVendor != null && javaVendor.contains("IBM") && javaVersion != null && javaVersion.contains("1.6"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private static void configureAbstractEntity(Configuration config) {
    ObjectClass abstractEntityClass = config.objectClass(AbstractEntity.class);
    ObjectField idField = abstractEntityClass.objectField("fId"); //$NON-NLS-1$
    idField.indexed(true);
    idField.cascadeOnActivate(true);
    abstractEntityClass.objectField("fProperties").cascadeOnUpdate(true); //$NON-NLS-1$
  }

  private static void configureFolder(Configuration config) {
    ObjectClass oc = config.objectClass(Folder.class);
    oc.objectField("fChildren").cascadeOnUpdate(true); //$NON-NLS-1$
  }

  private static void configureNews(Configuration config) {
    ObjectClass oc = config.objectClass(News.class);

    /* Indexes */
    oc.objectField("fParentId").indexed(true); //$NON-NLS-1$
    oc.objectField("fFeedLink").indexed(true); //$NON-NLS-1$
    oc.objectField("fStateOrdinal").indexed(true); //$NON-NLS-1$
  }

  private static void configureFeed(Configuration config) {
    ObjectClass oc = config.objectClass(Feed.class);

    ObjectField linkText = oc.objectField("fLinkText"); //$NON-NLS-1$
    linkText.indexed(true);
    linkText.cascadeOnActivate(true);

    oc.objectField("fTitle").cascadeOnActivate(true); //$NON-NLS-1$
  }

  /**
   * Shutdown the contributed Database.
   *
   * @throws PersistenceException In case of an error while shutting down the
   * contributed DataBase.
   */
  public void shutdown() throws PersistenceException {
    fLock.writeLock().lock();
    try {
      fireDatabaseEvent(new DatabaseEvent(fObjectContainer, fLock), false);
      if (fObjectContainer != null)
        while (!fObjectContainer.close());
    } finally {
      fLock.writeLock().unlock();
    }
  }

  public void dropDatabase() throws PersistenceException {
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        shutdown();
        if (!new File(getDBFilePath()).delete()) {
          Activator.getDefault().logError("Failed to delete db file", null); //$NON-NLS-1$
        }
        if (!getDBFormatFile().delete()) {
          Activator.getDefault().logError("Failed to delete db format file", null); //$NON-NLS-1$
        }
      }
    });
  }

  public final ObjectContainer getObjectContainer() {
    return fObjectContainer;
  }

  public IStatus getStartupStatus() {
    return startupStatus;
  }
}
