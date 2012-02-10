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

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.FacebookAuthentication;
import org.rssowl.core.connection.GmailAuthentication;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.util.SyncUtils;
import org.rssowl.ui.internal.util.SocialFolderListener;

import java.net.URI;

public class FolderDeletionTests {
  private IModelFactory fFactory;

  private final URI gLink =  URI.create(SyncUtils.GMAIL_AUTH_STATE);
  private ICredentialsProvider gCredProvider=Owl.getConnectionService().getCredentialsProvider(gLink);
  private final URI fLink =  URI.create(SyncUtils.FB_AUTH_STATE);
  private ICredentialsProvider fCredProvider=Owl.getConnectionService().getCredentialsProvider(fLink);


  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    fFactory = Owl.getModelFactory();
  }


  @Test
  public void testSaveCredential() throws Exception {

    /*____________________ FACEBOOK ___________________________*/

    /* Create the folder */
    IFolder fFolder = fFactory.createFolder(null, null, "Facebook Folder");
    DynamicDAO.save(fFolder);

    /* Set the status to 'completed' */
    FacebookAuthentication.setCURRENT_STATE(FacebookAuthentication.USER_DONE);

    /* Create listener */
    SocialFolderListener fListener = new SocialFolderListener(fFolder,true);
    DynamicDAO.addEntityListener(IFolder.class, fListener);

    /* Erase folder */
    DynamicDAO.delete(fFolder);

    /* Check states */

    assertFalse(FacebookAuthentication.USER_DONE==FacebookAuthentication.USER_NOT_AUTH);
    assertEquals(FacebookAuthentication.getCURRENT_STATE(),FacebookAuthentication.USER_NOT_AUTH);



    /*____________________ GMAIL ______________________________*/

    /* Create the folder */
    IFolder gFolder = fFactory.createFolder(null, null, "Gmail Folder");
    DynamicDAO.save(gFolder);

    /* Set the status to 'completed' */
    GmailAuthentication.setCURRENT_STATE(GmailAuthentication.USER_DONE);

    /* Create listener */
    SocialFolderListener gListener = new SocialFolderListener(gFolder,false);
    DynamicDAO.addEntityListener(IFolder.class, gListener);

    /* Erase folder */
    DynamicDAO.delete(gFolder);

    /* Check states */

    assertFalse(GmailAuthentication.USER_DONE==GmailAuthentication.USER_NOT_AUTH);
    assertEquals(GmailAuthentication.getCURRENT_STATE(),GmailAuthentication.USER_NOT_AUTH);

  }

}
