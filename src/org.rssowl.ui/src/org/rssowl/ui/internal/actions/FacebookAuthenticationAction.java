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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.StatusTextListener;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.FacebookAuthentication;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.dialogs.FacebookCredentialDialog;
import org.rssowl.ui.internal.editors.browser.WebBrowserContext;
import org.rssowl.ui.internal.editors.browser.WebBrowserView;
import org.rssowl.ui.internal.util.BrowserUtils;
import org.rssowl.ui.internal.util.SocialFolderListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@SuppressWarnings("restriction")
public class FacebookAuthenticationAction{
  /* Browser */
  private Browser browser = null;
  private IModelFactory fTypesFactory;
  public static boolean isFirstTime = true;
  private  WebBrowserView Wbv;

  /* Listeners */
  FacebookProgressListener progressListener = null;
  StatusTextListener statusTextListener = null;

  public FacebookAuthenticationAction(){
  }

  public boolean addFacebookFolder(){
    isFirstTime=false;

    /* Generate Feed URLs */
    String statusFeedUrl =  FacebookAuthentication.getFeedURL(FacebookAuthentication.STATUS_UPDATE_FEED);
    String notificationFeedUrl =FacebookAuthentication.getFeedURL( FacebookAuthentication.NOTIFICATIONS_FEED);
    String notesFeedUrl =FacebookAuthentication.getFeedURL(FacebookAuthentication.NOTES_FEED);
    String linksFeedUrl = FacebookAuthentication.getFeedURL(FacebookAuthentication.LINKS_FEED);


    /* TODO replace '&amp;' with '/&amp;'*/
    String message = "\n__________________________________________\n\n Here are your feeds... Keep them for you!"+ //$NON-NLS-1$
    "\n\nStatus Feed URL : "+ statusFeedUrl + //$NON-NLS-1$
    "\nNotification Feed URL : "+ notificationFeedUrl + //$NON-NLS-1$
    "\nNotes Feed URL : "+ notesFeedUrl + //$NON-NLS-1$
    "\nLinks Feed URL : "+ linksFeedUrl ; //$NON-NLS-1$

    // Activator.safeLogInfo(message);
    if (MessageDialog.openConfirm(browser.getShell(), "Process Complete!", "The Owl managed to computer the URLs of your feeds! Do you want to add them to reader ?\n"+message))  //$NON-NLS-1$ //$NON-NLS-2$
    {
      OwlUI.getPage().closeEditor(Wbv,false); /* Close the browser tab*/
      createFeeds(statusFeedUrl,notificationFeedUrl,notesFeedUrl,linksFeedUrl);

      /* Update all the feeds */
      new ReloadAllAction().run();

      isFirstTime=true;
      FacebookAuthentication.setCURRENT_STATE(FacebookAuthentication.USER_DONE);
      return true;
    }
    FacebookAuthentication.setCURRENT_STATE(FacebookAuthentication.USER_NOT_AUTH);
    OwlUI.getPage().closeEditor(Wbv,false);
    isFirstTime=true;
    return false;
  }

  public void createFeeds(String statusFeedUrl,String notificationFeedUrl, String notesFeedUrl,String linksFeedUrl) {
    URI statusFeedURI=null;
    URI notificationFeedURI=null ;
    URI notesFeedURI=null;
    URI linksFeedURI=null;

    try {
      statusFeedURI = new URI(statusFeedUrl);
      notificationFeedURI = new URI(notificationFeedUrl);
      notesFeedURI = new URI(notesFeedUrl);
      linksFeedURI = new URI(linksFeedUrl);
    } catch (URISyntaxException e1) {
      e1.printStackTrace();
    }

    /* Create the folder under the root */
    fTypesFactory = Owl.getModelFactory();
    Set<IFolder> setIFolder = CoreUtils.loadRootFolders();
    IFolder parent= setIFolder.iterator().next();
    String folderName = "Facebook Feeds"; //$NON-NLS-1$
    final IFolder facebookFolder = Owl.getModelFactory().createFolder(null, parent, folderName, null,null);

    /* Add the deleted folder listener */

    SocialFolderListener listener = new SocialFolderListener(facebookFolder,true);
    DynamicDAO.addEntityListener(IFolder.class, listener);

    /*Create IFeeds*/
    IFeed notificationFeed = new Feed(notificationFeedURI);
    IFeed statusFeed =  new Feed(statusFeedURI);
    IFeed linkFeed =  new Feed(linksFeedURI);
    IFeed noteFeed = new Feed(notesFeedURI);

    /* Create the IBookMark */
    FeedLinkReference notificationFeedReference =  new FeedLinkReference(notificationFeedURI);
    FeedLinkReference statusFeedReference =  new FeedLinkReference(statusFeedURI);
    FeedLinkReference linkFeedReference =  new FeedLinkReference(linksFeedURI);
    FeedLinkReference noteFeedReference =  new FeedLinkReference(notesFeedURI);

    IBookMark notificationFeedBookmark= Owl.getModelFactory().createBookMark(null, facebookFolder, notificationFeedReference, FacebookAuthentication.NOTIFICATIONS_FEED_TITLE);
    IBookMark statusFeedBookmark= Owl.getModelFactory().createBookMark(null, facebookFolder, statusFeedReference, FacebookAuthentication.STATUS_FEED_TITLE);
    IBookMark linkFeedBookmark= Owl.getModelFactory().createBookMark(null, facebookFolder, linkFeedReference, FacebookAuthentication.LINKS_FEED_TITLE);
    IBookMark noteFeedBookmark= Owl.getModelFactory().createBookMark(null, facebookFolder,noteFeedReference, FacebookAuthentication.NOTES_FEED_TITLE);

    /*Save everything in the UI */

    DynamicDAO.save(parent);

    notificationFeed = DynamicDAO.save(notificationFeed);
    statusFeed =  DynamicDAO.save(statusFeed);
    linkFeed=  DynamicDAO.save(linkFeed);
    noteFeed = DynamicDAO.save(noteFeed);

    DynamicDAO.save(notificationFeedBookmark);
    DynamicDAO.save(statusFeedBookmark);
    DynamicDAO.save(linkFeedBookmark);
    DynamicDAO.save(noteFeedBookmark);
  }


  public boolean startAuthentication(){
    /*Close the Dialog*/
    if (FacebookCredentialDialog.getVisibleInstance()!=null)
      FacebookCredentialDialog.getVisibleInstance().close();

    /* Change the state */
    FacebookAuthentication.setCURRENT_STATE(FacebookAuthentication.USER_GETTING_FEEDS_IN_PROGRESS);

    /*Open a new broser page and let the user get Authenticated */

    WebBrowserContext Wbc = WebBrowserContext.createFrom("Loading Procedure ..."); //$NON-NLS-1$.
    Wbv = BrowserUtils.openLinkInternal(FacebookAuthentication.getAuthenticationURL(),Wbc);

    browser =  Wbv.getBrowser().getControl();
    progressListener = new FacebookProgressListener(browser,this);
    statusTextListener = new FacebookStatusTextListener(browser);

    browser.addProgressListener(progressListener);
    browser.addStatusTextListener(statusTextListener);
    return true;
  }

}
