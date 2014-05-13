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

public enum Abi {
    ARMEABI(SdkConstants.ABI_ARMEABI),
    ARMEABI_V7A(SdkConstants.ABI_ARMEABI_V7A),
    ARM64_V8A(SdkConstants.ABI_ARM64_V8A),
    X86(SdkConstants.ABI_INTEL_ATOM),
    X86_64(SdkConstants.ABI_INTEL_ATOM64),
    MIPS(SdkConstants.ABI_MIPS),
    MIPS64(SdkConstants.ABI_MIPS64);

    @NonNull private final String mValue;

    Abi(@NonNull String value) {
        mValue = value;
    }

    @Nullable
    public static Abi getEnum(@NonNull String value) {
        for (Abi a : values()) {
            if (a.mValue.equals(value)) {
                return a;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return mValue;
    }
}
