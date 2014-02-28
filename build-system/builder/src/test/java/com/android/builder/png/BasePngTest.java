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

import com.android.annotations.NonNull;
import com.android.testutils.TestUtils;

import junit.framework.TestCase;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 */
public abstract class BasePngTest extends TestCase {

    @NonNull
    protected static File crunch(@NonNull File file) throws IOException, NinePatchException {
        File outFile = File.createTempFile("pngWriterTest", ".png");
        outFile.deleteOnExit();

        PngProcessor.process(file, outFile);
        return outFile;
    }

    protected static void compareImageContent(@NonNull File originalFile, @NonNull File createdFile,
            boolean is9Patch)
            throws IOException {
        BufferedImage originalImage = ImageIO.read(originalFile);
        BufferedImage createdImage = ImageIO.read(createdFile);

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        int createdWidth = createdImage.getWidth();
        int createdHeight = createdImage.getHeight();

        // compare sizes taking into account if the image is a 9-patch
        // in which case the original is bigger by 2 since it has the patch area still.
        assertEquals(originalWidth, createdWidth + (is9Patch ? 2 : 0));
        assertEquals(originalHeight, createdHeight + (is9Patch ? 2 : 0));

        // get the file content
        // always use the created Size. And for the original image, if 9-patch, just take
        // the image minus the 1-pixel border all around.
        int[] originalContent = new int[createdWidth * createdHeight];
        if (is9Patch) {
            originalImage.getRGB(1, 1, createdWidth, createdHeight, originalContent, 0, createdWidth);
        } else {
            originalImage.getRGB(0, 0, createdWidth, createdHeight, originalContent, 0, createdWidth);
        }

        int[] createdContent = new int[createdWidth * createdHeight];
        createdImage.getRGB(0, 0, createdWidth, createdHeight, createdContent, 0, createdWidth);

        for (int y = 0 ; y < createdHeight ; y++) {
            for (int x = 0 ; x < createdWidth ; x++) {
                int originalRGBA = originalContent[y * createdWidth + x];
                int createdRGBA = createdContent[y * createdWidth + x];
                assertEquals(String.format("%dx%d: 0x%08x : 0x%08x", x, y, originalRGBA, createdRGBA), originalRGBA, createdRGBA);
            }
        }
    }

    @NonNull
    protected static File getFile(@NonNull String name) {
        return new File(getPngFolder(), name);
    }

    @NonNull
    protected static File getPngFolder() {
        File folder = TestUtils.getRoot("png");
        assertTrue(folder.isDirectory());
        return folder;
    }
}
