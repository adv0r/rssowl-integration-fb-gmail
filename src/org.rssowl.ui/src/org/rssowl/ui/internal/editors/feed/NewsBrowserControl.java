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

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.CBrowser;

/**
 * Part of the FeedView to display News in a BrowserViewer.
 *
 * @author bpasero
 */
public class NewsBrowserControl implements IFeedViewPart {
  private static final String LOCALHOST = "127.0.0.1"; //$NON-NLS-1$
  private IEditorSite fEditorSite;
  private NewsBrowserViewer fViewer;
  private ISelectionListener fSelectionListener;
  private Object fInitialInput;
  private boolean fInputSet;
  private IPreferenceScope fInputPreferences;
  private IPropertyChangeListener fPropertyChangeListener;
  private boolean fStripMediaFromNews;
  private NewsComparator fNewsSorter;
  private FeedViewInput fEditorInput;

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#init(org.eclipse.ui.IEditorSite)
   */
  public void init(IEditorSite editorSite) {
    fEditorSite = editorSite;
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#onInputChanged(org.rssowl.ui.internal.editors.feed.FeedViewInput)
   */
  public void onInputChanged(FeedViewInput input) {
    fEditorInput = input;
    fInputPreferences = Owl.getPreferenceService().getEntityScope(input.getMark());
    fStripMediaFromNews = !fInputPreferences.getBoolean(DefaultPreferences.BM_LOAD_IMAGES);
    if (fViewer != null && fViewer.getLabelProvider() != null)
      ((NewsBrowserLabelProvider) fViewer.getLabelProvider()).setStripMediaFromNews(fStripMediaFromNews);
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#createViewer(org.eclipse.swt.widgets.Composite)
   */
  public NewsBrowserViewer createViewer(Composite parent) {
    fViewer = new NewsBrowserViewer(parent, SWT.NONE, fEditorSite);
    fViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    return fViewer;
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#getViewer()
   */
  public NewsBrowserViewer getViewer() {
    return fViewer;
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#initViewer(org.eclipse.jface.viewers.IStructuredContentProvider,
   * org.eclipse.jface.viewers.ViewerFilter)
   */
  public void initViewer(IStructuredContentProvider contentProvider, ViewerFilter filter) {

    /* Apply ContentProvider */
    fViewer.setContentProvider(contentProvider);

    /* Create LabelProvider */
    NewsBrowserLabelProvider labelProvider = new NewsBrowserLabelProvider(fViewer);
    labelProvider.setStripMediaFromNews(fStripMediaFromNews);
    fViewer.setLabelProvider(labelProvider);

    /* Create Sorter */
    fNewsSorter = new NewsComparator();
    fViewer.setComparator(fNewsSorter);
    updateSorting(fEditorInput.getMark(), false);

    /* Add ViewerFilter */
    fViewer.addFilter(filter);

    /* Register Listeners */
    registerListener();
  }

  void updateSorting(Object input, boolean refreshIfChanged) {
    if (fViewer.getControl().isDisposed())
      return;

    IPreferenceScope preferences;
    if (input instanceof IEntity)
      preferences = Owl.getPreferenceService().getEntityScope((IEntity) input);
    else
      preferences = Owl.getPreferenceService().getGlobalScope();

    NewsColumn sortColumn = NewsColumn.values()[preferences.getInteger(DefaultPreferences.BM_NEWS_SORT_COLUMN)];
    boolean ascending = preferences.getBoolean(DefaultPreferences.BM_NEWS_SORT_ASCENDING);

    NewsColumn oldSortColumn = fNewsSorter.getSortBy();
    boolean oldAscending = fNewsSorter.isAscending();

    fNewsSorter.setSortBy(sortColumn);
    fNewsSorter.setAscending(ascending);

    if (refreshIfChanged && ((oldSortColumn != sortColumn) || (oldAscending != ascending)))
      fViewer.refresh();
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#setInput(java.lang.Object)
   */
  public void setPartInput(Object input) {

    /* Update Columns for Input */
    if (input instanceof EntityGroup || input instanceof INewsMark)
      updateSorting(input, false);

    /* Set input to Viewer */
    fViewer.setInput(getInput(input));

    /* Remember as initial Input */
    fInitialInput = fViewer.getInput();
    fInputSet = true;
  }

  private Object getInput(Object obj) {

    /* Return Reference */
    if (obj instanceof INewsMark)
      return ((INewsMark) obj).toReference();

    /* News: Handle special dependant on settings */
    else if (obj instanceof INews)
      return getInput((INews) obj);

    /* NewsReference: Resolve and special handle */
    else if (obj instanceof NewsReference)
      return getInput(((NewsReference) obj).resolve());

    return obj;
  }

  private Object getInput(INews news) {
    if (fInputPreferences.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS))
      return CoreUtils.getLink(news);

    boolean openEmptyNews = Owl.getPreferenceService().getGlobalScope().getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_EMPTY_NEWS);
    if (openEmptyNews && CoreUtils.isEmpty(news))
      return CoreUtils.getLink(news);

    return news;
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#dispose()
   */
  public void dispose() {
    unregisterListeners();
    fEditorInput = null;
  }

  private void registerListener() {

    /* Listen on selection-changes */
    fSelectionListener = new ISelectionListener() {
      public void selectionChanged(IWorkbenchPart part, ISelection sel) {

        /* Only Track selections from the HeadlineControl */
        if (!part.equals(fEditorSite.getPart()))
          return;

        IStructuredSelection selection = (IStructuredSelection) sel;

        /* Restore Initial Input (if set) if selection is empty */
        if (selection.isEmpty() && fInputSet) {
          fViewer.setInput(fInitialInput);
        }

        /* Set Elements as Input if 1 Item is selected */
        else if (selection.size() == 1)
          setPartInput(selection.getFirstElement());
      }
    };
    fEditorSite.getPage().addSelectionListener(fSelectionListener);

    /* Send Browser-Status to Workbench-Status */
    ((Browser) fViewer.getControl()).addStatusTextListener(new StatusTextListener() {
      public void changed(StatusTextEvent event) {

        /* Don't show Status for the Handler Protocol */
        if (event.text != null && !event.text.contains(ILinkHandler.HANDLER_PROTOCOL) && !event.text.contains(LOCALHOST)) {

          /* Do not post to status line if browser is hidden (e.g. hidden tab) */
          if (!fViewer.getControl().isDisposed() && fViewer.getControl().isVisible()) {
            String statusText = event.text;
            statusText = URIUtils.fastDecode(statusText);
            statusText = statusText.replaceAll("&", "&&"); //$NON-NLS-1$//$NON-NLS-2$
            if (URIUtils.isManaged(statusText))
              statusText = URIUtils.toUnManaged(statusText);

            fEditorSite.getActionBars().getStatusLineManager().setMessage(statusText);
          }
        }
      }
    });

    /* Control Browser's visibility based on the location */
    ((Browser) fViewer.getControl()).addLocationListener(new LocationAdapter() {
      @Override
      public void changing(LocationEvent event) {
        if (event.doit) {
          String loc = event.location;
          boolean visible = fViewer.getControl().getVisible();

          /* Make Browser visible now */
          if (!visible && StringUtils.isSet(loc) && !URIUtils.ABOUT_BLANK.equals(loc))
            fViewer.getControl().setVisible(true);
        }
      }
    });

    /* Refresh Browser when Font Changes */
    fPropertyChangeListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        if (fViewer.getControl().isDisposed())
          return;

        if (OwlUI.NEWS_TEXT_FONT_ID.equals(event.getProperty()) || OwlUI.STICKY_BG_COLOR_ID.equals(event.getProperty()))
          fViewer.getBrowser().refresh();
      }
    };
    PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fPropertyChangeListener);
  }

  private void unregisterListeners() {
    fEditorSite.getPage().removeSelectionListener(fSelectionListener);
    PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fPropertyChangeListener);
  }

  /*
   * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#setFocus()
   */
  public void setFocus() {
    fViewer.getControl().setFocus();
  }

  /*
   * Executes JavaScript in the Browser to navigate between News.
   */
  void navigate(boolean next, boolean unread) {
    CBrowser browser = fViewer.getBrowser();
    if (browser.shouldDisableScript())
      browser.setScriptDisabled(false);

    /* Create JavaScript to Execute */
    StringBuffer js = new StringBuffer();
    if (browser.isIE())
      js.append("var scrollPosY = document.body.scrollTop; "); //$NON-NLS-1$
    else
      js.append("var scrollPosY = window.pageYOffset; "); //$NON-NLS-1$
    js.append("var body = document.getElementById(\"owlbody\"); "); //$NON-NLS-1$
    js.append("var divs = body.childNodes; "); //$NON-NLS-1$

    /* Next News */
    if (next) {
      js.append("  for (var i = 1; i < divs.length; i++) { "); //$NON-NLS-1$
      js.append("    if (divs[i].nodeType != 1) { "); //$NON-NLS-1$
      js.append("      continue; "); //$NON-NLS-1$
      js.append("    } "); //$NON-NLS-1$
      js.append("    var divPosY = divs[i].offsetTop; "); //$NON-NLS-1$
      if (unread) {
        js.append("  if (divPosY > scrollPosY && divs[i].className == \"newsitemUnread\") { "); //$NON-NLS-1$
      } else
        js.append("  if (divPosY > scrollPosY) { "); //$NON-NLS-1$
      js.append("      divs[i].scrollIntoView(); "); //$NON-NLS-1$
      js.append("      break; "); //$NON-NLS-1$
      js.append("    } "); //$NON-NLS-1$
      js.append("  } "); //$NON-NLS-1$
    }

    /* Previous News */
    else {
      js.append("  for (var i = divs.length - 1; i >= 0; i--) { "); //$NON-NLS-1$
      js.append("    if (divs[i].nodeType != 1) { "); //$NON-NLS-1$
      js.append("      continue; "); //$NON-NLS-1$
      js.append("    } "); //$NON-NLS-1$
      js.append("    var divPosY = divs[i].offsetTop; "); //$NON-NLS-1$
      if (unread) {
        js.append("  if (divPosY < scrollPosY - 10 && divs[i].className == \"newsitemUnread\") { "); //$NON-NLS-1$
      } else
        js.append("  if (divPosY < scrollPosY - 10) { "); //$NON-NLS-1$
      js.append("      divs[i].scrollIntoView(); "); //$NON-NLS-1$
      js.append("      break; "); //$NON-NLS-1$
      js.append("    } "); //$NON-NLS-1$
      js.append("  } "); //$NON-NLS-1$
    }

    /* See if the Scroll Position Changed at all and handle */
    String actionId;
    if (next) {
      if (unread)
        actionId = NewsBrowserViewer.NEXT_UNREAD_NEWS_HANDLER_ID;
      else
        actionId = NewsBrowserViewer.NEXT_NEWS_HANDLER_ID;
    } else {
      if (unread)
        actionId = NewsBrowserViewer.PREVIOUS_UNREAD_NEWS_HANDLER_ID;
      else
        actionId = NewsBrowserViewer.PREVIOUS_NEWS_HANDLER_ID;
    }

    if (browser.isIE())
      js.append("var newScrollPosY = document.body.scrollTop; "); //$NON-NLS-1$
    else
      js.append("var newScrollPosY = window.pageYOffset; "); //$NON-NLS-1$

    js.append("if (scrollPosY == newScrollPosY) { "); //$NON-NLS-1$
    js.append("  window.location.href = \"").append(ILinkHandler.HANDLER_PROTOCOL + actionId).append("\"; "); //$NON-NLS-1$ //$NON-NLS-2$
    js.append("} "); //$NON-NLS-1$

    try {
      browser.getControl().execute(js.toString());
    } finally {
      if (browser.shouldDisableScript())
        browser.setScriptDisabled(true);
    }
  }
}