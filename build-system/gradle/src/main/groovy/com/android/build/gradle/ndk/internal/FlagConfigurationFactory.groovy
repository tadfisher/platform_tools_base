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

import org.gradle.nativebinaries.BuildType
import org.gradle.nativebinaries.platform.Platform

/**
 * Factory to create FlagConfiguration
 */
class FlagConfigurationFactory {
    /**
     * Returns a FlagConfiguration.
     *
     * @param buildType Build type of the native binary.
     * @param platform Target platform of the native binary.
     * @return A FlagConfiguration for the targeted native binary.
     */
    public static FlagConfiguration create(BuildType buildType, Platform platform, NdkBuilder ndkBuilder) {
        return (ndkBuilder.getNdkConfig().getToolchain().equals("gcc")
                ? new GccFlagConfiguration(buildType, platform)
                : new ClangFlagConfiguration(ndkBuilder, buildType, platform))
    }
}
