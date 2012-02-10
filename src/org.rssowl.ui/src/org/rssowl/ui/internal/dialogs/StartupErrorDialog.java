/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2010 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.BrowserUtils;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * A Dialog that is shown to the user in case RSSOwl failed to start.
 *
 * @author bpasero
 */
public class StartupErrorDialog extends TitleAreaDialog {
  private LocalResourceManager fResources;
  private final IStatus fErrorStatus;

  /**
   * @param errorStatus
   */
  public StartupErrorDialog(IStatus errorStatus) {
    super(null);
    fErrorStatus = errorStatus;
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /*
   * @see org.eclipse.jface.dialogs.TrayDialog#close()
   */
  @Override
  public boolean close() {
    fResources.dispose();
    return super.close();
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    newShell.setText(Messages.StartupErrorDialog_RSSOWL_CRASH_REPORTER);
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {

    /* Composite to hold all components */
    Composite composite = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(2, 5, 10, 15, 5, false));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Title */
    setTitle(Messages.StartupErrorDialog_WE_SORRY);

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/welcome_wiz.gif")); //$NON-NLS-1$

    /* Title Message */
    setMessage(Messages.StartupErrorDialog_RSSOWL_CRASHED, IMessageProvider.WARNING);

    /* Crash Report Label */
    Link dialogMessageLabel = new Link(composite, SWT.WRAP);
    dialogMessageLabel.setText(Messages.StartupErrorDialog_CRASH_DIAGNOSE);
    dialogMessageLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    dialogMessageLabel.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if ("save".equals(e.text)) //$NON-NLS-1$
          doSaveErrorLog();
        else
          doSendErrorLog();
      }
    });

    /* Recovery Label */
    Link recoveryMessageLabel = new Link(composite, SWT.WRAP);
    recoveryMessageLabel.setText(Messages.StartupErrorDialog_CRASH_ADVISE);
    recoveryMessageLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    recoveryMessageLabel.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if ("faq".equals(e.text)) //$NON-NLS-1$
          doOpenFAQ();
        else if ("forum".equals(e.text)) //$NON-NLS-1$
          doOpenForum();
      }
    });

    /* Error Details Label */
    if (fErrorStatus != null && StringUtils.isSet(fErrorStatus.getMessage())) {
      Label reasonLabel = new Label(composite, SWT.NONE);
      reasonLabel.setText(Messages.StartupErrorDialog_ERROR_DETAILS);
      reasonLabel.setFont(OwlUI.getBold(JFaceResources.DIALOG_FONT));
      reasonLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

      Label errorDetailsLabel = new Label(composite, SWT.WRAP);
      errorDetailsLabel.setText(fErrorStatus.getMessage());
      errorDetailsLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    }

    /* Holder for the separator to the OK and Cancel buttons */
    Composite sepHolder = new Composite(parent, SWT.NONE);
    sepHolder.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    sepHolder.setLayout(LayoutUtils.createGridLayout(1, 0, 0));

    /* Separator */
    Label separator = new Label(sepHolder, SWT.SEPARATOR | SWT.HORIZONTAL);
    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    applyDialogFont(composite);

    return composite;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
   */
  @Override
  protected void buttonPressed(int buttonId) {
    setReturnCode(buttonId);
    close();
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    if (!Application.IS_MAC && !Application.IS_LINUX) { //Somehow a restart fails on Linux/Mac if invoked early on startup
      createButton(parent, IDialogConstants.RETRY_ID, Messages.StartupErrorDialog_RESTART_RSSOWL, true);
      createButton(parent, IDialogConstants.CLOSE_ID, Messages.StartupErrorDialog_QUIT_RSSOWL, false);
      getButton(IDialogConstants.RETRY_ID).setFocus();
    } else {
      createButton(parent, IDialogConstants.CLOSE_ID, Messages.StartupErrorDialog_QUIT_RSSOWL, true);
      getButton(IDialogConstants.CLOSE_ID).setFocus();
    }
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
   */
  @Override
  protected void initializeBounds() {
    super.initializeBounds();
    Point bestSize = getShell().computeSize(convertHorizontalDLUsToPixels(OwlUI.MIN_DIALOG_WIDTH_DLU), SWT.DEFAULT);
    Point location = getInitialLocation(bestSize);
    getShell().setBounds(location.x, location.y, bestSize.x, bestSize.y);
  }

  private void doSaveErrorLog() {
    FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
    dialog.setText(Messages.StartupErrorDialog_SAVE_CRASH_REPORT);
    dialog.setFilterExtensions(new String[] { "*.log" }); //$NON-NLS-1$
    dialog.setFileName("rssowl.log"); //$NON-NLS-1$
    dialog.setOverwrite(true);

    String file = dialog.open();
    if (StringUtils.isSet(file)) {
      try {

        /* Check for Log Message from Core to have a complete log */
        String logMessages = CoreUtils.getAndFlushLogMessages();
        if (logMessages != null && logMessages.length() > 0)
          Activator.safeLogError(logMessages, null);

        /* Help to find out where the log is coming from */
        Activator.safeLogInfo("Crash Report Exported"); //$NON-NLS-1$

        /* Export Log File */
        File logFile = Platform.getLogFileLocation().toFile();
        InputStream inS;
        if (logFile.exists())
          inS = new FileInputStream(logFile);
        else
          inS = new ByteArrayInputStream(new byte[0]);
        FileOutputStream outS = new FileOutputStream(new File(file));
        CoreUtils.copy(inS, outS);
      } catch (FileNotFoundException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  private void doSendErrorLog() {
    String address = "crash-report@rssowl.org"; //$NON-NLS-1$
    String subject = NLS.bind("RSSOwl Crash Report ({0})", CoreUtils.getUserAgent()); //$NON-NLS-1$
    String body = Messages.StartupErrorDialog_ATTACH_CRASH_REPORT;

    BrowserUtils.sendMail(address, subject, body);
  }

  private void doOpenFAQ() {
    BrowserUtils.openLinkExternal("http://www.rssowl.org/help#item_6"); //$NON-NLS-1$
  }

  private void doOpenForum() {
    BrowserUtils.openLinkExternal("http://sourceforge.net/projects/rssowl/forums/forum/296910"); //$NON-NLS-1$
  }
}