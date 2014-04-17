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

package com.android.build.gradle.ndk.internal

import com.android.builder.BuilderConstants
import org.gradle.nativebinaries.BuildType
import org.gradle.nativebinaries.platform.Platform

/**
 * Flag configuration for GCC toolchain.
 */
class GccFlagConfiguration implements FlagConfiguration{
    private static final List<String> RELEASE_CFLAGS = [
            "-ffunction-sections",
            "-funwind-tables",
            "-fstack-protector",
            "-O2",
            "-g",
            "-DNDEBUG",
            "-fomit-frame-pointer",
            "-fstrict-aliasing",
            "-funswitch-loops",
            "-finline-limit=300",
    ]
    private static final List<String> DEBUG_CFLAGS = RELEASE_CFLAGS + [
            "-O0",
            "-UNDEBUG",
            "-fno-omit-frame-pointer",
            "-fno-strict-aliasing",
    ]
    private static final List<String> RELEASE_CPPFLAGS = RELEASE_CFLAGS
    private static final List<String> DEBUG_CPPFLAGS = DEBUG_CFLAGS
    private static final List<String> LDFLAGS = []

    private Platform targetPlatform
    private boolean isDebugBuild

    GccFlagConfiguration(BuildType buildType) {
        this.isDebugBuild = (buildType.name.equals(BuilderConstants.DEBUG))
    }

    List<String> getCFlags() {
        isDebugBuild ? DEBUG_CFLAGS : RELEASE_CFLAGS
    }
    List<String> getCppFlags() {
        isDebugBuild ? DEBUG_CPPFLAGS : RELEASE_CPPFLAGS
    }
    List<String> getLdFlags() {
        LDFLAGS
    }
}
