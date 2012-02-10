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

public class LongHashSet {

  private static final long serialVersionUID = 1L;

  private static final float LOAD_FACTOR = 0.50f;

  private static final int DEFAULT_EXPECTED_SIZE = 16;

  private static final int RESIZE_MULTIPLIER = 2;

  private static final int PERTURB_SHIFT = 5;

  private int size;

  private int mask;

  private long[] elements;

  private byte[] states;

  private static final byte FREE = 0;

  private static final byte FULL = 1;

  private static final byte REMOVED = 2;

  public LongHashSet() {
    this(DEFAULT_EXPECTED_SIZE);
  }

  public LongHashSet(int expectedSize) {
    int capacity = computeCapacity(expectedSize);
    elements = new long[capacity];
    states = new byte[capacity];
    mask = capacity - 1;
  }

  public LongHashSet(LongHashSet source) {
    int length = source.elements.length;
    elements = new long[length];
    System.arraycopy(source.elements, 0, elements, 0, length);
    states = new byte[length];
    System.arraycopy(source.states, 0, states, 0, length);
    mask = source.mask;
    size = source.size;
  }

  private static int computeCapacity(int expectedSize) {
    if (expectedSize == 0)
      return 1;
    int capacity = (int) Math.ceil(expectedSize / LOAD_FACTOR);
    int powerOfTwo = Integer.highestOneBit(capacity);
    if (powerOfTwo == capacity)
      return capacity;
    return nextPowerOfTwo(capacity);
  }

  private static int nextPowerOfTwo(int i) {
    return Integer.highestOneBit(i) << 1;
  }

  public boolean contains(long element) {
    int hash = hashOf(element);
    int index = hash & mask;
    if (containsElement(element, index))
      return true;
    if (states[index] == FREE)
      return false;
    for (int perturb = perturb(hash), j = index; states[index] != FREE; perturb >>= PERTURB_SHIFT) {
      j = probe(perturb, j);
      index = j & mask;
      if (containsElement(element, index))
        return true;
    }
    return false;
  }

  private static int perturb(int hash) {
    return hash & 0x7fffffff;
  }

  private int findInsertionIndex(long element) {
    return findInsertionIndex(elements, states, element, mask);
  }

  private static int findInsertionIndex(long[] elements, byte[] states, long element, int mask) {
    int hash = hashOf(element);
    int index = hash & mask;
    if (states[index] == FREE)
      return index;
    else if (states[index] == FULL && elements[index] == element)
      return changeIndexSign(index);

    if (states[index] == FULL) {
      for (int perturb = perturb(hash), j = index;; perturb >>= PERTURB_SHIFT) {
        j = probe(perturb, j);
        index = j & mask;
        if (states[index] != FULL || elements[index] == element)
          break;
      }
    }
    if (states[index] == FREE)
      return index;
    /*
     * Due to the loop exit condition, if (states[index] == FULL) then
     * elements[index] == element
     */
    else if (states[index] == FULL)
      return changeIndexSign(index);

    int firstRemoved = index;
    for (int perturb = perturb(hash), j = index;; perturb >>= PERTURB_SHIFT) {
      j = probe(perturb, j);
      index = j & mask;
      if (states[index] == FREE)
        return firstRemoved;
      else if (states[index] == FULL && elements[index] == element)
        return changeIndexSign(index);
    }
  }

  private static int probe(int perturb, int j) {
    return (j << 2) + j + perturb + 1;
  }

  private static int changeIndexSign(int index) {
    return -index - 1;
  }

  public int size() {
    return size;
  }

  public boolean remove(long element) {
    int hash = hashOf(element);
    int index = hash & mask;
    if (containsElement(element, index)) {
      doRemove(index);
      return true;
    }
    if (states[index] == FREE)
      return false;

    for (int perturb = perturb(hash), j = index; states[index] != FREE; perturb >>= PERTURB_SHIFT) {
      j = probe(perturb, j);
      index = j & mask;
      if (containsElement(element, index)) {
        doRemove(index);
        return true;
      }
    }
    return false;
  }

  private boolean containsElement(long element, int index) {
    return (element != 0 || states[index] == FULL) && elements[index] == element;
  }

  private void doRemove(int index) {
    elements[index] = 0;
    states[index] = REMOVED;
    --size;
  }

  public boolean add(long element) {
    int index = findInsertionIndex(element);
    boolean newMapping = true;
    if (index < 0) {
      index = changeIndexSign(index);
      newMapping = false;
    }
    elements[index] = element;
    states[index] = FULL;
    if (newMapping) {
      ++size;
      if (shouldGrowTable())
        growTable();
    }
    return !newMapping;
  }

  private void growTable() {
    int oldLength = states.length;
    long[] oldElements = elements;
    byte[] oldStates = states;

    int newLength = RESIZE_MULTIPLIER * oldLength;
    long[] newElements = new long[newLength];
    byte[] newStates = new byte[newLength];
    int newMask = newLength - 1;
    for (int i = 0; i < oldLength; ++i) {
      if (oldStates[i] == FULL) {
        long element = oldElements[i];
        int index = findInsertionIndex(newElements, newStates, element, newMask);
        newElements[index] = element;
        newStates[index] = FULL;
      }
    }
    mask = newMask;
    elements = newElements;
    states = newStates;
  }

  private boolean shouldGrowTable() {
    return size > (mask + 1) * LOAD_FACTOR;
  }

  private static int hashOf(long l) {
    int h = (int)(l ^ (l >>> 32));
    h ^= ((h >>> 20) ^ (h >>> 12));
    return h ^ (h >>> 7) ^ (h >>> 4);
  }
}
