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

package org.rssowl.core.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IGuid;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISource;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.MergeUtils;
import org.rssowl.core.util.StringUtils;

import java.io.Serializable;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A News is a single entry inside a Feed. The attributes IsRead, IsNew and
 * IsDeleted describe the life-cycle of a News:
 * <ul>
 * <li>IsRead: The News has been marked read</li>
 * <li>IsNew: The News has not been read and was not yet looked at</li>
 * <li>IsDeleted: The News has been deleted by the user or system</li>
 * </ul>
 *
 * @author bpasero
 */
public class News extends AbstractEntity implements INews {

  static final class Lock {

    private final transient ReentrantReadWriteLock fLock = new ReentrantReadWriteLock();
    private volatile transient Thread fReadLockThread;

    void acquireReadLock() {
      fLock.readLock().lock();
    }

    void acquireReadLockSpecial() {
      fLock.readLock().lock();
      fReadLockThread = Thread.currentThread();
    }

    void acquireWriteLock() {
      if (fReadLockThread == Thread.currentThread()) {
        throw new IllegalStateException("Cannot acquire the write lock from the " + //$NON-NLS-1$
        "same thread as the read lock."); //$NON-NLS-1$
      }
      fLock.writeLock().lock();
    }

    void releaseReadLock() {
      fLock.readLock().unlock();
    }

    void releaseReadLockSpecial() {
      fReadLockThread = null;
      fLock.readLock().unlock();
    }

    void releaseWriteLock() {
      fLock.writeLock().unlock();
    }

  }

  private String fTitle;

  private String fLinkText;

  private String fBaseUri;

  /* SPM Mod*/
  private boolean newsWithMp3 = false;

  private Date fReceiveDate;

  private Date fPublishDate;

  private Date fModifiedDate;
  private String fComments;
  private String fInReplyTo;

  private boolean fIsFlagged;
  private int fRating;
  private int fStateOrdinal = INews.State.NEW.ordinal();
  private String fGuidValue;

  private transient IGuid fGuid;

  private boolean fGuidIsPermaLink;
  private ISource fSource;
  private String fFeedLink;

  private IPerson fAuthor;

  private List<IAttachment> fAttachments;

  private List<ICategory> fCategories;

  private Set<ILabel> fLabels;
  /* This field is only non-zero if the parent is not a feed */
  private long fParentId;
  /* We can't use fDescription to support migration from M7 to M8 */
  private transient String fTransientDescription;

  private transient boolean fTransientDescriptionSet;

  private transient final Lock fLock = new Lock();
  /**
   * Default constructor for deserialization
   */
  protected News() {
    // As per javadoc
  }

  /**
   * Constructor used by <code>DefaultModelFactory</code>
   *
   * @param feed The Feed this News is belonging to.
   */
  public News(IFeed feed) {
    super(null);
    Assert.isNotNull(feed, "The type News requires a Feed that is not NULL"); //$NON-NLS-1$
    fFeedLink = feed.getLink().toString();
    fReceiveDate = new Date();
    init();
  }

  /**
   * Creates a new Element of the Type News
   * <p>
   * TODO Consider whether feed can be null
   * </p>
   *
   * @param id The unique id of the News.
   * @param feed The Feed this News belongs to.
   * @param receiveDate The Date this News was received.
   */
  public News(Long id, IFeed feed, Date receiveDate) {
    super(id);
    Assert.isNotNull(feed, "The type News requires a Feed that is not NULL"); //$NON-NLS-1$
    fFeedLink = feed.getLink().toString();
    Assert.isNotNull(receiveDate, "The type News requires a ReceiveDate that is not NULL"); //$NON-NLS-1$
    fReceiveDate = receiveDate;
    init();
  }

  public News(News news, long parentId) {
    super(null, news);
    fParentId = parentId;
    news.fLock.acquireReadLock();
    try {
      for (IAttachment attachment : news.getAttachments())
        addAttachment(new Attachment(attachment, this));

      if (news.getAuthor() != null)
        fAuthor = new Person(news.getAuthor());

      fBaseUri = news.fBaseUri;

      for (ICategory category : news.getCategories())
        addCategory(new Category(category));

      setDescription(news.getDescription());
      fComments = news.fComments;
      fFeedLink = news.fFeedLink;
      setGuid(news.getGuid());
      fInReplyTo = news.fInReplyTo;
      fIsFlagged = news.fIsFlagged;

      /*
       * Don't need to copy the labels because the relationship is ManyToMany.
       */
      fLabels = news.fLabels == null ? null : new HashSet<ILabel>(news.fLabels);

      fLinkText = news.fLinkText;

      if (news.fModifiedDate != null)
        fModifiedDate = new Date(news.fModifiedDate.getTime());

      if (news.fPublishDate != null)
        fPublishDate = new Date(news.fPublishDate.getTime());

      fRating = news.fRating;

      if (news.fReceiveDate != null)
        fReceiveDate = new Date(news.fReceiveDate.getTime());

      if (news.getSource() != null)
        fSource = new Source(news.getSource());

      fStateOrdinal = news.fStateOrdinal;
      fTitle = news.fTitle;
    } finally {
      news.fLock.releaseReadLock();
    }
    init();
  }

  /**
   * Acquires the read lock used by all non-mutating public methods of this
   * object. This method also ensures that an IllegalStateException is thrown if
   * the same thread tries to acquire the write lock (by calling one of the
   * mutating methods) while still holding this read lock (to prevent deadlocks).
   *
   * <p>
   * This method should only be used in very specific circumstances. Avoid if
   * possible.
   * </p>
   *
   * @see #releaseReadLockSpecial()
   */
  public final void acquireReadLockSpecial() {
    fLock.acquireReadLockSpecial();
  }

  /*
   * @see
   * org.rssowl.core.model.types.INews#addAttachment(org.rssowl.core.model.types
   * .IAttachment)
   */
  public void addAttachment(IAttachment attachment) {
    Assert.isNotNull(attachment, "Exception adding NULL as Attachment into News"); //$NON-NLS-1$
    fLock.acquireWriteLock();
    try {
      if (fAttachments == null)
        fAttachments = new ArrayList<IAttachment>(1);

      /* Rule: Child needs to know about its new parent already! */
      Assert.isTrue(equals(attachment.getNews()), "The Attachment has a different News set!"); //$NON-NLS-1$
      fAttachments.add(attachment);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#addCategory(org.rssowl.core.model.types.ICategory)
   */
  public void addCategory(ICategory category) {
    fLock.acquireWriteLock();
    try {
      if (fCategories == null)
        fCategories = new ArrayList<ICategory>(1);
      fCategories.add(category);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  public boolean addLabel(ILabel label) {
    Assert.isNotNull(label, "label"); //$NON-NLS-1$
    fLock.acquireWriteLock();
    try {
      if (fLabels == null)
        fLabels = new HashSet<ILabel>(1);

      return fLabels.add(label);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  private boolean areEqual(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

  private boolean areGuidsIdentical(IGuid g0, IGuid g1) {
    return g0.getValue().equals(g1.getValue()) && g0.isPermaLink() == g1.isPermaLink();
  }

  public void clearTransientDescription() {
    fLock.acquireWriteLock();
    try {
      fTransientDescription = null;
      fTransientDescriptionSet = false;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  private boolean equals(Object o1, Object o2) {
    return o1 == null ? o2 == null : o1.equals(o2);
  }

  /*
   * @see org.rssowl.core.model.types.INews#getAttachments()
   */
  public List<IAttachment> getAttachments() {
    fLock.acquireReadLock();
    try {
      if (fAttachments == null)
        return new ArrayList<IAttachment>(0);
      return new ArrayList<IAttachment>(fAttachments);
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getAuthor()
   */
  public IPerson getAuthor() {
    fLock.acquireReadLock();
    try {
      return fAuthor;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getBase()
   */
  public URI getBase() {
    fLock.acquireReadLock();
    try {
      return createURI(fBaseUri);
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getCategories()
   */
  public List<ICategory> getCategories() {
    fLock.acquireReadLock();
    try {
      if (fCategories == null)
        return new ArrayList<ICategory>(0);
      return new ArrayList<ICategory>(fCategories);
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getComments()
   */
  public String getComments() {
    fLock.acquireReadLock();
    try {
      return fComments;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getDescription()
   */
  public String getDescription() {
    fLock.acquireReadLock();
    try {
      if (fTransientDescriptionSet)
        return fTransientDescription;
    } finally {
      fLock.releaseReadLock();
    }

    if (getId() == null)
      return null;

    Description description = loadDescription();
    return description == null ? null : description.getValue();
  }

  public String getFeedLinkAsText() {
    return fFeedLink;
  }

  /*
   * @see org.rssowl.core.model.types.INews#getFeed()
   */
  public FeedLinkReference getFeedReference() {
    fLock.acquireReadLock();
    try {
      return fFeedLink == null ? null : new FeedLinkReference(createURI(fFeedLink));
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getGuid()
   */
  public IGuid getGuid() {
    fLock.acquireReadLock();
    try {
      return fGuid;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getInReplyTo()
   */
  public String getInReplyTo() {
    fLock.acquireReadLock();
    try {
      return fInReplyTo;
    } finally {
      fLock.releaseReadLock();
    }
  }

  public Set<ILabel> getLabels()    {
    fLock.acquireReadLock();
    try {
      if (fLabels == null)
        return new HashSet<ILabel>(0);
      return new HashSet<ILabel>(fLabels);
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getLink()
   */
  public URI getLink() {
    fLock.acquireReadLock();
    try {
      return fLinkText == null ? null : createURI(fLinkText);
    } finally {
      fLock.releaseReadLock();
    }
  }

  public String getLinkAsText() {
    return fLinkText;
  }

  /*
   * @see org.rssowl.core.model.types.INews#getModifiedDate()
   */
  public Date getModifiedDate() {
    fLock.acquireReadLock();
    try {
      return fModifiedDate;
    } finally {
      fLock.releaseReadLock();
    }
  }

  public long getParentId() {
    fLock.acquireReadLock();
    try {
      return fParentId;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.internal.persist.AbstractEntity#getProperties()
   */
  @Override
  @SuppressWarnings("all")
  public Map<String, Serializable> getProperties() {
    fLock.acquireReadLock();
    try {
      return super.getProperties();
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.internal.persist.AbstractEntity#getProperty(java.lang.String)
   */
  @Override
  @SuppressWarnings("all")
  public Object getProperty(String key) {
    fLock.acquireReadLock();
    try {
      return super.getProperty(key);
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getPublishDate()
   */
  public Date getPublishDate() {
    fLock.acquireReadLock();
    try {
      return fPublishDate;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getRating()
   */
  public int getRating() {
    fLock.acquireReadLock();
    try {
      return fRating;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getReceiveDate()
   */
  public Date getReceiveDate() {
    fLock.acquireReadLock();
    try {
      return fReceiveDate;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getSource()
   */
  public ISource getSource() {
    fLock.acquireReadLock();
    try {
      return fSource;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getState()
   */
  public State getState() {
    fLock.acquireReadLock();
    try {
      return INews.State.getState(fStateOrdinal);
    } finally {
      fLock.releaseReadLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#getTitle()
   */
  public String getTitle() {
    fLock.acquireReadLock();
    try {
      return fTitle;
    } finally {
      fLock.releaseReadLock();
    }
  }

  public String getTransientDescription() {
    fLock.acquireReadLock();
    try {
      return fTransientDescription;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /**
   * Initialises object after deserialization. Should not be used otherwise.
   */
  public final void init() {
    fLock.acquireWriteLock();
    try {
      if (fGuidValue != null)
        fGuid = new Guid(fGuidValue, fGuidIsPermaLink);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  public boolean isEquivalent(INews o) {
    News other = (News) o;
    fLock.acquireReadLock();
    other.fLock.acquireReadLock();
    try {
      Assert.isNotNull(other, "other cannot be null"); //$NON-NLS-1$

      Boolean guidMatch = isEquivalentCompare(slashTrim(fGuidValue), slashTrim(other.fGuidValue));

      //TODO Consider simplifying this after M7. The case where one news
      //has permaLink == true and the other has permaLink == false with the
      //same guidValue should not happen in practice.
      if (guidMatch != null && guidMatch.equals(Boolean.FALSE) && (fGuidValue == null || fGuidIsPermaLink) && (other.fGuidValue == null || other.fGuidIsPermaLink))
        return false;
      else if (guidMatch != null && guidMatch.equals(Boolean.TRUE))
        return true;

      Boolean linkMatch = isEquivalentCompare(slashTrim(fLinkText), slashTrim(other.fLinkText));
      if (linkMatch != null) {
        if (linkMatch.equals(Boolean.TRUE))
          return true;

        return false;
      }
      if (!fFeedLink.equals(other.fFeedLink))
        return false;

      Boolean titleMatch = isEquivalentCompare(fTitle, other.fTitle);
      if (titleMatch != null && titleMatch.equals(Boolean.TRUE))
        return true;

      return false;
    } finally {
      fLock.releaseReadLock();
      other.fLock.releaseReadLock();
    }
  }

  private <T>Boolean isEquivalentCompare(T o1, T o2) {
    if ((o1 == null) && (o2 == null))
      return null;

    return Boolean.valueOf(equals(o1, o2));
  }

  /*
   * @see org.rssowl.core.model.types.INews#isFlagged()
   */
  public boolean isFlagged() {
    fLock.acquireReadLock();
    try {
      return fIsFlagged;
    } finally {
      fLock.releaseReadLock();
    }
  }

  /**
   * @param news
   * @return whether <code>news</code> is identical to this object.
   */
  public boolean isIdentical(INews news) {
    if (this == news)
      return true;

    if (!(news instanceof News))
      return false;

    News n = (News) news;
    fLock.acquireReadLock();
    n.fLock.acquireReadLock();
    try {
      return (getId() == null ? n.getId() == null : getId().equals(n.getId())) &&
      fFeedLink.equals(n.fFeedLink) &&
      simpleFieldsEqual(n) &&
      (fReceiveDate == null ? n.fReceiveDate == null : fReceiveDate.equals(n.fReceiveDate)) &&
      (getGuid() == null ? n.getGuid() == null : getGuid().equals(n.getGuid())) &&
      (fSource == null ? n.fSource == null : fSource.equals(n.fSource)) &&
      (fInReplyTo == null ? n.fInReplyTo == null : fInReplyTo.equals(n.fInReplyTo)) &&
      (getLabels().equals(n.getLabels())) &&
      (getAuthor() == null ? n.getAuthor() == null : getAuthor().equals(n.getAuthor())) &&
      getAttachments().equals(n.getAttachments()) &&
      getCategories().equals(n.getCategories()) &&
      getState() == n.getState() && fIsFlagged == n.fIsFlagged && fRating == n.fRating &&
      (getProperties() == null ? n.getProperties() == null : getProperties().equals(n.getProperties()));
    } finally {
      fLock.releaseReadLock();
      n.fLock.releaseReadLock();
    }

  }

  public boolean isNewsWithMp3() {
    return newsWithMp3;
  }

  public boolean isTransientDescriptionSet() {
    return fTransientDescriptionSet;
  }

  private boolean isUpdated(INews news) {
    State thisState = getState();
    if (thisState != State.UNREAD)
      return false;

    String title = news.getTitle();
    if (!(fTitle == null ? title == null : fTitle.equals(title)))
      return true;

    return false;
  }

  /*
   * @see org.rssowl.core.model.types.INews#isVisible()
   */
  public boolean isVisible() {
    /* No need to lock since getState() has a read lock */
    INews.State state = getState();
    return State.getVisible().contains(state);
  }

  public Description loadDescription() {
    return new DescriptionReference(this.getIdAsPrimitive()).resolve();
  }

  public MergeResult merge(INews news) {
    Assert.isNotNull(news, "news cannot be null"); //$NON-NLS-1$
    Assert.isLegal(this != news, "Trying to merge the same news, this is most likely a mistake, news: " + news); //$NON-NLS-1$

    //TODO It's _in theory_ possible for a deadlock to occur here. Even though
    //we may want to remove that possibility at some point, it should not happen
    //in practice because merge is always called on an existing news and passed
    //a news reloaded from the site. The latter is not yet visible to the app
    //so no locks should be held.
    News n = (News) news;
    n.fLock.acquireReadLock();
    try {
      fLock.acquireWriteLock();
      try {
        boolean updated = mergeState(news);

        MergeResult result = new MergeResult();
        /* Merge all items except for feed and id */
        updated |= processListMergeResult(result, mergeAttachments(n.fAttachments));
        updated |= processListMergeResult(result, mergeCategories(n.fCategories));
        updated |= processListMergeResult(result, mergeAuthor(n.fAuthor));
        updated |= mergeGuid(n.fGuid);
        mergeDescription(result, n);
        updated |= processListMergeResult(result, mergeSource(n.fSource));
        updated |= !simpleFieldsEqual(n);

        fBaseUri = n.fBaseUri;
        fComments = n.fComments;
        fLinkText = n.fLinkText;
        fModifiedDate = n.fModifiedDate;
        fPublishDate = n.fPublishDate;
        fTitle = n.fTitle;
        fInReplyTo = n.fInReplyTo;

        ComplexMergeResult<?> propertiesResult = MergeUtils.mergeProperties(this, news);
        if (updated || propertiesResult.isStructuralChange()) {
          result.addUpdatedObject(this);
          result.addAll(propertiesResult);
        }
        return result;
      } finally {
        fLock.releaseWriteLock();
      }
    } finally {
      n.fLock.releaseReadLock();
    }
  }

  private ComplexMergeResult<List<IAttachment>> mergeAttachments(List<IAttachment> attachments) {
    if (attachments == null)
      attachments = Collections.emptyList();

    Comparator<IAttachment> comparator = new Comparator<IAttachment>() {
      public int compare(IAttachment o1, IAttachment o2) {
        if (o1.getLink() == null ? o2.getLink() == null : o1.getLink().equals(o2.getLink())) {
          return 0;
        }
        return -1;
      }
    };
    ComplexMergeResult<List<IAttachment>> mergeResult = MergeUtils.merge(fAttachments, attachments, comparator, this);
    fAttachments = mergeResult.getMergedObject();
    return mergeResult;
  }

  private ComplexMergeResult<IPerson> mergeAuthor(IPerson author) {
    ComplexMergeResult<IPerson> mergeResult = MergeUtils.merge(fAuthor, author);
    fAuthor = mergeResult.getMergedObject();
    return mergeResult;
  }

  private ComplexMergeResult<List<ICategory>> mergeCategories(List<ICategory> categories) {
    if (categories == null)
      categories = Collections.emptyList();

    Comparator<ICategory> comparator = new Comparator<ICategory>() {

      public int compare(ICategory o1, ICategory o2) {
        if (o1.getName() == null ? o2.getName() == null : o1.getName().equals(o2.getName())) {
          return 0;
        }
        return -1;
      }

    };
    ComplexMergeResult<List<ICategory>> mergeResult = MergeUtils.merge(fCategories, categories, comparator, null);
    fCategories = mergeResult.getMergedObject();
    return mergeResult;
  }

  private void mergeDescription(MergeResult result, News news) {
    String newsDescription = null;
    if (news.getId() == null)
      newsDescription = news.fTransientDescription;
    else
      newsDescription = news.getDescription();

    Description description = loadDescription();
    boolean descriptionUpdated = false;
    if (description == null)
      description = new Description(this, null);

    if (fTransientDescriptionSet && (!areEqual(description.getValue(), fTransientDescription))) {
      description.setDescription(fTransientDescription);
      descriptionUpdated = true;
    }

    if (!areEqual(description.getValue(), newsDescription)) {
      setDescription(newsDescription);
      description.setDescription(newsDescription);
      descriptionUpdated = true;
    }

    if (descriptionUpdated) {
      if (description.getValue() == null)
        result.addRemovedObject(description);
      else
        result.addUpdatedObject(description);
    }
  }

  private boolean mergeGuid(IGuid guid) {
    if (fGuid == null && guid == null)
      return false;

    if (fGuid == null || guid == null || (!areGuidsIdentical(fGuid, guid))) {
      setGuid(guid);
      return true;
    }

    return false;
  }

  private ComplexMergeResult<ISource> mergeSource(ISource source) {
    ComplexMergeResult<ISource> mergeResult = MergeUtils.merge(fSource, source);
    fSource = mergeResult.getMergedObject();
    return mergeResult;
  }

  private boolean mergeState(INews news) {
    State thisState = getState();
    State otherState = news.getState();
    if (thisState != otherState && otherState != State.NEW) {
      setState(otherState);
      return true;
    }
    if (isUpdated(news)) {
      setState(State.UPDATED);
      return true;
    }
    return false;
  }

  /**
   * Releases the read lock acquired by calling
   * {@link #acquireReadLockSpecial()}. It's very important to _always_ call
   * this method after calling acquireReadLockSpecial. Typically this is
   * achieved with a try/finally block.
   *
   * @see #acquireReadLockSpecial()
   */
  public final void releaseReadLockSpecial() {
    fLock.releaseReadLockSpecial();
  }

  public void removeAttachment(IAttachment attachment) {
    fLock.acquireWriteLock();
    try {
      if (fAttachments != null)
        fAttachments.remove(attachment);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  public boolean removeLabel(ILabel label) {
    Assert.isNotNull(label, "label"); //$NON-NLS-1$
    fLock.acquireWriteLock();
    try {
      if (fLabels == null)
        return false;

      return fLabels.remove(label);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.internal.persist.AbstractEntity#removeProperty(java.lang.String)
   */
  @Override
  @SuppressWarnings("all")
  public Object removeProperty(String key) {
    fLock.acquireWriteLock();
    try {
      return super.removeProperty(key);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setAuthor(org.rssowl.core.model.types.IPerson)
   */
  public void setAuthor(IPerson author) {
    fLock.acquireWriteLock();
    try {
      fAuthor = author;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setBase(java.net.URI)
   */
  public void setBase(URI baseUri) {
    fLock.acquireWriteLock();
    try {
      fBaseUri = getURIText(baseUri);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setComments(java.lang.String)
   */
  public void setComments(String comments) {
    fLock.acquireWriteLock();
    try {
      fComments = comments;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  public void setDescription(String description) {
    fLock.acquireWriteLock();
    try {
      fTransientDescription = description;
      fTransientDescriptionSet = true;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setFlagged(boolean)
   */
  public void setFlagged(boolean isFlagged) {
    fLock.acquireWriteLock();
    try {
      fIsFlagged = isFlagged;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setGuid(org.rssowl.core.model.types.IGuid)
   */
  public void setGuid(IGuid guid) {
    fLock.acquireWriteLock();
    try {
      fGuid = guid;
      fGuidValue = (guid == null ? null : guid.getValue());
      fGuidIsPermaLink = (guid == null ? false : guid.isPermaLink());
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setInReplyTo(java.lang.String)
   */
  public void setInReplyTo(String guid) {
    fLock.acquireWriteLock();
    try {
      fInReplyTo = guid;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setLink(java.lang.String)
   */
  public void setLink(URI link) {
    fLock.acquireWriteLock();
    try {
      fLinkText = link == null ? null : link.toString();
    } finally {
      fLock.releaseWriteLock();
    }
  }


  /*
   * @see org.rssowl.core.model.types.INews#setModifiedDate(java.util.Date)
   */
  public void setModifiedDate(Date modifiedDate) {
    fLock.acquireWriteLock();
    try {
      fModifiedDate = modifiedDate;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  public void setNewsWithMp3() {
    newsWithMp3 = true;
  }

  public void setParent(IFeed feed) {
    Assert.isNotNull(feed, "feed"); //$NON-NLS-1$
    fLock.acquireWriteLock();
    try {
      this.fFeedLink = feed.getLink().toString();
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.internal.persist.AbstractEntity#setProperty(java.lang.String, java.lang.Object)
   */
  @Override
  @SuppressWarnings("all")
  public void setProperty(String key, Serializable value) {
    fLock.acquireWriteLock();
    try {
      super.setProperty(key, value);
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setPublishDate(java.util.Date)
   */
  public void setPublishDate(Date publishDate) {
    fLock.acquireWriteLock();
    try {
      fPublishDate = publishDate;
    } finally	{
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setRating(int)
   */
  public void setRating(int rating) {
    fLock.acquireWriteLock();
    try {
      fRating = rating;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setReceiveDate(java.util.Date)
   */
  public void setReceiveDate(Date receiveDate) {
    fLock.acquireWriteLock();
    try {
      fReceiveDate = receiveDate;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setSource(org.rssowl.core.model.types.ISource)
   */
  public void setSource(ISource source) {
    fLock.acquireWriteLock();
    try {
      fSource = source;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setState(org.rssowl.core.model.types.INews.State)
   */
  public void setState(State state) {
    Assert.isNotNull(state, "state cannot be null"); //$NON-NLS-1$
    fLock.acquireWriteLock();
    try {
      fStateOrdinal = state.ordinal();
    } finally {
      fLock.releaseWriteLock();
    }
  }

  /*
   * @see org.rssowl.core.model.types.INews#setTitle(java.lang.String)
   */
  public void setTitle(String title) {
    fLock.acquireWriteLock();
    try {
      fTitle = title;
    } finally {
      fLock.releaseWriteLock();
    }
  }

  private boolean simpleFieldsEqual(News news) {
    return MergeUtils.equals(fBaseUri, news.fBaseUri) &&
    MergeUtils.equals(fComments, news.fComments) &&
    MergeUtils.equals(fLinkText, news.fLinkText) &&
    MergeUtils.equals(fModifiedDate, news.fModifiedDate) &&
    MergeUtils.equals(fPublishDate, news.fPublishDate) &&
    MergeUtils.equals(fInReplyTo, news.fInReplyTo) &&
    MergeUtils.equals(fTitle, news.fTitle);
  }

  /* TODO Remove me in 2.1 (Pre 2.0 Support - see Bug 1092) */
  private String slashTrim(String str) {
    if (StringUtils.isSet(str) && str.length() > 1 && str.charAt(str.length() - 1) == '/')
      return str.substring(0, str.length() - 1);

    return str;
  }

  /**
   * Returns a String describing the state of this Entity.
   *
   * @return A String describing the state of this Entity.
   */
  public String toLongString() {
    StringBuilder str = new StringBuilder();

    str.append("\n\n****************************** News ******************************\n"); //$NON-NLS-1$
    fLock.acquireReadLock();
    try {
      str.append("\nNews ID: ").append(getId()); //$NON-NLS-1$
      if (fFeedLink != null)
        str.append("\nFeed Link: ").append(fFeedLink); //$NON-NLS-1$
      str.append("\nState: ").append(getState()); //$NON-NLS-1$
      if (getTitle() != null)
        str.append("\nTitle: ").append(getTitle()); //$NON-NLS-1$
      if (getLinkAsText() != null)
        str.append("\nLink: ").append(getLinkAsText()); //$NON-NLS-1$
      if (getBase() != null)
        str.append("\nBase URI: ").append(getBase()); //$NON-NLS-1$
      if (getDescription() != null)
        str.append("\nDescription: ").append(getDescription()); //$NON-NLS-1$
      str.append("\nRating: ").append(getRating()); //$NON-NLS-1$
      if (getPublishDate() != null)
        str.append("\nPublish Date: ").append(DateFormat.getDateTimeInstance().format(getPublishDate())); //$NON-NLS-1$
      if (getReceiveDate() != null)
        str.append("\nReceive Date: ").append(DateFormat.getDateTimeInstance().format(getReceiveDate())); //$NON-NLS-1$
      if (getModifiedDate() != null)
        str.append("\nModified Date: ").append(DateFormat.getDateTimeInstance().format(getModifiedDate())); //$NON-NLS-1$
      if (getAuthor() != null)
        str.append("\nAuthor: ").append(getAuthor()); //$NON-NLS-1$
      if (getComments() != null)
        str.append("\nComments: ").append(getComments()); //$NON-NLS-1$
      if (getGuid() != null)
        str.append("\nGUID: ").append(getGuid()); //$NON-NLS-1$
      if (getSource() != null)
        str.append("\nSource: ").append(getSource()); //$NON-NLS-1$
      if (getInReplyTo() != null)
        str.append("\nIn Reply To: ").append(getInReplyTo()); //$NON-NLS-1$
      str.append("\nLabesl: ").append(getLabels()); //$NON-NLS-1$
      str.append("\nAttachments: ").append(getAttachments()); //$NON-NLS-1$
      str.append("\nCategories: ").append(getCategories()); //$NON-NLS-1$
      str.append("\nIs Flagged: ").append(fIsFlagged); //$NON-NLS-1$
      str.append("\nProperties: ").append(getProperties()); //$NON-NLS-1$
    } finally {
      fLock.releaseReadLock();
    }
    return str.toString();
  }

  public NewsReference toReference() {
    return new NewsReference(getIdAsPrimitive());
  }

  @Override
  public synchronized String toString() {
    StringBuilder str = new StringBuilder();
    str.append("\n\n****************************** News ******************************\n"); //$NON-NLS-1$
    fLock.acquireReadLock();
    try {
      str.append("\nNews ID: ").append(getId()); //$NON-NLS-1$
      if (getTitle() != null)
        str.append("\nTitle: ").append(getTitle()); //$NON-NLS-1$
      if (getLinkAsText() != null)
        str.append("\nLink: ").append(getLinkAsText()); //$NON-NLS-1$
    } finally {
      fLock.releaseReadLock();
    }
    return str.toString();
  }

}