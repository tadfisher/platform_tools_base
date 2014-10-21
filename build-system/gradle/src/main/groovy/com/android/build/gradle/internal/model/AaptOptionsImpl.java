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

package com.android.build.gradle.internal.model;

import java.io.Serializable;
import java.util.Collection;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AaptOptions;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of AaptOptions that is Serializable.
 */
public class AaptOptionsImpl implements AaptOptions, Serializable {

    private static final long serialVersionUID = 1L;

    @Nullable
    private String mIgnoreAssets;

    @Nullable
    private Collection<String> mNoCompress;

    private boolean mFailOnMissingConfigEntry;

    private boolean mUseQueuedAaptPngCruncher;


    static AaptOptions create(@NonNull AaptOptions aaptOptions) {
        return new AaptOptionsImpl(
                aaptOptions.getIgnoreAssets(),
                aaptOptions.getNoCompress(),
                aaptOptions.getFailOnMissingConfigEntry(),
                aaptOptions.getUseQueuedAaptPngCruncher()
        );
    }

    private AaptOptionsImpl(String ignoreAssets, Collection<String> noCompress,
            boolean failOnMissingConfigEntry, boolean useQueuedAaptPngCruncher) {
        mIgnoreAssets = ignoreAssets;
        if (noCompress == null) {
            mNoCompress = null;
        } else {
            mNoCompress = ImmutableList.copyOf(noCompress);
        }
        mFailOnMissingConfigEntry = failOnMissingConfigEntry;
        mUseQueuedAaptPngCruncher = useQueuedAaptPngCruncher;
    }

    @Override

    public String getIgnoreAssets() {
        return mIgnoreAssets;
    }

    @Override
    public Collection<String> getNoCompress() {
        return mNoCompress;
    }

    @Override
    public boolean getFailOnMissingConfigEntry() {
        return mFailOnMissingConfigEntry;
    }

    @Override
    public boolean getUseQueuedAaptPngCruncher() {
        return mUseQueuedAaptPngCruncher;
    }

    public String toString() {
        return "AaptOptionsImpl{" +
                ", mIgnoreAssets=" + mIgnoreAssets +
                ", mNoCompress" + mNoCompress +
                ", mFailOnMissingConfigEntry" + mFailOnMissingConfigEntry +
                ", mUseQueuedAaptPngCruncher" + mUseQueuedAaptPngCruncher +
                "}";
    }
}
