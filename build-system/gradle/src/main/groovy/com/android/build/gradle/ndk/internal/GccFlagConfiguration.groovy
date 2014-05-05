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
 * Flag configuration for GCC toolchain.
 */
class GccFlagConfiguration implements FlagConfiguration{
    private static final List<String> RELEASE_CFLAGS = [
            "-MMD",
            "-MP",
            "-fpic",
            "-ffunction-sections",
            "-funwind-tables",
            "-fstack-protector",
            "-g",
            "-DNDEBUG",
            "-fomit-frame-pointer",
            "-Wa,--noexecstack",
            "-Wformat",
            "-Werror=format-security"
    ]
    private static final List<String> DEBUG_CFLAGS = [
            "-O0",
            "-UNDEBUG",
            "-fno-omit-frame-pointer",
            "-fno-strict-aliasing",
    ]

    private static final def PLATFORM_CFLAGS = [
            (SdkConstants.ABI_ARMEABI) : [
                    "-fpic",
                    "-mthumb",
                    "-Os",
                    "-fno-strict-aliasing",
                    "-finline-limit=64",
                    "-march=armv5te",
                    "-mtune=xscale",
                    "-msoft-float",
            ],
            (SdkConstants.ABI_ARMEABI_V7A) : [
                    "-fpic",
                    "-Os",
                    "-fno-strict-aliasing",
                    "-finline-limit=64",
                    "-march=armv7-a",
                    "-mfpu=vfpv3-d16",
                    "-mfloat-abi=softfp",
                    "-mthumb",
            ],
            (SdkConstants.ABI_INTEL_ATOM) : [
                    "-O2",
                    "-fstrict-aliasing",
                    "-funswitch-loops",
                    "-finline-limit=300",

            ],
            (SdkConstants.ABI_MIPS) : [
                    "-fpic",
                    "-fno-strict-aliasing",
                    "-finline-functions",
                    "-fmessage-length=0",
                    "-fno-inline-functions-called-once",
                    "-fgcse-after-reload",
                    "-frerun-cse-after-loop",
                    "-frename-registers",
                    "-O2",
                    "-funswitch-loops",
                    "-finline-limit=300",
            ]
    ]

    /*
   -Wl,-soname,libsanangeles.so
-shared
--sysroot=/usr/local/google/home/chiur/dev/android-ndk-r9d/platforms/android-19/arch-arm
/usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/build/ndk/arm/debug/obj/local/armeabi-v7a/objs-debug/sanangeles//usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/src/main/jni/app-android.o
/usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/build/ndk/arm/debug/obj/local/armeabi-v7a/objs-debug/sanangeles//usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/src/main/jni/demo.o
/usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/build/ndk/arm/debug/obj/local/armeabi-v7a/objs-debug/sanangeles//usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/src/main/jni/importgl.o
-lgcc
-no-canonical-prefixes
-march=armv7-a
-Wl,--fix-cortex-a8

-Wl,--no-undefined
-Wl,-z,noexecstack
-Wl,-z,relro
-Wl,-z,now

-L/usr/local/google/home/chiur/dev/android-ndk-r9d/platforms/android-19/arch-arm/usr/lib
-lGLESv1_CM
-ldl
-llog
-lc
-lm
-o
/usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/build/ndk/arm/debug/obj/local/armeabi-v7a/libsanangeles.so
*/

    private static final List<String> LDFLAGS = []

    private Platform platform
    private boolean isDebugBuild

    GccFlagConfiguration(BuildType buildType, Platform platform) {
        this.isDebugBuild = (buildType.name.equals(BuilderConstants.DEBUG))
        this.platform = platform
    }

    List<String> getCFlags() {
        RELEASE_CFLAGS + PLATFORM_CFLAGS[platform.name] + (isDebugBuild ? DEBUG_CFLAGS : [])
    }
    List<String> getCppFlags() {
        getCFlags()
    }
    List<String> getLdFlags() {
        LDFLAGS
    }
}
