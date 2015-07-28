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

package com.android.tools.perflib.heap.hprof;

import java.io.IOException;

public class HprofUnloadClass implements HprofRecord {
  public static final int TAG = 0x03;
  public final long time;
  public final long classSerialNumber;          // u4

  public HprofUnloadClass(long time, long classSerialNumber) {
    this.time = time;
    this.classSerialNumber = classSerialNumber;
  }

  public void write(HprofOutputStream hprof) throws IOException {
    int u4 = 4;
    hprof.writeRecordHeader(TAG, time, u4);
    hprof.writeU4(classSerialNumber);
  }
}
