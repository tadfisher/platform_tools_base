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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.google.common.base.Charsets;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * A Png Chunk
 */
class Chunk {

    @NonNull
    private final byte[] mType;
    @Nullable
    private final byte[] mData;
    private final long mCrc32;

    @VisibleForTesting
    Chunk(@NonNull byte[] type, @Nullable byte[] data, long crc32) {
        checkNotNull(type);
        checkArgument(type.length == 4);

        mType = type;
        mData = data;
        mCrc32 = crc32;
    }

    Chunk(@NonNull byte[] type, @Nullable byte[] data) {
        this(type, data, computeCrc32(type, data));
    }

    Chunk(@NonNull byte[] type) {
        this(type, null);
    }

    int getLength() {
        return mData != null ? mData.length : 0;
    }

    @NonNull
    public byte[] getType() {
        return mType;
    }

    public String getTypeAsString() {
        return new String(mType, Charsets.US_ASCII);
    }

    @Nullable
    public byte[] getData() {
        return mData;
    }

    public long getCrc32() {
        return mCrc32;
    }

    private static long  computeCrc32(@NonNull byte[] type, @Nullable byte[] data) {
        CRC32 checksum = new CRC32();
        checksum.update(type);
        if (data != null) {
            checksum.update(data);
        }

        return checksum.getValue();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Chunk chunk = (Chunk) o;

        if (mCrc32 != chunk.mCrc32) {
            return false;
        }
        if (!Arrays.equals(mData, chunk.mData)) {
            return false;
        }
        if (!Arrays.equals(mType, chunk.mType)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(mType);
        result = 31 * result + (mData != null ? Arrays.hashCode(mData) : 0);
        result = 31 * result + (int) (mCrc32 ^ (mCrc32 >>> 32));
        return result;
    }

    @Override
    public String toString() {
        if (Arrays.equals(mType, PngWriter.IHDR)) {
            ByteBuffer buffer = ByteBuffer.wrap(mData);
            return "Chunk{" +
                    "mType=" + getTypeAsString() +
                    ", mData=" + buffer.getInt() + "x" + buffer.getInt() + ":" + buffer.get() +
                            "-" + buffer.get() + "-" + buffer.get() + "-" + buffer.get() +
                            "-" + buffer.get() +
                    '}';
        }

        return "Chunk{" +
                "mType=" + getTypeAsString() +
                (getLength() <= 200 ? ", mData=" + getArray() : "") +
                ", mData-Length=" + getLength() +
                '}';
    }

    private String getArray() {
        int len = getLength();
        StringBuilder sb = new StringBuilder(len * 2);
        if (mData != null) {
            for (int i = 0 ; i < len ; i++) {
                sb.append(String.format("%02X", mData[i]));
            }
        }

        return sb.toString();
    }
}
