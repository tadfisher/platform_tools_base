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

package com.android.build.gradle.ndk.internal;

import com.android.SdkConstants;
import com.android.builder.core.BuilderConstants;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import org.gradle.nativeplatform.BuildType;
import org.gradle.nativeplatform.platform.NativePlatform;

import java.util.List;
import java.util.Map;

/**
 * Flag configuration for GCC toolchain.
 */
public class GccNativeToolSpecification extends AbstractNativeToolSpecification {

    private static final Map<String, List<String>> RELEASE_CFLAGS;
    static {
        RELEASE_CFLAGS = ImmutableMap.<String, List<String>>builder()
                .put(SdkConstants.ABI_ARMEABI, ImmutableList.of(
                        "-fpic",
                        "-ffunction-sections",
                        "-funwind-tables",
                        "-fstack-protector",
                        "-no-canonical-prefixes",
                        "-march=armv5te",
                        "-mtune=xscale",
                        "-msoft-float",
                        "-mthumb",
                        "-Os",
                        "-g",
                        "-DNDEBUG",
                        "-fomit-frame-pointer",
                        "-fno-strict-aliasing",
                        "-finline-limit=64"))
                .put(SdkConstants.ABI_ARMEABI_V7A, ImmutableList.of(
                        "-fpic",
                        "-ffunction-sections",
                        "-funwind-tables",
                        "-fstack-protector",
                        "-no-canonical-prefixes",
                        "-march=armv7-a",
                        "-mfpu=vfpv3-d16",
                        "-mfloat-abi=softfp",
                        "-mthumb",
                        "-Os",
                        "-g",
                        "-DNDEBUG",
                        "-fomit-frame-pointer",
                        "-fno-strict-aliasing",
                        "-finline-limit=64"))
                .put(SdkConstants.ABI_ARM64_V8A, ImmutableList.of(
                        "-fpic",
                        "-ffunction-sections",
                        "-funwind-tables",
                        "-fstack-protector",
                        "-no-canonical-prefixes",
                        "-O2",
                        "-g",
                        "-DNDEBUG",
                        "-fomit-frame-pointer",
                        "-fstrict-aliasing",
                        "-funswitch-loops",
                        "-finline-limit=300"))
                .put(SdkConstants.ABI_INTEL_ATOM, ImmutableList.of(
                        "-ffunction-sections",
                        "-funwind-tables",
                        "-no-canonical-prefixes",
                        "-fstack-protector",
                        "-O2",
                        "-g",
                        "-DNDEBUG",
                        "-fomit-frame-pointer",
                        "-fstrict-aliasing",
                        "-funswitch-loops",
                        "-finline-limit=300"))
                .put(SdkConstants.ABI_INTEL_ATOM64, ImmutableList.of(
                        "-ffunction-sections",
                        "-funwind-tables",
                        "-no-canonical-prefixes",
                        "-fstack-protector",
                        "-O2",
                        "-g",
                        "-DNDEBUG",
                        "-fomit-frame-pointer",
                        "-fstrict-aliasing",
                        "-funswitch-loops",
                        "-finline-limit=300"))
                .put(SdkConstants.ABI_MIPS, ImmutableList.of(
                        "-fpic",
                        "-fno-strict-aliasing",
                        "-finline-functions",
                        "-ffunction-sections",
                        "-funwind-tables",
                        "-fmessage-length=0",
                        "-fno-inline-functions-called-once",
                        "-fgcse-after-reload",
                        "-frerun-cse-after-loop",
                        "-frename-registers",
                        "-no-canonical-prefixes",
                        "-O2",
                        "-g",
                        "-DNDEBUG",
                        "-fomit-frame-pointer",
                        "-funswitch-loops",
                        "-finline-limit=300"))
                .put(SdkConstants.ABI_MIPS64, ImmutableList.of(
                        "-fpic",
                        "-fno-strict-aliasing",
                        "-finline-functions",
                        "-ffunction-sections",
                        "-funwind-tables",
                        "-fmessage-length=0",
                        "-fno-inline-functions-called-once",
                        "-fgcse-after-reload",
                        "-frerun-cse-after-loop",
                        "-frename-registers",
                        "-no-canonical-prefixes",
                        "-O2",
                        "-g",
                        "-DNDEBUG",
                        "-fomit-frame-pointer",
                        "-funswitch-loops",
                        "-finline-limit=300"))
                .build();
    }


    private static final Map<String, List<String>> DEBUG_CFLAGS;
    static {
        DEBUG_CFLAGS = ImmutableMap.<String, List<String>>builder()
                .put(SdkConstants.ABI_ARMEABI, ImmutableList.of(
                        "-O0",
                        "-UNDEBUG",
                        "-fno-omit-frame-pointer",
                        "-fno-strict-aliasing"))
                .put(SdkConstants.ABI_ARMEABI_V7A, ImmutableList.of(
                        "-O0",
                        "-UNDEBUG",
                        "-fno-omit-frame-pointer",
                        "-fno-strict-aliasing"))
                .put(SdkConstants.ABI_ARM64_V8A, ImmutableList.of(
                        "-O0",
                        "-UNDEBUG",
                        "-fno-omit-frame-pointer",
                        "-fno-strict-aliasing"))
                .put(SdkConstants.ABI_INTEL_ATOM, ImmutableList.of(
                        "-O0",
                        "-UNDEBUG",
                        "-fno-omit-frame-pointer",
                        "-fno-strict-aliasing"))
                .put(SdkConstants.ABI_INTEL_ATOM64, ImmutableList.of(
                        "-O0",
                        "-UNDEBUG",
                        "-fno-omit-frame-pointer",
                        "-fno-strict-aliasing"))
                .put(SdkConstants.ABI_MIPS, ImmutableList.of(
                        "-O0",
                        "-UNDEBUG",
                        "-fno-omit-frame-pointer",
                        "-fno-unswitch-loops"))
                .put(SdkConstants.ABI_MIPS64, ImmutableList.of(
                        "-O0",
                        "-UNDEBUG",
                        "-fno-omit-frame-pointer"))
                .build();
    }

    private static final Iterable<String> LDFLAGS = ImmutableList.of("-no-canonical-prefixes");

    private NativePlatform platform;

    private boolean isDebugBuild;


    public GccNativeToolSpecification(BuildType buildType, NativePlatform platform) {
        this.isDebugBuild = (buildType.getName().equals(BuilderConstants.DEBUG));
        this.platform = platform;
    }

    @Override
    public Iterable<String> getCFlags() {
        return Iterables.concat(
                RELEASE_CFLAGS.get(platform.getName()),
                (isDebugBuild ? DEBUG_CFLAGS.get(platform.getName()) : ImmutableList.<String>of()));
    }

    @Override
    public Iterable<String> getCppFlags() {
        return getCFlags();
    }

    @Override
    public Iterable<String> getLdFlags() {
        return LDFLAGS;
    }
}
