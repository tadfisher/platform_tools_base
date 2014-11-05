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

package com.android.build.gradle

import com.android.build.gradle.internal.test.category.DeviceTests
import com.android.build.gradle.internal.test.fixture.GradleTestProject
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Integration test for ApplicationVariants and LibraryVariants.
 */
class ApiTest {
    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromSample("regular/api")
            .build()

    @BeforeClass
    static void setup() {
        project.execute("clean", "assembleDebug");
    }

    @Test
    void "assembleDebug succeeds"() {
    }

    @Test
    @Category(DeviceTests.class)
    void "connectedCheck succeeds"() {
        project.execute("connectedCheck");
    }

}
