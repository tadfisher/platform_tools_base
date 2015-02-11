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

import static org.junit.Assert.assertEquals;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.ide.common.internal.PngCruncher;
import com.android.ide.common.internal.PngException;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.SdkManager;
import com.android.sdklib.repository.FullRevision;
import com.android.utils.ILogger;
import com.android.utils.StdLogger;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.DataFormatException;

/**
 * Synchronous version of the aapt cruncher test.
 */
public class NinePatchAaptProcessorTestUtils extends PngTestUtils {

    static File getAapt(FullRevision fullRevision) {
        ILogger logger = new StdLogger(StdLogger.Level.VERBOSE);
        SdkManager sdkManager = SdkManager.createManager(getSdkDir().getAbsolutePath(), logger);
        assert sdkManager != null;
        BuildToolInfo buildToolInfo = sdkManager.getBuildTool(fullRevision);
        if (buildToolInfo == null) {
            throw new RuntimeException("Test requires build-tools " + fullRevision.toShortString());
        }
        return new File(buildToolInfo.getPath(BuildToolInfo.PathId.AAPT));
    }


    public static void tearDownAndCheck(Map<File, File> sourceAndCrunchedFiles,
            PngCruncher cruncher, AtomicLong classStartTime) throws IOException, DataFormatException {
        long startTime = System.currentTimeMillis();
        try {
            cruncher.end();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(
                "waiting for requests completion : " + (System.currentTimeMillis() - startTime));
        System.out.println("total time : " + (System.currentTimeMillis() - classStartTime.get()));
        System.out.println("Comparing crunched files");
        long comparisonStartTime = System.currentTimeMillis();
        for (Map.Entry<File, File> sourceAndCrunched : sourceAndCrunchedFiles.entrySet()) {
            System.out.println(sourceAndCrunched.getKey().getName());
            File crunched = new File(sourceAndCrunched.getKey().getParent(),
                    sourceAndCrunched.getKey().getName() + getControlFileSuffix());

            //copyFile(sourceAndCrunched.getValue(), crunched);
            Map<String, Chunk> testedChunks = compareChunks(crunched, sourceAndCrunched.getValue());

            try {
                compareImageContent(crunched, sourceAndCrunched.getValue(), false);
            } catch (Throwable e) {
                throw new RuntimeException("Failed with " + testedChunks.get("IHDR"), e);
            }
        }
        System.out.println("Done comparing crunched files " + (System.currentTimeMillis()
                - comparisonStartTime));
    }

    protected static String getControlFileSuffix() {
        return ".crunched.aapt";
    }

    private static void copyFile(File source, File dest)
            throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }


    @NonNull
    static File crunchFile(@NonNull File file, PngCruncher aaptCruncher)
            throws PngException, IOException {
        File outFile = File.createTempFile("pngWriterTest", ".png");
        outFile.deleteOnExit();
        try {
            aaptCruncher.crunchPng(file, outFile);
        } catch (PngException e) {
            e.printStackTrace();
            throw e;
        }
        System.out.println("crunch " + file.getPath());
        return outFile;
    }


    private static Map<String, Chunk> compareChunks(@NonNull File original, @NonNull File tested)
            throws
            IOException, DataFormatException {
        Map<String, Chunk> originalChunks = readChunks(original);
        Map<String, Chunk> testedChunks = readChunks(tested);

        compareChunk(originalChunks, testedChunks, "IHDR");
        compareChunk(originalChunks, testedChunks, "npLb");
        compareChunk(originalChunks, testedChunks, "npTc");

        return testedChunks;
    }

    private static void compareChunk(
            @NonNull Map<String, Chunk> originalChunks,
            @NonNull Map<String, Chunk> testedChunks,
            @NonNull String chunkType) {
        assertEquals(originalChunks.get(chunkType), testedChunks.get(chunkType));
    }

    public static Collection<Object[]> getNinePatches() {
        File pngFolder = getPngFolder();
        File ninePatchFolder = new File(pngFolder, "ninepatch");

        File[] files = ninePatchFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getPath().endsWith(SdkConstants.DOT_9PNG);
            }
        });
        if (files != null) {
            ImmutableList.Builder<Object[]> params = ImmutableList.builder();
            for (File file : files) {
                params.add(new Object[]{file, file.getName()});
            }
            return params.build();
        }

        return ImmutableList.of();
    }
}
