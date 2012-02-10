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

package org.rssowl.ui.internal.dialogs.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.dialogs.properties.IEntityPropertyPage;
import org.rssowl.ui.dialogs.properties.IPropertyDialogSite;
import org.rssowl.ui.internal.editors.feed.NewsFilter;
import org.rssowl.ui.internal.editors.feed.NewsGrouping;
import org.rssowl.ui.internal.util.EditorUtils;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Display Properties.
 *
 * @author bpasero
 */
public class DisplayPropertyPage implements IEntityPropertyPage {
  private List<IEntity> fEntities;
  private Combo fFilterCombo;
  private Combo fGroupCombo;
  private Button fOpenSiteForNewsCheck;
  private Button fLoadImagesForNewsCheck;

  /* Settings */
  private List<IPreferenceScope> fEntityPreferences;
  private int fPrefSelectedFilter;
  private int fPrefSelectedGroup;
  private boolean fPrefOpenSiteForNews;
  private boolean fPrefLoadImagesForNews;
  private boolean fSettingsChanged;

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#init(org.rssowl.ui.dialogs.properties.IPropertyDialogSite,
   * java.util.List)
   */
  public void init(IPropertyDialogSite site, List<IEntity> entities) {
    Assert.isTrue(!entities.isEmpty());
    fEntities = entities;

    /* Load Entity Preferences */
    fEntityPreferences = new ArrayList<IPreferenceScope>(fEntities.size());
    for (IEntity entity : entities)
      fEntityPreferences.add(Owl.getPreferenceService().getEntityScope(entity));

    /* Load initial Settings */
    loadInitialSettings();
  }

  private void loadInitialSettings() {
    IPreferenceScope globalScope = Owl.getPreferenceService().getGlobalScope();
    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();

    /* Take the first scope as initial values */
    IPreferenceScope firstScope = fEntityPreferences.get(0);
    fPrefSelectedFilter = ModelUtils.loadIntegerValueWithFallback(firstScope, DefaultPreferences.BM_NEWS_FILTERING, globalScope, DefaultPreferences.FV_FILTER_TYPE);
    fPrefSelectedGroup = ModelUtils.loadIntegerValueWithFallback(firstScope, DefaultPreferences.BM_NEWS_GROUPING, globalScope, DefaultPreferences.FV_GROUP_TYPE);
    fPrefOpenSiteForNews = firstScope.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS);
    fPrefLoadImagesForNews = firstScope.getBoolean(DefaultPreferences.BM_LOAD_IMAGES);

    /* For any other scope not sharing the initial values, use the default */
    for (int i = 1; i < fEntityPreferences.size(); i++) {
      IPreferenceScope otherScope = fEntityPreferences.get(i);

      if (ModelUtils.loadIntegerValueWithFallback(otherScope, DefaultPreferences.BM_NEWS_FILTERING, globalScope, DefaultPreferences.FV_FILTER_TYPE) != fPrefSelectedFilter)
        fPrefSelectedFilter = ModelUtils.loadIntegerValueWithFallback(defaultScope, DefaultPreferences.BM_NEWS_FILTERING, defaultScope, DefaultPreferences.FV_FILTER_TYPE);

      if (ModelUtils.loadIntegerValueWithFallback(otherScope, DefaultPreferences.BM_NEWS_GROUPING, globalScope, DefaultPreferences.FV_GROUP_TYPE) != fPrefSelectedGroup)
        fPrefSelectedGroup = ModelUtils.loadIntegerValueWithFallback(defaultScope, DefaultPreferences.BM_NEWS_GROUPING, defaultScope, DefaultPreferences.FV_GROUP_TYPE);

      if (otherScope.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS) != fPrefOpenSiteForNews)
        fPrefOpenSiteForNews = defaultScope.getBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS);

      if (otherScope.getBoolean(DefaultPreferences.BM_LOAD_IMAGES) != fPrefLoadImagesForNews)
        fPrefLoadImagesForNews = defaultScope.getBoolean(DefaultPreferences.BM_LOAD_IMAGES);
    }
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 10, 10));

    /* Filter Settings */
    Label filterLabel = new Label(container, SWT.None);
    filterLabel.setText(Messages.DisplayPropertyPage_FILTER);

    fFilterCombo = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
    fFilterCombo.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));

    NewsFilter.Type[] filters = NewsFilter.Type.values();
    for (NewsFilter.Type filter : filters)
      fFilterCombo.add(filter.getName());

    fFilterCombo.select(fPrefSelectedFilter);
    fFilterCombo.setVisibleItemCount(fFilterCombo.getItemCount());

    /* Group Settings */
    Label groupLabel = new Label(container, SWT.None);
    groupLabel.setText(Messages.DisplayPropertyPage_GROUP);

    fGroupCombo = new Combo(container, SWT.BORDER | SWT.READ_ONLY);
    fGroupCombo.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));

    NewsGrouping.Type[] groups = NewsGrouping.Type.values();
    for (NewsGrouping.Type group : groups)
      fGroupCombo.add(group.getName());

    fGroupCombo.select(fPrefSelectedGroup);
    fGroupCombo.setVisibleItemCount(fGroupCombo.getItemCount());

    /* Open Site for News Settings */
    fOpenSiteForNewsCheck = new Button(container, SWT.CHECK);
    fOpenSiteForNewsCheck.setText(Messages.DisplayPropertyPage_OPEN_DIRECTLY);
    fOpenSiteForNewsCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));
    fOpenSiteForNewsCheck.setSelection(fPrefOpenSiteForNews);

    /* Load Images for News Settings */
    fLoadImagesForNewsCheck = new Button(container, SWT.CHECK);
    fLoadImagesForNewsCheck.setText(Messages.DisplayPropertyPage_DISPLAY_IMAGE_FLASH);
    fLoadImagesForNewsCheck.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));
    fLoadImagesForNewsCheck.setSelection(fPrefLoadImagesForNews);

    return container;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#getImage()
   */
  public ImageDescriptor getImage() {
    return null;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#setFocus()
   */
  public void setFocus() {}

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#performOk(java.util.Set)
   */
  public boolean performOk(Set<IEntity> entitiesToSave) {
    fSettingsChanged = false;

    /* Update this Entity */
    for (IPreferenceScope scope : fEntityPreferences) {
      if (updatePreferences(scope)) {
        IEntity entityToSave = fEntities.get(fEntityPreferences.indexOf(scope));
        entitiesToSave.add(entityToSave);
        fSettingsChanged = true;
      }
    }

    /* Update changes in all Childs as well if Folder */
    for (IEntity entity : fEntities) {
      if (fSettingsChanged && entity instanceof IFolder)
        updateChildPreferences((IFolder) entity);
    }

    return true;
  }

  private void updateChildPreferences(IFolder folder) {

    /* Update changes to Child-Marks */
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      IPreferenceScope scope = Owl.getPreferenceService().getEntityScope(mark);
      updatePreferences(scope);
    }

    /* Update changes to Child-Folders */
    List<IFolder> folders = folder.getFolders();
    for (IFolder childFolder : folders) {
      IPreferenceScope scope = Owl.getPreferenceService().getEntityScope(childFolder);
      updatePreferences(scope);

      /* Recursively Proceed */
      updateChildPreferences(childFolder);
    }
  }

  private boolean updatePreferences(IPreferenceScope scope) {
    boolean changed = false;

    /* Filter */
    int iVal = fFilterCombo.getSelectionIndex();
    if (fPrefSelectedFilter != iVal) {
      scope.putInteger(DefaultPreferences.BM_NEWS_FILTERING, iVal);
      changed = true;
    }

    /* Grouping */
    iVal = fGroupCombo.getSelectionIndex();
    if (fPrefSelectedGroup != iVal) {
      scope.putInteger(DefaultPreferences.BM_NEWS_GROUPING, iVal);
      changed = true;
    }

    /* Open Site for News */
    boolean bVal = fOpenSiteForNewsCheck.getSelection();
    if (fPrefOpenSiteForNews != bVal) {
      scope.putBoolean(DefaultPreferences.BM_OPEN_SITE_FOR_NEWS, bVal);
      changed = true;
    }

    /* Load Images for News */
    bVal = fLoadImagesForNewsCheck.getSelection();
    if (fPrefLoadImagesForNews != bVal) {
      scope.putBoolean(DefaultPreferences.BM_LOAD_IMAGES, bVal);
      changed = true;
    }

    return changed;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#finish()
   */
  public void finish() {

    /* Propagate change to open Editors */
    if (fSettingsChanged)
      EditorUtils.updateFilterAndGrouping();
  }
}