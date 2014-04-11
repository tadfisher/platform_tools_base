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

package com.android.sdklib.internal.repository.archives;

import com.android.annotations.NonNull;

/** The Architecture that this archive can be downloaded on. */
public enum BitSize {
    _32(32),
    _64(64);

    private final int mSize;

    private BitSize(@NonNull int size) {
        mSize = size;
    }

    /** Returns the size of the architecture. */
    public int getSize() {
        return mSize;
    }

    /** Returns the XML name of the bit size. */
    @NonNull
    public String getXmlName() {
        return Integer.toString(mSize);
    }
}
