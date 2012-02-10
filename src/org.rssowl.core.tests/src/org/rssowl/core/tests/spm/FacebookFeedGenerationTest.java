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
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.FacebookAuthentication;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.util.FacebookKeyExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FacebookFeedGenerationTest {

  private IModelFactory fFactory;
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    fFactory = Owl.getModelFactory();
  }


  @Test
  public void testFacebookFeedGeneration() throws Exception {
    String StatusFeedCorrect = "http://www.new.facebook.com/feeds/friends_status.php?id=100000015167804&key=6e6d796286&format=rss20";
    String NotificationFeedCorrect = "http://www.new.facebook.com/feeds/notifications.php?id=100000015167804&viewer=100000015167804&key=48d49d2fb7&format=rss20";
    String LinksFeedCorrect = "http://www.new.facebook.com/feeds/share_friends_posts.php?id=100000015167804&key=6e6d796286&format=rss20";
    String NotesFeedCorrect = "http://www.new.facebook.com/feeds/friends_notes.php?id=100000015167804&key=6e6d796286&format=rss20";
    String Correct_id="100000015167804";
    String Correct_mykey="48d49d2fb7";
    String Correct_friends_key="6e6d796286";

    String notes_path ="data/importer/notes_test.html";
    String notifications_path ="data/importer/notification_test.html";

    String innerHTML_notes ;
    String innerHTML_notifications ;

    /* Read from file */
    File file1 = new File(notes_path);
    StringBuffer contents_1 = new StringBuffer();
    BufferedReader reader_1 = null;

    File file2 = new File(notifications_path);
    StringBuffer contents_2 = new StringBuffer();
    BufferedReader reader_2 = null;

    try
    {
        reader_1 = new BufferedReader(new FileReader(file1));
        reader_2 = new BufferedReader(new FileReader(file2));
        String text1 = null;
        String text2 = null;

        while ((text1 = reader_1.readLine()) != null)
        {
            contents_1.append(text1)
                .append(System.getProperty(
                    "line.separator"));
        }

        while ((text2 = reader_2.readLine()) != null)
        {
            contents_2.append(text2)
                .append(System.getProperty(
                    "line.separator"));
        }

    } catch (FileNotFoundException e)
    {
        e.printStackTrace();
        fail();
    } catch (IOException e)
    {
        e.printStackTrace();
        fail();
    } finally
    {
        try
        {
            if (reader_1 != null)
                reader_1.close();
            if (reader_2 != null)
                reader_2.close();
        } catch (IOException e)
        {
            fail();
        }
    }

    innerHTML_notes = contents_1.toString();
    innerHTML_notifications = contents_2.toString();

    /* Extract user ID, my key, friends key */
    FacebookAuthentication.setUID(FacebookKeyExtractor.getUID(innerHTML_notifications));
    FacebookAuthentication.setUSER_KEY(FacebookKeyExtractor.getUserKey(innerHTML_notifications));
    FacebookAuthentication.setFRIENDS_KEY(FacebookKeyExtractor.getFriendsKey(innerHTML_notes));

    assertEquals(Correct_friends_key, FacebookAuthentication.getFRIENDS_KEY());
    assertEquals(Correct_id, FacebookAuthentication.getUID());
    assertEquals(Correct_mykey, FacebookAuthentication.getUSER_KEY());


    /* Generate Feed URLs */
    String statusFeedUrl =  FacebookAuthentication.getFeedURL( FacebookAuthentication.STATUS_UPDATE_FEED);
    String notificationFeedUrl =FacebookAuthentication.getFeedURL( FacebookAuthentication.NOTIFICATIONS_FEED);
    String notesFeedUrl =FacebookAuthentication.getFeedURL(FacebookAuthentication.NOTES_FEED);
    String linksFeedUrl = FacebookAuthentication.getFeedURL(FacebookAuthentication.LINKS_FEED);


    /* Assert */
    assertEquals(statusFeedUrl,StatusFeedCorrect);
    assertEquals(notificationFeedUrl,NotificationFeedCorrect);
    assertEquals(notesFeedUrl,NotesFeedCorrect);
    assertEquals(linksFeedUrl,LinksFeedCorrect);



  }
}
