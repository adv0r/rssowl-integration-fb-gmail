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

package org.rssowl.core.tests.interpreter;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.interpreter.UnknownFormatException;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.util.DateUtils;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * This TestCase covers use-cases for the Interpreter Plugin.
 *
 * @author bpasero
 */
public class InterpreterTest {

  /**
   * Test an Atom Feed.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testAtom() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_atom.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_atom.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("Atom 1.0", feed.getFormat());
    assertEquals("atom_title", feed.getTitle());
    assertEquals("atom_subtitle", feed.getDescription());
    assertEquals(new URI("atom_link_html"), feed.getHomepage());
    assertEquals("atom_generator", feed.getGenerator());
    assertEquals("en-us", feed.getLanguage());
    assertEquals(new URI("http://www.feed-uri.de"), feed.getBase());

    assertNotNull(feed.getImage());
    assertEquals(new URI("atom_logo"), feed.getImage().getLink());

    assertNotNull(feed.getLastModifiedDate());

    assertEquals(1, feed.getCategories().size());
    assertEquals("atom_category.label", feed.getCategories().get(0).getName());
    assertEquals("atom_category.term", feed.getCategories().get(0).getDomain());
    assertEquals("atom_copyright", feed.getCopyright());

    assertNotNull(feed.getAuthor());
    assertEquals("atom_author.name", feed.getAuthor().getName());
    assertEquals(new URI("atom_author.uri"), feed.getAuthor().getUri());
    assertEquals(new URI("atom_author.email"), feed.getAuthor().getEmail());

    assertEquals(6, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("atom_entry1.title", news1.getTitle());
   // assertEquals("atom_entry1.description", news1.getDescription());
    assertEquals(new URI("http://www.entry-uri.de"), news1.getBase());

    assertNotNull(news1.getCategories());
    assertEquals("atom_entry1.category.label", news1.getCategories().get(0).getName());
    assertEquals("atom_entry1.category.term", news1.getCategories().get(0).getDomain());

    assertNotNull(news1.getGuid());
    assertEquals("atom_entry1.id", news1.getGuid().getValue());

    assertEquals(new URI("atom_entry1.link.href"), news1.getLink());

    assertEquals(2, news1.getAttachments().size());
    assertEquals(new URI("atom_entry1.enclosure1.href"), news1.getAttachments().get(0).getLink());
    assertEquals("mp3", news1.getAttachments().get(0).getType());
    assertEquals(4500000, news1.getAttachments().get(0).getLength());
    assertEquals(new URI("atom_entry1.enclosure2.href"), news1.getAttachments().get(1).getLink());
    assertEquals("wav", news1.getAttachments().get(1).getType());
    assertEquals(2500000, news1.getAttachments().get(1).getLength());

    assertNotNull(news1.getPublishDate());
    assertNotNull(news1.getModifiedDate());

    assertNotNull(news1.getSource());
    assertEquals("atom_entry1.source.title", news1.getSource().getName());
    assertEquals(new URI("atom_entry1.source.id"), news1.getSource().getLink());

    assertNotNull(news1.getAuthor());
    assertEquals("atom_entry1.author.name", news1.getAuthor().getName());
    assertEquals(new URI("atom_entry1.author.uri"), news1.getAuthor().getUri());
    assertEquals(new URI("atom_entry1.author.email"), news1.getAuthor().getEmail());

    INews news2 = feed.getNews().get(1);
    assertEquals("<p>atom_entry2.title</p>", news2.getTitle());
   // assertEquals("<p>atom_entry2.description</p>", news2.getDescription());
    assertEquals(new URI("http://www.feed-uri.de/subfolder"), news2.getBase());

    assertNotNull(news2.getSource());
    assertEquals(new URI("atom_entry2.source.link"), news2.getSource().getLink());

    INews news3 = feed.getNews().get(2);
    assertEquals("<p xmlns=\"http://www.w3.org/1999/xhtml\">atom_entry3.title</p>", news3.getTitle());
   //assertEquals("<p xmlns=\"http://www.w3.org/1999/xhtml\">atom_entry3.description</p>", news3.getDescription());
    assertEquals(new URI("http://www.feed-uri.de"), news3.getBase());

    INews news4 = feed.getNews().get(3);
    assertEquals(new URI("atom_entry4.link.href"), news4.getLink());

    INews news5 = feed.getNews().get(4);
    assertEquals("<p xmlns=\"http://www.w3.org/1999/xhtml\">atom_entry5.title</p>", news5.getTitle());
   //assertEquals("<p xmlns=\"http://www.w3.org/1999/xhtml\">atom_entry5.description</p>", news5.getDescription());

    INews news6 = feed.getNews().get(5);
    assertEquals("<p>atom_entry6.title</p>", news6.getTitle());
  }

  /**
   * Test an RSS Feed.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testRSS() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_rss.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_rss.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("RSS 2.0", feed.getFormat());
    assertEquals("rss_title", feed.getTitle());
    assertEquals(new URI("rss_link"), feed.getHomepage());
    assertEquals("rss_description", feed.getDescription());
    assertEquals("rss_language", feed.getLanguage());
    assertNotNull(feed.getPublishDate());
    assertNotNull(feed.getLastBuildDate());
    assertEquals(new URI("rss_docs"), feed.getDocs());
    assertEquals("rss_generator", feed.getGenerator());

    assertNotNull(feed.getAuthor());
    assertEquals("rss_managingeditor", feed.getAuthor().getName());

    assertEquals("rss_webmaster", feed.getWebmaster());
    assertEquals("rss_copyright", feed.getCopyright());
    assertEquals(120, feed.getTTL());

    assertEquals(1, feed.getCategories().size());
    assertEquals("rss_category", feed.getCategories().get(0).getName());
    assertEquals("rss_category.domain", feed.getCategories().get(0).getDomain());

    assertNotNull(feed.getImage());
    assertEquals(new URI("rss_image.url"), feed.getImage().getLink());

    assertEquals(3, feed.getNews().size());
    INews news1 = feed.getNews().get(0);

    assertEquals("rss_item1.title", news1.getTitle());
    assertEquals(new URI("rss_item1.link"), news1.getLink());

    assertNotNull(news1.getAuthor());
    assertEquals("rss_item1.author", news1.getAuthor().getName());

    //assertEquals("rss_item1.description", news1.getDescription());

    assertNotNull(news1.getPublishDate());

    assertEquals("rss_item1.guid", news1.getGuid().getValue());
    assertEquals("rss_item1.comments", news1.getComments());

    assertEquals(1, news1.getAttachments().size());
    assertEquals(new URI("rss_item1.enclosure.url"), news1.getAttachments().get(0).getLink());
    assertEquals("rss_item1.enclosure.type", news1.getAttachments().get(0).getType());
    assertEquals(2500000, news1.getAttachments().get(0).getLength());

    assertEquals(1, news1.getCategories().size());
    assertEquals("rss_item1.category", news1.getCategories().get(0).getName());
    assertEquals("rss_item1.category.domain", news1.getCategories().get(0).getDomain());

    assertNotNull(news1.getSource());
    assertEquals("rss_item1.source", news1.getSource().getName());
    assertEquals(new URI("rss_item1.source.url"), news1.getSource().getLink());
  }

  /**
   * Test URIs being normalized.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testURINormalized() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_uri_mix.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_rss.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals(new URI("http://www.link.com/feed/link/"), feed.getHomepage());
    assertEquals(new URI("http://www.link.com/feed/docs"), feed.getDocs());

    assertNotNull(feed.getImage());
    assertEquals(new URI("http://www.link.com/image/url//"), feed.getImage().getLink());

    assertEquals(3, feed.getNews().size());
    INews news1 = feed.getNews().get(0);

    assertEquals(new URI("http://www.Link%20.com/%5Bfeed%5D/%7Bitem1%7D/link/"), news1.getLink());

    assertEquals(1, news1.getAttachments().size());
    assertEquals(new URI("http://www.Link.com/feed/item1/attachment1/link/"), news1.getAttachments().get(0).getLink());
  }

  /**
   * Test an RDF Feed.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testRDF() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_rdf.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_rdf.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("RDF", feed.getFormat());
    assertEquals("rdf_title", feed.getTitle());
    assertEquals(new URI("rdf_link"), feed.getHomepage());
    assertEquals("rdf_description", feed.getDescription());
    assertEquals("rdf_rights", feed.getCopyright());
    assertEquals("en-us", feed.getLanguage());

    assertNotNull(feed.getAuthor());
    assertEquals("rdf_publisher", feed.getAuthor().getName());

    assertNotNull(feed.getPublishDate());

    assertNotNull(feed.getImage());
    assertEquals(new URI("rdf_image.url"), feed.getImage().getLink());
    assertEquals("rdf_image.title", feed.getImage().getTitle());
    assertEquals(new URI("rdf_image.link"), feed.getImage().getHomepage());

    assertEquals(4, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("rdf_item1.title", news1.getTitle());
    assertEquals(new URI("rdf_item1.link"), news1.getLink());
    //assertEquals("rdf_item1.description", news1.getDescription());

    assertNotNull(news1.getGuid());
    assertEquals("rdf_item1.identifier", news1.getGuid().getValue());

    assertNotNull(news1.getPublishDate());

    assertNotNull(news1.getAuthor());
    assertEquals("rdf_item1.publisher", news1.getAuthor().getName());

    assertNotNull(news1.getSource());
    assertEquals(new URI("rdf_item1.source"), news1.getSource().getLink());

    INews news4 = feed.getNews().get(3);
    assertEquals("rdf_item4.title", news4.getTitle());
    assertEquals(new URI("rdf_item4.link"), news4.getLink());
    //assertEquals("rdf_item4.description", news4.getDescription());
  }

  /**
   * Test an RDF Feed with more DC elements.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testRDF_DublinCore() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_rdf_dc.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_rdf.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("RDF", feed.getFormat());
    assertEquals("rdf_title", feed.getTitle());
    assertEquals(new URI("rdf_link"), feed.getHomepage());
    assertEquals("rdf_description", feed.getDescription());
    assertEquals("rdf_rights", feed.getCopyright());
    assertEquals("en-us", feed.getLanguage());

    assertNotNull(feed.getAuthor());
    assertEquals("rdf_publisher", feed.getAuthor().getName());

    assertNotNull(feed.getPublishDate());

    assertNotNull(feed.getImage());
    assertEquals(new URI("rdf_image.url"), feed.getImage().getLink());
    assertEquals("rdf_image.title", feed.getImage().getTitle());
    assertEquals(new URI("rdf_image.link"), feed.getImage().getHomepage());
  }

  /**
   * Test an RDF Feed with rdf:about.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testRDFAbout() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_rdf_about.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_rdf_about.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("RDF", feed.getFormat());
    assertEquals("rdf_title", feed.getTitle());
    assertEquals(new URI("rdf_link"), feed.getHomepage());
    assertEquals("rdf_description", feed.getDescription());
    assertEquals("rdf_rights", feed.getCopyright());
    assertEquals("en-us", feed.getLanguage());

    assertNotNull(feed.getAuthor());
    assertEquals("rdf_publisher", feed.getAuthor().getName());

    assertNotNull(feed.getPublishDate());

    assertNotNull(feed.getImage());
    assertEquals(new URI("rdf_image.url"), feed.getImage().getLink());
    assertEquals("rdf_image.title", feed.getImage().getTitle());
    assertEquals(new URI("rdf_image.link"), feed.getImage().getHomepage());

    assertEquals(3, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("rdf_item1.title", news1.getTitle());
    assertEquals(new URI("rdf_item1.link"), news1.getLink());
    //assertEquals("rdf_item1.description", news1.getDescription());

    assertNotNull(news1.getGuid());
    assertEquals("rdf_item1.identifier", news1.getGuid().getValue());

    assertNotNull(news1.getPublishDate());

    assertNotNull(news1.getAuthor());
    assertEquals("rdf_item1.publisher", news1.getAuthor().getName());

    assertNotNull(news1.getSource());
    assertEquals(new URI("rdf_item1.source"), news1.getSource().getLink());

    INews news2 = feed.getNews().get(1);
    assertNotNull(news2.getGuid());
    assertEquals("rdf_item2.link", news2.getGuid().getValue());
    assertTrue(news2.getGuid().isPermaLink());

    INews news3 = feed.getNews().get(2);
    assertNull(news3.getGuid());
  }

  /**
   * Test an CDF Feed.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testCDF() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_cdf.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_cdf.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("CDF", feed.getFormat());
    assertEquals(new URI("cdf_base"), feed.getHomepage());
    assertNotNull(feed.getLastModifiedDate());
    assertEquals("cdf_title", feed.getTitle());
    assertEquals("cdf_abstract", feed.getDescription());

    assertEquals(3, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("cdf_entry1.title", news1.getTitle());
   // assertEquals("cdf_entry1.abstract", news1.getDescription());
    assertEquals(new URI("cdf_base/cdf_entry1.href"), news1.getLink());
    assertNotNull(news1.getPublishDate());
  }

  /**
   * Test an OPML Feed.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testOPML() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_opml.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_opml.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("OPML", feed.getFormat());
    assertEquals("opml_title", feed.getTitle());
    assertNotNull(feed.getLastModifiedDate());
    assertNotNull(feed.getLastBuildDate());

    assertNotNull(feed.getAuthor());
    assertEquals("opml_ownername", feed.getAuthor().getName());
    assertEquals(new URI("opml_owneremail"), feed.getAuthor().getEmail());

    assertEquals(4, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("opml_outline1.title", news1.getTitle());
    assertEquals(new URI("opml_outline1.url"), news1.getLink());
   // assertEquals("opml_outline1.text", news1.getDescription());

    INews news4 = feed.getNews().get(3);
    assertEquals("opml_outline4.title", news4.getTitle());
    assertEquals(new URI("opml_outline4.url"), news4.getLink());
   // assertEquals("opml_outline4.text", news4.getDescription());
  }

  /**
   * Test contributing Format Handlers.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testFormat() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_format.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_format.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("MyFeed", feed.getFormat());
    assertEquals("format_custom", feed.getTitle());
    assertEquals("format_channel.sprache", feed.getLanguage());

    assertEquals(1, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("format_item1.title", news1.getTitle());
    assertEquals(new URI("format_item1.link"), news1.getLink());
   // assertEquals("format_item1.description", news1.getDescription());
  }

  /**
   * Test a Feed using ISO Encoding.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testISO() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_ISO-8859-1.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_atom.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("iso_title#\u00F6\u00E4\u00FC\u00DF", feed.getTitle());
    assertEquals("iso_description#\u00F6\u00E4\u00FC\u00DF", feed.getDescription());

    assertEquals(1, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("iso_item1.title#\u00F6\u00E4\u00FC\u00DF", news1.getTitle());
    //assertEquals("iso_item1.description#\u00F6\u00E4\u00FC\u00DF", news1.getDescription());
  }

  /**
   * Test a Feed using Entities.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testEntities() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_entities.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_atom.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("entities_title#\u00F6\u00E4\u00FC&<>", feed.getTitle());
    assertEquals("entities_description#\u00F6\u00E4\u00FC&<>", feed.getDescription());

    assertEquals(1, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("entities_item1.title#\u00F6\u00E4\u00FC&<>", news1.getTitle());
  //  assertEquals("entities_item1.description#\u00F6\u00E4\u00FC&<>", news1.getDescription());
  }

  /**
   * Test a Feed using undeclared Entities. TODO: Enable again once it succeeds
   * on Java 1.6 (see Bug 325)
   *
   * @throws Exception
   */
  @Test
  @Ignore
  @SuppressWarnings("nls")
  public void testUndeclaredEntities() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_undeclared_entities.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_atom.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("entities_title#\u00F6\u00E4\u00FC&<>", feed.getTitle());
    assertEquals("entities_description#\u00F6\u00E4\u00FC&<>", feed.getDescription());

    assertEquals(1, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("entities_item1.title#\u00F6\u00E4\u00FC&<>", news1.getTitle());
    //assertEquals("entities_item1.description#\u00F6\u00E4\u00FC&<>", news1.getDescription());
  }

  /**
   * See Bug 1078.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testMediaTitle() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_media_title.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_rss.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals(1, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("News Title", news1.getTitle());
  }

  /**
   * Test a Feed that has been loaded via W3C Document.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testW3CDocument() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_opml.xml");
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder documentBuilder = factory.newDocumentBuilder();

    InputSource source = new InputSource(inS);
    org.w3c.dom.Document doc = documentBuilder.parse(source);
    inS.close();

    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_opml.xml"));
    Owl.getInterpreter().interpretW3CDocument(doc, feed);

    assertEquals("OPML", feed.getFormat());
    assertEquals("opml_title", feed.getTitle());
    assertNotNull(feed.getLastModifiedDate());
    assertNotNull(feed.getLastBuildDate());

    assertNotNull(feed.getAuthor());
    assertEquals("opml_ownername", feed.getAuthor().getName());
    assertEquals(new URI("opml_owneremail"), feed.getAuthor().getEmail());

    assertEquals(4, feed.getNews().size());

    INews news1 = feed.getNews().get(0);
    assertEquals("opml_outline1.title", news1.getTitle());
    assertEquals(new URI("opml_outline1.url"), news1.getLink());
    //assertEquals("opml_outline1.text", news1.getDescription());

    INews news4 = feed.getNews().get(3);
    assertEquals("opml_outline4.title", news4.getTitle());
    assertEquals(new URI("opml_outline4.url"), news4.getLink());
   // assertEquals("opml_outline4.text", news4.getDescription());
  }

  /**
   * Test a Feed with an unknown format
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testUnknownFormat() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_unknown.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_unknown.xml"));
    UnknownFormatException e = null;

    try {
      Owl.getInterpreter().interpret(inS, feed, null);
    } catch (UnknownFormatException e1) {
      e = e1;
    }

    assertNotNull(e);
  }

  /**
   * Test parsing some dates.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testDateParser() throws Exception {
    Calendar cal = new GregorianCalendar();
    cal.setTimeZone(TimeZone.getTimeZone("GMT"));

    /* Undetailed Checks */
    assertNotNull(DateUtils.parseDate("Mon, 19 Dec 2005 13:56:00 GMT"));
    assertNotNull(DateUtils.parseDate("Mon, 19 Dec 2005 07:00:21 PST"));
    assertNotNull(DateUtils.parseDate("Fri, 16 Dec 2005 00:00:00 EDT"));
    assertNotNull(DateUtils.parseDate("Mon, 19 Dec 2005 17:00:00 -0500"));
    assertNotNull(DateUtils.parseDate("Tue, 19 Jul 05 23:00:51 GMT"));
    assertNotNull(DateUtils.parseDate("2005-12-14T00:00:00+00:00"));
    assertNotNull(DateUtils.parseDate("2005-12-16T00:00:00Z"));
    assertNotNull(DateUtils.parseDate("2009-11-04T06:44:55.985-07:00"));
    assertNotNull(DateUtils.parseDate("2005-12-16T11:29:19+01:00"));
    assertNotNull(DateUtils.parseDate("2005-12-16T11:29:19 -01:00"));
    assertNotNull(DateUtils.parseDate("2005-12-16"));

    /* Detailed Check #1 */
    cal.setTime(DateUtils.parseDate("Tue, 19 Jul 2005 23:00:51 GMT"));
    assertEquals(2005, cal.get(Calendar.YEAR));
    assertEquals(6, cal.get(Calendar.MONTH));
    assertEquals(19, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(3, cal.get(Calendar.DAY_OF_WEEK));
    assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal.get(Calendar.MINUTE));
    assertEquals(51, cal.get(Calendar.SECOND));

    /* Detailed Check #2 */
    cal.setTime(DateUtils.parseDate("Tue, 19 Jul 05 23:00:51 GMT"));
    assertEquals(2005, cal.get(Calendar.YEAR));
    assertEquals(6, cal.get(Calendar.MONTH));
    assertEquals(19, cal.get(Calendar.DAY_OF_MONTH));
    assertEquals(3, cal.get(Calendar.DAY_OF_WEEK));
    assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
    assertEquals(0, cal.get(Calendar.MINUTE));
    assertEquals(51, cal.get(Calendar.SECOND));
  }

  /**
   * Test contributing an ElementHandler to a RSS.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testRSSElements() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_rss_elements.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_rss_elements.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("sub_rss_leveld", feed.getProperty("sub_rss_leveld"));
    assertEquals("sub_channel_leveld", feed.getProperty("sub_channel_leveld"));

    assertNotNull(feed.getImage());

    assertEquals(1, feed.getNews().size());
    INews news = feed.getNews().get(0);
    assertEquals("sub_item_leveld", news.getProperty("sub_item_leveld"));
  }

  /**
   * Test contributing an NamespaceHandler and using it in a RSS.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testRSSNamespaces() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_rss_namespaces.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_rss_namespaces.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("custom_formatAttribute", feed.getProperty("custom_formatAttribute"));
    assertEquals("custom_channelAttribute", feed.getProperty("custom_channelAttribute"));
    assertEquals("sub_channel_leveld", feed.getProperty("sub_channel_leveld"));
    assertEquals("custom_skipHoursAttribute", feed.getProperty("custom_skipHoursAttribute"));
    assertEquals("custom_skipDaysAttribute", feed.getProperty("custom_skipDaysAttribute"));

    assertEquals(1, feed.getCategories().size());
    assertEquals("custom_categoryAttribute", feed.getCategories().get(0).getProperty("custom_categoryAttribute"));

    assertNotNull(feed.getImage());

    assertEquals(1, feed.getNews().size());
    INews news = feed.getNews().get(0);
    assertEquals("custom_itemAttribute", news.getProperty("custom_itemAttribute"));
    assertEquals("sub_item_leveld", news.getProperty("sub_item_leveld"));
    assertEquals("custom_titleAttribute", news.getProperty("custom_titleAttribute"));

    assertNotNull(news.getSource());

    assertNotNull(news.getAttachments());
    assertEquals("custom_enclosureAttribute", news.getAttachments().get(0).getProperty("custom_enclosureAttribute"));

    assertNotNull(news.getSource());
  }

  /**
   * Test contributing an ElementHandler to an Atom.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testAtomElements() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_atom_elements.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_atom_elements.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("entry_leveld", feed.getProperty("entry_leveld"));

    assertNotNull(feed.getAuthor());
    assertEquals("sub_author_leveld", feed.getAuthor().getProperty("sub_author_leveld"));

    assertEquals(1, feed.getNews().size());
    INews news = feed.getNews().get(0);
    assertEquals("sub_entry_leveld", news.getProperty("sub_entry_leveld"));

    assertNotNull(news.getSource());

    assertNotNull(news.getAuthor());
    assertEquals("sub_author_leveld", news.getAuthor().getProperty("sub_author_leveld"));
  }

  /**
   * Test contributing an NamespaceHandler and using it in an Atom.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testAtomNamespaces() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_atom_namespaces.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_atom_namespaces.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("custom_formatAttribute", feed.getProperty("custom_formatAttribute"));
    assertEquals("custom_titleAttribute", feed.getProperty("custom_titleAttribute"));
    assertEquals("custom_linkAttribute", feed.getProperty("custom_linkAttribute"));

    assertEquals(1, feed.getCategories().size());
    assertEquals("custom_categoryAttribute", feed.getCategories().get(0).getProperty("custom_categoryAttribute"));

    assertEquals("entry_leveld", feed.getProperty("entry_leveld"));

    assertNotNull(feed.getAuthor());
    assertEquals("sub_author_leveld", feed.getAuthor().getProperty("sub_author_leveld"));
    assertEquals("custom_authorAttribute", feed.getAuthor().getProperty("custom_authorAttribute"));
    assertEquals("custom_nameAttribute", feed.getAuthor().getProperty("custom_nameAttribute"));

    assertEquals(1, feed.getNews().size());
    INews news = feed.getNews().get(0);
    assertEquals("sub_entry_leveld", news.getProperty("sub_entry_leveld"));
    assertEquals("custom_entryAttribute", news.getProperty("custom_entryAttribute"));
    assertEquals("custom_titleAttribute", news.getProperty("custom_titleAttribute"));
    assertEquals("custom_linkAttribute", news.getProperty("custom_linkAttribute"));

    assertEquals(1, news.getCategories().size());
    assertEquals("custom_categoryAttribute", news.getCategories().get(0).getProperty("custom_categoryAttribute"));

    assertEquals(1, news.getAttachments().size());
    assertEquals("custom_enclosureAttribute", news.getAttachments().get(0).getProperty("custom_enclosureAttribute"));

    assertNotNull(news.getSource());

    assertNotNull(news.getAuthor());
    assertEquals("sub_author_leveld", news.getAuthor().getProperty("sub_author_leveld"));
    assertEquals("custom_authorAttribute", news.getAuthor().getProperty("custom_authorAttribute"));
    assertEquals("custom_nameAttribute", news.getAuthor().getProperty("custom_nameAttribute"));
  }

  /**
   * Test contributing an ElementHandler to an OPML.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testOPMLElements() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_opml_elements.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_opml_elements.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("head_leveld", feed.getProperty("head_leveld"));
    assertEquals("sub_head_leveld", feed.getProperty("sub_head_leveld"));
    assertEquals("outline_leveld", feed.getProperty("outline_leveld"));
  }

  /**
   * Test contributing an NamespaceHandler and using it in an OPML.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testOPMLNamespaces() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_opml_namespaces.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_opml_namespaces.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("custom_formatAttribute", feed.getProperty("custom_formatAttribute"));
    assertEquals("custom_headAttribute", feed.getProperty("custom_headAttribute"));
    assertEquals("custom_titleAttribute", feed.getProperty("custom_titleAttribute"));
    assertEquals("custom_bodyAttribute", feed.getProperty("custom_bodyAttribute"));

    assertEquals("head_leveld", feed.getProperty("head_leveld"));
    assertEquals("sub_head_leveld", feed.getProperty("sub_head_leveld"));
    assertEquals("outline_leveld", feed.getProperty("outline_leveld"));

    assertEquals(1, feed.getNews().size());
    INews news = feed.getNews().get(0);
    assertEquals("custom_outlineAttribute", news.getProperty("custom_outlineAttribute"));
  }

  /**
   * Test contributing an ElementHandler to a CDF.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testCDFElements() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_cdf_elements.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_cdf_elements.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("item_leveld", feed.getProperty("item_leveld"));

    assertEquals(1, feed.getNews().size());
    INews news = feed.getNews().get(0);
    assertEquals("sub_item_leveld", news.getProperty("sub_item_leveld"));
  }

  /**
   * Test contributing an NamespaceHandler and using it in a CDF.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testCDFNamespaces() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_cdf_namespaces.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_cdf_namespaces.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("custom_formatAttribute", feed.getProperty("custom_formatAttribute"));
    assertEquals("custom_titleAttribute", feed.getProperty("custom_titleAttribute"));

    assertEquals("item_leveld", feed.getProperty("item_leveld"));

    assertEquals(1, feed.getNews().size());
    INews news = feed.getNews().get(0);
    assertEquals("sub_item_leveld", news.getProperty("sub_item_leveld"));
    assertEquals("custom_itemAttribute", news.getProperty("custom_itemAttribute"));
    assertEquals("custom_titleAttribute", news.getProperty("custom_titleAttribute"));
  }

  /**
   * Test contributing an ElementHandler to a RDF.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testRDFElements() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_rdf_elements.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_rdf_elements.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("channel_leveld", feed.getProperty("channel_leveld"));
    assertEquals("sub_channel_leveld", feed.getProperty("sub_channel_leveld"));

    assertNotNull(feed.getImage());

    assertEquals(1, feed.getNews().size());
    INews news = feed.getNews().get(0);
    assertEquals("sub_item_leveld", news.getProperty("sub_item_leveld"));
  }

  /**
   * Test contributing an NamespaceHandler and using it in a RDF.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testRDFNamespaces() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_rdf_namespaces.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_rdf_namespaces.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("custom_formatAttribute", feed.getProperty("custom_formatAttribute"));
    assertEquals("custom_channelAttribute", feed.getProperty("custom_channelAttribute"));
    assertEquals("custom_titleAttribute", feed.getProperty("custom_titleAttribute"));

    assertEquals("channel_leveld", feed.getProperty("channel_leveld"));
    assertEquals("sub_channel_leveld", feed.getProperty("sub_channel_leveld"));

    assertNotNull(feed.getImage());

    assertEquals(1, feed.getNews().size());
    INews news = feed.getNews().get(0);
    assertEquals("sub_item_leveld", news.getProperty("sub_item_leveld"));
    assertEquals("custom_itemAttribute", news.getProperty("custom_itemAttribute"));
    assertEquals("custom_titleAttribute", news.getProperty("custom_titleAttribute"));
  }

  /**
   * Test a Bugzilla-Bug-XML
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testBugzilla() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_bugzilla.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_bugzilla.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("Bugzilla 2.20", feed.getFormat());
    assertEquals(new URI("http://dev.rssowl.org/"), feed.getBase());
    assertEquals("bugzilla_short_description", feed.getTitle());
    assertEquals(new URI("http://dev.rssowl.org/show_bug.cgi?id=10"), feed.getHomepage());
    assertEquals(new URI("bugzilla_reporter@mail.xx"), feed.getAuthor().getEmail());

    assertEquals(3, feed.getNews().size());
    INews news = feed.getNews().get(0);
    assertEquals(new URI("http://dev.rssowl.org/show_bug.cgi?id=10"), news.getLink());
    assertEquals("Comment from bugzilla_who", news.getTitle());
   // assertEquals("bugzilla_thetext", news.getDescription());
    assertEquals(DateUtils.parseDate("2006-01-12 22:29"), news.getModifiedDate());
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSyndicationNamespace() throws Exception {
    InputStream inS = getClass().getResourceAsStream("/data/interpreter/feed_syndication.xml");
    IFeed feed = new Feed(new URI("http://www.data.interpreter.feed_syndication.xml"));
    Owl.getInterpreter().interpret(inS, feed, null);

    assertEquals("RSS 2.0", feed.getFormat());
  }
}