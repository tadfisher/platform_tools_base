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

package com.android.build.gradle.internal.test

import org.gradle.api.internal.tasks.options.Option

/**
 * Options for running tests.
 */
class TestOptions {
    /** Name of the results directory. */
    String resultsDir

    /** Name of the reports directory. */
    String reportDir

    /** Options for controlling unit tests execution. */
    UnitTests unitTests = new UnitTests()

    /** Options for controlling unit tests execution. */
    static class UnitTests {

        /**
         * Whether unmocked methods from android.jar should throw exceptions or return default
         * values (i.e. zero or null).
         */
        boolean returnDefaultValues

        @Option(option = "debug-jvm", description = "Enable debugging for the process. The process is started suspended and listening on port 5005. [INCUBATING]")
        boolean debug
    }
}
