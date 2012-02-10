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

package org.rssowl.ui.internal.util;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.FolderNewsMark;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;

/**
 * @author bpasero
 */
public class EditorUtils {

  /**
   * @param editorReferences
   * @param input
   * @return IEditorReference
   */
  public static IEditorReference findEditor(IEditorReference[] editorReferences, Object input) {
    for (IEditorReference reference : editorReferences) {
      try {
        IEditorInput editorInput = reference.getEditorInput();
        if (editorInput instanceof FeedViewInput) {
          FeedViewInput feedViewInput = (FeedViewInput) editorInput;
          Object inputObj = feedViewInput.getMark();
          if (inputObj instanceof FolderNewsMark)
            inputObj = ((FolderNewsMark) inputObj).getFolder();

          if (inputObj.equals(input))
            return reference;
        }
      } catch (PartInitException e) {
        Activator.getDefault().getLog().log(e.getStatus());
      }
    }

    return null;
  }

  /**
   * @return the number of editors able to be visible at the same time.
   */
  public static int getOpenEditorLimit() {
    IPreferenceScope preferences = Owl.getPreferenceService().getEclipseScope();
    boolean isLimited = preferences.getBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS);
    if (!isLimited)
      return Integer.MAX_VALUE;

    return preferences.getInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD);
  }

  /**
   * TODO Find a better solution once its possible to add listeners to
   * {@link IPreferenceScope} and then listen to changes of display-properties.
   */
  public static void updateFilterAndGrouping() {
    IWorkbenchPage page = OwlUI.getPage();
    if (page != null) {
      IEditorReference[] editorReferences = page.getEditorReferences();
      for (IEditorReference reference : editorReferences) {
        try {
          IEditorInput editorInput = reference.getEditorInput();
          if (editorInput instanceof FeedViewInput) {
            FeedView feedView = (FeedView) reference.getEditor(true);
            feedView.updateFilterAndGrouping(true);
          }
        } catch (PartInitException e) {
          Activator.getDefault().getLog().log(e.getStatus());
        }
      }
    }
  }

  /**
   * TODO Find a better solution once its possible to add listeners to
   * {@link IPreferenceScope} and then listen to changes of column-properties.
   */
  public static void updateColumns() {
    IWorkbenchPage page = OwlUI.getPage();
    if (page != null) {
      IEditorReference[] editorReferences = page.getEditorReferences();
      for (IEditorReference reference : editorReferences) {
        try {
          IEditorInput editorInput = reference.getEditorInput();
          if (editorInput instanceof FeedViewInput) {
            FeedView feedView = (FeedView) reference.getEditor(true);
            feedView.updateColumns();
          }
        } catch (PartInitException e) {
          Activator.getDefault().getLog().log(e.getStatus());
        }
      }
    }
  }
}