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

import org.rssowl.core.util.ArrayUtils;

public abstract class AbstractLongArrayList {

  protected long[] fElements;
  protected int fSize;

  /**
   * Provided for deserialization.
   */
  protected AbstractLongArrayList() {
    super();
  }

  public AbstractLongArrayList(int initialCapacity) {
    this.fElements = new long[initialCapacity];
  }

  public final int size() {
    return fSize;
  }

  public final long get(int index) {
    rangeCheck(index);
    return fElements[index];
  }

  public abstract boolean removeByElement(long element);

  protected final void remove(int index) {
    int movedCount = fSize - 1 - index;

    if (movedCount > 0)
      System.arraycopy(fElements, index + 1, fElements, index, movedCount);

    fElements[--fSize] = 0L;
  }

  public long removeByIndex(int index) {
    long oldValue = get(index);
    remove(index);
    return oldValue;
  }

  private void rangeCheck(int index) {
    if (index >= fSize)
      throw new IndexOutOfBoundsException("size: " + fSize + ", index: " + index); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public void add(long element) {
    fElements = ArrayUtils.ensureCapacity(fElements, fSize + 1);
    fElements[fSize++] = element;
  }

  public void compact() {
    long[] compacted = new long[fSize];
    System.arraycopy(fElements, 0, compacted, 0, fSize);
    fElements = compacted;
  }

  public void clear() {
    fSize = 0;
    fElements = new long[0];
  }
}
