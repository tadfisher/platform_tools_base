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
package com.android.tools.perflib.allocations;

import com.google.common.base.Charsets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Generates the .alloc files used by AllocationsParserTest and saves them in the home directory.
 */
@SuppressWarnings("unused")
public class AllocationsParserTestFileCreator {
  private static final String HOME = System.getProperty("user.home");

  public static void createTestFiles() throws IOException {
    writeAllocationInfo("empty.alloc", new String[0], new String[0], new String[0], new int[0][], new short[0][][]);
    writeAllocationInfo(
            "basic1.alloc", new String[]{"path.Foo"}, new String[0], new String[0], new int[][]{{32, 4, 0, 0}}, new short[][][]{{}});
    writeAllocationInfo("basic2.alloc", new String[]{"path.Foo", "path.Bar", "path.Baz"}, new String[]{"foo", "bar", "baz"},
            new String[]{"Foo.java", "Bar.java"}, new int[][]{{64, 0, 1, 3}},
            new short[][][]{{{1, 1, 1, -1}, {2, 0, 1, 2000}, {0, 2, 0, 10}}});
    writeAllocationInfo("basic3.alloc", new String[]{"path.Red", "path.Green", "path.Blue", "path.LightCanaryishGrey"},
            new String[]{"eatTiramisu", "failUnitTest", "watchCatVideos", "passGo", "collectFakeMoney", "findWaldo"},
            new String[]{"Red.java", "SomewhatBlue.java", "LightCanaryishGrey.java"},
            new int[][]{{128, 8, 0, 2}, {16, 8, 2, 1}, {42, 2, 1, 3}},
            new short[][][]{{{1, 0, 1, 100}, {2, 5, 1, -2}}, {{0, 1, 0, -1}}, {{3, 4, 2, 10001}, {0, 3, 0, 0}, {2, 2, 1, 16}}});
  }

  private static void writeAllocationInfo(String fileName, String[] classNames, String[] methodNames, String[] fileNames, int[][] entries,
                                          short[][][] stackFrames) throws IOException {
    byte msgHdrLen = 15, entryHdrLen = 9, stackFrameLen = 8;

    // Number of bytes from start of message to string tables
    int offset = msgHdrLen;
    for (int[] entry : entries) {
      offset += entryHdrLen + (stackFrameLen * entry[3]);
    }

    // Number of bytes in string tables
    int strNamesLen = 0;
    for (String name : classNames) { strNamesLen += 4 + (2 * name.length()); }
    for (String name : methodNames) { strNamesLen += 4 + (2 * name.length()); }
    for (String name : fileNames) { strNamesLen += 4 + (2 * name.length()); }

    ByteBuffer data = ByteBuffer.allocate(offset + strNamesLen);

    data.put(new byte[]{msgHdrLen, entryHdrLen, stackFrameLen});
    data.putShort((short) entries.length);
    data.putInt(offset);
    data.putShort((short) classNames.length);
    data.putShort((short) methodNames.length);
    data.putShort((short) fileNames.length);

    for (short i = 0; i < entries.length; ++i) {
      data.putInt(entries[i][0]); // total alloc size
      data.putShort((short) entries[i][1]); // thread id
      data.putShort((short) entries[i][2]); // allocated class index
      data.put((byte) entries[i][3]); // stack depth

      short[][] frames = stackFrames[i];
      for (short[] frame : frames) {
        data.putShort(frame[0]); // class name
        data.putShort(frame[1]); // method name
        data.putShort(frame[2]); // source file
        data.putShort(frame[3]); // line number
      }
    }

    for (String name : classNames) {
      data.putInt(name.length());
      data.put(strToBytes(name));
    }
    for (String name : methodNames) {
      data.putInt(name.length());
      data.put(strToBytes(name));
    }
    for (String name : fileNames) {
      data.putInt(name.length());
      data.put(strToBytes(name));
    }

    writeToFile(fileName, data.array());
  }

  private static byte[] strToBytes(String str) {
    return str.getBytes(Charsets.UTF_16BE);
  }

  private static void writeToFile(String fileName, byte[] data) throws IOException {
    FileOutputStream fos = new FileOutputStream(HOME + File.separator + fileName);
    try {
      fos.write(data);
    } finally {
      fos.close();
    }
  }
}
