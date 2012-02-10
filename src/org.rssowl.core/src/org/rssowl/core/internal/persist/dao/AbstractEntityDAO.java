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
package org.rssowl.core.internal.persist.dao;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.SafeRunner;
import org.rssowl.core.internal.persist.service.DBHelper;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.dao.IEntityDAO;
import org.rssowl.core.persist.event.EntityListener;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.event.runnable.EventType;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.LoggingSafeRunnable;

import com.db4o.ext.Db4oException;
import com.db4o.query.Query;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A db4o abstract implementation of EntityDAO.
 *
 * @param <T> Type of concrete implementation of IEntity.
 * @param <L> Type of EntityListener.
 * @param <E> Type of ModelEvent.
 */
public abstract class AbstractEntityDAO<T extends IEntity,
    L extends EntityListener<E, T>, E extends ModelEvent>
    extends AbstractPersistableDAO<T> implements IEntityDAO<T, L, E>{

  /** The List of Listeners for this DAO */
  protected final List<L> fEntityListeners = new CopyOnWriteArrayList<L>();

  /**
   * Creates an instance of this class.
   *
   * @param entityClass
   * @param saveFully
   */
  public AbstractEntityDAO(Class<? extends T> entityClass, boolean saveFully) {
    super(entityClass, saveFully);
  }

  protected abstract E createSaveEventTemplate(T entity);

  protected abstract E createDeleteEventTemplate(T entity);

  public boolean exists(long id) {
    try {
      return !(loadList(id).isEmpty());
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  /*
   * @see org.rssowl.core.model.internal.db4o.dao.PersistableDAO#load(long)
   */
  public T load(long id) {
    try {
      List<T> list = loadList(id);
      if (list.isEmpty())
        return null;
      if (list.size() > 1) {
        String message = "There should only be a single entity for a given id, but there are: " + list.size() + ", id: " + id + ", entities:\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        for (T entity : list)
          message += entity.toString() + "\n"; //$NON-NLS-1$
        throw new IllegalStateException(message);
      }
      T entity = list.get(0);
      // TODO Activate completely by default for now. Must decide how to deal
      // with this.
      fDb.activate(entity, Integer.MAX_VALUE);

      return entity;
    } catch (Db4oException e) {
      throw new PersistenceException(e);
    }
  }

  private List<T> loadList(long id) throws Db4oException   {
    Query query = fDb.query();
    query.constrain(fEntityClass);
    query.descend("fId").constrain(Long.valueOf(id)); //$NON-NLS-1$
    return getList(query);
  }

  @Override
  protected void preSave(T entity) {
    E event = createSaveEventTemplate(entity);
    if (event != null)
      DBHelper.putEventTemplate(event);
  }

  @Override
  protected void preDelete(T entity) {
    E event = createDeleteEventTemplate(entity);
    if (event != null)
      DBHelper.putEventTemplate(event);
  }

  public final void fireEvents(final Set<E> events, final EventType eventType) {
    Assert.isNotNull(eventType, "eventType"); //$NON-NLS-1$
    for (final L listener : fEntityListeners) {
      SafeRunner.run(new LoggingSafeRunnable() {
        public void run() throws Exception {
          switch (eventType) {
            case PERSIST:
              listener.entitiesAdded(events);
              break;
            case UPDATE:
              listener.entitiesUpdated(events);
              break;
            case REMOVE:
              listener.entitiesDeleted(events);
              break;
            default:
              throw new IllegalArgumentException("eventType unknown: " + eventType); //$NON-NLS-1$
          }
        }
      });
    }
  }

  public void addEntityListener(L listener) {
    fEntityListeners.add(listener);
  }

  public void removeEntityListener(L listener) {
    fEntityListeners.remove(listener);
  }

  /* Debugging code copied from old ListenerServiceImpl. Not being used atm */
//  @SuppressWarnings("nls")
//  private void logEvents(Set< ? extends ModelEvent> events, EventType eventType) {
//    if (DEBUG) {
//      String eventTypeString = null;
//      switch (eventType) {
//        case PERSIST:
//          eventTypeString = " Added ("; //$NON-NLS-1$
//          break;
//        case UPDATE:
//          eventTypeString = " Updated ("; //$NON-NLS-1$
//          break;
//        case REMOVE:
//          eventTypeString = " Removed ("; //$NON-NLS-1$
//          break;
//      }
//      IPersistable type = null;
//      ModelEvent event = events.iterator().next();
//      if (eventType != EventType.REMOVE)
//        type = event.getEntity();
//
//      String typeName = type == null ? "" : type.getClass().getSimpleName();
//      String typeString = type == null ? "" : type.toString();
//
//      if (events.size() > 0 && typeName == "")
//        typeName = events.iterator().next().getClass().getSimpleName();
//
//      System.out.println(typeName + eventTypeString + typeString + ", events = " + events.size() + ", isRoot = " + event.isRoot() + ")");
//    }
//  }
}
