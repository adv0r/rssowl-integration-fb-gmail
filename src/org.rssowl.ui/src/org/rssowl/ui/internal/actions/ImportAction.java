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

package org.rssowl.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.dialogs.CustomWizardDialog;
import org.rssowl.ui.internal.dialogs.importer.ImportWizard;

/**
 * Opens a Wizard to import {@link IFolderChild} with the option to also
 * consider Filters, Labels and Preferences.
 *
 * @author bpasero
 */
public class ImportAction extends Action implements IWorkbenchWindowActionDelegate {

  /** Action ID */
  public static final String ID = "org.rssowl.ui.actions.ImportFeeds"; //$NON-NLS-1$

  /* Section for Dialogs Settings */
  private static final String SETTINGS_SECTION = "org.rssowl.ui.internal.dialogs.importer.ImportWizard"; //$NON-NLS-1$

  private IWorkbenchWindow fWindow;

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {}

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {
    fWindow = window;
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    openWizard(fWindow.getShell());
  }

  /**
   * @param shell the {@link Shell} acting as parent of the wizard.
   */
  public void openWizard(Shell shell) {
    openWizard(shell, null);
  }

  /**
   * @param shell the {@link Shell} acting as parent of the wizard.
   * @param website a link to a website to discover feeds on.
   */
  public void openWizard(Shell shell, String website) {
    openWizard(shell, website, false);
  }

  /**
   * @param shell the {@link Shell} acting as parent of the wizard.
   * @param isKeywordSearch defines if the keyword search should be selected or
   * not.
   */
  public void openWizard(Shell shell, boolean isKeywordSearch) {
    openWizard(shell, null, isKeywordSearch);
  }

  /**
   * @param shell the {@link Shell} acting as parent of the wizard.
   * @param website a link to a website to discover feeds on.
   * @param isKeywordSearch defines if the keyword search should be selected or
   * not.
   */
  public void openWizard(Shell shell, final String website, boolean isKeywordSearch) {
    final ImportWizard importWizard = new ImportWizard(website, isKeywordSearch);
    CustomWizardDialog dialog = new CustomWizardDialog(shell, importWizard) {

      @Override
      protected boolean isResizable() {
        return true;
      }

      @Override
      public int open() {
        if (StringUtils.isSet(website))
          importWizard.getContainer().showPage(importWizard.getNextPage(getCurrentPage()));

        return super.open();
      }

      @Override
      protected IDialogSettings getDialogBoundsSettings() {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(SETTINGS_SECTION);
        if (section != null)
          return section;

        return settings.addNewSection(SETTINGS_SECTION);
      }

      @Override
      protected int getDialogBoundsStrategy() {
        return DIALOG_PERSISTSIZE;
      }
    };
    dialog.setMinimumPageSize(0, 0);
    dialog.create();
    dialog.open();
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {}
}