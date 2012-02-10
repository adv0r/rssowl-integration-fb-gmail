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

package org.rssowl.ui.internal.editors.feed;

import static org.rssowl.ui.internal.ILinkHandler.HANDLER_PROTOCOL;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.ATTACHMENTS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.DELETE_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.LABELS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.NEWS_MENU_HANDLER_ID;
import static org.rssowl.ui.internal.editors.feed.NewsBrowserViewer.SHARE_NEWS_MENU_HANDLER_ID;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISource;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.SearchMarkReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.ExpandingReader;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.FolderNewsMark.FolderNewsMarkReference;
import org.rssowl.ui.internal.util.CBrowser;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author bpasero
 */
public class NewsBrowserLabelProvider extends LabelProvider {

  /* Dynamic HTML in Content */
  enum Dynamic {
    NEWS("newsitem"), TITLE("title"), TOGGLE_READ_LINK("toggleRead"), TOGGLE_READ_IMG("toggleReadImg"), HEADER("header"), FOOTER("footer"), TOGGLE_STICKY("toggleSticky"), LABELS("labels"), LABELS_SEPARATOR("labelsSeparator"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$

    private String fId;

    Dynamic(String id) {
      fId = id;
    }

    String getId(INews news) {
      return fId + news.getId();
    }
  }

  /* Date Formatter for News */
  private DateFormat fDateFormat = OwlUI.getLongDateFormat();

  /* Time Formatter for News */
  private DateFormat fTimeFormat = OwlUI.getShortTimeFormat();

  /* Potential Media Tags */
  private final Set<String> fMediaTags = new HashSet<String>(Arrays.asList(new String[] { "img", "applet", "embed", "area", "frame", "frameset", "iframe", "map", "object" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$

  private String fNewsFontFamily;
  private String fNormalFontCSS;
  private String fSmallFontCSS;
  private String fBiggerFontCSS;
  private String fBiggestFontCSS;
  private String fStickyBGColorCSS;
  private IPropertyChangeListener fPropertyChangeListener;
  private final boolean fIsIE;
  private final NewsBrowserViewer fViewer;
  private boolean fStripMediaFromNews;
  private boolean fForceShowFeedInformation;
  private boolean fManageLinks;
  private final Calendar fSharedCalendar = Calendar.getInstance();
  private final Map<String, String> fMapFeedLinkToName = new HashMap<String, String>();

  /**
   * Creates a new Browser LabelProvider for News
   *
   * @param browser
   */
  public NewsBrowserLabelProvider(CBrowser browser) {
    this(null, browser.isIE());
  }

  /**
   * Creates a new Browser LabelProvider for News
   *
   * @param viewer
   */
  public NewsBrowserLabelProvider(NewsBrowserViewer viewer) {
    this(viewer, viewer.getBrowser().isIE());
  }

  private NewsBrowserLabelProvider(NewsBrowserViewer viewer, boolean isIE) {
    fViewer = viewer;
    fIsIE = isIE;

    IPreferenceScope preferences = Owl.getPreferenceService().getGlobalScope();
    fManageLinks = preferences.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER) || preferences.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER);

    createFonts();
    createColors();
    registerListeners();
  }

  private void close(StringBuilder builder, String tag) {
    builder.append("</").append(tag).append(">\n"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /* Init the Theme Color (from UI Thread) */
  private void createColors() {
    RGB stickyRgb = OwlUI.getThemeRGB(OwlUI.STICKY_BG_COLOR_ID, new RGB(255, 255, 180));
    fStickyBGColorCSS = "background-color: rgb(" + stickyRgb.red + "," + stickyRgb.green + "," + stickyRgb.blue + ");"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  }

  /* Init the Theme Font (from UI Thread) */
  private void createFonts() {
    int fontHeight = 10;
    Font newsFont = OwlUI.getThemeFont(OwlUI.NEWS_TEXT_FONT_ID, SWT.NORMAL);
    FontData[] fontData = newsFont.getFontData();
    if (fontData.length > 0) {
      fNewsFontFamily = fontData[0].getName();
      fontHeight = fontData[0].getHeight();
    }

    int normal = fontHeight;
    int small = normal - 1;
    int bigger = normal + 1;
    int biggest = bigger + 6;

    String fontUnit = "pt"; //$NON-NLS-1$
    fNormalFontCSS = "font-size: " + normal + fontUnit + ";"; //$NON-NLS-1$ //$NON-NLS-2$
    fSmallFontCSS = "font-size: " + small + fontUnit + ";"; //$NON-NLS-1$ //$NON-NLS-2$
    fBiggerFontCSS = "font-size: " + bigger + fontUnit + ";"; //$NON-NLS-1$ //$NON-NLS-2$
    fBiggestFontCSS = "font-size: " + biggest + fontUnit + ";"; //$NON-NLS-1$ //$NON-NLS-2$
  }

  /*
   * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    unregisterListeners();
    fMapFeedLinkToName.clear();
  }

  private void div(StringBuilder builder, String cssClass) {
    builder.append("<div class=\"").append(cssClass).append("\">\n"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private void div(StringBuilder builder, String cssClass, String id) {
    builder.append("<div id=\"").append(id).append("\" class=\"").append(cssClass).append("\">\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
  }

  private void div(StringBuilder builder, String cssClass, String extraCSS, String id) {
    builder.append("<div id=\"").append(id).append("\" class=\"").append(cssClass).append("\" style=\"").append(extraCSS).append("\">\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  }

  private StringBuilder getBuilder(INews news, String description) {
    int capacity = 0;

    if (news.getTitle() != null)
      capacity += news.getTitle().length();

    if (description != null)
      capacity += description.length();

    return new StringBuilder(capacity);
  }

  private String getLabel(EntityGroup group) {
    StringBuilder builder = new StringBuilder();

    /* DIV: Group */
    div(builder, "group"); //$NON-NLS-1$

    if (group.getColorHint() != null && !group.getColorHint().equals(new RGB(255, 255, 255)))
      span(builder, StringUtils.htmlEscape(group.getName()), null, OwlUI.toString(group.getColorHint()));
    else
      builder.append(StringUtils.htmlEscape(group.getName()));

    /* Close: Group */
    close(builder, "div"); //$NON-NLS-1$

    return builder.toString();
  }

  private String getLabel(INews news, boolean withInternalLinks, boolean withManagedLinks, int index) {
    String description = news.getDescription();
    if (fStripMediaFromNews)
      description = StringUtils.filterTags(description, fMediaTags, false);
    StringBuilder builder = getBuilder(news, description);
    StringBuilder search = new StringBuilder();

    String newsTitle = CoreUtils.getHeadline(news, false);
    String newsLink = CoreUtils.getLink(news);
    boolean hasLink = newsLink != null;
    State state = news.getState();
    boolean isUnread = state == State.NEW || state == State.UPDATED || state == State.UNREAD;
    Set<ILabel> labels = CoreUtils.getSortedLabels(news);
    String color = !labels.isEmpty() ? labels.iterator().next().getColor() : null;
    if ("0,0,0".equals(color) || "255,255,255".equals(color)) //Don't let black or white override link color //$NON-NLS-1$ //$NON-NLS-2$
      color = null;

    boolean hasAttachments = false;
    List<IAttachment> attachments = news.getAttachments();
    for (IAttachment attachment : attachments) {
      if (attachment.getLink() != null) {
        hasAttachments = true;
        break;
      }
    }

    /* Offer Search to Find Related News from Title */
    String relatedSearchLink = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.TITLE_HANDLER_ID + "?" + URIUtils.urlEncode(newsTitle); //$NON-NLS-1$
    link(search, relatedSearchLink, Messages.NewsBrowserLabelProvider_SIMILAR, "searchrelated"); //$NON-NLS-1$
    search.append(", "); //$NON-NLS-1$

    /* Add Labels to Search */
    for (ILabel label : labels) {
      String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.LABEL_HANDLER_ID + "?" + URIUtils.urlEncode(label.getName()); //$NON-NLS-1$
      String labelColor = label.getColor();

      if (!"0,0,0".equals(labelColor) && !"255,255,255".equals(labelColor)) //$NON-NLS-1$ //$NON-NLS-2$
        link(search, link, StringUtils.htmlEscape(label.getName()), "searchrelated", labelColor); //$NON-NLS-1$
      else
        link(search, link, StringUtils.htmlEscape(label.getName()), "searchrelated"); //$NON-NLS-1$

      search.append(", "); //$NON-NLS-1$
    }

    /* DIV: NewsItem */
    if (index == 0)
      div(builder, isUnread ? "newsitemUnread" : "newsitemRead", "border-top: none;", Dynamic.NEWS.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    else
      div(builder, isUnread ? "newsitemUnread" : "newsitemRead", Dynamic.NEWS.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$

    /* DIV: NewsItem/Header */
    div(builder, news.isFlagged() ? "headerSticky" : "header", Dynamic.HEADER.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$

    /* News Title */
    {

      /* DIV: NewsItem/Header/Title */
      div(builder, "title"); //$NON-NLS-1$

      String cssClass = isUnread ? "unread" : "read"; //$NON-NLS-1$ //$NON-NLS-2$

      /* Link */
      if (hasLink)
        link(builder, fManageLinks && withManagedLinks ? URIUtils.toManaged(newsLink) : newsLink, newsTitle, cssClass, Dynamic.TITLE.getId(news), color);

      /* Normal */
      else
        span(builder, newsTitle, cssClass, Dynamic.TITLE.getId(news), color);

      /* Close: NewsItem/Header/Title */
      close(builder, "div"); //$NON-NLS-1$
    }

    /* Delete */
    if (withInternalLinks) {

      /* DIV: NewsItem/Header/Delete */
      div(builder, "delete"); //$NON-NLS-1$

      String link = HANDLER_PROTOCOL + DELETE_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_DELETE, Messages.NewsBrowserLabelProvider_DELETE, "/icons/elcl16/remove_light.gif", "remove_light.gif", null, null); //$NON-NLS-1$ //$NON-NLS-2$

      /* DIV: NewsItem/Header/Delete */
      close(builder, "div"); //$NON-NLS-1$
    }

    /* DIV: NewsItem/Header/Subline */
    div(builder, "subline"); //$NON-NLS-1$
    builder.append("<table class=\"subline\">"); //$NON-NLS-1$
    builder.append("<tr class=\"subline\">"); //$NON-NLS-1$

    /* Actions */
    if (withInternalLinks) {

      /* Toggle Read  SPM DELETION
      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      String link = HANDLER_PROTOCOL + TOGGLE_READ_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      String text = (news.getState() == INews.State.READ) ? Messages.NewsBrowserLabelProvider_MARK_UNREAD : Messages.NewsBrowserLabelProvider_MARK_READ;
      imageLink(builder, link, text, text, "/icons/elcl16/mark_read_light.gif", "mark_read_light.gif", Dynamic.TOGGLE_READ_LINK.getId(news), Dynamic.TOGGLE_READ_IMG.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$

      /* Toggle Sticky
      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      link = HANDLER_PROTOCOL + TOGGLE_STICKY_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_STICKY, Messages.NewsBrowserLabelProvider_STICKY, news.isFlagged() ? "/icons/obj16/news_pinned_light.gif" : "/icons/obj16/news_pin_light.gif", news.isFlagged() ? "news_pinned_light.gif" : "news_pin_light.gif", null, Dynamic.TOGGLE_STICKY.getId(news)); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$ //$NON-NLS-4$
      builder.append("</td>"); //$NON-NLS-1$
       */
      /* Assign Labels */
      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      String link = HANDLER_PROTOCOL + LABELS_MENU_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_ASSIGN_LABELS, Messages.NewsBrowserLabelProvider_LABEL, "/icons/elcl16/labels_light.gif", "labels_light.gif", null, null); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$

      /* Share News Context Menu */
      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      link = HANDLER_PROTOCOL + SHARE_NEWS_MENU_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_SHARE_NEWS, Messages.NewsBrowserLabelProvider_SHARE, "/icons/elcl16/share_light.gif", "share_light.gif", null, null); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$

      /* News Context Menu */
      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      link = HANDLER_PROTOCOL + NEWS_MENU_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_MENU, Messages.NewsBrowserLabelProvider_MENU, "/icons/obj16/menu_light.gif", "menu_light.gif", null, null); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$

      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      builder.append("|"); //$NON-NLS-1$
      builder.append("</td>"); //$NON-NLS-1$
    }

    /* Date */
    builder.append("<td class=\"subline\">"); //$NON-NLS-1$

    Date newsDate = DateUtils.getRecentDate(news);
    if (isToday(newsDate))
      builder.append(fTimeFormat.format(newsDate));
    else
      builder.append(fDateFormat.format(newsDate));

    builder.append("</td>"); //$NON-NLS-1$

    /* Author */
    IPerson author = news.getAuthor();
    if (author != null) {
      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      builder.append("|"); //$NON-NLS-1$
      builder.append("</td>"); //$NON-NLS-1$

      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      String name = author.getName();
      String email = author.getEmail() != null ? author.getEmail().toASCIIString() : null;
      if (email != null && !email.contains("mail:")) //$NON-NLS-1$
        email = "mailto:" + email; //$NON-NLS-1$

      /* Use name as email if valid */
      if (email == null && name.contains("@") && !name.contains(" ")) //$NON-NLS-1$ //$NON-NLS-2$
        email = name;

      if (StringUtils.isSet(name) && email != null)
        link(builder, email, NLS.bind(Messages.NewsBrowserLabelProvider_BY_AUTHOR, StringUtils.htmlEscape(name)), "author"); //$NON-NLS-1$
      else if (StringUtils.isSet(name))
        builder.append(NLS.bind(Messages.NewsBrowserLabelProvider_BY_AUTHOR, StringUtils.htmlEscape(name)));
      else if (email != null)
        link(builder, email, NLS.bind(Messages.NewsBrowserLabelProvider_BY_AUTHOR, StringUtils.htmlEscape(email)), "author"); //$NON-NLS-1$
      else
        builder.append(Messages.NewsBrowserLabelProvider_UNKNOWN);

      /* Add to Search */
      String value = StringUtils.isSet(name) ? name : email;
      if (StringUtils.isSet(value)) {
        String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.AUTHOR_HANDLER_ID + "?" + URIUtils.urlEncode(value); //$NON-NLS-1$
        link(search, link, NLS.bind(Messages.NewsBrowserLabelProvider_BY_AUTHOR, StringUtils.htmlEscape(value)), "searchrelated"); //$NON-NLS-1$
        search.append(", "); //$NON-NLS-1$
      }
      builder.append("</td>"); //$NON-NLS-1$
    }

    /* Feed Information */
    if (showFeedInformation()) {

      /* Retrieve Name */
      String feedLinkAsText = news.getFeedLinkAsText();
      String feedName = fMapFeedLinkToName.get(feedLinkAsText);
      if (feedName == null) {
        IBookMark bm = CoreUtils.getBookMark(news.getFeedReference());
        if (bm != null) {
          feedName = StringUtils.htmlEscape(bm.getName());
          fMapFeedLinkToName.put(feedLinkAsText, feedName);
        }
      }

      /* Show Name if Provided */
      if (StringUtils.isSet(feedName)) {
        builder.append("<td class=\"subline\">"); //$NON-NLS-1$
        builder.append("|"); //$NON-NLS-1$
        builder.append("</td>"); //$NON-NLS-1$

        builder.append("<td class=\"subline\">"); //$NON-NLS-1$
        builder.append(feedName);
        builder.append("</td>"); //$NON-NLS-1$
      }
    }

    /* Comments */
    if (StringUtils.isSet(news.getComments()) && news.getComments().trim().length() > 0 && URIUtils.looksLikeLink(news.getComments())) {
      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      builder.append("|"); //$NON-NLS-1$
      builder.append("</td>"); //$NON-NLS-1$

      builder.append("<td class=\"subline\">"); //$NON-NLS-1$

      String comments = news.getComments();
      imageLink(builder, comments, Messages.NewsBrowserLabelProvider_READ_COMMENTS, Messages.NewsBrowserLabelProvider_COMMENTS, "/icons/obj16/comments_light.gif", "comments_light.gif", null, null); //$NON-NLS-1$ //$NON-NLS-2$

      builder.append("</td>"); //$NON-NLS-1$
    }

    /* Go to Attachments */
    if (hasAttachments) {
      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      builder.append("|"); //$NON-NLS-1$
      builder.append("</td>"); //$NON-NLS-1$

      builder.append("<td class=\"subline\">"); //$NON-NLS-1$
      String link = HANDLER_PROTOCOL + ATTACHMENTS_MENU_HANDLER_ID + "?" + news.getId(); //$NON-NLS-1$
      imageLink(builder, link, Messages.NewsBrowserLabelProvider_ATTACHMENTS, Messages.NewsBrowserLabelProvider_ATTACHMENTS, "/icons/obj16/attachment_light.gif", "attachment_light.gif", null, null); //$NON-NLS-1$ //$NON-NLS-2$
      builder.append("</td>"); //$NON-NLS-1$
    }

    /* Labels Separator  */
    if (labels.isEmpty())
      builder.append("<td id=\"").append(Dynamic.LABELS_SEPARATOR.getId(news)).append("\" class=\"subline\" style=\"display: none;\">"); //$NON-NLS-1$ //$NON-NLS-2$
    else
      builder.append("<td id=\"").append(Dynamic.LABELS_SEPARATOR.getId(news)).append("\" class=\"subline\">"); //$NON-NLS-1$ //$NON-NLS-2$
    builder.append("|"); //$NON-NLS-1$
    builder.append("</td>"); //$NON-NLS-1$

    /* Labels */
    builder.append("<td id=\"").append(Dynamic.LABELS.getId(news)).append("\" class=\"subline\">"); //$NON-NLS-1$ //$NON-NLS-2$

    if (!labels.isEmpty())
      builder.append(Messages.NewsBrowserLabelProvider_LABELS).append(" "); //$NON-NLS-1$

    /* Append Labels to Footer */
    int c = 0;
    for (ILabel label : labels) {
      c++;
      if (c < labels.size())
        span(builder, StringUtils.htmlEscape(label.getName()) + ", ", null, label.getColor()); //$NON-NLS-1$
      else
        span(builder, StringUtils.htmlEscape(label.getName()), null, label.getColor());
    }

    builder.append("</td>"); //$NON-NLS-1$

    /* Close: NewsItem/Header/Actions */
    builder.append("</tr>"); //$NON-NLS-1$
    builder.append("</table>"); //$NON-NLS-1$
    close(builder, "div"); //$NON-NLS-1$

    /* Close: NewsItem/Header */
    close(builder, "div"); //$NON-NLS-1$

    /* News Content */
    {

      /* DIV: NewsItem/Content */
      div(builder, "content"); //$NON-NLS-1$

      if (StringUtils.isSet(description) && !description.equals(news.getTitle()))
        builder.append(description);
      else {
        builder.append(Messages.NewsBrowserLabelProvider_NO_CONTENT);

        if (hasLink) {
          builder.append(" "); //$NON-NLS-1$
          link(builder, fManageLinks && withManagedLinks ? URIUtils.toManaged(newsLink) : newsLink, Messages.NewsBrowserLabelProvider_OPEN_IN_BROWSER, null);
        }
      }

      /* Close: NewsItem/Content */
      close(builder, "div"); //$NON-NLS-1$
    }

    /* News Footer */
    {
      StringBuilder footer = new StringBuilder();

      /* DIV: NewsItem/Footer */
      div(footer, news.isFlagged() ? "footerSticky" : "footer", Dynamic.FOOTER.getId(news)); //$NON-NLS-1$ //$NON-NLS-2$

      /* Attachments */
      if (attachments.size() != 0) {

        /* DIV: NewsItem/Footer/Attachments */
        div(footer, "attachments"); //$NON-NLS-1$

        /* Label */
        span(footer, attachments.size() == 1 ? Messages.NewsBrowserLabelProvider_ATTACHMENT : Messages.NewsBrowserLabelProvider_ATTACHMENTSS, "label"); //$NON-NLS-1$

        /* For each Attachment */
        boolean strip = false;
        for (IAttachment attachment : attachments) {
          if (attachment.getLink() != null) {
            strip = true;
            URI link = attachment.getLink();
            String name = URIUtils.getFile(link, OwlUI.getExtensionForMime(attachment.getType()));
            if (!StringUtils.isSet(name))
              name = link.toASCIIString();

            String size = OwlUI.getSize(attachment.getLength());
            if (size != null)
              link(footer, link.toASCIIString(), NLS.bind(Messages.NewsBrowserLabelProvider_NAME_SIZE, StringUtils.htmlEscape(name), size), "attachment"); //$NON-NLS-1$
            else
              link(footer, link.toASCIIString(), StringUtils.htmlEscape(name), "attachment"); //$NON-NLS-1$

            footer.append(", "); //$NON-NLS-1$
          }
        }

        if (strip && footer.length() > 0)
          footer.delete(footer.length() - 2, footer.length());

        /* Close: NewsItem/Footer/Attachments */
        close(footer, "div"); //$NON-NLS-1$
      }

      /* Source */
      ISource source = news.getSource();
      if (source != null) {
        String link = source.getLink() != null ? source.getLink().toASCIIString() : null;
        String name = source.getName();
        if (StringUtils.isSet(link) || StringUtils.isSet(name)) {

          /* DIV: NewsItem/Footer/Source */
          div(footer, "source"); //$NON-NLS-1$

          /* Label */
          span(footer, Messages.NewsBrowserLabelProvider_SOURCE, "label"); //$NON-NLS-1$

          if (StringUtils.isSet(name) && link != null)
            link(footer, link, StringUtils.htmlEscape(name), "source"); //$NON-NLS-1$
          else if (link != null)
            link(footer, link, StringUtils.htmlEscape(link), "source"); //$NON-NLS-1$
          else if (StringUtils.isSet(name))
            footer.append(StringUtils.htmlEscape(name));

          /* Close: NewsItem/Footer/Source */
          close(footer, "div"); //$NON-NLS-1$
        }
      }

      /* Add Categories to Search */
      List<ICategory> categories = news.getCategories();
      if (categories.size() > 0) {

        /* For each Category */
        for (ICategory category : categories) {
          String name = category.getName();

          /* Add to Search */
          if (StringUtils.isSet(name)) {
            String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.CATEGORY_HANDLER_ID + "?" + URIUtils.urlEncode(name); //$NON-NLS-1$
            link(search, link, StringUtils.htmlEscape(name), "searchrelated"); //$NON-NLS-1$
            search.append(", "); //$NON-NLS-1$
          }
        }
      }

      /* Find related News */
      if (search.length() > 0) {
        search.delete(search.length() - 2, search.length());

        /* DIV: NewsItem/Footer/SearchRelated */
        div(footer, "searchrelated"); //$NON-NLS-1$

        /* Label */
        if (withInternalLinks)
          span(footer, Messages.NewsBrowserLabelProvider_FIND_RELATED, "label"); //$NON-NLS-1$

        /* Append to Footer */
        if (withInternalLinks)
          footer.append(search);

        /* Close: NewsItem/Footer/SearchRelated */
        close(footer, "div"); //$NON-NLS-1$
      }

      /* Close: NewsItem/Footer */
      close(footer, "div"); //$NON-NLS-1$

      /* Append */
      builder.append(footer);
    }

    /* Close: NewsItem */
    close(builder, "div"); //$NON-NLS-1$

    String result = builder.toString();

    /* Highlight Support */
    if (fViewer != null) {
      Collection<String> wordsToHighlight = fViewer.getHighlightedWords();
      if (!wordsToHighlight.isEmpty()) {
        StringBuilder highlightedResult = new StringBuilder(result.length());

        RGB searchRGB = OwlUI.getThemeRGB(OwlUI.SEARCH_HIGHLIGHT_BG_COLOR_ID, new RGB(255, 255, 0));
        String preHighlight = "<span style=\"background-color:rgb(" + OwlUI.toString(searchRGB) + ");\">"; //$NON-NLS-1$ //$NON-NLS-2$
        String postHighlight = "</span>"; //$NON-NLS-1$

        ExpandingReader resultHighlightReader = new ExpandingReader(new StringReader(result), wordsToHighlight, preHighlight, postHighlight, true);

        int len = 0;
        char[] buf = new char[1000];
        try {
          while ((len = resultHighlightReader.read(buf)) != -1)
            highlightedResult.append(buf, 0, len);

          return highlightedResult.toString();
        } catch (IOException e) {
          Activator.getDefault().logError(e.getMessage(), e);
        }
      }
    }

    return result;
  }

  /*
   * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
   */
  @Override
  public String getText(Object element) {
    return getText(element, true, -1);
  }

  /**
   * @param element the element to get a HTML representation from.
   * @param withInternalLinks <code>true</code> to include links of the internal
   * protocol rssowl:// and <code>false</code> otherwise.
   * @return the HTML representation for the given element.
   */
  public String getText(Object element, boolean withInternalLinks) {
    return getText(element, withInternalLinks, -1);
  }

  /**
   * @param element the element to get a HTML representation from.
   * @param withInternalLinks <code>true</code> to include links of the internal
   * protocol rssowl:// and <code>false</code> otherwise.
   * @param withManagedLinks if set to <code>false</code>, the output will not
   * contain any managed links.
   * @param index the zero-based index of the element from top.
   * @return the HTML representation for the given element.
   */
  public String getText(Object element, boolean withInternalLinks, boolean withManagedLinks, int index) {

    /* Return HTML for a Group */
    if (element instanceof EntityGroup)
      return getLabel((EntityGroup) element);

    /* Return HTML for a News */
    else if (element instanceof INews)
      return getLabel((INews) element, withInternalLinks, withManagedLinks, index);

    return ""; //$NON-NLS-1$
  }

  /**
   * @param element the element to get a HTML representation from.
   * @param withInternalLinks <code>true</code> to include links of the internal
   * protocol rssowl:// and <code>false</code> otherwise.
   * @param index the zero-based index of the element from top.
   * @return the HTML representation for the given element.
   */
  public String getText(Object element, boolean withInternalLinks, int index) {
    return getText(element, withInternalLinks, true, index);
  }

  /**
   * @param element the element to get a HTML representation from.
   * @param index the zero-based index of the element from top.
   * @return the HTML representation for the given element.
   */
  public String getText(Object element, int index) {
    return getText(element, true, index);
  }

  private void imageLink(StringBuilder builder, String link, String tooltip, String alt, String imgPath, String imgName, String linkId, String imageId) {
    builder.append("<a"); //$NON-NLS-1$

    if (linkId != null)
      builder.append(" id=\"").append(linkId).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    builder.append(" title=\"").append(tooltip).append("\" href=\"").append(link).append("\">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    builder.append("<img"); //$NON-NLS-1$

    if (imageId != null)
      builder.append(" id=\"").append(imageId).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    String imageUri;
    if (fIsIE)
      imageUri = OwlUI.getImageUri(imgPath, imgName);
    else
      imageUri = ApplicationServer.getDefault().toResourceUrl(imgPath);

    builder.append(" alt=\"").append(alt).append("\" border=\"0\" src=\"").append(imageUri).append("\" />"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    builder.append("</a>"); //$NON-NLS-1$
  }

  private boolean isSingleNewsDisplayed() {
    Object input = fViewer != null ? fViewer.getInput() : null;
    return input instanceof INews;
  }

  private boolean isToday(Date date) {
    fSharedCalendar.set(Calendar.HOUR_OF_DAY, 0);
    fSharedCalendar.set(Calendar.MINUTE, 0);
    fSharedCalendar.set(Calendar.SECOND, 0);
    fSharedCalendar.set(Calendar.MILLISECOND, 0);

    return date.compareTo(fSharedCalendar.getTime()) >= 0;
  }

  private void link(StringBuilder builder, String link, String content, String cssClass) {
    link(builder, link, content, cssClass, null);
  }

  private void link(StringBuilder builder, String link, String content, String cssClass, String color) {
    link(builder, link, content, cssClass, null, color);
  }

  private void link(StringBuilder builder, String link, String content, String cssClass, String id, String color) {
    builder.append("<a href=\"").append(link).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    if (cssClass != null)
      builder.append(" class=\"").append(cssClass).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    if (color != null)
      builder.append(" style=\"color: rgb(").append(color).append(");\""); //$NON-NLS-1$ //$NON-NLS-2$

    if (id != null)
      builder.append(" id=\"").append(id).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    builder.append(">").append(content).append("</a>"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private void registerListeners() {

    /* Create Property Listener */
    fPropertyChangeListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (OwlUI.NEWS_TEXT_FONT_ID.equals(property))
          createFonts();
        else if (OwlUI.STICKY_BG_COLOR_ID.equals(property))
          createColors();
      }
    };

    /* Add it to listen to Theme Events */
    PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fPropertyChangeListener);
  }

  /**
   * @param forceShowFeedInformation if <code>true</code> will show the name of
   * a feed of a news when shown, <code>false</code> otherwise.
   */
  public void setForceShowFeedInformation(boolean forceShowFeedInformation) {
    fForceShowFeedInformation = forceShowFeedInformation;
  }

  /**
   * @param stripMediaFromNews <code>true</code> to strip images and other media
   * from the news and <code>false</code> otherwise.
   */
  void setStripMediaFromNews(boolean stripMediaFromNews) {
    fStripMediaFromNews = stripMediaFromNews;
  }

  private boolean showFeedInformation() {
    if (fForceShowFeedInformation)
      return true;

    Object input = fViewer != null ? fViewer.getInput() : null;
    return input instanceof FolderNewsMarkReference || input instanceof SearchMarkReference || input instanceof NewsBinReference;
  }

  private void span(StringBuilder builder, String content, String cssClass) {
    span(builder, content, cssClass, null);
  }

  private void span(StringBuilder builder, String content, String cssClass, String color) {
    span(builder, content, cssClass, null, color);
  }

  private void span(StringBuilder builder, String content, String cssClass, String id, String color) {
    if (cssClass != null)
      builder.append("<span class=\"").append(cssClass).append("\""); //$NON-NLS-1$ //$NON-NLS-2$
    else
      builder.append("<span"); //$NON-NLS-1$

    if (color != null)
      builder.append(" style=\"color: rgb(").append(color).append(");\""); //$NON-NLS-1$ //$NON-NLS-2$

    if (id != null)
      builder.append(" id=\"").append(id).append("\""); //$NON-NLS-1$ //$NON-NLS-2$

    builder.append(">").append(content).append("</span>\n"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private void unregisterListeners() {
    PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fPropertyChangeListener);
  }

  /**
   * Writes the CSS information to the given Writer.
   *
   * @param writer the writer to add the CSS information to.
   * @throws IOException In case of an error while writing.
   */
  public void writeCSS(Writer writer) throws IOException {
    writeCSS(writer, isSingleNewsDisplayed(), true);
  }

  /**
   * Writes the CSS information to the given Writer.
   *
   * @param writer the writer to add the CSS information to.
   * @param withInternalLinks <code>true</code> to include links of the internal
   * protocol rssowl:// and <code>false</code> otherwise.
   * @throws IOException In case of an error while writing.
   */
  public void writeCSS(Writer writer, boolean withInternalLinks) throws IOException {
    writeCSS(writer, isSingleNewsDisplayed(), withInternalLinks);
  }

  /**
   * Writes the CSS information to the given Writer.
   *
   * @param writer the writer to add the CSS information to.
   * @param forSingleNews if <code>true</code>, the site contains a single news,
   * or <code>false</code> if it contains a collection of news.
   * @param withInternalLinks <code>true</code> to include links of the internal
   * protocol rssowl:// and <code>false</code> otherwise.
   * @throws IOException In case of an error while writing.
   */
  public void writeCSS(Writer writer, boolean forSingleNews, boolean withInternalLinks) throws IOException {

    /* Open CSS */
    writer.write("<style type=\"text/css\">\n"); //$NON-NLS-1$

    /* General */
    writer.append("body { overflow: auto; margin: 0; font-family: ").append(fNewsFontFamily).append(",Verdanna,sans-serif; }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("a { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a:hover { color: #009; text-decoration: underline; }\n"); //$NON-NLS-1$
    writer.write("a:visited { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("img { border: none; }\n"); //$NON-NLS-1$
    writer.write("div.hidden { display: none; }\n"); //$NON-NLS-1$

    /* Group */
    writer.append("div.group { color: #678; ").append(fBiggestFontCSS).append(" font-weight: bold; padding: 10px 0px 10px 5px; }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Main DIV per Item */
    if (forSingleNews) {
      writer.write("div.newsitemUnread { margin: 0; border-bottom: dotted 1px silver; }\n"); //$NON-NLS-1$
      writer.write("div.newsitemRead { margin: 0; border-bottom: dotted 1px silver; }\n"); //$NON-NLS-1$
    } else {
      writer.write("div.newsitemUnread { margin: 0px 0px 25px 0px; border-top: dotted 1px silver; border-bottom: dotted 1px silver; }\n"); //$NON-NLS-1$
      writer.write("div.newsitemRead { margin: 0px 0px 25px 0px; border-top: dotted 1px silver; border-bottom: dotted 1px silver; }\n"); //$NON-NLS-1$
    }

    /* Main DIV Item Areas */
    writer.write("div.header { padding: 10px 10px 5px 10px; background-color: rgb(242,242,242); }\n"); //$NON-NLS-1$
    writer.append("div.headerSticky { padding: 10px 10px 5px 10px; ").append(fStickyBGColorCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("div.content { \n"); //$NON-NLS-1$
    writer.write("   padding: 15px 10px 15px 10px; border-top: dotted 1px silver; \n"); //$NON-NLS-1$
    writer.append("  background-color: #fff; clear: both; ").append(fNormalFontCSS).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("}\n"); //$NON-NLS-1$
    writer.write("div.footer { background-color: rgb(248,248,248); padding: 5px 10px 5px 10px; line-height: 20px; border-top: dotted 1px silver; clear: both; }\n"); //$NON-NLS-1$
    writer.append("div.footerSticky { ").append(fStickyBGColorCSS).append(" padding: 5px 10px 5px 10px; line-height: 20px; border-top: dotted 1px silver; clear: both; }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Restrict the style of embedded Paragraphs */
    writer.write("div.content p { margin-top: 0; padding-top: 0; margin-left: 0; padding-left: 0; }\n"); //$NON-NLS-1$

    /* Title */
    if (withInternalLinks) //Need to set width to avoid float drop bug of delete button on all OS (see Bug 1393)
      writer.append("div.title { width: 90%; float: left; padding-bottom: 6px; ").append(fBiggerFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    else
      writer.append("div.title { float: left; padding-bottom: 6px; ").append(fBiggerFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    writer.write("div.title a { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.title a.unread { font-weight: bold; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.title a:hover { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.title a:visited { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$

    /* Author */
    writer.write("a.author { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.author:hover { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.author:active { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.author:visited { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$

    /* Comments */
    writer.write("a.comments { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.comments:hover { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.comments:active { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("a.comments:visited { color: rgb(80,80,80); text-decoration: none; }\n"); //$NON-NLS-1$

    writer.write("div.title span.unread { font-weight: bold; }\n"); //$NON-NLS-1$

    /* Delete */
    writer.append("div.delete { text-align: right; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Subline */
    writer.append("div.subline { margin: 0; padding: 0; clear: left; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.append("table.subline { margin: 0; padding: 0; }\n"); //$NON-NLS-1$
    writer.append("tr.subline { margin: 0; padding: 0; }\n"); //$NON-NLS-1$
    writer.append("td.subline { margin: 0; padding: 0; color: rgb(80, 80, 80); padding-right: 8px; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Date */
    writer.append("div.date { float: left; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Author */
    writer.append("div.author { text-align: right; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Attachments */
    writer.append("div.attachments { clear: both; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("div.attachments span.label { float: left; padding-right: 5px; }\n"); //$NON-NLS-1$
    writer.write("div.attachments a { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.attachments a:visited { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.attachments a:hover { text-decoration: underline; }\n"); //$NON-NLS-1$

    /* Categories */
    writer.append("div.categories { clear: both; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("div.categories span.label { float: left; padding-right: 5px; }\n"); //$NON-NLS-1$
    writer.write("div.categories a { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.categories a:visited { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.categories a:hover { text-decoration: underline; }\n"); //$NON-NLS-1$

    /* Source */
    writer.append("div.source { clear: both; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("div.source span.label {float: left; padding-right: 5px; }\n"); //$NON-NLS-1$
    writer.write("div.source a { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.source a:visited { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.source a:hover { text-decoration: underline; }\n"); //$NON-NLS-1$

    /* Comments */
    writer.append("div.comments { clear: both; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("div.comments span.label {float: left; padding-right: 5px; }\n"); //$NON-NLS-1$
    writer.write("div.comments a { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.comments a:visited { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.comments a:hover { text-decoration: underline; }\n"); //$NON-NLS-1$

    /* Search Related */
    writer.append("div.searchrelated { clear: both; ").append(fSmallFontCSS).append(" }\n"); //$NON-NLS-1$ //$NON-NLS-2$
    writer.write("div.searchrelated span.label {float: left; padding-right: 5px; }\n"); //$NON-NLS-1$
    writer.write("div.searchrelated a { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.searchrelated a:visited { color: #009; text-decoration: none; }\n"); //$NON-NLS-1$
    writer.write("div.searchrelated a:hover { text-decoration: underline; }\n"); //$NON-NLS-1$

    /* Quotes */
    writer.write("span.quote_lvl1 { color: #660066; }\n"); //$NON-NLS-1$
    writer.write("span.quote_lvl2 { color: #007777; }\n"); //$NON-NLS-1$
    writer.write("span.quote_lvl3 { color: #3377ff; }\n"); //$NON-NLS-1$
    writer.write("span.quote_lvl4 { color: #669966; }\n"); //$NON-NLS-1$

    writer.write("</style>\n"); //$NON-NLS-1$
  }
}