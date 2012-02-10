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

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.StatusTextListener;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.FacebookAuthentication;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.ui.internal.actions.FacebookAuthenticationAction;
import org.rssowl.ui.internal.actions.FacebookProgressListener;
import org.rssowl.ui.internal.actions.FacebookStatusTextListener;
import org.rssowl.ui.internal.editors.browser.WebBrowserContext;
import org.rssowl.ui.internal.editors.browser.WebBrowserView;
import org.rssowl.ui.internal.util.BrowserUtils;
import org.rssowl.ui.internal.util.CBrowser;

public class FacebookBrowserTest {

  ProgressListener progressListener ;
  StatusTextListener statusTextListener ;
  Browser browser;


  @Test
  public void testFacebookBrowser() throws Exception {

  }


  private IModelFactory fFactory;
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    fFactory = Owl.getModelFactory();

    WebBrowserContext Wbc = WebBrowserContext.createFrom("Loading Procedure ..."); //$NON-NLS-1$.
    WebBrowserView Wbv = BrowserUtils.openLinkInternal(FacebookAuthentication.getAuthenticationURL(),Wbc);


    CBrowser cb = Wbv.getBrowser();
    browser = cb.getControl();
    progressListener = new FacebookProgressListener(browser,new FacebookAuthenticationAction());
    statusTextListener = new FacebookStatusTextListener(browser);
    browser.addProgressListener(progressListener);
    browser.addStatusTextListener(statusTextListener);
  }
}
