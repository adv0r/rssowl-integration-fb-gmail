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


public class FacebookKeyExtractor {


  public static String getFriendsKey(String innerHTML){
    String toReturn = null;
    String firstBound ="feeds/friends_notes.php?"; //$NON-NLS-1$
    String secondBound ="key="; //$NON-NLS-1$
    int indexOfHref = innerHTML.indexOf(firstBound);
    toReturn = innerHTML.substring(indexOfHref,indexOfHref+120);
    toReturn = toReturn.substring(toReturn.indexOf(secondBound)+secondBound.length());
    toReturn = toReturn.substring(0, toReturn.indexOf("&amp")); //$NON-NLS-1$
    return toReturn;
  }


  public static String getUID(String innerHTML){
    String toReturn = null;
    String firstBound ="feeds/notifications.php?"; //$NON-NLS-1$
    String secondBound = "id="; //$NON-NLS-1$
    int indexOfHref = innerHTML.indexOf(firstBound);
    toReturn = innerHTML.substring(indexOfHref,indexOfHref+120);
    toReturn = toReturn.substring(toReturn.indexOf(secondBound)+secondBound.length(),toReturn.indexOf("&amp")); //$NON-NLS-1$
    return toReturn;
  }

  public static String getUserKey(String innerHTML){
    String toReturn = null;
    String firstBound ="feeds/notifications.php?"; //$NON-NLS-1$
    String secondBound ="key="; //$NON-NLS-1$
    int indexOfHref = innerHTML.indexOf(firstBound);
    toReturn = innerHTML.substring(indexOfHref,indexOfHref+120);
    toReturn = toReturn.substring(toReturn.indexOf(secondBound)+secondBound.length());
    toReturn = toReturn.substring(0, toReturn.indexOf("&amp")); //$NON-NLS-1$
    return toReturn;
  }
}
