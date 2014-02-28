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
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * PNG Reader
 */
public class PngWriter {

    private static final Chunk sIend = new Chunk(new byte[] { 'I', 'E', 'N', 'D' });

    public static final byte[] SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };


    public static final byte[] IDAT = new byte[] { 'I', 'D', 'A', 'T' };
    public static final byte[] IHDR = new byte[] { 'I', 'H', 'D', 'R' };


    @NonNull
    private final File mToFile;

    @NonNull
    private final ByteUtils mByteUtils;

    private Chunk mIhdr;
    private final List<Chunk> mChunks = Lists.newArrayList();

    public PngWriter(@NonNull File toFile, @NonNull ByteUtils byteUtils) {
        mToFile = toFile;
        mByteUtils = byteUtils;
    }

    public PngWriter setIhdr(@NonNull Chunk chunk) {
        mIhdr = chunk;
        return this;
    }

    public PngWriter setChunk(@NonNull Chunk chunk) {
        mChunks.add(chunk);
        return this;
    }

    public PngWriter setChunks(@NonNull List<Chunk> chunks) {
        mChunks.addAll(chunks);
        return this;
    }

    public void write() throws IOException {
        int len = computeSize();

        byte[] buffer = new byte[len];
        // copy the sig
        System.arraycopy(SIGNATURE, 0, buffer, 0, 8);

        int index = writeChunk(mIhdr, buffer, 8);

        for (Chunk chunk : mChunks) {
            index = writeChunk(chunk, buffer, index);
        }

        writeChunk(sIend, buffer, index);

        Files.write(buffer, mToFile);
    }

    private int writeChunk(
            @NonNull Chunk chunk,
            @NonNull byte[] buffer, int index) {
        // write the chunk length
        int length = chunk.getLength();

        // TODO: fix?
        System.arraycopy(
                mByteUtils.getIntAsArray(length),
                0,
                buffer, index, 4);
        index +=4;

        System.arraycopy(chunk.getType(), 0, buffer, index, 4);
        index += 4;

        if (length != 0 && chunk.getData() != null) {
            System.arraycopy(chunk.getData(), 0, buffer, index, length);
            index += length;
        }

        System.arraycopy(
                mByteUtils.getLongAsIntArray(chunk.getCrc32()),
                4,
                buffer, index, 4);
        index += 4;

        return index;
    }

    private int computeSize() {
        int len = 8; // sig length

        len += mIhdr.getLength() + 4 + 4 + 4;

        for (Chunk chunk : mChunks) {
            len += chunk.getLength() + 4 + 4 + 4;
        }

        len += sIend.getLength() + 4 + 4 + 4;

        return len;
    }
}
