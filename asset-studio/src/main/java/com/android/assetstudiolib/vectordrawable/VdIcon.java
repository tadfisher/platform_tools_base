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

import com.android.assetstudiolib.Util;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public class VdIcon implements Icon, Comparable<VdIcon> {
    private VdTree mVdTree;
    private final String mName;
    private final URL mUrl;

    public VdIcon(URL url) {
        setDynamicIcon(url);
        mUrl = url;
        String fileName = url.getFile();
        mName = fileName.substring(fileName.lastIndexOf("/") + 1);
    }

    public String getName() {
        return mName;
    }

    public URL getURL() {
        return mUrl;
    }

    public void setDynamicIcon(URL url) {
        final VdParser p = new VdParser();
        try {
            mVdTree = p.parse(url.openStream(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        // We knew all the icons are square shape.
        int minSize = Math.min(c.getWidth(), c.getHeight());

        final BufferedImage image = Util.newArgbBufferedImage(minSize, minSize);
        mVdTree.drawIntoImage(image);

        //myIcon.paintIcon(this, gg, 0, 0);
        /*if (mDrawBoundary) {
            g.draw3DRect(0, 0, getWidth() - 1, getHeight() - 1, false);
            // g.draw3DRect(0, 0, image.getWidth(), image.getHeight(), false);
        }
        // g.drawImage(image, 0, 0, getWidth(), getHeight(), 0, 0, image.getWidth(), image.getHeight(), this);*/
        Rectangle rect = new Rectangle(0, 0, c.getWidth(), c.getHeight());
        Util.drawCenterInside((Graphics2D)g, image, rect);
    }

    @Override
    public int getIconWidth() {
        return (int) (mVdTree != null ? mVdTree.mPortWidth : 0);
    }

    @Override
    public int getIconHeight() {
        return (int) (mVdTree != null ? mVdTree.mPortHeight : 0);
    }

    @Override
    public int compareTo(VdIcon other) {
        return mName.compareTo(other.mName);
    }
}