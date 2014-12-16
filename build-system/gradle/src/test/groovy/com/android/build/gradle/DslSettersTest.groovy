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

import com.android.build.gradle.internal.test.BaseTest
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
/**
 * Tests for the AST transformation we use to generate "DSL" setters.
 */
public class DslSettersTest extends BaseTest {

    @Override
    protected void setUp() throws Exception {
        BasePlugin.TEST_SDK_DIR = new File("foo")
    }

    public void testSupportedMethodCalls() {
        Project project = ProjectBuilder.builder().withProjectDir(
                new File(testDir, "${FOLDER_TEST_SAMPLES}/basic")).build()

        project.apply plugin: 'com.android.application'

        project.android {
            aaptOptions {
                noCompress "foo"
                noCompress "foo", "bar"
                noCompress = ["foo", "bar"]

                useNewCruncher true
                useNewCruncher = true

                ignoreAssets = "foo"
                ignoreAssets "foo"
                ignoreAssetsPattern = "foo"
                ignoreAssetsPattern "foo"
            }
        }
    }

    public void testItWorks() {
        Project project = ProjectBuilder.builder().withProjectDir(
                new File(testDir, "${FOLDER_TEST_SAMPLES}/basic")).build()

        project.apply plugin: 'com.android.application'

        project.android {
            aaptOptions {
                ignoreAssets "foo"
            }
        }

        assert project.android.aaptOptions.ignoreAssets == "foo"
    }
}