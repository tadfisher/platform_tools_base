/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.build.gradle.internal.core;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

/**
 * Enum of valid ABI you can specify for NDK.
 */
public enum Abi {
    //          name                           architecture                        plaform                   gccPrefix                 supports64Bits
    ARMEABI    (SdkConstants.ABI_ARMEABI,      SdkConstants.CPU_ARCH_ARM,          "arm-linux-androideabi",  "arm-linux-androideabi",  false),
    ARMEABI_V7A(SdkConstants.ABI_ARMEABI_V7A,  SdkConstants.CPU_ARCH_ARM,          "arm-linux-androideabi",  "arm-linux-androideabi",  false),
    ARM64_V8A  (SdkConstants.ABI_ARM64_V8A,    SdkConstants.CPU_ARCH_ARM64,        "aarch64-linux-android",  "aarch64-linux-android",  true),
    X86        (SdkConstants.ABI_INTEL_ATOM,   SdkConstants.CPU_ARCH_INTEL_ATOM,   "x86",                    "i686-linux-android",     false),
    X86_64     (SdkConstants.ABI_INTEL_ATOM64, SdkConstants.CPU_ARCH_INTEL_ATOM64, "x86_64",                 "x86_64-linux-android",   true),
    MIPS       (SdkConstants.ABI_MIPS,         SdkConstants.CPU_ARCH_MIPS,         "mipsel-linux-android",   "mipsel-linux-android",   false),
    MIPS64     (SdkConstants.ABI_MIPS64,       SdkConstants.CPU_ARCH_MIPS64,       "mips64el-linux-android", "mips64el-linux-android", true);


    @NonNull
    private String name;
    @NonNull
    String architecture;
    @NonNull
    private String platform;
    @NonNull
    private String gccPrefix;
    private boolean supports64Bits;

    Abi(@NonNull String name, @NonNull String architecture,
            @NonNull String platform, @NonNull String gccPrefix, boolean supports64Bits) {
        this.name = name;
        this.architecture = architecture;
        this.platform = platform;
        this.gccPrefix = gccPrefix;
        this.supports64Bits = supports64Bits;
    }

    /**
     * Returns the ABI definition matching the given ABI code name.
     *
     * @param abi The ABI code name, used in the system-images and device definitions.
     * @return An existing {@link Abi} description or null.
     */
    @Nullable
    public static Abi getEnum(@NonNull String abi) {
        for (Abi a : values()) {
            if (a.name.equals(abi)) {
                return a;
            }
        }
        return null;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getArchitecture() {
        return architecture;
    }

    @NonNull
    public String getPlatform() {
        return platform;
    }

    @NonNull
    public String getGccPrefix() {
        return gccPrefix;
    }

    public boolean supports64Bits() {
        return supports64Bits;
    }
}

