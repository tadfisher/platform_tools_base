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

/**
 * Used to represent info to override the VectorDrawble's XML file content.
 */
public class VdOverrideInfo {
    public VdOverrideInfo(int width, int height, int opacity, boolean autoMirrored) {
        mWidth = width;
        mHeight = height;
        mOpacity = opacity;
        mAutoMirrored = autoMirrored;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int mWidth) {
        this.mWidth = mWidth;
    }

    public int getOpacity() {
        return mOpacity;
    }

    public void setOpacity(int mOpacity) {
        this.mOpacity = mOpacity;
    }

    public boolean isAutoMirrored() {
        return mAutoMirrored;
    }

    public void setAutoMirrored(boolean mAutoMirrored) {
        this.mAutoMirrored = mAutoMirrored;
    }

    private int mWidth;

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    boolean needsOverrideWidth() {
        return getWidth() > 0;
    }

    boolean needsOverrideHeight() {
        return getHeight() > 0;
    }

    boolean needsOverrideOpacity() {
        return getOpacity() < 100 && getOpacity() >=0;
    }

    boolean needsOverridAutoMirrored() {
        return mAutoMirrored;
    }

    private int mHeight;
    private int mOpacity;
    private boolean mAutoMirrored;

    public boolean needsOveride() {
        return needsOverridAutoMirrored() || needsOverrideWidth() || needsOverrideHeight()
               || needsOverrideOpacity();
    }
}
