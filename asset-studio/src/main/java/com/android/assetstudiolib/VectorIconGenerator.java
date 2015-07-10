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
package com.android.assetstudiolib;

import com.android.assetstudiolib.Util.Effect;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Generate icons for the action bar
 */
public class VectorIconGenerator extends GraphicGenerator {

    /** Creates a new {@link VectorIconGenerator} */
    public VectorIconGenerator() {
    }

    @Override
    public BufferedImage generate(GraphicGeneratorContext context, Options options) {
/*        Rectangle iconSizeMdpi = new Rectangle(0, 0, 32, 32);
        Rectangle targetRectMdpi = new Rectangle(0, 0, 32, 32);
        final float scaleFactor = GraphicGenerator.getMdpiScaleFactor(options.density);
        Rectangle imageRect = Util.scaleRectangle(iconSizeMdpi, scaleFactor);
        Rectangle targetRect = Util.scaleRectangle(targetRectMdpi, scaleFactor);

        // Still draw outside the source such that it can't be
        if (false) {
            Graphics2D sourceG2d = (Graphics2D)options.sourceImage.getGraphics();
            sourceG2d.setColor(new Color(255, 7, 12, 255));
            sourceG2d.drawRect(0, 0, options.sourceImage.getWidth() - 1, options.sourceImage.getHeight() - 1);
            sourceG2d.dispose();
        }

        BufferedImage outImage = Util.newArgbBufferedImage(imageRect.width, imageRect.height);
        Graphics2D g = (Graphics2D) outImage.getGraphics();

        BufferedImage tempImage = Util.newArgbBufferedImage(imageRect.width, imageRect.height);
        Graphics2D g2 = (Graphics2D)tempImage.getGraphics();
        Util.drawCenterInside(g2, options.sourceImage, targetRect);

        // When the input image is vector base, then skip the theme drawing.
        Util.drawEffects(g, tempImage, 0, 0, new Effect[]{});

        g.setColor(Color.BLUE);
        g.drawRect(1, 1, outImage.getWidth() - 2, outImage.getHeight() - 2);


        g.dispose();
        g2.dispose();*/

        return options.sourceImage;
    }

    public static class VectorIconOptions extends GraphicGenerator.Options {
    }


}
