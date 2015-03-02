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

import com.android.build.gradle.integration.common.category.DeviceTests
import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.build.gradle.integration.common.fixture.app.AndroidTestApp
import com.android.build.gradle.integration.common.fixture.app.HelloWorldApp
import com.android.build.gradle.integration.common.fixture.app.TestSourceFile
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Test resValue for string type is treated as String.
 */
class ResValueStringTest {
    static AndroidTestApp app = new HelloWorldApp()
    static {
        app.removeFile(app.getFile("HelloWorldTest.java"))
        app.addFile(new TestSourceFile("src/androidTest/java/com/example/helloworld", "ResValueTest.java",
"""
package com.example.helloworld;

import android.test.AndroidTestCase;

public class ResValueTest extends AndroidTestCase {
    public void testResValue() {
        assertEquals("00", getContext().getString(R.string.foo));
    }
}
"""))
    }

    @ClassRule
    public static GradleTestProject project = GradleTestProject.builder()
            .fromTestApp(app)
            .create()

    @BeforeClass
    static void setUp() {
        project.getBuildFile() << """
apply plugin: 'com.android.application'

android {
    compileSdkVersion $GradleTestProject.DEFAULT_COMPILE_SDK_VERSION
    buildToolsVersion "$GradleTestProject.DEFAULT_BUILD_TOOL_VERSION"

    defaultConfig {
        // foo becomes "0" if it is incorrectly treated  as int.
        resValue "string", "foo", "00"
    }
}
"""
    }


    @AfterClass
    static void cleanUp() {
        project = null
        app = null
    }


    @Test
    void "check <string> tag is used in generated.xml" () {
        project.execute("clean", "generateDebugResValue")
        File outputFile = project.file("build/generated/res/generated/debug/values/generated.xml")
        assertTrue("Missing file: " + outputFile, outputFile.isFile())
        assertEquals(
"""<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!-- Automatically generated file. DO NOT MODIFY -->

    <!-- Values from default config. -->
    <string name="foo">00</string>

</resources>
""",
                outputFile.getText("UTF-8"))
    }

    @Test
    @Category(DeviceTests.class)
    void "check resValue is treated as string"() {
        project.execute("clean", "connectedAndroidTest")
    }
}
