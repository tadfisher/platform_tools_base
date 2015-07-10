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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;

@RunWith(Parameterized.class)
public class DatabindingTest {

    @Parameterized.Parameters(name="forExperimentalPlugin={0}")
    public static Collection<Object[]> getParameters() {
        return ImmutableList.of(new Object[] {Boolean.TRUE}, new Object[]{Boolean.FALSE});
    }

    public DatabindingTest(boolean forExperimentalPlugin) {
        project = GradleTestProject.builder()
                .fromTestProject("databinding", forExperimentalPlugin ? "2" : null)
                .captureStdOut(true)
                .forExpermimentalPlugin(forExperimentalPlugin)
                .create();
    }

    @Rule
    public final GradleTestProject project;

    private String buildOutput;

    @Before
    public void setUp() {
        project.getStdout().reset();
        project.execute("assembleDebug");
        buildOutput = project.getStdout().toString();
    }

    @Test
    public void checkApkContainsDatabindingClasses() throws IOException, ProcessException {
        assertThat(buildOutput).contains(":dataBindingProcessLayoutsDebug");
        assertThatApk(project.getApk("debug")).containsClass(
                "Landroid/databinding/appwithspaces/databinding/ActivityMainBinding;");
    }
}
