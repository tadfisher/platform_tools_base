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

package com.android.builder.testing.api;

import com.android.annotations.NonNull;
import com.android.builder.testing.TestData;
import com.android.ddmlib.IShellEnabledDevice;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.testrunner.TestResult;
import com.google.common.annotations.Beta;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Provides a list of remote or local devices.
 */
@Beta
public abstract class RemoteTestRunner {

    /**
     * Returns the name of the provider. Must be unique, not contain spaces, and start with a lower
     * case.
     *
     * @return the name of the provider.
     */
    @NonNull
    public abstract String getName();

    /**
     * Initializes the provider. This is called before any other method (except {@link #getName()}).
     * @throws IOException
     */
    public abstract void init() throws IOException;

    public abstract void terminate() throws IOException;

    /**
     * Returns true if the provider is configured and able to run.
     *
     * @return if the provider is configured.
     */
    public abstract boolean isConfigured();

    /**
     * Run tests on the remote devices.
     *
     * @param testData The test to run on the remote devices.
     * @return a map of remote device identifiers to test output.
     */
    public abstract Map<String, String[]> runTests(TestData testData);

    public int getMaxThreads() {
        return 0;
    }

}
