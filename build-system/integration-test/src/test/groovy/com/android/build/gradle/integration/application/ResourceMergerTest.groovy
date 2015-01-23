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

package com.android.build.gradle.integration.application;

import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.build.gradle.integration.common.fixture.app.AndroidTestApp;
import com.android.build.gradle.integration.common.fixture.app.HelloWorldApp
import com.android.build.gradle.integration.common.fixture.app.TestSourceFile;
import com.android.builder.model.AndroidProject
import org.apache.tools.ant.BuildException
import org.gradle.api.tasks.TaskExecutionException
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.ClassRule;

/**
 * Created by cmw on 12/16/14.
 */
public class ResourceMergerTest {

    @ClassRule
    public static GradleTestProject project = GradleTestProject.builder().create()

    private static AndroidProject model

    @BeforeClass
    static void setUp() {
        AndroidTestApp app = new HelloWorldApp();
        app.addFile(new TestSourceFile("src/flavor1/res/layout", "main.xml", """
                <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    xmlns:tools="http://schemas.android.com/tools" android:layout_width="match_parent"
                    android:layout_height="match_parent" android:paddingLeft="10dp"
                    android:paddingRight="10dp"
                    android:paddingTop="10dp"
                    android:paddingBottom="10dp" tools:context=".HelloWorld">

                <TextView android:text="Hello flavor 1" android:layout_width="wrap_content"
                    android:layout_height="wrap_content" />

                </RelativeLayout>""".stripIndent()))

        app.addFile(new TestSourceFile("src/flavor2/res/layout", "main.xml", """
                <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:layout_height="match_parent" android:paddingLeft="10dp"
                    android:paddingRight="10dp"
                    android:paddingTop="10dp"
                    android:paddingBottom="10dp" tools:context=".HelloWorld">

                <TextView android:text="Hello flavor 2" android:layout_width="wrap_content"
                    android:layout_height="wrap_content" />

                </RelativeLayout>""".stripIndent()))
        app.writeSources(project.testDir)
        project.getBuildFile() << """
        apply plugin: 'com.android.application'

        android {
            compileSdkVersion $GradleTestProject.DEFAULT_COMPILE_SDK_VERSION
            buildToolsVersion "$GradleTestProject.DEFAULT_BUILD_TOOL_VERSION"

            defaultConfig {

            }

            buildTypes {
                debug {
                }
            }

            productFlavors {
                flavor1 {
                }
                flavor2 {
                    resValue "string", "VALUE", "20"
                }
            }

            applicationVariants.all { variant ->
                if (variant.buildType.name == "debug") {
                    variant.resValue "string", "VALUE", "1000"
                }
            }
        }
        """.stripIndent()

        model = project.executeAndReturnModel(
                'clean', 'generateFlavor2ReleaseResValue')
    }

    @AfterClass
    static void cleanUp() {
        project = null
        model = null
    }


    @Test
    void checkF2Value() {
        File outputFile = new File(project.getTestDir(),
                "build/generated/res/generated/flavor2Release/res")

        try {
            project.execute("assembleFlavor1");
        } catch (Exception e) {
            throw new Exception("Could not assemble flavor 1", e);
        }
        try {
            project.execute("assembleFlavor2");
        } catch (Exception e) {
            throw new Exception("Could not assemble flavor 2", e);
        }
    }
}
