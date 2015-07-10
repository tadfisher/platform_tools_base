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

package com.android.build.gradle.integration.databinding;

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatApk;
import static com.google.common.truth.Truth.assertThat;

import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.ide.common.process.ProcessException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

public class DatabindingTest {

    @ClassRule
    public static final GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("databinding")
            .captureStdOut(true)
            .create();

    private static String buildOutput;

    @BeforeClass
    public static void setUp() {
        project.getStdout().reset();
        project.execute("assembleDebug");
        buildOutput = project.getStdout().toString();
    }

    @Test
    public void checkTaskHasBeenRun() {
        assertThat(buildOutput).contains(":dataBindingProcessLayoutsDebug");
    }

    @Test
    public void checkApkContainsDatabindingClasses() throws IOException, ProcessException {
        assertThatApk(project.getApk("debug")).containsClass(
                "Landroid/databinding/appwithspaces/databinding/ActivityMainBinding;");
    }
}
