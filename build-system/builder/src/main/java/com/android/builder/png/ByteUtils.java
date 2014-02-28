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

package com.android.builder.png;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 */
public class ByteUtils {

    private final ByteBuffer mIntBuffer;
    private final ByteBuffer mLongBuffer;

    public ByteUtils() {
        // ByteBuffer for endian-ness conversion
        mIntBuffer = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
        mLongBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
    }

    public byte[] getLongAsIntArray(long value) {
        return ((ByteBuffer) mLongBuffer.rewind()).putLong(value).array();
    }

    public byte[] getIntAsArray(int value) {
        byte[] array = ((ByteBuffer) mIntBuffer.rewind()).putInt(value).array();
        return array;
    }
}
