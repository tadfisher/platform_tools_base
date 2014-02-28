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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.DataFormatException;

public class NinePatchProcessorTest extends BasePngTest {

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.setName("NinePatchProcessor");

        for (File file : getNinePatches()) {
            String testName = "process_" + file.getName();

            NinePatchProcessorTest test = (NinePatchProcessorTest) TestSuite.createTest(
                    NinePatchProcessorTest.class, testName);

            test.setFile(file);

            suite.addTest(test);
        }

        return suite;
    }

    @NonNull
    private File mFile;

    protected void setFile(@NonNull File file) {
        mFile = file;
    }

    @Override
    protected void runTest() throws Throwable {
        File outFile = crunch(mFile);
        File crunched = new File(mFile.getParent(), mFile.getName() + ".crunched");

        compareChunks(crunched, outFile);

        compareImageContent(mFile, outFile, true /*is9Patch*/);
    }

    private static void compareChunks(@NonNull File original, @NonNull File tested) throws
            IOException, DataFormatException {
        Map<String, Chunk> originalChunks = readChunks(original);
        Map<String, Chunk> testedChunks = readChunks(tested);

        compareChunk(originalChunks, testedChunks, "IHDR");
        compareChunk(originalChunks, testedChunks, "npLb");
        compareChunk(originalChunks, testedChunks, "npTc");
    }

    private static void compareChunk(
            @NonNull Map<String, Chunk> originalChunks,
            @NonNull Map<String, Chunk> testedChunks,
            @NonNull String chunkType) {
        assertEquals(originalChunks.get(chunkType), testedChunks.get(chunkType));
    }

    @NonNull
    private static Map<String, Chunk> readChunks(@NonNull File file) throws IOException {
        Map<String, Chunk> chunks = Maps.newHashMap();

        byte[] fileBuffer = Files.toByteArray(file);
        ByteBuffer buffer = ByteBuffer.wrap(fileBuffer);

        byte[] sig = new byte[8];
        buffer.get(sig);

        assertTrue(Arrays.equals(sig, PngWriter.SIGNATURE));

        byte[] data, type;
        int len;
        int crc32;

        while (buffer.hasRemaining()) {
            len = buffer.getInt();

            type = new byte[4];
            buffer.get(type);

            data = new byte[len];
            buffer.get(data);

            // crc
            crc32 = buffer.getInt();

            Chunk chunk = new Chunk(type, data, crc32);
            chunks.put(chunk.getTypeAsString(), chunk);
        }

        return chunks;
    }

    @NonNull
    private static File[] getNinePatches() {
        File pngFolder = getPngFolder();
        File ninePatchFolder = new File(pngFolder, "ninepatch");

        File[] files = ninePatchFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getPath().endsWith(SdkConstants.DOT_9PNG);
            }
        });
        if (files != null) {
            return files;
        }

        return new File[0];
    }
}
