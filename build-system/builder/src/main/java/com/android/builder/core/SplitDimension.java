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

package com.android.builder.core;

import com.android.annotations.NonNull;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * A {@link Split} dimension.
 *
 * Since splits are defined by developer by a set of rules rather than a list of discrete values,
 * this represent a dimension of split of a given type (Through the associated {@link SplitType}),
 * with its associated include/exclude rules.
 *
 * To get a list of actual discrete values, use {@link SplitResolver} to
 * get a {@link com.android.builder.core.ResolvedSplitDimension}.
 *
 */
public class SplitDimension {

    public static final String ALL = "all";

    @NonNull
    private final SplitType mType;
    @NonNull
    private final Set<String> mIncludes;
    @NonNull
    private final Set<String> mExcludes;

    public SplitDimension(@NonNull SplitType type,
            @NonNull Set<String> includes,
            @NonNull Set<String> excludes) {
        mType = type;
        mIncludes = ImmutableSet.<String>builder().addAll(includes).build();
        mExcludes = ImmutableSet.<String>builder().addAll(excludes).build();
    }

    @NonNull
    public SplitType getType() {
        return mType;
    }

    @NonNull
    public Set<String> getIncludes() {
        return mIncludes;
    }

    @NonNull
    public Set<String> getExcludes() {
        return mExcludes;
    }
}
