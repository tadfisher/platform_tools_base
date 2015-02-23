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

import javax.xml.bind.DatatypeConverter;
import java.util.HashSet;
import java.util.Set;

public class HandleTest extends TestCase {
  static final byte[] handleBytes = { -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18 };

  public void testHandleEquality() {
    // Check handle identity.
    Handle handle1 = new Handle(handleBytes);
    assertEquals(handle1, handle1);

    // Check equality of two handles created with the same bytes.
    Handle handle2 = new Handle(handleBytes);
    assertEquals(handle1, handle2);
  }

  public void testHandleNonEquality() {
    Handle handle = new Handle(handleBytes);
    assertFalse(handle.equals(null));

    // Check that we're getting a different handle than the zero-bytes handle.
    Handle zeroHandle = new Handle(new byte[handleBytes.length]);
    assertNotSame(zeroHandle, handle);

    // Check that we're getting a different handle if only the last byte differs.
    byte[] handleLastDiffBytes = new byte[handleBytes.length];
    System.arraycopy(handleBytes, 0, handleLastDiffBytes, 0, handleBytes.length);
    handleLastDiffBytes[handleLastDiffBytes.length-1]++;
    Handle handleLastDiff = new Handle(handleLastDiffBytes);
    assertNotSame(handleLastDiff, handle);

    // Check that we're getting a different handle if only the first byte differs.
    byte[] handleFirstDiffBytes = new byte[handleBytes.length];
    System.arraycopy(handleBytes, 0, handleFirstDiffBytes, 0, handleBytes.length);
    handleLastDiffBytes[0]++;
    Handle handleFirstDiff = new Handle(handleFirstDiffBytes);
    assertNotSame(handleFirstDiff, handle);
  }

  public void testHandleToString() {
    Handle handle = new Handle(handleBytes);

    StringBuilder sb = new StringBuilder();
    for (byte b : handleBytes) {
      sb.append(String.format("%02x", b&0xff));
    }
    assertEquals(sb.toString(), handle.toString());
  }

  public void testStringToHandle() {
    String hex = "0123456789abcdef0123456789abcdef01234567";
    Handle handle = new Handle(DatatypeConverter.parseHexBinary(hex));
    assertEquals(handle.toString(), hex);
  }

  public void testHandleAsKey() {
    Set<Handle> set = new HashSet<Handle>();

    Handle handle1 = new Handle(handleBytes);
    set.add(handle1);
    assertTrue(set.contains(handle1));
    assertEquals(set.size(), 1);

    Handle handle2 = new Handle(handleBytes);
    set.add(handle2);
    assertTrue(set.contains(handle2));
    assertEquals(set.size(), 1);

    Handle handle3 = new Handle(new byte[20]);
    set.add(handle3);
    assertTrue(set.contains(handle3));
    assertEquals(set.size(), 2);
  }
}
