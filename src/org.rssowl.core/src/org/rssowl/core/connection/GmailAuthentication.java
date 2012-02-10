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

public class GmailAuthentication {
  /* Credential providers */
  private static final URI gmailLink =  URI.create(SyncUtils.GMAIL_LOGIN);
  private static ICredentialsProvider gmailCredProvider=Owl.getConnectionService().getCredentialsProvider(gmailLink);


  /* State providers */
  private static final URI gstateLink =  URI.create(SyncUtils.GMAIL_AUTH_STATE);
  private static ICredentialsProvider gstateCredProvider=Owl.getConnectionService().getCredentialsProvider(gstateLink);


  /* Authentication States */
  public static final int USER_NOT_AUTH = 10;  //The user is not Authenticated
  public static final int USER_GETTING_FEEDS_IN_PROGRESS = 11;  //The user is Authenticated and the software getting feeds sources
  public static final int USER_AUTHENTICATED = 12; //The user is Authenticated
  public static final int USER_DONE = 13; //The user is Authenticated and the feeds are retreived

  private static int CURRENT_STATE  = USER_NOT_AUTH; // TODO take it from file getCURRENT_STATE();

  public static final String GMAIL_FEED_TITLE = "Gmail feed"; //$NON-NLS-1$

  public static int getCURRENT_STATE() {
    int toReturn = USER_NOT_AUTH;
    ICredentials gmailState = null;
    try {
      if (gstateCredProvider != null)
        gmailState = gstateCredProvider.getAuthCredentials(gstateLink, null);
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }

    if (gmailState != null) {
      String StoredState = gmailState.getPassword();
      toReturn = Integer.parseInt(StoredState);
    }
    return toReturn;

  }


  public static String getFeedURL(){
    // i.e.https://USERNAME:PASSWORD@gmail.google.com/gmail/feed/atom

    String URLToReturn =  "https://" ; //$NON-NLS-1$
    ICredentials gmailCredential;
    try {
      gmailCredential = gmailCredProvider.getAuthCredentials(gmailLink, null);
      URLToReturn+=gmailCredential.getUsername()+":"; //$NON-NLS-1$
      URLToReturn+=gmailCredential.getPassword()+"@gmail.google.com/gmail/feed/atom"; //$NON-NLS-1$
    } catch (CredentialsException e) {
      e.getMessage();
    }

    return URLToReturn;
  }

  private static boolean isAuthenticated(){
    //TODO do
    return true;
  }

  public static void setCURRENT_STATE(int current_state) {
    CURRENT_STATE = current_state;

    IState gmailState = new IState(Integer.toString(current_state));
    try {
      if (gstateCredProvider != null) {
        gstateCredProvider.setAuthCredentials(gmailState, gstateLink, null);
      }
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }
  }

}
