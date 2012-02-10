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

import org.junit.Test;
import org.rssowl.core.util.FacebookLikeUtil;

public class FacebookLikeButtonTest {
  @Test
  public void testFacebookButton() throws Exception {
String url = "http://www.ccc.co";
    String originalHTML = "<div id='fb-root'><script src='http://connect.facebook.net/en_US/all.js'></script>" +
    		"<script>  FB.init({  appId  : '149553541754308',  status : true, // check login status  " +
    		"cookie : true, // enable cookies to allow the server to access the session  " +
    		"xfbml  : true  // parse XFBML  });FB.login(function(response) " +
    		"{if (response.session) {} else { } });" +
    		"</script></div> <hr />" +
    		" <iframe src='http://www.facebook.com/plugins/like.php?href=http://www.ccc.co&amp;" +
    		"layout=standard&amp;show_faces=true&amp;" +
    		"width=600&amp;action=like&amp;" +
    		"font=verdana&amp;colorscheme=light&amp;height=80'" +
    		" scrolling='no' frameborder='0' style='border:none; " +
    		"overflow:hidden; width:600px; height:80px;' " +
    		"allowTransparency='true'>" +
    		"</iframe> ";

    String generatedHTML = FacebookLikeUtil.getButtonCode(url);
    assertEquals(originalHTML,generatedHTML);
  }
}
