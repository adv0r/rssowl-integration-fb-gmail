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
package org.rssowl.ui.internal.actions;

import org.rssowl.core.Owl;
import org.rssowl.core.connection.GmailAuthentication;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.util.SocialFolderListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class GmailAuthenticationAction {

  private IModelFactory fTypesFactory;

  public boolean addGmailFolder(){

    /* Generate Feed URLs */
    String gmailFeedUrl =  GmailAuthentication.getFeedURL();

    //String message = "\n Gmail Feed URL : =  "+ gmailFeedUrl ;//$NON-NLS-1$

    createFeed(gmailFeedUrl);

    /* Update all the feeds */
    new ReloadAllAction().run();

    GmailAuthentication.setCURRENT_STATE(GmailAuthentication.USER_DONE);
    return true;

  }

  @SuppressWarnings("restriction")
  public void createFeed(String gmailFeedUrl) {
    Activator.safeLogInfo(gmailFeedUrl);
    URI gmailFeedURI=null;

    try {
      gmailFeedURI = new URI(gmailFeedUrl);
    } catch (URISyntaxException e1) {
      e1.printStackTrace();
    }

    /* Create the folder under the root */
    fTypesFactory = Owl.getModelFactory();
    Set<IFolder> setIFolder = CoreUtils.loadRootFolders();
    IFolder parent= setIFolder.iterator().next();
    String folderName = "Gmail Feeds"; //$NON-NLS-1$
    final IFolder gmailFolder = Owl.getModelFactory().createFolder(null, parent, folderName, null,null);

    /* Add the deleted folder listener */
    SocialFolderListener listener = new SocialFolderListener(gmailFolder,false);
    DynamicDAO.addEntityListener(IFolder.class, listener);

    /*Create IFeeds*/
    IFeed gmailFeed = new Feed(gmailFeedURI);

    /* Create the IBookMark */
    FeedLinkReference gmailFeedReference =  new FeedLinkReference(gmailFeedURI);
    IBookMark gmailFeedBookmark= Owl.getModelFactory().createBookMark(null,gmailFolder, gmailFeedReference,GmailAuthentication.GMAIL_FEED_TITLE);

    /*Save everything in the UI */
    DynamicDAO.save(parent);
    gmailFeed = DynamicDAO.save(gmailFeed);
    DynamicDAO.save(gmailFeedBookmark);

  }

}
