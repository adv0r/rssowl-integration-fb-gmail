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

package org.rssowl.core.connection;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.URIUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The default implementation of the ICredentialsProvider retrieves
 * authentication Credentials from the Equinox Security Storage.
 *
 * @author bpasero
 */
@SuppressWarnings("restriction")
public class PlatformCredentialsProvider implements ICredentialsProvider {

  /* Node for feed related security preferences */
  private static final String SECURE_FEED_NODE = "rssowl/feeds"; //$NON-NLS-1$

  /* File with credentials stored */
  private static final String SECURE_STORAGE_FILE = ".credentials"; //$NON-NLS-1$

  /* ID of the Win32 dependent password provider (win32) */
  private static final String WIN_PW_PROVIDER_ID = "org.eclipse.equinox.security.WindowsPasswordProvider"; //$NON-NLS-1$

  /* ID of the RSSOwl password provider (Dialog asking for Master Password) */
  private static final String RSSOWL_PW_PROVIDER_ID = "org.rssowl.ui.RSSOwlPasswordProvider"; //$NON-NLS-1$

  /* ID of the MacOS dependent password provider */
  private static final String MACOS_PW_PROVIDER_ID = "org.eclipse.equinox.security.OSXKeystoreIntegration"; //$NON-NLS-1$

  /* Unique Key to store Usernames */
  private static final String USERNAME = "org.rssowl.core.connection.auth.Username"; //$NON-NLS-1$

  /* Unique Key to store Passwords */
  private static final String PASSWORD = "org.rssowl.core.connection.auth.Password"; //$NON-NLS-1$

  /* Unique Key to store Domains */
  private static final String DOMAIN = "org.rssowl.core.connection.auth.Domain"; //$NON-NLS-1$

  /* Default Realm being used to store credentials */
  private static final String REALM = ""; //$NON-NLS-1$

  /* A cache of non-protected Links (in the form Link + Realm) */
  private final Set<String> fUnprotectedLinksCache = Collections.synchronizedSet(new HashSet<String>());

  /* Simple POJO Implementation of ICredentials */
  private static class Credentials implements ICredentials {
    private String fUsername;
    private String fPassword;
    private String fDomain;

    Credentials(String username, String password, String domain) {
      fUsername = username;
      fPassword = password;
      fDomain = domain;
    }

    public String getUsername() {
      return fUsername;
    }

    public String getPassword() {
      return fPassword;
    }

    public String getDomain() {
      return fDomain;
    }
  }

  /*
   * @see
   * org.rssowl.core.connection.ICredentialsProvider#getAuthCredentials(java
   * .net.URI, java.lang.String)
   */
  public synchronized ICredentials getAuthCredentials(URI link, String realm) throws CredentialsException {

    /* Check Cache first */
    if (checkCacheProtected(link.toString(), realm))
      return null;

    /* Retrieve Credentials */
    ICredentials authorizationInfo = getAuthorizationInfo(link, realm);

    /* Credentials Provided */
    if (authorizationInfo != null)
      return authorizationInfo;

    /* Cache as unprotected */
    addCacheProtected(link.toString(), realm);

    /* Credentials not provided */
    return null;
  }

  @SuppressWarnings("deprecation")
  private ISecurePreferences getSecurePreferences() {
    if (!InternalOwl.IS_ECLIPSE) {
      IPreferenceScope prefs = Owl.getPreferenceService().getGlobalScope();
      boolean useOSPasswordProvider = prefs.getBoolean(DefaultPreferences.USE_OS_PASSWORD);

      /* Disable OS Password if Master Password shall be used */
      if (prefs.getBoolean(DefaultPreferences.USE_MASTER_PASSWORD))
        useOSPasswordProvider = false;

      /* Try storing credentials in profile folder */
      try {
        Activator activator = Activator.getDefault();

        /* Check if Bundle is Stopped */
        if (activator == null)
          return null;

        IPath stateLocation = activator.getStateLocation();
        stateLocation = stateLocation.append(SECURE_STORAGE_FILE);
        URL location = stateLocation.toFile().toURL();
        Map<String, String> options = null;

        /* Use OS dependent password provider if available */
        if (useOSPasswordProvider) {
          if (Platform.OS_WIN32.equals(Platform.getOS())) {
            options = new HashMap<String, String>();
            options.put(IProviderHints.REQUIRED_MODULE_ID, WIN_PW_PROVIDER_ID);
          } else if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            options = new HashMap<String, String>();
            options.put(IProviderHints.REQUIRED_MODULE_ID, MACOS_PW_PROVIDER_ID);
          }
        }

        /* Use RSSOwl password provider */
        else {
          options = new HashMap<String, String>();
          options.put(IProviderHints.REQUIRED_MODULE_ID, RSSOWL_PW_PROVIDER_ID);
        }

        return SecurePreferencesFactory.open(location, options);
      } catch (MalformedURLException e) {
        Activator.safeLogError(e.getMessage(), e);
      } catch (IllegalStateException e1) {
        Activator.safeLogError(e1.getMessage(), e1);
      } catch (IOException e2) {
        Activator.safeLogError(e2.getMessage(), e2);
      }
    }

    /* Fallback to default location */
    return SecurePreferencesFactory.getDefault();
  }

  private ICredentials getAuthorizationInfo(URI link, String realm) throws CredentialsException {
    ISecurePreferences securePreferences = getSecurePreferences();

    /* Check if Bundle is Stopped */
    if (securePreferences == null)
      return null;

    /* Return from Equinox Security Storage */
    if (securePreferences.nodeExists(SECURE_FEED_NODE)) { // Global Feed Node
      ISecurePreferences allFeedsPreferences = securePreferences.node(SECURE_FEED_NODE);
      if (allFeedsPreferences.nodeExists(EncodingUtils.encodeSlashes(link.toString()))) { // Feed Node
        ISecurePreferences feedPreferences = allFeedsPreferences.node(EncodingUtils.encodeSlashes(link.toString()));
        if (feedPreferences.nodeExists(EncodingUtils.encodeSlashes(realm != null ? realm : REALM))) { // Realm Node
          ISecurePreferences realmPreferences = feedPreferences.node(EncodingUtils.encodeSlashes(realm != null ? realm : REALM));

          try {
            String username = realmPreferences.get(USERNAME, null);
            String password = realmPreferences.get(PASSWORD, null);
            String domain = realmPreferences.get(DOMAIN, null);

            if (username != null && password != null)
              return new Credentials(username, password, domain);
          } catch (StorageException e) {
            throw new CredentialsException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
          }
        }
      }
    }

    return null;
  }

  /*
   * @see
   * org.rssowl.core.connection.auth.ICredentialsProvider#getProxyCredentials
   * (java.net.URI)
   */
  public IProxyCredentials getProxyCredentials(URI link) {
    Activator activator = Activator.getDefault();

    /* Check if Bundle is Stopped */
    if (activator == null)
      return null;

    IProxyService proxyService = activator.getProxyService();

    /* Check if Proxy is enabled */
    if (!proxyService.isProxiesEnabled())
      return null;

    String host = URIUtils.safeGetHost(link);
    boolean isSSL = "https".equals(link.getScheme()); //$NON-NLS-1$

    /* Retrieve Proxy Data */
    final IProxyData proxyData = proxyService.getProxyDataForHost(host, isSSL ? IProxyData.HTTPS_PROXY_TYPE : IProxyData.HTTP_PROXY_TYPE);
    if (proxyData != null) {
      return new IProxyCredentials() {
        public String getHost() {
          return proxyData.getHost();
        }

        public int getPort() {
          return proxyData.getPort();
        }

        public String getDomain() {
          return null;
        }

        public String getPassword() {
          return proxyData.getPassword();
        }

        public String getUsername() {
          return proxyData.getUserId();
        }
      };
    }

    /* Feed does not require Proxy or Credentials not supplied */
    return null;
  }

  /*
   * @see
   * org.rssowl.core.connection.ICredentialsProvider#setAuthCredentials(org.
   * rssowl.core.connection.ICredentials, java.net.URI, java.lang.String)
   */
  public void setAuthCredentials(ICredentials credentials, URI link, String realm) throws CredentialsException {
    ISecurePreferences securePreferences = getSecurePreferences();

    /* Check if Bundle is Stopped */
    if (securePreferences == null)
      return;

    /* Store in Equinox Security Storage */
    ISecurePreferences allFeedsPreferences = securePreferences.node(SECURE_FEED_NODE);
    ISecurePreferences feedPreferences = allFeedsPreferences.node(EncodingUtils.encodeSlashes(link.toString()));
    ISecurePreferences realmPreference = feedPreferences.node(EncodingUtils.encodeSlashes(realm != null ? realm : REALM));

    IPreferenceScope globalScope = Owl.getPreferenceService().getGlobalScope();

    /* OS Password is only supported on Windows and Mac */
    boolean useOSPassword = globalScope.getBoolean(DefaultPreferences.USE_OS_PASSWORD);
    if (!Platform.OS_WIN32.equals(Platform.getOS()) && !Platform.OS_MACOSX.equals(Platform.getOS()))
      useOSPassword = false;

    boolean encryptPW = useOSPassword || globalScope.getBoolean(DefaultPreferences.USE_MASTER_PASSWORD);
    try {
      if (credentials.getUsername() != null)
        realmPreference.put(USERNAME, credentials.getUsername(), encryptPW);

      if (credentials.getPassword() != null)
        realmPreference.put(PASSWORD, credentials.getPassword(), encryptPW);

      if (credentials.getDomain() != null)
        realmPreference.put(DOMAIN, credentials.getDomain(), encryptPW);

      realmPreference.flush(); // Flush to disk early
    } catch (StorageException e) {
      throw new CredentialsException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    } catch (IOException e) {
      throw new CredentialsException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }

    /* Uncache */
    removeCacheProtected(link.toString(), realm);
  }

  /*
   * @see
   * org.rssowl.core.connection.auth.ICredentialsProvider#setProxyCredentials
   * (org.rssowl.core.connection.auth.IProxyCredentials, java.net.URI)
   */
  public void setProxyCredentials(IProxyCredentials credentials, URI link) {
    IProxyService proxyService = Activator.getDefault().getProxyService();
    proxyService.setProxiesEnabled(true);
    boolean isSSL = "https".equals(link.getScheme()); //$NON-NLS-1$

    /* Retrieve Proxy Data */
    final IProxyData proxyData = proxyService.getProxyData(isSSL ? IProxyData.HTTPS_PROXY_TYPE : IProxyData.HTTP_PROXY_TYPE);
    if (proxyData != null) { //TODO What if Data is NULL?
      proxyData.setHost(credentials.getHost());
      proxyData.setPort(credentials.getPort());
      proxyData.setUserid(credentials.getUsername());
      proxyData.setPassword(credentials.getPassword());
    }
  }

  /*
   * @see
   * org.rssowl.core.connection.ICredentialsProvider#deleteAuthCredentials(java
   * .net.URI, java.lang.String)
   */
  public synchronized void deleteAuthCredentials(URI link, String realm) throws CredentialsException {
    ISecurePreferences securePreferences = getSecurePreferences();

    /* Check if Bundle is Stopped */
    if (securePreferences == null)
      return;

    /* Remove from Equinox Security Storage */
    if (securePreferences.nodeExists(SECURE_FEED_NODE)) { // Global Feed Node
      ISecurePreferences allFeedsPreferences = securePreferences.node(SECURE_FEED_NODE);
      if (allFeedsPreferences.nodeExists(EncodingUtils.encodeSlashes(link.toString()))) { // Feed Node
        ISecurePreferences feedPreferences = allFeedsPreferences.node(EncodingUtils.encodeSlashes(link.toString()));
        if (feedPreferences.nodeExists(EncodingUtils.encodeSlashes(realm != null ? realm : REALM))) { // Realm Node
          ISecurePreferences realmPreferences = feedPreferences.node(EncodingUtils.encodeSlashes(realm != null ? realm : REALM));
          realmPreferences.clear();
          realmPreferences.removeNode();
          try {
            feedPreferences.flush();
          } catch (IOException e) {
            throw new CredentialsException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
          }
        }
      }
    }

    /* Delete from Cache */
    removeCacheProtected(link.toString(), realm);
  }

  /*
   * @see
   * org.rssowl.core.connection.auth.ICredentialsProvider#deleteProxyCredentials
   * (java.net.URI)
   */
  public void deleteProxyCredentials(URI link) {
    IProxyService proxyService = Activator.getDefault().getProxyService();
    proxyService.setProxiesEnabled(false);
    //TODO System Properties are still set?
  }

  /**
   * An internal method only available for the
   * {@link PlatformCredentialsProvider} to clear all secure preferences nodes.
   * This method is called e.g. when the master password is to be changed or
   * disabled.
   */
  public void clear() {

    /* Clear cached info */
    InternalExchangeUtils.passwordProvidersReset();

    /* Remove all Nodes */
    ISecurePreferences secureRoot = getSecurePreferences();

    /* Check if Bundle is Stopped */
    if (secureRoot == null)
      return;

    String[] childrenNames = secureRoot.childrenNames();
    for (String child : childrenNames) {
      secureRoot.node(child).removeNode();
    }

    /* Flush to Disk */
    try {
      secureRoot.flush();
    } catch (IOException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }
  }

  private boolean checkCacheProtected(String link, String realm) {
    return fUnprotectedLinksCache.contains(toCacheKey(link, realm));
  }

  private void addCacheProtected(String link, String realm) {
    String cacheKey = toCacheKey(link, realm);
    if (!fUnprotectedLinksCache.contains(cacheKey))
      fUnprotectedLinksCache.add(cacheKey);
  }

  private void removeCacheProtected(String link, String realm) {
    fUnprotectedLinksCache.remove(toCacheKey(link, realm));
  }

  private String toCacheKey(String link, String realm) {
    if (realm == null)
      realm = REALM;

    return link + realm;
  }
}