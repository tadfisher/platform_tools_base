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

import com.android.build.gradle.internal.test.category.DeviceTests
import com.android.build.gradle.internal.test.fixture.GradleTestProject
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import static org.junit.Assume.assumeTrue

/**
 * Integration test for STL containers.
 *
 * This unit test is parameterized and will be executed for various values of STL.
 */
@RunWith(Parameterized.class)
public class SampleIntegTest {

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> data() {
        return [
                //projectName              hasDeviceTest
                //------------             ------------------
                [ "aidl",                  false ].toArray(),
                [ "api",                   true ].toArray(),
                [ "applibtest",            false ].toArray(),
                [ "assets",                true ].toArray(),
                [ "attrOrder",             true ].toArray(),
                [ "basic",                 true ].toArray(),
                [ "dependencies",          true ].toArray(),
                [ "dependencyChecker",     false ].toArray(),
                [ "densitySplit",          true ].toArray(),
                [ "emptySplit",            false ].toArray(),
                [ "filteredOutBuildType",  false ].toArray(),
                [ "filteredOutVariants",   false ].toArray(),
                [ "flavored",              true ].toArray(),
                [ "flavorlib",             true ].toArray(),
                [ "flavoredlib",           true ].toArray(),
                [ "flavors",               true ].toArray(),
                [ "genFolderApi",          false ].toArray(),
                [ "libMinifyJarDep",       true ].toArray(),
                [ "libMinifyLibDep",       true ].toArray(),
                [ "libTestDep",            true ].toArray(),
                [ "libsTest",              true ].toArray(),
                [ "localAarTest",          false ].toArray(),
                [ "localJars",             false ].toArray(),
                [ "migrated",              true ].toArray(),
                [ "multiDex",              true ].toArray(),
                [ "multiDexWithLib",       true ].toArray(),
                [ "multiproject",          false ].toArray(),
                [ "multires",              true ].toArray(),
                [ "ndkJniLib",             true ].toArray(),
                [ "ndkLibPrebuilts",       true ].toArray(),
                [ "ndkPrebuilts",          false ].toArray(),
                [ "ndkSanAngeles",         true ].toArray(),
                [ "noPreDex",              false ].toArray(),
                [ "overlay1",              true ].toArray(),
                [ "overlay2",              true ].toArray(),
                [ "packagingOptions",      true ].toArray(),
                [ "pkgOverride",           true ].toArray(),
                [ "minify",                true ].toArray(),
                [ "minifyLib",             true ].toArray(),
                [ "renderscript",          false ].toArray(),
                [ "renderscriptInLib",     false ].toArray(),
                [ "renderscriptMultiSrc",  false ].toArray(),
                [ "rsSupportMode",         false ].toArray(),
                [ "sameNamedLibs",         true ].toArray(),
                [ "tictactoe",             false ].toArray()
        ]
    }

    private String projectName

    private boolean hasDeviceTest

    private boolean assembled = false

    SampleIntegTest(String projectName, boolean hasDeviceTest) {
        this.projectName = projectName
        this.hasDeviceTest = hasDeviceTest
    }

    @Rule
    public GradleTestProject project = GradleTestProject.builder()
            .fromSample("regular/" + projectName)
            .withName(projectName)
            .withSingleProject(true)
            .build();

    @Before
    void setup() {
        if (!assembled) {
            project.execute("clean", "assembleDebug")
        }
        assembled = true
    }

    @Test
    void assembleDebug() {
    }

    @Test
    @Category(DeviceTests.class)
    void deviceTest() {
        assumeTrue(hasDeviceTest)
        project.execute("connectedCheck");
    }
}
