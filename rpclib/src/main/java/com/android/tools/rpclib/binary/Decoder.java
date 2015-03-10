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
package com.android.tools.rpclib.binary;

import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * A decoder of various RPC primitive types.
 */
public class Decoder {
  @NotNull private final TIntObjectHashMap<BinaryObject> mDecodedMap;
  @NotNull private final InputStream mInputStream;
  @NotNull private final byte[] mBuffer;

  public Decoder(@NotNull InputStream in) {
    mDecodedMap = new TIntObjectHashMap<BinaryObject>();
    mInputStream = in;
    mBuffer = new byte[9];
  }

  public void read(byte[] buf, int count) throws IOException {
    int off = 0;
    while (off < count) {
      off += mInputStream.read(buf, off, count - off);
    }
  }

  private void read(int count) throws IOException {
    read(mBuffer, count);
  }

  public boolean bool() throws IOException {
    read(1);
    return mBuffer[0] != 0;
  }

  public byte int8() throws IOException {
    read(1);
    return mBuffer[0];
  }

  public byte uint8() throws IOException {
    return int8();
  }

  public short int16() throws IOException {
    short uv = uint16();
    int v = (uv >> 1) & 0x7fff;
    if ((uv & 1) != 0) {
      v = ~v;
    }
    return (short)v;
  }

  public short uint16() throws IOException {
    read(1);
    int count = 0;
    for (int tag = (mBuffer[0] & 0xff); (tag & 0x80) != 0; tag <<= 1) count += 1;
    int v = mBuffer[0] & (0xff >> count);
    if (count == 0) {
      return (short)v;
    }
    read(count);
    for (int i = 0; i < count; i++) {
      v = (v << 8) | (mBuffer[i] & 0xff);
    }
    return (short)v;
  }

  public int int32() throws IOException {
    int uv = uint32();
    int v = uv >>> 1;
    if ((uv & 1) != 0) {
      v = ~v;
    }
    return v;
  }

  public int uint32() throws IOException {
    read(1);
    int count = 0;
    for (int tag = (mBuffer[0] & 0xff); (tag & 0x80) != 0; tag <<= 1) count += 1;
    int v = mBuffer[0] & (0xff >> count);
    if (count == 0) {
      return v;
    }
    read(count);
    for (int i = 0; i < count; i++) {
      v = (v << 8) | (mBuffer[i] & 0xff);
    }
    return v;
  }

  public long int64() throws IOException {
    long uv = uint64();
    long v = uv >>> 1;
    if ((uv & 1) != 0) {
      v = ~v;
    }
    return v;
  }

  public long uint64() throws IOException {
    read(1);
    int count = 0;
    for (int tag =( mBuffer[0] & 0xff); (tag & 0x80) != 0; tag <<= 1) count += 1;
    long v = mBuffer[0] & (0xff >>> count);
    if (count == 0) {
      return v;
    }
    read(count);
    for (int i = 0; i < count; i++) {
      v = (v << 8) | (mBuffer[i] & 0xffL);
    }
    return v;
  }

  public float float32() throws IOException {
    int bits = uint32();
    int shuffled = ((bits & 0x000000ff) <<  24) |
                   ((bits & 0x0000ff00) <<   8) |
                   ((bits & 0x00ff0000) >>   8) |
                   ((bits & 0xff000000) >>> 24);
    return Float.intBitsToFloat(shuffled);
  }

  public double float64() throws IOException {
    long bits = uint64();
    long shuffled = ((bits & 0x00000000000000ffL) <<  56) |
                    ((bits & 0x000000000000ff00L) <<  40) |
                    ((bits & 0x0000000000ff0000L) <<  24) |
                    ((bits & 0x00000000ff000000L) <<   8) |
                    ((bits & 0x000000ff00000000L) >>   8) |
                    ((bits & 0x0000ff0000000000L) >>  24) |
                    ((bits & 0x00ff000000000000L) >>  40) |
                    ((bits & 0xff00000000000000L) >>> 56);
    return Double.longBitsToDouble(shuffled);
  }

  public String string() throws IOException {
    int size = uint32();
    byte[] bytes = new byte[size];
    for (int i = 0; i < size; i++) {
      bytes[i] = int8();
    }
    try {
      return new String(bytes, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e); // Should never happen
    }
  }

  @Nullable
  public BinaryObject object() throws IOException {
    int key = uint32();

    if (key == BinaryObject.NULL_ID) {
      return null;
    }

    BinaryObject obj = mDecodedMap.get(key);
    if (obj != null) {
      return obj;
    }

    ObjectTypeID type = new ObjectTypeID(this);
    BinaryObjectCreator creator = ObjectTypeID.lookup(type);
    if (creator == null) {
      throw new RuntimeException("Unknown type id encountered: " + type);
    }
    obj = creator.create();
    obj.decode(this);

    mDecodedMap.put(key, obj);
    return obj;
  }

  public InputStream stream() {
    return mInputStream;
  }
}
