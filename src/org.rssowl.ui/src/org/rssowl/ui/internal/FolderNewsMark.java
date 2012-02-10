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

package org.rssowl.ui.internal;

import org.rssowl.core.internal.persist.Mark;
import org.rssowl.core.internal.persist.NewsContainer;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.ModelReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.PersistenceException;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An internal subclass of {@link Mark} that implements {@link INewsMark} to
 * provide the news of all bookmarks, bins and saved searches inside a folder.
 * The {@link FolderNewsMark} is created dynamically whenever a folder is opened
 * in the feedview and is never persisted to the DB.
 * <p>
 * TODO This class is not very good in terms of performance because it has to
 * load all news of the folder on the fly to produce the news container. When then
 * opened from the feedview, these news get resolved again and thereby twice.
 * </p>
 *
 * @author bpasero
 */
@SuppressWarnings("restriction")
public class FolderNewsMark extends Mark implements INewsMark {
  private NewsContainer fNewsContainer;
  private final IFolder fFolder;

  /**
   * Internal implementation of the <code>ModelReference</code> for the internal
   * Type <code>FolderNewsMark</code>.
   *
   * @author bpasero
   */
  public static final class FolderNewsMarkReference extends ModelReference {

    /**
     * @param id
     */
    public FolderNewsMarkReference(long id) {
      super(id, FolderNewsMark.class);
    }

    @Override
    public IFolder resolve() throws PersistenceException {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * @param folder
   */
  public FolderNewsMark(IFolder folder) {
    super(folder.getId(), folder.getParent(), folder.getName());
    fFolder = folder;
    fNewsContainer = new NewsContainer(Collections.<INews.State, Boolean> emptyMap());
    fillNews(folder);
  }

  private void fillNews(IFolder folder) {
    List<IFolderChild> children = folder.getChildren();
    for (IFolderChild child : children) {
      if (child instanceof INewsMark) {
        INewsMark newsmark = (INewsMark) child;
        List<INews> news = newsmark.getNews(INews.State.getVisible());
        for (INews newsitem : news) {
          if (newsitem != null && newsitem.getId() != null)
            fNewsContainer.addNews(newsitem);
        }
      }

      /* Recursively treat Folders */
      if (child instanceof IFolder)
        fillNews((IFolder) child);
    }
  }

  /**
   * @return the {@link IFolder} that serves as input to this {@link INewsMark}.
   */
  public IFolder getFolder() {
    return fFolder;
  }

  /*
   * @see org.rssowl.core.internal.persist.AbstractEntity#setProperty(java.lang.String, java.io.Serializable)
   */
  @Override
  public synchronized void setProperty(String key, Serializable value) {
    fFolder.setProperty(key, value);
  }

  /*
   * @see org.rssowl.core.internal.persist.Mark#getName()
   */
  @Override
  public synchronized String getName() {
    return fFolder.getName();
  }

  /*
   * @see org.rssowl.core.internal.persist.AbstractEntity#getProperties()
   */
  @Override
  public synchronized Map<String, Serializable> getProperties() {
    return fFolder.getProperties();
  }

  /*
   * @see org.rssowl.core.internal.persist.AbstractEntity#getProperty(java.lang.String)
   */
  @Override
  public synchronized Object getProperty(String key) {
    return fFolder.getProperty(key);
  }

  /*
   * @see org.rssowl.core.internal.persist.Mark#getParent()
   */
  /*
   * @see org.rssowl.core.internal.persist.Mark#getParent()
   */
  @Override
  public synchronized IFolder getParent() {
    return fFolder.getParent();
  }

  /**
   * @param news
   * @return <code>true</code> if the given News belongs to any
   * {@link IBookMark} or {@link INewsBin} of the given {@link IFolder}.
   */
  public boolean isRelatedTo(INews news) {

    /* Might be contained */
    if (containsNews(news))
      return true;

    FeedLinkReference feedRef = news.getFeedReference();
    return isRelatedTo(fFolder, news, feedRef);
  }

  private boolean isRelatedTo(IFolder folder, INews news, FeedLinkReference ref) {
    List<IFolderChild> children = folder.getChildren();

    for (IFolderChild child : children) {

      /* Check contained in Folder */
      if (child instanceof IFolder && isRelatedTo((IFolder) child, news, ref))
        return true;

      /* News could be part of the Feed (but is no copy) */
      else if (news.getParentId() == 0 && child instanceof IBookMark) {
        IBookMark bookmark = (IBookMark) child;
        if (bookmark.getFeedLinkReference().equals(ref))
          return true;
      }

      /* News could be part of Bin (and is a copy) */
      else if (news.getParentId() != 0 && child instanceof INewsBin) {
        INewsBin bin = (INewsBin) child;
        if (bin.getId() == news.getParentId())
          return true;
      }
    }

    return false;
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#containsNews(org.rssowl.core.persist.INews)
   */
  public synchronized boolean containsNews(INews news) {
    return fNewsContainer.containsNews(news);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNews()
   */
  public synchronized List<INews> getNews() {
    return getNews(EnumSet.allOf(INews.State.class));
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNews(java.util.Set)
   */
  public List<INews> getNews(Set<State> states) {
    List<NewsReference> newsRefs;
    synchronized (this) {
      newsRefs = fNewsContainer.getNews(states);
    }
    return getNews(newsRefs);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNewsCount(java.util.Set)
   */
  public synchronized int getNewsCount(Set<State> states) {
    return fNewsContainer.getNewsCount(states);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNewsRefs()
   */
  public synchronized List<NewsReference> getNewsRefs() {
    return fNewsContainer.getNews();
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNewsRefs(java.util.Set)
   */
  public synchronized List<NewsReference> getNewsRefs(Set<State> states) {
    return fNewsContainer.getNews(states);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#isGetNewsRefsEfficient()
   */
  public boolean isGetNewsRefsEfficient() {
    return false;
  }

  /*
   * @see org.rssowl.core.persist.IEntity#toReference()
   */
  public ModelReference toReference() {
    return new FolderNewsMarkReference(getId());
  }
}