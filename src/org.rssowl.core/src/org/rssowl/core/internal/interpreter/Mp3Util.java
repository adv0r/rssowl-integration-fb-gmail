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
package org.rssowl.core.internal.interpreter;
/*Refers to AtomInterpreter.java:231 and RSSInterpreter.java:449 */

/* Will use the mp3 flash player http://flash-mp3-player.net/players/normal/generator/ */

/* HTML EXAMPLE
 *   <object type="application/x-shockwave-flash" data="http://flash-mp3-player.net/medias/player_mp3_mini.swf" width="200" height="20">
 *   <param name="movie" value="http://flash-mp3-player.net/medias/player_mp3_mini.swf" />
 *   <param name="bgcolor" value="#000000" />
 *   <param name="FlashVars" value="mp3=http%3A//flash-mp3-player.net/medias/another_world.mp3" />
 *   </object>                                  */


public class Mp3Util {
  public static final String SWF_FILENAME = "player_mp3_mini.swf"; //$NON-NLS-1$
  public static final String SWF_PATH = "http://flash-mp3-player.net/medias/"; //$NON-NLS-1$  under ui project root
  public final static String PATH_TO_SWF = SWF_PATH+SWF_FILENAME;

  /*Player configuration */
  public final static int WIDTH = 200;
  public final static int HEIGHT = 20;
  public final static String BGCOLOR ="#000000" ; //$NON-NLS-1$

  public final static String[] MP3_MIME_TYPES={"audio/mpeg3","audio/x-mpeg-3","audio/mpeg","mp3"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  public final static String ICON_URL = "http://www.lize.it/up/rssowl/mp3_icon.png"; //$NON-NLS-1$


  public static String getHeader()
  {
    return  "<br /> <hr />   <h2> <img src=\""+ICON_URL+"\" alt=\"Mp3 icon\" height=\"30\" width=\"30\"</img>Embedded mp3 player  </h2>  "; //$NON-NLS-1$ //$NON-NLS-2$
  }

  public static String getPlayerCode(String fileUrl)
  {
    String toReturn="<br /><hr /> <p>Play song : <u>"+fileUrl.substring(fileUrl.lastIndexOf("/")+1)+"</u></p>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

    toReturn+="<object type=\"application/x-shockwave-flash\" data=\""+PATH_TO_SWF+"\" width=\""+WIDTH+"\" height=\""+HEIGHT+"\">"+//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    "<param name=\"movie\" value=\""+PATH_TO_SWF+"\" />"+  //$NON-NLS-1$//$NON-NLS-2$
    "<param name=\"bgcolor\" value=\""+BGCOLOR+"\" />" +  //$NON-NLS-1$//$NON-NLS-2$
    "<param name=\"FlashVars\" value=\"mp3="+fileUrl+"\" />"+ //$NON-NLS-1$ //$NON-NLS-2$
    "</object> "+ //$NON-NLS-1$
    "<hr /> "; //$NON-NLS-1$

    return toReturn;
  }

  public static boolean isMp3(String type)
  {

    for(int i=0; i< Mp3Util.MP3_MIME_TYPES.length; i++)
    {
      if (type.equals(Mp3Util.MP3_MIME_TYPES[i]))
        return true;
    }
    return false;
  }
}
