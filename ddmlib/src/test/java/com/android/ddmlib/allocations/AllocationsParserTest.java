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
package com.android.ddmlib.allocations;

import com.android.ddmlib.AllocationInfo;
import com.android.ddmlib.AllocationsParser;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class AllocationsParserTest extends TestCase {

  public void testConstructorOnBadFile() {
    try {
      new AllocationsParser(new File(""));
      assertTrue("Creation of parser should have failed", false);
    } catch (IllegalArgumentException ex) {
      assertTrue(true);
    }
  }

  public void testConstructorOnGoodFile() {
    assertNull((new AllocationsParser(getFile("empty.alloc"))).getAllocations());
  }

  public void testParsingOnNoAllocations() throws IOException {
    assertEquals(0, getAllocations("empty.alloc").length);
  }

  public void testParsingOnOneAllocationWithoutStackTrace() throws IOException {
    AllocationInfo[] info = getAllocations("basic1.alloc");
    assertEquals(1, info.length);

    AllocationInfo alloc = info[0];
    checkEntry(1, "path.Foo", 32, 4, alloc);
    checkFirstTrace(null, null, alloc);
    assertEquals(0, alloc.getStackTrace().length);
  }

  public void testParsingOnOneAllocationWithStackTrace() throws IOException {
    AllocationInfo[] info = getAllocations("basic2.alloc");
    assertEquals(1, info.length);

    AllocationInfo alloc = info[0];
    checkEntry(1, "path.Bar", 64, 0, alloc);
    checkFirstTrace("path.Bar", "bar", alloc);

    StackTraceElement[] elems = alloc.getStackTrace();
    assertEquals(3, elems.length);

    checkStackFrame("path.Bar", "bar", "Bar.java", -1, elems[0]);
    checkStackFrame("path.Baz", "foo", "Bar.java", 2000, elems[1]);
    checkStackFrame("path.Foo", "baz", "Foo.java", 10, elems[2]);
  }

  public void testParsing() throws IOException {
    AllocationInfo[] info = getAllocations("basic3.alloc");
    assertEquals(3, info.length);

    AllocationInfo alloc1 = info[0];
    checkEntry(3, "path.Red", 128, 8, alloc1);
    checkFirstTrace("path.Green", "eatTiramisu", alloc1);

    StackTraceElement[] elems1 = alloc1.getStackTrace();
    assertEquals(2, elems1.length);

    checkStackFrame("path.Green", "eatTiramisu", "SomewhatBlue.java", 100, elems1[0]);
    checkStackFrame("path.Blue", "findWaldo", "SomewhatBlue.java", -2, elems1[1]);

    AllocationInfo alloc2 = info[1];
    checkEntry(2, "path.Blue", 16, 8, alloc2);
    checkFirstTrace("path.Red", "failUnitTest", alloc2);

    StackTraceElement[] elems2 = alloc2.getStackTrace();
    assertEquals(1, elems2.length);

    checkStackFrame("path.Red", "failUnitTest", "Red.java", -1, elems2[0]);

    AllocationInfo alloc3 = info[2];
    checkEntry(1, "path.Green", 42, 2, alloc3);
    checkFirstTrace("path.LightCanaryishGrey", "collectFakeMoney", alloc3);

    StackTraceElement[] elems3 = alloc3.getStackTrace();
    assertEquals(3, elems3.length);

    checkStackFrame("path.LightCanaryishGrey", "collectFakeMoney", "LightCanaryishGrey.java", 10001, elems3[0]);
    checkStackFrame("path.Red", "passGo", "Red.java", 0, elems3[1]);
    checkStackFrame("path.Blue", "watchCatVideos", "SomewhatBlue.java", 16, elems3[2]);
  }

  private static void checkEntry(int order, String className, int size, int thread, AllocationInfo alloc) {
    assertEquals(order, alloc.getAllocNumber());
    assertEquals(className, alloc.getAllocatedClass());
    assertEquals(size, alloc.getSize());
    assertEquals(thread, alloc.getThreadId());
  }

  private static void checkFirstTrace(String className, String methodName, AllocationInfo alloc) {
    assertEquals(className, alloc.getFirstTraceClassName());
    assertEquals(methodName, alloc.getFirstTraceMethodName());
  }

  private static void checkStackFrame(String className, String methodName, String fileName, int lineNumber, StackTraceElement elem) {
    assertEquals(className, elem.getClassName());
    assertEquals(methodName, elem.getMethodName());
    assertEquals(fileName, elem.getFileName());
    assertEquals(lineNumber, elem.getLineNumber());
  }

  private static AllocationInfo[] getAllocations(String fileName) throws IOException {
    AllocationsParser parser = new AllocationsParser(getFile(fileName));
    parser.parse();
    AllocationInfo[] info = parser.getAllocations();
    assertNotNull(info);
    return info;
  }

  private static File getFile(String path) {
    URL resource = AllocationsParserTest.class.getResource(File.separator + path);
    assertNotNull(path + " not found", resource);
    return new File(resource.getFile());
  }
}