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

import com.android.annotations.NonNull;
import com.android.tools.perflib.heap.Heap;
import com.android.tools.perflib.heap.Instance;
import com.android.tools.perflib.heap.RootObj;
import com.android.tools.perflib.heap.Visitor;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Initial implementation of dominator computation.
 *
 * The full algorithm is described in {@see http://www.cs.rice.edu/~keith/EMBED/dom.pdf}. It's a
 * simple iterative algorithm with worst-case complexity of O(N^2).
 */
public class Dominators {

    private final Heap mHeap;

    private final Map<Instance, Instance> mDominatorMap = Maps.newHashMap();

    public Dominators(@NonNull Heap heap) {
        mHeap = heap;
    }

    @NonNull
    public Map<Instance, Instance> getDominatorMap() {
        if (mDominatorMap.isEmpty()) {
            calculate();
        }
        return ImmutableMap.copyOf(mDominatorMap);
    }

    private void calculate() {
        // Only instances reachable from the roots will participate in dominator computation.
        // Start with a topological sort because we want to process a node after all its parents
        Map<Instance, Integer> topSort = getTopologicalSort(mHeap.getRoots());
        Set<Instance> roots = Sets.newHashSet();
        for (RootObj root : mHeap.getRoots()) {
            if (root.getReferredInstance() != null) {
                Instance ref = root.getReferredInstance();
                mDominatorMap.put(ref, ref);
                roots.add(ref);
            }
        }

        // We need to iterate on the dominator computation because the graph may contain cycles.
        // TODO: Check how long it takes to converge, and whether we need to place an upper bound.
        boolean changed = true;
        while (changed) {
            changed = false;

            for (Instance node : topSort.keySet()) {
                if (!roots.contains(node)) { // Root nodes are skipped
                    Instance dominator = null;

                    for (Instance predecessor : node.getReferences()) {
                        if (dominator == null) {
                            dominator = predecessor;
                        } else {
                            // If we don't have a dominator/approximation for predecessor, skip it
                            if (mDominatorMap.get(predecessor) != null) {
                                Instance fingerA = dominator;
                                Instance fingerB = predecessor;
                                while (!fingerA.equals(fingerB)) {
                                    if (topSort.get(fingerA) < topSort.get(fingerB)) {
                                        fingerB = mDominatorMap.get(fingerB);
                                    } else {
                                        fingerA = mDominatorMap.get(fingerA);
                                    }
                                }
                                dominator = fingerA;
                            }
                        }
                    }

                    if (mDominatorMap.get(node) != dominator) {
                        mDominatorMap.put(node, dominator);
                        changed = true;
                    }
                }
            }
        }
    }

    private Map<Instance, Integer> getTopologicalSort(Iterable<? extends Instance> roots) {
        TopologicalSortVisitor visitor = new TopologicalSortVisitor();
        for (Instance root : roots) {
            root.accept(visitor);
        }
        return visitor.getTopologicalSort();
    }


    private static class TopologicalSortVisitor implements Visitor {

        private final Set<Instance> mVisited = Sets.newHashSet();
        private final List<Instance> mPostorder = Lists.newArrayList();

        @Override
        public boolean visitEnter(Instance instance) {
            return mVisited.add(instance);
        }

        @Override
        public void visitLeave(Instance instance) {
            mPostorder.add(instance);
        }

        Map<Instance, Integer> getTopologicalSort() {
            Map<Instance, Integer> result = Maps.newLinkedHashMap();
            int currentIndex = 0;
            for (Instance node : Lists.reverse(mPostorder)) {
                result.put(node, ++currentIndex);
            }
            return result;
        }
    }
}
