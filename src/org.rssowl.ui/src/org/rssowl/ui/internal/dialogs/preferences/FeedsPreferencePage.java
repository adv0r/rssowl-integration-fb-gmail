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

package org.rssowl.ui.internal.dialogs.preferences;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.RetentionStrategy;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.editors.feed.NewsColumnViewModel;
import org.rssowl.ui.internal.editors.feed.NewsFilter;
import org.rssowl.ui.internal.editors.feed.NewsGrouping;
import org.rssowl.ui.internal.services.FeedReloadService;
import org.rssowl.ui.internal.util.EditorUtils;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;
import org.rssowl.ui.internal.util.NewsColumnSelectionControl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Preferences related to Feeds.
 *
 * @author bpasero
 */
public class FeedsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  /** ID of the Page */
  public static String ID = "org.rssowl.ui.FeedsPreferences"; //$NON-NLS-1$

  /* Interval-Scopes in Seconds */
  private static final long DAY_IN_SECONDS = 24 * 60 * 60;
  private static final long HOUR_IN_SECONDS = 60 * 60;
  private static final long MINUTE_IN_SECONDS = 60;

  /* Interval-Indeces in Combo */
  private static final int MINUTES_SCOPE = 0;
  private static final int HOURS_SCOPE = 1;
  private static final int DAYS_SCOPE = 2;

  private IPreferenceScope fGlobalScope;
  private FeedReloadService fReloadService;
  private Button fUpdateCheck;
  private Combo fUpdateScopeCombo;
  private Spinner fUpdateValueSpinner;
  private Button fOpenOnStartupCheck;
  private Button fDeleteNewsByCountCheck;
  private Spinner fDeleteNewsByCountValue;
  private Button fDeleteNewsByAgeCheck;
  private Spinner fDeleteNewsByAgeValue;
  private Button fDeleteReadNewsCheck;
  private Button fNeverDeleteUnReadNewsCheck;
  private Button fNeverDeleteLabeledNewsCheck;
  private Button fReloadOnStartupCheck;
  private Combo fFilterCombo;
  private Combo fGroupCombo;
  private Button fOpenSiteForNewsCheck;
  private Button fOpenSiteForEmptyNewsCheck;
  private Button fLoadImagesForNewsCheck;
  private Button fMarkReadStateCheck;
  private Spinner fMarkReadAfterSpinner;
  private Button fMarkReadOnMinimize;
  private Button fMarkReadOnChange;
  private LocalResourceManager fResources;
  private Button fMarkReadOnTabClose;
  private NewsColumnSelectionControl fColumnSelectionControl;

  /** Leave for reflection */
  public FeedsPreferencePage() {
    fGlobalScope = Owl.getPreferenceService().getGlobalScope();
    fReloadService = Controller.getDefault().getReloadService();
    fResources = new LocalResourceManager(JFaceResources.getResources());
    setImageDescriptor(OwlUI.BOOKMARK);
  }

  /*
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {}

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
    updateApplyEnablement(false);
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    fResources.dispose();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite container = createComposite(parent);

    TabFolder tabFolder = new TabFolder(container, SWT.None);
    tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* General */
    createGeneralGroup(tabFolder);

    /* Reading */
    createReadingGroup(tabFolder);

    /* Display */
    createDisplayGroup(tabFolder);

    /* Columns */
    createColumnsGroup(tabFolder);

    /* Clean-Up */
    createCleanUpGroup(tabFolder);

    /* Info Container */
    Composite infoContainer = new Composite(container, SWT.None);
    infoContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    infoContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));

    Label infoImg = new Label(infoContainer, SWT.NONE);
    infoImg.setImage(OwlUI.getImage(fResources, "icons/obj16/info.gif")); //$NON-NLS-1$
    infoImg.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

    Label infoText = new Label(infoContainer, SWT.WRAP);
    infoText.setText(Messages.FeedsPreferencePage_PROPERTY_INFO);
    infoText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    applyDialogFont(container);

    /* Enable Apply Button on Selection Changes */
    OwlUI.runOnSelection(new Runnable() {
      public void run() {
        updateApplyEnablement(true);
      }
    }, container);

    return container;
  }

  private void createGeneralGroup(TabFolder parent) {
    Composite group = new Composite(parent, SWT.NONE);
    group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    group.setLayout(LayoutUtils.createGridLayout(1, 10, 10));

    TabItem item = new TabItem(parent, SWT.None);
    item.setText(Messages.FeedsPreferencePage_GENERAL);
    item.setControl(group);

    /* Auto-Reload */
    Composite autoReloadContainer = new Composite(group, SWT.NONE);
    autoReloadContainer.setLayout(LayoutUtils.createGridLayout(3, 0, 0));
    autoReloadContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fUpdateCheck = new Button(autoReloadContainer, SWT.CHECK);
    fUpdateCheck.setText(Messages.FeedsPreferencePage_UPDATE_FEEDS);
    fUpdateCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE));
    fUpdateCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fUpdateValueSpinner.setEnabled(fUpdateCheck.getSelection());
        fUpdateScopeCombo.setEnabled(fUpdateCheck.getSelection());
      }
    });

    fUpdateValueSpinner = new Spinner(autoReloadContainer, SWT.BORDER);
    fUpdateValueSpinner.setMinimum(1);
    fUpdateValueSpinner.setMaximum(999);
    fUpdateValueSpinner.setEnabled(fUpdateCheck.getSelection());

    long updateInterval = fGlobalScope.getLong(DefaultPreferences.BM_UPDATE_INTERVAL);
    int updateScope = getUpdateIntervalScope();

    if (updateScope == MINUTES_SCOPE)
      fUpdateValueSpinner.setSelection((int) (updateInterval / MINUTE_IN_SECONDS));
    else if (updateScope == HOURS_SCOPE)
      fUpdateValueSpinner.setSelection((int) (updateInterval / HOUR_IN_SECONDS));
    else if (updateScope == DAYS_SCOPE)
      fUpdateValueSpinner.setSelection((int) (updateInterval / DAY_IN_SECONDS));

    fUpdateScopeCombo = new Combo(autoReloadContainer, SWT.READ_ONLY);
    fUpdateScopeCombo.add(Messages.FeedsPreferencePage_MINUTES);
    fUpdateScopeCombo.add(Messages.FeedsPreferencePage_HOURS);
    fUpdateScopeCombo.add(Messages.FeedsPreferencePage_DAYS);
    fUpdateScopeCombo.select(updateScope);
    fUpdateScopeCombo.setEnabled(fUpdateCheck.getSelection());

    /* Reload Feeds on Startup */
    fReloadOnStartupCheck = new Button(group, SWT.CHECK);
    fReloadOnStartupCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fReloadOnStartupCheck.setText(Messages.FeedsPreferencePage_UPDATE_ON_STARTUP);
    fReloadOnStartupCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.BM_RELOAD_ON_STARTUP));

    /* Open on Startup */
    fOpenOnStartupCheck = new Button(group, SWT.CHECK);
    fOpenOnStartupCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fOpenOnStartupCheck.setText(Messages.FeedsPreferencePage_DISPLAY_ON_STARTUP);
    fOpenOnStartupCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.BM_OPEN_ON_STARTUP));
  }

  private void createReadingGroup(TabFolder parent) {
    Composite group = new Composite(parent, SWT.NONE);
    group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    group.setLayout(LayoutUtils.createGridLayout(1, 10, 10));

    TabItem item = new TabItem(parent, SWT.None);
    item.setText(Messages.FeedsPreferencePage_READING);
    item.setControl(group);

    /* Mark read after millis */
    Composite markReadAfterContainer = new Composite(group, SWT.None);
    markReadAfterContainer.setLayout(LayoutUtils.createGridLayout(3, 0, 0));

    /* Mark Read after Millis */
    fMarkReadStateCheck = new Button(markReadAfterContainer, SWT.CHECK);
    fMarkReadStateCheck.setText(Messages.FeedsPreferencePage_MARK_READ_AFTER);
    fMarkReadStateCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.MARK_READ_STATE));
    fMarkReadStateCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fMarkReadAfterSpinner.setEnabled(fMarkReadStateCheck.getSelection());
      }
    });

    fMarkReadAfterSpinner = new Spinner(markReadAfterContainer, SWT.BORDER);
    fMarkReadAfterSpinner.setMinimum(0);
    fMarkReadAfterSpinner.setMaximum(100);
    fMarkReadAfterSpinner.setSelection(fGlobalScope.getInteger(DefaultPreferences.MARK_READ_IN_MILLIS) / 1000);
    fMarkReadAfterSpinner.setEnabled(fMarkReadStateCheck.getSelection());

    Label label = new Label(markReadAfterContainer, SWT.None);
    label.setText(Messages.FeedsPreferencePage_SECONDS);

    /* Mark Read on changing displayed Feed */
    fMarkReadOnChange = new Button(group, SWT.CHECK);
    fMarkReadOnChange.setText(Messages.FeedsPreferencePage_MARK_READ_ON_SWITCH);
    fMarkReadOnChange.setSelection(fGlobalScope.getBoolean(DefaultPreferences.MARK_READ_ON_CHANGE));

    /* Mark Read on closing the Feed Tab */
    fMarkReadOnTabClose = new Button(group, SWT.CHECK);
    fMarkReadOnTabClose.setText(Messages.FeedsPreferencePage_MARK_READ_ON_CLOSE);
    fMarkReadOnTabClose.setSelection(fGlobalScope.getBoolean(DefaultPreferences.MARK_READ_ON_TAB_CLOSE));

    /* Mark Read on Minimize */
    fMarkReadOnMinimize = new Button(group, SWT.CHECK);
    fMarkReadOnMinimize.setText(Messages.FeedsPreferencePage_MARK_READ_ON_MINIMIZE);
    fMarkReadOnMinimize.setSelection(fGlobalScope.getBoolean(DefaultPreferences.MARK_READ_ON_MINIMIZE));
  }

  private void createDisplayGroup(TabFolder parent) {
    Composite group = new Composite(parent, SWT.NONE);
    group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    group.setLayout(LayoutUtils.createGridLayout(2, 10, 10));

    TabItem item = new TabItem(parent, SWT.None);
    item.setText(Messages.FeedsPreferencePage_DISPLAY);
    item.setControl(group);

    /* Filter Settings */
    Label filterLabel = new Label(group, SWT.None);
    filterLabel.setText(Messages.FeedsPreferencePage_FILTER_NEWS);

    fFilterCombo = new Combo(group, SWT.BORDER | SWT.READ_ONLY);
    fFilterCombo.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));

    NewsFilter.Type[] filters = NewsFilter.Type.values();
    for (NewsFilter.Type filter : filters)
      fFilterCombo.add(filter.getName());

    fFilterCombo.select(ModelUtils.loadIntegerValueWithFallback(fGlobalScope, DefaultPreferences.BM_NEWS_FILTERING, fGlobalScope, DefaultPreferences.FV_FILTER_TYPE));
    fFilterCombo.setVisibleItemCount(fFilterCombo.getItemCount());

    /* Group Settings */
    Label groupLabel = new Label(group, SWT.None);
    groupLabel.setText(Messages.FeedsPreferencePage_GROUP_NEWS);

    fGroupCombo = new Combo(group, SWT.BORDER | SWT.READ_ONLY);
    fGroupCombo.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));

    NewsGrouping.Type[] groups = NewsGrouping.Type.values();
    for (NewsGrouping.Type groupT : groups)
      fGroupCombo.add(groupT.getName());

    fGroupCombo.select(ModelUtils.loadIntegerValueWithFallback(fGlobalScope, DefaultPreferences.BM_NEWS_GROUPING, fGlobalScope, DefaultPreferences.FV_GROUP_TYPE));
    fGroupCombo.setVisibleItemCount(fGroupCombo.getItemCount());

    /* Open Site for News Settings */
    fOpenSiteForNewsCheck = new Button(group, SWT.CHECK);
    fOpenSiteForNewsCheck.setText(Messages.FeedsPreferencePage_OPEN_ON_SELECT);
    fOpenSiteForNewsCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));
    fOpenSiteForNewsCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS));

    /* Open Site for empty News Settings */
    fOpenSiteForEmptyNewsCheck = new Button(group, SWT.CHECK);
    fOpenSiteForEmptyNewsCheck.setText(Messages.FeedsPreferencePage_OPEN_WHEN_EMPTY);
    fOpenSiteForEmptyNewsCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));
    fOpenSiteForEmptyNewsCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_EMPTY_NEWS));

    /* Load Images for News Settings */
    fLoadImagesForNewsCheck = new Button(group, SWT.CHECK);
    fLoadImagesForNewsCheck.setText(Messages.FeedsPreferencePage_DISPLAY_IMAGE_FLASH);
    fLoadImagesForNewsCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));
    fLoadImagesForNewsCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.BM_LOAD_IMAGES));
  }

  private void createColumnsGroup(TabFolder parent) {
    Composite group = new Composite(parent, SWT.NONE);
    group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    group.setLayout(LayoutUtils.createGridLayout(1, 10, 10));

    TabItem item = new TabItem(parent, SWT.None);
    item.setText(Messages.FeedsPreferencePage_COLUMNS);
    item.setControl(group);

    fColumnSelectionControl = new NewsColumnSelectionControl(group, SWT.NONE);
    fColumnSelectionControl.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fColumnSelectionControl.setInput(NewsColumnViewModel.loadFrom(fGlobalScope));
  }

  private void createCleanUpGroup(TabFolder parent) {
    Composite group = new Composite(parent, SWT.NONE);
    group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    group.setLayout(LayoutUtils.createGridLayout(2, 10, 10, 5, 5, false));

    TabItem item = new TabItem(parent, SWT.None);
    item.setText(Messages.FeedsPreferencePage_CLEAN_UP);
    item.setControl(group);

    /* Explanation Label */
    Label explanationLabel = new Label(group, SWT.WRAP);
    explanationLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
    explanationLabel.setText(Messages.FeedsPreferencePage_CLEAN_UP_INFO);

    /* Delete by Count */
    fDeleteNewsByCountCheck = new Button(group, SWT.CHECK);
    fDeleteNewsByCountCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    fDeleteNewsByCountCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_COUNT_STATE));
    fDeleteNewsByCountCheck.setText(Messages.FeedsPreferencePage_MAX_NUMBER);
    fDeleteNewsByCountCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fDeleteNewsByCountValue.setEnabled(fDeleteNewsByCountCheck.getSelection());
      }
    });

    fDeleteNewsByCountValue = new Spinner(group, SWT.BORDER);
    fDeleteNewsByCountValue.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    fDeleteNewsByCountValue.setEnabled(fDeleteNewsByCountCheck.getSelection());
    fDeleteNewsByCountValue.setMinimum(0);
    fDeleteNewsByCountValue.setMaximum(99999);
    fDeleteNewsByCountValue.setSelection(fGlobalScope.getInteger(DefaultPreferences.DEL_NEWS_BY_COUNT_VALUE));

    /* Delete by Age */
    fDeleteNewsByAgeCheck = new Button(group, SWT.CHECK);
    fDeleteNewsByAgeCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    fDeleteNewsByAgeCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_AGE_STATE));
    fDeleteNewsByAgeCheck.setText(Messages.FeedsPreferencePage_MAX_AGE);
    fDeleteNewsByAgeCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fDeleteNewsByAgeValue.setEnabled(fDeleteNewsByAgeCheck.getSelection());
      }
    });

    fDeleteNewsByAgeValue = new Spinner(group, SWT.BORDER);
    fDeleteNewsByAgeValue.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    fDeleteNewsByAgeValue.setEnabled(fDeleteNewsByAgeCheck.getSelection());
    fDeleteNewsByAgeValue.setMinimum(0);
    fDeleteNewsByAgeValue.setMaximum(99999);
    fDeleteNewsByAgeValue.setSelection(fGlobalScope.getInteger(DefaultPreferences.DEL_NEWS_BY_AGE_VALUE));

    /* Delete by State */
    fDeleteReadNewsCheck = new Button(group, SWT.CHECK);
    fDeleteReadNewsCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
    fDeleteReadNewsCheck.setText(Messages.FeedsPreferencePage_DELETE_READ);
    fDeleteReadNewsCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.DEL_READ_NEWS_STATE));

    /* Never Delete Unread State */
    fNeverDeleteUnReadNewsCheck = new Button(group, SWT.CHECK);
    fNeverDeleteUnReadNewsCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
    fNeverDeleteUnReadNewsCheck.setText(Messages.FeedsPreferencePage_NEVER_DELETE_UNREAD);
    fNeverDeleteUnReadNewsCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.NEVER_DEL_UNREAD_NEWS_STATE));

    /* Never Delete Labeled State */
    fNeverDeleteLabeledNewsCheck = new Button(group, SWT.CHECK);
    fNeverDeleteLabeledNewsCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
    fNeverDeleteLabeledNewsCheck.setText(Messages.FeedsPreferencePage_NEVER_DELETE_LABELED);
    fNeverDeleteLabeledNewsCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.NEVER_DEL_LABELED_NEWS_STATE));

    /* Info Container */
    Composite infoContainer = new Composite(group, SWT.None);
    infoContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
    infoContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    ((GridLayout) infoContainer.getLayout()).marginTop = 5;

    Label infoImg = new Label(infoContainer, SWT.NONE);
    infoImg.setImage(OwlUI.getImage(fResources, "icons/obj16/info.gif")); //$NON-NLS-1$
    infoImg.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

    Label infoText = new Label(infoContainer, SWT.WRAP);
    infoText.setText(Messages.FeedsPreferencePage_CLEAN_UP_NOTE);
    infoText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
  }

  private Composite createComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
    composite.setFont(parent.getFont());
    return composite;
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
  public boolean performOk() {

    /* Track Changes */
    boolean autoUpdateChange = false;
    boolean displayChange = false;
    boolean columnChange = false;
    boolean runCleanUp = false;

    /* General */
    long lVal;
    int updateScope = fUpdateScopeCombo.getSelectionIndex();

    if (updateScope == MINUTES_SCOPE)
      lVal = fUpdateValueSpinner.getSelection() * MINUTE_IN_SECONDS;
    else if (updateScope == HOURS_SCOPE)
      lVal = fUpdateValueSpinner.getSelection() * HOUR_IN_SECONDS;
    else
      lVal = fUpdateValueSpinner.getSelection() * DAY_IN_SECONDS;

    if (fGlobalScope.getBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE) != fUpdateCheck.getSelection()) {
      autoUpdateChange = true;
      fGlobalScope.putBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE, fUpdateCheck.getSelection());
    }

    if (fGlobalScope.getLong(DefaultPreferences.BM_UPDATE_INTERVAL) != lVal) {
      autoUpdateChange = true;
      fGlobalScope.putLong(DefaultPreferences.BM_UPDATE_INTERVAL, lVal);
    }

    fGlobalScope.putBoolean(DefaultPreferences.BM_OPEN_ON_STARTUP, fOpenOnStartupCheck.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.BM_RELOAD_ON_STARTUP, fReloadOnStartupCheck.getSelection());

    /* Reading */
    fGlobalScope.putBoolean(DefaultPreferences.MARK_READ_ON_MINIMIZE, fMarkReadOnMinimize.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.MARK_READ_ON_CHANGE, fMarkReadOnChange.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.MARK_READ_ON_TAB_CLOSE, fMarkReadOnTabClose.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.MARK_READ_STATE, fMarkReadStateCheck.getSelection());
    fGlobalScope.putInteger(DefaultPreferences.MARK_READ_IN_MILLIS, fMarkReadAfterSpinner.getSelection() * 1000);

    /* Display */
    if (ModelUtils.loadIntegerValueWithFallback(fGlobalScope, DefaultPreferences.BM_NEWS_FILTERING, fGlobalScope, DefaultPreferences.FV_FILTER_TYPE) != (fFilterCombo.getSelectionIndex())) {
      fGlobalScope.putInteger(DefaultPreferences.BM_NEWS_FILTERING, fFilterCombo.getSelectionIndex());
      displayChange = true;
    }

    if (ModelUtils.loadIntegerValueWithFallback(fGlobalScope, DefaultPreferences.BM_NEWS_GROUPING, fGlobalScope, DefaultPreferences.FV_GROUP_TYPE) != (fGroupCombo.getSelectionIndex())) {
      fGlobalScope.putInteger(DefaultPreferences.BM_NEWS_GROUPING, fGroupCombo.getSelectionIndex());
      displayChange = true;
    }

    if (fGlobalScope.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS) != (fOpenSiteForNewsCheck.getSelection()))
      fGlobalScope.putBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS, fOpenSiteForNewsCheck.getSelection());

    if (fGlobalScope.getBoolean(DefaultPreferences.BM_LOAD_IMAGES) != (fLoadImagesForNewsCheck.getSelection()))
      fGlobalScope.putBoolean(DefaultPreferences.BM_LOAD_IMAGES, fLoadImagesForNewsCheck.getSelection());

    fGlobalScope.putBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_EMPTY_NEWS, fOpenSiteForEmptyNewsCheck.getSelection());

    /* Columns */
    columnChange = fColumnSelectionControl.getModel().saveTo(fGlobalScope);

    /* Clean-Up */
    if (fGlobalScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_COUNT_STATE) != fDeleteNewsByCountCheck.getSelection()) {
      fGlobalScope.putBoolean(DefaultPreferences.DEL_NEWS_BY_COUNT_STATE, fDeleteNewsByCountCheck.getSelection());

      if (fDeleteNewsByCountCheck.getSelection())
        runCleanUp = true;
    }

    if (fGlobalScope.getInteger(DefaultPreferences.DEL_NEWS_BY_COUNT_VALUE) != fDeleteNewsByCountValue.getSelection()) {
      fGlobalScope.putInteger(DefaultPreferences.DEL_NEWS_BY_COUNT_VALUE, fDeleteNewsByCountValue.getSelection());
      runCleanUp = true;
    }

    if (fGlobalScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_AGE_STATE) != fDeleteNewsByAgeCheck.getSelection()) {
      fGlobalScope.putBoolean(DefaultPreferences.DEL_NEWS_BY_AGE_STATE, fDeleteNewsByAgeCheck.getSelection());

      if (fDeleteNewsByAgeCheck.getSelection())
        runCleanUp = true;
    }

    if (fGlobalScope.getInteger(DefaultPreferences.DEL_NEWS_BY_AGE_VALUE) != fDeleteNewsByAgeValue.getSelection()) {
      fGlobalScope.putInteger(DefaultPreferences.DEL_NEWS_BY_AGE_VALUE, fDeleteNewsByAgeValue.getSelection());
      runCleanUp = true;
    }

    if (fGlobalScope.getBoolean(DefaultPreferences.DEL_READ_NEWS_STATE) != fDeleteReadNewsCheck.getSelection()) {
      fGlobalScope.putBoolean(DefaultPreferences.DEL_READ_NEWS_STATE, fDeleteReadNewsCheck.getSelection());
      if (fDeleteReadNewsCheck.getSelection())
        runCleanUp = true;
    }

    fGlobalScope.putBoolean(DefaultPreferences.NEVER_DEL_UNREAD_NEWS_STATE, fNeverDeleteUnReadNewsCheck.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.NEVER_DEL_LABELED_NEWS_STATE, fNeverDeleteLabeledNewsCheck.getSelection());

    /* Run certain tasks now */
    finish(autoUpdateChange, displayChange, columnChange, runCleanUp);

    return super.performOk();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performApply()
   */
  @Override
  protected void performApply() {
    super.performApply();
    updateApplyEnablement(false);
  }

  private void finish(boolean autoUpdateChange, boolean displayChange, boolean columnChange, boolean runCleanup) throws PersistenceException {
    final Collection<IFolder> rootFolders = CoreUtils.loadRootFolders();

    /* Inform Reload Service about update-change */
    if (autoUpdateChange) {
      for (IFolder rootFolder : rootFolders) {
        updateReloadService(rootFolder);
      }
    }

    /* Inform open editors about display-change */
    if (displayChange)
      EditorUtils.updateFilterAndGrouping();

    /* Inform open editors about columns-change */
    if (columnChange)
      EditorUtils.updateColumns();

    /* Peform clean-up on all BookMarks */
    if (runCleanup) {
      Job retentionJob = new Job(Messages.FeedsPreferencePage_PERFORMNG_CLEANUP) {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
          try {
            Set<IBookMark> bookmarks = new HashSet<IBookMark>();
            CoreUtils.fillBookMarks(bookmarks, rootFolders);

            monitor.beginTask(Messages.FeedsPreferencePage_PERFORMNG_CLEANUP, bookmarks.size());

            for (IBookMark bookmark : bookmarks) {
              if (Controller.getDefault().isShuttingDown() || monitor.isCanceled())
                break;

              monitor.subTask(bookmark.getName());
              RetentionStrategy.process(bookmark);
              monitor.worked(1);
            }
          } finally {
            monitor.done();
          }

          return Status.OK_STATUS;
        }
      };

      retentionJob.schedule();
    }
  }

  private void updateReloadService(IFolder folder) {
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof IBookMark)
        fReloadService.sync(((IBookMark) mark));
    }

    List<IFolder> childFolders = folder.getFolders();
    for (IFolder childFolder : childFolders) {
      updateReloadService(childFolder);
    }
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    super.performDefaults();

    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();

    /* General */
    fUpdateCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.BM_UPDATE_INTERVAL_STATE));
    fUpdateValueSpinner.setEnabled(fUpdateCheck.getSelection());
    fUpdateScopeCombo.setEnabled(fUpdateCheck.getSelection());

    long updateInterval = defaultScope.getLong(DefaultPreferences.BM_UPDATE_INTERVAL);
    int updateScope = getUpdateIntervalScope();

    if (updateScope == MINUTES_SCOPE)
      fUpdateValueSpinner.setSelection((int) (updateInterval / MINUTE_IN_SECONDS));
    else if (updateScope == HOURS_SCOPE)
      fUpdateValueSpinner.setSelection((int) (updateInterval / HOUR_IN_SECONDS));
    else if (updateScope == DAYS_SCOPE)
      fUpdateValueSpinner.setSelection((int) (updateInterval / DAY_IN_SECONDS));

    fUpdateScopeCombo.select(updateScope);
    fOpenOnStartupCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.BM_OPEN_ON_STARTUP));
    fReloadOnStartupCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.BM_RELOAD_ON_STARTUP));

    /* Reading */
    fMarkReadOnMinimize.setSelection(defaultScope.getBoolean(DefaultPreferences.MARK_READ_ON_MINIMIZE));
    fMarkReadOnChange.setSelection(defaultScope.getBoolean(DefaultPreferences.MARK_READ_ON_CHANGE));
    fMarkReadOnTabClose.setSelection(defaultScope.getBoolean(DefaultPreferences.MARK_READ_ON_TAB_CLOSE));
    fMarkReadStateCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.MARK_READ_STATE));
    fMarkReadAfterSpinner.setSelection(defaultScope.getInteger(DefaultPreferences.MARK_READ_IN_MILLIS) / 1000);
    fMarkReadAfterSpinner.setEnabled(fMarkReadStateCheck.getSelection());

    /* Display */
    fFilterCombo.select(ModelUtils.loadIntegerValueWithFallback(defaultScope, DefaultPreferences.BM_NEWS_FILTERING, defaultScope, DefaultPreferences.FV_FILTER_TYPE));
    fGroupCombo.select(ModelUtils.loadIntegerValueWithFallback(defaultScope, DefaultPreferences.BM_NEWS_GROUPING, defaultScope, DefaultPreferences.FV_GROUP_TYPE));
    fOpenSiteForNewsCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS));
    fOpenSiteForEmptyNewsCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_EMPTY_NEWS));
    fLoadImagesForNewsCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.BM_LOAD_IMAGES));

    /* Columns */
    fColumnSelectionControl.setInput(NewsColumnViewModel.loadFrom(defaultScope));

    /* Clean-Up */
    fDeleteNewsByCountCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_COUNT_STATE));
    fDeleteNewsByCountValue.setSelection(defaultScope.getInteger(DefaultPreferences.DEL_NEWS_BY_COUNT_VALUE));
    fDeleteNewsByCountValue.setEnabled(fDeleteNewsByCountCheck.getSelection());
    fDeleteNewsByAgeCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_AGE_STATE));
    fDeleteNewsByAgeValue.setSelection(defaultScope.getInteger(DefaultPreferences.DEL_NEWS_BY_AGE_VALUE));
    fDeleteNewsByAgeValue.setEnabled(fDeleteNewsByAgeCheck.getSelection());
    fDeleteReadNewsCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.DEL_READ_NEWS_STATE));
    fNeverDeleteUnReadNewsCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.NEVER_DEL_UNREAD_NEWS_STATE));
    fNeverDeleteLabeledNewsCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.NEVER_DEL_LABELED_NEWS_STATE));

    updateApplyEnablement(true);
  }

  private int getUpdateIntervalScope() {
    long updateInterval = fGlobalScope.getLong(DefaultPreferences.BM_UPDATE_INTERVAL);
    if (updateInterval % DAY_IN_SECONDS == 0)
      return DAYS_SCOPE;

    if (updateInterval % HOUR_IN_SECONDS == 0)
      return HOURS_SCOPE;

    return MINUTES_SCOPE;
  }

  private void updateApplyEnablement(boolean enable) {
    Button applyButton = getApplyButton();
    if (applyButton != null && !applyButton.isDisposed() && applyButton.isEnabled() != enable)
      applyButton.setEnabled(enable);
  }
}