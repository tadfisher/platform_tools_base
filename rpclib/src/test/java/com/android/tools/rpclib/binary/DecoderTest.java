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

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DecoderTest extends TestCase {
  public void testDecodeBool() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x01, (byte)0x00
    });
    final boolean[] expected = new boolean[]{true, false};

    Decoder d = new Decoder(input);
    try {
      for (boolean bool : expected) {
        assertEquals(bool, d.bool());
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeInt8() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x00, (byte)0x7f, (byte)0x80, (byte)0xff
    });
    final byte[] expected = new byte[]{0, 127, -128, -1};

    Decoder d = new Decoder(input);
    try {
      for (short s8 : expected) {
        assertEquals(s8, d.int8());
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeUint8() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x00, (byte)0x7f, (byte)0x80, (byte)0xff
    });
    final short[] expected = new short[]{0x00, 0x7f, 0x80, 0xff};

    Decoder d = new Decoder(input);
    try {
      for (short u8 : expected) {
        assertEquals(u8, d.uint8() & 0xff);
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeInt16() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x00, (byte)0x00,
      (byte)0xff, (byte)0x7f,
      (byte)0x00, (byte)0x80,
      (byte)0xff, (byte)0xff
    });
    final short[] expected = new short[]{0, 32767, -32768, -1};

    Decoder d = new Decoder(input);
    try {
      for (short s16 : expected) {
        assertEquals(s16, d.int16());
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeUint16() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x00, (byte)0x00,
      (byte)0xef, (byte)0xbe,
      (byte)0xde, (byte)0xc0
    });
    final int[] expected = new int[]{0, 0xbeef, 0xc0de};

    Decoder d = new Decoder(input);
    try {
      for (int u16 : expected) {
        assertEquals(u16, d.uint16() & 0xffff);
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeInt32() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
      (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f,
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x80,
      (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff
    });
    final int[] expected = new int[]{0, 2147483647, -2147483648, -1};

    Decoder d = new Decoder(input);
    try {
      for (int s32 : expected) {
        assertEquals(s32, d.int32());
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeUint32() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
      (byte)0x67, (byte)0x45, (byte)0x23, (byte)0x01,
      (byte)0xef, (byte)0xcd, (byte)0xab, (byte)0x10
    });
    final long[] expected = new long[]{0, 0x01234567, 0x10abcdef};

    Decoder d = new Decoder(input);
    try {
      for (long u32 : expected) {
        assertEquals(u32, d.uint32() & 0xffffffff);
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeInt64() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,

      (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
      (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f,

      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x80,

      (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
      (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff
    });
    final long[] expected = new long[]{0L, 9223372036854775807L, -9223372036854775808L, -1L};

    Decoder d = new Decoder(input);
    try {
      for (long s64 : expected) {
        assertEquals(s64, d.int64());
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeUint64() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,

      (byte)0xef, (byte)0xcd, (byte)0xab, (byte)0x89,
      (byte)0x67, (byte)0x45, (byte)0x23, (byte)0x01,

      (byte)0x10, (byte)0x32, (byte)0x54, (byte)0x76,
      (byte)0x98, (byte)0xba, (byte)0xdc, (byte)0xfe
    });
    final long[] expected = new long[]{0L, 0x0123456789abcdefL, 0xfedcba9876543210L};

    Decoder d = new Decoder(input);
    try {
      for (long u64 : expected) {
        assertEquals(u64, d.uint64());
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeFloat32() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
      (byte)0x00, (byte)0x00, (byte)0x80, (byte)0x3f,
      (byte)0x00, (byte)0x00, (byte)0x81, (byte)0x42,
    });
    final float[] expected = new float[]{0.F, 1.F, 64.5F};

    Decoder d = new Decoder(input);
    try {
      for (float f32 : expected) {
        assertEquals(f32, d.float32());
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeFloat64() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,

      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
      (byte)0x00, (byte)0x00, (byte)0xf0, (byte)0x3f,

      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
      (byte)0x00, (byte)0x20, (byte)0x50, (byte)0x40
    });
    final double[] expected = new double[]{0.D, 1.D, 64.5D};

    Decoder d = new Decoder(input);
    try {
      for (double f64 : expected) {
        assertEquals(f64, d.float64());
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeString() {
    final ByteArrayInputStream input = new ByteArrayInputStream(new byte[]{
      0x05, 0x00, 0x00, 0x00, 'H', 'e', 'l', 'l', 'o',
      0x00, 0x00, 0x00, 0x00, // empty string
      0x05, 0x00, 0x00, 0x00, 'W', 'o', 'r', 'l', 'd',

      0x15, 0x00, 0x00, 0x00,
      (byte)0xe3, (byte)0x81, (byte)0x93, (byte)0xe3, (byte)0x82, (byte)0x93, (byte)0xe3,
      (byte)0x81, (byte)0xab, (byte)0xe3, (byte)0x81, (byte)0xa1, (byte)0xe3, (byte)0x81,
      (byte)0xaf, (byte)0xe4, (byte)0xb8, (byte)0x96, (byte)0xe7, (byte)0x95, (byte)0x8c
    });
    final String[] expected = new String[]{"Hello", "", "World", "こんにちは世界"};

    Decoder d = new Decoder(input);
    try {
      for (String str : expected) {
        assertEquals(str, d.string());
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }

  public void testDecodeObject() {
    final byte[] stubObjectTypeIDBytes = new byte[]{
      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09
    };
    class DummyObject implements BinaryObject {
      final String dummy = "dummy";
      @Override
      public ObjectTypeID type() {
        return new ObjectTypeID(stubObjectTypeIDBytes);
      }
      @Override
      public void decode(@NotNull Decoder d) throws IOException {
        assert d.string().equals(dummy);
      }
      @Override
      public void encode(@NotNull Encoder e) throws IOException {
        e.string(dummy);
      }
    }
    final BinaryObject dummyObject = new DummyObject();

    class DummyObjectCreator implements BinaryObjectCreator {
      @Override public BinaryObject create() {
        return dummyObject;
      }
    }
    final BinaryObjectCreator dummyObjectCreator = new DummyObjectCreator();
    ObjectTypeID.register(dummyObject.type(), dummyObjectCreator);

    ByteArrayOutputStream inputBytes = new ByteArrayOutputStream();
    try {
      // null BinaryObject:
      inputBytes.write(new byte[]{(byte)0xff, (byte)0xff}); // BinaryObject.NULL_ID

      // stubObject:
      inputBytes.write(new byte[]{0x00, 0x00}); // stubObject reference
      inputBytes.write(stubObjectTypeIDBytes); // stubObject.type()
      inputBytes.write(new byte[]{0x05, 0x00, 0x00, 0x00, 'd', 'u', 'm', 'm', 'y'});

      // stubObject again, only by reference this time:
      inputBytes.write(new byte[]{0x00, 0x00}); // stubObject reference
    }
    catch (IOException ex) {
      assertNull(ex);
    }

    final ByteArrayInputStream input = new ByteArrayInputStream(inputBytes.toByteArray());
    final BinaryObject[] expected = new BinaryObject[]{null, dummyObject, dummyObject};

    Decoder d = new Decoder(input);
    try {
      for (BinaryObject obj : expected) {
        assertEquals(obj, d.object());
      }
    }
    catch (IOException ex) {
      assertNull(ex);
    }
  }
}
