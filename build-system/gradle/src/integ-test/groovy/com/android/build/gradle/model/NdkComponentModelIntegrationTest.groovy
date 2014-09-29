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

package com.android.build.gradle.model

/**
 * Created by chiur on 9/26/14.
 */
import static org.junit.Assert.assertNotNull

import com.android.build.gradle.internal.test.category.DeviceTests
import com.android.build.gradle.internal.test.fixture.GradleProjectTestRule
import com.android.build.gradle.internal.test.fixture.app.HelloWorldJniApp
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category

import java.util.zip.ZipFile

/**
 * Basic integration test for native plugin.
 */
class NdkComponentModelIntegrationTest {
    @ClassRule static public GradleProjectTestRule fixture = new GradleProjectTestRule();

    @BeforeClass
    static public void setup() {
        new HelloWorldJniApp(true /* useCppSource */).writeSources(fixture.getSourceDir())
        fixture.getBuildFile() << """
import com.android.build.gradle.model.NdkComponentModelPlugin
apply plugin: NdkComponentModelPlugin

model {
    android.ndk {
        compileSdkVersion 19
        moduleName "hello-jni"
    }
}
"""
    }

    @Test
    public void assemble() {
        fixture.execute("assemble");
    }
}
