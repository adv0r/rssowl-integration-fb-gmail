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

package org.rssowl.ui.internal;

import org.eclipse.core.commands.Command;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.rssowl.core.IApplicationService;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.MonitorCanceledException;
import org.rssowl.core.connection.NotModifiedException;
import org.rssowl.core.connection.UnknownProtocolException;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.interpreter.ParserException;
import org.rssowl.core.interpreter.ITypeExporter.Options;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.dao.IConditionalGetDAO;
import org.rssowl.core.persist.dao.ILabelDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.dao.IPreferenceDAO;
import org.rssowl.core.persist.dao.ISearchMarkDAO;
import org.rssowl.core.persist.event.BookMarkAdapter;
import org.rssowl.core.persist.event.BookMarkEvent;
import org.rssowl.core.persist.event.LabelAdapter;
import org.rssowl.core.persist.event.LabelEvent;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.ExtensionUtils;
import org.rssowl.core.util.ITask;
import org.rssowl.core.util.JobQueue;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.TaskAdapter;
import org.rssowl.core.util.Triple;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.actions.FindUpdatesAction;
import org.rssowl.ui.internal.dialogs.LoginDialog;
import org.rssowl.ui.internal.dialogs.properties.EntityPropertyPageWrapper;
import org.rssowl.ui.internal.dialogs.welcome.TutorialWizard;
import org.rssowl.ui.internal.dialogs.welcome.WelcomeWizard;
import org.rssowl.ui.internal.handler.LabelNewsHandler;
import org.rssowl.ui.internal.notifier.NotificationService;
import org.rssowl.ui.internal.services.CleanUpReminderService;
import org.rssowl.ui.internal.services.ContextService;
import org.rssowl.ui.internal.services.DownloadService;
import org.rssowl.ui.internal.services.FeedReloadService;
import org.rssowl.ui.internal.services.SavedSearchService;
import org.rssowl.ui.internal.util.ImportUtils;
import org.rssowl.ui.internal.util.JobRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * Note: The Controller should not be responsible for handling Exceptions, e.g.
 * showing a Dialog when Authentication is required. Its only responsible for
 * calling the appropiate methods on a complex operation like loading a Feed.
 * </p>
 * <p>
 * Note: As required by the UI, the controller should be filled with more
 * methods.
 * </p>
 *
 * @author bpasero
 */
public class Controller {

  /* Backup Files */
  private static final String DAILY_BACKUP = "backup.opml"; //$NON-NLS-1$
  private static final String BACKUP_TMP = "backup.tmp"; //$NON-NLS-1$
  private static final String WEEKLY_BACKUP = "backup_weekly.opml"; //$NON-NLS-1$

  /* Extension-Points */
  private static final String ENTITY_PROPERTY_PAGE_EXTENSION_POINT = "org.rssowl.ui.EntityPropertyPage"; //$NON-NLS-1$

  /** Property to store info about a Realm in a Bookmark */
  public static final String BM_REALM_PROPERTY = "org.rssowl.ui.BMRealmProperty"; //$NON-NLS-1$

  /** Prefix for dynamic Label Actions */
  public static final String LABEL_ACTION_PREFIX = "org.rssowl.ui.LabelAction"; //$NON-NLS-1$

  /** Key to store error messages into entities during reload */
  public static final String LOAD_ERROR_KEY = "org.rssowl.ui.internal.LoadErrorKey"; //$NON-NLS-1$

  /* ID of RSSOwl's Keybinding Category */
  private static final String RSSOWL_KEYBINDING_CATEGORY = "org.rssowl.ui.commands.category.RSSOwl"; //$NON-NLS-1$

  /* The Singleton Instance */
  private static Controller fInstance;

  /* Token to ask the DB if this is the first start of RSSOwl */
  private static final String FIRST_START_TOKEN = "org.rssowl.ui.FirstStartToken"; //$NON-NLS-1$

  /* Default Max. number of concurrent running reload Jobs */
  private static final int DEFAULT_MAX_CONCURRENT_RELOAD_JOBS = 10;

  /* System Property to override default Max. number of concurrent running reload Jobs */
  private static final String MAX_CONCURRENT_RELOAD_JOBS_PROPERTY = "maxReloadJobs"; //$NON-NLS-1$

  /* Max. number of concurrent Jobs for saving a Feed */
  private static final int MAX_CONCURRENT_SAVE_JOBS = 1;

  /* Max number of jobs in the queue used for saving feeds before it blocks */
  private static final int MAX_SAVE_QUEUE_SIZE = 1;

  /* Connection Timeouts in MS */
  private static final int DEFAULT_FEED_CON_TIMEOUT = 30000;

  /* System Property to override default connection timeout */
  private static final String FEED_CON_TIMEOUT_PROPERTY = "conTimeout"; //$NON-NLS-1$

  /* System Property to import a file on first startup */
  private static final String IMPORT_PROPERTY = "import"; //$NON-NLS-1$

  /* System Property indicating that the application is running portable */
  private static final String PORTABLE_PROPERTY = "portable"; //$NON-NLS-1$

  /* System Property indicating that the application should not support update */
  private static final String DISABLE_UPDATE_PROPERTY = "disableUpdate"; //$NON-NLS-1$

  /* Queue for reloading Feeds */
  private final JobQueue fReloadFeedQueue;

  /* Queue for saving Feeds */
  private final JobQueue fSaveFeedQueue;

  /* Notification Service */
  private NotificationService fNotificationService;

  /* Saved Search Service */
  private SavedSearchService fSavedSearchService;

  /* Download Service */
  private DownloadService fDownloadService;

  /* Feed-Reload Service */
  private FeedReloadService fFeedReloadService;

  /* Contributed Entity-Property-Pages */
  final List<EntityPropertyPageWrapper> fEntityPropertyPages;

  /* Flag is set to TRUE when the application is done starting */
  private boolean fIsStarted;

  /* Flag is set to TRUE when shutting down the application */
  private boolean fShuttingDown;

  /* Flag is set to TRUE when restarting the application */
  private boolean fRestarting;

  /* Service to manage Contexts */
  private ContextService fContextService;

  /* Share News Provider Extension Point */
  private static final String SHARE_PROVIDER_EXTENSION_POINT = "org.rssowl.ui.ShareProvider"; //$NON-NLS-1$

  /* Feed Search Extension Point */
  private static final String FEED_SEARCH_EXTENSION_POINT = "org.rssowl.ui.FeedSearch"; //$NON-NLS-1$

  /* Token to replace keywords with in the Feed Search Link */
  private static final String FEED_SEARCH_KEYWORD_TOKEN = "[K]"; //$NON-NLS-1$

  /* Misc. */
  private final IApplicationService fAppService;
  private CleanUpReminderService fCleanUpReminderService;
  private final IBookMarkDAO fBookMarkDAO;
  private final ISearchMarkDAO fSearchMarkDAO;
  private final IConditionalGetDAO fConditionalGetDAO;
  private final IPreferenceDAO fPrefsDAO;
  private final ILabelDAO fLabelDao;
  private final IModelFactory fFactory;
  private final Lock fLoginDialogLock = new ReentrantLock();
  private BookMarkAdapter fBookMarkListener;
  private LabelAdapter fLabelListener;
  private ListenerList fBookMarkLoadListeners = new ListenerList();
  private final int fConnectionTimeout;
  private List<ShareProvider> fShareProviders = new ArrayList<ShareProvider>();
  private Map<Long, Long> fDeletedBookmarksCache = new ConcurrentHashMap<Long, Long>();
  private String fFeedSearchUrl;
  private boolean fShowWelcome;
  private boolean fPortable;
  private boolean fDisableUpdate;
  private String fNl;

  /**
   * A listener that informs when a {@link IBookMark} is getting reloaded from
   * the {@link Controller}.
   */
  public static interface BookMarkLoadListener {

    /**
     * @param bookmark the {@link IBookMark} that is about to load.
     */
    void bookMarkAboutToLoad(IBookMark bookmark);

    /**
     * @param bookmark the {@link IBookMark} that is done loading.
     */
    void bookMarkDoneLoading(IBookMark bookmark);
  }

  /* Task to perform Reload-Operations */
  private class ReloadTask implements ITask {
    private final Long fId;
    private final IBookMark fBookMark;
    private final Shell fShell;
    private final Priority fPriority;

    ReloadTask(IBookMark bookmark, Shell shell, ITask.Priority priority) {
      Assert.isNotNull(bookmark);
      Assert.isNotNull(bookmark.getId());

      fBookMark = bookmark;
      fId = bookmark.getId();
      fShell = shell;
      fPriority = priority;
    }

    public IStatus run(IProgressMonitor monitor) {
      IStatus status = reload(fBookMark, fShell, monitor);
      return status;
    }

    public String getName() {
      return fBookMark.getName();
    }

    public Priority getPriority() {
      return fPriority;
    }

    @Override
    public int hashCode() {
      return fId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;

      if (obj == null)
        return false;

      if (getClass() != obj.getClass())
        return false;

      final ReloadTask other = (ReloadTask) obj;
      return fId.equals(other.fId);
    }
  }

  private Controller() {
    int maxConcurrentReloadJobs = getSystemProperty(MAX_CONCURRENT_RELOAD_JOBS_PROPERTY, 0, DEFAULT_MAX_CONCURRENT_RELOAD_JOBS);
    fReloadFeedQueue = new JobQueue(Messages.Controller_UPDATING_FEEDS, Messages.Controller_UPDATING, maxConcurrentReloadJobs, Integer.MAX_VALUE, true, 0);
    fSaveFeedQueue = new JobQueue(Messages.Controller_UPDATING_FEEDS, MAX_CONCURRENT_SAVE_JOBS, MAX_SAVE_QUEUE_SIZE, false, 0);
    fSaveFeedQueue.setUnknownProgress(true);
    fEntityPropertyPages = loadEntityPropertyPages();
    fBookMarkDAO = DynamicDAO.getDAO(IBookMarkDAO.class);
    fSearchMarkDAO = DynamicDAO.getDAO(ISearchMarkDAO.class);
    fConditionalGetDAO = DynamicDAO.getDAO(IConditionalGetDAO.class);
    fLabelDao = DynamicDAO.getDAO(ILabelDAO.class);
    fPrefsDAO = Owl.getPersistenceService().getDAOService().getPreferencesDAO();
    fAppService = Owl.getApplicationService();
    fFactory = Owl.getModelFactory();
    fConnectionTimeout = getSystemProperty(FEED_CON_TIMEOUT_PROPERTY, DEFAULT_FEED_CON_TIMEOUT, DEFAULT_FEED_CON_TIMEOUT);
    fPortable = System.getProperty(PORTABLE_PROPERTY) != null;
    fDisableUpdate = Boolean.getBoolean(DISABLE_UPDATE_PROPERTY);
    fNl = System.getProperty("line.separator"); //$NON-NLS-1$
    if (!StringUtils.isSet(fNl))
      fNl = "\n"; //$NON-NLS-1$
  }

  private int getSystemProperty(String key, int minValue, int defaultValue) {
    String strVal = System.getProperty(key);
    if (strVal != null) {
      int intVal = 0;
      try {
        intVal = Integer.parseInt(strVal);
      } catch (NumberFormatException e) {
        Activator.getDefault().logError(e.getMessage(), e);
        return defaultValue;
      }

      if (intVal > minValue)
        return intVal;
    }

    return defaultValue;
  }

  private void registerListeners() {

    /* Delete Favicon when Bookmark gets deleted and remember ID */
    fBookMarkListener = new BookMarkAdapter() {
      @Override
      public void entitiesDeleted(Set<BookMarkEvent> events) {
        if (!fShuttingDown) {
          for (BookMarkEvent event : events) {
            Long id = event.getEntity().getId();
            OwlUI.deleteImage(id);
            if (id != null)
              fDeletedBookmarksCache.put(id, id);
          }
        }
      }
    };

    DynamicDAO.addEntityListener(IBookMark.class, fBookMarkListener);

    /* Update Label conditions when Label name changes */
    fLabelListener = new LabelAdapter() {

      @Override
      public void entitiesAdded(Set<LabelEvent> events) {
        if (!fShuttingDown)
          updateLabelCommands();
      }

      @Override
      public void entitiesUpdated(Set<LabelEvent> events) {
        if (!fShuttingDown) {
          updateLabelCommands();

          for (LabelEvent event : events) {
            ILabel oldLabel = event.getOldLabel();
            ILabel updatedLabel = event.getEntity();
            if (!oldLabel.getName().equals(updatedLabel.getName())) {
              updateLabelConditions(oldLabel.getName(), updatedLabel.getName());
            }
          }
        }
      }

      @Override
      public void entitiesDeleted(Set<LabelEvent> events) {
        if (!fShuttingDown)
          updateLabelCommands();
      }
    };

    DynamicDAO.addEntityListener(ILabel.class, fLabelListener);
  }

  private void updateLabelConditions(String oldLabelName, String newLabelName) {
    Set<ISearchMark> searchMarksToUpdate = new HashSet<ISearchMark>(1);

    for (ISearchMark searchMark : fSearchMarkDAO.loadAll()) {
      List<ISearchCondition> conditions = searchMark.getSearchConditions();
      for (ISearchCondition condition : conditions) {
        if (condition.getField().getId() == INews.LABEL && condition.getValue().equals(oldLabelName)) {
          condition.setValue(newLabelName);
          searchMarksToUpdate.add(searchMark);
        }
      }
    }

    DynamicDAO.getDAO(ISearchMarkDAO.class).saveAll(searchMarksToUpdate);
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(IBookMark.class, fBookMarkListener);
    DynamicDAO.removeEntityListener(ILabel.class, fLabelListener);
  }

  private List<EntityPropertyPageWrapper> loadEntityPropertyPages() {
    List<EntityPropertyPageWrapper> pages = new ArrayList<EntityPropertyPageWrapper>();

    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(ENTITY_PROPERTY_PAGE_EXTENSION_POINT);

    /* For each contributed property Page */
    for (IConfigurationElement element : elements) {
      try {
        String id = element.getAttribute("id"); //$NON-NLS-1$
        String name = element.getAttribute("name"); //$NON-NLS-1$
        int order = Integer.valueOf(element.getAttribute("order")); //$NON-NLS-1$
        boolean handlesMultipleEntities = Boolean.valueOf(element.getAttribute("handlesMultipleEntities")); //$NON-NLS-1$

        List<Class<?>> targetEntities = new ArrayList<Class<?>>();
        IConfigurationElement[] entityTargets = element.getChildren("targetEntity"); //$NON-NLS-1$
        for (IConfigurationElement entityTarget : entityTargets)
          targetEntities.add(Class.forName(entityTarget.getAttribute("class"))); //$NON-NLS-1$

        pages.add(new EntityPropertyPageWrapper(id, element, targetEntities, name, order, handlesMultipleEntities));
      } catch (ClassNotFoundException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    return pages;
  }

  /**
   * @return The Singleton Instance.
   */
  public static Controller getDefault() {
    if (fInstance == null)
      fInstance = new Controller();

    return fInstance;
  }

  /**
   * @return <code>true</code> if the {@link Controller} has been initialized
   * before and <code>false</code> otherwise.
   */
  public static boolean isInitialized() {
    return fInstance != null;
  }

  /**
   * @param entities
   * @return The EntityPropertyPageWrappers for the given Entity.
   */
  public Set<EntityPropertyPageWrapper> getEntityPropertyPagesFor(List<IEntity> entities) {
    Set<EntityPropertyPageWrapper> pages = new HashSet<EntityPropertyPageWrapper>();

    /* Retrieve Class-Objects from Entities */
    Set<Class<? extends IEntity>> entityClasses = new HashSet<Class<? extends IEntity>>();
    for (IEntity entity : entities)
      entityClasses.add(entity.getClass());

    /* For each contributed Entity Property-Page */
    for (EntityPropertyPageWrapper pageWrapper : fEntityPropertyPages) {

      /* Ignore Pages that dont handle Multi-Selection */
      if (!pageWrapper.isHandlingMultipleEntities() && entities.size() > 1)
        continue;

      /* Check if Page is handling all of the given Entity-Classes */
      if (pageWrapper.handles(entityClasses))
        pages.add(pageWrapper);
    }

    return pages;
  }

  /**
   * @return Returns the savedSearchService.
   */
  public SavedSearchService getSavedSearchService() {
    return fSavedSearchService;
  }

  /**
   * @return Returns the download service.
   */
  public DownloadService getDownloadService() {
    return fDownloadService;
  }

  /**
   * @return Returns the notificationService.
   */
  public NotificationService getNotificationService() {
    return fNotificationService;
  }

  /**
   * @return Returns the contextService.
   */
  public ContextService getContextService() {

    /* Create the Context Service if not yet done */
    if (fContextService == null)
      fContextService = new ContextService();

    return fContextService;
  }

  /**
   * @return Returns the JobFamily all reload-jobs belong to.
   */
  public Object getReloadFamily() {
    return fReloadFeedQueue;
  }

  /**
   * @return Returns the reload-service.
   */
  public FeedReloadService getReloadService() {
    return fFeedReloadService;
  }

  /**
   * Reload the given List of BookMarks. The BookMarks are processed in a queue
   * that stores all Tasks of this kind and guarantees that a certain amount of
   * Jobs process the Task concurrently.
   *
   * @param bookmarks The BookMarks to reload.
   * @param shell The Shell this operation is running in, used to open Dialogs
   * if necessary.
   */
  public void reloadQueued(Set<IBookMark> bookmarks, final Shell shell) {

    /* Decide wether this is a high prio Job */
    boolean highPrio = bookmarks.size() == 1;

    /* Create a Task for each Feed to Reload */
    List<ITask> tasks = new ArrayList<ITask>();
    for (final IBookMark bookmark : bookmarks) {
      ReloadTask task = new ReloadTask(bookmark, shell, highPrio ? ITask.Priority.SHORT : ITask.Priority.DEFAULT);

      /* Check if Task is not yet Queued already */
      if (!fReloadFeedQueue.isQueued(task))
        tasks.add(task);
    }

    fReloadFeedQueue.schedule(tasks);
  }

  /**
   * Reload the given BookMark. The BookMark is processed in a queue that stores
   * all Tasks of this kind and guarantees that a certain amount of Jobs process
   * the Task concurrently.
   *
   * @param bookmark The BookMark to reload.
   * @param shell The Shell this operation is running in, used to open Dialogs
   * if necessary.
   */
  public void reloadQueued(IBookMark bookmark, final Shell shell) {

    /* Create a Task for the Bookmark to Reload */
    ReloadTask task = new ReloadTask(bookmark, shell, ITask.Priority.DEFAULT);

    /* Check if Task is not yet Queued already */
    if (!fReloadFeedQueue.isQueued(task))
      fReloadFeedQueue.schedule(task);
  }

  /**
   * Reload the given BookMark.
   *
   * @param bookmark The BookMark to reload.
   * @param shell The Shell this operation is running in, used to open Dialogs
   * if necessary, or <code>NULL</code> if no Shell is available.
   * @param monitor A monitor to report progress and respond to cancelation. Use
   * a <code>NullProgressMonitor</code> if no progress is to be reported.
   * @return Returns the Status of the Operation.
   */
  public IStatus reload(final IBookMark bookmark, Shell shell, final IProgressMonitor monitor) {
    Assert.isNotNull(bookmark);
    CoreException ex = null;

    /* Keep URL and Homepage of Feed as final var */
    final URI feedLink = bookmark.getFeedLinkReference().getLink();
    URI feedHomepage = null;

    try {

      /* Return on Cancelation or shutdown or deletion */
      if (!shouldProceedReloading(monitor, bookmark))
        return Status.CANCEL_STATUS;

      /* Notify about Bookmark getting loaded */
      fireBookMarkAboutToLoad(bookmark);

      /* Load Conditional Get for the URL */
      IConditionalGet conditionalGet = fConditionalGetDAO.load(feedLink);

      /* Define Properties for Connection */
      Map<Object, Object> properties = new HashMap<Object, Object>();
      properties.put(IConnectionPropertyConstants.CON_TIMEOUT, fConnectionTimeout);

      /* Add Conditional GET Headers if present */
      if (conditionalGet != null) {
        String ifModifiedSince = conditionalGet.getIfModifiedSince();
        if (ifModifiedSince != null)
          properties.put(IConnectionPropertyConstants.IF_MODIFIED_SINCE, ifModifiedSince);

        String ifNoneMatch = conditionalGet.getIfNoneMatch();
        if (ifNoneMatch != null)
          properties.put(IConnectionPropertyConstants.IF_NONE_MATCH, ifNoneMatch);
      }

      /* Return on Cancelation or shutdown or deletion */
      if (!shouldProceedReloading(monitor, bookmark))
        return Status.CANCEL_STATUS;

      /* Load the Feed */
      final Triple<IFeed, IConditionalGet, URI> result = Owl.getConnectionService().reload(feedLink, monitor, properties);

      /* Return on Cancelation or shutdown or deletion */
      if (!shouldProceedReloading(monitor, bookmark) || result == null)
        return Status.CANCEL_STATUS;

      /* Remember Homepage of feed */
      feedHomepage = result.getFirst().getHomepage();

      /* Update ConditionalGet Entity */
      boolean conditionalGetIsNull = (conditionalGet == null);
      conditionalGet = updateConditionalGet(feedLink, conditionalGet, result.getSecond());
      boolean deleteConditionalGet = (!conditionalGetIsNull && conditionalGet == null);

      /* Return on Cancelation or shutdown or deletion */
      if (!shouldProceedReloading(monitor, bookmark))
        return Status.CANCEL_STATUS;

      /* Load the Favicon directly afterwards if required */
      if (!InternalOwl.PERF_TESTING && OwlUI.getFavicon(bookmark) == null)
        loadFavicon(bookmark, monitor, result.getThird(), feedHomepage);

      /* Return on Cancelation or shutdown or deletion */
      if (!shouldProceedReloading(monitor, bookmark))
        return Status.CANCEL_STATUS;

      /* Merge and Save Feed */
      if (!InternalOwl.TESTING) {
        final IConditionalGet finalConditionalGet = conditionalGet;
        final boolean finalDeleteConditionalGet = deleteConditionalGet;
        fSaveFeedQueue.schedule(new TaskAdapter() {
          public IStatus run(IProgressMonitor otherMonitor) {

            /* Return on Cancelation or shutdown or deletion */
            if (otherMonitor.isCanceled() || !shouldProceedReloading(monitor, bookmark))
              return Status.CANCEL_STATUS;

            /* Handle Feed Reload */
            fAppService.handleFeedReload(bookmark, result.getFirst(), finalConditionalGet, finalDeleteConditionalGet, otherMonitor);
            return Status.OK_STATUS;
          }

          @Override
          public String getName() {
            return Messages.Controller_UPDATING_FEEDS_JOB;
          }
        });
      } else {
        fAppService.handleFeedReload(bookmark, result.getFirst(), conditionalGet, deleteConditionalGet, new NullProgressMonitor());
      }
    }

    /* Error while reloading */
    catch (CoreException e) {
      ex = e;

      /* Authentication Required */
      final Shell[] shellAr = new Shell[] { shell };
      if (e instanceof AuthenticationRequiredException && shouldProceedReloading(monitor, bookmark)) {

        /* Resolve active Shell if necessary */
        if (shellAr[0] == null || shellAr[0].isDisposed()) {
          SafeRunner.run(new LoggingSafeRunnable() {
            public void run() throws Exception {
              shellAr[0] = OwlUI.getActiveShell();
            }
          });
        }

        /* Only one Login Dialog at the same time */
        if (shellAr[0] != null && !shellAr[0].isDisposed()) {
          fLoginDialogLock.lock();
          try {
            final AuthenticationRequiredException authEx = (AuthenticationRequiredException) e;
            JobRunner.runSyncedInUIThread(shellAr[0], new Runnable() {
              public void run() {

                /* Return on Cancelation or shutdown or deletion */
                if (!shouldProceedReloading(monitor, bookmark))
                  return;

                /* Credentials might have been provided meanwhile in another dialog */
                try {
                  URI normalizedUri = URIUtils.normalizeUri(feedLink, true);
                  if (Owl.getConnectionService().getAuthCredentials(normalizedUri, authEx.getRealm()) != null) {
                    reloadQueued(bookmark, shellAr[0]);
                    return;
                  }
                } catch (CredentialsException exe) {
                  Activator.getDefault().getLog().log(exe.getStatus());
                }

                /* Show Login Dialog */
                LoginDialog login = new LoginDialog(shellAr[0], feedLink, authEx.getRealm());
                if (login.open() == Window.OK && shouldProceedReloading(monitor, bookmark)) {

                  /* Store info about Realm in Bookmark */
                  if (StringUtils.isSet(authEx.getRealm())) {
                    bookmark.setProperty(BM_REALM_PROPERTY, authEx.getRealm());
                    fBookMarkDAO.save(bookmark);
                  }

                  /* Re-Reload Bookmark */
                  reloadQueued(bookmark, shellAr[0]);
                }

                /* Update Error Flag if user hit Cancel */
                else if (shouldProceedReloading(monitor, bookmark) && !bookmark.isErrorLoading()) {
                  bookmark.setErrorLoading(true);
                  if (StringUtils.isSet(authEx.getMessage()))
                    bookmark.setProperty(LOAD_ERROR_KEY, authEx.getMessage());
                  fBookMarkDAO.save(bookmark);
                }
              }
            });

            return Status.OK_STATUS;
          } finally {
            fLoginDialogLock.unlock();
          }
        }
      }

      /* Load the Favicon directly afterwards if required */
      else if (!InternalOwl.TESTING && (e instanceof NotModifiedException || e instanceof InterpreterException || e instanceof ParserException) && OwlUI.getFavicon(bookmark) == null && shouldProceedReloading(monitor, bookmark)) {
        loadFavicon(bookmark, monitor, feedLink, feedHomepage);
      }

      /* Feed has not been Modified Since */
      if (e instanceof NotModifiedException)
        return Status.OK_STATUS;

      /* Reload was canceled (avoid logging to avoid spam) */
      if (e.getStatus() != null && e.getStatus().getException() instanceof MonitorCanceledException)
        return Status.OK_STATUS;

      /* Report Exceptions as Info to avoid Log Spam */
      return createInfoStatus(e.getStatus(), bookmark, feedLink);
    }

    /* Save Error State to the Bookmark if present */
    finally {
      updateErrorIndicator(bookmark, monitor, ex);

      /* Notify about Bookmark done loading */
      fireBookMarkDoneLoading(bookmark);
    }

    return Status.OK_STATUS;
  }

  private void loadFavicon(final IBookMark bookmark, final IProgressMonitor monitor, final URI feedLink, URI feedHomepage) {
    try {
      byte[] faviconBytes = null;

      /* First try using the Homepage of the Feed */
      if (feedHomepage != null && StringUtils.isSet(feedHomepage.toString()) && feedHomepage.isAbsolute())
        faviconBytes = Owl.getConnectionService().getFeedIcon(feedHomepage, monitor);

      /* Then try with Feed address itself */
      if (faviconBytes == null)
        faviconBytes = Owl.getConnectionService().getFeedIcon(feedLink, monitor);

      /* Store locally */
      if (shouldProceedReloading(monitor, bookmark))
        OwlUI.storeImage(bookmark.getId(), faviconBytes, OwlUI.BOOKMARK, 16, 16);
    } catch (UnknownProtocolException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    } catch (ConnectionException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }
  }

  private void updateErrorIndicator(final IBookMark bookmark, final IProgressMonitor monitor, CoreException ex) {

    /* Return on Cancelation or shutdown or deletion */
    if (!shouldProceedReloading(monitor, bookmark))
      return;

    /* Reset Error-Loading flag if necessary */
    if (bookmark.isErrorLoading() && (ex == null || ex instanceof NotModifiedException)) {
      bookmark.setErrorLoading(false);
      bookmark.removeProperty(LOAD_ERROR_KEY);
      fBookMarkDAO.save(bookmark);
    }

    /* Set Error-Loading flag if necessary */
    else if (ex != null && !(ex instanceof NotModifiedException) && !(ex instanceof AuthenticationRequiredException)) {
      boolean wasShowingError = bookmark.isErrorLoading();
      Object oldMessage = bookmark.getProperty(LOAD_ERROR_KEY);

      bookmark.setErrorLoading(true);
      String message = CoreUtils.toMessage(ex);
      if (StringUtils.isSet(message))
        bookmark.setProperty(LOAD_ERROR_KEY, message);
      else
        bookmark.removeProperty(LOAD_ERROR_KEY);

      if (!wasShowingError || (oldMessage != null && message == null) || (message != null && !message.equals(oldMessage)))
        fBookMarkDAO.save(bookmark);
    }
  }

  private boolean shouldProceedReloading(IProgressMonitor monitor, IBookMark mark) {
    if (InternalOwl.TESTING)
      return true;

    if (fShuttingDown)
      return false;

    if (monitor.isCanceled())
      return false;

    if (isDeleted(mark))
      return false;

    return true;
  }

  private boolean isDeleted(IBookMark mark) {
    Long id = mark.getId();
    if (id != null)
      return fDeletedBookmarksCache.containsKey(id);

    return false;
  }

  /*
   * Note that this does not save the conditional get, it just updates the its
   * values.
   */
  private IConditionalGet updateConditionalGet(final URI feedLink, IConditionalGet oldConditionalGet, IConditionalGet newConditionalGet) {

    /* Conditional Get not provided, return */
    if (newConditionalGet == null)
      return null;

    String ifModifiedSince = newConditionalGet.getIfModifiedSince();
    String ifNoneMatch = newConditionalGet.getIfNoneMatch();
    if (ifModifiedSince != null || ifNoneMatch != null) {

      /* Create new */
      if (oldConditionalGet == null)
        return fFactory.createConditionalGet(ifModifiedSince, feedLink, ifNoneMatch);

      /* Else: Update old */
      oldConditionalGet.setHeaders(ifModifiedSince, ifNoneMatch);
      return oldConditionalGet;
    }

    return null;
  }

  /**
   * Tells the Controller to start. This method is called automatically from
   * osgi as soon as the org.rssowl.ui bundle gets activated.
   */
  public void startup() {

    /* Create Relations and Import Default Feeds if required */
    if (!InternalOwl.TESTING) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {

          /* First check wether this action is required */
          IPreference firstStartToken = fPrefsDAO.load(FIRST_START_TOKEN);
          if (firstStartToken != null)
            return;

          onFirstStartup();

          /* Mark this as the first start */
          fPrefsDAO.save(fFactory.createPreference(FIRST_START_TOKEN));
        }
      });
    }

    /* Set hidden News from previous Session to deleted */
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        DynamicDAO.getDAO(INewsDAO.class).setState(EnumSet.of(INews.State.HIDDEN), INews.State.DELETED, false);
      }
    });

    /* Create the Notification Service */
    if (!InternalOwl.TESTING)
      fNotificationService = new NotificationService();

    /* Create the Saved Search Service */
    fSavedSearchService = new SavedSearchService();

    /* Create the Download Service */
    fDownloadService = new DownloadService();

    /* Register Listeners */
    registerListeners();

    /* Load Contributed News Share Providers */
    loadShareProviders();

    /* Load Feed Search Provider */
    fFeedSearchUrl = loadFeedSearchUrl();
  }

  private void loadShareProviders() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(SHARE_PROVIDER_EXTENSION_POINT);

    /* For each contributed property keyword feed */
    for (int i = 0; i < elements.length; i++) {
      IConfigurationElement element = elements[i];

      String id = element.getAttribute("id"); //$NON-NLS-1$
      String name = element.getAttribute("name"); //$NON-NLS-1$
      String iconPath = element.getAttribute("icon"); //$NON-NLS-1$
      String url = element.getAttribute("url"); //$NON-NLS-1$
      String maxTitleLength = element.getAttribute("maxTitleLength"); //$NON-NLS-1$
      String enabled = element.getAttribute("enabled"); //$NON-NLS-1$

      boolean isEnabled = (enabled != null && Boolean.parseBoolean(enabled));
      fShareProviders.add(new ShareProvider(id, element.getNamespaceIdentifier(), i, name, iconPath, url, maxTitleLength, isEnabled));
    }
  }

  private String loadFeedSearchUrl() {
    String feedSearch = null;
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(FEED_SEARCH_EXTENSION_POINT);
    for (IConfigurationElement element : elements) {
      try {
        feedSearch = element.getAttribute("searchUrl"); //$NON-NLS-1$

        /* Let 3d-Party contributions override our contributions */
        String nsId = element.getNamespaceIdentifier();
        if (!nsId.contains(ExtensionUtils.RSSOWL_NAMESPACE))
          return feedSearch;
      } catch (InvalidRegistryObjectException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    return feedSearch;
  }

  /**
   * @return a sorted {@link List} of all contributed providers for sharing
   * links.
   */
  public List<ShareProvider> getShareProviders() {
    IPreferenceScope preferences = Owl.getPreferenceService().getGlobalScope();
    int[] providerState = preferences.getIntegers(DefaultPreferences.SHARE_PROVIDER_STATE);

    /* Ignore State if Number of Providers got smaller */
    if (providerState.length > fShareProviders.size())
      return fShareProviders;

    List<ShareProvider> sortedProviders = new ArrayList<ShareProvider>();
    for (int i = 0; i < providerState.length; i++) {
      int providerIndex = providerState[i];
      boolean enabled = providerIndex > 0;
      if (providerIndex < 0)
        providerIndex = providerIndex * -1;
      providerIndex--; //Adjust to zero-indexing

      if (providerIndex < fShareProviders.size()) {
        ShareProvider provider = fShareProviders.get(providerIndex);
        provider.setEnabled(enabled);
        sortedProviders.add(provider);
      }
    }

    /* Add missing ones as disabled if any (can happen for new contributions) */
    for (ShareProvider shareProvider : fShareProviders) {
      if (!sortedProviders.contains(shareProvider)) {
        shareProvider.setEnabled(false);
        sortedProviders.add(shareProvider);
      }
    }

    return sortedProviders;
  }

  /**
   * Tells the Controller to stop. This method is called automatically from osgi
   * as soon as the org.rssowl.ui bundle gets stopped.
   *
   * @param emergency If set to <code>TRUE</code>, this method is called from a
   * shutdown hook that got triggered from a non-normal shutdown (e.g. System
   * Shutdown).
   */
  public void shutdown(boolean emergency) {
    fShuttingDown = true;

    /* Emergency Shutdown */
    if (emergency)
      emergencyShutdown();

    /* Normal Shutdown */
    else
      normalShutdown();
  }

  private void normalShutdown() {
    fShuttingDown = true;

    /* Unregister Listeners */
    unregisterListeners();

    /* Stop the Download Service */
    if (fDownloadService != null)
      fDownloadService.stopService();

    /* Stop Clean-Up Reminder Service */
    if (!InternalOwl.TESTING && fCleanUpReminderService != null)
      fCleanUpReminderService.stopService();

    /* Stop the Feed Reload Service */
    if (!InternalOwl.TESTING && fFeedReloadService != null)
      fFeedReloadService.stopService();

    /* Cancel/Seal the reload queue */
    if (fReloadFeedQueue != null)
      fReloadFeedQueue.cancel(false, true);

    /* Cancel the feed-save queue (join) */
    if (fSaveFeedQueue != null)
      fSaveFeedQueue.cancel(true, true);

    /* Stop the Notification Service */
    if (!InternalOwl.TESTING && fNotificationService != null)
      fNotificationService.stopService();

    /* Stop the Saved Search Service */
    if (fSavedSearchService != null)
      fSavedSearchService.stopService();

    /* Shutdown ApplicationServer */
    ApplicationServer.getDefault().shutdown();
  }

  private void emergencyShutdown() {
    fShuttingDown = true;

    /* Cancel/Seal the reload queue */
    if (fReloadFeedQueue != null)
      fReloadFeedQueue.seal();

    /* Cancel the feed-save queue (join) */
    if (fSaveFeedQueue != null)
      fSaveFeedQueue.cancel(true, true);
  }

  /**
   * This method is called just after the Window has opened.
   */
  public void postWindowOpen() {

    /* Create the Feed-Reload Service */
    if (!InternalOwl.TESTING)
      fFeedReloadService = new FeedReloadService();

    /* Create the Clean-Up Reminder Service */
    fCleanUpReminderService = new CleanUpReminderService();

    /* Support Keybindings for assigning Labels */
    defineLabelCommands(CoreUtils.loadSortedLabels());

    /* Check for Status of Startup */
    IStatus startupStatus = Owl.getPersistenceService().getStartupStatus();
    if (startupStatus.getSeverity() == IStatus.ERROR)
      ErrorDialog.openError(OwlUI.getPrimaryShell(), Messages.Controller_STARTUP_ERROR, Messages.Controller_START_ERROR_MSG, startupStatus);

    /* Backup Subscriptions as OPML if no error */
    else {
      JobRunner.runDelayedInBackgroundThread(new Runnable() {
        public void run() {
          SafeRunner.run(new LoggingSafeRunnable() {
            public void run() throws Exception {
              if (!fShuttingDown)
                backupSubscriptions();
            }
          });
        }
      });
    }

    /* Show the Welcome & Tutorial Wizard if this is the first startup */
    if (fShowWelcome) {
      fShowWelcome = false; //Set to false to avoid another Wizard when opening new window
      JobRunner.runInUIThread(200, OwlUI.getActiveShell(), new Runnable() {
        public void run() {
          showWelcomeAndTutorial();
        }
      });
    }

    /* Check for Updates if Set */
    else if (!Application.IS_ECLIPSE && !fDisableUpdate) {
      JobRunner.runInUIThread(5000, OwlUI.getActiveShell(), new Runnable() {
        public void run() {
          if (Owl.getPreferenceService().getGlobalScope().getBoolean(DefaultPreferences.UPDATE_ON_STARTUP)) {
            FindUpdatesAction action = new FindUpdatesAction(false);
            action.init(OwlUI.getWindow());
            action.run();
          }
        }
      });
    }

    /* Inform the User if the ApplicationServer is not running */
    ApplicationServer server = ApplicationServer.getDefault();
    if (!server.isRunning()) {
      Shell shell = OwlUI.getPrimaryShell();
      if (shell != null)
        MessageDialog.openError(shell, Messages.Controller_ERROR, NLS.bind(Messages.Controller_ERROR_STARTING_SERVER, server.getPort(), server.getHost()));
    }

    /* Indicate Application is started */
    fIsStarted = true;
  }

  private void showWelcomeAndTutorial() {
    Shell activeShell = OwlUI.getActiveShell();

    /* Show Welcome */
    WelcomeWizard welcomeWizard = new WelcomeWizard();
    OwlUI.openWizard(activeShell, welcomeWizard, true, true, null);

    /* Show Tutorial */
    if (PlatformUI.isWorkbenchRunning() && !fShuttingDown) { //Do not show if User asked to Restart
      TutorialWizard tutorialWizard = new TutorialWizard();
      OwlUI.openWizard(activeShell, tutorialWizard, false, false, null);
    }
  }

  private void backupSubscriptions() {
    IPath rootPath = Platform.getLocation();
    File root = rootPath.toFile();
    if (!root.exists())
      root.mkdir();

    IPath dailyBackupPath = rootPath.append(DAILY_BACKUP);
    IPath backupTmpPath = rootPath.append(BACKUP_TMP);
    IPath weeklyBackupPath = rootPath.append(WEEKLY_BACKUP);

    File dailyBackupFile = dailyBackupPath.toFile();
    File backupTmpFile = backupTmpPath.toFile();
    backupTmpFile.deleteOnExit();
    File weeklyBackupFile = weeklyBackupPath.toFile();

    if (dailyBackupFile.exists()) {

      /* Update Weekly Backup if required */
      if (!weeklyBackupFile.exists() || (weeklyBackupFile.lastModified() + DateUtils.WEEK < System.currentTimeMillis())) {
        weeklyBackupFile.delete();
        dailyBackupFile.renameTo(weeklyBackupFile);
      }

      /* Check 1 Day Condition */
      long lastModified = dailyBackupFile.lastModified();
      if (lastModified + DateUtils.DAY > System.currentTimeMillis())
        return;
    }

    /* Create Daily Backup */
    try {
      Set<IFolder> rootFolders = CoreUtils.loadRootFolders();
      if (!rootFolders.isEmpty() && !fShuttingDown) {
        Owl.getInterpreter().exportTo(backupTmpFile, rootFolders, EnumSet.of(Options.EXPORT_FILTERS, Options.EXPORT_LABELS, Options.EXPORT_PREFERENCES));

        /* Rename to actual backup in a short op to avoid corrupt data */
        if (!backupTmpFile.renameTo(dailyBackupFile)) {
          dailyBackupFile.delete();
          backupTmpFile.renameTo(dailyBackupFile);
        }
      }
    } catch (InterpreterException e) {
      if (!fShuttingDown)
        Activator.safeLogError(e.getMessage(), e);
    }
  }

  /**
   * Returns wether the application is in process of shutting down.
   *
   * @return <code>TRUE</code> if the application has been closed, and
   * <code>FALSE</code> otherwise.
   */
  public boolean isShuttingDown() {
    return fShuttingDown;
  }

  /**
   * Returns wether the application is in process of restarting.
   *
   * @return <code>TRUE</code> if the application is restarting, and
   * <code>FALSE</code> otherwise.
   */
  public boolean isRestarting() {
    return fRestarting;
  }

  /**
   * Returns wether the application has finished starting.
   *
   * @return <code>TRUE</code> if the application is started, and
   * <code>FALSE</code> otherwise if still initializing.
   */
  public boolean isStarted() {
    return fIsStarted;
  }

  /**
   * This method is called immediately prior to workbench shutdown before any
   * windows have been closed.
   *
   * @return <code>true</code> to allow the workbench to proceed with shutdown,
   * <code>false</code> to veto a non-forced shutdown
   */
  public boolean preUIShutdown() {
    fShuttingDown = true;

    return true;
  }

  private void onFirstStartup() {

    /* Add Default Labels */
    addDefaultLabels();

    /* Import File if specified */
    boolean showWelcome = true;
    String importFile = System.getProperty(IMPORT_PROPERTY);
    if (StringUtils.isSet(importFile) && new File(importFile).exists()) {
      try {
        initialImportFile(importFile);
        showWelcome = false;
      } catch (Exception e) {
        Activator.safeLogError(e.getMessage(), e);
      }
    }

    /* Show Welcome Otherwise */
    if (showWelcome) {
      DynamicDAO.save(Owl.getModelFactory().createFolder(null, null, Messages.Controller_MY_BOOKMARKS));
      fShowWelcome = true;
    }
  }

  private void addDefaultLabels() throws PersistenceException {
    ILabel label = fFactory.createLabel(null, Messages.Controller_IMPORTANT);
    label.setColor("163,21,2"); //$NON-NLS-1$
    label.setOrder(0);
    fLabelDao.save(label);

    label = fFactory.createLabel(null, Messages.Controller_WORK);
    label.setColor("200,118,10"); //$NON-NLS-1$
    label.setOrder(1);
    fLabelDao.save(label);

    label = fFactory.createLabel(null, Messages.Controller_PERSONAL);
    label.setColor("82,92,58"); //$NON-NLS-1$
    label.setOrder(2);
    fLabelDao.save(label);

    label = fFactory.createLabel(null, Messages.Controller_TODO);
    label.setColor("92,101,126"); //$NON-NLS-1$
    label.setOrder(3);
    fLabelDao.save(label);

    label = fFactory.createLabel(null, Messages.Controller_LATER);
    label.setColor("82,16,0"); //$NON-NLS-1$
    label.setOrder(4);
    fLabelDao.save(label);
  }

  private void initialImportFile(String file) throws PersistenceException, InterpreterException, ParserException, FileNotFoundException {
    InputStream inS = new FileInputStream(file);
    List<? extends IEntity> types = Owl.getInterpreter().importFrom(inS);

    ImportUtils.doImport(null, types, false);
  }

  private IStatus createInfoStatus(IStatus status, IBookMark bookmark, URI feedLink) {
    StringBuilder msg = createLogEntry(bookmark, feedLink, status.getMessage());
    return new Status(IStatus.INFO, status.getPlugin(), status.getCode(), msg.toString(), null);
  }

  StringBuilder createLogEntry(IBookMark bookmark, URI feedLink, String msg) {
    StringBuilder entry = new StringBuilder();
    entry.append(NLS.bind(Messages.Controller_ERROR_LOADING, bookmark.getName()));

    if (StringUtils.isSet(msg))
      entry.append(fNl).append(NLS.bind(Messages.Controller_PROBLEM, msg));

    if (feedLink != null)
      entry.append(fNl).append(NLS.bind(Messages.Controller_LINK, feedLink));
    else if (bookmark.getFeedLinkReference() != null)
      entry.append(fNl).append(NLS.bind(Messages.Controller_LINK, bookmark.getFeedLinkReference().getLinkAsText()));

    return entry;
  }

  /*
   * Registeres a command per Label to assign key-bindings. Should be called
   * when {@link ILabel} get added, updated or removed and must be called once
   * after startup.
   */
  private void defineLabelCommands(Collection<ILabel> labels) {
    if (InternalOwl.TESTING)
      return;

    ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
    if (commandService == null)
      return;

    /* Define Command For Each Label */
    for (final ILabel label : labels) {
      Command command = commandService.getCommand(LABEL_ACTION_PREFIX + label.getOrder());
      command.define(NLS.bind(Messages.Controller_LABEL, label.getName()), NLS.bind(Messages.Controller_LABEL_MSG, label.getName()), commandService.getCategory(RSSOWL_KEYBINDING_CATEGORY));
      command.setHandler(new LabelNewsHandler(label));
    }
  }

  private void updateLabelCommands() {
    Set<ILabel> labels = CoreUtils.loadSortedLabels();
    undefineLabelCommands(labels);
    defineLabelCommands(labels);
  }

  /* TODO Also need to remove any keybinding associated with Label if existing */
  private void undefineLabelCommands(Collection<ILabel> labels) {
    if (InternalOwl.TESTING)
      return;

    ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
    if (commandService == null)
      return;

    for (ILabel label : labels) {
      commandService.getCommand(LABEL_ACTION_PREFIX + label.getOrder()).undefine();
    }
  }

  /**
   * @param listener the listener thats gets informed when a bookmark is loaded
   * from the controller.
   */
  public void addBookMarkLoadListener(BookMarkLoadListener listener) {
    fBookMarkLoadListeners.add(listener);
  }

  /**
   * @param listener the listener thats gets informed when a bookmark is done
   * loading from the controller.
   */
  public void removeBookMarkLoadListener(BookMarkLoadListener listener) {
    fBookMarkLoadListeners.remove(listener);
  }

  /**
   * @return the {@link Lock} used to ensure that at any time only a single
   * login dialog is showing.
   */
  public Lock getLoginDialogLock() {
    return fLoginDialogLock;
  }

  /**
   * @param keywords the keywords to use as search for feeds.
   * @return a {@link String} that can be used as link for search.
   */
  public String toFeedSearchLink(String keywords) {
    String encodedKeywords = URIUtils.urlEncode(keywords.trim());
    return StringUtils.replaceAll(fFeedSearchUrl, FEED_SEARCH_KEYWORD_TOKEN, encodedKeywords);
  }

  private void fireBookMarkAboutToLoad(final IBookMark bookmark) {
    Object[] listeners = fBookMarkLoadListeners.getListeners();
    for (final Object listener : listeners) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          ((BookMarkLoadListener) listener).bookMarkAboutToLoad(bookmark);
        }
      });
    }
  }

  private void fireBookMarkDoneLoading(final IBookMark bookmark) {
    Object[] listeners = fBookMarkLoadListeners.getListeners();
    for (final Object listener : listeners) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          ((BookMarkLoadListener) listener).bookMarkDoneLoading(bookmark);
        }
      });
    }
  }

  /**
   * Cancels all pending Updates to Feeds.
   */
  public void stopUpdate() {
    fReloadFeedQueue.cancel(false, false);
  }

  /**
   * @return <code>true</code> if this application runs in portable mode and
   * <code>false</code> otherwise.
   */
  public boolean isPortable() {
    return fPortable;
  }

  /**
   * @return <code>true</code> if updates should be disabled and
   * <code>false</code> otherwise.
   */
  public boolean isUpdateDisabled() {
    return fDisableUpdate;
  }

  /**
   * Restart the Application.
   */
  public void restart() {

    /* First Check for active Downloads */
    if (fDownloadService.isActive()) {
      MessageDialog.openWarning(OwlUI.getActiveShell(), Messages.Controller_RESTART_RSSOWL, Messages.Controller_ACTIVE_DOWNLOADS_WARNING);
      return;
    }

    fRestarting = true;

    /* Run the restart() */
    PlatformUI.getWorkbench().restart();
  }
}