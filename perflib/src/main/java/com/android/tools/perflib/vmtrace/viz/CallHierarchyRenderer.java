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

import com.android.annotations.NonNull;
import com.android.tools.perflib.vmtrace.Call;
import com.android.tools.perflib.vmtrace.MethodInfo;
import com.android.tools.perflib.vmtrace.VmTraceData;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Iterator;

public class CallHierarchyRenderer {
    private static final int PER_LEVEL_HEIGHT_PX = 10;
    private static final int PADDING = 1;

    private static final int TEXT_HEIGHT = 6;
    private static final int TEXT_LEFT_PADDING = 5;

    private static final Color FILL_COLOR = Color.BLACK;

    private final VmTraceData mTraceData;
    private final Call mCall;
    private final long mGlobalStartTime;
    private final int mYOffset;

    private final Rectangle mLayout = new Rectangle();
    private final Point2D.Float mSrc = new Point2D.Float();
    private final Point2D.Float mDst = new Point2D.Float();

    private Font myFont;

    public CallHierarchyRenderer(@NonNull VmTraceData vmTraceData, @NonNull Call c,
            long globalStartTime, int yOffset) {
        mTraceData = vmTraceData;
        mCall = c;
        mYOffset = yOffset;
        mGlobalStartTime = globalStartTime;
    }

    public void render(Graphics2D g) {
        Rectangle clip = g.getClipBounds();

        Iterator<Call> it = mCall.getCallHierarchyIterator();
        while (it.hasNext()) {
            Call c = it.next();
            fillLayoutBounds(c, mLayout);

            if (!clip.intersects(mLayout)) {
                continue;
            }

            double widthOnScreen = g.getTransform().getScaleX() * mLayout.width;
            if (widthOnScreen < 1) {
                continue;
            }

            Color fillColor = getFillColor(c);
            g.setColor(fillColor);
            g.fillRect(mLayout.x, mLayout.y, mLayout.width, mLayout.height);

            String name = getName(c);
            drawString(g, name, mLayout, getFontColor(c));
        }
    }

    private void drawString(Graphics2D g, String name, Rectangle bounds, Color fontColor) {
        double widthOnScreen = g.getTransform().getScaleX() * bounds.width;

        if (myFont == null) {
            myFont = g.getFont().deriveFont(8.0f);
        }
        g.setFont(myFont);
        g.setColor(fontColor);

        AffineTransform origTx = g.getTransform();

        mSrc.x = bounds.x + TEXT_LEFT_PADDING;
        mSrc.y = bounds.y + TEXT_HEIGHT;
        origTx.transform(mSrc, mDst);
        g.setTransform(new AffineTransform());

        if (widthOnScreen > g.getFontMetrics().stringWidth(name)) {
            g.drawString(name, mDst.x, mDst.y);
        }

        g.setTransform(origTx);
    }

    private void fillLayoutBounds(Call c, Rectangle layoutBounds) {
        layoutBounds.x = (int) (c.getEntryGlobalTime() - mGlobalStartTime + PADDING);
        layoutBounds.y = c.getDepth() * PER_LEVEL_HEIGHT_PX + mYOffset + PADDING;
        layoutBounds.width  = (int) c.getInclusiveGlobalTime() - 2 * PADDING;
        layoutBounds.height = PER_LEVEL_HEIGHT_PX - 2 * PADDING;
    }

    @NonNull
    private String getName(@NonNull Call c) {
        long methodId = c.getMethodId();
        MethodInfo info = mTraceData.getMethod(methodId);
        return info.getShortName();
    }

    private Color getFillColor(Call c) {
        MethodInfo info = mTraceData.getMethod(c.getMethodId());
        int percent = quantize(info.getInclusiveThreadPercent());
        return getFill(percent);
    }

    private Color getFontColor(Call c) {
        MethodInfo info = mTraceData.getMethod(c.getMethodId());
        int percent = quantize(info.getInclusiveThreadPercent());
        return getFontColor(percent);
    }

    private static final Color[] QUANTIZED_COLORS = {
            new Color(247,251,255),
            new Color(222,235,247),
            new Color(198,219,239),
            new Color(158,202,225),
            new Color(107,174,214),
            new Color(66,146,198),
            new Color(33,113,181),
            new Color(8,81,156),
            new Color(8,48,107),
    };

    private Color getFill(int percent) {
        int i = percent / 10;
        if (i >= QUANTIZED_COLORS.length) {
            i = QUANTIZED_COLORS.length - 1;
        }
        return QUANTIZED_COLORS[i];
    }

    private Color getFontColor(int percent) {
        int i = percent / 10;
        if (i >= QUANTIZED_COLORS.length) {
            i = QUANTIZED_COLORS.length - 1;
        }

        return  i > 6 ? Color.WHITE : Color.BLACK;
    }

    private int quantize(float inclusiveThreadPercent) {
        return ((int)(inclusiveThreadPercent + 9) / 10) * 10;
    }
}
