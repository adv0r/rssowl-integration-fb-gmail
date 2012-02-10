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

package org.rssowl.core.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.internal.Activator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Some tools to synchronize with online services like Google Reader.
 *
 * @author bpasero
 */
public class SyncUtils {

  /** Google Client Login Site */
  public static final String GOOGLE_LOGIN = "https://www.google.com/accounts/ClientLogin"; //$NON-NLS-1$

  /** FB Client Login Site */
  public static final String FB_LOGIN = "https://www.facebook.com/"; //$NON-NLS-1$

  /** FB Authentication STATE */
  public static final String FB_AUTH_STATE = "https://www.fbstate.com/"; //$NON-NLS-1$


  /** Gmail Client Login Site */
  public static final String GMAIL_LOGIN = "https://gmail.google.com/"; //$NON-NLS-1$

  /** Gmail Authentication  STATE */
  public static final String GMAIL_AUTH_STATE= "https://www.google.it/"; //$NON-NLS-1$


  /* Google Auth Identifier */
  private static final String AUTH_IDENTIFIER = "Auth="; //$NON-NLS-1$

  /* Google Auth Header */
  private static final String GOOGLE_LOGIN_HEADER_VALUE = "GoogleLogin auth="; //$NON-NLS-1$

  /**
   * @param authToken the authorization token that can be obtained from
   * {@link SyncUtils#getGoogleAuthToken(String, String, IProgressMonitor)}
   * @return a header value that can be used inside <code>Authorization</code>
   * to get access to Google Services.
   */
  public static String getGoogleAuthorizationHeader(String authToken) {
    return GOOGLE_LOGIN_HEADER_VALUE + authToken;
  }

  /**
   * Obtains the Google Auth Token to perform REST operations for Google
   * Services.
   *
   * @param email the user account for google
   * @param pw the password for the user account
   * @param monitor an instance of {@link IProgressMonitor} that can be used to
   * cancel the operation and report progress.
   * @return the google Auth Token for the given account or <code>null</code> if
   * none.
   * @throws ConnectionException Checked Exception to be used in case of any
   * Exception.
   */
  public static String getGoogleAuthToken(String email, String pw, IProgressMonitor monitor) throws ConnectionException {
    try {
      URI uri = new URI(GOOGLE_LOGIN);
      IProtocolHandler handler = Owl.getConnectionService().getHandler(uri);
      if (handler != null) {

        /* Google Specific Headers */
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("accountType", "GOOGLE"); //$NON-NLS-1$ //$NON-NLS-2$
        headers.put("Email", email); //$NON-NLS-1$
        headers.put("Passwd", pw); //$NON-NLS-1$
        headers.put("service", "reader"); //$NON-NLS-1$ //$NON-NLS-2$
        headers.put("source", "RSSOwl.org-RSSOwl-" + Activator.getDefault().getVersion()); //$NON-NLS-1$ //$NON-NLS-2$

        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties.put(IConnectionPropertyConstants.HEADERS, headers);
        properties.put(IConnectionPropertyConstants.POST, Boolean.TRUE);

        BufferedReader reader = null;
        try {
          InputStream inS = handler.openStream(uri, monitor, properties);
          reader = new BufferedReader(new InputStreamReader(inS));
          String line;
          while (!monitor.isCanceled() && (line = reader.readLine()) != null) {
            if (line.startsWith(AUTH_IDENTIFIER))
              return line.substring(AUTH_IDENTIFIER.length());
          }
        } finally {
          if (reader != null)
            reader.close();
        }
      }
    } catch (URISyntaxException e) {
      throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    } catch (IOException e) {
      throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }

    return null;
  }
}