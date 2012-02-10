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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.interpreter.ITypeImporter;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IGuid;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.NewsAdapter;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.RegExUtils;
import org.rssowl.core.util.ReparentInfo;
import org.rssowl.ui.internal.util.ModelUtils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests methods in {@link CoreUtils}.
 *
 * @author bpasero
 */
public class CoreUtilsTest {
  private IModelFactory fFactory = Owl.getModelFactory();

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
  }

  /**
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testNormalize() throws Exception {
    CoreUtils.normalize(null);
    CoreUtils.normalize(Collections.EMPTY_LIST);

    IModelFactory factory = Owl.getModelFactory();
    IFolder root = factory.createFolder(null, null, "Root");
    IFolder folder1 = factory.createFolder(null, root, "Folder 1");
    IFolder folder2 = factory.createFolder(null, root, "Folder 2");
    IFolder folder3 = factory.createFolder(null, folder2, "Folder 3");
    IBookMark mark1 = factory.createBookMark(null, root, new FeedLinkReference(new URI("#")), "Mark 1");
    IBookMark mark2 = factory.createBookMark(null, folder1, new FeedLinkReference(new URI("#")), "Mark 2");
    IBookMark mark3 = factory.createBookMark(null, folder2, new FeedLinkReference(new URI("#")), "Mark 3");
    IBookMark mark4 = factory.createBookMark(null, folder3, new FeedLinkReference(new URI("#")), "Mark 4");

    List<IEntity> entities = new ArrayList<IEntity>();
    entities.add(root);
    CoreUtils.normalize(entities);
    assertEquals(1, entities.size());
    assertEquals(root, entities.get(0));

    entities.clear();
    entities.addAll(Arrays.asList(new IFolderChild[] { root, folder1, folder2, folder3 }));
    CoreUtils.normalize(entities);
    assertEquals(1, entities.size());
    assertEquals(root, entities.get(0));

    entities.clear();
    entities.addAll(Arrays.asList(new IFolderChild[] { root, folder1, folder2, folder3, mark1, mark2, mark3, mark4 }));
    CoreUtils.normalize(entities);
    assertEquals(1, entities.size());
    assertEquals(root, entities.get(0));

    entities.clear();
    entities.addAll(Arrays.asList(new IFolderChild[] { root, mark4 }));
    CoreUtils.normalize(entities);
    assertEquals(1, entities.size());
    assertEquals(root, entities.get(0));

    entities.clear();
    entities.addAll(Arrays.asList(new IFolderChild[] { mark1, mark2, mark3, mark4 }));
    CoreUtils.normalize(entities);
    assertEquals(4, entities.size());
    assertEquals(mark1, entities.get(0));
    assertEquals(mark2, entities.get(1));
    assertEquals(mark3, entities.get(2));
    assertEquals(mark4, entities.get(3));

    entities.clear();
    entities.addAll(Arrays.asList(new IFolderChild[] { folder3, mark4 }));
    CoreUtils.normalize(entities);
    assertEquals(1, entities.size());
    assertEquals(folder3, entities.get(0));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGenerateNameForSearch() throws Exception {
    IFolderChild root = DynamicDAO.save(fFactory.createFolder(null, null, "Root"));

    ISearchField field1 = fFactory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    ISearchField field2 = fFactory.createSearchField(INews.LOCATION, INews.class.getName());
    ISearchField field3 = fFactory.createSearchField(INews.IS_FLAGGED, INews.class.getName());
    ISearchField field4 = fFactory.createSearchField(INews.PUBLISH_DATE, INews.class.getName());
    ISearchField field5 = fFactory.createSearchField(INews.STATE, INews.class.getName());

    ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "foo bar");
    ISearchCondition cond2 = fFactory.createSearchCondition(field2, SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(root)));
    ISearchCondition cond3 = fFactory.createSearchCondition(field3, SearchSpecifier.IS, true);
    ISearchCondition cond4 = fFactory.createSearchCondition(field4, SearchSpecifier.IS, new Date());
    ISearchCondition cond5 = fFactory.createSearchCondition(field5, SearchSpecifier.IS, INews.State.getVisible());

    String name = CoreUtils.getName(Arrays.asList(new ISearchCondition[] { cond1, cond2, cond3, cond4, cond5 }), true);
    assertNotNull(name);
    assertTrue(name.contains("foo bar"));
    assertTrue(name.contains("Root"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetHeadline() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news1 = fFactory.createNews(null, feed, new Date());

    assertNotNull(CoreUtils.getHeadline(news1, false));

    news1.setDescription("Foo Bar");
    assertEquals("Foo Bar", CoreUtils.getHeadline(news1, false));

    news1.setDescription("Foo &amp; Bar");
    assertEquals("Foo &amp; Bar", CoreUtils.getHeadline(news1, false));
    assertEquals("Foo & Bar", CoreUtils.getHeadline(news1, true));

    news1.setTitle("A Foo Bar");
    assertEquals("A Foo Bar", CoreUtils.getHeadline(news1, false));

    news1.setTitle("A Foo &amp; Bar");
    assertEquals("A Foo &amp; Bar", CoreUtils.getHeadline(news1, false));
    assertEquals("A Foo & Bar", CoreUtils.getHeadline(news1, true));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetLink() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news1 = fFactory.createNews(null, feed, new Date());

    assertNull(CoreUtils.getLink(news1));

    IGuid guid = fFactory.createGuid(news1, "www.guid.de", false);
    news1.setGuid(guid);
    assertEquals("http://www.guid.de", CoreUtils.getLink(news1));

    news1.setLink(new URI("www.link.de"));
    assertEquals("http://www.link.de", CoreUtils.getLink(news1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetLinkRelative() throws Exception {
    IFeed feedNoBase = fFactory.createFeed(null, new URI("http://www.feed1.com"));

    IFeed feedWithBase = fFactory.createFeed(null, new URI("http://www.feed2.com"));
    feedWithBase.setBase(new URI("http://www.base.com"));

    /* No Base */
    {
      INews newsNoBase = fFactory.createNews(null, feedNoBase, new Date());
      assertNull(CoreUtils.getLink(newsNoBase));

      newsNoBase.setLink(new URI("link"));
      assertEquals("http://www.feed1.com/link", CoreUtils.getLink(newsNoBase));

      newsNoBase.setLink(new URI("http://www.rssowl.org/foo"));
      assertEquals("http://www.rssowl.org/foo", CoreUtils.getLink(newsNoBase));
    }

    /* With Base */
    {
      INews newsWithBase = fFactory.createNews(null, feedWithBase, new Date());
      newsWithBase.setBase(feedWithBase.getBase());
      assertNull(CoreUtils.getLink(newsWithBase));

      newsWithBase.setLink(new URI("link"));
      assertEquals("http://www.base.com/link", CoreUtils.getLink(newsWithBase));

      newsWithBase.setLink(new URI("http://www.rssowl.org/foo"));
      assertEquals("http://www.rssowl.org/foo", CoreUtils.getLink(newsWithBase));
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testStickyStateChange() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.isStickyStateChange(events.iterator().next()));
            assertTrue(CoreUtils.isStickyStateChange(events));
            assertTrue(CoreUtils.isStickyStateChange(events, true));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertFalse(CoreUtils.isStickyStateChange(events.iterator().next()));
            assertFalse(CoreUtils.isStickyStateChange(events));
            assertFalse(CoreUtils.isStickyStateChange(events, true));
            counter.incrementAndGet();
          }

          else if (mode.get() == 2) {
            assertTrue(CoreUtils.isStickyStateChange(events.iterator().next()));
            assertTrue(CoreUtils.isStickyStateChange(events));
            assertFalse(CoreUtils.isStickyStateChange(events, true));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setFlagged(true);
      DynamicDAO.save(news);

      mode.set(1);
      news.setTitle("Foo");
      DynamicDAO.save(news);

      mode.set(2);
      news.setFlagged(false);
      DynamicDAO.save(news);

      assertEquals(3, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @Ignore
  public void testStateStateChange() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.isStateChange(events.iterator().next()));
            assertTrue(CoreUtils.isStateChange(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertFalse(CoreUtils.isStateChange(events.iterator().next()));
            assertFalse(CoreUtils.isStateChange(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 2) {
            assertTrue(CoreUtils.isStateChange(events.iterator().next()));
            assertTrue(CoreUtils.isStateChange(events));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setState(INews.State.UNREAD);
      DynamicDAO.save(news);

      mode.set(1);
      news.setTitle("Foo");
      DynamicDAO.save(news);

      mode.set(2);
      news.setState(INews.State.UNREAD);
      DynamicDAO.save(news);

      assertEquals(3, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGotDeleted() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertFalse(CoreUtils.gotDeleted(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertTrue(CoreUtils.gotDeleted(events));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setState(INews.State.UNREAD);
      DynamicDAO.save(news);

      mode.set(1);
      news.setState(INews.State.HIDDEN);
      DynamicDAO.save(news);

      assertEquals(2, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGotRestored() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.gotDeleted(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertTrue(CoreUtils.gotRestored(events));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setState(INews.State.HIDDEN);
      DynamicDAO.save(news);

      mode.set(1);
      news.setState(INews.State.UNREAD);
      DynamicDAO.save(news);

      assertEquals(2, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testLabelChange() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.isLabelChange(events.iterator().next()));
            assertTrue(CoreUtils.isLabelChange(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertTrue(CoreUtils.isLabelChange(events.iterator().next()));
            assertTrue(CoreUtils.isLabelChange(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 2) {
            assertFalse(CoreUtils.isLabelChange(events.iterator().next()));
            assertFalse(CoreUtils.isLabelChange(events));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      ILabel label = DynamicDAO.save(fFactory.createLabel(null, "Label"));
      news.addLabel(label);
      DynamicDAO.save(news);

      mode.set(1);
      news.removeLabel(label);
      DynamicDAO.save(news);

      mode.set(2);
      news.setTitle("Foo");
      DynamicDAO.save(news);

      assertEquals(3, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testTitleChange() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.isTitleChange(events));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertFalse(CoreUtils.isTitleChange(events));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setTitle("Foo");
      DynamicDAO.save(news);

      mode.set(1);
      news.setDescription("Bar");
      DynamicDAO.save(news);

      assertEquals(2, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFeedLinks() throws Exception {
    IFeed feed1 = DynamicDAO.save(fFactory.createFeed(null, new URI("feed1")));
    IFeed feed2 = DynamicDAO.save(fFactory.createFeed(null, new URI("feed2")));
    DynamicDAO.save(fFactory.createFeed(null, new URI("feed3")));

    IFolder root = fFactory.createFolder(null, null, "root");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed1.getLink()), "Mark 1");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed2.getLink()), "Mark 2");

    DynamicDAO.save(root);

    Set<String> feedLinks = CoreUtils.getFeedLinks();
    assertEquals(2, feedLinks.size());
    assertTrue(feedLinks.contains("feed1"));
    assertTrue(feedLinks.contains("feed2"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetBookMark() throws Exception {
    IFeed feed1 = DynamicDAO.save(fFactory.createFeed(null, new URI("feed1")));
    IFeed feed2 = DynamicDAO.save(fFactory.createFeed(null, new URI("feed2")));

    IFolder root = fFactory.createFolder(null, null, "root");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed1.getLink()), "Mark 1");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed2.getLink()), "Mark 2");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed2.getLink()), "Mark 3");

    DynamicDAO.save(root);

    assertEquals("Mark 1", CoreUtils.getBookMark(new FeedLinkReference(feed1.getLink())).getName());
    assertNotNull(CoreUtils.getBookMark(new FeedLinkReference(feed2.getLink())));
    assertNull(CoreUtils.getBookMark(new FeedLinkReference(new URI("feed3"))));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testGetBookMarkByLinkText() throws Exception {
    IFeed feed1 = DynamicDAO.save(fFactory.createFeed(null, new URI("feed1")));
    IFeed feed2 = DynamicDAO.save(fFactory.createFeed(null, new URI("feed2")));

    IFolder root = fFactory.createFolder(null, null, "root");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed1.getLink()), "Mark 1");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed2.getLink()), "Mark 2");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed2.getLink()), "Mark 3");

    DynamicDAO.save(root);

    assertEquals("Mark 1", CoreUtils.getBookMark(feed1.getLink().toString()).getName());
    assertNotNull(CoreUtils.getBookMark(feed2.getLink().toString()));
    assertNull(CoreUtils.getBookMark("feed3"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testExtractWords() throws Exception {
    ISearchField field1 = fFactory.createSearchField(INews.TITLE, INews.class.getName());
    ISearchField field2 = fFactory.createSearchField(INews.DESCRIPTION, INews.class.getName());
    ISearchField field3 = fFactory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());

    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>();
    conditions.add(fFactory.createSearchCondition(field1, SearchSpecifier.CONTAINS, "foo bar"));
    conditions.add(fFactory.createSearchCondition(field2, SearchSpecifier.CONTAINS, "benjamin ?asero"));
    conditions.add(fFactory.createSearchCondition(field3, SearchSpecifier.CONTAINS, "see the code"));

    Set<String> words = CoreUtils.extractWords(conditions, false);
    assertEquals(7, words.size());
    assertTrue(words.containsAll(Arrays.asList(new String[] { "foo", "bar", "benjamin", "?asero", "see", "the", "code" })));

    words = CoreUtils.extractWords(conditions, true);
    assertEquals(6, words.size());
    assertTrue(words.containsAll(Arrays.asList(new String[] { "foo", "bar", "benjamin", "?asero", "see", "code" })));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testIsEmpty() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));

    INews news = fFactory.createNews(null, feed, new Date());
    assertTrue(CoreUtils.isEmpty(news));

    news.setTitle("Foo");
    assertTrue(CoreUtils.isEmpty(news));

    news.setDescription("Bar");
    assertFalse(CoreUtils.isEmpty(news));

    news.setDescription("Foo");
    assertTrue(CoreUtils.isEmpty(news));
  }

  /**
   * @throws Exception
   */
  @Test
  public void loadSortedSearchMarks() throws Exception {
    IFolder root = fFactory.createFolder(null, null, "Root");
    fFactory.createSearchMark(null, root, "C Search");
    fFactory.createSearchMark(null, root, "A Search");
    fFactory.createSearchMark(null, root, "B Search");

    DynamicDAO.save(root);

    Set<ISearchMark> searches = CoreUtils.loadSortedSearchMarks();
    Iterator<ISearchMark> iterator = searches.iterator();
    assertEquals("A Search", iterator.next().getName());
    assertEquals("B Search", iterator.next().getName());
    assertEquals("C Search", iterator.next().getName());
  }

  /**
   * @throws Exception
   */
  @Test
  public void loadSortedFilters() throws Exception {
    DynamicDAO.save(fFactory.createSearchFilter(null, null, "C Filter"));
    DynamicDAO.save(fFactory.createSearchFilter(null, null, "A Filter"));
    DynamicDAO.save(fFactory.createSearchFilter(null, null, "B Filter"));

    Set<ISearchFilter> filters = CoreUtils.loadSortedNewsFilters();
    Iterator<ISearchFilter> iterator = filters.iterator();
    assertEquals("A Filter", iterator.next().getName());
    assertEquals("B Filter", iterator.next().getName());
    assertEquals("C Filter", iterator.next().getName());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testContainsState() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("feed"));
    INews news = fFactory.createNews(null, feed, new Date());
    news.setState(INews.State.NEW);

    DynamicDAO.save(feed);

    final AtomicInteger mode = new AtomicInteger(0);
    final AtomicInteger counter = new AtomicInteger(0);

    NewsListener listener = null;
    try {
      listener = new NewsAdapter() {
        @Override
        public void entitiesUpdated(Set<NewsEvent> events) {
          assertEquals(1, events.size());

          if (mode.get() == 0) {
            assertTrue(CoreUtils.containsState(events, INews.State.NEW));
            assertFalse(CoreUtils.containsState(events, INews.State.UNREAD));
            counter.incrementAndGet();
          }

          else if (mode.get() == 1) {
            assertTrue(CoreUtils.containsState(events, INews.State.UNREAD));
            assertFalse(CoreUtils.containsState(events, INews.State.NEW));
            counter.incrementAndGet();
          }
        }
      };

      DynamicDAO.addEntityListener(INews.class, listener);

      mode.set(0);
      news.setTitle("Foo");
      DynamicDAO.save(news);

      mode.set(1);
      news.setState(INews.State.UNREAD);
      DynamicDAO.save(news);

      assertEquals(2, counter.get());
    } finally {
      if (listener != null)
        DynamicDAO.removeEntityListener(INews.class, listener);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSplitScope() throws Exception {
    IFolderChild root = DynamicDAO.save(fFactory.createFolder(null, null, "Root"));

    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>();

    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.TITLE, INews.class.getName()), SearchSpecifier.CONTAINS, "foo"));
    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.LOCATION, INews.class.getName()), SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(root))));
    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.LOCATION, INews.class.getName()), SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Collections.singletonList(root))));

    Pair<ISearchCondition, List<ISearchCondition>> result = CoreUtils.splitScope(conditions);
    assertNotNull(result.getFirst());
    assertEquals(2, result.getSecond().size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testIsLocationConflict() throws Exception {
    IFolderChild root = DynamicDAO.save(fFactory.createFolder(null, null, "Root"));

    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>();

    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.TITLE, INews.class.getName()), SearchSpecifier.CONTAINS, "foo"));
    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.LOCATION, INews.class.getName()), SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(root))));
    conditions.add(fFactory.createSearchCondition(fFactory.createSearchField(INews.LOCATION, INews.class.getName()), SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Collections.singletonList(root))));

    assertTrue(CoreUtils.isLocationConflict(conditions));

    conditions.remove(1);
    assertFalse(CoreUtils.isLocationConflict(conditions));

    conditions.clear();
    assertFalse(CoreUtils.isLocationConflict(conditions));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testReparentWithProperties() throws Exception {
    IFolder root = fFactory.createFolder(null, null, "Root");

    IFolder child1 = fFactory.createFolder(null, root, "Child 1");
    IFolder child2 = fFactory.createFolder(null, root, "Child 2");
    child2.setProperty("foo", "bar");
    IFolder child3 = fFactory.createFolder(null, child2, "Child 3");

    root = DynamicDAO.save(root);

    ReparentInfo<IFolderChild, IFolder> info = ReparentInfo.create((IFolderChild) child1, child2, child3, true);

    CoreUtils.reparentWithProperties(Collections.singletonList(info));

    assertEquals("bar", child1.getProperty("foo"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testExtractLinksFromText() throws Exception {
    List<String> links = RegExUtils.extractLinksFromText("", false);
    assertTrue(links.isEmpty());

    links = RegExUtils.extractLinksFromText("foo bar", true);
    assertTrue(links.isEmpty());

    links = RegExUtils.extractLinksFromText("this is a www.rssowl.org short link to www.google.com as well", false);
    assertEquals(2, links.size());
    assertTrue(links.containsAll(Arrays.asList(new String[] { "www.rssowl.org", "www.google.com" })));

    links = RegExUtils.extractLinksFromText("this is a www.rssowl.org short link to www.google.com as well", true);
    assertTrue(links.isEmpty());

    links = RegExUtils.extractLinksFromText("this is a http://www.rssowl.org short link to http://www.google.com as well", true);
    assertEquals(2, links.size());
    assertTrue(links.containsAll(Arrays.asList(new String[] { "http://www.rssowl.org", "http://www.google.com" })));

    links = RegExUtils.extractLinksFromText("this is a feed://www.rssowl.org short link to feed://google.com as well", true);
    assertEquals(2, links.size());
    assertTrue(links.containsAll(Arrays.asList(new String[] { "feed://www.rssowl.org", "feed://google.com" })));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testExistsSearchMark() throws Exception {
    IFolder rootA = fFactory.createFolder(null, null, "Root A");
    IFolder folderA = fFactory.createFolder(null, rootA, "Folder A");

    fFactory.createBookMark(null, rootA, new FeedLinkReference(new URI("bmA")), "BM Root A");
    fFactory.createBookMark(null, folderA, new FeedLinkReference(new URI("bmB")), "BM Folder A");

    fFactory.createNewsBin(null, rootA, "BIN Root A");
    fFactory.createNewsBin(null, folderA, "BIN Folder A");

    ISearchMark smRootA = createSimpleSearchMark(rootA, "SM Root A");
    ISearchMark smRootALocation = createLocationSearchMark(rootA, "SM Root A Location");
    ISearchMark smFolderA = createSimpleSearchMark(folderA, "SM Folder A");
    ISearchMark smFolderALocation = createLocationSearchMark(folderA, "SM Folder A Location");

    rootA = DynamicDAO.save(rootA);

    IFolder rootB = fFactory.createFolder(null, null, "Root B");
    IFolder folderB = fFactory.createFolder(null, rootB, "Folder B");

    fFactory.createBookMark(null, rootB, new FeedLinkReference(new URI("bmC")), "BM Root B");
    fFactory.createBookMark(null, folderB, new FeedLinkReference(new URI("bmD")), "BM Folder B");

    fFactory.createNewsBin(null, rootB, "BIN Root B");
    fFactory.createNewsBin(null, folderB, "BIN Folder B");

    ISearchMark smRootB = createSimpleSearchMark(rootB, "SM Root B");
    ISearchMark smRootBAuthor = createAuthorSearchMark(rootB, "SM Root B Author");
    ISearchMark smFolderB = createSimpleSearchMark(folderB, "SM Folder B");
    ISearchMark smFolderBAuthor = createAuthorSearchMark(folderB, "SM Folder B Author");

    rootB = DynamicDAO.save(rootB);

    /* Start Testing */
    IFolder rootACopy = fFactory.createFolder(null, null, "Root A");
    IFolder folderACopy = fFactory.createFolder(null, rootACopy, "Folder A");

    fFactory.createNewsBin(null, rootA, "BIN Root A");
    fFactory.createNewsBin(null, folderACopy, "BIN Folder A");

    ISearchMark smRootACopy = createSimpleSearchMark(rootACopy, "SM Root A");
    ISearchMark smRootACopyLocation = createLocationSearchMark(rootACopy, "SM Root A Location");
    ISearchMark smFolderACopy = createSimpleSearchMark(folderACopy, "SM Folder A");
    ISearchMark smFolderACopyLocation = createLocationSearchMark(folderACopy, "SM Folder A Location");

    IFolder rootBCopy = fFactory.createFolder(null, null, "Root B");
    IFolder folderBCopy = fFactory.createFolder(null, rootBCopy, "Folder B");

    fFactory.createNewsBin(null, rootB, "BIN Root B");
    fFactory.createNewsBin(null, folderBCopy, "BIN Folder B");

    ISearchMark smRootBCopy = createSimpleSearchMark(rootBCopy, "SM Root B");
    ISearchMark smRootBAuthorCopy = createAuthorSearchMark(rootBCopy, "SM Root B Other");
    ISearchMark smFolderBCopy = createSimpleSearchMark(folderBCopy, "SM Folder B");
    ISearchMark smFolderBAuthorCopy = createSimpleSearchMark(folderBCopy, "SM Folder B");

    IFolder rootC = fFactory.createFolder(null, null, "Root C");
    IFolder folderC = fFactory.createFolder(null, rootC, "Folder C");

    fFactory.createNewsBin(null, rootC, "BIN Root C");
    fFactory.createNewsBin(null, folderC, "BIN Folder C");

    ISearchMark smRootC = createSimpleSearchMark(rootC, "SM Root C");
    ISearchMark smFolderC = createSimpleSearchMark(folderC, "SM Folder C");

    assertTrue(smRootA.getId() != -1);
    assertTrue(smRootALocation.getId() != -1);
    assertTrue(smFolderA.getId() != -1);
    assertTrue(smFolderALocation.getId() != -1);
    assertTrue(smRootB.getId() != -1);
    assertTrue(smRootBAuthor.getId() != -1);
    assertTrue(smFolderB.getId() != -1);
    assertTrue(smFolderBAuthor.getId() != -1);

    assertTrue(CoreUtils.existsSearchMark(smRootACopy));
    assertFalse(CoreUtils.existsSearchMark(smRootACopyLocation));
    assertTrue(CoreUtils.existsSearchMark(smFolderACopy));
    assertFalse(CoreUtils.existsSearchMark(smFolderACopyLocation));

    assertTrue(CoreUtils.existsSearchMark(smRootBCopy));
    assertTrue(CoreUtils.existsSearchMark(smRootBAuthorCopy));
    assertTrue(CoreUtils.existsSearchMark(smFolderBCopy));
    assertTrue(CoreUtils.existsSearchMark(smFolderBAuthorCopy));

    assertFalse(CoreUtils.existsSearchMark(smRootC));
    assertFalse(CoreUtils.existsSearchMark(smFolderC));
  }

  private ISearchMark createSimpleSearchMark(IFolder parent, String name) {
    ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foo bar");

    ISearchMark sm = fFactory.createSearchMark(null, parent, name);
    sm.addSearchCondition(condition);

    return sm;
  }

  private ISearchMark createAuthorSearchMark(IFolder parent, String name) {
    ISearchField field = fFactory.createSearchField(INews.AUTHOR, INews.class.getName());
    ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, "bar");

    ISearchMark sm = fFactory.createSearchMark(null, parent, name);
    sm.addSearchCondition(condition);

    return sm;
  }

  private ISearchMark createLocationSearchMark(IFolder parent, String name) {
    ISearchField field = fFactory.createSearchField(INews.LOCATION, INews.class.getName());
    ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList((IFolderChild) parent)));

    ISearchMark sm = fFactory.createSearchMark(null, parent, name);
    sm.addSearchCondition(condition);

    return sm;
  }

  /**
   * @throws Exception
   */
  @Test
  public void testExistsNewsBin() throws Exception {
    IFolder rootA = fFactory.createFolder(null, null, "Root A");
    IFolder folderA = fFactory.createFolder(null, rootA, "Folder A");

    fFactory.createBookMark(null, rootA, new FeedLinkReference(new URI("bmA")), "BM Root A");
    fFactory.createBookMark(null, folderA, new FeedLinkReference(new URI("bmB")), "BM Folder A");

    INewsBin binRootA = fFactory.createNewsBin(null, rootA, "BIN Root A");
    INewsBin binFolderA = fFactory.createNewsBin(null, folderA, "BIN Folder A");

    createSimpleSearchMark(rootA, "SM Root A");
    createSimpleSearchMark(folderA, "SM Folder A");

    rootA = DynamicDAO.save(rootA);

    IFolder rootB = fFactory.createFolder(null, null, "Root B");
    IFolder folderB = fFactory.createFolder(null, rootB, "Folder B");

    fFactory.createBookMark(null, rootB, new FeedLinkReference(new URI("bmC")), "BM Root B");
    fFactory.createBookMark(null, folderB, new FeedLinkReference(new URI("bmD")), "BM Folder B");

    INewsBin binRootB = fFactory.createNewsBin(null, rootB, "BIN Root B");
    INewsBin binFolderB = fFactory.createNewsBin(null, folderB, "BIN Folder B");

    createSimpleSearchMark(rootB, "SM Root B");
    createSimpleSearchMark(folderB, "SM Folder B");

    rootB = DynamicDAO.save(rootB);

    /* Start Testing */
    IFolder rootACopy = fFactory.createFolder(null, null, "Root A");
    IFolder folderACopy = fFactory.createFolder(null, rootACopy, "Folder A");

    INewsBin binRootACopy = fFactory.createNewsBin(null, rootA, "BIN Root A");
    INewsBin binFolderACopy = fFactory.createNewsBin(null, folderACopy, "BIN Folder A");

    createSimpleSearchMark(rootACopy, "SM Root A");
    createSimpleSearchMark(folderACopy, "SM Folder A");

    IFolder rootBCopy = fFactory.createFolder(null, null, "Root B");
    IFolder folderBCopy = fFactory.createFolder(null, rootBCopy, "Folder B");

    INewsBin binRootBCopy = fFactory.createNewsBin(null, rootB, "BIN Root B");
    INewsBin binFolderBCopy = fFactory.createNewsBin(null, folderBCopy, "BIN Folder B");

    createSimpleSearchMark(rootBCopy, "SM Root B");
    createSimpleSearchMark(folderBCopy, "SM Folder B");

    IFolder rootC = fFactory.createFolder(null, null, "Root C");
    IFolder folderC = fFactory.createFolder(null, rootC, "Folder C");

    INewsBin binRootC = fFactory.createNewsBin(null, rootC, "BIN Root C");
    INewsBin binFolderC = fFactory.createNewsBin(null, folderC, "BIN Folder C");

    IFolder rootD = fFactory.createFolder(null, null, "Root D");
    rootD.setProperty(ITypeImporter.TEMPORARY_FOLDER, true);
    IFolder folderD = fFactory.createFolder(null, rootD, "Folder A");

    INewsBin binRootD = fFactory.createNewsBin(null, rootD, "BIN Root A");
    INewsBin binFolderD = fFactory.createNewsBin(null, folderD, "BIN Folder A");

    assertTrue(binRootA.getId() != -1);
    assertTrue(binFolderA.getId() != -1);
    assertTrue(binRootB.getId() != -1);
    assertTrue(binFolderB.getId() != -1);

    assertTrue(CoreUtils.existsNewsBin(binRootACopy));
    assertTrue(CoreUtils.existsNewsBin(binFolderACopy));

    assertTrue(CoreUtils.existsNewsBin(binRootBCopy));
    assertTrue(CoreUtils.existsNewsBin(binFolderBCopy));

    assertFalse(CoreUtils.existsNewsBin(binRootC));
    assertFalse(CoreUtils.existsNewsBin(binFolderC));

    assertTrue(CoreUtils.existsNewsBin(binRootD));
    assertTrue(CoreUtils.existsNewsBin(binFolderD));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFindFolder() throws Exception {
    IFolder rootA = fFactory.createFolder(null, null, "Root A");
    IFolder folderA = fFactory.createFolder(null, rootA, "Folder A");

    fFactory.createBookMark(null, rootA, new FeedLinkReference(new URI("bmA")), "BM Root A");
    fFactory.createBookMark(null, folderA, new FeedLinkReference(new URI("bmB")), "BM Folder A");

    fFactory.createNewsBin(null, rootA, "BIN Root A");
    fFactory.createNewsBin(null, folderA, "BIN Folder A");

    createSimpleSearchMark(rootA, "SM Root A");
    createSimpleSearchMark(folderA, "SM Folder A");

    rootA = DynamicDAO.save(rootA);

    IFolder rootB = fFactory.createFolder(null, null, "Root B");
    IFolder folderB = fFactory.createFolder(null, rootB, "Folder B");

    fFactory.createBookMark(null, rootB, new FeedLinkReference(new URI("bmC")), "BM Root B");
    fFactory.createBookMark(null, folderB, new FeedLinkReference(new URI("bmD")), "BM Folder B");

    fFactory.createNewsBin(null, rootB, "BIN Root B");
    fFactory.createNewsBin(null, folderB, "BIN Folder B");

    createSimpleSearchMark(rootB, "SM Root B");
    createSimpleSearchMark(folderB, "SM Folder B");

    rootB = DynamicDAO.save(rootB);

    /* Start Testing */
    IFolder rootACopy = fFactory.createFolder(null, null, "Root A");
    IFolder folderACopy = fFactory.createFolder(null, rootACopy, "Folder A");

    IFolder rootBCopy = fFactory.createFolder(null, null, "Root B");
    IFolder folderBCopy = fFactory.createFolder(null, rootBCopy, "Folder B");

    IFolder rootC = fFactory.createFolder(null, null, "Root C");
    IFolder folderC = fFactory.createFolder(null, rootC, "Folder C");

    IFolder rootD = fFactory.createFolder(null, null, "Root D");
    rootD.setProperty(ITypeImporter.TEMPORARY_FOLDER, true);
    IFolder folderD = fFactory.createFolder(null, rootD, "Folder A");

    assertTrue(rootA.getId() != -1);
    assertTrue(folderA.getId() != -1);
    assertTrue(rootB.getId() != -1);
    assertTrue(folderB.getId() != -1);

    assertEquals(rootA, CoreUtils.findFolder(rootACopy));
    assertEquals(folderA, CoreUtils.findFolder(folderACopy));

    assertEquals(rootB, CoreUtils.findFolder(rootBCopy));
    assertEquals(folderB, CoreUtils.findFolder(folderBCopy));

    assertNull(CoreUtils.findFolder(rootC));
    assertNull(CoreUtils.findFolder(folderC));

    assertEquals(rootA, CoreUtils.findFolder(rootD));
    assertEquals(folderA, CoreUtils.findFolder(folderD));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFindFaviconAbsolute() throws Exception {
    StringReader reader = new StringReader("<html><link rel=\"shortcut icon\" href=\"http://www.rssowl.org/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href='http://www.rssowl.org/favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href=http://www.rssowl.org/favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href=\"http://www.rssowl.org/favicon.ico\" /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href= \"http://www.rssowl.org/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href =\"http://www.rssowl.org/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href = \"http://www.rssowl.org/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" HREF=\"http://www.rssowl.org/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href= 'http://www.rssowl.org/favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href ='http://www.rssowl.org/favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href = 'http://www.rssowl.org/favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href =http://www.rssowl.org/favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href= http://www.rssowl.org/favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href = http://www.rssowl.org/favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link href=\"http://www.rssowl.org/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), null).toString());

    reader = new StringReader("<html><link hreff=\"http://www.rssowl.org/favicon.ico\"/></html>");
    assertEquals(null, CoreUtils.findFavicon(new BufferedReader(reader), null));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFindFaviconRelative() throws Exception {
    StringReader reader = new StringReader("<html><link rel=\"shortcut icon\" href=\"/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href='/favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href=/favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href=\"/favicon.ico\" /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href= \"/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href =\"/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href = \"/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" HREF=\"/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href= '/favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href ='/favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href = '/favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href =/favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href= /favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href = /favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link href=\"/favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href=\"favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href='favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href=favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href=\"favicon.ico\" /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href= \"favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href =\"favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href = \"favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" HREF=\"favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href= 'favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href ='favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href = 'favicon.ico'/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href =favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href= favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link rel=\"shortcut icon\" href = favicon.ico /></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link href=\"favicon.ico\"/></html>");
    assertEquals("http://www.rssowl.org/favicon.ico", CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")).toString());

    reader = new StringReader("<html><link hreff=\"favicon.ico\"/></html>");
    assertEquals(null, CoreUtils.findFavicon(new BufferedReader(reader), URI.create("http://www.rssowl.org")));
  }

  /**
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testReplace() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("rssowl.org"));

    INews news1 = fFactory.createNews(null, feed, new Date());
    news1.setTitle("news1");

    INews news2 = fFactory.createNews(null, feed, new Date());
    news2.setTitle("news2");

    INews news3 = fFactory.createNews(null, feed, new Date());
    news3.setTitle("news3");

    assertEquals(null, CoreUtils.replace(null, null));
    assertEquals(null, CoreUtils.replace(null, Collections.EMPTY_MAP));
    assertEquals(0, CoreUtils.replace(Collections.EMPTY_LIST, null).size());
    assertEquals(0, CoreUtils.replace(Collections.EMPTY_LIST, Collections.EMPTY_MAP).size());

    Map<INews, INews> replacement= new HashMap<INews, INews>();
    replacement.put(news1, news3);
    replacement.put(news3, news1);

    List<INews> allNews= new ArrayList<INews>();
    allNews.add(news1);
    allNews.add(news2);
    allNews.add(news3);

    List<INews> replacedNews = CoreUtils.replace(allNews, replacement);
    assertEquals(3, replacedNews.size());
    assertEquals("news3", replacedNews.get(0).getTitle());
    assertEquals("news2", replacedNews.get(1).getTitle());
    assertEquals("news1", replacedNews.get(2).getTitle());
  }
}
