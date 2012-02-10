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

package org.rssowl.core.tests.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.rssowl.core.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests methods in {@link StringUtils}.
 *
 * @author bpasero
 */
public class StringUtilsTest {

  /**
   * @throws Exception
   */
  @Test
  public void testStripTags() throws Exception {
    assertEquals("Foo Bar", StringUtils.stripTags("Foo Bar", true));
    assertEquals("Foo < Bar", StringUtils.stripTags("Foo &lt; Bar", true));
    assertEquals("Foo &lt; Bar", StringUtils.stripTags("Foo &lt; Bar", false));
    assertEquals("Foo  Bar", StringUtils.stripTags("Foo <br> Bar", true));
    assertEquals("Foo Bar", StringUtils.stripTags("Foo Bar<br>", true));
    assertEquals("Foo Bar Foo Bar", StringUtils.stripTags("Foo Bar<br> Foo Bar<br>", true));
    assertEquals("Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar Foo Bar", StringUtils.stripTags("Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br> Foo Bar<br>", true));
    assertEquals("T-Systems übernimmt DVB-H-Sendebetrieb", StringUtils.stripTags("<h3 class=\"anriss\"><a href=\"/newsticker/meldung/97406\">T-Systems übernimmt DVB-H-Sendebetrieb</a></h3>", true));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testStripTagsTimeMagazine() throws Exception {
    assertEquals("", StringUtils.stripTags("<img src=\"http://feeds.feedburner.com/~r/time/topstories/~4/183799328\" height=\"1\" width=\"1\"/>", true));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testNormalize() throws Exception {
    assertEquals("Foo Bar", StringUtils.normalizeString("Foo Bar"));
    assertEquals("FooBar", StringUtils.normalizeString("FooBar"));
    assertEquals("FooBar", StringUtils.normalizeString(" FooBar"));
    assertEquals("FooBar", StringUtils.normalizeString(" FooBar "));
    assertEquals("FooBar", StringUtils.normalizeString("  FooBar "));
    assertEquals("FooBar", StringUtils.normalizeString("  FooBar  "));
    assertEquals("Foo Bar", StringUtils.normalizeString("  Foo Bar  "));
    assertEquals("Foo Bar", StringUtils.normalizeString("  Foo\nBar  "));
    assertEquals("Foo Bar", StringUtils.normalizeString("  Foo\n\t Bar  "));
    assertEquals("Foo Bar", StringUtils.normalizeString("  Foo Bar\n  "));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testTokenizePhraseAware() throws Exception {
    assertTrue(StringUtils.tokenizePhraseAware(null).isEmpty());
    assertTrue(StringUtils.tokenizePhraseAware("").isEmpty());
    assertTrue(StringUtils.tokenizePhraseAware(" ").isEmpty());
    assertTrue(StringUtils.tokenizePhraseAware("  ").isEmpty());
    assertTrue(StringUtils.tokenizePhraseAware("     ").isEmpty());

    assertEquals(Arrays.asList(new String[] { "\"" }), StringUtils.tokenizePhraseAware("\""));
    assertEquals(Arrays.asList(new String[] { "\"\"" }), StringUtils.tokenizePhraseAware("\"\""));
    assertEquals(Arrays.asList(new String[] { "\"\"", "a" }), StringUtils.tokenizePhraseAware("\"\" a"));

    assertEquals(Arrays.asList(new String[] { "foo" }), StringUtils.tokenizePhraseAware("foo"));
    assertEquals(Arrays.asList(new String[] { "foo", "bar" }), StringUtils.tokenizePhraseAware("foo bar"));
    assertEquals(Arrays.asList(new String[] { "foo", "bar" }), StringUtils.tokenizePhraseAware(" foo bar "));
    assertEquals(Arrays.asList(new String[] { "foo", "bar" }), StringUtils.tokenizePhraseAware(" foo    bar "));
    assertEquals(Arrays.asList(new String[] { "foo", "bar", "foobar" }), StringUtils.tokenizePhraseAware("foo bar foobar"));
    assertEquals(Arrays.asList(new String[] { "foo", "bar", "foobar" }), StringUtils.tokenizePhraseAware(" foo bar foobar "));
    assertEquals(Arrays.asList(new String[] { "foo", "bar", "foobar" }), StringUtils.tokenizePhraseAware(" foo    bar foobar "));

    assertEquals(Arrays.asList(new String[] { "\"foo" }), StringUtils.tokenizePhraseAware("\"foo"));
    assertEquals(Arrays.asList(new String[] { "foo\"" }), StringUtils.tokenizePhraseAware("foo\""));
    assertEquals(Arrays.asList(new String[] { "\"foo\"" }), StringUtils.tokenizePhraseAware("\"foo\""));
    assertEquals(Arrays.asList(new String[] { "\"foo bar\"" }), StringUtils.tokenizePhraseAware("\"foo bar\""));
    assertEquals(Arrays.asList(new String[] { "\"foo bar" }), StringUtils.tokenizePhraseAware("\"foo bar"));
    assertEquals(Arrays.asList(new String[] { "foo", "bar\"" }), StringUtils.tokenizePhraseAware("foo bar\""));
    assertEquals(Arrays.asList(new String[] { "\"foo\"", "bar" }), StringUtils.tokenizePhraseAware("\"foo\" bar"));
    assertEquals(Arrays.asList(new String[] { "\"foo\"", "\"bar" }), StringUtils.tokenizePhraseAware("\"foo\" \"bar"));
    assertEquals(Arrays.asList(new String[] { "\"foo\"", "bar\"" }), StringUtils.tokenizePhraseAware("\"foo\" bar\""));
    assertEquals(Arrays.asList(new String[] { "\"foo\"", "\"bar\"" }), StringUtils.tokenizePhraseAware("\"foo\" \"bar\""));

    assertEquals(Arrays.asList(new String[] { "\"foo bar\"", "foobar" }), StringUtils.tokenizePhraseAware("\"foo bar\" foobar"));
    assertEquals(Arrays.asList(new String[] { "foo", "\"bar foobar\"" }), StringUtils.tokenizePhraseAware("foo \"bar foobar\""));
    assertEquals(Arrays.asList(new String[] { "foo", "\"bar foobar\"" }), StringUtils.tokenizePhraseAware("foo  \"bar    foobar\""));
    assertEquals(Arrays.asList(new String[] { "\"foo bar foobar\"" }), StringUtils.tokenizePhraseAware("\"foo bar foobar\""));
    assertEquals(Arrays.asList(new String[] { "\"foo\"bar\"foobar\"" }), StringUtils.tokenizePhraseAware("\"foo\"bar\"foobar\""));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSmartTrim() throws Exception {
    assertEquals("foo", StringUtils.smartTrim("foo", 10));
    assertEquals("foo bar", StringUtils.smartTrim("foo bar", 10));
    assertEquals("foo...", StringUtils.smartTrim("foo bar", 5));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testReplaceAll() throws Exception {
    Set<String> strings = new HashSet<String>();
    strings.add("foo");
    strings.add("bar");
    strings.add("foo ? bar");

    Set<String> result = StringUtils.replaceAll(strings, "foo", "bar");
    assertTrue(result.containsAll(Arrays.asList(new String[] { "bar", "bar ? bar" })));

    result = StringUtils.replaceAll(strings, "?", "bar");
    assertTrue(result.containsAll(Arrays.asList(new String[] { "bar", "foo", "foo bar bar" })));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testhtmlEscape() throws Exception {
    assertEquals("foo bar", StringUtils.htmlEscape("foo bar"));
    assertEquals("&lt;foo bar&gt;", StringUtils.htmlEscape("<foo bar>"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFilterTags() throws Exception {
    assertEquals("foo bar", StringUtils.filterTags("foo bar", null, false));
    assertEquals(" bar", StringUtils.filterTags("<foo> bar", null, false));
    assertEquals("foo bar", StringUtils.filterTags("foo bar", new HashSet<String>(Arrays.asList(new String[] { "a", "br" })), false));
    assertEquals("<foo> bar", StringUtils.filterTags("<foo> bar", new HashSet<String>(Arrays.asList(new String[] { "a", "br" })), false));
    assertEquals("alles       bar", StringUtils.filterTags("alles <foo> bar", new HashSet<String>(Arrays.asList(new String[] { "foo", "br" })), false));
    assertEquals("      bar", StringUtils.filterTags("<foo> bar", new HashSet<String>(Arrays.asList(new String[] { "foo", "br" })), false));
  }
}