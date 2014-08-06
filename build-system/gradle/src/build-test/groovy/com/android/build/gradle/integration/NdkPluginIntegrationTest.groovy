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

package com.android.build.gradle.integration

import com.android.build.gradle.internal.test.category.DeviceTests
import org.gradle.tooling.BuildException
import org.junit.ClassRule

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.fail

import com.android.build.gradle.internal.test.fixture.IntegrationTestRule
import com.android.build.gradle.internal.test.fixture.app.HelloWorldJniApp
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category;

import java.util.zip.ZipFile

/**
 * Created by chiur on 8/15/14.
 */
class NdkPluginIntegrationTest {
    @ClassRule static public IntegrationTestRule fixture = new IntegrationTestRule();

    @Before
    public void setup() {
        new HelloWorldJniApp().writeSources(fixture.getSourceDir())
        fixture.getBuildFile() << """
apply plugin: 'com.android.application'

android {
    compileSdkVersion 19
    buildToolsVersion "19.1.0"
    useNewNativePlugin true
    ndk {
        moduleName "hello-jni"
    }
}
"""
    }

    @Test
    public void checkSystem() {
        fixture.execute("assembleDebug", []);

        //ZipFile apk = new ZipFile(fixture.file("build/outputs/apk/checkSystem-debug.apk"));
        //checkAllLibraries(apk);
    }

    private static void checkAllLibraries(ZipFile apk) {
        assertNotNull(apk.getEntry("lib/x86/libhello-jni.so"));
        assertNotNull(apk.getEntry("lib/mips/libhello-jni.so"));
        assertNotNull(apk.getEntry("lib/armeabi/libhello-jni.so"));
        assertNotNull(apk.getEntry("lib/armeabi-v7a/libhello-jni.so"));
    }
}
