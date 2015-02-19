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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Allows tests to be run on an externally managed collection of remote test runners.
 */
@Beta
public interface RemoteTestRunner extends Closeable {

    /**
     * Returns the name of the provider. Must be unique, not contain spaces, and start with a lower
     * case.
     */
    @NonNull
    String getName();

    /**
     * Initializes the provider. This will be called by the android gradle plugin before any other
     * method (except {@link #getName()}).
     * @throws IOException
     */
    void init() throws IOException;

    /**
     * Shuts down the provider.
     *
     * Called afer all the tests are run. The provider should be able to be reinitialized if
     * <code>init()</code> is called again.
     * @throws DeviceException
     */
    @Override
    void close() throws IOException;

    /**
     * Returns true if the provider is configured and able to run.
     *
     * If the provider is not configured, the <code>${providername}AndroidTest</code> task will
     * still be created, but it will be disabled.
     */
    boolean isConfigured();

    /**
     * Runs tests on the remote devices.
     *
     * This could be called from multiple threads.
     *
     * @param testData The tests to run on the remote devices.
     * @return a map from remote device identifier to multi-line instrumentation output.
     */
    Map<String, List<String>> runTests(TestData testData, File testApk);
}
