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

package com.android.ide.common.vectordrawable;

import com.android.annotations.Nullable;
import com.google.common.base.Charsets;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 *
 * Generate a Image based on the VectorDrawable's XML content.
 */
public class VdPreview {
    /**
     *
     * @param forceImageWidth the width of result image if this is bigger than 0. Otherwise, take
     *                        the imageScale to scale.
     * @param imageScale scale the image based on its baseWidth and baseHeight. This is
     *                   ignored when forceImageWidth > 0.
     * @param xmlFileContent  VectorDrawable's XML file's content.
     * @param vdErrorLog log for the parsing errors and warnings.
     * @return an preview image according to the VectorDrawable's XML
     */
    @Nullable
    public static BufferedImage getPreviewFromVectorXml(int forceImageWidth, float imageScale,
                                                        @Nullable String xmlFileContent,
                                                        @Nullable StringBuilder vdErrorLog) {
        if (xmlFileContent == null || xmlFileContent.isEmpty()) {
            return null;
        }
        VdParser p = new VdParser();
        VdTree vdTree;

        InputStream inputStream = new ByteArrayInputStream(
            xmlFileContent.getBytes(Charsets.UTF_8));
        vdTree = p.parse(inputStream, vdErrorLog);
        if (vdTree == null) {
            return null;
        }

        // If the forceImageWidth is set (>0), then we honor that.
        // Otherwise, we will ask the vectorDrawable for the prefer size, then apply the imageScale.
        float vdWidth = vdTree.getBaseWidth();
        float vdHeight = vdTree.getBaseHeight();
        float imageWidth;
        float imageHeight;
        if (forceImageWidth > 0) {
            imageWidth = forceImageWidth;
            imageHeight = forceImageWidth * vdHeight / vdWidth;
        } else {
            imageWidth = vdWidth * imageScale;
            imageHeight = vdHeight * imageScale;
        }

        // Create the image according to the vectorDrawable's aspect ratio.
        BufferedImage image = new BufferedImage((int) imageWidth, (int) imageHeight,
                                  BufferedImage.TYPE_INT_ARGB);

        Graphics g = image.getGraphics();
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        vdTree.draw(g, image.getWidth(), image.getHeight());
        return image;
    }
}
