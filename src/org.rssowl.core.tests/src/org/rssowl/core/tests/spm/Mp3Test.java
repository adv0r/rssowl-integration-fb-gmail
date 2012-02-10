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
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.interpreter.Mp3Util;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;

import java.io.InputStream;
import java.net.URI;

public class Mp3Test {
  @Test

  public void testMp3() throws Exception {


    /*Testing the MIME Types*/
    assertTrue(Mp3Util.isMp3("audio/x-mpeg-3"));
    assertTrue(Mp3Util.isMp3("audio/mpeg3"));
    assertTrue(Mp3Util.isMp3("audio/mpeg"));
    assertTrue(Mp3Util.isMp3("mp3"));

    String mp3UrlCorrect = "http://cdn03.cdn.gorillavsbear.net/wp-content/uploads/2010/12/01-Holiday-Face.mp3" ;

    String mp3CodeCorrect ="<br /><hr /> <p>Play song : <u>"+mp3UrlCorrect.substring(mp3UrlCorrect.lastIndexOf("/")+1)+"</u></p>"+
    "<object type=\"application/x-shockwave-flash\" data=\""+Mp3Util.PATH_TO_SWF+"\" width=\""+Mp3Util.WIDTH+"\" height=\""+Mp3Util.HEIGHT+"\">"+
    "<param name=\"movie\" value=\""+Mp3Util.PATH_TO_SWF+"\" />"+
    "<param name=\"bgcolor\" value=\""+Mp3Util.BGCOLOR+"\" />" +
    "<param name=\"FlashVars\" value=\"mp3="+mp3UrlCorrect+"\" />"+
    "</object> "+
    "<hr /> ";

    String mp3CodeGenerated = Mp3Util.getPlayerCode(mp3UrlCorrect);

    assertEquals(mp3CodeCorrect,mp3CodeGenerated);


    //With default preferences  ( MP3 player on )

    /* Atom */
    InputStream inS1 = getClass().getResourceAsStream("/data/interpreter/feed_atom.xml");
    IFeed feed1 = new Feed(new URI("http://www.data.interpreter.feed_atom.xml"));
    Owl.getInterpreter().interpret(inS1, feed1, null);
    INews newsAtom_withenclosure = feed1.getNews().get(0);
    INews newsAtom_noenclosure = feed1.getNews().get(1);

    assertTrue(newsAtom_withenclosure.isNewsWithMp3());
    assertFalse(newsAtom_noenclosure.isNewsWithMp3());


    /* RSS */
    InputStream inS2 = getClass().getResourceAsStream("/data/interpreter/enclosure.rss");
    IFeed feed2 = new Feed(new URI("http://www.data.interpreter.feed_rss.xml"));
    Owl.getInterpreter().interpret(inS2, feed2, null);
    INews newsRss_withenclosure = feed2.getNews().get(0);
    INews newsRss_noenclosure = feed2.getNews().get(1);

    assertTrue(newsRss_withenclosure.isNewsWithMp3());
    assertFalse(newsRss_noenclosure.isNewsWithMp3());


    //After changing preferences

    Owl.getPreferenceService().getGlobalScope().putBoolean(DefaultPreferences.HIDE_MP3, true);
    /* Atom */
    InputStream inS3 = getClass().getResourceAsStream("/data/interpreter/feed_atom.xml");
    IFeed feed3 = new Feed(new URI("http://www.data.interpreter.feed_atom.xml"));
    Owl.getInterpreter().interpret(inS3, feed3, null);
    INews newsAtom_withenclosure3 = feed3.getNews().get(0);

    assertTrue(!newsAtom_withenclosure3.isNewsWithMp3());

    /* RSS */
    InputStream inS4 = getClass().getResourceAsStream("/data/interpreter/enclosure.rss");
    IFeed feed4 = new Feed(new URI("http://www.data.interpreter.feed_rss.xml"));
    Owl.getInterpreter().interpret(inS4, feed4, null);
    INews newsRss_withenclosure4 = feed4.getNews().get(0);

    assertTrue(!newsRss_withenclosure4.isNewsWithMp3());


  }
}
