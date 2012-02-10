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

package org.rssowl.ui.internal.services;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.progress.IProgressConstants;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.HttpConnectionInputStream;
import org.rssowl.core.connection.IAbortable;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.dialogs.LoginDialog;
import org.rssowl.ui.internal.util.DownloadJobQueue;
import org.rssowl.ui.internal.util.JobRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service to download files in a {@link DownloadJobQueue} with proper
 * progress reporting.
 *
 * @author bpasero
 */
public class DownloadService {

  /* Max. number of concurrent Jobs for downloading files */
  private static final int MAX_CONCURRENT_DOWNLOAD_JOBS = 3;

  /* Connection Timeouts in MS */
  private static final int DEFAULT_CON_TIMEOUT = 30000;

  /* Default Length for Download Tasks */
  private static final int DEFAULT_TASK_LENGTH = 1000000;

  /* Default Progress for Download Tasks */
  private static final int DEFAULT_WORKED = 200;

  private DownloadJobQueue fDownloadQueue;
  private Map<OutputStream, OutputStream> fOutputStreamMap = new ConcurrentHashMap<OutputStream, OutputStream>();
  private IPreferenceScope fPreferences = Owl.getPreferenceService().getGlobalScope();

  /* Task for a Download */
  private class AttachmentDownloadTask extends DownloadJobQueue.DownloadTask {
    private final IAttachment fAttachment;
    private final URI fFile;
    private final File fFolder;
    private final boolean fUserInitiated;
    private final String fName;

    private AttachmentDownloadTask(IAttachment attachment, URI file, String name, File folder, boolean userInitiated) {
      fAttachment = attachment;
      fFile = file;
      fName = name;
      fFolder = folder;
      fUserInitiated = userInitiated;
    }

    @Override
    public IStatus run(Job job, IProgressMonitor monitor) {
      return internalDownload(job, fAttachment, fFile, fName, fFolder, monitor, fUserInitiated);
    }

    public String getName() {
      return NLS.bind(Messages.DownloadService_DOWNLOADING_N, fFile.toString());
    }

    public Priority getPriority() {
      return Priority.DEFAULT;
    }

    @Override
    public int hashCode() {
      return fFile.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;

      if (obj == null)
        return false;

      if (getClass() != obj.getClass())
        return false;

      final AttachmentDownloadTask other = (AttachmentDownloadTask) obj;
      return fFile.equals(other.fFile);
    }
  }

  /** Default Constructor to create a Download Queue */
  public DownloadService() {
    fDownloadQueue = new DownloadJobQueue(Messages.DownloadService_DOWNLOADING_TITLE, MAX_CONCURRENT_DOWNLOAD_JOBS, Integer.MAX_VALUE);
  }

  /**
   * @param attachment the {@link IAttachment} to download.
   * @param file the file to download as {@link URI}.
   * @param folder the folder to download to as {@link File}.
   * @param userInitiated if <code>true</code> the user explicitly asked for a
   * download and <code>false</code> otherwise (e.g. if download is from a
   * filter).
   */
  public void download(IAttachment attachment, URI file, File folder, boolean userInitiated) {
    download(attachment, file, null, folder, userInitiated);
  }

  /**
   * @param attachment the {@link IAttachment} to download.
   * @param file the file to download as {@link URI}.
   * @param name the name of the file to use or <code>null</code> to take this
   * information from the link.
   * @param folder the folder to download to as {@link File}.
   * @param userInitiated if <code>true</code> the user explicitly asked for a
   * download and <code>false</code> otherwise (e.g. if download is from a
   * filter).
   */
  public void download(IAttachment attachment, URI file, String name, File folder, boolean userInitiated) {
    AttachmentDownloadTask task = new AttachmentDownloadTask(attachment, file, name, folder, userInitiated);
    if (!fDownloadQueue.isQueued(task))
      fDownloadQueue.schedule(task);
  }

  private IStatus internalDownload(Job job, final IAttachment attachment, final URI link, final String fileName, final File folder, final IProgressMonitor monitor, final boolean userInitiated) {
    String downloadFileName;
    if (StringUtils.isSet(fileName))
      downloadFileName = fileName;
    else
      downloadFileName = URIUtils.getFile(link, OwlUI.getExtensionForMime(attachment.getType()));

    File downloadFile = new File(folder, downloadFileName);
    job.setName(NLS.bind(Messages.DownloadService_DOWNLOADING, downloadFileName));
    job.setProperty(IProgressConstants.ICON_PROPERTY, OwlUI.getAttachmentImage(downloadFileName, attachment.getType()));

    int bytesConsumed = 0;
    try {
      IProtocolHandler handler = Owl.getConnectionService().getHandler(link);
      if (handler != null) {
        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties.put(IConnectionPropertyConstants.CON_TIMEOUT, DEFAULT_CON_TIMEOUT);

        /* Check for Cancellation and Shutdown */
        if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
          return Status.CANCEL_STATUS;

        /* Initialize Fields */
        long bytesPerSecond = 0;
        long lastTaskNameUpdate = 0;
        long lastBytesCheck = 0;
        int length = attachment.getLength();
        byte[] buffer = new byte[8192];

        /* Begin Task */
        monitor.beginTask(formatTask(bytesConsumed, length, -1), length > 0 ? length : DEFAULT_TASK_LENGTH);

        /* First Download to a temporary File */
        int contentLength = length;
        InputStream in = null;
        FileOutputStream out = null;
        File partFile = new File(folder, downloadFileName + ".part"); //$NON-NLS-1$
        boolean canceled = false;
        Exception error = null;
        try {

          /* Open Stream */
          in = handler.openStream(link, monitor, properties);

          /* Obtain real Content Length from Stream if available */
          if (in instanceof HttpConnectionInputStream) {
            int len = ((HttpConnectionInputStream) in).getContentLength();
            if (len > 0)
              contentLength = len;
          }

          /* Create tmp part File */
          partFile.createNewFile();
          partFile.deleteOnExit();

          /* Keep Outputstream for later */
          out = new FileOutputStream(partFile);
          fOutputStreamMap.put(out, out);

          /* Download */
          while (true) {

            /* Check for Cancellation and Shutdown */
            if (monitor.isCanceled() || Controller.getDefault().isShuttingDown()) {
              canceled = true;
              return Status.CANCEL_STATUS;
            }

            /* Read from Stream */
            int read = in.read(buffer);
            bytesConsumed += read;
            if (read == -1)
              break;

            /* Write to File */
            out.write(buffer, 0, read);

            /* Update Task Name once per Second */
            long now = System.currentTimeMillis();
            long timeDiff = (now - lastTaskNameUpdate);
            if (timeDiff > 1000) {
              long bytesDiff = bytesConsumed - lastBytesCheck;
              bytesPerSecond = bytesDiff / (timeDiff / 1000);
              monitor.setTaskName(formatTask(bytesConsumed, contentLength, (int) bytesPerSecond));
              lastTaskNameUpdate = now;
              lastBytesCheck = bytesConsumed;
            }

            /* Report accurate progress */
            if (attachment.getLength() > 0)
              monitor.worked(read);

            /* Report calculated progress if possible */
            else if (contentLength > 0) {
              float relWorked = read / (float) contentLength;
              monitor.worked((int) (relWorked * DEFAULT_TASK_LENGTH));
            }

            /* Use a generic Progress Value */
            else
              monitor.worked(DEFAULT_WORKED);
          }
        } catch (FileNotFoundException e) {
          error = e;
          return Activator.getDefault().createErrorStatus(e.getMessage(), e);
        } catch (IOException e) {
          error = e;
          return Activator.getDefault().createErrorStatus(e.getMessage(), e);
        } catch (ConnectionException e) {
          final boolean showError[] = new boolean[] { true };

          /* Offer a Login Dialog if Authentication is Required */
          if (userInitiated && e instanceof AuthenticationRequiredException && !monitor.isCanceled() && !Controller.getDefault().isShuttingDown()) {
            final Shell shell = OwlUI.getActiveShell();
            if (shell != null && !shell.isDisposed()) {
              Controller.getDefault().getLoginDialogLock().lock();
              try {
                final AuthenticationRequiredException authEx = (AuthenticationRequiredException) e;
                JobRunner.runSyncedInUIThread(shell, new Runnable() {
                  public void run() {

                    /* Return on Cancelation or shutdown or deletion */
                    if (monitor.isCanceled() || Controller.getDefault().isShuttingDown())
                      return;

                    /* Credentials might have been provided meanwhile in another dialog */
                    try {
                      URI normalizedUri = URIUtils.normalizeUri(link, true);
                      if (Owl.getConnectionService().getAuthCredentials(normalizedUri, authEx.getRealm()) != null) {
                        fDownloadQueue.schedule(new AttachmentDownloadTask(attachment, link, fileName, folder, userInitiated));
                        showError[0] = false;
                        return;
                      }
                    } catch (CredentialsException exe) {
                      Activator.getDefault().getLog().log(exe.getStatus());
                    }

                    /* Show Login Dialog */
                    LoginDialog login = new LoginDialog(shell, link, authEx.getRealm());
                    if (login.open() == Window.OK && !monitor.isCanceled() && !Controller.getDefault().isShuttingDown()) {
                      fDownloadQueue.schedule(new AttachmentDownloadTask(attachment, link, fileName, folder, userInitiated));
                      showError[0] = false;
                    }
                  }
                });
              } finally {
                Controller.getDefault().getLoginDialogLock().unlock();
              }
            }
          }

          /* User has not Provided Login Credentials or any other error */
          if (showError[0]) {
            error = e;
            return Activator.getDefault().createErrorStatus(e.getMessage(), e);
          }

          /* User has Provided Login Credentials - cancel this Task */
          monitor.setCanceled(true);
          canceled = true;
          return Status.CANCEL_STATUS;
        } finally {
          monitor.done();

          /* Indicate Error Message if any and offer Action to download again */
          if (error != null) {
            String errorMessage = CoreUtils.toMessage(error);
            if (StringUtils.isSet(errorMessage))
              job.setName(NLS.bind(Messages.DownloadService_ERROR_DOWNLOADING_N, downloadFileName, errorMessage));
            else
              job.setName(NLS.bind(Messages.DownloadService_ERROR_DOWNLOADING, downloadFileName));

            job.setProperty(IProgressConstants.ICON_PROPERTY, OwlUI.ERROR);
            job.setProperty(IProgressConstants.ACTION_PROPERTY, getRedownloadAction(new AttachmentDownloadTask(attachment, link, fileName, folder, true)));
            monitor.setTaskName(Messages.DownloadService_TRY_AGAIN);
          }

          /* Close Output Stream */
          if (out != null) {
            try {
              out.close();
              fOutputStreamMap.remove(out);
              if (canceled || error != null)
                partFile.delete();
            } catch (IOException e) {
              return Activator.getDefault().createErrorStatus(e.getMessage(), e);
            }
          }

          /* Close Input Stream */
          if (in != null) {
            try {
              if ((canceled || error != null) && in instanceof IAbortable)
                ((IAbortable) in).abort();
              else
                in.close();
            } catch (IOException e) {
              return Activator.getDefault().createErrorStatus(e.getMessage(), e);
            }
          }
        }

        /* Check for Cancellation and Shutdown */
        if (monitor.isCanceled() || Controller.getDefault().isShuttingDown()) {
          partFile.delete();
          return Status.CANCEL_STATUS;
        }

        /* Now copy over the part file to the actual file in an atomic operation */
        if (!partFile.renameTo(downloadFile)) {
          downloadFile.delete();
          partFile.renameTo(downloadFile);
        }
      }
    } catch (ConnectionException e) {
      return Activator.getDefault().createErrorStatus(e.getMessage(), e);
    }

    /* Offer Action to Open Attachment by keeping Job in Viewer if set */
    if (!fPreferences.getBoolean(DefaultPreferences.HIDE_COMPLETED_DOWNLOADS)) {
      job.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
      job.setProperty(IProgressConstants.ACTION_PROPERTY, getOpenAction(downloadFile));
    }

    /* Update Job Name */
    if (bytesConsumed > 0)
      job.setName(NLS.bind(Messages.DownloadService_N_OF_M, downloadFileName, OwlUI.getSize(bytesConsumed)));
    else
      job.setName(downloadFileName);

    /* The Label of the Status is used as Link for Action */
    return new Status(IStatus.OK, Activator.PLUGIN_ID, Messages.DownloadService_OPEN_FOLDER);
  }

  private String formatTask(int bytesConsumed, int totalBytes, int bytesPerSecond) {
    StringBuilder str = new StringBuilder();

    /* "Time Remaining" */
    int bytesToGo = totalBytes - bytesConsumed;
    if (bytesToGo > 0 && bytesPerSecond > 0) {
      int secondsRemaining = bytesToGo / bytesPerSecond;
      String period = OwlUI.getPeriod(secondsRemaining);
      if (period != null)
        str.append(NLS.bind(Messages.DownloadService_BYTES_REMAINING, period)).append(" - "); //$NON-NLS-1$
    }

    /* "X MB of Y MB "*/
    String consumed = OwlUI.getSize(bytesConsumed);
    if (consumed == null)
      consumed = "0"; //$NON-NLS-1$

    String total = OwlUI.getSize(totalBytes);
    if (total != null)
      str.append(NLS.bind(Messages.DownloadService_BYTES_OF_BYTES, consumed, total));
    else
      str.append(NLS.bind(Messages.DownloadService_BYTES_OF_UNKNOWN, consumed));

    /* "(X MB/sec)" */
    if (bytesPerSecond > 0) {
      str.append(" "); //$NON-NLS-1$
      str.append(NLS.bind(Messages.DownloadService_BYTES_PER_SECOND, OwlUI.getSize(bytesPerSecond)));
    }

    return str.toString();
  }

  private IAction getOpenAction(final File downloadFile) {
    return new Action(Messages.DownloadService_OPEN_FOLDER) {
      @Override
      public void run() {
        Program.launch(downloadFile.getParent());
      }
    };
  }

  private IAction getRedownloadAction(final AttachmentDownloadTask task) {
    return new Action(Messages.DownloadService_RE_DOWNLOAD) {
      @Override
      public void run() {
        fDownloadQueue.schedule(task);
      }
    };
  }

  /**
   * Stops this Service and cancels all pending downloads.
   */
  public void stopService() {
    fDownloadQueue.cancel(false);

    /* Need to properly close yet opened Streams */
    Set<OutputStream> openStreams = fOutputStreamMap.keySet();
    for (OutputStream out : openStreams) {
      try {
        out.close();
      } catch (IOException e) {
        /* Ignore */}
    }
  }

  /**
   * @return <code>true</code> if there are active download jobs running and
   * <code>false</code> otherwise.
   */
  public boolean isActive() {
    return fDownloadQueue.isWorking();
  }
}