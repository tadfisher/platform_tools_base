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
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;

import javax.imageio.ImageIO;

/**
 */
public class PngProcessor {

    private static final int COLOR_TRANSPARENT        = 0x00000000;
    private static final int COLOR_WHITE              = 0xFFFFFFFF;
    private static final int COLOR_TICK               = 0xFF000000;
    private static final int COLOR_LAYOUT_BOUNDS_TICK = 0xFF0000FF;

    @NonNull
    private final File mFile;

    @NonNull
    private final ByteUtils mByteUtils;

    private Chunk mIhdr;
    private Chunk mIdat;
    private List<Chunk> mOtherChunks = Lists.newArrayList();

    public static void process(@NonNull File from, @NonNull File to)
            throws IOException, NinePatchException {
        ByteUtils byteUtils = new ByteUtils();
        PngProcessor processor = new PngProcessor(from, byteUtils);
        processor.read();

        PngWriter writer = new PngWriter(to, byteUtils);
        writer.setIhdr(processor.getIhdr())
                .setChunks(processor.getOtherChunks())
                .setChunk(processor.getIdat());

        writer.write();
    }

    @VisibleForTesting
    PngProcessor(@NonNull File file, @NonNull ByteUtils byteUtils) {
        mFile = file;
        mByteUtils = byteUtils;
    }


    @VisibleForTesting
    @NonNull
    Chunk getIhdr() {
        return mIhdr;
    }

    @VisibleForTesting
    @NonNull
    List<Chunk> getOtherChunks() {
        return mOtherChunks;
    }

    @VisibleForTesting
    @NonNull
    Chunk getIdat() {
        return mIdat;
    }

    @VisibleForTesting
    void read() throws IOException, NinePatchException {
        BufferedImage image = ImageIO.read(mFile);

        byte[] imageBuffer = processImageContent(image);
        mIdat = new Chunk(PngWriter.IDAT, imageBuffer);
    }

    byte[] processImageContent(@NonNull BufferedImage image) throws NinePatchException {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] content = new int[width * height];

        image.getRGB(0, 0, width, height, content, 0, width);

        int startX = 0;
        int startY = 0;
        int endX = width;
        int endY = height;

        if (is9Patch()) {
            startX = 1;
            startY = 1;
            endX--;
            endY--;

            processBorder(content, width, height);
        }

        int len = (1 + (endX - startX) * 4) * (endY - startY);
        ByteBuffer buffer = ByteBuffer.allocate(len);

        for (int y = startY ; y < endY ; y++) {
            buffer.put((byte) 0);

            for (int x = startX ; x < endX ; x++) {
                int value = content[(width * y) + x];
                int a = (value >> 24) & 0x000000FF;
                int rgb = value << 8;
                buffer.putInt(rgb | a);
            }
        }

        byte[] compressed = new byte[len];

        Deflater deflater = new Deflater(Deflater.DEFLATED);
        deflater.setInput(buffer.array());
        deflater.finish();
        int compressedDataLength = deflater.deflate(compressed);

        byte[] finalBuffer = new byte[compressedDataLength];
        System.arraycopy(compressed, 0, finalBuffer, 0, compressedDataLength);

        mIhdr = computeIhdr(endX - startX, endY - startY, (byte) 8, ColorType.RGBA);

        return finalBuffer;
    }

    /**
     * process the border of the image to find 9-patch info
     * @param content the content of ARGB
     * @param width the width
     * @param height the height
     * @throws IOException
     */
    private void processBorder(int[] content, int width, int height)
            throws NinePatchException {
        // Validate size...
        if (width < 3 || height < 3) {
            throw new NinePatchException("Image must be at least 3x3 (1x1 without frame) pixels");
        }

        int i, j;

        int[] xDivs = new int[width];
        int[] yDivs = new int[height];
        Arrays.fill(xDivs, -1);
        Arrays.fill(yDivs, -1);

        int numXDivs = 0;
        int numYDivs = 0;
        byte numColors = 0;
        int numRows, numCols;
        int top, left, right, bottom;

        int paddingLeft = 0, paddingTop = 0, paddingRight = 0, paddingBottom = 0;

        boolean transparent = (content[0] & 0xFF000000) == 0;
        boolean hasColor = false;

        int colorIndex = 0;

        // Validate frame...
        if (!transparent && content[0] != 0xFFFFFFFF) {
            throw new NinePatchException("Must have one-pixel frame that is either transparent or white");
        }

        // Find left and right of sizing areas...
        AtomicInteger outInt = new AtomicInteger(0);
        try {
            get_horizontal_ticks(
                    content, 0, width,
                    transparent, true /*required*/,
                    xDivs, 0, 1, outInt,
                    true /*multipleAllowed*/);
            numXDivs = outInt.get();
        } catch (TickException e) {
            throw new NinePatchException(e, "top", xDivs[0]);
        }

        // Find top and bottom of sizing areas...
        outInt.set(0);
        try {
            get_vertical_ticks(content, 0, width, height,
                    transparent, true /*required*/,
                    yDivs, 0, 1, outInt,
                    true /*multipleAllowed*/);
            numYDivs = outInt.get();
        } catch (TickException e) {
            throw new NinePatchException(e, "left", yDivs[0]);
        }

        // Find left and right of padding area...
        int[] values = new int[2];
        try {
            get_horizontal_ticks(
                    content, width * (height -1 ), width,
                    transparent, false /*required*/,
                    values, 0, 1, null,
                    false /*multipleAllowed*/);
            paddingLeft = values[0];
            paddingRight = values[1];
            values[0] = values[1] = 0;
        } catch (TickException e) {
            throw new NinePatchException(e, "bottom", values[0]);
        }

        // Find top and bottom of padding area...
        try {
            get_vertical_ticks(
                    content, width-1, width, height,
                    transparent, false /*required*/,
                    values, 0, 1, null,
                    false /*multipleAllowed*/);
            paddingTop = values[0];
            paddingBottom = values[1];
        } catch (TickException e) {
            throw new NinePatchException(e, "right", values[0]);
        }


        // Create the chunks.
        NinePatchChunk ninePatchChunk = new NinePatchChunk(xDivs, yDivs, numXDivs, numYDivs, numColors);
        mOtherChunks.add(ninePatchChunk.getChunk());
    }

    private static enum TickType {
        NONE, TICK, LAYOUT_BOUNDS, BOTH
    }

    static TickType tick_type(int color, boolean transparent)
            throws TickException {

        int alpha = (color >> 24) & 0x000000FF;

        if (transparent) {
            if (alpha == 0) {
                return TickType.NONE;
            }
            if (color == COLOR_LAYOUT_BOUNDS_TICK) {
                return TickType.LAYOUT_BOUNDS;
            }
            if (color == COLOR_TICK) {
                return TickType.TICK;
            }

            // Error cases
            if (alpha != 0xFF) {
                throw new TickException("Frame pixels must be either solid or transparent (not intermediate alphas)");
            }
            if ((color & 0x00FFFFFF) != 0) {
                throw new TickException("Ticks in transparent frame must be black or red");
            }
            return TickType.TICK;
        }

        if (alpha != 0xFF) {
            throw new TickException("White frame must be a solid color (no alpha)");
        }
        if (color == COLOR_WHITE) {
            return TickType.NONE;
        }
        if (color == COLOR_TICK) {
            return TickType.TICK;
        }
        if (color == COLOR_LAYOUT_BOUNDS_TICK) {
            return TickType.LAYOUT_BOUNDS;
        }

        if ((color & 0x00FFFFFF) != 0) {
            throw new TickException("Ticks in transparent frame must be black or red");
        }

        return TickType.TICK;
    }


    private static enum Tick {
        START, INSIDE_1, OUTSIDE_1
    }

    static class TickException extends Exception {
        @NonNull
        private final String mMessage;
        private final int mLocation;

        TickException(@NonNull String message, int location) {
            super(message);
            mMessage = message;
            mLocation = location;
        }

        TickException(@NonNull String message) {
            this(message, -1);
        }

        TickException(@NonNull Throwable t, int location) {
            super(t.getMessage(), t);
            mMessage = t.getMessage();
            mLocation = location;
        }
    }

    static void get_horizontal_ticks(int[] content, int offset, int width,
            boolean transparent, boolean required,
            @NonNull int[] divs, int left, int right,
            @Nullable AtomicInteger outDivs, boolean multipleAllowed)
            throws TickException {
        int i;
        divs[left] = divs[right] = -1;
        Tick state = Tick.START;
        boolean found = false;

        for (i=1; i<width-1; i++) {
            TickType tickType;
            try {
                tickType = tick_type(content[offset+i], transparent);
            } catch (TickException e) {
                throw new TickException(e, i);
            }

            if (TickType.TICK == tickType) {
                if (state == Tick.START ||
                        (state == Tick.OUTSIDE_1 && multipleAllowed)) {
                    divs[left] = i-1;
                    divs[right] = width-2;
                    found = true;
                    if (outDivs != null) {
                        outDivs.addAndGet(2);
                    }
                    state = Tick.INSIDE_1;
                } else if (state == Tick.OUTSIDE_1) {
                    throw new TickException("Can't have more than one marked region along edge", i);
                }
            } else {
                if (state == Tick.INSIDE_1) {
                    // We're done with this div.  Move on to the next.
                    divs[right] = i-1;
                    right += 2;
                    left += 2;
                    state = Tick.OUTSIDE_1;
                }
            }

        }

        if (required && !found) {
            throw new TickException("No marked region found along edge");
        }
    }

    static void get_vertical_ticks(
            int[] content, int offset, int width, int height,
            boolean transparent, boolean required,
            @NonNull int[] divs, int top, int bottom,
            @Nullable AtomicInteger outDivs, boolean multipleAllowed) throws TickException {

        int i;
        divs[top] = divs[bottom] = -1;
        Tick state = Tick.START;
        boolean found = false;

        for (i=1; i<height-1; i++) {
            TickType tickType;
            try {
                tickType = tick_type(content[offset + width * i], transparent);
            } catch (TickException e) {
                throw new TickException(e, i);
            }

            if (TickType.TICK == tickType) {
                if (state == Tick.START ||
                        (state == Tick.OUTSIDE_1 && multipleAllowed)) {
                    divs[top] = i-1;
                    divs[bottom] = height - 2;
                    found = true;
                    if (outDivs != null) {
                        outDivs.addAndGet(2);
                    }
                    state = Tick.INSIDE_1;
                } else if (state == Tick.OUTSIDE_1) {
                    throw new TickException("Can't have more than one marked region along edge", i);
                }
            } else {
                if (state == Tick.INSIDE_1) {
                    // We're done with this div.  Move on to the next.
                    divs[bottom] = i-1;
                    top += 2;
                    bottom += 2;
                    state = Tick.OUTSIDE_1;
                }
            }
        }

        if (required && !found) {
            throw new TickException("No marked region found along edge");
        }
    }


    @VisibleForTesting
    Chunk computeIhdr(int width, int height, byte bitDepth, @NonNull ColorType colorType) {
        byte[] buffer = new byte[13];

        System.arraycopy(mByteUtils.getIntAsArray(width), 0, buffer, 0, 4);
        System.arraycopy(mByteUtils.getIntAsArray(height), 0, buffer, 4, 4);
        buffer[8] = bitDepth;
        buffer[9] = colorType.getFlag();
        buffer[10] = 0; // compressionMethod
        buffer[11] = 0; // filterMethod;
        buffer[12] = 0; // interlaceMethod

        return new Chunk(PngWriter.IHDR, buffer);
    }

    private boolean is9Patch() {
        return mFile.getPath().endsWith(SdkConstants.DOT_9PNG);
    }
}
