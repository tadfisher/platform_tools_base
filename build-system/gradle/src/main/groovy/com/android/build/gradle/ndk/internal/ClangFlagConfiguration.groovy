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

import com.android.SdkConstants
import com.android.builder.BuilderConstants
import org.gradle.nativebinaries.BuildType
import org.gradle.nativebinaries.platform.Platform

/**
 * Flag configuration for Clang toolchain.
 */
class ClangFlagConfiguration implements FlagConfiguration{
    private NdkBuilder ndkBuilder
    private Platform platform
    private boolean isDebugBuild

    private TARGET = [
            (SdkConstants.ABI_INTEL_ATOM) : "i686-none-linux-android",
            (SdkConstants.ABI_ARMEABI_V7A) : "armv7-none-linux-android",
            (SdkConstants.ABI_ARMEABI) : "armv5-none-linux-android",
            (SdkConstants.ABI_MIPS) : "mips-none-linux-android",
    ]

    ClangFlagConfiguration(NdkBuilder ndkBuilder, BuildType buildType, Platform platform) {
        this.ndkBuilder = ndkBuilder
        this.isDebugBuild = (buildType.name.equals(BuilderConstants.DEBUG))
        this.platform = platform
    }

    List<String> getCFlags() {
        [
                "-gcc-toolchain",
                ndkBuilder.getToolchainPath("gcc", "4.8", platform.name),
//                "/usr/local/google/home/chiur/dev/android-ndk-r9d/toolchains/arm-linux-androideabi-4.8/prebuilt/linux-x86_64",
                "-target",
                TARGET[platform.name],
                "-march=armv7-a",
                "-mfloat-abi=softfp",
                "-mfpu=vfpv3-d16",
                "-mthumb",
        ]
    }
    List<String> getCppFlags() {
        getCFlags()
    }
    List<String> getLdFlags() {
        [
                "-gcc-toolchain",
                ndkBuilder.getToolchainPath("gcc", "4.8", platform.name),
//                "/usr/local/google/home/chiur/dev/android-ndk-r9d/toolchains/arm-linux-androideabi-4.8/prebuilt/linux-x86_64",
                "-target",
                TARGET[platform.name],
        ]
    }
}
