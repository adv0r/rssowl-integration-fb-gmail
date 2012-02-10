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
package org.rssowl.core.persist.event.runnable;

import org.rssowl.core.persist.event.SearchConditionEvent;

/**
 * Provides a way to fire a SearchConditionEventRunnable in the future.
 * 
 * @see EventRunnable
 * @author Ismael Juma (ismael@juma.me.uk)
 */
public class SearchConditionEventRunnable extends 
    EventRunnable<SearchConditionEvent> {

  /**
   * Creates a new instance of this object.
   */
  public SearchConditionEventRunnable() {
    super(SearchConditionEvent.class, getDAOService().getSearchConditionDAO());
  }
}
