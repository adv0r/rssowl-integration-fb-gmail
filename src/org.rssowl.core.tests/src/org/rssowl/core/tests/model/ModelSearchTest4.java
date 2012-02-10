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

package org.rssowl.core.tests.model;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.search.ModelSearchQueries;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.tests.TestUtils;
import org.rssowl.core.util.SearchHit;
import org.rssowl.ui.internal.util.ModelUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

/**
 * Test searching types from the persistence layer.
 *
 * @author bpasero
 */
public class ModelSearchTest4 extends AbstractModelSearchTest {

  /**
   * @throws Exception
   */
  @Test
  public void testPhraseSearch_CONTAINS() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      /* Title */
      INews news1 = createNews(feed, "Johnny lives hungry Depp", "http://www.news.com/news1.html", State.NEW);

      /* Description */
      INews news2 = createNews(feed, "News2", "http://www.news.com/news2.html", State.NEW);
      news2.setDescription("This is a longer name like Michael Jackson.");

      /* Author */
      INews news3 = createNews(feed, "News3", "http://www.news.com/news3.html", State.NEW);
      IPerson author = fFactory.createPerson(null, news3);
      author.setName("Arnold Schwarzenegger");

      /* Category */
      INews news4 = createNews(feed, "lives", "http://www.news.com/news4.html", State.NEW);
      ICategory category = fFactory.createCategory(null, news4);
      category.setName("Roberts");

      /* Attachment Content */
      INews news5 = createNews(feed, "hungry", "http://www.news.com/news5.html", State.NEW);
      IAttachment attachment = fFactory.createAttachment(null, news5);
      attachment.setLink(new URI("http://www.attachment.com/att1news2.file"));
      attachment.setType("Hasselhoff");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), false);
        assertSame(result, news1, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), true);
        assertTrue(result.isEmpty());
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"lives hungry\" lives hungry");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"lives hungry\" lives hungry");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news4, news5);
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPhraseSearch_CONTAINS_ALL() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      /* Title */
      INews news1 = createNews(feed, "Johnny lives hungry Depp", "http://www.news.com/news1.html", State.NEW);

      /* Description */
      INews news2 = createNews(feed, "News2", "http://www.news.com/news2.html", State.NEW);
      news2.setDescription("This is a longer name like Michael Jackson.");

      /* Author */
      INews news3 = createNews(feed, "News3", "http://www.news.com/news3.html", State.NEW);
      IPerson author = fFactory.createPerson(null, news3);
      author.setName("Arnold Schwarzenegger");

      /* Category */
      INews news4 = createNews(feed, "lives", "http://www.news.com/news4.html", State.NEW);
      ICategory category = fFactory.createCategory(null, news4);
      category.setName("Roberts");

      /* Attachment Content */
      INews news5 = createNews(feed, "hungry", "http://www.news.com/news5.html", State.NEW);
      IAttachment attachment = fFactory.createAttachment(null, news5);
      attachment.setLink(new URI("http://www.attachment.com/att1news2.file"));
      attachment.setType("Hasselhoff");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), false);
        assertSame(result, news1, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), true);
        assertTrue(result.isEmpty());
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"lives hungry\" lives hungry");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"lives hungry\" lives hung");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertTrue(result.isEmpty());
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPhraseSearch_CONTAINS_NOT() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      /* Title */
      INews news1 = createNews(feed, "Johnny lives hungry Depp", "http://www.news.com/news1.html", State.NEW);

      /* Description */
      INews news2 = createNews(feed, "News2", "http://www.news.com/news2.html", State.NEW);
      news2.setDescription("This is a longer name like Michael Jackson.");

      /* Author */
      INews news3 = createNews(feed, "News3", "http://www.news.com/news3.html", State.NEW);
      IPerson author = fFactory.createPerson(null, news3);
      author.setName("Arnold Schwarzenegger");

      /* Category */
      INews news4 = createNews(feed, "lives", "http://www.news.com/news4.html", State.NEW);
      ICategory category = fFactory.createCategory(null, news4);
      category.setName("Roberts");

      /* Attachment Content */
      INews news5 = createNews(feed, "hungry", "http://www.news.com/news5.html", State.NEW);
      IAttachment attachment = fFactory.createAttachment(null, news5);
      attachment.setLink(new URI("http://www.attachment.com/att1news2.file"));
      attachment.setType("Hasselhoff");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), true);
        assertSame(result, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"lives hungry\" lives hungry");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"lives hungry\" lives hungry");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testLocationSearch_BINs() throws Exception {
    IFolderChild root = fFactory.createFolder(null, null, "Root");
    DynamicDAO.save(root);

    IFolderChild child = fFactory.createFolder(null, (IFolder) root, "Child");
    DynamicDAO.save(child);

    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));
    INews news1 = createNews(feed, "Title", "http://www.news.com/news1.html", State.NEW);
    DynamicDAO.save(feed);

    IFolderChild mark = fFactory.createBookMark(null, (IFolder) child, new FeedLinkReference(feed.getLink()), "Mark");
    DynamicDAO.save(mark);

    IFolderChild bin = fFactory.createNewsBin(null, (IFolder) root, "Bin");
    DynamicDAO.save(bin);
    News copiedNews = new News((News) news1, bin.getId().longValue());
    DynamicDAO.save(copiedNews);
    DynamicDAO.save(bin);

    /* Wait for Indexer */
    waitForIndexer();

    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(mark)));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
      assertEquals(1, result.size());
      assertSame(result, news1);
    }

    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(bin)));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
      assertEquals(1, result.size());
      assertSame(result, copiedNews);
    }

    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(child)));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
      assertEquals(1, result.size());
      assertSame(result, news1);
    }

    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(root)));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
      assertEquals(2, result.size());
      assertSame(result, news1, copiedNews);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testHiddenDeletedNewsNotIndexed_1() throws Exception {

    /* First add some Types */
    IFeed feed1 = fFactory.createFeed(null, new URI("http://www.feed.com/feed1.xml"));

    createNews(feed1, "First News of Feed One", "http://www.news.com/news1.html", State.HIDDEN);
    createNews(feed1, "Second News of Feed One", "http://www.news.com/news2.html", State.DELETED);

    DynamicDAO.save(feed1);

    /* Wait for Indexer */
    waitForIndexer();

    ISearchField field1 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
    ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, "http://www.feed.com/feed1.xml");

    List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
    assertEquals(0, result.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testHiddenDeletedNewsNotIndexed_2() throws Exception {

    /* First add some Types */
    IFeed feed1 = fFactory.createFeed(null, new URI("http://www.feed.com/feed1.xml"));

    INews news1 = createNews(feed1, "First News of Feed One", "http://www.news.com/news1.html", State.NEW);
    INews news2 = createNews(feed1, "Second News of Feed One", "http://www.news.com/news2.html", State.UNREAD);

    DynamicDAO.save(feed1);

    /* Wait for Indexer */
    waitForIndexer();

    news1.setState(INews.State.HIDDEN);
    news2.setState(INews.State.DELETED);

    DynamicDAO.save(feed1);

    /* Wait for Indexer */
    waitForIndexer();

    ISearchField field1 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
    ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, "http://www.feed.com/feed1.xml");

    List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
    assertEquals(0, result.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRestoredNewsSearchable() throws Exception {

    /* First add some Types */
    IFeed feed1 = fFactory.createFeed(null, new URI("http://www.feed.com/feed1.xml"));

    INews news1 = createNews(feed1, "First News of Feed One", "http://www.news.com/news1.html", State.HIDDEN);
    INews news2 = createNews(feed1, "Second News of Feed One", "http://www.news.com/news2.html", State.HIDDEN);

    DynamicDAO.save(feed1);

    /* Wait for Indexer */
    waitForIndexer();

    news1.setState(INews.State.NEW);
    news2.setState(INews.State.UNREAD);

    DynamicDAO.save(feed1);

    /* Wait for Indexer */
    waitForIndexer();

    ISearchField field1 = fFactory.createSearchField(INews.FEED, fNewsEntityName);
    ISearchCondition cond1 = fFactory.createSearchCondition(field1, SearchSpecifier.IS, "http://www.feed.com/feed1.xml");

    List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
    assertSame(result, news1, news2);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testScopeSearch() throws Exception {

    /* First add some Types */
    IFolder rootFolder = fFactory.createFolder(null, null, "Root");
    DynamicDAO.save(rootFolder);

    IFolder subFolder1 = fFactory.createFolder(null, rootFolder, "Sub Folder 1");
    DynamicDAO.save(subFolder1);

    IFolder subFolder2 = fFactory.createFolder(null, rootFolder, "Sub Folder 2");
    DynamicDAO.save(subFolder2);

    IFeed feed1 = fFactory.createFeed(null, new URI("http://www.feed.com/feed1.xml"));
    IFeed feed2 = fFactory.createFeed(null, new URI("http://www.feed.com/feed2.xml"));
    IFeed feed3 = fFactory.createFeed(null, new URI("http://www.feed.com/feed3.xml"));

    INews news1 = createNews(feed1, "First News of Feed One", "http://www.news.com/news1.html", State.NEW);
    INews news2 = createNews(feed2, "First News of Feed Two", "http://www.news.com/news2.html", State.UNREAD);
    INews news3 = createNews(feed3, "First News of Feed Three", "http://www.news.com/news3.html", State.UNREAD);
    news3.setFlagged(true);

    DynamicDAO.save(feed1);
    DynamicDAO.save(feed2);
    DynamicDAO.save(feed3);

    IBookMark bm1 = fFactory.createBookMark(null, rootFolder, new FeedLinkReference(feed1.getLink()), "BM1");
    IBookMark bm2 = fFactory.createBookMark(null, subFolder1, new FeedLinkReference(feed2.getLink()), "BM2");
    IBookMark bm3 = fFactory.createBookMark(null, subFolder2, new FeedLinkReference(feed3.getLink()), "BM3");

    DynamicDAO.save(bm1);
    DynamicDAO.save(bm2);
    DynamicDAO.save(bm3);

    /* Wait for Indexer */
    waitForIndexer();

    ISearchField fieldLoc = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
    ISearchField fieldState = fFactory.createSearchField(INews.STATE, fNewsEntityName);
    ISearchField fieldIsFlagged = fFactory.createSearchField(INews.IS_FLAGGED, fNewsEntityName);
    ISearchField fieldAllFields = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);

    /* Search Test: Simple */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootFolder })));
      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertSame(result, news1, news2, news3);
    }

    /* Search Test: Simple */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subFolder1 })));
      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertSame(result, news2);
    }

    /* Search Test: Simple */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subFolder2 })));
      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertSame(result, news3);
    }

    /* Search Test: Simple */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subFolder1, subFolder2 })));
      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertSame(result, news2, news3);
    }

    /* Search Test: Simple */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, bm2, bm3 })));
      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertSame(result, news1, news2, news3);
    }

    /* Search Test: Simple */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1 })));
      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertSame(result, news1);
    }

    /* Search Test: Simple */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, subFolder2 })));
      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertSame(result, news1, news3);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootFolder })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), false);
      assertSame(result, news1);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootFolder })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), true);
      assertSame(result, news1);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootFolder, subFolder2 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), false);
      assertSame(result, news1);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), false);
      assertTrue(result.isEmpty());
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm2, bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), false);
      assertTrue(result.isEmpty());
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, new Long[][] { { null }, { null }, { null } });

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1), false);
      assertTrue(result.isEmpty());
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, new Long[][] { { null }, { null }, { null } });
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW, INews.State.UNREAD));

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2), false);
      assertTrue(result.isEmpty());
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootFolder })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "Three");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), false);
      assertSame(result, news1, news3);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootFolder })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "Three");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertTrue(result.isEmpty());
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { rootFolder })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "One");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertSame(result, news1);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "Three");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), false);
      assertSame(result, news1);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "Three");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertTrue(result.isEmpty());
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "One");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertSame(result, news1);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, bm2, bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "Three");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), false);
      assertSame(result, news1, news3);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, bm2, bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "Three");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertTrue(result.isEmpty());
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, bm2, bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "One");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertSame(result, news1);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, bm2, bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS_ALL, "Three");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), false);
      assertSame(result, news1, news3);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, bm2, bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS_ALL, "Three");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertTrue(result.isEmpty());
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, bm2, bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS_ALL, "One");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertSame(result, news1);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, bm2, bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS_NOT, "Three");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), false);
      assertSame(result, news1, news2);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, bm2, bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS_NOT, "Three");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertSame(result, news1);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1, bm2, bm3 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS_NOT, "One");

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3), true);
      assertTrue(result.isEmpty());
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { bm1 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "Three");
      ISearchCondition cond4 = fFactory.createSearchCondition(fieldIsFlagged, SearchSpecifier.IS, true);

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3, cond4), false);
      assertSame(result, news1);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subFolder1, subFolder2, bm1 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.NEW));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "Three");
      ISearchCondition cond4 = fFactory.createSearchCondition(fieldIsFlagged, SearchSpecifier.IS, true);

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3, cond4), false);
      assertSame(result, news1, news3);
    }

    /* Search Test: Complex */
    {
      ISearchCondition cond1 = fFactory.createSearchCondition(fieldLoc, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { subFolder1, subFolder2, bm1 })));
      ISearchCondition cond2 = fFactory.createSearchCondition(fieldState, SearchSpecifier.IS, EnumSet.of(INews.State.UNREAD));
      ISearchCondition cond3 = fFactory.createSearchCondition(fieldAllFields, SearchSpecifier.CONTAINS, "Three");
      ISearchCondition cond4 = fFactory.createSearchCondition(fieldIsFlagged, SearchSpecifier.IS, true);

      List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(cond1, cond2, cond3, cond4), true);
      assertSame(result, news3);
    }
  }

  /**
   * See http://dev.rssowl.org/show_bug.cgi?id=1122
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testSearchNewsWithPhraseInCategory() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      INews news = createNews(feed, "Friend", "http://www.news.com/news3.html", State.UNREAD);
      ICategory category = fFactory.createCategory(null, news);
      category.setName("Global");
      news.addCategory(category);

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      /* Condition 1 */
      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);

        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Giant Global Graph\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1), false);
        assertEquals(0, result.size());
      }

      /* Condition 2 */
      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);

        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"Giant Global Graph\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1), false);
        assertEquals(0, result.size());
      }

      /* Condition 1 */
      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);

        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "Giant Global Graph");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1), false);
        assertEquals(0, result.size());
      }

      /* Condition 1 */
      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);

        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "Giant Global Graph");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1), false);
        assertEquals(1, result.size());
        assertSame(result, news);
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSearchNewsWithInvalidLocation() throws Exception {

    /* First add some Types */
    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

    INews news = createNews(feed, "Friend", "http://www.news.com/news3.html", State.UNREAD);
    ICategory category = fFactory.createCategory(null, news);
    category.setName("Global");
    news.addCategory(category);

    DynamicDAO.save(feed);

    IFolder root = fFactory.createFolder(null, null, "Root");
    fFactory.createBookMark(null, root, new FeedLinkReference(feed.getLink()), "Bookmark");
    DynamicDAO.save(root);

    /* Wait for Indexer */
    waitForIndexer();

    ISearchField field = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);

    ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.IS, new Long[][] { { 10l }, { 20l }, { 30l } });

    List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1), false);
    assertEquals(0, result.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testNewsReindexedWhenLabelChanges() throws Exception {
    ILabel label = DynamicDAO.save(fFactory.createLabel(null, "Foo"));

    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));
    INews news = createNews(feed, "News with Label", "http://www.news.com/news3.html", State.UNREAD);
    news.addLabel(label);
    DynamicDAO.save(feed);

    waitForIndexer();

    ISearchField field = fFactory.createSearchField(INews.LABEL, fNewsEntityName);

    ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.IS, "foo");

    List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1), false);
    assertEquals(1, result.size());
    assertEquals("News with Label", result.get(0).getResult().resolve().getTitle());

    label.setName("Bar");
    DynamicDAO.save(label);

    waitForIndexer();

    condition1 = fFactory.createSearchCondition(field, SearchSpecifier.IS, "bar");

    result = fModelSearch.searchNews(list(condition1), false);
    assertEquals(1, result.size());
    assertEquals("News with Label", result.get(0).getResult().resolve().getTitle());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testMaxClauseCount() throws Exception {
    int maxClauseCount = BooleanQuery.getMaxClauseCount();

    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));
    createNews(feed, "Foo", "http://www.news.com/news3.html", State.UNREAD);
    DynamicDAO.save(feed);

    waitForIndexer();

    ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);

    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>();
    for (int i = 0; i < 1030; i++) {
      ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foo" + i);
      conditions.add(condition1);
    }

    conditions.add(fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foo"));

    List<SearchHit<NewsReference>> result = fModelSearch.searchNews(conditions, false);
    assertEquals(1, result.size());
    assertEquals("Foo", result.get(0).getResult().resolve().getTitle());

    BooleanQuery.setMaxClauseCount(maxClauseCount);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testMaxClauseCountForQuery() throws Exception {
    int maxClauseCount = BooleanQuery.getMaxClauseCount();
    BooleanQuery.setMaxClauseCount(3);

    IFolderChild root = fFactory.createFolder(null, null, "Root");
    IFeed feed1 = DynamicDAO.save(fFactory.createFeed(null, new URI("http://www.feed.com/feed1.xml")));
    IFeed feed2 = DynamicDAO.save(fFactory.createFeed(null, new URI("http://www.feed.com/feed2.xml")));
    IFeed feed3 = DynamicDAO.save(fFactory.createFeed(null, new URI("http://www.feed.com/feed3.xml")));
    IFeed feed4 = DynamicDAO.save(fFactory.createFeed(null, new URI("http://www.feed.com/feed4.xml")));

    DynamicDAO.save(fFactory.createBookMark(null, (IFolder) root, new FeedLinkReference(feed1.getLink()), "BM1"));
    DynamicDAO.save(fFactory.createBookMark(null, (IFolder) root, new FeedLinkReference(feed2.getLink()), "BM1"));
    DynamicDAO.save(fFactory.createBookMark(null, (IFolder) root, new FeedLinkReference(feed3.getLink()), "BM1"));
    DynamicDAO.save(fFactory.createBookMark(null, (IFolder) root, new FeedLinkReference(feed4.getLink()), "BM1"));

    ISearchField field = fFactory.createSearchField(INews.LOCATION, fNewsEntityName);
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>();
    conditions.add(fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Collections.singletonList(root))));

    Query query = ModelSearchQueries.createQuery(conditions, false);
    assertNotNull(query);

    BooleanQuery.setMaxClauseCount(maxClauseCount);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testReindexAll() throws Exception {
    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));
    createNews(feed, "Foo", "http://www.news.com/news.html", State.NEW);
    DynamicDAO.save(feed);

    waitForIndexer();

    ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
    ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foo");

    List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
    assertEquals("Foo", result.get(0).getResult().resolve().getTitle());

    fModelSearch.reindexAll(new NullProgressMonitor());
    fModelSearch.optimize();

    waitForIndexer();

    fModelSearch.shutdown(false);
    fModelSearch.startup();

    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
    assertEquals("Foo", result.get(0).getResult().resolve().getTitle());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSearchNewsByAge() throws Exception {

    /* First add some Types */
    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

    INews news_1_Minute = fFactory.createNews(null, feed, new Date());
    news_1_Minute.setPublishDate(new Date(System.currentTimeMillis() - 1 * MINUTE));

    INews news_2_Minutes = fFactory.createNews(null, feed, new Date());
    news_2_Minutes.setPublishDate(new Date(System.currentTimeMillis() - 2 * MINUTE));

    INews news_1_Hour = fFactory.createNews(null, feed, new Date());
    news_1_Hour.setPublishDate(new Date(System.currentTimeMillis() - 60 * MINUTE));

    INews news_2_Hours = fFactory.createNews(null, feed, new Date());
    news_2_Hours.setPublishDate(new Date(System.currentTimeMillis() - 120 * MINUTE));

    INews news_1_Day = fFactory.createNews(null, feed, new Date());
    news_1_Day.setPublishDate(new Date(System.currentTimeMillis() - 1 * DAY - 1 * MINUTE));

    INews news_2_Days = fFactory.createNews(null, feed, new Date());
    news_2_Days.setPublishDate(new Date(System.currentTimeMillis() - 2 * DAY));

    DynamicDAO.save(feed);

    /* Wait for Indexer */
    waitForIndexer();

    ISearchField ageField = fFactory.createSearchField(INews.AGE_IN_DAYS, fNewsEntityName);

    /* 1 Minute */
    ISearchCondition condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS, -1);
    List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getResult().references(news_1_Minute));

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_LESS_THAN, -1);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(0, result.size());

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_GREATER_THAN, -1);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(5, result.size());

    /* 2 Minutes */
    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS, -2);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getResult().references(news_2_Minutes));

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_LESS_THAN, -2);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getResult().references(news_1_Minute));

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_GREATER_THAN, -2);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(4, result.size());

    /* 1 Hour */
    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS, -60);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getResult().references(news_1_Hour));

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_LESS_THAN, -60);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(2, result.size());

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_GREATER_THAN, -60);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(3, result.size());

    /* 2 Hours */
    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS, -120);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getResult().references(news_2_Hours));

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_LESS_THAN, -120);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(3, result.size());

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_GREATER_THAN, -120);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(2, result.size());

    /* 1 Day */
    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS, 1);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getResult().references(news_1_Day));

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_LESS_THAN, 1);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(4, result.size());

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_GREATER_THAN, 1);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getResult().references(news_2_Days));

    /* 2 Days */
    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS, 2);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getResult().references(news_2_Days));

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_LESS_THAN, 2);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(5, result.size());

    condition = fFactory.createSearchCondition(ageField, SearchSpecifier.IS_GREATER_THAN, 2);
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(0, result.size());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSearchNewsWithOddDoubleQuotes() throws Exception {

    /* First add some Types */
    IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));
    createNews(feed, "Hello World", "http://www.news.com/news1.html", State.UNREAD);

    DynamicDAO.save(feed);

    /* Wait for Indexer */
    waitForIndexer();

    ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);

    ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "Hello");
    List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());

    condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "Hello World");
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());

    condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Hello World\"");
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());

    condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "Hello\" World");
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());

    condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "Hello\" \"World");
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());

    condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Hello World\"\"");
    result = fModelSearch.searchNews(list(condition), false);
    assertEquals(1, result.size());
  }
}
