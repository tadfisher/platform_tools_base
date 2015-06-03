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

package com.android.build.gradle.integration.test

import com.android.build.gradle.integration.common.fixture.GradleTestProject
import groovy.transform.CompileStatic
import org.junit.AfterClass
import org.junit.ClassRule
import org.junit.Test

/**
 * Regression test for a infinite loop error in DependencyManager when importing a project
 * with the same local name as the requester.
 */
@CompileStatic
class DuplicateModuleNameImportTest {

    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("duplicateNameImport")
            .create()

    @AfterClass
    static void cleanUp() {
        project = null
    }

    @Test
    void "check build"() throws Exception {
        // just building is enough.
        project.execute("clean", "assemble")
    }
}
