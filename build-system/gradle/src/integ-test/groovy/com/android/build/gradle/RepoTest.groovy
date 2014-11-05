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
import com.android.build.gradle.internal.test.fixture.app.HelloWorldJniApp
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category

import java.util.zip.ZipFile

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull

/**
 * Integration test for uploadAchives with multiple projects.
 */
class RepoTest {

    @ClassRule
    static public GradleTestProject app = GradleTestProject.builder()
            .withName("app")
            .fromSample("manual/repo/app")
            .build();

    @ClassRule
    static public GradleTestProject baseLibrary = GradleTestProject.builder()
            .withName("baseLibrary")
            .fromSample("manual/repo/baseLibrary")
            .build();

    @ClassRule
    static public GradleTestProject library = GradleTestProject.builder()
            .withName("library")
            .fromSample("manual/repo/library")
            .build();

    @ClassRule
    static public GradleTestProject util = GradleTestProject.builder()
            .withName("util")
            .fromSample("manual/repo/util")
            .build();

    @Test
    void repo() {
        try {
            util.execute("clean", "uploadArchives")
            baseLibrary.execute("clean", "uploadArchives")
            library.execute("clean", "uploadArchives")
            app.execute("clean", "uploadArchives")
        } finally {
            // clean up the test repository.
            File testRepo = new File(app.testDir, "../testrepo");
            deleteFolder(testRepo);
        }
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles()
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file)
                } else {
                    file.delete()
                }
            }
        }

        folder.delete()
    }
}
