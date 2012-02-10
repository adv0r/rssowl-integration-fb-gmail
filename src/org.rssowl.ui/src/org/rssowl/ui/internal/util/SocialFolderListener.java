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
package org.rssowl.ui.internal.util;

import org.rssowl.core.connection.FacebookAuthentication;
import org.rssowl.core.connection.GmailAuthentication;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.event.FolderAdapter;
import org.rssowl.core.persist.event.FolderEvent;

import java.util.Set;

public class SocialFolderListener extends FolderAdapter {
  private IFolder socialFolder;
  private boolean isFacebook;

  public SocialFolderListener(IFolder socialFolder,boolean isFacebook) {
    this.isFacebook=isFacebook;
    this.socialFolder=socialFolder;
  }

  @Override
  public void entitiesDeleted(Set<FolderEvent> events) {
    for (FolderEvent event : events) {
      IFolder folder = event.getEntity();
      if (folder.equals(socialFolder))
        if(isFacebook)
          FacebookAuthentication.setCURRENT_STATE(FacebookAuthentication.USER_NOT_AUTH);
        else
          GmailAuthentication.setCURRENT_STATE(GmailAuthentication.USER_NOT_AUTH);
    }
  }

}
