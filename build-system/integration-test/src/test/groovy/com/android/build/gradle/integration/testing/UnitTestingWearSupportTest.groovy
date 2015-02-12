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

package com.android.build.gradle.integration.testing

import com.android.build.gradle.integration.common.fixture.GradleTestProject
import org.junit.ClassRule
import org.junit.Test

import static com.android.build.gradle.integration.testing.JUnitResults.Outcome.FAILED
import static com.android.build.gradle.integration.testing.JUnitResults.Outcome.PASSED
import static com.google.common.truth.Truth.assertThat

/**
 * Checks that projects with Wear apps work fine with the unit testing support.
 */
class UnitTestingWearSupportTest {
    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromSample("embedded")
            .create()

    @Test
    void mainApp() {
        project.execute("clean", ":main:test")

        checkResults(
                "main/build/test-results/TEST-UnitTest.xml",
                [ "success" ],
                [])
    }

    @Test
    void wearApp() {
        project.execute("clean", ":micro-apps:default:test")

        checkResults(
                "micro-apps/default/build/test-results/TEST-UnitTest.xml",
                [ "success" ],
                [])
    }

    private static void checkResults(String xmlPath, ArrayList<String> passed, ArrayList<String> failed) {
        def results = new JUnitResults(project.file(xmlPath))
        assertThat(results.allTestCases).containsExactlyElementsIn(failed + passed)
        passed.each { assert results.outcome(it) == PASSED }
        failed.each { assert results.outcome(it) == FAILED }
    }
}
