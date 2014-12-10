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

package com.android.build.gradle.integration.application

import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.google.common.io.Files
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES
import static com.google.common.base.Charsets.UTF_8
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Assemble tests for privateResources.
 */
class PrivateResourceTest {
    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("privateResources")
            .create()

    @BeforeClass
    static void setup() {
        project.execute("clean", "assembleRelease", "assembleDebug");
    }

    @AfterClass
    static void cleanUp() {
        project = null
    }

    @Test
    void "check private resources resources"() {
        File debugPublic = project.file("mylibrary/build/" + FD_INTERMEDIATES + "/bundles/debug/public.txt");
        File debugRelease = project.file("mylibrary/build/" + FD_INTERMEDIATES + "/bundles/release/public.txt");
        assertTrue(debugPublic.getPath(), debugPublic.isFile());
        assertTrue(debugRelease.getPath(), debugRelease.isFile());

        String expected = """\
string mylib_app_name
string mylib_public_string
"""
        assertEquals(expected, Files.toString(debugPublic, UTF_8))
        assertEquals(expected, Files.toString(debugPublic, UTF_8))
    }
}
