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
package org.rssowl.core.connection;

import org.rssowl.core.Owl;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.util.SyncUtils;

import java.net.URI;



public class FacebookAuthentication {

  /* ID of the Facebook Application */
  public static final String APPLICATION_ID = Messages.FacebookAuthentication_0;

  public static final String AFTER_LOGIN_URL = "http://www.facebook.com/connect/login_success.html"; //$NON-NLS-1$
  public static final String NOTIFICATIONS_URL = "http://www.facebook.com/notifications.php"; //$NON-NLS-1$
  public static final String NOTES_URL = "http://www.facebook.com/notes.php"; //$NON-NLS-1$
  /* URL of facebook web site*/
  private static final String FACEBOOK_URL = Messages.FacebookAuthentication_3;

  /* URL of process complete web page*/
  private static final String COMPLETED_URL = Messages.FacebookAuthentication_18;

  /* URL of facebook web site new*/
  private static final String FACEBOOK_URL_NEW = Messages.FacebookAuthentication_5;

  /* URL of facebook Open Graph*/
  private static final String FACEBOOK_URL_GRAPH =Messages.FacebookAuthentication_11;

  /* URL of redirect after logging in */
  private static final String FACEBOOK_URL_REDIRECT = FACEBOOK_URL + Messages.FacebookAuthentication_12;

  /* Base URL for generic Feeds*/
  private static final String FEED_PATH = FACEBOOK_URL_NEW+Messages.FacebookAuthentication_6;

  /* Notifications Feed */
  private static final String MY_NOTIFICATION_PATH =  FEED_PATH + Messages.FacebookAuthentication_7;

  /* Friends Status Feed*/
  private static final String FRIENDS_STATUS_UPDATE_PATH = FEED_PATH +  Messages.FacebookAuthentication_8;

  /* Friends Links Feed */
  private static final String FRIENDS_LINKS_PATH =  FEED_PATH + Messages.FacebookAuthentication_9;

  /* Friends Links Feed */
  private static final String FRIENDS_NOTES_PATH =  FEED_PATH + Messages.FacebookAuthentication_13;

  private static final String FEED_TYPE = Messages.FacebookAuthentication_10;

  /* Different kinds of feed */
  public static final int NOTIFICATIONS_FEED = 1;

  public static final int STATUS_UPDATE_FEED = 2;
  public static final int LINKS_FEED = 3;
  public static final int NOTES_FEED = 4;
  /* Feeds Titles */
  public static final String NOTIFICATIONS_FEED_TITLE = Messages.FacebookAuthentication_14;

  public static final String NOTES_FEED_TITLE = Messages.FacebookAuthentication_15;
  public static final String LINKS_FEED_TITLE = Messages.FacebookAuthentication_16;
  public static final String STATUS_FEED_TITLE = Messages.FacebookAuthentication_17;

  /* Authentication States */
  public static final int USER_NOT_AUTH = 10;  //The user is not Authenticated
  public static final int USER_GETTING_FEEDS_IN_PROGRESS = 11;  //The user is Authenticated and the software getting feeds sources
  public static final int USER_AUTHENTICATED = 12; //The user is Authenticated
  public static final int USER_DONE = 13; //The user is Authenticated and the feeds are retreived
  private static final URI fLink =  URI.create(SyncUtils.FB_AUTH_STATE);
  private static ICredentialsProvider fCredProvider=Owl.getConnectionService().getCredentialsProvider(fLink);

  public static IState Ifs ;
  private static String UID;

  private static String USER_KEY;
  private static String FRIENDS_KEY;
  private static int CURRENT_STATE  = USER_NOT_AUTH;
  public static String getAuthenticationURL(){
    String DisplayType = "popup"; //$NON-NLS-1$
    String Type = "user_agent"; //$NON-NLS-1$
    String URLToReturn = FACEBOOK_URL_GRAPH +
    "oauth/authorize?client_id="+APPLICATION_ID+ //$NON-NLS-1$
    "&redirect_uri="+ FACEBOOK_URL_REDIRECT+ //$NON-NLS-1$
    "&type="+Type+ //$NON-NLS-1$
    "&display="+DisplayType; //$NON-NLS-1$

    return URLToReturn;

  }

  public static String getCOMPLETED_URL() {
    return COMPLETED_URL;
  }
  public static int getCURRENT_STATE() {
    int toReturn = USER_NOT_AUTH;
    ICredentials facebookState = null;
    try {
      if (fCredProvider != null)
        facebookState = fCredProvider.getAuthCredentials(fLink, null);
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }

    if (facebookState != null) {
      String StoredState = facebookState.getPassword();
      toReturn = Integer.parseInt(StoredState);
    }
    return toReturn;

  }

  public static String getFeedURL(int feedType){
    String URLToReturn = FEED_PATH;
    switch(feedType){
      case NOTIFICATIONS_FEED :
        //  i.e. http://www.new.facebook.com/feeds/notifications.php?id=your_facebook_id&viewer=your_facebook_id&key=your_internal_key&format=rss20
        URLToReturn = MY_NOTIFICATION_PATH + "id="+getUID()+"&viewer="+getUID()+"&key="+getUSER_KEY();  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
        break;

      case STATUS_UPDATE_FEED :
        // i.e  http://www.new.facebook.com/feeds/friends_status.php?id=your_facebook_id&key=your_friends_key&format=rss20
        URLToReturn = FRIENDS_STATUS_UPDATE_PATH + "id="+getUID()+"&key="+getFRIENDS_KEY(); //$NON-NLS-1$ //$NON-NLS-2$
        break;

      case LINKS_FEED :
        // i.e. http://www.new.facebook.com/feeds/share_friends_posts.php?id=your_facebook_id&key=your_friend_key&format=rss20
        URLToReturn = FRIENDS_LINKS_PATH + "id="+getUID()+"&key="+getFRIENDS_KEY(); //$NON-NLS-1$ //$NON-NLS-2$
        break;
      case NOTES_FEED :
        // i.e.http://www.facebook.com/feeds/friends_notes.php?id=525510878&key=0d16f973e1&format=rss20
        URLToReturn = FRIENDS_NOTES_PATH + "id="+getUID()+"&key="+getFRIENDS_KEY(); //$NON-NLS-1$ //$NON-NLS-2$
        break;

      default:
        break;
    }
    URLToReturn+= "&format="+FEED_TYPE; //$NON-NLS-1$
    return URLToReturn;
  }

  public static String getFRIENDS_KEY() {
    return FRIENDS_KEY;
  }
  public static String getUID() {
    return UID;
  }
  public static String getUSER_KEY() {
    return USER_KEY;
  }

  public static void setCURRENT_STATE(int current_state) {
    CURRENT_STATE = current_state;


    IState facebookState = new IState(Integer.toString(current_state));
    try {
      if (fCredProvider != null) {
        fCredProvider.setAuthCredentials(facebookState, fLink, null);
      }
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }
  }


  public static void setFRIENDS_KEY(String friends_key) {
    FRIENDS_KEY = friends_key;
  }

  public static void setUID(String uid) {
    UID = uid;
  }

  public static void setUSER_KEY(String user_key) {
    USER_KEY = user_key;
  }

  public FacebookAuthentication(){
  }

}
