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
import com.android.build.gradle.integration.common.fixture.app.AndroidTestApp
import com.android.build.gradle.integration.common.fixture.app.HelloWorldApp
import com.android.build.gradle.integration.common.fixture.app.TestSourceFile
import com.android.utils.FileUtils
import groovy.transform.CompileStatic
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Test resValue for array types are handled correctly.
 */
@CompileStatic
class ResValueArrayTest {

    static AndroidTestApp app = new HelloWorldApp()

    static {
        app.removeFile(app.getFile("HelloWorldTest.java"))
        app.addFile(new TestSourceFile("src/androidTest/java/com/example/helloworld",
                "ResValueTest.java",
                """
package com.example.helloworld;

import android.test.AndroidTestCase;

public class ResValueTest extends AndroidTestCase {
    public void testResValue() {
        assertEquals("00", getContext().getString(R.string.resString));
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
        resValue "integer-array",     "intArray",            "[123, 456]"
        resValue "string-array",      "strArray",            "[\\"foo1\\", \\"foo2\\"]"
        resValue "array",             "typedArray",          "[#FFFF0000, #FF00FF00, #FF0000FF]"
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
    void "check array types are handled correctly in generated.xml"() {
        project.execute("clean", "generateDebugResValue")
        File outputFile = project.file("build/generated/res/resValues/debug/values/generated.xml")
        assertTrue("Missing file: " + outputFile, outputFile.isFile())
        assertEquals(
                """<?xml version="1.0" encoding="utf-8"?>
<resources>

    <!-- Automatically generated file. DO NOT MODIFY -->

    <!-- Values from default config. -->
    <integer-array name="intArray">
        <item>123</item>
        <item>456</item>
    </integer-array>

    <string-array name="strArray">
        <item>foo1</item>
        <item>foo2</item>
    </string-array>

    <array name="typedArray">
        <item>#FFFF0000</item>
        <item>#FF00FF00</item>
        <item>#FF0000FF</item>
    </array>

</resources>""", FileUtils.loadFileWithUnixLineSeparators(outputFile))
    }
}
