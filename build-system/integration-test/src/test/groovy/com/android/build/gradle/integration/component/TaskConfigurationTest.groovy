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

package com.android.build.gradle.integration.component

import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.build.gradle.integration.common.fixture.app.HelloWorldApp
import com.android.build.gradle.integration.common.truth.TruthHelper
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat

/**
 * Test for custom configuration of tasks.
 */
class TaskConfigurationTest {

    @ClassRule
    public static GradleTestProject project = GradleTestProject.builder()
            .fromTestApp(new HelloWorldApp())
            .forExpermimentalPlugin(true)
            .create();

    @BeforeClass
    public static void setUp() {

        project.buildFile << """
apply plugin: 'com.android.model.application'

import com.android.build.gradle.internal.TaskType
import com.android.build.gradle.model.AndroidBinary

binaries.withType(AndroidBinary) {
    configure(TaskType.ABSTRACT_COMPILE) {
        destinationDir = file("build/custom-java")
    }
}

model {
    android {
        compileSdkVersion = $GradleTestProject.DEFAULT_COMPILE_SDK_VERSION
        buildToolsVersion = "$GradleTestProject.DEFAULT_BUILD_TOOL_VERSION"
    }
}
"""
    }

    @AfterClass
    static void cleanUp() {
        project = null
    }

    @Test
    void "check destination dir of the java compile task is modified"() {
        project.execute("compileDebugJavaWithJavac")
        assertThat(project.file("build/java/com/example/helloworld/HelloWorld.class")).doesNotExist()
        assertThat(project.file("build/custom-java/com/example/helloworld/HelloWorld.class")).exists()
    }
}
