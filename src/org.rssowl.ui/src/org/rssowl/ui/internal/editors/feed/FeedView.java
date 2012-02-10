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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.IAbortable;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsBinDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.dao.ISearchMarkDAO;
import org.rssowl.core.persist.event.BookMarkAdapter;
import org.rssowl.core.persist.event.BookMarkEvent;
import org.rssowl.core.persist.event.BookMarkListener;
import org.rssowl.core.persist.event.FeedAdapter;
import org.rssowl.core.persist.event.FeedEvent;
import org.rssowl.core.persist.event.FolderAdapter;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.persist.event.MarkEvent;
import org.rssowl.core.persist.event.NewsBinAdapter;
import org.rssowl.core.persist.event.NewsBinEvent;
import org.rssowl.core.persist.event.NewsBinListener;
import org.rssowl.core.persist.event.SearchConditionEvent;
import org.rssowl.core.persist.event.SearchConditionListener;
import org.rssowl.core.persist.event.SearchMarkAdapter;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.ITreeNode;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.RetentionStrategy;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.TreeTraversal;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.FolderNewsMark;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.Controller.BookMarkLoadListener;
import org.rssowl.ui.internal.actions.DeleteTypesAction;
import org.rssowl.ui.internal.actions.FindAction;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.actions.RetargetActions;
import org.rssowl.ui.internal.undo.UndoStack;
import org.rssowl.ui.internal.util.CBrowser;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.UIBackgroundJob;
import org.rssowl.ui.internal.util.WidgetTreeNode;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The FeedView is an instance of <code>EditorPart</code> capable of displaying
 * News in a Table-Viewer and Browser-Viewer. It offers controls to Filter and
 * Group them.
 *
 * @author bpasero
 */
public class FeedView extends EditorPart implements IReusableEditor {

  /** List of UI-Events interesting for the FeedView */
  public enum UIEvent {

    /** Other Feed Displayed */
    FEED_CHANGE,

    /** Application Minimized */
    MINIMIZE,

    /** Application Closing */
    CLOSE,

    /** Tab Closed */
    TAB_CLOSE
  }

  /* TODO Move this to Settings */
  private static final boolean BROWSER_SHOWS_ALL = false;

  /* Delay in ms to Mark *new* News to *unread* on Part-Deactivation */
  private static final int HANDLE_NEWS_SEEN_DELAY = 100;

  /* Millies before news seen are handled */
  private static final int HANDLE_NEWS_SEEN_BLOCK_DELAY = 800;

  /* Millies before the next clean up is allowed to run again */
  private static final int CLEAN_UP_BLOCK_DELAY = 1000;

  /* The last visible Feedview */
  private static FeedView fgLastVisibleFeedView = null;

  /* Flag to indicate if feed change events should be blocked or not */
  private static boolean fgBlockFeedChangeEvent;

  /** ID of this EditorPart */
  public static final String ID = "org.rssowl.ui.FeedView"; //$NON-NLS-1$

  /**
   * @param blockFeedChangeEvent <code>true</code> to block the processing of
   * feed change events and <code>false</code> otherwise.
   */
  public static void setBlockFeedChangeEvent(boolean blockFeedChangeEvent) {
    fgBlockFeedChangeEvent = blockFeedChangeEvent;
  }
  /* Editor Data */
  private FeedViewInput fInput;

  private IEditorSite fEditorSite;

  /* Part to display News in Table */
  private NewsTableControl fNewsTableControl;

  /* Part to display News in Browser */
  private NewsBrowserControl fNewsBrowserControl;
  /* Bars */
  private FilterBar fFilterBar;

  private BrowserBar fBrowserBar;
  /* Shared Viewer classes */
  private NewsFilter fNewsFilter;
  private NewsGrouping fNewsGrouping;

  private NewsContentProvider fContentProvider;

  /* Container for the Browser Viewer */
  private Composite fBrowserViewerControlContainer;
  /* Listeners */
  private IPartListener2 fPartListener;
  private BookMarkListener fBookMarkListener;
  private SearchMarkAdapter fSearchMarkListener;
  private FeedAdapter fFeedListener;
  private SearchConditionListener fSearchConditionListener;
  private NewsBinListener fNewsBinListener;
  private FolderAdapter fFolderListener;

  private BookMarkLoadListener fBookMarkLoadListener;
  /* Settings */
  NewsFilter.Type fInitialFilterType;
  NewsGrouping.Type fInitialGroupType;
  NewsFilter.SearchTarget fInitialSearchTarget;
  boolean fInitialLayoutClassic;
  private boolean fInitialBrowserMaximized;
  private int fInitialWeights[];

  private int fCacheWeights[];
  /* Global Actions */
  private IAction fReloadAction;
  private IAction fSelectAllAction;
  private IAction fDeleteAction;
  private IAction fCutAction;
  private IAction fCopyAction;
  private IAction fPasteAction;
  private IAction fPrintAction;
  private IAction fUndoAction;
  private IAction fRedoAction;

  private IAction fFindAction;
  /* Misc. */
  private Composite fParent;
  private SashForm fSashForm;
  private Label fHorizontalTableBrowserSep;
  private Label fVerticalTableBrowserSep;
  private LocalResourceManager fResourceManager;
  private IPreferenceScope fPreferences;
  private long fOpenTime;
  private boolean fCreated;
  private final Object fCacheJobIdentifier = new Object();
  private ImageDescriptor fTitleImageDescriptor;
  private Label fHorizontalBrowserSep;
  private Label fVerticalBrowserSep;
  private final INewsDAO fNewsDao = Owl.getPersistenceService().getDAOService().getNewsDAO();
  private boolean fIsDisposed;

  private AtomicLong fLastCleanUpRun = new AtomicLong();

  private void createGlobalActions() {

    /* Hook into Reload */
    fReloadAction = new Action() {
      @Override
      public void run() {
        new ReloadTypesAction(new StructuredSelection(fInput.getMark()), getEditorSite().getShell()).run();
      }
    };

    /* Select All */
    fSelectAllAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Select All in Text Widget */
        if (focusControl instanceof Text) {
          ((Text) focusControl).selectAll();
        }

        /* Select All in Viewer Tree */
        else {
          ((Tree) fNewsTableControl.getViewer().getControl()).selectAll();
          fNewsTableControl.getViewer().setSelection(fNewsTableControl.getViewer().getSelection());
        }
      }
    };

    /* Delete */
    fDeleteAction = new Action() {
      @Override
      public void run() {
        new DeleteTypesAction(fParent.getShell(), (IStructuredSelection) fNewsTableControl.getViewer().getSelection()).run();
      }
    };

    /* Cut */
    fCutAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Cut in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).cut();
      }
    };

    /* Copy */
    fCopyAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Copy in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).copy();
      }
    };

    /* Paste */
    fPasteAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Paste in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).paste();
      }
    };

    /* Print */
    fPrintAction = new Action() {
      @Override
      public void run() {
        print();
      }
    };

    /* Undo (Eclipse Integration) */
    fUndoAction = new Action() {
      @Override
      public void run() {
        UndoStack.getInstance().undo();
      }
    };

    /* Redo (Eclipse Integration) */
    fRedoAction = new Action() {
      @Override
      public void run() {
        UndoStack.getInstance().redo();
      }
    };

    /* Find (Eclipse Integration) */
    fFindAction = new FindAction();
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    fCreated = true;
    fParent = parent;

    /* Shared Viewer Helper */
    fNewsFilter = new NewsFilter();
    fNewsFilter.setType(fInitialFilterType);
    fNewsFilter.setSearchTarget(fInitialSearchTarget);
    fNewsFilter.setNewsMark(fInput.getMark());

    fNewsGrouping = new NewsGrouping();
    fNewsGrouping.setType(fInitialGroupType);

    /* Top-Most root Composite in Editor */
    Composite rootComposite = new Composite(fParent, SWT.NONE);
    rootComposite.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    ((GridLayout) rootComposite.getLayout()).verticalSpacing = 0;

    /* FilterBar */
    fFilterBar = new FilterBar(this, rootComposite);

    /* Separate from SashForm */
    if (!Application.IS_MAC || fInitialBrowserMaximized || !fInitialLayoutClassic) {
      Label sep = new Label(rootComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
      sep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    }

    /* SashForm dividing Feed and News View */
    fSashForm = new SashForm(rootComposite, (fInitialLayoutClassic ? SWT.VERTICAL : SWT.HORIZONTAL) | SWT.SMOOTH);
    fSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Table-Viewer to display headlines */
    NewsTableViewer tableViewer;
    {
      Composite container = new Composite(fSashForm, SWT.None);
      container.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
      container.addControlListener(new ControlAdapter() {
        @Override
        public void controlResized(ControlEvent e) {
          fCacheWeights = fSashForm.getWeights();
        }
      });

      fNewsTableControl = new NewsTableControl();
      fNewsTableControl.init(fEditorSite);
      fNewsTableControl.onInputChanged(fInput);

      /* Create Viewer */
      tableViewer = fNewsTableControl.createViewer(container);

      /* Clear any quicksearch when ESC is hit from the Tree */
      tableViewer.getControl().addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.keyCode == SWT.ESC)
            fFilterBar.clearQuickSearch(true);
        }
      });

      /* Separate from Browser-Viewer (Vertically) */
      fVerticalTableBrowserSep = new Label(container, SWT.SEPARATOR | SWT.VERTICAL);
      fVerticalTableBrowserSep.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));
      ((GridData) fVerticalTableBrowserSep.getLayoutData()).exclude = fInitialLayoutClassic || fInitialBrowserMaximized;

      /* Separate from Browser-Viewer (Horizontally) */
      fHorizontalTableBrowserSep = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
      fHorizontalTableBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
      ((GridData) fHorizontalTableBrowserSep.getLayoutData()).exclude = !fInitialLayoutClassic || fInitialBrowserMaximized;
    }

    /* Browser-Viewer to display news */
    NewsBrowserViewer browserViewer;
    {
      fBrowserViewerControlContainer = new Composite(fSashForm, SWT.None);
      fBrowserViewerControlContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
      fBrowserViewerControlContainer.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

      /* Separate to Browser (Vertically) */
      fVerticalBrowserSep = new Label(fBrowserViewerControlContainer, SWT.SEPARATOR | SWT.VERTICAL);
      fVerticalBrowserSep.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false, 1, 3));
      ((GridData) fVerticalBrowserSep.getLayoutData()).exclude = fInitialLayoutClassic || fInitialBrowserMaximized;

      /* Browser Bar for Navigation */
      fBrowserBar = new BrowserBar(this, fBrowserViewerControlContainer);

      /* Separate to Browser (Horizontally) */
      fHorizontalBrowserSep = new Label(fBrowserViewerControlContainer, SWT.SEPARATOR | SWT.HORIZONTAL);

      /* Horizontal Layout */
      if (fInitialLayoutClassic && !fInitialBrowserMaximized) {
        fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, fInitialLayoutClassic ? 2 : 1, 1));
        ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = false;
      }

      /* Verical Layout */
      else if (!fInitialBrowserMaximized) {
        fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, fInitialLayoutClassic ? 2 : 1, 1));
        ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = !fBrowserBar.isVisible();
      }

      /* Browser Maximized */
      else {
        fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
        ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = !fBrowserBar.isVisible();
      }

      fNewsBrowserControl = new NewsBrowserControl();
      fNewsBrowserControl.init(fEditorSite);
      fNewsBrowserControl.onInputChanged(fInput);

      /* Create Viewer */
      browserViewer = fNewsBrowserControl.createViewer(fBrowserViewerControlContainer);

      /* Clear any quicksearch when ESC is hit from the Tree */
      browserViewer.getControl().addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.keyCode == SWT.ESC)
            fFilterBar.clearQuickSearch(true);
        }
      });

      /* Init the Browser Bar with the CBrowser */
      fBrowserBar.init(browserViewer.getBrowser());
    }

    /* SashForm weights */
    fSashForm.setWeights(fInitialWeights);
    if (fInitialBrowserMaximized)
      fSashForm.setMaximizedControl(fBrowserViewerControlContainer);

    /* Create the shared Content-Provider */
    fContentProvider = new NewsContentProvider(tableViewer, browserViewer, this);

    /* Init all Viewers */
    fNewsTableControl.initViewer(fContentProvider, fNewsFilter);
    fNewsBrowserControl.initViewer(fContentProvider, fNewsFilter);

    /* Set Input to Viewers */
    setInput(fInput.getMark(), false);
  }

  @Override
  public void dispose() {
    saveSettings();
    unregisterListeners();

    super.dispose();
    fContentProvider.dispose();
    fNewsTableControl.dispose();
    fNewsBrowserControl.dispose();
    fResourceManager.dispose();
    fIsDisposed = true;
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void doSave(IProgressMonitor monitor) {
    /* Not Supported */
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#doSaveAs()
   */
  @Override
  public void doSaveAs() {
    if (fIsDisposed || Controller.getDefault().isShuttingDown())
      return;

    /* Ask user for File */
    FileDialog dialog = new FileDialog(getSite().getShell(), SWT.SAVE);
    dialog.setOverwrite(true);
    if (fInput.getMark() instanceof IBookMark)
      dialog.setFilterExtensions(new String[] { ".html", ".xml" }); //$NON-NLS-1$ //$NON-NLS-2$
    else
      dialog.setFilterExtensions(new String[] { ".html" }); //$NON-NLS-1$
    dialog.setFileName(Application.IS_WINDOWS ? CoreUtils.getSafeFileNameForWindows(fInput.getName()) : fInput.getName());

    String fileName = dialog.open();
    if (fileName == null)
      return;

    if (fileName.endsWith(".xml")) //$NON-NLS-1$
      saveAsXml(fileName);
    else
      saveAsHtml(fileName);
  }

  private void expandNewsTableViewerGroups(boolean delayRedraw, ISelection oldSelection) {
    TreeViewer viewer = fNewsTableControl.getViewer();
    Tree tree = (Tree) viewer.getControl();

    /* Remember TopItem if required */
    TreeItem topItem = oldSelection.isEmpty() ? tree.getTopItem() : null;

    /* Expand All & Restore Selection with redraw false */
    if (delayRedraw)
      tree.getParent().setRedraw(false);
    try {
      viewer.expandAll();

      /* Restore selection if required */
      if (!oldSelection.isEmpty() && viewer.getSelection().isEmpty())
        viewer.setSelection(oldSelection, true);
      else if (topItem != null)
        tree.setTopItem(topItem);
    } finally {
      if (delayRedraw)
        tree.getParent().setRedraw(true);
    }
  }

  private void fillBookMarksToReload(List<IBookMark> bookMarksToReload, IFolder folder) {
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof IBookMark) {
        if (((IBookMark) mark).getMostRecentNewsDate() == null)
          bookMarksToReload.add((IBookMark) mark);
      }
    }

    List<IFolder> childs = folder.getFolders();
    for (IFolder child : childs) {
      fillBookMarksToReload(bookMarksToReload, child);
    }
  }

  /**
   * The user performed the "Find" action.
   */
  public void find() {
    if (fFilterBar != null)
      fFilterBar.focusQuickSearch();
  }

  /**
   * Returns the <code>Composite</code> that is the Parent Control of this
   * Editor Part.
   *
   * @return The <code>Composite</code> that is the Parent Control of this
   * Editor Part.
   */
  Composite getEditorControl() {
    return fParent;
  }

  /**
   * Get the shared ViewerFilter used to filter News.
   *
   * @return the shared ViewerFilter used to filter News.
   */
  NewsFilter getFilter() {
    return fNewsFilter;
  }

  /**
   * Get the shared Viewer-Grouper used to group News.
   *
   * @return the shared Viewer-Grouper used to group News.
   */
  NewsGrouping getGrouper() {
    return fNewsGrouping;
  }

  NewsBrowserControl getNewsBrowserControl() {
    return fNewsBrowserControl;
  }

  NewsTableControl getNewsTableControl() {
    return fNewsTableControl;
  }

  /**
   * A special key was pressed from the Quicksearch Input-Field. Handle it.
   *
   * @param traversal The Traversal that occured from the quicksearch.
   */
  void handleQuicksearchTraversalEvent(int traversal) {

    /* Enter was hit */
    if ((traversal & SWT.TRAVERSE_RETURN) != 0) {

      /* Select and Focus TreeViewer */
      if (isTableViewerVisible()) {
        Tree tree = (Tree) fNewsTableControl.getViewer().getControl();
        if (tree.getItemCount() > 0) {
          fNewsTableControl.getViewer().setSelection(new StructuredSelection(tree.getItem(0).getData()));
          fNewsTableControl.setFocus();
        }
      }

      /* Move Focus into BrowserViewer */
      else {
        fNewsBrowserControl.setFocus();
      }
    }

    /* Page Up / Down was hit */
    else if ((traversal & SWT.TRAVERSE_PAGE_NEXT) != 0 || (traversal & SWT.TRAVERSE_PAGE_PREVIOUS) != 0) {
      setFocus();
    }
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
   * org.eclipse.ui.IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) {
    Assert.isTrue(input instanceof FeedViewInput);

    fEditorSite = site;
    setSite(site);
    fResourceManager = new LocalResourceManager(JFaceResources.getResources());

    /* Load Settings */
    fPreferences = Owl.getPreferenceService().getGlobalScope();
    loadSettings((FeedViewInput) input);

    /* Apply Input */
    setInput(input);

    /* Hook into Global Actions */
    createGlobalActions();
    setGlobalActions();

    /* Register Listeners */
    registerListeners();
  }

  /**
   * @return <code>true</code> if the news browser viewer of this feed view is
   * showing the contents of a website and <code>false</code> otherwise.
   */
  public boolean isBrowserShowingNews() {
    if (fNewsBrowserControl != null && fNewsBrowserControl.getViewer() != null) {
      CBrowser browser = fNewsBrowserControl.getViewer().getBrowser();
      if (browser != null && browser.getControl() != null && !browser.getControl().isDisposed()) {
        String url = browser.getControl().getUrl();
        return StringUtils.isSet(url) && ApplicationServer.getDefault().isNewsServerUrl(url);
      }
    }

    return false;
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#isDirty()
   */
  @Override
  public boolean isDirty() {
    return false;
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
   */
  @Override
  public boolean isSaveAsAllowed() {
    return true;
  }

  /**
   * Check wether the News-Table-Part of this Editor is visible or not
   * (minmized).
   *
   * @return TRUE if the News-Table-Part is visible, FALSE otherwise.
   */
  boolean isTableViewerVisible() {
    return fSashForm.getMaximizedControl() == null;
  }

  private boolean isValidNavigation(ITreeNode node, boolean unread) {
    Object data = node.getData();

    /* Require a News */
    if (!(data instanceof INews))
      return false;

    /* Check if News is unread if set as flag */
    INews news = (INews) data;
    if (unread && !CoreUtils.isUnread(news.getState()))
      return false;

    return true;
  }

  /**
   * @return <code>true</code> if this feedview is currently visible or
   * <code>false</code> otherwise.
   */
  public boolean isVisible() {
    return fEditorSite.getPage().isPartVisible(fEditorSite.getPart());
  }

  private boolean justOpened() {
    return System.currentTimeMillis() - fOpenTime < HANDLE_NEWS_SEEN_BLOCK_DELAY;
  }

  private void loadSettings(FeedViewInput input) {

    /* Filter Settings */
    IPreferenceScope preferences = Owl.getPreferenceService().getEntityScope(input.getMark());
    int iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_FILTERING);
    if (iVal >= 0)
      fInitialFilterType = NewsFilter.Type.values()[iVal];
    else
      fInitialFilterType = NewsFilter.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_FILTER_TYPE)];

    /* Group Settings */
    iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_GROUPING);
    if (iVal >= 0)
      fInitialGroupType = NewsGrouping.Type.values()[iVal];
    else
      fInitialGroupType = NewsGrouping.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_GROUP_TYPE)];

    /* Other Settings */
    fInitialBrowserMaximized = fPreferences.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED);
    fInitialLayoutClassic = fPreferences.getBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC);
    fInitialWeights = fPreferences.getIntegers(DefaultPreferences.FV_SASHFORM_WEIGHTS);
    fInitialSearchTarget = NewsFilter.SearchTarget.values()[fPreferences.getInteger(DefaultPreferences.FV_SEARCH_TARGET)];
  }

  /**
   * Navigate to the next/previous read or unread News respecting the News-Items
   * that are displayed in the NewsTableControl.
   *
   * @param respectSelection If <code>TRUE</code>, respect the current selected
   * Item from the Tree as starting-node for the navigation, or
   * <code>FALSE</code> otherwise.
   * @param delay if <code>true</code> delay the navigation if the browser is
   * maximized and <code>false</code> if not.
   * @param next If <code>TRUE</code>, move to the next item, or previous if
   * <code>FALSE</code>.
   * @param unread If <code>TRUE</code>, only move to unread items, or ignore if
   * <code>FALSE</code>.
   * @return Returns <code>TRUE</code> in case navigation found a valid item, or
   * <code>FALSE</code> otherwise.
   */
  public boolean navigate(boolean respectSelection, boolean delay, final boolean next, final boolean unread) {

    /* Check for unread counter */
    if (unread && fInput.getMark().getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)) == 0)
      return false;

    /* Navigate in maximized Browser */
    if (!isTableViewerVisible()) {

      /* Delay navigation because input was just set and browser needs a little to render */
      if (delay) {
        JobRunner.runInUIThread(300, fNewsBrowserControl.getViewer().getControl(), new Runnable() {
          public void run() {
            fNewsBrowserControl.navigate(next, unread);
          }
        });
      }

      /* Directly Navigate */
      else
        fNewsBrowserControl.navigate(next, unread);
      return true;
    }

    Tree newsTree = fNewsTableControl.getViewer().getTree();

    /* Nothing to Navigate to */
    if (newsTree.getItemCount() == 0 || newsTree.isDisposed())
      return false;

    /* Navigate */
    return navigate(newsTree, respectSelection, next, unread);
  }

  private boolean navigate(ITreeNode startingNode, boolean next, final boolean unread) {

    /* Create Traverse-Helper */
    TreeTraversal traverse = new TreeTraversal(startingNode) {
      @Override
      public boolean select(ITreeNode node) {
        return isValidNavigation(node, unread);
      }
    };

    /* Retrieve and select new Target Node */
    ITreeNode targetNode = next ? traverse.nextNode() : traverse.previousNode();
    if (targetNode != null) {
      ISelection selection = new StructuredSelection(targetNode.getData());
      fNewsTableControl.getViewer().setSelection(selection, true);
      return true;
    }

    return false;
  }

  private boolean navigate(Tree tree, boolean respectSelection, boolean next, boolean unread) {

    /* Selection is Present */
    if (respectSelection && tree.getSelectionCount() > 0) {

      /* Try navigating from Selection */
      ITreeNode startingNode = new WidgetTreeNode(tree.getSelection()[0], fNewsTableControl.getViewer());
      if (navigate(startingNode, next, unread))
        return true;
    }

    /* No Selection is Present */
    else {
      ITreeNode startingNode = new WidgetTreeNode(tree, fNewsTableControl.getViewer());
      return navigate(startingNode, true, unread);
    }

    return false;
  }

  /**
   * Notifies this editor about a UI-Event just occured. In dependance of the
   * event, the Editor might want to update the state on the displayed News.
   *
   * @param event The UI-Event that just occured as described in the
   * <code>UIEvent</code> enumeration.
   */
  public void notifyUIEvent(final UIEvent event) {
    final IMark inputMark = fInput.getMark();
    final IStructuredSelection lastSelection = fNewsTableControl.getLastSelection();

    /* Specially Treat Restart Situation */
    if (Controller.getDefault().isRestarting()) {
      if (event == UIEvent.TAB_CLOSE && fInput.exists())
        rememberSelection(inputMark, lastSelection);

      return; // Ignore other events during restart
    }

    /* Specially Treat Closing Situation */
    else if (Controller.getDefault().isShuttingDown()) {
      if (event == UIEvent.TAB_CLOSE && fInput.exists())
        rememberSelection(inputMark, lastSelection);

      if (event != UIEvent.CLOSE)
        return; // Ignore other events than CLOSE that might get issued
    }

    /* Operate on a Copy of the Content Providers News */
    final Collection<INews> news = fContentProvider.getCachedNewsCopy();
    IPreferenceScope inputPreferences = Owl.getPreferenceService().getEntityScope(inputMark);

    /*
     * News can be NULL at this moment, if the Job that is to refresh the cache
     * in the Content Provider was never scheduled. This can happen when quickly
     * navigating between feeds. Also, the input could have been deleted and the
     * editor closed. Thereby do not react.
     */
    if (news == null || !fInput.exists())
      return;

    final boolean markReadOnFeedChange = inputPreferences.getBoolean(DefaultPreferences.MARK_READ_ON_CHANGE);
    final boolean markReadOnTabClose = inputPreferences.getBoolean(DefaultPreferences.MARK_READ_ON_TAB_CLOSE);
    final boolean markReadOnMinimize = inputPreferences.getBoolean(DefaultPreferences.MARK_READ_ON_MINIMIZE);

    /* Mark *new* News as *unread* when closing the entire application */
    if (event == UIEvent.CLOSE) {

      /* Perform the State Change */
      List<INews> newsToUpdate = new ArrayList<INews>();
      for (INews newsItem : news) {
        if (newsItem.getState() == INews.State.NEW)
          newsToUpdate.add(newsItem);
      }

      /* Perform Operation */
      fNewsDao.setState(newsToUpdate, INews.State.UNREAD, true, false);
    }

    /* Handle seen News: Feed Change (also closing the feed view), Closing or Minimize Event */
    else if (event == UIEvent.FEED_CHANGE || event == UIEvent.MINIMIZE || event == UIEvent.TAB_CLOSE) {

      /* Return early if this is a feed change which should be ignored */
      if (event == UIEvent.FEED_CHANGE && fgBlockFeedChangeEvent)
        return;

      /*
       * TODO This is a workaround to avoid potential race-conditions when closing a Tab. The problem
       * is that both FEED_CHANGE (due to hiding the tab) and TAB_CLOSE (due to actually closing
       * the tab) get sent when the user closes a tab. The workaround is to delay the processing of
       * TAB_CLOSE a bit to minimize the chance of a race condition.
       */
      int delay = HANDLE_NEWS_SEEN_DELAY;
      if (event == UIEvent.TAB_CLOSE)
        delay += 100;

      JobRunner.runInBackgroundThread(delay, new Runnable() {
        public void run() {

          /* Application might be in process of closing */
          if (Controller.getDefault().isShuttingDown())
            return;

          /* Check settings if mark as read should be performed */
          boolean markRead = false;
          switch (event) {
            case FEED_CHANGE:
              markRead = markReadOnFeedChange;
              break;

            case TAB_CLOSE:
              markRead = markReadOnTabClose;
              break;

            case MINIMIZE:
              markRead = markReadOnMinimize;
              break;
          }

          /* Perform the State Change */
          List<INews> newsToUpdate = new ArrayList<INews>();
          for (INews newsItem : news) {
            if (newsItem.getState() == INews.State.NEW)
              newsToUpdate.add(newsItem);
            else if (markRead && (newsItem.getState() == INews.State.UPDATED || newsItem.getState() == INews.State.UNREAD))
              newsToUpdate.add(newsItem);
          }

          if (!newsToUpdate.isEmpty()) {

            /* Force quick update on Feed-Change or Tab Close */
            if (event == UIEvent.FEED_CHANGE || event == UIEvent.TAB_CLOSE)
              Controller.getDefault().getSavedSearchService().forceQuickUpdate();

            /* Support Undo */
            //SPM DELETION UndoStack.getInstance().addOperation(new NewsStateOperation(newsToUpdate, markRead ? INews.State.READ : INews.State.UNREAD, true));

            /* Perform Operation */
            fNewsDao.setState(newsToUpdate, INews.State.UNREAD, true, false);
          }

          /* Retention Strategy */
          if (inputMark instanceof IBookMark)
            performCleanUp((IBookMark) inputMark, news);

          /* Also remember the last selected News */
          if (event == UIEvent.TAB_CLOSE)
            rememberSelection(inputMark, lastSelection);
        }
      });
    }
  }

  private void onFoldersDeleted(Set<FolderEvent> events) {
    if (!(fInput.getMark() instanceof FolderNewsMark))
      return;

    FolderNewsMark folderNewsMark = (FolderNewsMark) fInput.getMark();
    for (FolderEvent event : events) {
      final IFolder folder = event.getEntity();
      if (folder.equals(folderNewsMark.getFolder())) {
        fInput.setDeleted();
        JobRunner.runInUIThread(fParent, new Runnable() {
          public void run() {
            fEditorSite.getPage().closeEditor(FeedView.this, false);
          }
        });
        break;
      }
    }
  }

  /* React on the Input being set */
  private void onInputSet() {

    /* Check if an action is to be performed */
    PerformAfterInputSet perform = fInput.getPerformOnInputSet();
    perform(perform);

    /* DB Roundtrips done in the background */
    JobRunner.runInBackgroundThread(new Runnable() {
      public void run() {
        if (fInput == null)
          return;

        IMark mark = fInput.getMark();

        /* Trigger a reload if this is the first time open */
        if (mark instanceof IBookMark) {
          IBookMark bookmark = (IBookMark) mark;
          if (bookmark.getLastVisitDate() == null && !fContentProvider.hasCachedNews())
            new ReloadTypesAction(new StructuredSelection(mark), getEditorSite().getShell()).run();
        }

        /* Trigger reload of not loaded included Bookmarks */
        else if (mark instanceof FolderNewsMark) {
          IFolder folder = ((FolderNewsMark) mark).getFolder();
          List<IBookMark> bookMarksToReload = new ArrayList<IBookMark>();
          fillBookMarksToReload(bookMarksToReload, folder);
          if (!bookMarksToReload.isEmpty())
            new ReloadTypesAction(new StructuredSelection(bookMarksToReload.toArray()), getEditorSite().getShell()).run();
        }

        /* Mark the Searchmark as visited */
        if (mark instanceof ISearchMark) {
          DynamicDAO.getDAO(ISearchMarkDAO.class).visited((ISearchMark) mark);
        }

        /* Mark the newsbin as visited */
        else if (mark instanceof INewsBin) {
          DynamicDAO.getDAO(INewsBinDAO.class).visited((INewsBin) mark);
        }

        /* TODO Fixme once IBookMarkDAO.visited() is implemented */
        else if (!(mark instanceof FolderNewsMark)) {
          mark.setPopularity(mark.getPopularity() + 1);
          mark.setLastVisitDate(new Date());
          DynamicDAO.save(mark);
        }
      }
    });
  }

  private void onNewsFoldersUpdated(final Set<FolderEvent> events) {
    JobRunner.runInUIThread(fParent, new Runnable() {
      public void run() {
        if (!(fInput.getMark() instanceof FolderNewsMark))
          return;

        final IEditorPart activeFeedView = fEditorSite.getPage().getActiveEditor();
        FolderNewsMark folderNewsMark = (FolderNewsMark) fInput.getMark();
        for (FolderEvent event : events) {
          final IFolder folder = event.getEntity();
          if (folder.equals(folderNewsMark.getFolder())) {
            setPartName(folder.getName());
            if (activeFeedView == FeedView.this)
              OwlUI.updateWindowTitle(new IMark[] { fInput.getMark() });

            break;
          }
        }
      }
    });
  }

  private void onNewsMarksDeleted(Set<? extends MarkEvent> events) {
    for (MarkEvent event : events) {
      IMark mark = event.getEntity();
      if (fInput.getMark().getId().equals(mark.getId())) {
        fInput.setDeleted();
        JobRunner.runInUIThread(fParent, new Runnable() {
          public void run() {
            fEditorSite.getPage().closeEditor(FeedView.this, false);
          }
        });
        break;
      }
    }
  }

  private void onNewsMarksUpdated(final Set<? extends MarkEvent> events) {
    JobRunner.runInUIThread(fParent, new Runnable() {
      public void run() {
        final IEditorPart activeFeedView = fEditorSite.getPage().getActiveEditor();
        for (MarkEvent event : events) {
          final IMark mark = event.getEntity();
          if (mark.getId().equals(fInput.getMark().getId())) {
            setPartName(mark.getName());
            if (activeFeedView == FeedView.this)
              OwlUI.updateWindowTitle(new IMark[] { fInput.getMark() });

            break;
          }
        }
      }
    });
  }

  /**
   * @param perform the action to perform on this editor.
   */
  public void perform(PerformAfterInputSet perform) {
    if (perform != null) {

      /* Select first News */
      if (perform.getType() == PerformAfterInputSet.Kind.SELECT_FIRST_NEWS)
        navigate(false, false, true, false);

      /* Select first unread News */
      else if (perform.getType() == PerformAfterInputSet.Kind.SELECT_UNREAD_NEWS)
        navigate(false, true, true, true);

      /* Select specific News */
      else if (perform.getType() == PerformAfterInputSet.Kind.SELECT_SPECIFIC_NEWS)
        setSelection(new StructuredSelection(perform.getNewsToSelect()));

      /* Make sure to activate this FeedView in case of an action */
      if (perform.shouldActivate())
        fEditorSite.getPage().activate(fEditorSite.getPart());
    }
  }

  private void performCleanUp(IBookMark bookmark, Collection<INews> news) {
    if (System.currentTimeMillis() - fLastCleanUpRun.get() > CLEAN_UP_BLOCK_DELAY) {
      RetentionStrategy.process(bookmark, news);
      fLastCleanUpRun.set(System.currentTimeMillis());
    }
  }

  /**
   * Print the contents of the Browser if any.
   */
  public void print() {
    if (fNewsBrowserControl != null)
      fNewsBrowserControl.getViewer().getBrowser().print();
  }

  /**
   * Refreshes all parts of this editor.
   *
   * @param delayRedraw If <code>TRUE</code> delay redraw until operation is
   * done.
   * @param updateLabels If <code>TRUE</code> update all Labels.
   */
  void refresh(boolean delayRedraw, boolean updateLabels) {
    refreshTableViewer(delayRedraw, updateLabels);
    refreshBrowserViewer();
  }

  /* Refresh Browser-Viewer */
  private void refreshBrowserViewer() {

    /* Return on Shutdown */
    if (Controller.getDefault().isShuttingDown())
      return;

    /* Refresh */
    fNewsBrowserControl.getViewer().refresh();
  }

  /* Refresh Table-Viewer if visible */
  void refreshTableViewer(boolean delayRedraw, boolean updateLabels) {

    /* Return on Shutdown */
    if (Controller.getDefault().isShuttingDown())
      return;

    /* Only if Table Viewer is visible */
    if (isTableViewerVisible()) {
      boolean groupingEnabled = fNewsGrouping.getType() != NewsGrouping.Type.NO_GROUPING;

      /* Remember Selection if grouping enabled */
      ISelection selection = StructuredSelection.EMPTY;
      if (groupingEnabled)
        selection = fNewsTableControl.getViewer().getSelection();

      /* Delay redraw operations if requested */
      if (delayRedraw)
        fNewsTableControl.getViewer().getControl().getParent().setRedraw(false);
      try {

        /* Refresh */
        fNewsTableControl.getViewer().refresh(updateLabels);

        /* Expand all Groups if grouping is enabled */
        if (groupingEnabled)
          expandNewsTableViewerGroups(false, selection);
      }

      /* Redraw now if delayed before */
      finally {
        if (delayRedraw)
          fNewsTableControl.getViewer().getControl().getParent().setRedraw(true);
      }
    }
  }

  private void registerListeners() {
    fPartListener = new IPartListener2() {

      public void partActivated(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
      }

      /* Hook into Global Actions for this Editor */
      public void partBroughtToTop(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false))) {
          setGlobalActions();
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);

          /* Notify last visible feedview about change */
          if (fgLastVisibleFeedView != null && fgLastVisibleFeedView != FeedView.this && !fgLastVisibleFeedView.fIsDisposed) {
            fgLastVisibleFeedView.notifyUIEvent(UIEvent.FEED_CHANGE);
            fgLastVisibleFeedView = null;
          }
        }

        /* Any other editor was brought to top, reset last visible feedview */
        else if (!ID.equals(partRef.getId()))
          fgLastVisibleFeedView = null;
      }

      public void partClosed(IWorkbenchPartReference partRef) {
        IEditorReference[] editors = partRef.getPage().getEditorReferences();
        boolean equalsThis = FeedView.this.equals(partRef.getPart(false));
        if (editors.length == 0 && equalsThis)
          OwlUI.updateWindowTitle((String) null);

        if (equalsThis) {
          if (fgLastVisibleFeedView == FeedView.this) //Avoids duplicate UI Event handling
            fgLastVisibleFeedView = null;
          notifyUIEvent(UIEvent.TAB_CLOSE);
        }
      }

      public void partDeactivated(IWorkbenchPartReference partRef) {}

      /* Mark *new* News as *unread* or *read* */
      public void partHidden(IWorkbenchPartReference partRef) {

        /* Return early if event is too close after opening the feed */
        if (justOpened())
          return;

        /* Remember this feedview as being the last visible one */
        if (FeedView.this.equals(partRef.getPart(false)))
          fgLastVisibleFeedView = FeedView.this;
      }

      public void partInputChanged(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
      }

      public void partOpened(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)) && isVisible()) {
          fOpenTime = System.currentTimeMillis();
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
        }
      }

      public void partVisible(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
      }
    };

    fEditorSite.getPage().addPartListener(fPartListener);

    /* React on Bookmark Events */
    fBookMarkListener = new BookMarkAdapter() {
      @Override
      public void entitiesDeleted(Set<BookMarkEvent> events) {
        onNewsMarksDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<BookMarkEvent> events) {
        onNewsMarksUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(IBookMark.class, fBookMarkListener);

    /* React on Folder Events */
    fFolderListener = new FolderAdapter() {
      @Override
      public void entitiesDeleted(Set<FolderEvent> events) {
        onFoldersDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<FolderEvent> events) {
        onNewsFoldersUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(IFolder.class, fFolderListener);

    /* React on Searchmark Events */
    fSearchMarkListener = new SearchMarkAdapter() {
      @Override
      public void entitiesDeleted(Set<SearchMarkEvent> events) {
        onNewsMarksDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<SearchMarkEvent> events) {
        onNewsMarksUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(ISearchMark.class, fSearchMarkListener);

    /* Refresh on Condition Changes if SearchMark showing */
    fSearchConditionListener = new SearchConditionListener() {
      public void entitiesAdded(Set<SearchConditionEvent> events) {
        refreshIfRequired(events);
      }

      public void entitiesDeleted(Set<SearchConditionEvent> events) {
        /* Ignore Due to Bug 1140 (http://dev.rssowl.org/show_bug.cgi?id=1140) */
      }

      public void entitiesUpdated(Set<SearchConditionEvent> events) {
        /* Ignore Due to Bug 1140 (http://dev.rssowl.org/show_bug.cgi?id=1140) */
      }

      /* We rely on the implementation detail that updating a SM means deleting/adding conditions */
      private void refreshIfRequired(Set<SearchConditionEvent> events) {
        if (fInput.getMark() instanceof ISearchMark) {
          ISearchMarkDAO dao = DynamicDAO.getDAO(ISearchMarkDAO.class);
          for (SearchConditionEvent event : events) {
            ISearchCondition condition = event.getEntity();
            ISearchMark searchMark = dao.load(condition);
            if (searchMark != null && searchMark.equals(fInput.getMark())) {
              JobRunner.runUIUpdater(new UIBackgroundJob(fParent) {
                @Override
                public boolean belongsTo(Object family) {
                  return fCacheJobIdentifier.equals(family);
                }

                @Override
                protected void runInBackground(IProgressMonitor monitor) {
                  if (!Controller.getDefault().isShuttingDown())
                    fContentProvider.refreshCache(fInput.getMark(), false);
                }

                @Override
                protected void runInUI(IProgressMonitor monitor) {
                  if (!Controller.getDefault().isShuttingDown())
                    refresh(true, true);
                }
              });

              break;
            }
          }
        }
      }
    };
    DynamicDAO.addEntityListener(ISearchCondition.class, fSearchConditionListener);

    /* React on Newsbin Events */
    fNewsBinListener = new NewsBinAdapter() {
      @Override
      public void entitiesDeleted(Set<NewsBinEvent> events) {
        onNewsMarksDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<NewsBinEvent> events) {
        onNewsMarksUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(INewsBin.class, fNewsBinListener);

    /* Listen if Title Image is changing */
    fFeedListener = new FeedAdapter() {
      @Override
      public void entitiesUpdated(Set<FeedEvent> events) {

        /* Only supported for BookMarks */
        if (!(fInput.getMark() instanceof IBookMark) || events.size() == 0)
          return;

        /* Check if Feed-Event affecting us */
        for (FeedEvent event : events) {
          FeedLinkReference feedRef = ((IBookMark) fInput.getMark()).getFeedLinkReference();
          if (feedRef.references(event.getEntity())) {
            ImageDescriptor imageDesc = fInput.getImageDescriptor();

            /* Title Image Change - Update! */
            if (!fTitleImageDescriptor.equals(imageDesc)) {
              fTitleImageDescriptor = imageDesc;

              JobRunner.runInUIThread(fParent, new Runnable() {
                public void run() {
                  setTitleImage(OwlUI.getImage(fResourceManager, fTitleImageDescriptor));
                }
              });
            }

            break;
          }
        }
      }
    };
    DynamicDAO.addEntityListener(IFeed.class, fFeedListener);

    /* Show Busy when Input is loaded */
    fBookMarkLoadListener = new Controller.BookMarkLoadListener() {
      public void bookMarkAboutToLoad(IBookMark bookmark) {
        if (!fIsDisposed && bookmark.equals(fInput.getMark()))
          showBusyLoading(true);
      }

      public void bookMarkDoneLoading(IBookMark bookmark) {
        if (!fIsDisposed && bookmark.equals(fInput.getMark()))
          showBusyLoading(false);
      }
    };
    Controller.getDefault().addBookMarkLoadListener(fBookMarkLoadListener);
  }

  private void rememberSelection(final IMark inputMark, final IStructuredSelection selection) {
    SafeRunnable.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        IPreferenceScope inputPrefs = Owl.getPreferenceService().getEntityScope(inputMark);
        long oldSelectionValue = inputPrefs.getLong(DefaultPreferences.NM_SELECTED_NEWS);

        /* Find Selected News ID */
        long newSelectionValue = 0;
        if (!selection.isEmpty()) {
          Object obj = selection.getFirstElement();
          if (obj instanceof INews)
            newSelectionValue = ((INews) obj).getId();
        }

        boolean needToSave = false;

        /* Selection Provided */
        if (newSelectionValue > 0) {
          if (oldSelectionValue != newSelectionValue) {
            needToSave = true;
            inputPrefs.putLong(DefaultPreferences.NM_SELECTED_NEWS, newSelectionValue);
          }
        }

        /* No Selection Provided */
        else {
          if (oldSelectionValue > 0) {
            needToSave = true;
            inputPrefs.delete(DefaultPreferences.NM_SELECTED_NEWS);
          }
        }

        IEntity entityToSave;
        if (fInput.getMark() instanceof FolderNewsMark)
          entityToSave = ((FolderNewsMark) fInput.getMark()).getFolder();
        else
          entityToSave = fInput.getMark();

        if (needToSave)
          DynamicDAO.save(entityToSave);
      }
    });
  }

  /* Build Content as String from Feed */
  private void saveAsHtml(String fileName) {
    StringBuilder content = new StringBuilder();
    NewsBrowserLabelProvider labelProvider = (NewsBrowserLabelProvider) fNewsBrowserControl.getViewer().getLabelProvider();

    /* Save from Table */
    if (isTableViewerVisible()) {
      Tree tree = fNewsTableControl.getViewer().getTree();
      TreeItem[] items = tree.getItems();
      if (items.length > 0) {
        List<INews> newsToSave = new ArrayList<INews>();

        /* Ungrouped */
        if (items[0].getItemCount() == 0) {
          for (TreeItem item : items) {
            if (item.getData() instanceof INews)
              newsToSave.add((INews) item.getData());
          }
        }

        /* Grouped */
        else {
          for (TreeItem parentItem : items) {
            TreeItem[] childItems = parentItem.getItems();
            for (TreeItem item : childItems) {
              if (item.getData() instanceof INews)
                newsToSave.add((INews) item.getData());
            }
          }
        }

        /* Create Content for each Item */
        content.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n"); //$NON-NLS-1$
        content.append("<html>\n  <head>\n"); //$NON-NLS-1$
        content.append("\n  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"); //$NON-NLS-1$
        StringWriter css = new StringWriter();
        try {
          labelProvider.writeCSS(css, newsToSave.size() == 1, false);
        } catch (IOException e) {
          /* Ignore */
        }
        content.append(css.toString());
        content.append("  </head>\n  <body>\n"); //$NON-NLS-1$
        for (int i = 0; i < newsToSave.size(); i++) {
          String text = labelProvider.getText(newsToSave.get(i), false, false, i);
          content.append(text);
        }
        content.append("\n  </body>\n</html>"); //$NON-NLS-1$
      }
    }

    /* Save from Browser */
    else {
      content.append(fNewsBrowserControl.getViewer().getBrowser().getControl().getText());
    }

    if (content.length() == 0)
      return;

    /* Write into File */
    CoreUtils.write(fileName, content);
  }

  @SuppressWarnings("restriction")
  private void saveAsXml(final String fileName) {
    final IBookMark bm = (IBookMark) fInput.getMark();
    final URI feedLink = bm.getFeedLinkReference().getLink();
    try {
      final IProtocolHandler handler = Owl.getConnectionService().getHandler(feedLink);
      if (handler instanceof org.rssowl.core.internal.connection.DefaultProtocolHandler) {
        Job downloadJob = new Job(Messages.FeedView_DOWNLOADING_FEED) {
          @Override
          protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask(bm.getName(), IProgressMonitor.UNKNOWN);

            InputStream in = null;
            FileOutputStream out = null;
            boolean canceled = false;
            Exception error = null;
            try {
              byte[] buffer = new byte[8192];

              in = handler.openStream(feedLink, monitor, null);
              out = new FileOutputStream(fileName);
              while (true) {

                /* Check for Cancellation and Shutdown */
                if (monitor.isCanceled() || Controller.getDefault().isShuttingDown()) {
                  canceled = true;
                  return Status.CANCEL_STATUS;
                }

                /* Read from Stream */
                int read = in.read(buffer);
                if (read == -1)
                  break;

                out.write(buffer, 0, read);
              }
            } catch (FileNotFoundException e) {
              error = e;
              Activator.safeLogError(e.getMessage(), e);
            } catch (IOException e) {
              error = e;
              Activator.safeLogError(e.getMessage(), e);
            } catch (ConnectionException e) {
              error = e;
              Activator.safeLogError(e.getMessage(), e);
            } finally {
              monitor.done();

              if (out != null) {
                try {
                  out.close();
                } catch (IOException e) {
                  Activator.safeLogError(e.getMessage(), e);
                }
              }

              if (in != null) {
                try {
                  if ((canceled || error != null) && in instanceof IAbortable)
                    ((IAbortable) in).abort();
                  else
                    in.close();
                } catch (IOException e) {
                  Activator.safeLogError(e.getMessage(), e);
                }
              }
            }

            return Status.OK_STATUS;
          }
        };
        downloadJob.schedule();
      }
    } catch (ConnectionException e) {
      Activator.safeLogError(e.getMessage(), e);
    }
  }

  private void saveSettings() {

    /* Update Settings in DB */
    if (fCacheWeights != null && fCacheWeights[0] != fCacheWeights[1]) {
      int weightDiff = fInitialWeights[0] - fCacheWeights[0];
      if (Math.abs(weightDiff) > 5) {
        int strWeights[] = new int[] { fCacheWeights[0], fCacheWeights[1] };
        fPreferences.putIntegers(DefaultPreferences.FV_SASHFORM_WEIGHTS, strWeights);
      }
    }
  }

  private void setBrowserMaximized(boolean maximized) {
    Control maximizedControl = fSashForm.getMaximizedControl();

    /* Maximize Browser */
    if (maximized && maximizedControl == null) {
      updateSeparators(fInitialLayoutClassic, true);
      fSashForm.setMaximizedControl(fBrowserViewerControlContainer);
      fNewsTableControl.getViewer().setSelection(StructuredSelection.EMPTY);
      fNewsBrowserControl.setPartInput(fInput.getMark());
      fNewsTableControl.setPartInput(null);
      fNewsBrowserControl.setFocus();
    }

    /* Restore Table */
    else if (maximizedControl != null) {
      updateSeparators(fInitialLayoutClassic, false);
      fSashForm.setMaximizedControl(null);
      fNewsTableControl.setPartInput(fInput.getMark());
      fNewsTableControl.adjustScrollPosition();
      expandNewsTableViewerGroups(true, StructuredSelection.EMPTY);
      fNewsBrowserControl.setPartInput(null);
      fNewsTableControl.setFocus();
    }
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {

    /* Focus Headlines */
    if (isTableViewerVisible())
      fNewsTableControl.setFocus();

    /* Focus Browser */
    else {
      Runnable runnable = new Runnable() {
        public void run() {
          fNewsBrowserControl.setFocus();
        }
      };

      /* Run setFocus() delayed if input not yet set */
      Browser browser = fNewsBrowserControl.getViewer().getBrowser().getControl();
      if (!StringUtils.isSet(browser.getUrl()))
        JobRunner.runDelayedInUIThread(browser, runnable);
      else
        runnable.run();
    }
  }

  private void setGlobalActions() {

    /* Define Retargetable Global Actions */
    fEditorSite.getActionBars().setGlobalActionHandler(RetargetActions.RELOAD, fReloadAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), fDeleteAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.CUT.getId(), fCutAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.PASTE.getId(), fPasteAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.PRINT.getId(), fPrintAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.UNDO.getId(), fUndoAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.REDO.getId(), fRedoAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.FIND.getId(), fFindAction);

    /* Disable some Edit-Actions at first */
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(false);
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(false);
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(false);
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
   */
  @Override
  public void setInput(IEditorInput input) {
    Assert.isTrue(input instanceof FeedViewInput);

    /* Handle Old being hidden now */
    if (fInput != null) {
      notifyUIEvent(UIEvent.FEED_CHANGE);
      rememberSelection(fInput.getMark(), fNewsTableControl.getLastSelection());
    }

    /* Set New */
    super.setInput(input);
    fInput = (FeedViewInput) input;

    /* Update UI of Feed-View if new Editor */
    if (!fCreated)
      updateTab(fInput);

    /* Clear Filter Bar */
    if (fFilterBar != null)
      fFilterBar.clearQuickSearch(false);

    /* Editor is being reused */
    if (fCreated) {
      firePropertyChange(PROP_INPUT);

      /* Load Filter Settings for this Mark if present */
      updateFilterAndGrouping(false);

      /* Cancel any previous running Cache-Job */
      Job.getJobManager().cancel(fCacheJobIdentifier);

      /* Re-Create the ContentProvider to avoid concurrency problems with Cache */
      fContentProvider = new NewsContentProvider(fNewsTableControl.getViewer(), fNewsBrowserControl.getViewer(), this);
      fNewsTableControl.getViewer().setContentProvider(fContentProvider);
      fNewsTableControl.onInputChanged(fInput);
      fNewsBrowserControl.getViewer().setContentProvider(fContentProvider);
      fNewsBrowserControl.onInputChanged(fInput);

      /* Reset the Quicksearch if active */
      if (fNewsFilter.isPatternSet())
        fNewsFilter.setPattern(""); //$NON-NLS-1$

      /* Update news mark in filter */
      fNewsFilter.setNewsMark(fInput.getMark());

      /* Apply Input */
      setInput(fInput.getMark(), true);
    }
  }

  /* Set Input to Viewers */
  private void setInput(final INewsMark mark, final boolean reused) {

    /* Update Cache in Background and then apply to UI */
    JobRunner.runUIUpdater(new UIBackgroundJob(fParent) {
      private IProgressMonitor fBgMonitor;

      @Override
      public boolean belongsTo(Object family) {
        return fCacheJobIdentifier.equals(family);
      }

      @Override
      protected void runInBackground(IProgressMonitor monitor) {
        fBgMonitor = monitor;
        if (!monitor.isCanceled())
          fContentProvider.refreshCache(mark, false);
      }

      @Override
      protected void runInUI(IProgressMonitor monitor) {
        IStructuredSelection oldSelection = null;
        IPreferenceScope entityPreferences = Owl.getPreferenceService().getEntityScope(mark);

        long value = entityPreferences.getLong(DefaultPreferences.NM_SELECTED_NEWS);
        if (value > 0) {
          boolean openEmptyNews = entityPreferences.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_EMPTY_NEWS);
          boolean openAllNews = entityPreferences.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS);
          boolean useExternalBrowser = fPreferences.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER) || fPreferences.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER);

          /* Only re-select if this has not the potential of opening in external Browser */
          if (!useExternalBrowser || !openAllNews && !openEmptyNews)
            oldSelection = new StructuredSelection(new NewsReference(value));
        }

        /* Set input to News-Table if Visible */
        if (!fBgMonitor.isCanceled() && isTableViewerVisible())
          stableSetInputToNewsTable(mark, oldSelection);

        /* Clear old Input from Table */
        else if (!fBgMonitor.isCanceled() && reused)
          fNewsTableControl.setPartInput(null);

        /* Set input to News-Browser if visible */
        if (!fBgMonitor.isCanceled() && (!isTableViewerVisible() || BROWSER_SHOWS_ALL /* && oldSelection == null */))
          fNewsBrowserControl.setPartInput(mark);

        /* Reset old Input to Browser if available */
        else if (!fBgMonitor.isCanceled() && oldSelection != null) {
          ISelection selection = fNewsTableControl.getViewer().getSelection();
          if (!selection.isEmpty()) //Could be filtered
            fNewsBrowserControl.setPartInput(oldSelection.getFirstElement());
        }

        /* Clear old Input from Browser */
        else if (!fBgMonitor.isCanceled() && reused)
          fNewsBrowserControl.setPartInput(null);

        /* Update Tab now */
        if (reused)
          updateTab(fInput);

        /* Handle Input being set now */
        onInputSet();
      }
    });
  }

  /**
   * Sets the given <code>IStructuredSelection</code> to the News-Table showing
   * in the FeedView. Will ignore the selection, if the Table is minimized.
   *
   * @param selection The Selection to show in the News-Table.
   */
  public void setSelection(IStructuredSelection selection) {

    /* Return if Table is not visible */
    if (!isTableViewerVisible())
      return;

    /* Remove Filter if selection is hidden */
    if (fNewsFilter.getType() != NewsFilter.Type.SHOW_ALL) {
      boolean unfilter = false;
      List<?> elements = selection.toList();
      for (Object element : elements) {

        /* Resolve the actual News */
        if (element instanceof NewsReference)
          element = ((NewsReference) element).resolve();

        /* This Element is filtered */
        if (!fNewsFilter.select(fNewsTableControl.getViewer(), null, element)) {
          unfilter = true;
          break;
        }
      }

      /* Remove Filter if selection is hidden */
      if (unfilter) {
        fNewsBrowserControl.getViewer().setBlockRefresh(true);
        try {
          fFilterBar.doFilter(NewsFilter.Type.SHOW_ALL, true, false);
        } finally {
          fNewsBrowserControl.getViewer().setBlockRefresh(false);
        }
      }
    }

    /* Apply selection to Table */
    fNewsTableControl.getViewer().setSelection(selection, true);
  }

  private void showBusyLoading(final boolean busy) {
    JobRunner.runInUIThread(fParent, new Runnable() {
      @SuppressWarnings("restriction")
      public void run() {
        if (!fIsDisposed && getSite() instanceof org.eclipse.ui.internal.PartSite)
          ((org.eclipse.ui.internal.PartSite) getSite()).getPane().setBusy(busy);
      }
    });
  }

  /* TODO This is a Workaround until Eclipse Bug #159586 is fixed */
  private void stableSetInputToNewsTable(Object input, ISelection oldSelection) {
    TreeViewer viewer = fNewsTableControl.getViewer();
    Tree tree = (Tree) viewer.getControl();

    /* Set Input & Restore Selection with redraw false */
    tree.getParent().setRedraw(false);
    try {
      fNewsTableControl.setPartInput(input);

      /* Restore selection if required */
      if (oldSelection != null) {
        fNewsTableControl.setBlockNewsStateTracker(true);
        try {
          viewer.setSelection(oldSelection);
        } finally {
          fNewsTableControl.setBlockNewsStateTracker(false);
        }
      }

      /* Adjust Scroll Position */
      fNewsTableControl.adjustScrollPosition();
    } finally {
      tree.getParent().setRedraw(true);
    }
  }

  /**
   * Toggle between maximized and normal Browser-Control.
   */
  void toggleBrowserViewMaximized() {
    final boolean isMaximized = !isTableViewerVisible();
    setBrowserMaximized(!isMaximized);
    fPreferences.putBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED, !isMaximized);
  }

  private void unregisterListeners() {
    fEditorSite.getPage().removePartListener(fPartListener);
    DynamicDAO.removeEntityListener(IBookMark.class, fBookMarkListener);
    DynamicDAO.removeEntityListener(IFolder.class, fFolderListener);
    DynamicDAO.removeEntityListener(ISearchMark.class, fSearchMarkListener);
    DynamicDAO.removeEntityListener(IFeed.class, fFeedListener);
    DynamicDAO.removeEntityListener(ISearchCondition.class, fSearchConditionListener);
    DynamicDAO.removeEntityListener(INewsBin.class, fNewsBinListener);
    Controller.getDefault().removeBookMarkLoadListener(fBookMarkLoadListener);
  }

  /**
   * Update the State of showing Headlines in the Feed View.
   */
  public void updateBrowserViewMaximized() {
    boolean browserMaximized = fPreferences.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED);
    setBrowserMaximized(browserMaximized);
    fFilterBar.updateBrowserViewMaximized(browserMaximized);
  }

  /**
   * Refresh the visible columns of the opened news table control.
   */
  public void updateColumns() {
    if (isTableViewerVisible() && fInput != null)
      fNewsTableControl.updateColumns(fInput.getMark());
    fNewsBrowserControl.updateSorting(fInput.getMark(), true);
  }

  /**
   * Load Filter Settings for the Mark that is set as input if present
   * <p>
   * TODO Find a better solution once its possible to add listeners to
   * {@link IPreferenceScope} and then listen to changes of display-properties.
   * </p>
   *
   * @param refresh If TRUE, refresh the Viewer, FALSE otherwise.
   */
  public void updateFilterAndGrouping(boolean refresh) {
    IPreferenceScope preferences = Owl.getPreferenceService().getEntityScope(fInput.getMark());
    int iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_FILTERING);
    if (iVal >= 0)
      fFilterBar.doFilter(NewsFilter.Type.values()[iVal], refresh, false);
    else
      fFilterBar.doFilter(NewsFilter.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_FILTER_TYPE)], refresh, false);

    /* Load Group Settings for this Mark if present */
    iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_GROUPING);
    if (iVal >= 0)
      fFilterBar.doGrouping(NewsGrouping.Type.values()[iVal], refresh, false);
    else
      fFilterBar.doGrouping(NewsGrouping.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_GROUP_TYPE)], refresh, false);
  }

  /**
   * Update the Layout in the Feed View.
   */
  public void updateLayout() {

    /* Update Browser Maximized State */
    updateBrowserViewMaximized();

    /* Update Classic/Vertical Alignment if necessary */
    if (!fPreferences.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED)) {

      /* Vertical Alignment */
      if (!fPreferences.getBoolean(DefaultPreferences.FV_LAYOUT_CLASSIC)) {
        updateSeparators(false, false);
        fSashForm.setOrientation(SWT.HORIZONTAL);
        fHorizontalTableBrowserSep.getParent().layout();
        fHorizontalBrowserSep.getParent().layout();
      }

      /* Classic Alignment (default) */
      else {
        updateSeparators(true, false);
        fSashForm.setOrientation(SWT.VERTICAL);
        fHorizontalTableBrowserSep.getParent().layout();
        fHorizontalBrowserSep.getParent().layout();
      }
    }
  }

  private void updateSeparators(boolean layoutClassic, boolean browserMaximized) {

    /* Table Separators */
    ((GridData) fVerticalTableBrowserSep.getLayoutData()).exclude = layoutClassic || browserMaximized;
    ((GridData) fHorizontalTableBrowserSep.getLayoutData()).exclude = !layoutClassic || browserMaximized;

    /* Browser Separators */
    ((GridData) fVerticalBrowserSep.getLayoutData()).exclude = layoutClassic || browserMaximized;

    /* Horizontal Layout */
    if (layoutClassic && !browserMaximized) {
      fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, layoutClassic ? 2 : 1, 1));
      ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = false;
    }

    /* Verical Layout */
    else if (!browserMaximized) {
      fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, layoutClassic ? 2 : 1, 1));
      ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = !fBrowserBar.isVisible();
    }

    /* Browser Maximized */
    else {
      fHorizontalBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
      ((GridData) fHorizontalBrowserSep.getLayoutData()).exclude = !fBrowserBar.isVisible();
    }

    /* Update Visibility based on Layout Data */
    fVerticalTableBrowserSep.setVisible(!((GridData) fVerticalTableBrowserSep.getLayoutData()).exclude);
    fHorizontalTableBrowserSep.setVisible(!((GridData) fHorizontalTableBrowserSep.getLayoutData()).exclude);
    fVerticalBrowserSep.setVisible(!((GridData) fVerticalBrowserSep.getLayoutData()).exclude);
    fHorizontalBrowserSep.setVisible(!((GridData) fHorizontalBrowserSep.getLayoutData()).exclude);
  }

  /* Update Title and Image of the FeedView's Tab */
  private void updateTab(FeedViewInput input) {
    setPartName(input.getName());
    fTitleImageDescriptor = input.getImageDescriptor();
    setTitleImage(OwlUI.getImage(fResourceManager, fTitleImageDescriptor));
  }
}