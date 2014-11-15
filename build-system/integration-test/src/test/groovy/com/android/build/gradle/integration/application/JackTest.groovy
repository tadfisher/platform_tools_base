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

import com.android.build.gradle.integration.common.category.DeviceTests
import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.google.common.collect.ImmutableList
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category

/**
 * Test Jack integration.
 */
class JackTest {
    @ClassRule
    static public GradleTestProject basic = GradleTestProject.builder()
            .withName("basic")
            .fromSample("regular/basic")
            .create()

    static public GradleTestProject minify = GradleTestProject.builder()
            .withName("minify")
            .fromSample("regular/minify")
            .create()

    static public GradleTestProject multiDex = GradleTestProject.builder()
            .withName("multiDex")
            .fromSample("regular/multiDex")
            .create()

    @BeforeClass
    static void setup() {
        final List<String> JACK_OPTIONS = ImmutableList.of(
                "-PCUSTOM_JACK=1",
                "-PCUSTOM_BUILDTOOLS=21.1.0")
        basic.execute(JACK_OPTIONS, "clean", "assembleDebug")
        minify.execute(JACK_OPTIONS, "clean", "assembleDebug")
        multiDex.execute(JACK_OPTIONS, "clean", "assembleDebug")
    }

    @Test
    void assembleDebug() {
        // Empty test to ensure setup succeeds if DeviceTests are not run.
    }

    @Test
    @Category(DeviceTests.class)
    void "basic connectedCheck"() {
        basic.execute("connectedCheck");
    }

    @Test
    @Category(DeviceTests.class)
    void "minify connectedCheck"() {
        minify.execute("connectedCheck");
    }

    @Test
    @Category(DeviceTests.class)
    void "multiDex connectedCheck"() {
        multiDex.execute("connectedCheck");
    }
}
