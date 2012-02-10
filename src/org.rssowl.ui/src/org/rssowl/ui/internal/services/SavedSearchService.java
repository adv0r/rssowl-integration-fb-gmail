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

package org.rssowl.ui.internal.services;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.ISearchMarkDAO;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.persist.service.IndexListener;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.SearchHit;
import org.rssowl.ui.internal.Controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The <code>SavedSearchService</code> is responsible to listen for updates to
 * the search-index and updating all <code>ISearchMark</code>s as a result to
 * that event in order to reflect changing search results in the UI.
 *
 * @author bpasero
 */
public class SavedSearchService {

  /* Time in millies before updating the saved searches (long) */
  private static final int BATCH_INTERVAL_LONG = 1000;

  /* Time in millies before updating the saved searches (short) */
  private static final int BATCH_INTERVAL_SHORT = 100;

  /* Number of updated documents before using the long batch interval */
  private static final int SHORT_THRESHOLD = 1;

  private final Job fBatchJob;
  private final IndexListener fIndexListener;
  private final AtomicBoolean fBatchInProcess = new AtomicBoolean(false);
  private final AtomicBoolean fUpdatedOnce = new AtomicBoolean(false);
  private final AtomicBoolean fForceQuickUpdate = new AtomicBoolean(false);

  /** Creates and Starts this Service */
  public SavedSearchService() {
    fBatchJob = createBatchJob();
    fIndexListener = registerListeners();
  }

  private Job createBatchJob() {
    Job job = new Job("") { //$NON-NLS-1$
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        fBatchInProcess.set(false);
        fForceQuickUpdate.set(false);

        /* Update all saved searches */
        SafeRunner.run(new LoggingSafeRunnable() {
          public void run() throws Exception {
            if (!Controller.getDefault().isShuttingDown())
              updateSavedSearches(true);
          }
        });

        return Status.OK_STATUS;
      }
    };

    job.setSystem(true);
    job.setUser(false);

    return job;
  }

  private IndexListener registerListeners() {
    IndexListener listener = new IndexListener() {
      public void indexUpdated(int entitiesCount) {
        if (!Controller.getDefault().isShuttingDown())
          onIndexUpdated(entitiesCount);
      }
    };

    Owl.getPersistenceService().getModelSearch().addIndexListener(listener);
    return listener;
  }

  private void unregisterListeners() {
    Owl.getPersistenceService().getModelSearch().removeIndexListener(fIndexListener);
  }

  private void onIndexUpdated(int entitiesCount) {

    /* Start a new Batch if one is not in progress */
    if (!fBatchInProcess.getAndSet(true)) {
      fBatchJob.schedule((entitiesCount <= SHORT_THRESHOLD || fForceQuickUpdate.get()) ? BATCH_INTERVAL_SHORT : BATCH_INTERVAL_LONG);
      return;
    }
  }

  /**
   * Tells this Service to rapidly update all saved searches when the next
   * indexing is done. This can be called after an atomic operation (e.g.
   * Marking some News as read) to force a quick update on all saved searches.
   */
  public void forceQuickUpdate() {
    fForceQuickUpdate.set(true);
  }

  /**
   * Update the results of all <code>ISearchMark</code>s stored in RSSOwl.
   *
   * @param force If set to <code>TRUE</code>, update saved searches even if
   * done before.
   */
  public void updateSavedSearches(boolean force) {
    if (!force && fUpdatedOnce.get())
      return;

    Collection<ISearchMark> searchMarks = DynamicDAO.loadAll(ISearchMark.class);
    updateSavedSearches(searchMarks);
  }

  /**
   * @param searchMarks The Set of <code>ISearchMark</code> to update the
   * results in.
   */
  public void updateSavedSearches(Collection<ISearchMark> searchMarks) {
    updateSavedSearches(searchMarks, false);
  }

  /**
   * @param searchMarks The Set of <code>ISearchMark</code> to update the
   * results in.
   * @param fromUserEvent Indicates whether to update the saved searches due to
   * a user initiated event or an automatic one.
   */
  public void updateSavedSearches(Collection<ISearchMark> searchMarks, boolean fromUserEvent) {
    boolean firstUpdate = !fUpdatedOnce.get();

    fUpdatedOnce.set(true);
    IModelSearch modelSearch = Owl.getPersistenceService().getModelSearch();
    Set<SearchMarkEvent> events = new HashSet<SearchMarkEvent>(searchMarks.size());

    /* For each Search Mark */
    for (ISearchMark searchMark : searchMarks) {

      /* Return early if shutting down */
      if (Controller.getDefault().isShuttingDown())
        return;

      /* Execute the search */
      List<SearchHit<NewsReference>> results = modelSearch.searchNews(searchMark.getSearchConditions(), searchMark.matchAllConditions());

      /* Fill Result into Map Buckets */
      Map<INews.State, List<NewsReference>> resultsMap = new EnumMap<INews.State, List<NewsReference>>(INews.State.class);

      Set<State> visibleStates = INews.State.getVisible();
      for (SearchHit<NewsReference> searchHit : results) {

        /* Return early if shutting down */
        if (Controller.getDefault().isShuttingDown())
          return;

        INews.State state = (State) searchHit.getData(INews.STATE);
        if (visibleStates.contains(state)) {
          List<NewsReference> newsRefs = resultsMap.get(state);
          if (newsRefs == null) {
            newsRefs = new ArrayList<NewsReference>(results.size() / 3);
            resultsMap.put(state, newsRefs);
          }
          newsRefs.add(searchHit.getResult());
        }
      }

      /* Set Result */
      Pair<Boolean, Boolean> result = searchMark.setNewsRefs(resultsMap);
      boolean changed = result.getFirst();
      boolean newNewsAdded = result.getSecond();

      /* Create Event to indicate changed results if any */
      if (changed)
        events.add(new SearchMarkEvent(searchMark, null, true, !firstUpdate && !fromUserEvent && newNewsAdded));
    }

    /* Notify Listeners */
    if (!events.isEmpty() && !Controller.getDefault().isShuttingDown())
      DynamicDAO.getDAO(ISearchMarkDAO.class).fireResultsChanged(events);
  }

  /** Stops this service and unregisters any listeners added. */
  public void stopService() {
    unregisterListeners();
  }
}