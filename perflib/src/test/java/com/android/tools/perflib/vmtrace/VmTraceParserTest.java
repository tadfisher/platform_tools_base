/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.tools.perflib.vmtrace;

import com.android.utils.SparseArray;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

public class VmTraceParserTest extends TestCase {
    public void testParseHeader() throws IOException {
        File f = getFile("/header.trace");
        VmTraceParser parser = new VmTraceParser(f);
        parser.parseHeader(f);
        VmTraceData traceData = parser.getTraceData();

        assertEquals(3, traceData.getVersion());
        assertTrue(traceData.isDataFileOverflow());
        assertEquals(VmTraceData.ClockType.DUAL, traceData.getClockType());
        assertEquals("dalvik", traceData.getVm());

        SparseArray<String> threads = traceData.getThreads();
        assertEquals(2, threads.size());
        assertEquals("main", threads.get(1));
        assertEquals("AsyncTask #1", threads.get(11));

        Collection<MethodInfo> methods = traceData.getMethods();
        assertEquals(4, methods.size());

        MethodInfo info = traceData.getMethod(0x62830738);
        assertNotNull(info);
        assertEquals("android/graphics/Bitmap", info.className);
        assertEquals("access$100", info.methodName);
        assertEquals("(I)V", info.signature);
        assertEquals("android/graphics/BitmapF.java", info.srcPath);
        assertEquals(29, info.srcLineNumber);

        info = traceData.getMethod(0x6282b4b0);
        assertNotNull(info);
        assertEquals(-1, info.srcLineNumber);
    }

    public void testParseData() throws IOException {
        VmTraceData traceData = getVmTraceData("/exception.trace");
    }

    private VmTraceData getVmTraceData(String traceFilePath) throws IOException {
        VmTraceParser parser = new VmTraceParser(getFile(traceFilePath));
        parser.parse();
        return parser.getTraceData();
    }

    private File getFile(String path) {
        URL resource = getClass().getResource(path);
        // Note: When running from an IntelliJ, make sure the IntelliJ compiler settings treats
        // *.trace files as resources, otherwise they are excluded from compiler output
        // resulting in a NPE.
        assertNotNull(path + " not found", resource);
        return new File(resource.getFile());
    }
}
