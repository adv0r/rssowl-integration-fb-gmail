/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
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

package org.rssowl.ui.internal.editors.browser;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.browser.IWebBrowser;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.util.BrowserUtils;

import java.net.URI;
import java.net.URL;

/**
 * The embedded web browser used from {@link WebBrowserSupport} in RSSOwl.
 *
 * @author bpasero
 */
public class EmbeddedWebBrowser implements IWebBrowser {
  private final String fBrowserId;
  private WebBrowserContext fContext;

  /**
   * @param browserId
   */
  public EmbeddedWebBrowser(String browserId) {
    this(browserId, null);
  }

  /**
   * @param browserId
   * @param context
   */
  public EmbeddedWebBrowser(String browserId, WebBrowserContext context) {
    fBrowserId = browserId;
    fContext = context;
  }

  /**
   * @param context the context from which this browser was created or
   * <code>null</code> if none.
   */
  public void setContext(WebBrowserContext context) {
    fContext = context;
  }

  /*
   * @see org.eclipse.ui.browser.IWebBrowser#openURL(java.net.URL)
   */
  public void openURL(URL url) {
    Assert.isNotNull(url);

    /* Open externally */
    if (useExternalBrowser())
      openExternal(url);

    /* Open internally */
    else
      BrowserUtils.openLinkInternal(url.toExternalForm(), fContext);
  }

  /*
   * @see org.eclipse.ui.browser.IWebBrowser#openURL(java.net.URL)
   */
  public void openURL(URI uri) {
    Assert.isNotNull(uri);

    /* Open externally */
    if (useExternalBrowser())
      openExternal(uri);

    /* Open internally */
    else
      BrowserUtils.openLinkInternal(uri.toString(), fContext);
  }

  private boolean useExternalBrowser() {
    IPreferenceScope globalScope = Owl.getPreferenceService().getGlobalScope();
    return globalScope.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER) || globalScope.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER);
  }

  private void openExternal(URL url) {
    BrowserUtils.openLinkExternal(url.toExternalForm());
  }

  private void openExternal(URI uri) {
    BrowserUtils.openLinkExternal(uri.toString());
  }

  /*
   * @see org.eclipse.ui.browser.IWebBrowser#close()
   */
  public boolean close() {
    return true;
  }

  /*
   * @see org.eclipse.ui.browser.IWebBrowser#getId()
   */
  public String getId() {
    return fBrowserId;
  }
}