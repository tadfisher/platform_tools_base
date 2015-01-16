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

package com.android.build.gradle.integration.application

import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.build.gradle.integration.common.fixture.app.HelloWorldApp
import com.android.build.gradle.integration.common.fixture.app.HelloWorldJniApp
import org.junit.BeforeClass
import org.junit.ClassRule

/**
 * Created by chiur on 1/26/15.
 */
class ZipAlignTest {

    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder().create()

    @BeforeClass
    static public void setUp() {
        new HelloWorldApp().writeSources(project.testDir)

        project.getBuildFile() << """
apply plugin: 'com.android.application'

android {
    applicationVariants.all { variant ->
        if (variant.buildType.name == "debug") {
            variant.outputs.each { output ->
                output.createZipAlignTask("alignDebug", output.getOutput
            }
            variant.buildConfigField "int", "VALUE_VARIANT", "1000"
        }
    }
}
"""
    }
}
