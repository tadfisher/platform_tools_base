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

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A binary resource handle. These are used as 20-bytes-unique identifiers and
 * often returned by RPC calls where retrieving the data can be a lengthy process.
 * For each {@link Handle} type there will be a corresponding Resolve RPC function
 * that fetches the resource data from the server.
 */
public class Handle {
  public static final int SIZE = 20;
  private final byte[] mValue = new byte[SIZE];

  public Handle(byte[] value) {
    assert value.length == SIZE;
    System.arraycopy(mValue, 0, value, 0, SIZE);
  }

  public Handle(Decoder d) throws IOException {
    assert d.stream().read(mValue) == SIZE;
  }

  public void encode(Encoder e) throws IOException {
    e.stream().write(mValue);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) return false;
    if (other == this) return true;
    if (!(other instanceof Handle)) return false;
    return Arrays.equals(mValue, ((Handle)other).mValue);
  }

  @Override
  public String toString() {
    return String.format("%0" + SIZE + "x", new BigInteger(mValue));
  }

  @Override
  public int hashCode() {
    return ByteBuffer.wrap(mValue).getInt();
  }
}
