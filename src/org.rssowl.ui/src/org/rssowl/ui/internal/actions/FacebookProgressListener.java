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

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.rssowl.core.connection.FacebookAuthentication;
import org.rssowl.core.util.FacebookKeyExtractor;
import org.rssowl.ui.internal.dialogs.ActivityDialog;
import org.rssowl.ui.internal.util.FacebookJob;

public class FacebookProgressListener implements ProgressListener{
  private Browser browser ;
  private FacebookAuthenticationAction action ;
  private int comingFrom = 0;
  private  FacebookJob job;
  org.rssowl.ui.internal.dialogs.ActivityDialog instance;

  public FacebookProgressListener(Browser browser,FacebookAuthenticationAction action){
    this.browser = browser;
    this.action = action;
  }
  public void changed(ProgressEvent event) {
  }

  public void completed(ProgressEvent event) {
    if (browser!=null)
    {
      String newUrl = browser.getUrl();

      if (newUrl.startsWith(FacebookAuthentication.AFTER_LOGIN_URL))         //After authentication
      {
        FacebookAuthentication.setCURRENT_STATE(FacebookAuthentication.USER_AUTHENTICATED);

        /*Start the Job with progress bar*/
        job = new FacebookJob("Getting the source of feeds") ; //$NON-NLS-1$
        job.schedule();

        /*Show the Download and Activity Dialog */
        instance = ActivityDialog.getVisibleInstance();
        if (instance == null) {
          ActivityDialog dialog = new ActivityDialog(browser.getShell());
          dialog.setBlockOnOpen(false);
          dialog.open();
        } else
          instance.getShell().forceActive();

        job.setState(job.JOB_LOOKING_UID_PASSWORD);
        browser.getParent().setEnabled(false); //Enable the browser
        comingFrom = 1;
        browser.setUrl(FacebookAuthentication.NOTIFICATIONS_URL);
      }

      else if(newUrl.equals(FacebookAuthentication.NOTIFICATIONS_URL)){
        if(comingFrom==1){
          browser.execute("window.status=document.getElementById('content').innerHTML;"); //$NON-NLS-1$
          String innerHTML = (String)browser.getData("query"); //$NON-NLS-1$
          FacebookAuthentication.setUID(FacebookKeyExtractor.getUID(innerHTML));
          FacebookAuthentication.setUSER_KEY(FacebookKeyExtractor.getUserKey(innerHTML));
          job.setState(job.JOB_LOOKING_FRIENDSKEY);
          comingFrom = 2;
          browser.setUrl(FacebookAuthentication.NOTES_URL);
        }
      }

      else if (newUrl.equals(FacebookAuthentication.NOTES_URL)){
        if(comingFrom==2){
          browser.execute("window.status=document.getElementById('leftCol').innerHTML;"); //$NON-NLS-1$
          String innerHTML = (String)browser.getData("query"); //$NON-NLS-1$
          FacebookAuthentication.setFRIENDS_KEY(FacebookKeyExtractor.getFriendsKey(innerHTML));
          job.setState(job.JOB_OVER);

          comingFrom = -1;
          if (FacebookAuthenticationAction.isFirstTime)
          {
            action.addFacebookFolder();
          }
        }
      }
      else
        comingFrom = -1;
    }
  }


}
