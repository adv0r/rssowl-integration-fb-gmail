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
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.util.SyncUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.util.GmailCredentials;

import java.net.URI;


public class GmailCredentialDialogTests {

  private IModelFactory fFactory;
  private String usernameCorrect="myUser";
  private String passwordCorrect="myPass";
  private String usernameWrong="myWrongUser";
  private String passwordWrong="myWrongPass";

  private final URI gLink =  URI.create(SyncUtils.GMAIL_LOGIN);
  private ICredentialsProvider fCredProvider=Owl.getConnectionService().getCredentialsProvider(gLink);

  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    fFactory = Owl.getModelFactory();
  }


  @Test
  public void testSaveCredential() throws Exception {
    final String username = usernameCorrect;
    final String password = passwordCorrect;

    GmailCredentials credentials = new GmailCredentials(username,password);

    try {
      if (fCredProvider != null) {
        fCredProvider.setAuthCredentials(credentials, gLink, null);
      }
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }

    ICredentials authCredentials = null;
    try {
        authCredentials = fCredProvider.getAuthCredentials(gLink, null);
    } catch (CredentialsException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }

    if (authCredentials != null) {
      String usernameStored = authCredentials.getUsername();
      String passwordStored = authCredentials.getPassword();

      //Verify that usernameWrong and usernameCorrect are different
      assertFalse(usernameCorrect.equals(usernameWrong));
      //Verify that passwordWrong and passwordCorrect are different
      assertFalse(passwordCorrect.equals(passwordWrong));

      //Verify that the stored credentials are corrects
      assertEquals(usernameStored, usernameCorrect);
      assertEquals(passwordStored, passwordCorrect);

      assertFalse(usernameStored.equals(usernameWrong));
      assertFalse(passwordStored.equals(passwordWrong));

    }

    else  {
       fail();
    }

  }

  }
