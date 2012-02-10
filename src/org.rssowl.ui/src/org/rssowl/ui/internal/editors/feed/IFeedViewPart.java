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

import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;

/**
 * Instances of <code>IFeedViewPart</code> live inside a FeedView and provide
 * Viewer-Functionality by wrapping around a ContentViewer.
 * 
 * @author bpasero
 */
public interface IFeedViewPart {

  /**
   * Initializes the Part with an instance of <code>IEditorSite</code>.
   * 
   * @param editorSite The primary interface between an editor part and the
   * workbench.
   */
  void init(IEditorSite editorSite);

  /**
   * Asks the Part to create the Viewer and return it.
   * 
   * @param parent The parent composite of the Viewer.
   * @return ContentViewer The newly created Viewer.
   */
  ContentViewer createViewer(Composite parent);

  /**
   * Get the Viewer of this Part.
   * 
   * @return The Viewer of this Part.
   */
  ContentViewer getViewer();

  /**
   * Initializes this Parts Viewer with the shared ContentProvider and Filter.
   * 
   * @param contentProvider The shared News-ContentProvider.
   * @param filter The shared News-Filter.
   */
  void initViewer(IStructuredContentProvider contentProvider, ViewerFilter filter);

  /**
   * Sets an input to this Parts Viewer.
   * 
   * @param input The Input to set into this Parts Viewer.
   */
  void setPartInput(Object input);

  /**
   * Notified the Part about the <code>FeedViewInput</code> that has been set
   * to show.
   * 
   * @param input the <code>FeedViewInput</code> that has been set to show.
   */
  void onInputChanged(FeedViewInput input);

  /**
   * Disposes this Part.
   */
  void dispose();

  /**
   * Sets Focus into this Part.
   */
  void setFocus();
}