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

package com.android.tools.perflib.heap.io;

import com.android.tools.perflib.heap.HprofParser;
import com.android.tools.perflib.heap.Snapshot;

import junit.framework.TestCase;

import java.io.File;

public class HprofBufferTest extends TestCase {

    File file = new File(getClass().getResource("/dialer.android-hprof").getFile());

    public void testSimpleMapping() throws Exception {
        HprofBuffer.BUFFER_SIZE = (int) file.length();
        Snapshot snapshot = (new HprofParser(file)).parse();
        assertSnapshotCorrect(snapshot);
    }

    public void testMultiMapping() throws Exception {
        HprofBuffer.BUFFER_SIZE = 4096; // Force mapping of multiple shards
        MultiMappedBuffer.PADDING = 128;
        Snapshot snapshot = (new HprofParser(file)).parse();
        assertSnapshotCorrect(snapshot);
    }

    public void testMultiMappingOddSplit() throws Exception {
        HprofBuffer.BUFFER_SIZE = 9973;  // Force mapping of multiple shards
        MultiMappedBuffer.PADDING = 8;   // Results in some strings split over two shards
        Snapshot snapshot = (new HprofParser(file)).parse();
        assertSnapshotCorrect(snapshot);
    }

    private void assertSnapshotCorrect(Snapshot snapshot) {
        assertEquals(11182, snapshot.getGCRoots().size());
        assertEquals(38, snapshot.getHeap(65).getClasses().size());
        assertEquals(1406, snapshot.getHeap(65).getInstances().size());
        assertEquals(3533, snapshot.getHeap(90).getClasses().size());
        assertEquals(38710, snapshot.getHeap(90).getInstances().size());
    }
}
