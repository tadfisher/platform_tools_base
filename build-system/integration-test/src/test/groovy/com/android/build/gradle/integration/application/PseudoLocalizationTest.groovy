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

package com.android.build.gradle.integration.application

import com.android.build.gradle.integration.common.category.Lint
import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.sdklib.repository.FullRevision
import com.google.common.truth.Truth
import groovy.transform.CompileStatic
import org.junit.AfterClass
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatApk
/**
 * Test for pseudolocalized.
 */
@CompileStatic
class PseudoLocalizationTest {
    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("pseudolocalized")
            .create()

    @BeforeClass
    static void setUp() {
        Truth.ASSERT.that(FullRevision.parseRevision(GradleTestProject.DEFAULT_BUILD_TOOL_VERSION))
                .isGreaterThan(new FullRevision(21,0,0))

        project.execute("clean", "assembleDebug")
    }

    @AfterClass
    static void cleanUp() {
        project = null
    }

    @Test
    @Category(Lint.class)
    void lint() {
        project.execute("lint")
    }

    @Test
    public void testPseudolocalization() throws Exception {
        assertThatApk(project.getApk("debug")).locales().containsAllOf("en-XA", "ar-XB")
    }
}
