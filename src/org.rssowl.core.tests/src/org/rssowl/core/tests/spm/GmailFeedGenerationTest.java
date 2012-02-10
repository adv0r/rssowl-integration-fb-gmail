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
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.GmailAuthentication;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.util.SyncUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.util.GmailCredentials;

import java.net.URI;

public class GmailFeedGenerationTest {
  private IModelFactory fFactory;
  private final URI gLink =  URI.create(SyncUtils.GMAIL_LOGIN);
  private ICredentialsProvider gCredProvider=Owl.getConnectionService().getCredentialsProvider(gLink);
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    fFactory = Owl.getModelFactory();
  }


  @Test
  public void testGmailFeedGeneration() throws Exception {
    String usernameCorrect = "my_user.name";
    String passwordCorrect = "myp4ssw0rd";

    String feedCorrect = "https://"+usernameCorrect+":"+passwordCorrect+"@gmail.google.com/gmail/feed/atom";


    GmailCredentials credentials = new GmailCredentials(usernameCorrect,passwordCorrect);

    try {
      if (gCredProvider != null) {
        gCredProvider.setAuthCredentials(credentials, gLink, null);
      }
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }

    ICredentials authCredentials = null;
    try {
        authCredentials = gCredProvider.getAuthCredentials(gLink, null);
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }

    if (authCredentials != null) {
      String usernameStored = authCredentials.getUsername();
      String passwordStored = authCredentials.getPassword();

    assertEquals(usernameCorrect,usernameStored);
    assertEquals(passwordCorrect,passwordStored);
    assertEquals(feedCorrect,GmailAuthentication.getFeedURL());
    }
    else fail();

  }
}
