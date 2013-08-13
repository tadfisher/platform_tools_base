/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.tools.perflib.vmtrace.viz;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

public class TimeScaleRenderer {
    private static final int SCALE_Y = 20;

    private long mTraceEntryGlobalTime;

    private double[] mPoints = new double[8];

    private AffineTransform mViewTransform;
    private AffineTransform mViewTransformInverse;

    public TimeScaleRenderer(long start) {
        mTraceEntryGlobalTime = start;
    }

    public void paint(Graphics2D g2d, AffineTransform screenTransform,
            AffineTransform viewPortTransform, int screenWidth) {
        AffineTransform originalTransform = g2d.getTransform();

        g2d.setTransform(screenTransform);

        createInverse(viewPortTransform);

        long start = mTraceEntryGlobalTime;
        long end = mTraceEntryGlobalTime + screenWidth;

        mPoints[0] = mPoints[1] = 0; // (0,0)

        mPoints[2] = screenWidth; // (screenWidth, 0)
        mPoints[3] = 0;

        if (mViewTransformInverse != null) {
            mViewTransformInverse.transform(mPoints, 0, mPoints, 4, 2);
            start = (long) mPoints[4] + mTraceEntryGlobalTime;
            end = (long) mPoints[6] + mTraceEntryGlobalTime;
        }

        g2d.setColor(Color.BLACK);
        g2d.drawLine(0, SCALE_Y, screenWidth, SCALE_Y);
        g2d.drawString(Long.toString(start), 10, SCALE_Y - 5);
        g2d.drawString(Long.toString(end), screenWidth - 80, SCALE_Y - 5);

        g2d.setTransform(originalTransform);
    }

    public int getLayoutHeight() {
        return SCALE_Y + 10;
    }

    private void createInverse(AffineTransform viewPortTransform) {
        if (!viewPortTransform.equals(mViewTransform)) {
            // cache source transformation matrix
            mViewTransform = new AffineTransform(viewPortTransform);

            try {
                mViewTransformInverse = mViewTransform.createInverse();
            } catch (NoninvertibleTransformException e) {
                mViewTransformInverse = null;
            }
        }
    }
}
