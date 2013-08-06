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

package com.android.tools.perflib.viz;

import com.android.annotations.Nullable;

import java.awt.*;

public class VisualItem<D> {
    private final String mCategory;
    private final D mSrc;

    private int mLayoutX, mLayoutY, mLayoutWidth, mLayoutHeight;

    private Color mFill;

    public VisualItem(@Nullable D src, @Nullable String category) {
        mSrc = src;
        mCategory = category;
    }

    @Nullable
    public String getCategory() {
        return mCategory;
    }

    @Nullable
    public D getSource() {
        return mSrc;
    }

    public void setLayoutBounds(int x, int y, int w, int h) {
        mLayoutX = x;
        mLayoutY = y;
        mLayoutWidth = w;
        mLayoutHeight = h;
    }

    public void fillLayoutBounds(Rectangle r) {
        r.x = mLayoutX;
        r.y = mLayoutY;
        r.width = mLayoutWidth;
        r.height = mLayoutHeight;
    }

    public void setFill(Color c) {
        mFill = c;
    }

    public Color getFill() {
        return mFill;
    }
}
