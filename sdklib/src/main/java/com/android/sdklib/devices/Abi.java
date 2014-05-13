/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.sdklib.devices;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

/**
 * ABI values that can appear in a device's xml &lt;abi> field <em>and</em>
 * in a system-image abi.
 * <p/>
 * The CPU arch and model values are used to configure an AVD using a given ABI.
 */
public enum Abi {
    ARMEABI    (SdkConstants.ABI_ARMEABI,     SdkConstants.CPU_ARCH_ARM),
    ARMEABI_V7A(SdkConstants.ABI_ARMEABI_V7A, SdkConstants.CPU_ARCH_ARM),
    ARM64_V8A  (SdkConstants.ABI_ARM64_V8A,   SdkConstants.CPU_ARCH_ARM, SdkConstants.CPU_MODEL_CORTEX_A8),
    X86        (SdkConstants.ABI_INTEL_ATOM,  SdkConstants.CPU_ARCH_INTEL_ATOM),
    X86_64     (SdkConstants.ABI_INTEL_ATOM64,SdkConstants.CPU_ARCH_INTEL_ATOM),
    MIPS       (SdkConstants.ABI_MIPS,        SdkConstants.CPU_ARCH_MIPS),
    MIPS64     (SdkConstants.ABI_MIPS64,      SdkConstants.CPU_ARCH_MIPS);

    @NonNull private final String mAbi;
    @NonNull private final String mCpuArch;
    @NonNull private final String mCpuModel;

    Abi(@NonNull String abi, @NonNull String cpu) {
        this(abi, cpu, null);
    }

    Abi(@NonNull String abi, @NonNull String cpuArch, @Nullable String cpuModel) {
        mAbi = abi;
        mCpuArch = cpuArch;
        mCpuModel = cpuModel;
    }

    @Nullable
    public static Abi getEnum(@NonNull String value) {
        for (Abi a : values()) {
            if (a.mAbi.equals(value)) {
                return a;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public String toString() {
        return mAbi;
    }

    @NonNull
    public String getCpuArch() {
        return mCpuArch;
    }

    @Nullable
    public String getCpuModel() {
        return mCpuModel;
    }
}
