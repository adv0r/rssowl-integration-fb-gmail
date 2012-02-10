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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.actions.ActionFactory;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.ITask;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.TaskAdapter;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.ContextMenuCreator;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.FolderNewsMark;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.dialogs.SearchMarkDialog;
import org.rssowl.ui.internal.editors.feed.NewsFilter.SearchTarget;
import org.rssowl.ui.internal.editors.feed.NewsFilter.Type;
import org.rssowl.ui.internal.util.EditorUtils;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.JobTracker;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The FilterBar is the central control to filter News that are showing in the
 * FeedView. It supports filtering, grouping and a quick-search.
 *
 * @author bpasero
 */
public class FilterBar {

  /* Action to Filter News */
  private static final String FILTER_ACTION = "org.rssowl.ui.internal.editors.feed.FilterAction"; //$NON-NLS-1$

  /* Action to Group News */
  private static final String GROUP_ACTION = "org.rssowl.ui.internal.editors.feed.GroupAction"; //$NON-NLS-1$

  /* Columns */
  private static final String COLUMNS_ACTION = "org.rssowl.ui.internal.editors.feed.ColumnsAction"; //$NON-NLS-1$

  /* Maximize Browser */
  private static final String TOGGLE_MAXIMIZED_ACTION = "org.rssowl.ui.internal.editors.feed.ToggleMaximizedAction"; //$NON-NLS-1$

  /* Action to Quicksearch */
  private static final String QUICKSEARCH_ACTION = "org.rssowl.ui.internal.editors.feed.QuickSearchAction"; //$NON-NLS-1$

  private Composite fParent;
  private ToolBarManager fSecondToolBarManager;
  private ToolBarManager fFirstToolBarManager;
  private FeedView fFeedView;
  private JobTracker fQuickSearchTracker;
  private Text fSearchInput;
  private IPreferenceScope fGlobalPreferences;
  private boolean fMaximized;
  private ToolBarManager fFilterToolBar;
  private boolean fBlockRefresh;
  private NewsFilter.Type fLastFilterType;
  private NewsGrouping.Type fLastGroupType;

  /**
   * @param feedView
   * @param parent
   */
  public FilterBar(FeedView feedView, Composite parent) {
    fFeedView = feedView;
    fParent = parent;
    fQuickSearchTracker = new JobTracker(500, false, true, ITask.Priority.SHORT);
    fGlobalPreferences = Owl.getPreferenceService().getGlobalScope();
    fMaximized = fGlobalPreferences.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED);
    createControl();
  }

  /**
   * Clear the Quick-Search
   *
   * @param refresh
   */
  public void clearQuickSearch(boolean refresh) {
    if (fSearchInput.getText().length() != 0) {
      fBlockRefresh = !refresh;
      try {
        fSearchInput.setText(""); //$NON-NLS-1$
      } finally {
        fBlockRefresh = false;
      }
    }
  }

  private void createControl() {
    Composite container = new Composite(fParent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(5, 3, 0));
    container.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Filter */
    fFirstToolBarManager = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
    createFilterBar();
    createGrouperBar();
    fFirstToolBarManager.createControl(container);
    fFirstToolBarManager.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    /* Separator */
    Label sep = new Label(container, SWT.SEPARATOR | SWT.VERTICAL);
    sep.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
    ((GridData) sep.getLayoutData()).heightHint = 18;

    fSecondToolBarManager = new ToolBarManager(SWT.FLAT);

    /* Toggle Layout */
    createLayoutBar();

    /* Highlight Searches */
    createHighlightBar();

    fSecondToolBarManager.createControl(container);
    fSecondToolBarManager.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));

    /* Separator */
    sep = new Label(container, SWT.SEPARATOR | SWT.VERTICAL);
    sep.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
    ((GridData) sep.getLayoutData()).heightHint = 18;

    /* Quick Search */
    createQuickSearch(container);
  }

  /* News Filter */
  private void createFilterBar() {
    final NewsFilter filter = fFeedView.getFilter();

    IAction newsFilterAction = new Action(Messages.FilterBar_FILTER_NEWS, IAction.AS_DROP_DOWN_MENU) {
      @Override
      public ImageDescriptor getImageDescriptor() {
        if (filter.getType() == NewsFilter.Type.SHOW_ALL)
          return OwlUI.FILTER;

        return OwlUI.getImageDescriptor("icons/etool16/filter_active.gif"); //$NON-NLS-1$
      }

      @Override
      public String getText() {
        return filter.getType().getDisplayName();
      }

      @Override
      public void run() {

        /* Toggle Show All */
        if (filter.getType() != NewsFilter.Type.SHOW_ALL)
          onFilter(NewsFilter.Type.SHOW_ALL);

        /* Toggle back to previous filter */
        else if (fLastFilterType != null)
          onFilter(fLastFilterType);

        /* Show Menu */
        else
          OwlUI.positionDropDownMenu(this, fFirstToolBarManager);
      }
    };
    newsFilterAction.setId(FILTER_ACTION);

    ActionContributionItem item = new ActionContributionItem(newsFilterAction);
    item.setMode(ActionContributionItem.MODE_FORCE_TEXT);

    fFirstToolBarManager.add(item);

    newsFilterAction.setMenuCreator(new ContextMenuCreator() {

      @Override
      public Menu createMenu(Control parent) {
        Menu menu = new Menu(parent);

        /* Filter: None */
        final MenuItem showAll = new MenuItem(menu, SWT.RADIO);
        showAll.setText(NewsFilter.Type.SHOW_ALL.getName());
        showAll.setSelection(NewsFilter.Type.SHOW_ALL == filter.getType());
        showAll.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showAll.getSelection() && filter.getType() != NewsFilter.Type.SHOW_ALL)
              onFilter(NewsFilter.Type.SHOW_ALL);
          }
        });
        menu.setDefaultItem(showAll);

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Filter: New */
        final MenuItem showNew = new MenuItem(menu, SWT.RADIO);
        //SPM DELETION   showNew.setText(NewsFilter.Type.SHOW_NEW.getName());
        //SPM DELETION   showNew.setSelection(NewsFilter.Type.SHOW_NEW == filter.getType());
        showNew.addSelectionListener(new SelectionAdapter() {

          @Override
          public void widgetSelected(SelectionEvent e) {
            //SPM DELETION     if (showNew.getSelection() && filter.getType() != NewsFilter.Type.SHOW_NEW)
            //SPM DELETION    onFilter(NewsFilter.Type.SHOW_NEW);
          }
        });

        /* Filter: Unread */
        final MenuItem showUnread = new MenuItem(menu, SWT.RADIO);
        //SPM DELETION   showUnread.setText(NewsFilter.Type.SHOW_UNREAD.getName());
        //SPM DELETION   showUnread.setSelection(NewsFilter.Type.SHOW_UNREAD == filter.getType());
        showUnread.addSelectionListener(new SelectionAdapter() {

          @Override
          public void widgetSelected(SelectionEvent e) {
            //SPM DELETION    if (showUnread.getSelection() && filter.getType() != NewsFilter.Type.SHOW_UNREAD)
            //SPM DELETION     onFilter(NewsFilter.Type.SHOW_UNREAD);
          }
        });

        /* Filter: Sticky */
        final MenuItem showSticky = new MenuItem(menu, SWT.RADIO);
        //SPM DELETION  showSticky.setText(NewsFilter.Type.SHOW_STICKY.getName());
        //SPM DELETION  showSticky.setSelection(NewsFilter.Type.SHOW_STICKY == filter.getType());
        showSticky.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            //SPM DELETION      if (showSticky.getSelection() && filter.getType() != NewsFilter.Type.SHOW_STICKY)
            //SPM DELETION      onFilter(NewsFilter.Type.SHOW_STICKY);
          }
        });

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Filter: Recent News */
        final MenuItem showRecent = new MenuItem(menu, SWT.RADIO);
        showRecent.setText(NewsFilter.Type.SHOW_RECENT.getName());
        showRecent.setSelection(NewsFilter.Type.SHOW_RECENT == filter.getType());
        showRecent.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showRecent.getSelection() && filter.getType() != NewsFilter.Type.SHOW_RECENT)
              onFilter(NewsFilter.Type.SHOW_RECENT);
          }
        });

        /* Filter: Last 5 Days */
        final MenuItem showLastFiveDays = new MenuItem(menu, SWT.RADIO);
        showLastFiveDays.setText(NewsFilter.Type.SHOW_LAST_5_DAYS.getName());
        showLastFiveDays.setSelection(NewsFilter.Type.SHOW_LAST_5_DAYS == filter.getType());
        showLastFiveDays.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showLastFiveDays.getSelection() && filter.getType() != NewsFilter.Type.SHOW_LAST_5_DAYS)
              onFilter(NewsFilter.Type.SHOW_LAST_5_DAYS);
          }
        });

        /* Offer to Save as Search */
        INewsMark inputMark = ((FeedViewInput) fFeedView.getEditorInput()).getMark();
        if (inputMark instanceof IBookMark || inputMark instanceof INewsBin || inputMark instanceof FolderNewsMark) {

          /* Separator */
          new MenuItem(menu, SWT.SEPARATOR);

          /* Convert Filter to Saved Search */
          final MenuItem createSavedSearch = new MenuItem(menu, SWT.RADIO);
          createSavedSearch.setText(Messages.FilterBar_SAVE_SEARCH);
          createSavedSearch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
              onCreateSavedSearch(false);
            }
          });
        }

        return menu;
      }
    });
  }

  /* News Group */
  private void createGrouperBar() {
    final NewsGrouping grouping = fFeedView.getGrouper();

    final IAction newsGroup = new Action(Messages.FilterBar_GROUP_NEWS, IAction.AS_DROP_DOWN_MENU) {
      @Override
      public ImageDescriptor getImageDescriptor() {
        if (grouping.getType() == NewsGrouping.Type.NO_GROUPING)
          return OwlUI.getImageDescriptor("icons/etool16/group.gif"); //$NON-NLS-1$

        return OwlUI.getImageDescriptor("icons/etool16/group_active.gif"); //$NON-NLS-1$
      }

      @Override
      public String getText() {
        return grouping.getType().getDisplayName();
      }

      @Override
      public void run() {

        /* Toggle Ungrouped */
        if (fFeedView.getGrouper().getType() != NewsGrouping.Type.NO_GROUPING)
          onGrouping(NewsGrouping.Type.NO_GROUPING);

        /* Toggle back to previous grouping */
        else if (fLastGroupType != null)
          onGrouping(fLastGroupType);

        /* Show Menu */
        else
          OwlUI.positionDropDownMenu(this, fFirstToolBarManager);
      }
    };

    newsGroup.setId(GROUP_ACTION);

    newsGroup.setMenuCreator(new ContextMenuCreator() {

      @Override
      public Menu createMenu(Control parent) {
        Menu menu = new Menu(parent);

        /* Group: None */
        final MenuItem noGrouping = new MenuItem(menu, SWT.RADIO);
        noGrouping.setText(NewsGrouping.Type.NO_GROUPING.getName());
        noGrouping.setSelection(grouping.getType() == NewsGrouping.Type.NO_GROUPING);
        noGrouping.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (noGrouping.getSelection() && grouping.getType() != NewsGrouping.Type.NO_GROUPING)
              onGrouping(NewsGrouping.Type.NO_GROUPING);
          }
        });
        menu.setDefaultItem(noGrouping);

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Group: By Date */
        final MenuItem groupByDate = new MenuItem(menu, SWT.RADIO);
        groupByDate.setText(NewsGrouping.Type.GROUP_BY_DATE.getName());
        groupByDate.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_DATE);
        groupByDate.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByDate.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_DATE)
              onGrouping(NewsGrouping.Type.GROUP_BY_DATE);
          }
        });

        /* Group: By Author */
        final MenuItem groupByAuthor = new MenuItem(menu, SWT.RADIO);
        groupByAuthor.setText(NewsGrouping.Type.GROUP_BY_AUTHOR.getName());
        groupByAuthor.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_AUTHOR);
        groupByAuthor.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByAuthor.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_AUTHOR)
              onGrouping(NewsGrouping.Type.GROUP_BY_AUTHOR);
          }
        });

        /* Group: By Category */
        final MenuItem groupByCategory = new MenuItem(menu, SWT.RADIO);
        groupByCategory.setText(NewsGrouping.Type.GROUP_BY_CATEGORY.getName());
        groupByCategory.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_CATEGORY);
        groupByCategory.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByCategory.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_CATEGORY)
              onGrouping(NewsGrouping.Type.GROUP_BY_CATEGORY);
          }
        });

        /* Group: By Topic */
        final MenuItem groupByTopic = new MenuItem(menu, SWT.RADIO);
        groupByTopic.setText(NewsGrouping.Type.GROUP_BY_TOPIC.getName());
        groupByTopic.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_TOPIC);
        groupByTopic.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByTopic.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_TOPIC)
              onGrouping(NewsGrouping.Type.GROUP_BY_TOPIC);
          }
        });

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Group: By Other */
        final MenuItem groupByOther = new MenuItem(menu, SWT.CASCADE);
        groupByOther.setText(Messages.FilterBar_OTHER);
        Menu otherMenu = new Menu(groupByOther);
        groupByOther.setMenu(otherMenu);

        /* Group: By State */
        final MenuItem groupByState = new MenuItem(otherMenu, SWT.RADIO);
        groupByState.setText(NewsGrouping.Type.GROUP_BY_STATE.getName());
        groupByState.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_STATE);
        groupByState.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByState.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_STATE)
              onGrouping(NewsGrouping.Type.GROUP_BY_STATE);
          }
        });

        /* Group: By Stickyness */
        final MenuItem groupByStickyness = new MenuItem(otherMenu, SWT.RADIO);
        groupByStickyness.setText(NewsGrouping.Type.GROUP_BY_STICKY.getName());
        groupByStickyness.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_STICKY);
        groupByStickyness.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByStickyness.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_STICKY)
              onGrouping(NewsGrouping.Type.GROUP_BY_STICKY);
          }
        });

        /* Separator */
        new MenuItem(otherMenu, SWT.SEPARATOR);

        /* Group: By Label */
        final MenuItem groupByLabel = new MenuItem(otherMenu, SWT.RADIO);
        groupByLabel.setText(NewsGrouping.Type.GROUP_BY_LABEL.getName());
        groupByLabel.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_LABEL);
        groupByLabel.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByLabel.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_LABEL)
              onGrouping(NewsGrouping.Type.GROUP_BY_LABEL);
          }
        });

        /* Group: By Rating */
        final MenuItem groupByRating = new MenuItem(otherMenu, SWT.RADIO);
        groupByRating.setText(NewsGrouping.Type.GROUP_BY_RATING.getName());
        groupByRating.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_RATING);
        groupByRating.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByRating.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_RATING)
              onGrouping(NewsGrouping.Type.GROUP_BY_RATING);
          }
        });

        /* Separator */
        new MenuItem(otherMenu, SWT.SEPARATOR);

        /* Group: By Feed */
        final MenuItem groupByFeed = new MenuItem(otherMenu, SWT.RADIO);
        groupByFeed.setText(NewsGrouping.Type.GROUP_BY_FEED.getName());
        groupByFeed.setSelection(grouping.getType() == NewsGrouping.Type.GROUP_BY_FEED);
        groupByFeed.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByFeed.getSelection() && grouping.getType() != NewsGrouping.Type.GROUP_BY_FEED)
              onGrouping(NewsGrouping.Type.GROUP_BY_FEED);
          }
        });

        return menu;
      }
    });

    ActionContributionItem item = new ActionContributionItem(newsGroup);
    item.setMode(ActionContributionItem.MODE_FORCE_TEXT);

    fFirstToolBarManager.add(item);
  }

  private void createHighlightBar() {
    IAction highlightSearchAction = new Action(Messages.FilterBar_HIGHLIGHT, IAction.AS_CHECK_BOX) {
      @Override
      public void run() {
        fGlobalPreferences.putBoolean(DefaultPreferences.FV_HIGHLIGHT_SEARCH_RESULTS, isChecked());
        fFeedView.getNewsBrowserControl().getViewer().refresh();
      }
    };

    highlightSearchAction.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/highlight.gif")); //$NON-NLS-1$
    highlightSearchAction.setToolTipText(Messages.FilterBar_HIGHLIGHT);
    highlightSearchAction.setChecked(fGlobalPreferences.getBoolean(DefaultPreferences.FV_HIGHLIGHT_SEARCH_RESULTS));

    fSecondToolBarManager.add(highlightSearchAction);
  }

  /* Layout */
  private void createLayoutBar() {
    final ImageDescriptor columnsImgDisabled = OwlUI.getImageDescriptor("icons/dtool16/columns.gif"); //$NON-NLS-1$

    /* Set Columns */
    IAction columnDropdown = new Action(Messages.FilterBar_VISIBLE_COLUMNS, IAction.AS_DROP_DOWN_MENU) {
      @Override
      public ImageDescriptor getDisabledImageDescriptor() {
        return columnsImgDisabled;
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.COLUMNS;
      }

      @Override
      public boolean isEnabled() {
        return !fMaximized;
      }

      @Override
      public void run() {
        OwlUI.positionDropDownMenu(this, fSecondToolBarManager);
      }
    };
    columnDropdown.setId(COLUMNS_ACTION);

    columnDropdown.setMenuCreator(new ContextMenuCreator() {

      @Override
      public Menu createMenu(Control parent) {
        Menu menu = new Menu(parent);

        MenuItem restoreDefaults = new MenuItem(menu, SWT.None);
        restoreDefaults.setText(Messages.FilterBar_RESTORE_DEFAULTS);
        restoreDefaults.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            NewsColumnViewModel defaultModel = NewsColumnViewModel.createDefault(false);
            onColumnsChange(defaultModel);
          }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        NewsColumn[] columns = NewsColumn.values();
        for (final NewsColumn column : columns) {
          if (column.isSelectable()) {
            final NewsColumnViewModel model = getColumnModel();

            MenuItem item = new MenuItem(menu, SWT.CHECK);
            item.setText(column.getName());
            if (model.contains(column))
              item.setSelection(true);

            item.addSelectionListener(new SelectionAdapter() {
              @Override
              public void widgetSelected(SelectionEvent e) {
                if (model.contains(column))
                  model.removeColumn(column);
                else
                  model.addColumn(column);

                onColumnsChange(model);
              }
            });
          }
        }

        return menu;
      }
    });

    fSecondToolBarManager.add(columnDropdown);

    /* Maximize / Minimize Browser */
    final ImageDescriptor img = OwlUI.getImageDescriptor("icons/etool16/browsermaximized.gif"); //$NON-NLS-1$

    IAction toggleMaximized = new Action("", IAction.AS_CHECK_BOX) { //$NON-NLS-1$

      @Override
      public ImageDescriptor getImageDescriptor() {
        return img;
      }

      @Override
      public String getToolTipText() {
        if (fMaximized)
          return Messages.FilterBar_SHOW_HEADLINES;

        return Messages.FilterBar_HIDE_HEADLINES;
      }

      @Override
      public boolean isChecked() {
        return fMaximized;
      }

      @Override
      public void run() {
        fFeedView.toggleBrowserViewMaximized();
        fMaximized = !fMaximized;
        fSecondToolBarManager.find(TOGGLE_MAXIMIZED_ACTION).update();
        fSecondToolBarManager.find(COLUMNS_ACTION).update();
      }
    };
    toggleMaximized.setId(TOGGLE_MAXIMIZED_ACTION);

    fSecondToolBarManager.add(toggleMaximized);
  }

  /* Quick Search */
  private void createQuickSearch(Composite parent) {
    Composite searchContainer = new Composite(parent, SWT.NONE);
    searchContainer.setLayout(LayoutUtils.createGridLayout(Application.IS_MAC ? 2 : 3, 0, 0, 0, 0, false));
    ((GridLayout) searchContainer.getLayout()).marginTop = 1;
    searchContainer.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));
    ((GridData) searchContainer.getLayoutData()).widthHint = 180;

    final ToolBarManager manager = new ToolBarManager(SWT.FLAT);
    final NewsFilter filter = fFeedView.getFilter();

    IAction quickSearch = new Action(Messages.FilterBar_QUICK_SEARCH, IAction.AS_DROP_DOWN_MENU) {
      @Override
      public String getId() {
        return QUICKSEARCH_ACTION;
      }

      @Override
      public void run() {
        OwlUI.positionDropDownMenu(this, manager);
      }
    };
    quickSearch.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/find.gif")); //$NON-NLS-1$

    quickSearch.setMenuCreator(new ContextMenuCreator() {

      @Override
      public Menu createMenu(Control parent) {
        Menu menu = new Menu(parent);

        /* Search on: Subject */
        final MenuItem searchHeadline = new MenuItem(menu, SWT.RADIO);
        searchHeadline.setText(NewsFilter.SearchTarget.HEADLINE.getName());
        searchHeadline.setSelection(NewsFilter.SearchTarget.HEADLINE == filter.getSearchTarget());
        searchHeadline.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchHeadline.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.HEADLINE)
              doSearch(NewsFilter.SearchTarget.HEADLINE);
          }
        });

        /* Search on: Entire News */
        final MenuItem searchEntireNews = new MenuItem(menu, SWT.RADIO);
        searchEntireNews.setText(NewsFilter.SearchTarget.ALL.getName());
        searchEntireNews.setSelection(NewsFilter.SearchTarget.ALL == filter.getSearchTarget());
        searchEntireNews.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchEntireNews.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.ALL)
              doSearch(NewsFilter.SearchTarget.ALL);
          }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        /* Search on: Author */
        final MenuItem searchAuthor = new MenuItem(menu, SWT.RADIO);
        searchAuthor.setText(NewsFilter.SearchTarget.AUTHOR.getName());
        searchAuthor.setSelection(NewsFilter.SearchTarget.AUTHOR == filter.getSearchTarget());
        searchAuthor.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchAuthor.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.AUTHOR)
              doSearch(NewsFilter.SearchTarget.AUTHOR);
          }
        });

        /* Search on: Category */
        final MenuItem searchCategory = new MenuItem(menu, SWT.RADIO);
        searchCategory.setText(NewsFilter.SearchTarget.CATEGORY.getName());
        searchCategory.setSelection(NewsFilter.SearchTarget.CATEGORY == filter.getSearchTarget());
        searchCategory.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchCategory.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.CATEGORY)
              doSearch(NewsFilter.SearchTarget.CATEGORY);
          }
        });

        /* Search on: Source */
        final MenuItem searchSource = new MenuItem(menu, SWT.RADIO);
        searchSource.setText(NewsFilter.SearchTarget.SOURCE.getName());
        searchSource.setSelection(NewsFilter.SearchTarget.SOURCE == filter.getSearchTarget());
        searchSource.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchSource.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.SOURCE)
              doSearch(NewsFilter.SearchTarget.SOURCE);
          }
        });

        /* Search on: Attachments */
        final MenuItem searchAttachments = new MenuItem(menu, SWT.RADIO);
        searchAttachments.setText(NewsFilter.SearchTarget.ATTACHMENTS.getName());
        searchAttachments.setSelection(NewsFilter.SearchTarget.ATTACHMENTS == filter.getSearchTarget());
        searchAttachments.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchAttachments.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.ATTACHMENTS)
              doSearch(NewsFilter.SearchTarget.ATTACHMENTS);
          }
        });

        /* Search on: Labels */
        final MenuItem searchLabels = new MenuItem(menu, SWT.RADIO);
        searchLabels.setText(NewsFilter.SearchTarget.LABELS.getName());
        searchLabels.setSelection(NewsFilter.SearchTarget.LABELS == filter.getSearchTarget());
        searchLabels.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (searchLabels.getSelection() && filter.getSearchTarget() != NewsFilter.SearchTarget.LABELS)
              doSearch(NewsFilter.SearchTarget.LABELS);
          }
        });

        /* Offer to Save as Search */
        INewsMark inputMark = ((FeedViewInput) fFeedView.getEditorInput()).getMark();
        if (inputMark instanceof IBookMark || inputMark instanceof INewsBin || inputMark instanceof FolderNewsMark) {

          /* Separator */
          new MenuItem(menu, SWT.SEPARATOR);

          /* Convert Filter to Saved Search */
          final MenuItem createSavedSearch = new MenuItem(menu, SWT.RADIO);
          createSavedSearch.setText(Messages.FilterBar_SAVE_SEARCH);
          createSavedSearch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
              onCreateSavedSearch(true);
            }
          });
        }

        return menu;
      }
    });

    manager.add(quickSearch);
    manager.createControl(searchContainer);

    /* Input for the Search */
    fSearchInput = new Text(searchContainer, SWT.BORDER | SWT.SINGLE | SWT.SEARCH | SWT.CANCEL);
    fSearchInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fSearchInput.setMessage(fFeedView.getFilter().getSearchTarget().getName());

    /* Register this Input Field to Context Service */
    Controller.getDefault().getContextService().registerInputField(fSearchInput);

    /* Reset any Filter if set on ESC */
    fSearchInput.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.keyCode == SWT.ESC)
          clearQuickSearch(true);
      }
    });

    /* Handle the CR Key Pressed */
    fSearchInput.addTraverseListener(new TraverseListener() {
      public void keyTraversed(TraverseEvent e) {
        if (e.detail == SWT.TRAVERSE_RETURN || e.detail == SWT.TRAVERSE_PAGE_NEXT || e.detail == SWT.TRAVERSE_PAGE_PREVIOUS) {
          e.doit = false;
          fFeedView.handleQuicksearchTraversalEvent(e.detail);
        }
      }
    });

    /* Run search when text is entered */
    fSearchInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {

        /* Clear Search immediately */
        if (fSearchInput.getText().length() == 0 && fFeedView.getFilter().isPatternSet()) {
          fFeedView.getFilter().setPattern(fSearchInput.getText());
          if (!fBlockRefresh) {
            BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
              public void run() {
                fFeedView.refresh(true, false);
              }
            });
          }

          setClearBarVisible(false);
        }

        /* Run Search in JobTracker */
        else if (fSearchInput.getText().length() > 0) {
          fQuickSearchTracker.run(new TaskAdapter() {
            public IStatus run(IProgressMonitor monitor) {
              BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
                public void run() {
                  fFeedView.getFilter().setPattern(fSearchInput.getText());
                  fFeedView.refresh(true, false);
                }
              });
              setClearBarVisible(true);
              return Status.OK_STATUS;
            }
          });
        }
      }
    });

    fSearchInput.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(true);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(true);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(true);
      }

      public void focusLost(FocusEvent e) {
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(false);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(false);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(false);
      }
    });

    /* Clear Button */
    if (!Application.IS_MAC) {
      ToolBar toolBar = new ToolBar(searchContainer, SWT.FLAT | SWT.HORIZONTAL);
      fFilterToolBar = new ToolBarManager(toolBar);
      fFilterToolBar.getControl().setBackground(parent.getBackground());
      fFilterToolBar.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));

      /* Initially Hide */
      ((GridData) toolBar.getLayoutData()).exclude = true;
      toolBar.setVisible(false);

      IAction clearTextAction = new Action("", IAction.AS_PUSH_BUTTON) {//$NON-NLS-1$
        @Override
        public void run() {
          clearQuickSearch(true);
        }
      };

      clearTextAction.setToolTipText(Messages.FilterBar_CLEAR);
      clearTextAction.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/clear.gif")); //$NON-NLS-1$

      fFilterToolBar.add(clearTextAction);

      fFilterToolBar.update(false);
      fFilterToolBar.getControl().setVisible(false);
    }
  }

  void doFilter(final NewsFilter.Type type, boolean refresh, boolean saveSettings) {
    boolean noChange = fFeedView.getFilter().getType() == type;

    if (type != Type.SHOW_ALL)
      fLastFilterType = type;
    else if (fFeedView.getFilter().getType() != Type.SHOW_ALL)
      fLastFilterType = fFeedView.getFilter().getType();

    fFeedView.getFilter().setType(type);
    fFirstToolBarManager.find(FILTER_ACTION).update();

    /* No need to refresh or save settings if nothing changed */
    if (noChange)
      return;

    /* Refresh if set */
    if (refresh) {
      NewsTableControl newsTable = fFeedView.getNewsTableControl();
      boolean isNewsTableVisible = fFeedView.isTableViewerVisible();

      /* Only Refresh Table as Browser shows single News */
      if (newsTable != null && isNewsTableVisible)
        fFeedView.refreshTableViewer(true, false);

      /* Refresh All */
      else
        fFeedView.refresh(true, false);
    }

    /* Update Settings */
    if (saveSettings)
      saveIntegerValue(DefaultPreferences.BM_NEWS_FILTERING, type.ordinal());
  }

  void doGrouping(final NewsGrouping.Type type, boolean refresh, boolean saveSettings) {
    boolean noChange = fFeedView.getGrouper().getType() == type;

    if (type != NewsGrouping.Type.NO_GROUPING)
      fLastGroupType = type;
    else if (fFeedView.getGrouper().getType() != NewsGrouping.Type.NO_GROUPING)
      fLastGroupType = fFeedView.getGrouper().getType();

    fFeedView.getGrouper().setType(type);
    fFirstToolBarManager.find(GROUP_ACTION).update();

    /* No need to refresh or save settings if nothing changed */
    if (noChange)
      return;

    /* Refresh if set */
    if (refresh) {
      NewsTableControl newsTable = fFeedView.getNewsTableControl();
      boolean isNewsTableVisible = fFeedView.isTableViewerVisible();
      try {

        /* Only Refresh Table as Browser shows single News */
        if (newsTable != null && isNewsTableVisible) {
          newsTable.setBlockNewsStateTracker(true);
          fFeedView.refreshTableViewer(true, false);
        }

        /* Refresh All */
        else
          fFeedView.refresh(true, false);
      } finally {
        if (newsTable != null && isNewsTableVisible)
          newsTable.setBlockNewsStateTracker(false);
      }
    }

    /* Update Settings */
    if (saveSettings)
      saveIntegerValue(DefaultPreferences.BM_NEWS_GROUPING, type.ordinal());
  }

  private void doSearch(final NewsFilter.SearchTarget target) {
    fFeedView.getFilter().setSearchTarget(target);
    fSearchInput.setMessage(fFeedView.getFilter().getSearchTarget().getName());
    fSearchInput.setFocus();

    if (fSearchInput.getText().length() > 0)
      fFeedView.refresh(true, false);

    /* Update Settings */
    JobRunner.runInBackgroundThread(new Runnable() {
      public void run() {
        fGlobalPreferences.putInteger(DefaultPreferences.FV_SEARCH_TARGET, target.ordinal());
      }
    });
  }

  /** Give Focus to the Quicksearch Input */
  public void focusQuickSearch() {
    fSearchInput.setFocus();
  }

  private NewsColumnViewModel getColumnModel() {
    FeedViewInput input = (FeedViewInput) fFeedView.getEditorInput();
    return NewsColumnViewModel.loadFrom(Owl.getPreferenceService().getEntityScope(input.getMark()));
  }

  private void onColumnsChange(NewsColumnViewModel newModel) {
    FeedViewInput input = (FeedViewInput) fFeedView.getEditorInput();

    /* Save only into Entity if the Entity was configured with Column Settings before */
    IPreferenceScope entityPrefs = Owl.getPreferenceService().getEntityScope(input.getMark());
    if (entityPrefs.hasKey(DefaultPreferences.BM_NEWS_COLUMNS) || entityPrefs.hasKey(DefaultPreferences.BM_NEWS_SORT_COLUMN) || entityPrefs.hasKey(DefaultPreferences.BM_NEWS_SORT_ASCENDING)) {
      newModel.saveTo(entityPrefs);
      if (input.getMark() instanceof FolderNewsMark)
        DynamicDAO.save(((FolderNewsMark) input.getMark()).getFolder());
      else
        DynamicDAO.save(input.getMark());
    }

    /* Save Globally */
    else
      newModel.saveTo(fGlobalPreferences);

    /* Update Columns of all visible Feedviews */
    EditorUtils.updateColumns();
  }

  private void onCreateSavedSearch(boolean withQuickSearch) {
    IModelFactory factory = Owl.getModelFactory();
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(2);

    /* Create Condition from Location */
    List<IFolderChild> searchScope = new ArrayList<IFolderChild>(1);
    searchScope.add(((FeedViewInput) fFeedView.getEditorInput()).getMark());
    ISearchField field = factory.createSearchField(INews.LOCATION, INews.class.getName());
    conditions.add(factory.createSearchCondition(field, SearchSpecifier.SCOPE, ModelUtils.toPrimitive(searchScope)));

    /* Create Condition from Filter */
    Type filterType = fFeedView.getFilter().getType();
    switch (filterType) {
      case SHOW_ALL:
        if (!withQuickSearch) {
          field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
          conditions.add(factory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "")); //$NON-NLS-1$
        }
        break;

        /* //SPM DELETIONcase SHOW_NEW:
        field = factory.createSearchField(INews.STATE, INews.class.getName());
        conditions.add(factory.createSearchCondition(field, SearchSpecifier.IS, EnumSet.of(INews.State.NEW)));
        break;
         */
      case SHOW_RECENT:
        field = factory.createSearchField(INews.AGE_IN_DAYS, INews.class.getName());
        conditions.add(factory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 2));
        break;

      case SHOW_LAST_5_DAYS:
        field = factory.createSearchField(INews.AGE_IN_DAYS, INews.class.getName());
        conditions.add(factory.createSearchCondition(field, SearchSpecifier.IS_LESS_THAN, 6));
        break;

        /* //SPM DELETION  case SHOW_STICKY:
        field = factory.createSearchField(INews.IS_FLAGGED, INews.class.getName());
        conditions.add(factory.createSearchCondition(field, SearchSpecifier.IS, true));
        break;

      case SHOW_UNREAD:
        field = factory.createSearchField(INews.STATE, INews.class.getName());
        conditions.add(factory.createSearchCondition(field, SearchSpecifier.IS, EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)));
        break; */
    }

    /* Also add Quick Search if required */
    if (withQuickSearch) {
      SearchTarget target = fFeedView.getFilter().getSearchTarget();
      String text = fSearchInput.getText();

      /* Convert to Wildcard Query */
      if (StringUtils.isSet(text) && !text.endsWith("*")) //$NON-NLS-1$
        text = text + "*"; //$NON-NLS-1$

      switch (target) {
        case ALL:
          field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
          conditions.add(factory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, text));
          break;

        case HEADLINE:
          field = factory.createSearchField(INews.TITLE, INews.class.getName());
          conditions.add(factory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, text));
          break;

        case ATTACHMENTS:
          field = factory.createSearchField(INews.ATTACHMENTS_CONTENT, INews.class.getName());
          conditions.add(factory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, text));
          break;

        case AUTHOR:
          field = factory.createSearchField(INews.AUTHOR, INews.class.getName());
          conditions.add(factory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, text));
          break;

        case CATEGORY:
          field = factory.createSearchField(INews.CATEGORIES, INews.class.getName());
          conditions.add(factory.createSearchCondition(field, SearchSpecifier.IS, text));
          break;

        case LABELS:
          field = factory.createSearchField(INews.LABEL, INews.class.getName());
          conditions.add(factory.createSearchCondition(field, SearchSpecifier.IS, text));
          break;

        case SOURCE:
          field = factory.createSearchField(INews.SOURCE, INews.class.getName());
          conditions.add(factory.createSearchCondition(field, SearchSpecifier.IS, text));
          break;
      }
    }

    /* Create and Show SM Dialog */
    SearchMarkDialog dialog = new SearchMarkDialog(fParent.getShell(), OwlUI.getBookMarkExplorerSelection(), null, conditions, true);
    dialog.open();
  }

  private void onFilter(NewsFilter.Type type) {
    doFilter(type, true, true);
    EditorUtils.updateFilterAndGrouping();
  }

  private void onGrouping(NewsGrouping.Type type) {
    doGrouping(type, true, true);
    EditorUtils.updateFilterAndGrouping();
  }

  /*
   * This Method stores an Integer value to either the entity scope or global scope,
   * depending on if the current feed view input has the given setting stored in the
   * entity or not.
   */
  private void saveIntegerValue(String key, int value) {
    FeedViewInput input = (FeedViewInput) fFeedView.getEditorInput();

    /* Save only into Entity if the Entity was configured with the given Settings before */
    IPreferenceScope entityPrefs = Owl.getPreferenceService().getEntityScope(input.getMark());
    if (entityPrefs.hasKey(key)) {
      entityPrefs.putInteger(key, value);
      if (input.getMark() instanceof FolderNewsMark)
        DynamicDAO.save(((FolderNewsMark) input.getMark()).getFolder());
      else
        DynamicDAO.save(input.getMark());
    }

    /* Save Globally */
    else
      fGlobalPreferences.putInteger(key, value);
  }

  void setClearBarVisible(boolean visible) {
    if (fFilterToolBar != null && ((GridData) fFilterToolBar.getControl().getLayoutData()).exclude == visible) {
      ((GridData) fFilterToolBar.getControl().getLayoutData()).exclude = !visible;
      fFilterToolBar.getControl().setVisible(visible);
      fFilterToolBar.getControl().getParent().layout();
    }
  }

  void updateBrowserViewMaximized(boolean maximized) {
    fMaximized = maximized;
    fSecondToolBarManager.find(TOGGLE_MAXIMIZED_ACTION).update();
    fSecondToolBarManager.find(COLUMNS_ACTION).update();
  }
}