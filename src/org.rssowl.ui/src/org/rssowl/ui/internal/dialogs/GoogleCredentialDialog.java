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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.SyncUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.GmailAuthenticationAction;
import org.rssowl.ui.internal.util.GmailCredentials;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.net.URI;

public class GoogleCredentialDialog extends TitleAreaDialog {

  /* Keep the visible instance saved */
  private static GoogleCredentialDialog fVisibleInstance;

  public static GoogleCredentialDialog getVisibleInstance() {
    return fVisibleInstance;
  }
  /* Fields */
  private LocalResourceManager fResources;
  private Text usernameField;
  private Text passwordField;
  private final URI gLink =  URI.create(SyncUtils.GMAIL_LOGIN);

  private ICredentialsProvider gCredProvider=Owl.getConnectionService().getCredentialsProvider(gLink);


  public GoogleCredentialDialog(Shell parentShell) {
    super(parentShell);
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  @Override
  protected void buttonPressed(int buttonId) {
    /* User pressed SAVE Button */
    if (buttonId == IDialogConstants.OK_ID) {
      final String username = usernameField.getText();
      final String password = passwordField.getText();
      if (!username.equals("") && !password.equals("")){  //$NON-NLS-1$//$NON-NLS-2$
        GmailCredentials credentials = new GmailCredentials(username,password);
        try {
          if (gCredProvider != null) {
            gCredProvider.setAuthCredentials(credentials, gLink, null);
          }
        } catch (CredentialsException e) {
          Activator.getDefault().getLog().log(e.getStatus());
        }
        GmailAuthenticationAction gAuthAction = new GmailAuthenticationAction();
        gAuthAction.addGmailFolder();
      }
      else
      {
        MessageDialog.openError(this.getParentShell(), "You must enter valid credentials", "Empty strings are not allowed"); //$NON-NLS-1$ //$NON-NLS-2$
      }
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
    shell.setText(Messages.GoogleCredentialDialog_WINDOW_TITLE);
  }

  //Create the Dialog structure

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Save", true); //$NON-NLS-1$
    createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false); //$NON-NLS-1$
  }

  @Override
  protected Control createDialogArea(Composite parent) {

    /* Title */
    setTitle(Messages.GoogleCredentialDialog_TITLE);

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/login_wiz.png")); //$NON-NLS-1$

    /* Title Message */
    setMessage(Messages.GoogleCredentialDialog_MESSAGE);

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Composite to hold all components */
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(3, 5, 10));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    /* Label for username */
    Label userLabel = new Label(composite, SWT.WRAP);
    userLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    userLabel.setText(Messages.GoogleCredentialDialog_USERNAME);

    /* Field for username */
    Composite textIndent = new Composite(composite, SWT.NONE);
    textIndent.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    textIndent.setLayout(new GridLayout(1, false));


    /* Label for username */
    Label userSuggestLabel = new Label(composite, SWT.WRAP);
    userSuggestLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    userSuggestLabel.setText("@gmail.com"); //$NON-NLS-1$



    usernameField = new Text(textIndent, SWT.BORDER);
    usernameField.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    usernameField.setFocus();

    /* Label for password */
    Label passwordLabel = new Label(composite, SWT.WRAP);
    passwordLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    passwordLabel.setText(Messages.GoogleCredentialDialog_PASSWORD);


    /* Field for password */
    Composite textIndent2 = new Composite(composite, SWT.NONE);
    textIndent2.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    textIndent2.setLayout(new GridLayout(1, false));

    passwordField = new Text(textIndent2, SWT.BORDER | SWT.PASSWORD );
    passwordField.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Try loading Credentials from Platform if available */
    preload();
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

  private void preload() {
    ICredentials authCredentials = null;
    try {
      if (gCredProvider != null)
        authCredentials = gCredProvider.getAuthCredentials(gLink, null);
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }

    if (authCredentials != null) {
      String username = authCredentials.getUsername();
      String password = authCredentials.getPassword();

      if (StringUtils.isSet(username)) {
        usernameField.setText(username);
        usernameField.selectAll();
      }

      if (StringUtils.isSet(password))
        passwordField.setText(password);
    }
  }

}
