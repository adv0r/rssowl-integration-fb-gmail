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
package org.rssowl.ui.internal.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.rssowl.core.connection.GmailAuthentication;
import org.rssowl.ui.internal.dialogs.GoogleCredentialDialog;

public class gmailButtonHandler extends AbstractHandler implements IHandler {

  public Object execute(ExecutionEvent event) {
    if (GmailAuthentication.getCURRENT_STATE() ==  GmailAuthentication.USER_NOT_AUTH ) {

      //Executed when the gmail Button ( toolbar ) is pressed
      GoogleCredentialDialog instance = GoogleCredentialDialog.getVisibleInstance();
      if (instance == null) {
        //First Opening
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        GoogleCredentialDialog dialog = new GoogleCredentialDialog(window.getShell());
        dialog.setBlockOnOpen(false);
        dialog.open();
      } else {
        //Late Opening
        instance.getShell().forceActive();
      }
    }
    else
    {
      IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
      MessageDialog.openInformation(window.getShell(), "Your feeds are already stored", "You have already retrieved your Gmail feed. " + //$NON-NLS-1$ //$NON-NLS-2$
          "\nIf you want to change the gmail account, you can delete the Gmail folder " + //$NON-NLS-1$
      "on the sidebar and press the button again."); //$NON-NLS-1$
    }

    return null;
  }

}
