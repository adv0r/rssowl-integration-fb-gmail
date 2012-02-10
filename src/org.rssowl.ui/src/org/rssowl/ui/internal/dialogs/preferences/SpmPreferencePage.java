/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2010 RSSOwl Development Team                                  **
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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

public class SpmPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
  public static String ID = "org.rssowl.ui.SpmPreferencePage"; //$NON-NLS-1$

  private IPreferenceScope fGlobalScope;
  private IPreferenceScope fEclipseScope;

  private Button fShowMp3;
  private Button fShowLike;

  public SpmPreferencePage(){
    fGlobalScope = Owl.getPreferenceService().getGlobalScope();
    fEclipseScope = Owl.getPreferenceService().getEclipseScope();
    setImageDescriptor(OwlUI.getImageDescriptor("icons/elcl16/share.gif")); //$NON-NLS-1$

  }

  public SpmPreferencePage(String title){
    super(title);
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

  @Override
  protected Control createContents(Composite parent) {
    Composite container = createComposite(parent);

    /* System Tray Options */
    if (!Application.IS_ECLIPSE)
      createTrayOptions(container);

    applyDialogFont(container);

    /* Enable Apply Button on Selection Changes */
    OwlUI.runOnSelection(new Runnable() {
      public void run() {
        updateApplyEnablement(true);
      }
    }, container);

    return container;
  }

  @Override
  public void createControl(Composite parent) {
    super.createControl(parent);
    updateApplyEnablement(false);
  }

  private void createTrayOptions(Composite container) {
    Label label = new Label(container, SWT.NONE);
    label.setText("Software Process Management project options"); //$NON-NLS-1$
    label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

    /* Group */
    Composite group = new Composite(container, SWT.None);
    group.setLayout(LayoutUtils.createGridLayout(1, 7, 3));
    ((GridLayout) group.getLayout()).marginBottom = 5;
    group.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    label = new Label(group, SWT.NONE);
    label.setText("Enable/Disable features"); //$NON-NLS-1$



    /* Enable / Disable Mp3Player embedding */
    fShowMp3 = new Button(group, SWT.CHECK);
    fShowMp3.setText("Hide mp3 player"); //$NON-NLS-1$
    fShowMp3.setSelection(fGlobalScope.getBoolean(DefaultPreferences.HIDE_MP3));
    fShowMp3.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateRestoreEnablement();
      }
    });


    /* Enable / Disable Facebook Like Button */
    fShowLike = new Button(group, SWT.CHECK);
    fShowLike.setText("Hide Facebook Like Button"); //$NON-NLS-1$
    fShowLike.setSelection(fGlobalScope.getBoolean(DefaultPreferences.HIDE_LIKE));
    fShowLike.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateRestoreEnablement();
      }
    });



    //((GridData) label.getLayoutData()).verticalIndent = 5;

    updateRestoreEnablement();
  }

  public void init(IWorkbench workbench) {}

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performApply()
   */
  @Override
  protected void performApply() {
    super.performApply();
    updateApplyEnablement(false);
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    super.performDefaults();

    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();

    if (!Application.IS_ECLIPSE) {
      fShowMp3.setSelection(defaultScope.getBoolean(DefaultPreferences.HIDE_MP3));
      fShowLike.setSelection(defaultScope.getBoolean(DefaultPreferences.HIDE_LIKE));
      updateRestoreEnablement();
    }
    updateApplyEnablement(true);
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
  public boolean performOk() {

    if (!Application.IS_ECLIPSE) {
      fGlobalScope.putBoolean(DefaultPreferences.HIDE_MP3, fShowMp3.getSelection());
      fGlobalScope.putBoolean(DefaultPreferences.HIDE_LIKE, fShowLike.getSelection());
    }

    /* Update Visible feeds TODO */
    return super.performOk();
  }

  private void updateApplyEnablement(boolean enable) {
    Button applyButton = getApplyButton();
    if (applyButton != null && !applyButton.isDisposed() && applyButton.isEnabled() != enable)
      applyButton.setEnabled(enable);
  }

  private void updateRestoreEnablement() {
    // boolean enable = fShowGmail.getSelection() || fShowFb.getSelection() || fShowLike.getSelection()  || fShowMp3.getSelection();
  }

}
