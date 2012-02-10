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

package org.rssowl.core.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.MonitorCanceledException;
import org.rssowl.core.connection.UnknownProtocolException;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.newsaction.CopyNewsAction;
import org.rssowl.core.internal.newsaction.MoveNewsAction;
import org.rssowl.core.interpreter.ITypeImporter;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.interpreter.ParserException;
import org.rssowl.core.interpreter.UnknownFormatException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFilterAction;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IGuid;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchFilter;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.ISearchValueType;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.FolderReference;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.PersistenceException;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * Helper class for various Core operations.
 *
 * @author bpasero
 */
public class CoreUtils {

  /** Folder Index Value for Long Arrays */
  public static final int FOLDER = 0;

  /** Bookmark Index Value for Long Arrays */
  public static final int BOOKMARK = 1;

  /** Newsbin Index Value for Long Arrays */
  public static final int NEWSBIN = 2;

  /** Mime Types for Feeds */
  public static final String[] FEED_MIME_TYPES = new String[] { "application/rss+xml", "application/atom+xml", "application/rdf+xml" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

  /* A Set of Stop Words in English */
  private static final Set<String> STOP_WORDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(StringUtils.ENGLISH_STOP_WORDS)));

  /* A buffer that can be used to add log entries from db4o */
  private static final StringBuffer fgLogBuffer = new StringBuffer();

  /*
   * Special case structural actions that need to run as last action (but before
   * display actions)
   */
  private static List<String> STRUCTURAL_ACTIONS = Arrays.asList(new String[] { MoveNewsAction.ID, CopyNewsAction.ID });

  /* Special case display actions that need to run as last action */
  private static List<String> DISPLAY_ACTIONS = Arrays.asList(new String[] { "org.rssowl.ui.ShowNotifierNewsAction" }); //$NON-NLS-1$

  /* Favicon Markers */
  private static final String[] FAVICON_MARKERS = new String[] { "shortcut icon", ".ico" }; //$NON-NLS-1$ //$NON-NLS-2$

  /* Href Variants */
  private static final String[] HREF_VARIANTS = new String[] { "href = ", "href= ", "href=", "HREF=", "href =" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

  /* Reserved Characters for Filenames on Windows */
  private static final String[] RESERVED_FILENAME_CHARACTERS_WINDOWS = new String[] { "<", ">", ":", "\"", "/", "\\", "|", "?", "*" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$

  /* This utility class constructor is hidden */
  private CoreUtils() {
  // Protect default constructor
  }

  /**
   * @param filter an instance of {@link ISearchFilter} to obtain a collection
   * of {@link IFilterAction}.
   * @return a collection of {@link IFilterAction}. the collection is sorted
   * such as structural actions are moved to the end of the list.
   */
  public static Collection<IFilterAction> getActions(ISearchFilter filter) {
    Set<IFilterAction> actions = new TreeSet<IFilterAction>(new Comparator<IFilterAction>() {
      public int compare(IFilterAction o1, IFilterAction o2) {
        if (DISPLAY_ACTIONS.contains(o1.getActionId()))
          return 1;

        if (DISPLAY_ACTIONS.contains(o2.getActionId()))
          return -1;

        if (STRUCTURAL_ACTIONS.contains(o1.getActionId()))
          return 1;

        if (STRUCTURAL_ACTIONS.contains(o2.getActionId()))
          return -1;

        return 1;
      }
    });

    actions.addAll(filter.getActions());
    return actions;
  }

  /**
   * @param conditions The search conditions to find a human readable name for.
   * @param matchAllConditions Either true or false depending on the search.
   * @return A human readable name for all the conditions.
   */
  public static String getName(List<ISearchCondition> conditions, boolean matchAllConditions) {
    StringBuilder name = new StringBuilder();
    List<ISearchCondition> locationConditions = new ArrayList<ISearchCondition>(conditions.size());

    /* First group Conditions by Field */
    Map<String, List<ISearchCondition>> mapFieldNameToConditions = new HashMap<String, List<ISearchCondition>>();
    for (ISearchCondition condition : conditions) {

      /* Handle Location at the End */
      if (condition.getField().getId() == INews.LOCATION) {
        locationConditions.add(condition);
        continue;
      }

      String fieldName = condition.getField().getName();
      String condValue = condition.getValue().toString();

      if (condValue.length() > 0) {
        List<ISearchCondition> fieldConditions = mapFieldNameToConditions.get(fieldName);
        if (fieldConditions == null) {
          fieldConditions = new ArrayList<ISearchCondition>();
          mapFieldNameToConditions.put(fieldName, fieldConditions);
        }

        fieldConditions.add(condition);
      }
    }

    /* For each Field Group */
    Set<Entry<String, List<ISearchCondition>>> entries = mapFieldNameToConditions.entrySet();
    DateFormat dateFormat = DateFormat.getDateInstance();
    for (Entry<String, List<ISearchCondition>> entry : entries) {
      String prevSpecName = null;
      String fieldName = entry.getKey();
      List<ISearchCondition> fieldConditions = entry.getValue();
      StringBuilder fieldExpression = new StringBuilder();

      /* Append Field Name */
      fieldExpression.append(fieldName).append(" "); //$NON-NLS-1$

      /* For each Field Condition */
      for (ISearchCondition fieldCondition : fieldConditions) {
        String condValue = fieldCondition.getValue().toString();
        String specName = fieldCondition.getSpecifier().getName();
        int typeId = fieldCondition.getField().getSearchValueType().getId();
        int fieldId = fieldCondition.getField().getId();

        /* Condition Value provided */
        if (condValue.length() > 0) {

          /* Append specifier if not identical with previous */
          if (prevSpecName == null || !prevSpecName.equals(specName)) {
            fieldExpression.append(specName).append(" "); //$NON-NLS-1$
            prevSpecName = specName;
          }

          /* Specially Treat Age */
          if (fieldId == INews.AGE_IN_DAYS || fieldId == INews.AGE_IN_MINUTES) {
            Integer value = Integer.valueOf(condValue);
            if (value >= 0)
              fieldExpression.append(value == 1 ? NLS.bind(Messages.CoreUtils_N_DAY, value) : NLS.bind(Messages.CoreUtils_N_DAYS, value));
            else if (value % 60 == 0)
              fieldExpression.append(value == -60 ? NLS.bind(Messages.CoreUtils_N_HOUR, Math.abs(value) / 60) : NLS.bind(Messages.CoreUtils_N_HOURS, Math.abs(value) / 60));
            else
              fieldExpression.append(value == -1 ? NLS.bind(Messages.CoreUtils_N_MINUTE, Math.abs(value)) : NLS.bind(Messages.CoreUtils_N_MINUTES, Math.abs(value)));
          }

          /* Append Condition Value based on Type */
          else {
            switch (typeId) {
              case ISearchValueType.STRING:
                fieldExpression.append("'").append(condValue).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
                break;
              case ISearchValueType.LINK:
                fieldExpression.append("'").append(condValue).append("'"); //$NON-NLS-1$ //$NON-NLS-2$
                break;
              case ISearchValueType.ENUM:
                condValue = condValue.toLowerCase();
                condValue = condValue.replace("[", ""); //$NON-NLS-1$ //$NON-NLS-2$
                condValue = condValue.replace("]", ""); //$NON-NLS-1$ //$NON-NLS-2$

                fieldExpression.append(condValue.toLowerCase());

                break;
              case ISearchValueType.DATE:
                fieldExpression.append(dateFormat.format(fieldCondition.getValue()));
                break;
              case ISearchValueType.TIME:
                fieldExpression.append(dateFormat.format(fieldCondition.getValue()));
                break;
              case ISearchValueType.DATETIME:
                fieldExpression.append(dateFormat.format(fieldCondition.getValue()));
                break;

              default:
                fieldExpression.append(condValue);
            }
          }

          fieldExpression.append(" ").append(matchAllConditions ? Messages.CoreUtils_AND : Messages.CoreUtils_OR).append(" "); //$NON-NLS-1$ //$NON-NLS-2$
        }
      }

      if (fieldExpression.length() > 0)
        fieldExpression.delete(fieldExpression.length() - (matchAllConditions ? (" " + Messages.CoreUtils_AND + " ").length() : (" " + Messages.CoreUtils_OR + " ").length()), fieldExpression.length()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

      name.append(fieldExpression).append(" ").append(matchAllConditions ? Messages.CoreUtils_AND : Messages.CoreUtils_OR).append(" "); //$NON-NLS-1$ //$NON-NLS-2$
    }

    if (name.length() > 0)
      name.delete(name.length() - (matchAllConditions ? (" " + Messages.CoreUtils_AND + " ").length() : (" " + Messages.CoreUtils_OR + " ").length()), name.length()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    /* Append location if provided */
    if (!locationConditions.isEmpty()) {
      StringBuilder locationsValue = new StringBuilder();
      for (ISearchCondition locationCondition : locationConditions) {
        List<IFolderChild> locations = toEntities((Long[][]) locationCondition.getValue());
        for (IFolderChild location : locations) {
          locationsValue.append("'").append(location.getName()).append("', "); //$NON-NLS-1$ //$NON-NLS-2$
        }
      }

      locationsValue.delete(locationsValue.length() - 2, locationsValue.length());

      if (name.length() == 0)
        name.append(NLS.bind(Messages.CoreUtils_ALL_IN_N, locationsValue.toString()));
      else
        name.append(" ").append(NLS.bind(Messages.CoreUtils_IN_N, locationsValue.toString())); //$NON-NLS-1$
    }

    return name.toString();
  }

  /**
   * @param primitives An array that stores the IDs of {@link INewsBin}.
   * @return a list of {@link INewsBin} loaded from the provided IDs.
   */
  public static List<INewsBin> toBins(Long[] primitives) {
    List<INewsBin> bins = new ArrayList<INewsBin>(primitives.length);
    for (Long id : primitives) {
      INewsBin bin = DynamicDAO.load(INewsBin.class, id);
      if (bin != null)
        bins.add(bin);
    }

    return bins;
  }

  /**
   * @param primitives A multi-dimensional array where an array of {@link Long}
   * is stored in the first index representing IDs of all {@link IFolder} in the
   * list. The second index is an array of {@link Long} that represents the IDs
   * of all {@link IBookMark} in the list. The third index is an array of
   * {@link Long} that represents the IDs of all {@link INewsBin} in the list.
   * @return A list of folder childs (limited to folders, bookmarks and news
   * bins).
   */
  public static List<IFolderChild> toEntities(Long[][] primitives) {
    if (primitives == null)
      return Collections.emptyList();

    List<IFolderChild> childs = new ArrayList<IFolderChild>();

    /* Folders */
    for (int i = 0; primitives[FOLDER] != null && i < primitives[FOLDER].length; i++) {
      try {
        if (primitives[FOLDER][i] != null && primitives[FOLDER][i] != 0) {
          IFolder folder = new FolderReference(primitives[FOLDER][i]).resolve();
          if (folder != null)
            childs.add(folder);
        }
      } catch (PersistenceException e) {
        /* Ignore - Could be deleted already */
      }
    }

    /* BookMarks */
    for (int i = 0; primitives[BOOKMARK] != null && i < primitives[BOOKMARK].length; i++) {
      try {
        if (primitives[BOOKMARK][i] != null && primitives[BOOKMARK][i] != 0) {
          IBookMark bookmark = new BookMarkReference(primitives[BOOKMARK][i]).resolve();
          if (bookmark != null)
            childs.add(bookmark);
        }
      } catch (PersistenceException e) {
        /* Ignore - Could be deleted already */
      }
    }

    /* News Bins */
    if (primitives.length == 3) {
      for (int i = 0; primitives[NEWSBIN] != null && i < primitives[NEWSBIN].length; i++) {
        try {
          if (primitives[NEWSBIN][i] != null && primitives[NEWSBIN][i] != 0) {
            INewsBin newsbin = new NewsBinReference(primitives[NEWSBIN][i]).resolve();
            if (newsbin != null)
              childs.add(newsbin);
          }
        } catch (PersistenceException e) {
          /* Ignore - Could be deleted already */
        }
      }
    }

    return childs;
  }

  /**
   * Delete any Folder and Mark that is child of folders contained in the list.
   *
   * @param entities the list to scan for elements that are already contained in
   * existing folders.
   */
  public static void normalize(List<? extends IEntity> entities) {
    if (entities == null)
      return;

    /* Find Folders */
    List<IFolder> folders = null;
    for (Object element : entities) {
      if (element instanceof IFolder) {
        if (folders == null)
          folders = new ArrayList<IFolder>();
        folders.add((IFolder) element);
      }
    }

    /* Normalize */
    if (folders != null) {
      for (IFolder folder : folders) {
        CoreUtils.normalize(folder, entities);
      }
    }
  }

  /**
   * Delete any Folder and Mark that is child of the given Folder
   *
   * @param folder
   * @param entities
   */
  public static void normalize(IFolder folder, List<? extends IEntity> entities) {

    /* Cleanup Marks */
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks)
      entities.remove(mark);

    /* Cleanup Folders and recursively treat Subfolders */
    List<IFolder> subFolders = folder.getFolders();
    for (IFolder subFolder : subFolders) {
      entities.remove(subFolder);
      normalize(subFolder, entities);
    }
  }

  /**
   * Returns a Headline for the given News. In general this will be the Title of
   * the News, but if not provided, parts of the Content will be taken instead.
   *
   * @param news The News to get the Headline from.
   * @param replaceEntities <code>true</code> to replace entities and
   * <code>false</code> otherwise.
   * @return the Headline of the given News or "No Headline" if none.
   */
  public static String getHeadline(INews news, boolean replaceEntities) {

    /* Title provided */
    String title = StringUtils.stripTags(news.getTitle(), replaceEntities);
    title = StringUtils.normalizeString(title);
    if (StringUtils.isSet(title))
      return title;

    /* Try Content instead */
    String content = news.getDescription();
    if (StringUtils.isSet(content)) {
      content = StringUtils.stripTags(content, replaceEntities);
      content = StringUtils.normalizeString(content);
      content = StringUtils.smartTrim(content, 50);

      if (StringUtils.isSet(content))
        return content;
    }

    return Messages.CoreUtils_NO_HEADLINE;
  }

  /**
   * @param news the news to obtain the link from. Will fall back to the
   * {@link IGuid} if necessary.
   * @return the link that can be used to open the news in a browser or
   * <code>null</code> if none.
   */
  public static String getLink(INews news) {
    String linkAsStr = getLinkAsString(news);
    if (linkAsStr != null) {

      /* Return early if link is absolute (includes protocol identifier) */
      if (linkAsStr.contains(URIUtils.PROTOCOL_IDENTIFIER))
        return linkAsStr;

      /* Append missing protocol if this is a web link */
      else if (linkAsStr.startsWith("www.")) //$NON-NLS-1$
        return URIUtils.HTTP + linkAsStr;

      /* Now try to resolve the relative link as absolute if a base is provided */
      URI base = news.getBase();
      if (base != null) {
        try {
          return URIUtils.resolve(base, new URI(linkAsStr)).toString();
        } catch (URISyntaxException e) {
          return linkAsStr;
        }
      }

      /* Finally resolve against Feed Link */
      try {
        return URIUtils.resolve(new URI(news.getFeedLinkAsText()), new URI(linkAsStr)).toString();
      } catch (URISyntaxException e) {
        return linkAsStr;
      }
    }

    return null;
  }

  private static String getLinkAsString(INews news) {

    /* Check Link Provided */
    String link = news.getLinkAsText();
    if (StringUtils.isSet(link))
      return link;

    /* Fallback to Guid if available */
    IGuid guid = news.getGuid();
    if (guid != null) {
      String value = guid.getValue();
      if (URIUtils.looksLikeLink(value))
        return value;
    }

    return null;
  }

  /**
   * Normalizes the given Title by removing various kinds of response codes
   * (e.g. Re).
   *
   * @param title The title to normalize.
   * @return Returns the normalized Title (that is, response codes have been
   * removed).
   */
  public static String normalizeTitle(String title) {

    /* Check that title is provided, otherwise return */
    if (!StringUtils.isSet(title))
      return title;

    String normalizedTitle = null;
    int start = 0;
    int len = title.length();
    boolean done = false;

    /* Strip response codes */
    while (!done) {
      done = true;

      /* Skip Whitespaces */
      while (start < len && title.charAt(start) == ' ')
        start++;

      if (start < (len - 2)) {
        char c1 = title.charAt(start);
        char c2 = title.charAt(start + 1);
        char c3 = title.charAt(start + 2);

        /* Beginning "Re" */
        if ((c1 == 'r' || c1 == 'R') && (c2 == 'e' || c2 == 'E')) {

          /* Skip "Re:" */
          if (c3 == ':') {
            start += 3;
            done = false;
          }

          /* Skip numbered response codes like [12] */
          else if (start < (len - 2) && (c3 == '[' || c3 == '(')) {
            int i = start + 3;

            /* Skip entire number */
            while (i < len && title.charAt(i) >= '0' && title.charAt(i) <= '9')
              i++;

            char ci1 = title.charAt(i);
            char ci2 = title.charAt(i + 1);
            if (i < (len - 1) && (ci1 == ']' || ci1 == ')') && ci2 == ':') {
              start = i + 2;
              done = false;
            }
          }
        }
      }

      int end = len;

      /* Unread whitespace */
      while (end > start && title.charAt(end - 1) < ' ')
        end--;

      /* Build simplified Title */
      if (start == 0 && end == len)
        normalizedTitle = title;
      else
        normalizedTitle = title.substring(start, end);
    }

    return normalizedTitle;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case the <code>INews.State.NEW</code> changed
   * its value for any of the given Events, <code>FALSE</code> otherwise.
   */
  public static boolean isNewStateChange(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;
        boolean oldStateNew = INews.State.NEW.equals(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
        boolean currentStateNew = INews.State.NEW.equals(newsEvent.getEntity().getState());

        if (oldStateNew != currentStateNew)
          return true;
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case the Sticky-State of the given News
   * changed its value for any of the given Events, <code>FALSE</code>
   * otherwise.
   */
  public static boolean isStickyStateChange(Set<? extends ModelEvent> events) {
    return isStickyStateChange(events, false);
  }

  /**
   * @param newsEvent
   * @return <code>TRUE</code> in case the Sticky-State of the given News
   * changed its value for any of the given Events, <code>FALSE</code>
   * otherwise.
   */
  public static boolean isStickyStateChange(NewsEvent newsEvent) {
    boolean oldSticky = (newsEvent.getOldNews() != null) ? newsEvent.getOldNews().isFlagged() : false;
    boolean currentSticky = newsEvent.getEntity().isVisible() && newsEvent.getEntity().isFlagged();

    return oldSticky != currentSticky;
  }

  /**
   * @param events
   * @param onlyHasBecomeSticky if <code>true</code>, only return
   * <code>true</code> if a news has become sticky.
   * @return <code>TRUE</code> in case the Sticky-State of the given News
   * changed its value for any of the given Events, <code>FALSE</code>
   * otherwise. Respects the onlyHasBecomeSticky parameter.
   */
  public static boolean isStickyStateChange(Set<? extends ModelEvent> events, boolean onlyHasBecomeSticky) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;
        boolean oldSticky = (newsEvent.getOldNews() != null) ? newsEvent.getOldNews().isFlagged() : false;
        boolean currentSticky = newsEvent.getEntity().isVisible() && newsEvent.getEntity().isFlagged();

        /* Only return true if sticky state is now TRUE */
        if (onlyHasBecomeSticky) {
          if (!oldSticky && currentSticky)
            return true;
        }

        /* Return TRUE on sticky state change */
        else {
          if (oldSticky != currentSticky)
            return true;
        }
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any state changed from NEW, UPDATED or
   * UNREAD to a different one, <code>FALSE</code> otherwise.
   */
  public static boolean isReadStateChange(Set<? extends ModelEvent> events) {
    return isReadStateChange(events, false);
  }

  /**
   * @param events
   * @param onlyHasBecomeUnread if <code>true</code>, only return
   * <code>true</code> if a news has become unread.
   * @return <code>TRUE</code> in case any state changed from NEW, UPDATED or
   * UNREAD to a different one, <code>FALSE</code> otherwise. Respects the
   * onlyHasBecomeUnread parameter.
   */
  public static boolean isReadStateChange(Set<? extends ModelEvent> events, boolean onlyHasBecomeUnread) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;
        boolean oldStateUnread = isUnread(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
        boolean newStateUnread = isUnread(newsEvent.getEntity().getState());

        /* Only return true if unread state is now TRUE */
        if (onlyHasBecomeUnread) {
          if (!oldStateUnread && newStateUnread)
            return true;
        }

        /* Return TRUE on sticky state change */
        else {
          if (oldStateUnread != newStateUnread)
            return true;
        }
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case the <code>INews.State.NEW</code> or any
   * unread-state (NEW, UPDATED, UNREAD) changed its value for any of the given
   * Events, <code>FALSE</code> otherwise.
   */
  public static boolean isNewOrReadStateChange(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;

        boolean oldStateNew = INews.State.NEW.equals(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
        boolean currentStateNew = INews.State.NEW.equals(newsEvent.getEntity().getState());

        if (oldStateNew != currentStateNew)
          return true;

        boolean oldStateUnread = isUnread(newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null);
        boolean newStateUnread = isUnread(newsEvent.getEntity().getState());

        if (oldStateUnread != newStateUnread)
          return true;
      }
    }

    return false;
  }

  /**
   * @param state
   * @return TRUE if the State is NEW, UPDATED or UNREAD and FALSE otherwise.
   */
  public static boolean isUnread(INews.State state) {
    return state == INews.State.NEW || state == INews.State.UPDATED || state == INews.State.UNREAD;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any State changed for ther given Events,
   * <code>FALSE</code> otherwise.
   */
  public static boolean isStateChange(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;
        INews.State oldState = newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null;
        if (oldState != newsEvent.getEntity().getState())
          return true;
      }
    }

    return false;
  }

  /**
   * @param newsEvent
   * @return <code>TRUE</code> in case any State changed for ther given Events,
   * <code>FALSE</code> otherwise.
   */
  public static boolean isStateChange(NewsEvent newsEvent) {
    INews.State oldState = newsEvent.getOldNews() != null ? newsEvent.getOldNews().getState() : null;
    if (oldState != newsEvent.getEntity().getState())
      return true;

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the News got deleted and
   * <code>FALSE</code> otherwise.
   */
  public static boolean gotDeleted(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;

        boolean isVisible = newsEvent.getEntity().isVisible();
        boolean wasVisible = newsEvent.getOldNews() != null ? newsEvent.getOldNews().isVisible() : false;

        if (!isVisible && wasVisible)
          return true;
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the News got restored and
   * <code>FALSE</code> otherwise.
   */
  public static boolean gotRestored(Set<? extends ModelEvent> events) {
    for (ModelEvent event : events) {
      if (event instanceof NewsEvent) {
        NewsEvent newsEvent = (NewsEvent) event;

        boolean isVisible = newsEvent.getEntity().isVisible();
        boolean wasVisible = newsEvent.getOldNews() != null ? newsEvent.getOldNews().isVisible() : false;

        if (isVisible && !wasVisible && newsEvent.getOldNews() != null)
          return true;
      }
    }

    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Publish-Date of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isDateChange(Set<? extends ModelEvent> events) {
    for (ModelEvent modelEvent : events) {
      if (modelEvent instanceof NewsEvent) {
        NewsEvent event = (NewsEvent) modelEvent;

        Date oldDate = event.getOldNews() != null ? DateUtils.getRecentDate(event.getOldNews()) : null;
        Date newDate = DateUtils.getRecentDate(event.getEntity());

        if (!newDate.equals(oldDate))
          return true;
      }
    }
    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Author of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isAuthorChange(Set<? extends ModelEvent> events) {
    for (ModelEvent modelEvent : events) {
      if (modelEvent instanceof NewsEvent) {
        NewsEvent event = (NewsEvent) modelEvent;

        IPerson oldAuthor = event.getOldNews() != null ? event.getOldNews().getAuthor() : null;
        IPerson newAuthor = event.getEntity().getAuthor();

        if (newAuthor != null && !newAuthor.equals(oldAuthor))
          return true;
        else if (oldAuthor != null && !oldAuthor.equals(newAuthor))
          return true;
      }
    }
    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Category of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isCategoryChange(Set<? extends ModelEvent> events) {
    for (ModelEvent modelEvent : events) {
      if (modelEvent instanceof NewsEvent) {
        NewsEvent event = (NewsEvent) modelEvent;

        List<ICategory> oldCategories = event.getOldNews() != null ? event.getOldNews().getCategories() : null;
        List<ICategory> newCategories = event.getEntity().getCategories();

        if (!newCategories.equals(oldCategories))
          return true;
      }
    }
    return false;
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Label of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isLabelChange(Set<? extends ModelEvent> events) {
    for (ModelEvent modelEvent : events) {
      if (modelEvent instanceof NewsEvent) {
        NewsEvent event = (NewsEvent) modelEvent;

        Set<ILabel> oldLabels = event.getOldNews() != null ? event.getOldNews().getLabels() : null;
        Set<ILabel> newLabels = event.getEntity().getLabels();

        if (!newLabels.equals(oldLabels))
          return true;
      }
    }
    return false;
  }

  /**
   * @param event
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Label of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isLabelChange(NewsEvent event) {
    Set<ILabel> oldLabels = event.getOldNews() != null ? event.getOldNews().getLabels() : null;
    Set<ILabel> newLabels = event.getEntity().getLabels();

    return !newLabels.equals(oldLabels);
  }

  /**
   * @param events
   * @return <code>TRUE</code> in case any of the events tell about a change in
   * the Title of the News, <code>FALSE</code> otherwise.
   */
  public static boolean isTitleChange(Set<? extends ModelEvent> events) {
    for (ModelEvent modelEvent : events) {
      if (modelEvent instanceof NewsEvent) {
        NewsEvent event = (NewsEvent) modelEvent;

        String oldTopic = event.getOldNews() != null ? getHeadline(event.getOldNews(), true) : null;
        String newTopic = getHeadline(event.getEntity(), true);

        if (!newTopic.equals(oldTopic))
          return true;
      }
    }
    return false;
  }

  /**
   * @param parent
   * @param entityToCheck
   * @return <code>TRUE</code> in case the given Entity is a child of the given
   * Folder, <code>FALSE</code> otherwise.
   */
  public static boolean hasChildRelation(IFolder parent, IEntity entityToCheck) {
    if (entityToCheck instanceof IFolder) {
      IFolder folder = (IFolder) entityToCheck;
      if (parent.equals(folder))
        return true;

      return hasChildRelation(parent, folder.getParent());
    }

    else if (entityToCheck instanceof IMark) {
      IMark mark = (IMark) entityToCheck;
      if (mark.getParent().equals(parent))
        return true;

      return hasChildRelation(parent, mark.getParent());
    }

    return false;
  }

  /**
   * Returns a Set of all Links that are added as Bookmarks.
   *
   * @return Returns a Set of all Links that are added as Bookmarks.
   */
  public static Set<String> getFeedLinks() {
    IBookMarkDAO bookMarkDAO = Owl.getPersistenceService().getDAOService().getBookMarkDAO();
    Collection<IBookMark> bookMarks = bookMarkDAO.loadAll();
    Set<String> links = new HashSet<String>(bookMarks.size());

    for (IBookMark bookmark : bookMarks) {
      links.add(bookmark.getFeedLinkReference().getLinkAsText());
    }

    return links;
  }

  /**
   * Returns the first <code>IBookMark</code> that references the same feed as
   * <code>feedRef</code> or <code>null</code> if none.
   *
   * @param feedRef The desired Feed.
   * @return Returns the first <code>IBookMark</code> that references the given
   * Feed or <code>null</code> if none.
   */
  public static IBookMark getBookMark(FeedLinkReference feedRef) {
    IBookMarkDAO bookMarkDAO = Owl.getPersistenceService().getDAOService().getBookMarkDAO();
    Collection<IBookMark> bookMarks = bookMarkDAO.loadAll();
    for (IBookMark bookMark : bookMarks) {
      if (bookMark.getFeedLinkReference().equals(feedRef))
        return bookMark;
    }

    return null;
  }

  /**
   * Returns the first <code>IBookMark</code> that references the same feed as
   * <code>feedRef</code> or <code>null</code> if none.
   *
   * @param feedRef The desired Feed.
   * @return Returns the first <code>IBookMark</code> that references the given
   * Feed or <code>null</code> if none.
   */
  public static IBookMark getBookMark(String feedRef) {
    IBookMarkDAO bookMarkDAO = Owl.getPersistenceService().getDAOService().getBookMarkDAO();
    Collection<IBookMark> bookMarks = bookMarkDAO.loadAll();
    for (IBookMark bookMark : bookMarks) {
      if (bookMark.getFeedLinkReference().getLinkAsText().equals(feedRef))
        return bookMark;
    }

    return null;
  }

  /**
   * @param news
   * @return Returns a Map mapping from a news-state to a list of
   * news-references.
   */
  public static Map<INews.State, List<NewsReference>> toStateMap(Collection<INews> news) {
    Map<INews.State, List<NewsReference>> map = new HashMap<State, List<NewsReference>>();
    for (INews newsitem : news) {
      INews.State state = newsitem.getState();
      List<NewsReference> newsrefs = map.get(state);
      if (newsrefs == null) {
        newsrefs = new ArrayList<NewsReference>();
        map.put(state, newsrefs);
      }

      newsrefs.add(newsitem.toReference());
    }

    return map;
  }

  /**
   * @param map
   * @return Returns a List of all News resolved.
   */
  public static List<INews> resolveAll(Map<State, List<NewsReference>> map) {
    List<INews> news = new ArrayList<INews>();

    Collection<List<NewsReference>> values = map.values();
    for (List<NewsReference> value : values) {
      for (NewsReference newsRef : value) {
        INews newsitem = newsRef.resolve();
        if (newsitem != null)
          news.add(newsitem);
      }
    }

    return news;
  }

  /**
   * @param conditions
   * @param ignoreStopWords
   * @return Returns a set of words from the given conditions.
   */
  public static Set<String> extractWords(List<ISearchCondition> conditions, boolean ignoreStopWords) {
    Set<String> words = new HashSet<String>(conditions.size());

    /* Check Search Conditions for String-Values */
    for (ISearchCondition cond : conditions) {
      if (cond.getValue() instanceof String) {
        String value = cond.getValue().toString();

        /* Ignore Wildcard Only Values (e.g. search for Labels) */
        if ("?".equals(value) || "*".equals(value)) //$NON-NLS-1$//$NON-NLS-2$
          continue;

        /* Split into Words */
        value = StringUtils.replaceAll(value, "\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
        StringTokenizer tokenizer = new StringTokenizer(value);
        while (tokenizer.hasMoreElements()) {
          String nextWord = tokenizer.nextElement().toString().toLowerCase();

          /* Ignore Stop Words if required */
          if (!ignoreStopWords || !STOP_WORDS.contains(nextWord))
            words.add(nextWord);
        }
      }
    }

    return words;
  }

  /**
   * @param news the {@link INews} to check.
   * @return <code>true</code> if the content is either empty or identical with
   * the title, <code>false</code> otherwise.
   */
  public static boolean isEmpty(INews news) {
    if (!StringUtils.isSet(news.getDescription()))
      return true;

    if (StringUtils.isSet(news.getTitle()) && news.getTitle().equals(news.getDescription()))
      return true;

    return false;
  }

  /**
   * @return all root folders sorted by their ID.
   */
  public static Set<IFolder> loadRootFolders() {

    /* Sort by ID to respect order */
    Set<IFolder> rootFolders = new TreeSet<IFolder>(new Comparator<IFolder>() {
      public int compare(IFolder f1, IFolder f2) {
        if (f1.equals(f2))
          return 0;

        return f1.getId() < f2.getId() ? -1 : 1;
      }
    });

    /* Add Root-Folders */
    rootFolders.addAll(DynamicDAO.getDAO(IFolderDAO.class).loadRoots());

    return rootFolders;
  }

  /**
   * @return all saved searches sorted by name.
   */
  public static Set<ISearchMark> loadSortedSearchMarks() {

    /* Sort by Sort Key to respect order */
    Set<ISearchMark> searchmarks = new TreeSet<ISearchMark>(new Comparator<ISearchMark>() {
      public int compare(ISearchMark s1, ISearchMark s2) {
        if (s1.getName().equalsIgnoreCase(s2.getName())) //Duplicate names are allowed!
          return -1;

        return s1.getName().compareToIgnoreCase(s2.getName());
      }
    });

    /* Add Searchmarks */
    searchmarks.addAll(DynamicDAO.loadAll(ISearchMark.class));

    return searchmarks;
  }

  /**
   * @return all news filters sorted by name.
   */
  public static Set<ISearchFilter> loadSortedNewsFilters() {

    /* Sort by Sort Key to respect order */
    Set<ISearchFilter> filters = new TreeSet<ISearchFilter>(new Comparator<ISearchFilter>() {
      public int compare(ISearchFilter f1, ISearchFilter f2) {
        if (f1.getName().equalsIgnoreCase(f2.getName())) //Duplicate names are allowed!
          return -1;

        return f1.getName().compareToIgnoreCase(f2.getName());
      }
    });

    /* Add Filters */
    filters.addAll(DynamicDAO.loadAll(ISearchFilter.class));

    return filters;
  }

  /**
   * @param news the news to obtain the labels from.
   * @return all labels sorted by their sort value from the given news or an
   * empty {@link Set} if none.
   */
  public static Set<ILabel> getSortedLabels(INews news) {
    Set<ILabel> newsLabels = news.getLabels();
    if (newsLabels.isEmpty())
      return newsLabels;

    return sortLabels(newsLabels);
  }

  /**
   * @return all labels sorted by their sort value.
   */
  public static Set<ILabel> loadSortedLabels() {
    return sortLabels(DynamicDAO.loadAll(ILabel.class));
  }

  private static Set<ILabel> sortLabels(Collection<ILabel> labels) {

    /* Sort by Sort Key to respect order */
    Set<ILabel> sortedLabels = new TreeSet<ILabel>(new Comparator<ILabel>() {
      public int compare(ILabel l1, ILabel l2) {
        if (l1.equals(l2))
          return 0;

        return l1.getOrder() < l2.getOrder() ? -1 : 1;
      }
    });

    /* Add Labels */
    sortedLabels.addAll(labels);

    return sortedLabels;
  }

  /**
   * @param events a {@link Set} of news events.
   * @param state the {@link State} to search for.
   * @return <code>true</code> if any of the events has the given state and
   * <code>false</code> otherwise.
   */
  public static boolean containsState(Set<NewsEvent> events, INews.State state) {
    for (NewsEvent event : events) {
      INews entity = event.getEntity();
      if (entity != null && entity.getState() == state)
        return true;
    }

    return false;
  }

  /**
   * @param conditions the conditions to split into scope and other conditions.
   * @return a {@link Pair} containing a condition for the scope or
   * <code>null</code> and the other conditions that are from different type.
   */
  public static Pair<ISearchCondition, List<ISearchCondition>> splitScope(List<ISearchCondition> conditions) {
    if (conditions == null)
      return null;

    ISearchCondition scope = null;
    List<ISearchCondition> otherConditions = new ArrayList<ISearchCondition>(conditions.size());

    for (ISearchCondition condition : conditions) {
      if (condition.getSpecifier() == SearchSpecifier.SCOPE)
        scope = condition;
      else
        otherConditions.add(condition);
    }

    return Pair.create(scope, otherConditions);
  }

  /**
   * @param conditions the list of search conditions.
   * @return <code>true</code> if there are conflicting location conditions and
   * <code>false</code> otherwise.
   */
  public static boolean isLocationConflict(List<ISearchCondition> conditions) {
    if (conditions == null || conditions.isEmpty())
      return false;

    Pair<ISearchCondition, List<ISearchCondition>> splitConditions = splitScope(conditions);
    if (splitConditions.getFirst() == null)
      return false;

    for (ISearchCondition condition : splitConditions.getSecond()) {
      if (condition.getField().getId() == INews.LOCATION)
        return true;
    }

    return false;
  }

  /**
   * Copies the contents of one stream to another.
   *
   * @param fis the input stream to read from.
   * @param fos the output stream to write to.
   */
  public static void copy(InputStream fis, OutputStream fos) {
    try {
      byte buffer[] = new byte[0xffff];
      int nbytes;

      while ((nbytes = fis.read(buffer)) != -1)
        fos.write(buffer, 0, nbytes);
    } catch (IOException e) {
      if (!(e instanceof MonitorCanceledException))
        Activator.safeLogError(e.getMessage(), e);
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          if (!(e instanceof MonitorCanceledException))
            Activator.safeLogError(e.getMessage(), e);
        }
      }

      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          if (!(e instanceof MonitorCanceledException))
            Activator.safeLogError(e.getMessage(), e);
        }
      }
    }
  }

  /**
   * @param fileName the name of the file to write the content into.
   * @param content the content to write into the file as {@link StringBuilder}.
   */
  public static void write(String fileName, StringBuilder content) {
    OutputStreamWriter writer = null;
    try {
      writer = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"); //$NON-NLS-1$
      writer.write(content.toString());
      writer.close();
    } catch (IOException e) {
      Activator.safeLogError(e.getMessage(), e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          Activator.safeLogError(e.getMessage(), e);
        }
      }
    }
  }

  /**
   * @param <T> the type of elements of the list.
   * @param list a list to remove duplicates from using object identity equal
   * checks.
   * @return returns a list where all duplicates are removed using object
   * identiy equalness.
   */
  public static <T> List<T> removeIdentityDuplicates(List<T> list) {
    List<T> newList = new ArrayList<T>(list.size());
    Map<T, T> identityMap = new IdentityHashMap<T, T>();
    for (T t : list) {
      if (!identityMap.containsKey(t)) {
        newList.add(t);
        identityMap.put(t, t);
      }
    }

    return newList;
  }

  /**
   * @param reparenting
   */
  public static void reparentWithProperties(List<ReparentInfo<IFolderChild, IFolder>> reparenting) {

    /* Copy over Properties from new Parent that are unset in folder child */
    for (ReparentInfo<IFolderChild, IFolder> info : reparenting) {
      IFolderChild objToReparent = info.getObject();
      IFolder newParent = info.getNewParent();
      Set<Entry<String, Serializable>> set = newParent.getProperties().entrySet();
      for (Entry<String, Serializable> entry : set) {
        if (objToReparent.getProperty(entry.getKey()) == null)
          objToReparent.setProperty(entry.getKey(), entry.getValue());
      }
    }

    /* Perform Reparenting */
    Owl.getPersistenceService().getDAOService().getFolderDAO().reparent(reparenting);
  }

  /**
   * Check if the given searchmark is already existing in the set of
   * subscriptions by comparing names of all parents and conditions.
   *
   * @param search the searchmark to find in the current set of subscriptions.
   * @return <code>true</code> if there is a {@link ISearchMark} that matches
   * the name of the given search including the names of all parent folders or
   * and its conditions or <code>false</code> otherwise.
   */
  public static boolean existsSearchMark(ISearchMark search) {
    if (search == null || search.getParent() == null)
      return false;

    IFolder folder = findFolder(search.getParent());
    if (folder != null) {
      List<IMark> marks = folder.getMarks();
      for (IMark mark : marks) {
        if (mark instanceof ISearchMark) {
          ISearchMark other = (ISearchMark) mark;
          if (isIdentical(search, other))
            return true;
        }
      }
    }

    return false;
  }

  private static final boolean isIdentical(ISearchMark s1, ISearchMark s2) {

    /* Check "matchAllConditions" */
    if (s1.matchAllConditions() != s2.matchAllConditions())
      return false;

    List<ISearchCondition> conditions1 = s1.getSearchConditions();
    List<ISearchCondition> conditions2 = s2.getSearchConditions();

    /* Check on Number of Conditions */
    if (conditions1.size() != conditions2.size())
      return false;

    /* Compare Conditions */
    for (int i = 0; i < conditions1.size(); i++) {
      ISearchCondition condition1 = conditions1.get(i);
      ISearchCondition condition2 = conditions2.get(i);

      /* Check Field */
      if (condition1.getField().getId() != condition2.getField().getId())
        return false;

      /* Check Specifier */
      if (condition1.getSpecifier() != condition2.getSpecifier())
        return false;

      //TODO We need to properly support Locations too, but the IDs may differ
      if (condition1.getField().getId() == INews.LOCATION || condition2.getField().getId() == INews.LOCATION)
        return false;

      /* Check Value */
      if (condition1.getValue() != null && !condition1.getValue().equals(condition2.getValue()))
        return false;
      else if (condition1.getValue() == null && condition2.getValue() != null)
        return false;
    }

    return true;
  }

  /**
   * Check if the given bin is already existing in the set of subscriptions by
   * comparing names of all parents.
   *
   * @param bin the bin to find in the current set of subscriptions.
   * @return <code>true</code> if there is a {@link INewsBin} that matches the
   * name of the given bin including the names of all parent folders or
   * <code>false</code> otherwise.
   */
  public static boolean existsNewsBin(INewsBin bin) {
    if (bin == null || bin.getParent() == null)
      return false;

    IFolder folder = findFolder(bin.getParent());
    if (folder != null) {
      List<IMark> marks = folder.getMarks();
      for (IMark mark : marks) {
        if (mark instanceof INewsBin && mark.getName().equals(bin.getName()))
          return true;
      }
    }

    return false;
  }

  /**
   * Check if the given folder is already existing in the set of folders by
   * comparing names of all parents.
   *
   * @param folder the folder to find in the current set of folders.
   * @return the {@link IFolder} that matches the name of the given folder
   * including the names of all parent folders or <code>null</code> if none.
   */
  public static IFolder findFolder(IFolder folder) {
    if (folder == null)
      return null;

    /* Load Roots */
    Collection<IFolder> folders = loadRootFolders();
    if (folders.isEmpty())
      return null;

    /* Use oldest Root Folder as Default if required */
    String defaultSetName = folders.iterator().next().getName();

    /* Build a Chain of all Parent Names starting from Root */
    List<String> folderNameChain = new ArrayList<String>();
    if (folder.getProperty(ITypeImporter.TEMPORARY_FOLDER) != null)
      folderNameChain.add(defaultSetName);
    else
      folderNameChain.add(folder.getName());

    IFolder parent = folder;
    while ((parent = parent.getParent()) != null) {
      if (parent.getProperty(ITypeImporter.TEMPORARY_FOLDER) != null)
        folderNameChain.add(defaultSetName);
      else
        folderNameChain.add(parent.getName());
    }

    /* Reverse so that parents appear first */
    Collections.reverse(folderNameChain);

    /* Search the Folder by Name using the Chain */
    IFolder foundFolder = null;
    for (String folderNameChainValue : folderNameChain) {
      foundFolder = findFolderByName(folderNameChainValue, folders);
      if (foundFolder != null)
        folders = foundFolder.getFolders();
      else
        return null;
    }

    return foundFolder;
  }

  private static IFolder findFolderByName(String name, Collection<IFolder> folders) {
    for (IFolder folder : folders) {
      if (folder.getName().equals(name))
        return folder;
    }

    return null;
  }

  /**
   * @param reader a {@link BufferedReader} to read from. The caller is
   * responsible to close any streams associated with it.
   * @param base the base {@link URI} to resolve any relative {@link URI}
   * against.
   * @return a {@link URI} of a feed found in the content of the reader or
   * <code>null</code> if none.
   */
  public static URI findFeed(BufferedReader reader, URI base) {
    return findUri(reader, base, FEED_MIME_TYPES);
  }

  /**
   * @param reader a {@link BufferedReader} to read from. The caller is
   * responsible to close any streams associated with it.
   * @param base the base {@link URI} to resolve any relative {@link URI}
   * against.
   * @return a {@link URI} of a favicon found in the content of the reader or
   * <code>null</code> if none.
   */
  public static URI findFavicon(BufferedReader reader, URI base) {
    return findUri(reader, base, FAVICON_MARKERS);
  }

  private static URI findUri(BufferedReader reader, URI base, String[] markers) {
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        for (String marker : markers) {
          int index = line.indexOf(marker);

          /* Marker Found */
          if (index > -1) {

            /* Set index to where the Link Element starts */
            for (int i = index; i >= 0; i--) {
              if (line.charAt(i) == '<') {
                index = i;
                break;
              }
            }

            /* Find the HREF */
            String usedHref = null;
            int hrefIndex = -1;
            for (String href : HREF_VARIANTS) {
              hrefIndex = line.indexOf(href, index);
              if (hrefIndex != -1) {
                usedHref = href;
                break;
              }
            }

            if (hrefIndex > -1 && usedHref != null) {
              boolean inQuotes = false;
              StringBuilder str = new StringBuilder();
              for (int i = hrefIndex + usedHref.length(); i < line.length(); i++) {
                char c = line.charAt(i);

                if (c == '\"' || c == '\'') {
                  if (inQuotes)
                    break;

                  inQuotes = true;
                  continue;
                }

                if (Character.isWhitespace(c) || c == '>')
                  break;

                str.append(c);
              }

              String linkVal = str.toString();

              /* Convert &amp; to & as it is a common character in a URL */
              linkVal = StringUtils.replaceAll(linkVal, "&amp;", "&"); //$NON-NLS-1$ //$NON-NLS-2$

              /* Handle relative Links */
              try {
                URI uri = new URI(linkVal);
                linkVal = URIUtils.resolve(base, uri).toString();
              }

              /* Fallback if URI is not valid */
              catch (URISyntaxException e) {
                if (!linkVal.contains("://")) { //$NON-NLS-1$
                  try {
                    if (!linkVal.startsWith("/")) //$NON-NLS-1$
                      linkVal = "/" + linkVal; //$NON-NLS-1$
                    linkVal = base.resolve(linkVal).toString();
                  } catch (IllegalArgumentException e1) {
                    linkVal = linkVal.startsWith("/") ? base.toString() + linkVal : base.toString() + "/" + linkVal; //$NON-NLS-1$ //$NON-NLS-2$
                  }
                }
              }

              return new URI(URIUtils.fastEncode(linkVal));
            }
          }
        }
      }
    } catch (IOException e) {
      if (!(e instanceof MonitorCanceledException))
        Activator.safeLogError(e.getMessage(), e);
    } catch (URISyntaxException e) {
      Activator.safeLogError(e.getMessage(), e);
    }

    return null;
  }

  /**
   * @param ex the {@link CoreException} that occured.
   * @return a human readable message for the occured exception.
   */
  public static String toMessage(Exception ex) {

    /* Specially treat InvocationTargetExceptions */
    if (ex instanceof InvocationTargetException && ex.getCause() != null && ex.getCause() instanceof Exception) {
      ex = (Exception) ex.getCause();
    }

    /* Protocol Unsupported */
    if (ex instanceof UnknownProtocolException) {
      String protocol = ((UnknownProtocolException) ex).getProtocol();
      return NLS.bind(Messages.CoreUtils_UNSUPPORTED_PROTOCOL, protocol);
    }

    /* Format Unsupported */
    if (ex instanceof UnknownFormatException) {
      String format = ((UnknownFormatException) ex).getFormat();
      return NLS.bind(Messages.CoreUtils_UNSUPPORTED_FORMAT, format);
    }

    /* Issues Interpreting the Feed */
    if (ex instanceof InterpreterException && ex.getCause() == null) {
      return Messages.CoreUtils_INVALID_FEED;
    }

    /* Issues Parsing XML */
    if (ex instanceof ParserException) {
      return Messages.CoreUtils_INVALID_FEED;
    }

    /* Issues Connecting to a remote resource */
    if (ex instanceof ConnectionException) {
      Throwable cause = ex.getCause();

      /* Signals that a timeout has occurred on a socket read or accept */
      if (cause instanceof SocketTimeoutException)
        return Messages.CoreUtils_CONNECTION_TIMEOUT;

      /*
       * Thrown to indicate that the IP address of a host could not be
       * determined
       */
      if (cause instanceof UnknownHostException)
        return Messages.CoreUtils_UNABLE_RESOLVE_HOST;

      /*
       * Signals that an error occurred while attempting to connect a socket to
       * a remote address and port
       */
      if (cause instanceof ConnectException)
        return Messages.CoreUtils_UNABLE_CONNECT;

      /*
       * Thrown to indicate that there is an error in the underlying protocol,
       * such as a TCP error
       */
      if (cause instanceof SocketException)
        return Messages.CoreUtils_UNNABLE_CONNECT;
    }

    return ex.getMessage();
  }

  /**
   * @param fileName the proposed filename.
   * @return a filename that is safe to be used on Windows.
   */
  public static String getSafeFileNameForWindows(String fileName) {
    String candidate = fileName;
    for (String reservedChar : RESERVED_FILENAME_CHARACTERS_WINDOWS) {
      candidate = StringUtils.replaceAll(candidate, reservedChar, ""); //$NON-NLS-1$
    }

    return candidate;
  }

  /**
   * @return the rssowl user agent string.
   */
  public static String getUserAgent() {
    String version = Activator.getDefault().getVersion();
    String os = Platform.getOS();
    if (Platform.OS_WIN32.equals(os))
      return "RSSOwl/" + version + " (Windows; U; en)"; //$NON-NLS-1$ //$NON-NLS-2$
    else if (Platform.OS_LINUX.equals(os))
      return "RSSOwl/" + version + " (X11; U; en)"; //$NON-NLS-1$//$NON-NLS-2$
    else if (Platform.OS_MACOSX.equals(os))
      return "RSSOwl/" + version + " (Macintosh; U; en)"; //$NON-NLS-1$ //$NON-NLS-2$
    return "RSSOwl/" + version; //$NON-NLS-1$
  }

  /**
   * @param news a {@link List} of {@link INews} that are potentially replaced
   * with versions from the provided {@link Map}.
   * @param replacements a {@link Map} of {@link INews} that are to replace
   * other {@link INews} or <code>null</code> if none.
   * @return the replaced version of the news list. Will be identical if the
   * replacements map is empty or <code>null</code>.
   */
  public static List<INews> replace(List<INews> news, Map<INews, INews> replacements) {
    if (replacements == null || replacements.isEmpty())
      return news;

    List<INews> replacedNews = new ArrayList<INews>();
    for (INews newsitem : news) {
      INews replacedItem = replacements.get(newsitem);
      if (replacedItem != null)
        replacedNews.add(replacedItem);
      else
        replacedNews.add(newsitem);
    }

    return replacedNews;
  }

  /**
   * @param str the String to append to the Log Buffer.
   */
  public static void appendLogMessage(String str) {
    if (str != null)
      fgLogBuffer.append(str);
  }

  /**
   * @return the collection of Log entries for this session. The collection is
   * cleared when calling this method to avoid duplicate logging.
   */
  public static String getAndFlushLogMessages() {
    String str = fgLogBuffer.toString();
    fgLogBuffer.setLength(0);
    return str;
  }

  /**
   * @param bookmarks the collection of bookmarks to fill from the given
   * folders.
   * @param folders the folders to extract all bookmarks from.
   */
  public static void fillBookMarks(Collection<IBookMark> bookmarks, Collection<IFolder> folders) {
    for (IFolder folder : folders) {
      List<IFolderChild> children = folder.getChildren();
      for (IFolderChild child : children) {
        if (child instanceof IBookMark)
          bookmarks.add((IBookMark) child);
        else if (child instanceof IFolder)
          fillBookMarks(bookmarks, Collections.singleton((IFolder) child));
      }
    }
  }

  /**
   * @param searchmarks the collection of searchmarks to fill from the given
   * folders.
   * @param folders the folders to extract all searchmarks from.
   */
  public static void fillSearchMarks(Collection<ISearchMark> searchmarks, Collection<IFolder> folders) {
    for (IFolder folder : folders) {
      List<IFolderChild> children = folder.getChildren();
      for (IFolderChild child : children) {
        if (child instanceof ISearchMark)
          searchmarks.add((ISearchMark) child);
        else if (child instanceof IFolder)
          fillSearchMarks(searchmarks, Collections.singleton((IFolder) child));
      }
    }
  }

  /**
   * @param newsbins the collection of newsbins to fill from the given folders.
   * @param folders the folders to extract all newsbins from.
   */
  public static void fillNewsBins(Collection<INewsBin> newsbins, Collection<IFolder> folders) {
    for (IFolder folder : folders) {
      List<IFolderChild> children = folder.getChildren();
      for (IFolderChild child : children) {
        if (child instanceof INewsBin)
          newsbins.add((INewsBin) child);
        else if (child instanceof IFolder)
          fillNewsBins(newsbins, Collections.singleton((IFolder) child));
      }
    }
  }

  /**
   * @param foldersList the collection of folders to fill from the given
   * folders.
   * @param folders the folders to extract all newsbins from.
   */
  public static void fillFolders(Collection<IFolder> foldersList, Collection<IFolder> folders) {
    for (IFolder folder : folders) {
      List<IFolder> children = folder.getFolders();
      for (IFolder child : children) {
        foldersList.add(child);
        fillFolders(foldersList, Collections.singleton(child));
      }
    }
  }
}