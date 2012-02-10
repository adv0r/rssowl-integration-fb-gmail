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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.FacebookAuthenticationAction;
import org.rssowl.ui.internal.util.LayoutUtils;

public class FacebookCredentialDialog extends TitleAreaDialog {

  /* Keep the visible instance saved */
  private static FacebookCredentialDialog fVisibleInstance;

  public static FacebookCredentialDialog getVisibleInstance() {
    return fVisibleInstance;
  }
  /* Fields */
  private LocalResourceManager fResources;

  FacebookAuthenticationAction fAuthAction = null;

  public FacebookCredentialDialog(Shell parentShell) {
    super(parentShell);
    fAuthAction = new FacebookAuthenticationAction();
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  @Override
  protected void buttonPressed(int buttonId) {
    /* User pressed SAVE Button */
    if (buttonId == IDialogConstants.OK_ID) {
      fAuthAction.startAuthentication();
    }
    super.buttonPressed(buttonId);
  }

  @Override
  public boolean close() {
    fVisibleInstance = null;
    fResources.dispose();
    return super.close();
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(Messages.FacebookCredentialDialog_WINDOW_TITLE);
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Ok", true); //$NON-NLS-1$
    createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false); //$NON-NLS-1$
  }

  //Create the Dialog structure

  @Override
  protected Control createDialogArea(Composite parent) {

    /* Title */
    setTitle(Messages.FacebookCredentialDialog_TITLE);

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/login_wiz.png")); //$NON-NLS-1$

    /* Title Message */
    setMessage(Messages.FacebookCredentialDialog_MESSAGE);

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Composite to hold all components */
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(1, 5, 10));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    composite.setBackground(parent.getBackground());

    /* Label for username */
    Label userLabel = new Label(composite, SWT.WRAP);
    userLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    userLabel.setText(Messages.FacebookCredentialDialog_EXPLANATION);


    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Try loading Credentials from Platform if available */
    applyDialogFont(composite);
    return composite;
  }


  /*
   * @see org.eclipse.jface.window.Window#getShellStyle()
   */
  @Override
  protected int getShellStyle() {
    int style = SWT.TITLE | SWT.BORDER | SWT.CLOSE | getDefaultOrientation();
    return style;
  }

  @Override
  public int open() {
    fVisibleInstance = this;
    return super.open();
  }



}
