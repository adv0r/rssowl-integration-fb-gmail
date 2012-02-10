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
package org.rssowl.core.tests.persist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.internal.persist.LongHashSet;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class LongHashSetTest {
    private Set<Long> javaSet = new HashSet<Long>();

    @Before
    public void setUp() throws Exception {
        javaSet.add(50L);
        javaSet.add(75L);
        javaSet.add(25L);
        javaSet.add(Long.MAX_VALUE);
        javaSet.add(0L);
        javaSet.add(1L);
        javaSet.add(33L);
        javaSet.add(23234234L);
        javaSet.add(Long.MIN_VALUE);

        /* Add a few more to cause the table to rehash */
        javaSet.addAll(generate());

    }

    private Set<Long> generate() {
        Set<Long> set = new HashSet<Long>();
        Random r = new Random();
        for (int i = 0; i < 2000; ++i)
            set.add(r.nextLong());
        return set;
    }

    private LongHashSet createFromJavaSet() {
        LongHashSet set = new LongHashSet();
        for (Long element : javaSet)
            set.add(element);
        return set;
    }

    @Test
    public void testAddAndContainsWith0ExpectedSize() {
        LongHashSet set = new LongHashSet(0);
        assertAddAndContains(set);
    }

    @Test
    public void testAddAndContainsWithExpectedSize() {
        LongHashSet set = new LongHashSet(500);
        assertAddAndContains(set);
    }

    @Test
    public void testAddAndContains() {
        LongHashSet set = new LongHashSet();
        assertAddAndContains(set);
    }

    private void assertAddAndContains(LongHashSet set) {
        assertAddAndContains(set, 0, new HashSet<Long>());
    }

    private void assertAddAndContains(LongHashSet set, int setSize,
            Set<Long> keysInSet) {
        assertEquals(setSize, set.size());
        for (Long element : javaSet) {
            set.add(element);
            if (!keysInSet.contains(element))
              ++setSize;
            assertEquals(setSize, set.size());
            assertTrue(set.contains(element));
        }
    }

    @Test
    public void testAddAbsentOnExisting() {
        LongHashSet set = createFromJavaSet();
        int size = javaSet.size();
        for (long element : generateAbsent()) {
            set.add(element);
            assertEquals(++size, set.size());
            assertTrue(set.contains(element));
        }
    }

    @Test
    public void testAddOnExisting() {
        LongHashSet set = createFromJavaSet();
        for (long element : javaSet) {
            set.add(element);
            assertEquals(javaSet.size(), set.size());
            assertTrue(set.contains(element));
        }
    }

    @Test
    public void testContainsAbsent() {
        Set<Long> generated = generateAbsent();
        LongHashSet set = createFromJavaSet();

        for (long element : generated)
          assertFalse(set.contains(element));
    }

    @Test
    public void testContainsFromEmpty() {
        LongHashSet set = new LongHashSet();
        assertFalse(set.contains(5));
        assertFalse(set.contains(0));
        assertFalse(set.contains(50));
    }

    @Test
    public void testRemove() {
        LongHashSet set = createFromJavaSet();
        int setSize = javaSet.size();
        assertEquals(setSize, set.size());
        for (long element : javaSet) {
            set.remove(element);
            assertEquals(--setSize, set.size());
            assertFalse(set.contains(element));
        }

        /* Ensure that add and get still work correctly after removals */
        assertAddAndContains(set);
    }

    /* This time only remove some entries */
    @Test
    public void testRemove2() {
        LongHashSet set = createFromJavaSet();
        int setSize = javaSet.size();
        int count = 0;
        Set<Long> keysInSet = new HashSet<Long>(javaSet);
        for (long element : javaSet) {
            keysInSet.remove(element);
            set.remove(element);
            assertEquals(--setSize, set.size());
            assertFalse(set.contains(element));
            if (count++ > 5)
                break;
        }

        /* Ensure that add and get still work correctly after removals */
//    assertAddAndContains(set, setSize, keysInSet);
    }

    @Test
    public void testRemoveFromEmpty() {
        LongHashSet set = new LongHashSet();
        assertFalse(set.remove(50));
    }

    @Test
    public void testRemoveAbsent() {
        Set<Long> generated = generateAbsent();

        LongHashSet set = createFromJavaSet();
        int setSize = set.size();

        for (long element : generated) {
            set.remove(element);
            assertEquals(setSize, set.size());
            assertFalse(set.contains(element));
        }
    }

    /**
     * Returns a set with at least 100 elements where each element is absent from javaSet.
     */
    private Set<Long> generateAbsent() {
        Set<Long> generated = new HashSet<Long>();
        do {
            generated.addAll(generate());
            for (Long key : javaSet)
                generated.remove(key);
        } while (generated.size() < 100);
        return generated;
    }

    @Test
    public void testCopy() {
        LongHashSet copy = new LongHashSet(createFromJavaSet());
        assertEquals(javaSet.size(), copy.size());

        for (Long element : javaSet)
          assertTrue(copy.contains(element));
    }
}