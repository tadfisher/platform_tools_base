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

package com.android.build.gradle.integration.dependencies
import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.builder.model.AndroidProject
import groovy.transform.CompileStatic
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatAar
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatApk
/**
 * test for optional aar (using the provided scope)
 */
@CompileStatic
class OptionalAarTest {

    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("projectWithModules")
            .create()
    static Map<String, AndroidProject> models

    @BeforeClass
    static void setUp() {
        project.getSubproject('app').getBuildFile() << """

dependencies {
    compile project(':library')
}
"""
        project.getSubproject('library').getBuildFile() << """

dependencies {
    provided project(':library2')
}
"""
        models = project.executeAndReturnMultiModel("clean", ":app:assembleDebug")
    }

    @AfterClass
    static void cleanUp() {
        project = null
        models = null
    }

    @Test
    void "check provided library is absent from app"() {
        File apk = project.getSubproject('app').getApk("debug")

        assertThatApk(apk).doesNotContainResource("layout/optional.xml")
        assertThatApk(apk).doesNotContainClass("Lcom/example/android/multiproject/library2/PersonView2;")
    }

    @Test
    void "check provided library symbol is not in aar"() {
        File aar = project.getSubproject('library').getAar("release")

        assertThatAar(aar).doesNotContainResource("layout/optional.xml")
        assertThatAar(aar).textSymbolFile().doesNotContain("layout optional")
    }
}
