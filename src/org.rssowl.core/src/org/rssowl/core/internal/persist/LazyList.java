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
package org.rssowl.core.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IEntity;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public final class LazyList<E extends IEntity> implements List<E>   {

  private final LongArrayList fIds;
  private final ObjectContainer fObjectContainer;

  public LazyList(ObjectSet<? extends E> entities, ObjectContainer objectContainer) {
    Assert.isNotNull(entities, "entities"); //$NON-NLS-1$
    Assert.isNotNull(objectContainer, "objectContainer"); //$NON-NLS-1$
    long[] ids = entities.ext().getIDs();
    fIds = new LongArrayList(ids.length);
    fIds.setAll(ids);
    fObjectContainer = objectContainer;
  }

  public Object[] toArray() {
    Object[] array = new Object[size()];
    int index = 0;
    for (E e : this)
      array[index++] = e;

    return array;
  }

  @SuppressWarnings("unchecked")
  public <T> T[] toArray(T[] a) {
    int size = size();
    T[] array = a;
    if (a.length < size)
      array = (T[]) Array.newInstance(a.getClass().getComponentType(), size);

    int index = 0;
    for (E e : this)
      array[index++] = (T) e;

    return array;
  }

  private E getEntity(long id) {
    E object = fObjectContainer.ext().getByID(id);
    fObjectContainer.activate(object, Integer.MAX_VALUE);
    return object;
  }

  public E get(int index) {
    return getEntity(fIds.get(index));
  }

  public int indexOf(Object o) {
    if (o instanceof IEntity) {
      IEntity entity = (IEntity) o;
      if (entity.getId() != null)
        return fIds.indexOf(entity.getId());
    }
    return -1;
  }

  public int lastIndexOf(Object o) {
    if (o instanceof IEntity) {
      IEntity entity = (IEntity) o;
      if (entity.getId() != null)
        return fIds.lastIndexOf(entity.getId());
    }
    return -1;
  }

  public ListIterator<E> listIterator() {
    return listIterator(0);
  }

  public ListIterator<E> listIterator(int index) {
    return new ListIterator<E>() {

      private int cursor;
      private int lastReturned = -1;
      public boolean hasNext() {
        return cursor < fIds.size();
      }

      public boolean hasPrevious() {
        return cursor > 0;
      }

      public E next() {
        E entity = getEntity(fIds.get(cursor));
        lastReturned = cursor++;
        return entity;
      }

      public int nextIndex() {
        return cursor;
      }

      public E previous() {
        E entity = getEntity(fIds.get(--cursor));
        lastReturned = cursor;
        return entity;
      }

      public int previousIndex() {
        return cursor - 1;
      }

      public void remove() {
        if (lastReturned == -1)
          throw new IllegalStateException();

        fIds.removeByIndex(lastReturned);
        if (lastReturned < cursor)
          --cursor;

        lastReturned = -1;
      }

      public void set(E o) {
        throw new UnsupportedOperationException();
      }

      public void add(E o) {
        throw new UnsupportedOperationException();
      }
    };
  }

  public E remove(int index) {
    return getEntity(fIds.removeByIndex(index));
  }

  public Iterator<E> iterator() {
    return listIterator();
  }

  public void clear() {
    fIds.clear();
  }

  public boolean contains(Object o) {
    if (o instanceof IEntity) {
      return fIds.contains(((IEntity) o).getId());
    }
    return false;
  }

  public boolean containsAll(Collection< ? > c) {
    for (Object o : c) {
      if (!contains(o))
        return false;
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;

    if (!(o instanceof Set<?>))
      return false;
    Collection< ? > c = (Collection< ? >) o;
    if (c.size() != size())
      return false;

    return containsAll(c);
  }

  @Override
  public int hashCode() {
    int h = 0;
    for (E e : this) {
      if (e != null)
        h += e.hashCode();
    }
    return h;
  }

  public boolean isEmpty() {
    return fIds.isEmpty();
  }

  public boolean remove(Object o) {
    int index = indexOf(o);
    if (index >= 0) {
      fIds.removeByIndex(index);
      return true;
    }
    return false;
  }

  public boolean removeAll(Collection< ? > c) {
    boolean changed = false;
    for (Object o : c)
      changed |= remove(o);

    return changed;
  }

  public int size() {
    return fIds.size();
  }

  public List<E> subList(int fromIndex, int toIndex) {
    //TODO Implement
    throw new UnsupportedOperationException();
  }

  public boolean retainAll(Collection< ? > c) {
    throw new UnsupportedOperationException();
  }

  public void add(int index, E element) {
    throw new UnsupportedOperationException();
  }

  public boolean addAll(int index, Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  public E set(int index, E element) {
    throw new UnsupportedOperationException();
  }

  public boolean add(E e) {
    throw new UnsupportedOperationException();
  }

  public boolean addAll(Collection< ? extends E> c) {
    throw new UnsupportedOperationException();
  }
}
