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

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 */
public class NinePatchChunk {

    private static final byte[] sChunkType = new byte[] { 'n', 'p', 'T', 'c' };

    private byte mNumXDivs;
    private byte mNumYDivs;
    private byte mNumColors;

    byte[] mXDivs;
    byte[] mYDivs;

    NinePatchChunk(int[] xDivs, int[] yDivs, int numXDivs, int numYDivs, int numColors) {
        mNumXDivs = (byte) numXDivs;
        mNumYDivs = (byte) numYDivs;
        mNumColors = (byte) numColors;

        // fill the bytes array from the int array
        mXDivs = intToByteArray(xDivs);
        mYDivs = intToByteArray(yDivs);
    }

    @VisibleForTesting
    static byte[] intToByteArray(int[] array) {
        byte[] byteArray = new byte[array.length * 4];

        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();

        intBuffer.put(array);

        return byteArray;
    }

    @NonNull
    Chunk getChunk() {
        // TODO
        return new Chunk(sChunkType);
    }
}
