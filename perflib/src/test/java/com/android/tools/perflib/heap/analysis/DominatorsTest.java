/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.perflib.heap.analysis;

import com.android.tools.perflib.heap.Heap;
import com.android.tools.perflib.heap.Instance;

import junit.framework.TestCase;

import java.util.Map;

public class DominatorsTest extends TestCase {

    private Heap mHeap;
    private Map<Instance, Instance> mDominators;

    public void testSimpleGraph() {
        mHeap = new SyntheticHeap(6)
                .addEdge(1, 2)
                .addEdge(1, 3)
                .addEdge(2, 4)
                .addEdge(2, 6)
                .addEdge(3, 4)
                .addEdge(3, 5)
                .addEdge(4, 6)
                .addRoot(1)
                .getHeap();

        mDominators = new Dominators(mHeap).getDominatorMap();
        assertEquals(6, mDominators.size());
        assertDominates(1, 2);
        assertDominates(1, 3);
        assertDominates(1, 4);
        assertDominates(1, 6);
        assertDominates(3, 5);
    }

    public void testCyclicGraph() {
        mHeap = new SyntheticHeap(4)
                .addEdge(1, 2)
                .addEdge(1, 3)
                .addEdge(1, 4)
                .addEdge(2, 3)
                .addEdge(3, 4)
                .addEdge(4, 2)
                .addRoot(1)
                .getHeap();
        mDominators = new Dominators(mHeap).getDominatorMap();
        assertEquals(4, mDominators.size());
        assertDominates(1, 2);
        assertDominates(1, 3);
        assertDominates(1, 4);
    }

    /** Asserts that nodeA dominates nodeB in mHeap. */
    private void assertDominates(int nodeA, int nodeB) {
        assertEquals(mHeap.getInstance(nodeA), mDominators.get(mHeap.getInstance(nodeB)));
    }
}
