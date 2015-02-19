/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.tests.basic.buildscript;

import com.android.annotations.NonNull;
import com.android.builder.testing.TestData;
import com.android.builder.testing.api.RemoteTestRunner;
import com.android.ddmlib.MultiLineReceiver;

import java.io.IOException;
import java.io.File;
import java.lang.Exception;
import java.lang.RuntimeException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class FakeRemoteTestRunner implements RemoteTestRunner {

    private boolean initCalled = false;
    private boolean terminateCalled = false;

    @NonNull
    @Override
    public String getName() {
        return "fakeRemoteTestRunner";
    }

    @Override
    public void init() {
        System.out.println("INIT CALLED");
        initCalled = true;
    }

    @Override
    public void close() throws IOException {
        System.out.println("TERMINATE CALLED");
        terminateCalled = true;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public Map<String, List<String>> runTests(TestData testData, File testApk) {
        System.out.println("RUN TESTS CALLED");

        Map<String, List<String>> results = new HashMap<String, List<String>>();
        List<String> abis = new ArrayList<String>();
        abis.add("x86");
        if (!testApk.exists()) {
            throw new RuntimeException("Test APK " + testApk.getPath() + " not found.");
        }
        testData.getTestedApks(160 /* density */, "en" /* language */,
                null /* region */, abis);
        results.put("device1", FakeDevice.testOutput);
        results.put("device1b", FakeDevice.testOutput);

        return results;
    }
}