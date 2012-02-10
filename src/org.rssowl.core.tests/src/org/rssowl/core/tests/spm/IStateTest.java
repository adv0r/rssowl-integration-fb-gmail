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
package org.rssowl.core.tests.spm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.FacebookAuthentication;
import org.rssowl.core.connection.GmailAuthentication;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.connection.IState;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.util.SyncUtils;
import org.rssowl.ui.internal.Activator;

import java.net.URI;

public class IStateTest {
  private IModelFactory fFactory;
  private final URI fLink =  URI.create(SyncUtils.FB_AUTH_STATE);
  private final URI gLink =  URI.create(SyncUtils.GMAIL_AUTH_STATE);

  private ICredentialsProvider fCredProvider=Owl.getConnectionService().getCredentialsProvider(fLink);
  private ICredentialsProvider gCredProvider=Owl.getConnectionService().getCredentialsProvider(gLink);


  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    fFactory = Owl.getModelFactory();
  }


  @Test
  public void testFacebookAuthenticationState() throws Exception {
    final int initial_state = FacebookAuthentication.USER_NOT_AUTH;

    /* Try with FACEBOOK */
    IState credentials = new IState(Integer.toString(initial_state));

    try {
      if (fCredProvider != null) {
        fCredProvider.setAuthCredentials(credentials, fLink, null);
      }
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
      fail();
    }

    ICredentials fbAuthState = null;
    try {
      fbAuthState = fCredProvider.getAuthCredentials(fLink, null);
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
      fail();
    }

    if (fbAuthState != null) {
      int stateStored = Integer.parseInt(fbAuthState.getPassword());

      //Verify that initialState and stateStored are correct

      assertEquals(initial_state, stateStored);

      /*Change the state to newState */
      int newState = FacebookAuthentication.USER_AUTHENTICATED;
      FacebookAuthentication.setCURRENT_STATE(newState);
      assertFalse(initial_state==newState); //Verify that is different from the initial state

      //Retreive from the DB the current state

      int newStateStored = FacebookAuthentication.getCURRENT_STATE();
      assertEquals(newState, newStateStored);
    }

    else
       fail();

    /* Try with GMAIL */

    final int initial_state_g = GmailAuthentication.USER_NOT_AUTH;

    IState credentials_g = new IState(Integer.toString(initial_state));

    try {
      if (gCredProvider != null) {
        gCredProvider.setAuthCredentials(credentials_g, gLink, null);
      }
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
      fail();
    }

    ICredentials gAuthState = null;
    try {
      gAuthState = gCredProvider.getAuthCredentials(gLink, null);
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
      fail();
    }

    if (gAuthState != null) {
      int stateStored = Integer.parseInt(gAuthState.getPassword());

      //Verify that initialState and stateStored are correct

      assertEquals(initial_state_g, stateStored);

      /*Change the state to newState */
      int newState = GmailAuthentication.USER_AUTHENTICATED;
      GmailAuthentication.setCURRENT_STATE(newState);
      assertFalse(initial_state_g==newState); //Verify that is different from the initial state

      //Retreive from the DB the current state

      int newStateStored = GmailAuthentication.getCURRENT_STATE();
      assertEquals(newState, newStateStored);
    }

    else
       fail();

  }


}
