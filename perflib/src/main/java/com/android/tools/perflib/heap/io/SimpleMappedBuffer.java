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

package com.android.tools.perflib.heap.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

class SimpleMappedBuffer extends HprofBuffer {

    ByteBuffer mByteBuffer;

    SimpleMappedBuffer(File f) throws IOException {
        FileInputStream inputStream = new FileInputStream(f);
        try {
            mByteBuffer = inputStream.getChannel()
                    .map(FileChannel.MapMode.READ_ONLY, 0, f.length());
            mByteBuffer.order(ByteOrder.BIG_ENDIAN);
        } finally {
            inputStream.close();
        }
    }

    @Override
    public byte readByte() {
        return mByteBuffer.get();
    }

    @Override
    public void read(byte[] b) {
        mByteBuffer.get(b);
    }

    @Override
    public char readChar() {
        return mByteBuffer.getChar();
    }

    @Override
    public short readShort() {
        return mByteBuffer.getShort();
    }

    @Override
    public int readInt() {
        return mByteBuffer.getInt();
    }

    @Override
    public long readLong() {
        return mByteBuffer.getLong();
    }

    @Override
    public float readFloat() {
        return mByteBuffer.getFloat();
    }

    @Override
    public double readDouble() {
        return mByteBuffer.getDouble();
    }

    @Override
    public void setPosition(long position) {
        mByteBuffer.position((int) position);
    }

    @Override
    public long position() {
        return mByteBuffer.position();
    }

    @Override
    public boolean hasRemaining() {
        return mByteBuffer.hasRemaining();
    }

    @Override
    public long remaining() {
        return mByteBuffer.remaining();
    }
}
