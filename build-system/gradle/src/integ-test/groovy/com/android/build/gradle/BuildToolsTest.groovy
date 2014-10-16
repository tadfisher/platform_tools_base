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

package com.android.build.gradle

import com.android.build.gradle.internal.test.fixture.GradleProjectTestRule
import com.android.build.gradle.internal.test.fixture.app.HelloWorldApp
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class BuildToolsTest {

    @ClassRule
    static public GradleProjectTestRule fixture = new GradleProjectTestRule();

    @BeforeClass
    static public void setup() {
        new HelloWorldApp().writeSources(fixture.getSourceDir())
        fixture.getBuildFile() << """
        apply plugin: 'com.android.application'

        android {
            compileSdkVersion 19
            buildToolsVersion "20.0.0"
        }
        """
    }

    @Test
    public void assemble() {
        fixture.execute("assemble");

        // Comment the change and the last assert will fail
        fixture.getBuildFile() << """
        apply plugin: 'com.android.application'

        android {
            compileSdkVersion 19
            buildToolsVersion "19.1.0"
        }
        """

        String output = fixture.execute("assemble");

        assertTrue(output.contains("Skipping task ':compileDebugJava' as it is up-to-date"));
        assertFalse(output.contains("Skipping task ':dexDebug' as it is up-to-date"));
    }
}
