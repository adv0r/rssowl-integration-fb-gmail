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

package org.rssowl.ui.internal.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.StreamGobbler;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.dialogs.preferences.BrowserPreferencePage;
import org.rssowl.ui.internal.editors.browser.WebBrowserContext;
import org.rssowl.ui.internal.editors.browser.WebBrowserInput;
import org.rssowl.ui.internal.editors.browser.WebBrowserView;

import java.io.IOException;

/**
 * @author bpasero
 */
public class BrowserUtils {

  /* Either netscape or mozilla for Linux / Solaris */
  private static String webBrowser;

  /* Flag to indicate a successfull launch on Linux / Solaris */
  private static boolean webBrowserSuccessfullyOpened;

  /* This utility class constructor is hidden */
  private BrowserUtils() {
  // Protect default constructor
  }

  /**
   * @param href Any URL
   * @param context the context from which this link is coming from or
   * <code>null</code> if none.
   * @return the {@link WebBrowserInput} created to show the link or
   * <code>null</code> if it could not be created.
   */
  public static WebBrowserView openLinkInternal(String href, WebBrowserContext context) {
    WebBrowserView view = null;

    try {
      IPreferenceScope eclipsePreferences = Owl.getPreferenceService().getEclipseScope();
      IPreferenceScope owlPreferences = Owl.getPreferenceService().getGlobalScope();

      WebBrowserInput input = new WebBrowserInput(href, context);
      IWorkbenchPage page = OwlUI.getPage();
      if (page != null) {
        boolean multipleTabs = eclipsePreferences.getBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS);
        boolean openInBackground = owlPreferences.getBoolean(DefaultPreferences.OPEN_BROWSER_IN_BACKGROUND);
        boolean reuseTab = owlPreferences.getBoolean(DefaultPreferences.ALWAYS_REUSE_BROWSER);

        /* Try to Reuse existing Browser first if set */
        if (reuseTab) {
          WebBrowserView browser = OwlUI.getFirstActiveBrowser();
          if (browser != null) {
            browser.setInput(input);
            if (!openInBackground)
              page.activate(browser);

            return browser;
          }
        }

        /* Open Browser Tab in Background */
        if (multipleTabs && openInBackground) {
          IEditorPart previousActiveEditor = page.getActiveEditor();
          page.getWorkbenchWindow().getShell().setRedraw(false);
          try {
            view = (WebBrowserView) page.openEditor(input, WebBrowserView.EDITOR_ID, true);

            if (previousActiveEditor != null)
              page.activate(previousActiveEditor);
          } finally {
            page.getWorkbenchWindow().getShell().setRedraw(true);
          }
        }

        /* Open Browser Tab in Front */
        else
          view = (WebBrowserView) page.openEditor(input, WebBrowserView.EDITOR_ID, true);
      }
    } catch (PartInitException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }

    return view;
  }

  /**
   * Open a link in the external browser
   *
   * @param href Any URL
   */
  public static void openLinkExternal(String href) {

    /* If href points to local file */
    if (href.startsWith("file:")) { //$NON-NLS-1$
      href = href.substring(5);
      while (href.startsWith("/")) { //$NON-NLS-1$
        href = href.substring(1);
      }
      href = "file:///" + href; //$NON-NLS-1$
    }

    String localHref = href;

    /* Surround href with double quotes if it containes spaces */
    if (localHref.contains(" ")) //$NON-NLS-1$
      localHref = "\"" + localHref + "\""; //$NON-NLS-1$ //$NON-NLS-2$

    /* Open Custom External Browser */
    if (Owl.isStarted() && Owl.getPreferenceService().getGlobalScope().getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER))
      useCustomBrowser(localHref);

    /* Open Default External Browser */
    else
      useDefaultBrowser(localHref);
  }

  /**
   * Open the default Mail Application with the given Subject and Body for a new
   * Mail.
   *
   * @param subject The Subject of the new Mail or NULL if none.
   * @param body The Body of the new Mail or NULL if none.
   */
  public static void sendMail(String subject, String body) {
    sendMail(null, subject, body);
  }

  /**
   * Open the default Mail Application with the given Subject and Body for a new
   * Mail.
   *
   * @param address The Address of the new Mail or NULL if none.
   * @param subject The Subject of the new Mail or NULL if none.
   * @param body The Body of the new Mail or NULL if none.
   */
  public static void sendMail(String address, String subject, String body) {
    StringBuilder str = new StringBuilder();
    str.append("mailto:"); //$NON-NLS-1$
    str.append(address != null ? URIUtils.mailToUrllEncode(address) : ""); //$NON-NLS-1$
    str.append("?body="); //$NON-NLS-1$
    str.append(body != null ? URIUtils.mailToUrllEncode(body) : ""); //$NON-NLS-1$
    str.append("&subject="); //$NON-NLS-1$
    str.append(subject != null ? URIUtils.mailToUrllEncode(subject) : ""); //$NON-NLS-1$

    openLinkExternal(str.toString());
  }

  /**
   * Open the webbrowser on Linux or Solaris
   *
   * @param href An URL
   * @return Process The process that was executed
   */
  private static Process openWebBrowser(String href) {
    Process p = null;

    /* Try Netscape as default browser */
    if (webBrowser == null) {
      try {
        webBrowser = "netscape"; //$NON-NLS-1$
        p = Runtime.getRuntime().exec(webBrowser + "  " + href); //$NON-NLS-1$
      } catch (IOException e) {
        webBrowser = "mozilla"; //$NON-NLS-1$
      }
    }

    /* Try Mozilla as default browser */
    if (p == null) {
      try {
        p = Runtime.getRuntime().exec(webBrowser + " " + href); //$NON-NLS-1$
      } catch (IOException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
    return p;
  }

  /**
   * Use default browser to display the URL
   */
  private static void useDefaultBrowser(final String link) {

    /* Try Program-API first */
    if (Program.launch(link))
      return;

    /* Show Error Dialog on Windows */
    if (Application.IS_WINDOWS) {
      showErrorIfExternalBrowserFails();
    }

    /* Launch default browser on Mac */
    else if (Application.IS_MAC) {
      try {
        Process proc = Runtime.getRuntime().exec("/usr/bin/open " + link); //$NON-NLS-1$

        /* Let StreamGobbler handle error message */
        StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

        /* Let StreamGobbler handle output */
        StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());

        /* Flush both error and output streams */
        errorGobbler.schedule();
        outputGobbler.schedule();
      }

      /* Show error message, default browser could not be launched */
      catch (IOException e) {
        Activator.getDefault().logError(e.getMessage(), e);
        showErrorIfExternalBrowserFails();
      }
    }

    /* Launch default browser on Linux & Solaris */
    else {

      /* Run browser in a seperate thread */
      Thread launcher = new Thread("") { //$NON-NLS-1$
        @Override
        public void run() {
          try {

            /* Return early if shutting down */
            if (Controller.getDefault().isShuttingDown())
              return;

            /* The default browser was successfully launched once, use again */
            if (webBrowserSuccessfullyOpened) {
              Process proc = Runtime.getRuntime().exec(webBrowser + " -remote openURL(" + link + ")"); //$NON-NLS-1$ //$NON-NLS-2$

              /* Let StreamGobbler handle error message */
              StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

              /* Let StreamGobbler handle output */
              StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());

              /* Flush both error and output streams */
              errorGobbler.schedule();
              outputGobbler.schedule();
            }

            /* The default browser was not yet launched, try NS and Mozilla */
            else {
              Process proc = openWebBrowser(link);
              webBrowserSuccessfullyOpened = true;

              if (proc != null) {

                /* Let StreamGobbler handle error message */
                StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

                /* Let StreamGobbler handle output */
                StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());

                /* Flush both error and output streams */
                errorGobbler.schedule();
                outputGobbler.schedule();
              }

              /* Wait for this process */
              try {
                if (proc != null)
                  proc.waitFor();
              } catch (InterruptedException e) {
                Activator.safeLogError(e.getMessage(), e);
              } finally {
                webBrowserSuccessfullyOpened = false;
              }
            }
          }

          /* Show error, default browser could not be launched */
          catch (IOException e) {
            Activator.safeLogError(e.getMessage(), e);
            showErrorIfExternalBrowserFails();
          }
        }
      };
      launcher.setDaemon(true);
      launcher.start();
    }
  }

  private static void showErrorIfExternalBrowserFails() {
    final IWorkbenchWindow window = OwlUI.getWindow();
    if (window == null)
      return;

    JobRunner.runInUIThread(window.getShell(), new Runnable() {
      public void run() {
        MessageBox box = new MessageBox(window.getShell(), SWT.ICON_WARNING | SWT.OK | SWT.CANCEL);
        box.setText(Messages.BrowserUtils_ERROR_LAUNCH_BROWSER);
        box.setMessage(Messages.BrowserUtils_ERROR_LAUNCH_BROWSER_MSG);

        if (box.open() == SWT.OK)
          PreferencesUtil.createPreferenceDialogOn(window.getShell(), BrowserPreferencePage.ID, null, null).open();
      }
    });
  }

  private static void useCustomBrowser(final String link) {
    String browser = Owl.getPreferenceService().getGlobalScope().getString(DefaultPreferences.CUSTOM_BROWSER_PATH);
    final String executable = browser + " " + link; //$NON-NLS-1$

    /* Launch custom browser in seperate thread */
    Thread launcher = new Thread("") { //$NON-NLS-1$
      @Override
      public void run() {

        /* Return early if shutting down */
        if (Controller.getDefault().isShuttingDown())
          return;

        /* Execute custom browser */
        try {
          Process proc = Runtime.getRuntime().exec(executable);

          /* Let StreamGobbler handle error message */
          StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

          /* Let StreamGobbler handle output */
          StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());

          /* Flush both error and output streams */
          errorGobbler.schedule();
          outputGobbler.schedule();

          /* Wait for the process to terminate */
          proc.waitFor();
        } catch (IOException e) {
          Activator.safeLogError(e.getMessage(), e);

          /* Use default browser if custom browser is not working */
          useDefaultBrowser(link);

        } catch (InterruptedException e) {
          Activator.safeLogError(e.getMessage(), e);
        }
      }
    };
    launcher.setDaemon(true);
    launcher.start();
  }
}