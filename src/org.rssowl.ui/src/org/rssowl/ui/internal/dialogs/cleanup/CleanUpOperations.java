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

package org.rssowl.ui.internal.dialogs.cleanup;

/**
 * A container of all supported clean up operations in RSSOwl.
 *
 * @author bpasero
 */
public class CleanUpOperations {

  /* Feed Operations */
  private boolean fLastVisitInDaysState;
  private int fLastVisitInDays;

  private boolean fLastUpdateInDaysState;
  private int fLastUpdateInDays;

  private boolean fDeleteFeedsByConError;
  private final boolean fDeleteFeedsByDuplicates;

  /* News Operations */
  private boolean fMaxNewsCountPerFeedState;
  private int fMaxNewsCountPerFeed;

  private boolean fMaxNewsAgeState;
  private int fMaxNewsAge;

  private boolean fDeleteReadNews;
  private boolean fKeepUnreadNews;
  private boolean fKeepLabeledNews;

  /**
   * @param lastVisitState
   * @param lastVisitInDays
   * @param lastUpdateState
   * @param lastUpdateInDays
   * @param deleteFeedsByConError
   * @param deleteFeedsByDuplicates
   * @param maxNewsCountPerFeedState
   * @param maxNewsCountPerFeed
   * @param maxNewsAgeState
   * @param maxNewsAge
   * @param deleteReadNews
   * @param keepUnreadNews
   * @param keepLabeledNews
   */
  public CleanUpOperations(boolean lastVisitState, int lastVisitInDays, boolean lastUpdateState, int lastUpdateInDays, boolean deleteFeedsByConError, boolean deleteFeedsByDuplicates, boolean maxNewsCountPerFeedState, int maxNewsCountPerFeed, boolean maxNewsAgeState, int maxNewsAge, boolean deleteReadNews, boolean keepUnreadNews, boolean keepLabeledNews) {
    fLastVisitInDaysState = lastVisitState;
    fLastVisitInDays = lastVisitInDays;
    fLastUpdateInDaysState = lastUpdateState;
    fLastUpdateInDays = lastUpdateInDays;
    fDeleteFeedsByConError = deleteFeedsByConError;
    fDeleteFeedsByDuplicates = deleteFeedsByDuplicates;
    fMaxNewsCountPerFeedState = maxNewsCountPerFeedState;
    fMaxNewsCountPerFeed = maxNewsCountPerFeed;
    fMaxNewsAgeState = maxNewsAgeState;
    fMaxNewsAge = maxNewsAge;
    fDeleteReadNews = deleteReadNews;
    fKeepUnreadNews = keepUnreadNews;
    fKeepLabeledNews = keepLabeledNews;
  }

  boolean deleteFeedByLastVisit() {
    return fLastVisitInDaysState;
  }

  int getLastVisitDays() {
    return fLastVisitInDays;
  }

  boolean deleteFeedByLastUpdate() {
    return fLastUpdateInDaysState;
  }

  int getLastUpdateDays() {
    return fLastUpdateInDays;
  }

  boolean deleteFeedsByConError() {
    return fDeleteFeedsByConError;
  }

  boolean deleteFeedsByDuplicates() {
    return fDeleteFeedsByDuplicates;
  }

  boolean deleteNewsByCount() {
    return fMaxNewsCountPerFeedState;
  }

  int getMaxNewsCountPerFeed() {
    return fMaxNewsCountPerFeed;
  }

  boolean deleteNewsByAge() {
    return fMaxNewsAgeState;
  }

  int getMaxNewsAge() {
    return fMaxNewsAge;
  }

  boolean deleteReadNews() {
    return fDeleteReadNews;
  }

  boolean keepUnreadNews() {
    return fKeepUnreadNews;
  }

  boolean keepLabeledNews() {
    return fKeepLabeledNews;
  }
}