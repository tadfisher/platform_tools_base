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

package com.android.assetstudiolib.vectordrawable;

import com.android.assetstudiolib.vectordrawable.vg.VDParser;
import com.android.assetstudiolib.vectordrawable.vg.VDTree;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Generate a Image based on the VectorDrawable's XML content.
 */
public class VDPreview {
    public static BufferedImage getPreviewFromVectorXML(String xmlFileContent)
            throws UnsupportedEncodingException {
        BufferedImage image;
        image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);

        VDParser p = new VDParser();
        VDTree vdTree;

        InputStream inputStream = new ByteArrayInputStream(xmlFileContent.getBytes("UTF-8"));
        try {
            vdTree = p.parse(inputStream);
            Graphics g = image.getGraphics();
            g.setColor(new Color(255, 255, 255, 0));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            vdTree.drawInternal(g, image.getWidth(), image.getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return image;
    }
}
