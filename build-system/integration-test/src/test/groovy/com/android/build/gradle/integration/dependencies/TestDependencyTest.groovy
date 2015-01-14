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
import com.android.build.gradle.integration.common.fixture.app.HelloWorldApp
import com.android.builder.model.AndroidProject
import com.android.builder.model.SyncIssue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatApk
/**
 * Tests the handling of test dependency.
 */
class TestDependencyTest {

    @Rule
    public GradleTestProject project = GradleTestProject.builder().create()

    @Before
    public void setUp() {
        new HelloWorldApp().writeSources(project.testDir)
        project.getBuildFile() << """
apply plugin: 'com.android.application'

repositories {
    jcenter()
}

android {
    compileSdkVersion $GradleTestProject.DEFAULT_COMPILE_SDK_VERSION
    buildToolsVersion "$GradleTestProject.DEFAULT_BUILD_TOOL_VERSION"
}
"""
    }

    @Test
    public void "Test with lower dep version than Tested"() {
        project.getBuildFile() << """
dependencies {
    compile 'com.google.guava:guava:18.0'
    androidTestCompile 'com.google.guava:guava:17.0'
}
"""
        // no need to do a full build. Let's just run the manifest task.
        AndroidProject model = project.getSingleModel()

        assertThat(model).issues().hasOnly(
                SyncIssue.SEVERITY_ERROR,
                SyncIssue.TYPE_MISMATCH_DEP,
                "")
    }

    @Test
    public void "Test with same dep version than Tested does NOT embed dependency"() {
        project.getBuildFile() << """
dependencies {
    compile 'com.google.guava:guava:18.0'
    androidTestCompile 'com.google.guava:guava:18.0'
}
"""
        // no need to do a full build. Let's just run the manifest task.
        project.execute("clean", "assembleDebugAndroidTest")

        File apk = project.getApk("debug", "androidTest", "unaligned")

        assertThatApk(apk).doesNotContainClass("Lcom/google/common/io/Files;")
    }
}
