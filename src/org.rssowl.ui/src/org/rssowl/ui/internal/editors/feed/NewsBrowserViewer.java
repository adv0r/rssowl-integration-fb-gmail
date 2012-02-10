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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.ApplicationActionBarAdvisor;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.AutomateFilterAction;
import org.rssowl.ui.internal.actions.MoveCopyNewsToBinAction;
import org.rssowl.ui.internal.actions.NavigationActionFactory;
import org.rssowl.ui.internal.actions.OpenInExternalBrowserAction;
import org.rssowl.ui.internal.actions.OpenNewsAction;
import org.rssowl.ui.internal.actions.CreateFilterAction.PresetAction;
import org.rssowl.ui.internal.dialogs.SearchNewsDialog;
import org.rssowl.ui.internal.editors.feed.NewsBrowserLabelProvider.Dynamic;
import org.rssowl.ui.internal.undo.NewsStateOperation;
import org.rssowl.ui.internal.undo.StickyOperation;
import org.rssowl.ui.internal.undo.UndoStack;
import org.rssowl.ui.internal.util.CBrowser;
import org.rssowl.ui.internal.util.JobRunner;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author bpasero
 */
public class NewsBrowserViewer extends ContentViewer implements ILinkHandler {

  /* ID for Link Handlers */
  static final String TITLE_HANDLER_ID = "org.rssowl.ui.search.Title"; //$NON-NLS-1$
  static final String AUTHOR_HANDLER_ID = "org.rssowl.ui.search.Author"; //$NON-NLS-1$
  static final String CATEGORY_HANDLER_ID = "org.rssowl.ui.search.Category"; //$NON-NLS-1$
  static final String LABEL_HANDLER_ID = "org.rssowl.ui.search.Label"; //$NON-NLS-1$
  //SPM DELETIOn static final String TOGGLE_READ_HANDLER_ID = "org.rssowl.ui.ToggleRead"; //$NON-NLS-1$
  static final String TOGGLE_STICKY_HANDLER_ID = "org.rssowl.ui.ToggleSticky"; //$NON-NLS-1$
  static final String DELETE_HANDLER_ID = "org.rssowl.ui.Delete"; //$NON-NLS-1$
  static final String ATTACHMENTS_MENU_HANDLER_ID = "org.rssowl.ui.AttachmentsMenu"; //$NON-NLS-1$
  static final String LABELS_MENU_HANDLER_ID = "org.rssowl.ui.LabelsMenu"; //$NON-NLS-1$
  static final String NEWS_MENU_HANDLER_ID = "org.rssowl.ui.NewsMenu"; //$NON-NLS-1$
  static final String SHARE_NEWS_MENU_HANDLER_ID = "org.rssowl.ui.ShareNewsMenu"; //$NON-NLS-1$
  static final String NEXT_NEWS_HANDLER_ID = "org.rssowl.ui.NextNews"; //$NON-NLS-1$
  static final String NEXT_UNREAD_NEWS_HANDLER_ID = "org.rssowl.ui.NextUnreadNews"; //$NON-NLS-1$
  static final String PREVIOUS_NEWS_HANDLER_ID = "org.rssowl.ui.PreviousNews"; //$NON-NLS-1$
  static final String PREVIOUS_UNREAD_NEWS_HANDLER_ID = "org.rssowl.ui.PreviousUnreadNews"; //$NON-NLS-1$

  private Object fInput;
  private CBrowser fBrowser;
  private IWorkbenchPartSite fSite;
  private boolean fIsEmbedded;
  private Menu fNewsContextMenu;
  private Menu fAttachmentsContextMenu;
  private Menu fLabelsContextMenu;
  private Menu fShareNewsContextMenu;
  private IStructuredSelection fCurrentSelection = StructuredSelection.EMPTY;
  private ApplicationServer fServer;
  private String fId;
  private boolean fBlockRefresh;
  private IModelFactory fFactory;
  private IPreferenceScope fPreferences = Owl.getPreferenceService().getGlobalScope();
  private INewsDAO fNewsDao = DynamicDAO.getDAO(INewsDAO.class);

  /* This viewer's sorter. <code>null</code> means there is no sorter. */
  private ViewerComparator fSorter;

  /* This viewer's filters (element type: <code>ViewerFilter</code>). */
  private List<ViewerFilter> fFilters;
  private NewsFilter fNewsFilter;

  /**
   * @param parent
   * @param style
   */
  public NewsBrowserViewer(Composite parent, int style) {
    this(parent, style, null);
  }

  /**
   * @param parent
   * @param style
   * @param site
   */
  public NewsBrowserViewer(Composite parent, int style, IWorkbenchPartSite site) {
    fBrowser = new CBrowser(parent, style);
    fSite = site;
    fIsEmbedded = fSite != null;
    hookControl(fBrowser.getControl());
    hookNewsContextMenu();
    hookAttachmentsContextMenu();
    hookLabelContextMenu();
    hookShareNewsContextMenu();
    fId = String.valueOf(hashCode());
    fServer = ApplicationServer.getDefault();
    fServer.register(fId, this);
    fFactory = Owl.getModelFactory();

    /* Register Link Handler */
    fBrowser.addLinkHandler(TITLE_HANDLER_ID, this);
    fBrowser.addLinkHandler(AUTHOR_HANDLER_ID, this);
    fBrowser.addLinkHandler(CATEGORY_HANDLER_ID, this);
    fBrowser.addLinkHandler(LABEL_HANDLER_ID, this);
    //SPM DELETION fBrowser.addLinkHandler(TOGGLE_READ_HANDLER_ID, this);
    fBrowser.addLinkHandler(TOGGLE_STICKY_HANDLER_ID, this);
    fBrowser.addLinkHandler(DELETE_HANDLER_ID, this);
    fBrowser.addLinkHandler(ATTACHMENTS_MENU_HANDLER_ID, this);
    fBrowser.addLinkHandler(LABELS_MENU_HANDLER_ID, this);
    fBrowser.addLinkHandler(NEWS_MENU_HANDLER_ID, this);
    fBrowser.addLinkHandler(SHARE_NEWS_MENU_HANDLER_ID, this);
    fBrowser.addLinkHandler(NEXT_NEWS_HANDLER_ID, this);
    fBrowser.addLinkHandler(NEXT_UNREAD_NEWS_HANDLER_ID, this);
    fBrowser.addLinkHandler(PREVIOUS_NEWS_HANDLER_ID, this);
    fBrowser.addLinkHandler(PREVIOUS_UNREAD_NEWS_HANDLER_ID, this);
  }

  /**
   * @param parentElement
   * @param childElement
   */
  public void add(Object parentElement, Object childElement) {
    Assert.isNotNull(parentElement);
    Assert.isNotNull(childElement);

    refresh(); // TODO Optimize
  }

  /**
   * @param parentElement
   * @param childElements
   */
  public void add(Object parentElement, Object[] childElements) {
    Assert.isNotNull(parentElement);
    assertElementsNotNull(childElements);

    if (childElements.length > 0)
      refresh(); // TODO Optimize
  }

  /**
   * Adds the given filter to this viewer.
   *
   * @param filter a viewer filter
   */
  public void addFilter(ViewerFilter filter) {
    if (fFilters == null)
      fFilters = new ArrayList<ViewerFilter>();

    fFilters.add(filter);
    if (filter instanceof NewsFilter)
      fNewsFilter = (NewsFilter) filter;
  }

  private void assertElementsNotNull(Object[] elements) {
    Assert.isNotNull(elements);
    for (Object element : elements) {
      Assert.isNotNull(element);
    }
  }

  private boolean contained(INewsBin bin, IStructuredSelection selection) {
    if (selection == null || selection.isEmpty())
      return false;

    Object element = selection.getFirstElement();
    if (element instanceof INews) {
      INews news = (INews) element;
      return news.getParentId() == bin.getId();
    }

    return false;
  }

  private void delayInUI(Runnable runnable) {
    JobRunner.runInUIThread(0, true, getControl(), runnable);
  }

  /**
   * @return The wrapped Browser (CBrowser).
   */
  public CBrowser getBrowser() {
    return fBrowser;
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#getControl()
   */
  @Override
  public Control getControl() {
    return fBrowser.getControl();
  }

  private StringBuilder getElementById(String id) {
    return new StringBuilder("document.getElementById('" + id + "')"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private Object[] getFilteredChildren(Object parent) {
    Object[] result = getRawChildren(parent);

    /* Never filter a selected News, thereby return here */
    if (fInput instanceof INews)
      return result;

    /* Run Filters over result */
    if (fFilters != null) {
      for (Object filter : fFilters) {
        ViewerFilter f = (ViewerFilter) filter;
        result = f.filter(this, parent, result);
      }
    }
    return result;
  }

  /**
   * @param input Can either be an Array of Feeds or News
   * @return An flattend array of Objects.
   */
  public Object[] getFlattendChildren(Object input) {

    /* Using NewsContentProvider */
    if (input != null && getContentProvider() instanceof NewsContentProvider) {
      NewsContentProvider cp = (NewsContentProvider) getContentProvider();

      /*
       * Flatten Children since Grouping is Enabled and the Parent is not
       * containing just News (so either Feed or ViewerGroups).
       */
      if (cp.isGroupingEnabled() && !isNews(input)) {
        List<Object> flatList = new ArrayList<Object>();

        /* Wrap into Object-Array */
        if (!(input instanceof Object[]))
          input = new Object[] { input };

        /* For each Group retrieve Children (sorted and filtered) */
        Object groups[] = (Object[]) input;
        for (Object group : groups) {

          /* Make sure this child has children */
          if (cp.hasChildren(group)) {
            Object sortedChilds[] = getSortedChildren(group);

            /* Only add if there are Childs */
            if (sortedChilds.length > 0) {
              flatList.add(group);
              flatList.addAll(Arrays.asList(sortedChilds));
            }
          }

          /* Otherwise just add */
          else {
            flatList.add(group);
          }
        }

        return flatList.toArray();
      }

      /* Grouping is not enabled, just return sorted Children */
      return getSortedChildren(input);
    }

    /* Structured ContentProvider */
    else if (input != null && getContentProvider() instanceof IStructuredContentProvider)
      return getSortedChildren(input);

    /* No Element to show */
    return new Object[0];
  }

  /**
   * @return Returns a List of Strings that should get highlighted per News that
   * is displayed.
   */
  protected Collection<String> getHighlightedWords() {
    if (getContentProvider() instanceof NewsContentProvider && fPreferences.getBoolean(DefaultPreferences.FV_HIGHLIGHT_SEARCH_RESULTS)) {
      INewsMark mark = ((NewsContentProvider) getContentProvider()).getInput();
      Set<String> extractedWords;

      /* Extract from Conditions if any */
      if (mark instanceof ISearch) {
        List<ISearchCondition> conditions = ((ISearch) mark).getSearchConditions();
        extractedWords = CoreUtils.extractWords(conditions, true);
      } else
        extractedWords = new HashSet<String>(1);

      /* Fill Pattern if set */
      if (fNewsFilter != null && StringUtils.isSet(fNewsFilter.getPatternString())) {
        String pattern = fNewsFilter.getPatternString();

        /* News Filter always converts to wildcard query */
        if (!pattern.endsWith("*")) //$NON-NLS-1$
          pattern = pattern + "*"; //$NON-NLS-1$

        StringTokenizer tokenizer = new StringTokenizer(pattern);
        while (tokenizer.hasMoreElements())
          extractedWords.add(tokenizer.nextToken());
      }

      return extractedWords;
    }

    return Collections.emptyList();
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#getInput()
   */
  @Override
  public Object getInput() {
    return fInput;
  }

  private INews getNews(String query) {
    try {
      long id = Long.parseLong(query);
      return fNewsDao.load(id);
    } catch (NullPointerException e) {
      return null;
    }
  }

  private Object[] getRawChildren(Object parent) {
    Object[] result = null;
    if (parent != null) {
      IStructuredContentProvider cp = (IStructuredContentProvider) getContentProvider();
      if (cp != null)
        result = cp.getElements(parent);
    }
    return result != null ? result : new Object[0];
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#getSelection()
   */
  @Override
  public ISelection getSelection() {
    return fCurrentSelection;
  }

  private Object[] getSortedChildren(Object parent) {
    Object[] result = getFilteredChildren(parent);
    if (fSorter != null) {

      /* be sure we're not modifying the original array from the model */
      result = result.clone();
      fSorter.sort(this, result);
    }
    return result;
  }

  /*
   * @see org.rssowl.ui.internal.ILinkHandler#handle(java.lang.String, java.net.URI)
   */
  public void handle(String id, URI link) {

    /* Extract Query Part and Decode */
    String query = link.getQuery();
    boolean queryProvided = StringUtils.isSet(query);
    if (queryProvided) {
      query = URIUtils.urlDecode(query).trim();
      queryProvided = StringUtils.isSet(query);
    }

    /* Handler to perform a Search */
    if (queryProvided && (TITLE_HANDLER_ID.equals(id) || AUTHOR_HANDLER_ID.equals(id) || CATEGORY_HANDLER_ID.equals(id) || LABEL_HANDLER_ID.equals(id))) {
      final List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);
      String entity = INews.class.getName();

      /* Search on Title */
      if (TITLE_HANDLER_ID.equals(id)) {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, entity);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, query);
        conditions.add(condition);
      }

      /* Search on Author */
      else if (AUTHOR_HANDLER_ID.equals(id)) {
        ISearchField field = fFactory.createSearchField(INews.AUTHOR, entity);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, query);
        conditions.add(condition);
      }

      /* Search on Category */
      else if (CATEGORY_HANDLER_ID.equals(id)) {
        ISearchField field = fFactory.createSearchField(INews.CATEGORIES, entity);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, query);
        conditions.add(condition);
      }

      /* Search on Label */
      else if (LABEL_HANDLER_ID.equals(id)) {
        ISearchField field = fFactory.createSearchField(INews.LABEL, entity);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, query);
        conditions.add(condition);
      }

      /* Open Dialog and Search */
      if (conditions.size() >= 1 && !fBrowser.getControl().isDisposed()) {
        final boolean useLowScoreFilter = TITLE_HANDLER_ID.equals(id);

        /* See Bug 747 - run asynced */
        delayInUI(new Runnable() {
          public void run() {
            SearchNewsDialog dialog = new SearchNewsDialog(fBrowser.getControl().getShell(), conditions, true, true);
            dialog.setUseLowScoreFilter(useLowScoreFilter);
            dialog.open();
          }
        });
      }
    }

    /* SPM DELETION  Toggle Read
    else if (queryProvided && TOGGLE_READ_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {

        INews.State newState = news.getState() == INews.State.READ ? INews.State.UNREAD : INews.State.READ;
        Set<INews> singleNewsSet = Collections.singleton(news);
        UndoStack.getInstance().addOperation(new NewsStateOperation(singleNewsSet, newState, true));
        fNewsDao.setState(singleNewsSet, newState, true, false);
      }
    }
     */
    /*  Toggle Sticky */
    else if (queryProvided && TOGGLE_STICKY_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {
        Set<INews> singleNewsSet = Collections.singleton(news);
        UndoStack.getInstance().addOperation(new StickyOperation(singleNewsSet, !news.isFlagged()));
        news.setFlagged(!news.isFlagged());
        Controller.getDefault().getSavedSearchService().forceQuickUpdate();
        DynamicDAO.saveAll(singleNewsSet);
      }
    }

    /*  Delete */
    else if (queryProvided && DELETE_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {
        Set<INews> singleNewsSet = Collections.singleton(news);
        UndoStack.getInstance().addOperation(new NewsStateOperation(singleNewsSet, INews.State.HIDDEN, false));
        fNewsDao.setState(singleNewsSet, INews.State.HIDDEN, false, false);
      }
    }

    /*  Labels Menu */
    else if (queryProvided && LABELS_MENU_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {
        setSelection(new StructuredSelection(news));
        Point cursorLocation = fBrowser.getControl().getDisplay().getCursorLocation();
        cursorLocation.y = cursorLocation.y + 16;
        fLabelsContextMenu.setLocation(cursorLocation);
        fLabelsContextMenu.setVisible(true);
      }
    }

    /*  Attachments Menu */
    else if (queryProvided && ATTACHMENTS_MENU_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {
        setSelection(new StructuredSelection(news));
        Point cursorLocation = fBrowser.getControl().getDisplay().getCursorLocation();
        cursorLocation.y = cursorLocation.y + 16;
        fAttachmentsContextMenu.setLocation(cursorLocation);
        fAttachmentsContextMenu.setVisible(true);
      }
    }

    /* News Context Menu */
    else if (queryProvided && NEWS_MENU_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {
        setSelection(new StructuredSelection(news));
        Point cursorLocation = fBrowser.getControl().getDisplay().getCursorLocation();
        cursorLocation.y = cursorLocation.y + 16;
        fNewsContextMenu.setLocation(cursorLocation);
        fNewsContextMenu.setVisible(true);
      }
    }

    /* Share News Context Menu */
    else if (queryProvided && SHARE_NEWS_MENU_HANDLER_ID.equals(id)) {
      INews news = getNews(query);
      if (news != null) {
        setSelection(new StructuredSelection(news));
        Point cursorLocation = fBrowser.getControl().getDisplay().getCursorLocation();
        cursorLocation.y = cursorLocation.y + 16;
        fShareNewsContextMenu.setLocation(cursorLocation);
        fShareNewsContextMenu.setVisible(true);
      }
    }

    /* Go to Next News */
    else if (NEXT_NEWS_HANDLER_ID.equals(id)) {
      delayInUI(new Runnable() {
        public void run() {
          NavigationActionFactory factory = new NavigationActionFactory();
          try {
            factory.setInitializationData(null, null, NavigationActionFactory.NavigationActionType.NEXT_FEED.getId());
            IWorkbenchWindowActionDelegate action = (IWorkbenchWindowActionDelegate) factory.create();
            action.run(null);
          } catch (CoreException e) {
            /* Ignore */
          }
        }
      });
    }

    /* Go to Next Unread News */
    else if (NEXT_UNREAD_NEWS_HANDLER_ID.equals(id)) {
      delayInUI(new Runnable() {
        public void run() {
          NavigationActionFactory factory = new NavigationActionFactory();
          try {
            factory.setInitializationData(null, null, NavigationActionFactory.NavigationActionType.NEXT_UNREAD_FEED.getId());
            IWorkbenchWindowActionDelegate action = (IWorkbenchWindowActionDelegate) factory.create();
            action.run(null);
          } catch (CoreException e) {
            /* Ignore */
          }
        }
      });
    }

    /* Go to Previous News */
    else if (PREVIOUS_NEWS_HANDLER_ID.equals(id)) {
      delayInUI(new Runnable() {
        public void run() {
          NavigationActionFactory factory = new NavigationActionFactory();
          try {
            factory.setInitializationData(null, null, NavigationActionFactory.NavigationActionType.PREVIOUS_FEED.getId());
            IWorkbenchWindowActionDelegate action = (IWorkbenchWindowActionDelegate) factory.create();
            action.run(null);
          } catch (CoreException e) {
            /* Ignore */
          }
        }
      });
    }

    /* Go to Previous Unread News */
    else if (PREVIOUS_UNREAD_NEWS_HANDLER_ID.equals(id)) {
      delayInUI(new Runnable() {
        public void run() {
          NavigationActionFactory factory = new NavigationActionFactory();
          try {
            factory.setInitializationData(null, null, NavigationActionFactory.NavigationActionType.PREVIOUS_UNREAD_FEED.getId());
            IWorkbenchWindowActionDelegate action = (IWorkbenchWindowActionDelegate) factory.create();
            action.run(null);
          } catch (CoreException e) {
            /* Ignore */
          }
        }
      });
    }
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
   */
  @Override
  protected void handleDispose(DisposeEvent event) {
    fServer.unregister(fId);
    fCurrentSelection = null;
    fNewsContextMenu.dispose();
    fAttachmentsContextMenu.dispose();
    fLabelsContextMenu.dispose();
    fShareNewsContextMenu.dispose();
    super.handleDispose(event);
  }

  /**
   * Shows the intial Input in the Browser.
   */
  public void home() {
    setInput(fInput, true);
  }

  private void hookAttachmentsContextMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        ApplicationActionBarAdvisor.fillAttachmentsMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), true);
      }
    });

    /* Create  */
    fAttachmentsContextMenu = manager.createContextMenu(fBrowser.getControl().getShell());
  }

  private void hookLabelContextMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        ApplicationActionBarAdvisor.fillLabelMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), true);
      }
    });

    /* Create  */
    fLabelsContextMenu = manager.createContextMenu(fBrowser.getControl().getShell());
  }

  private void hookNewsContextMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      @SuppressWarnings("restriction")
      public void menuAboutToShow(IMenuManager manager) {

        /* Open */
        {
          boolean useSeparator = true;

          /* Open in FeedView */
          if (!fIsEmbedded) {
            manager.add(new Separator("internalopen")); //$NON-NLS-1$
            if (!fCurrentSelection.isEmpty()) {
              manager.appendToGroup("internalopen", new OpenNewsAction(fCurrentSelection, fBrowser.getControl().getShell())); //$NON-NLS-1$
              useSeparator = false;
            }
          }

          manager.add(useSeparator ? new Separator("open") : new GroupMarker("open")); //$NON-NLS-1$ //$NON-NLS-2$

          /* Show only when internal browser is used */
          if (!fCurrentSelection.isEmpty() && !fPreferences.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER) && !fPreferences.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER))
            manager.add(new OpenInExternalBrowserAction(fCurrentSelection));
        }

        /* Attachments */
        {
          ApplicationActionBarAdvisor.fillAttachmentsMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), false);
        }

        /* Mark / Label */
        {
          /*manager.add(new Separator("mark")); //$NON-NLS-1$  //SPM DELETION


          MenuManager markMenu = new MenuManager(Messages.NewsBrowserViewer_MARK, "mark"); //$NON-NLS-1$
          manager.add(markMenu);

          /* Mark as Read
          IAction action = new ToggleReadStateAction(fCurrentSelection);
          action.setEnabled(!fCurrentSelection.isEmpty());
          markMenu.add(action);

          /* Mark All Read
          action = new MarkAllNewsReadAction();
          markMenu.add(action);

          /* Sticky
          markMenu.add(new Separator());
          action = new MakeNewsStickyAction(fCurrentSelection);
          action.setEnabled(!fCurrentSelection.isEmpty());
          markMenu.add(action);
           */
          /* Label */
          ApplicationActionBarAdvisor.fillLabelMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), false);
        }

        /* Move To / Copy To */
        if (!fCurrentSelection.isEmpty()) {
          manager.add(new Separator("movecopy")); //$NON-NLS-1$

          /* Load all news bins and sort by name */
          List<INewsBin> newsbins = new ArrayList<INewsBin>(DynamicDAO.loadAll(INewsBin.class));

          Comparator<INewsBin> comparator = new Comparator<INewsBin>() {
            public int compare(INewsBin o1, INewsBin o2) {
              return o1.getName().compareTo(o2.getName());
            };
          };

          Collections.sort(newsbins, comparator);

          /* Move To */
          MenuManager moveMenu = new MenuManager(Messages.NewsBrowserViewer_MOVE_TO, "moveto"); //$NON-NLS-1$
          manager.add(moveMenu);

          for (INewsBin bin : newsbins) {
            if (contained(bin, fCurrentSelection))
              continue;

            moveMenu.add(new MoveCopyNewsToBinAction(fCurrentSelection, bin, true));
          }

          moveMenu.add(new MoveCopyNewsToBinAction(fCurrentSelection, null, true));
          moveMenu.add(new Separator());
          moveMenu.add(new AutomateFilterAction(PresetAction.MOVE, fCurrentSelection));

          /* Copy To */
          MenuManager copyMenu = new MenuManager(Messages.NewsBrowserViewer_COPY_TO, "copyto"); //$NON-NLS-1$
          manager.add(copyMenu);

          for (INewsBin bin : newsbins) {
            if (contained(bin, fCurrentSelection))
              continue;

            copyMenu.add(new MoveCopyNewsToBinAction(fCurrentSelection, bin, false));
          }

          copyMenu.add(new MoveCopyNewsToBinAction(fCurrentSelection, null, false));
          copyMenu.add(new Separator());
          copyMenu.add(new AutomateFilterAction(PresetAction.COPY, fCurrentSelection));
        }

        /* Share */
        {
          ApplicationActionBarAdvisor.fillShareMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), false);
        }

        manager.add(new Separator("filter")); //$NON-NLS-1$
        manager.add(new Separator("copy")); //$NON-NLS-1$
        manager.add(new GroupMarker("edit")); //$NON-NLS-1$
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        /* Fill Contributions if Context Menu not registered */
        if (fSite == null)
          org.eclipse.ui.internal.ObjectActionContributorManager.getManager().contributeObjectActions(null, manager, NewsBrowserViewer.this);
      }
    });

    /* Create and Register with Workbench */
    fNewsContextMenu = manager.createContextMenu(fBrowser.getControl().getShell());

    /* Register with Part Site if possible */
    if (fSite != null)
      fSite.registerContextMenu(manager, this);
  }

  private void hookShareNewsContextMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        ApplicationActionBarAdvisor.fillShareMenu(manager, fCurrentSelection, new SameShellProvider(fBrowser.getControl().getShell()), true);
      }
    });

    /* Create  */
    fShareNewsContextMenu = manager.createContextMenu(fBrowser.getControl().getShell());
  }

  private boolean internalRemove(Object[] elements) {
    boolean toggleJS = fBrowser.shouldDisableScript();
    try {
      if (toggleJS)
        fBrowser.setScriptDisabled(false);

      for (Object element : elements) {
        if (element instanceof INews) {
          INews news = (INews) element;
          StringBuilder js = new StringBuilder();
          js.append("var node = ").append(getElementById(Dynamic.NEWS.getId(news))).append("; "); //$NON-NLS-1$ //$NON-NLS-2$
          js.append("if (node != null) { "); //$NON-NLS-1$
          js.append("node.className='hidden';"); //$NON-NLS-1$
          js.append(" } "); //$NON-NLS-1$

          boolean res = fBrowser.getControl().execute(js.toString());
          if (!res)
            return false;
        }
      }
    } finally {
      if (toggleJS)
        fBrowser.setScriptDisabled(true);
    }

    return true;
  }

  private boolean internalUpdate(Set<NewsEvent> newsEvents) {
    boolean toggleJS = fBrowser.shouldDisableScript();
    try {
      if (toggleJS)
        fBrowser.setScriptDisabled(false);

      /* Update for each Event */
      for (NewsEvent newsEvent : newsEvents) {
        INews news = newsEvent.getEntity();

        StringBuilder js = new StringBuilder();

        /* State (Bold/Plain Title, Mark Read Tooltip) */
        if (CoreUtils.isStateChange(newsEvent)) {
          /* SPM DELETION   boolean isRead = INews.State.READ == news.getState();
          js.append(getElementById(Dynamic.NEWS.getId(news)).append(isRead ? ".className='newsitemRead'; " : ".className='newsitemUnread'; ")); //$NON-NLS-1$ //$NON-NLS-2$
          js.append(getElementById(Dynamic.TITLE.getId(news)).append(isRead ? ".className='read'; " : ".className='unread'; ")); //$NON-NLS-1$ //$NON-NLS-2$
          js.append(getElementById(Dynamic.TOGGLE_READ_LINK.getId(news)).append(isRead ? ".title='Mark Unread'; " : ".title='Mark Read'; ")); //$NON-NLS-1$ //$NON-NLS-2$
          js.append(getElementById(Dynamic.TOGGLE_READ_IMG.getId(news)).append(isRead ? ".alt='Mark Unread'; " : ".alt='Mark Read'; ")); //$NON-NLS-1$ //$NON-NLS-2$ */
        }

        /* Sticky (Title Background, Footer Background, Mark Sticky Image) */
        if (CoreUtils.isStickyStateChange(newsEvent)) {
          boolean isSticky = news.isFlagged();
          js.append(getElementById(Dynamic.HEADER.getId(news)).append(isSticky ? ".className='headerSticky'; " : ".className='header'; ")); //$NON-NLS-1$ //$NON-NLS-2$
          js.append(getElementById(Dynamic.FOOTER.getId(news)).append(isSticky ? ".className='footerSticky'; " : ".className='footer'; ")); //$NON-NLS-1$ //$NON-NLS-2$

          String stickyImgUri;
          if (fBrowser.isIE())
            stickyImgUri = isSticky ? OwlUI.getImageUri("/icons/obj16/news_pinned_light.gif", "news_pinned_light.gif") : OwlUI.getImageUri("/icons/obj16/news_pin_light.gif", "news_pin_light.gif"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            else
              stickyImgUri = isSticky ? ApplicationServer.getDefault().toResourceUrl("/icons/obj16/news_pinned_light.gif") : ApplicationServer.getDefault().toResourceUrl("/icons/obj16/news_pin_light.gif"); //$NON-NLS-1$ //$NON-NLS-2$

              js.append(getElementById(Dynamic.TOGGLE_STICKY.getId(news)).append(".src='").append(stickyImgUri).append("'; ")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        /* Label (Title Foreground, Label List) */
        if (CoreUtils.isLabelChange(newsEvent)) {
          Set<ILabel> labels = CoreUtils.getSortedLabels(news);
          String defaultColor = CoreUtils.getLink(news) != null ? "#009" : "rgb(0,0,0)"; //$NON-NLS-1$ //$NON-NLS-2$
          String color = labels.isEmpty() ? defaultColor : "rgb(" + OwlUI.toString(OwlUI.getRGB(labels.iterator().next())) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
          if ("rgb(0,0,0)".equals(color)) //Don't let black override link color //$NON-NLS-1$
            color = defaultColor;
          js.append(getElementById(Dynamic.TITLE.getId(news)).append(".style.color='").append(color).append("'; ")); //$NON-NLS-1$ //$NON-NLS-2$

          if (labels.isEmpty()) {
            js.append(getElementById(Dynamic.LABELS_SEPARATOR.getId(news)).append(".style.display='none'; ")); //$NON-NLS-1$
            js.append(getElementById(Dynamic.LABELS.getId(news)).append(".innerHTML=''; ")); //$NON-NLS-1$
          } else {
            js.append(getElementById(Dynamic.LABELS_SEPARATOR.getId(news)).append(".style.display='inline'; ")); //$NON-NLS-1$

            StringBuilder labelsHtml = new StringBuilder(Messages.NewsBrowserViewer_LABELS);
            labelsHtml.append(" "); //$NON-NLS-1$
            int c = 0;
            for (ILabel label : labels) {
              c++;
              if (c < labels.size())
                span(labelsHtml, StringUtils.htmlEscape(label.getName()) + ", ", label.getColor()); //$NON-NLS-1$
              else
                span(labelsHtml, StringUtils.htmlEscape(label.getName()), label.getColor());
            }

            js.append(getElementById(Dynamic.LABELS.getId(news)).append(".innerHTML='").append(labelsHtml.toString()).append("'; ")); //$NON-NLS-1$ //$NON-NLS-2$
          }
        }

        if (js.length() > 0) {
          boolean res = fBrowser.getControl().execute(js.toString());
          if (!res)
            return false;
        }
      }
    } finally {
      if (toggleJS)
        fBrowser.setScriptDisabled(true);
    }

    return true;
  }

  /* Returns TRUE if the Input consists only of INews */
  private boolean isNews(Object input) {
    if (input instanceof Object[]) {
      Object elements[] = (Object[]) input;
      for (Object element : elements) {
        if (!(element instanceof INews))
          return false;
      }
    } else if (!(input instanceof INews))
      return false;

    return true;
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#refresh()
   */
  @Override
  public void refresh() {
    if (!fBlockRefresh)
      fBrowser.refresh();
  }

  /**
   * @param element
   */
  public void remove(Object element) {
    Assert.isNotNull(element);

    /* Refresh if dynamic removal failed */
    if (!internalRemove(new Object[] { element }))
      refresh();
  }

  /**
   * @param objects
   */
  public void remove(Object[] objects) {
    assertElementsNotNull(objects);

    /* Refresh if dynamic removal failed */
    if (!internalRemove(objects))
      refresh();
  }

  /**
   * Removes the given filter from this viewer, and triggers refiltering and
   * resorting of the elements if required. Has no effect if the identical
   * filter is not registered.
   *
   * @param filter a viewer filter
   */
  public void removeFilter(ViewerFilter filter) {
    Assert.isNotNull(filter);
    if (fFilters != null) {
      for (Iterator<ViewerFilter> i = fFilters.iterator(); i.hasNext();) {
        Object o = i.next();
        if (o == filter) {
          i.remove();
          refresh();
          if (fFilters.size() == 0)
            fFilters = null;

          return;
        }
      }
    }

    if (filter == fNewsFilter)
      fNewsFilter = null;
  }

  /* Checks wether the given Input is same to the existing one */
  private boolean sameInput(Object input) {
    if (fInput instanceof Object[])
      return input instanceof Object[] && Arrays.equals((Object[]) fInput, (Object[]) input);

    if (fInput != null)
      return fInput.equals(input);

    return false;
  }

  void setBlockRefresh(boolean block) {
    fBlockRefresh = block;
  }

  /**
   * @param comparator
   */
  public void setComparator(ViewerComparator comparator) {
    if (fSorter != comparator)
      fSorter = comparator;
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#setContentProvider(org.eclipse.jface.viewers.IContentProvider)
   */
  @Override
  public void setContentProvider(IContentProvider contentProvider) {
    fBlockRefresh = true;
    try {
      super.setContentProvider(contentProvider);
    } finally {
      fBlockRefresh = false;
    }
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#setInput(java.lang.Object)
   */
  @Override
  public void setInput(Object input) {
    setInput(input, false);
  }

  private void setInput(Object input, boolean force) {

    /* Ignore this Input if its already set */
    if (!force && sameInput(input))
      return;

    /* Remember Input */
    fInput = input;

    /* Stop any other Website if required */
    String url = fBrowser.getControl().getUrl();
    if (!"".equals(url)) //$NON-NLS-1$
      fBrowser.getControl().stop();

    /* Input is a URL - display it */
    if (input instanceof String) {
      fBrowser.setUrl((String) input, true);
      return;
    }

    /* Set URL if its not already showing and contains a display-operation */
    String inputUrl = fServer.toUrl(fId, input);
    if (fServer.isDisplayOperation(inputUrl) && !inputUrl.equals(url))
      fBrowser.setUrl(inputUrl);

    /* Hide the Browser as soon as the input is set to Null */
    if (input == null && fBrowser.getControl().getVisible())
      fBrowser.getControl().setVisible(false);
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#setLabelProvider(org.eclipse.jface.viewers.IBaseLabelProvider)
   */
  @Override
  public void setLabelProvider(IBaseLabelProvider labelProvider) {
    fBlockRefresh = true;
    try {
      super.setLabelProvider(labelProvider);
    } finally {
      fBlockRefresh = false;
    }
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection,
   * boolean)
   */
  @Override
  public void setSelection(ISelection selection, boolean reveal) {
    fCurrentSelection = (IStructuredSelection) selection;
    fireSelectionChanged(new SelectionChangedEvent(this, selection));
  }

  private void span(StringBuilder builder, String content, String color) {
    builder.append("<span style=\"color: rgb(").append(color).append(");\""); //$NON-NLS-1$ //$NON-NLS-2$
    builder.append(">").append(content).append("</span>"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * @param news
   */
  public void update(Set<NewsEvent> news) {

    /*
     * The update-event could have been sent out a lot faster than the Browser
     * having a chance to react. In this case, rather then refreshing a possible
     * blank page (or wrong page), re-set the input.
     */
    String inputUrl = fServer.toUrl(fId, fInput);
    String browserUrl = fBrowser.getControl().getUrl();
    boolean resetInput = browserUrl.length() == 0 || URIUtils.ABOUT_BLANK.equals(browserUrl);
    if (inputUrl.equals(browserUrl)) {
      if (!internalUpdate(news))
        refresh(); // Refresh if dynamic update failed
    } else if (fServer.isDisplayOperation(inputUrl) && resetInput)
      fBrowser.setUrl(inputUrl);
  }
}