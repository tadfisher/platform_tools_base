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
 * a Resolved split dimension. This is the result of running the include/exclude rules from
 * {@link SplitDimension} on the actual content of the project. It contains the list of {@link Split}
 * and associate them with a {@link SplitType} (though each Split is also associated with it).
 *
 * For instance if splitting by ABI, the rule might say "include: all", but the resolved splits
 * might only contain "x86, "arm" if "mips" is not being built.
 *
 * @see SplitResolver
 */
public class ResolvedSplitDimension {

    @NonNull
    private final SplitType mType;
    @NonNull
    private final Set<Split> mValues;

    public ResolvedSplitDimension(
            @NonNull SplitType type,
            @NonNull Set<Split> values) {
        mType = type;
        mValues = ImmutableSet.<Split>builder().addAll(values).build();
    }

    @NonNull
    public SplitType getType() {
        return mType;
    }

    @NonNull
    public Set<Split> getSplitsForMultiApk() {
        return mValues;
    }
}
