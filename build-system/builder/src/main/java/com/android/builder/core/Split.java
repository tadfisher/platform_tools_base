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
 *
 * A Split is more focused Product Flavor used mostly to restrict packaging instead of providing
 * the ability to generate a truly different variant (different code, manifest, resources, ...)
 *
 * This allow for more optimized builds since more things can be shared across variants that only
 * differ on their splits values.
 *
 * A split instance is a value in a given {@link com.android.builder.core.SplitDimension}.
 * The actual dimension this split is a value of is controlled by the SplitType.
 *
 * The value(s) are usually filters on some parts of the application (density, abi, ...)
 *
 * @see com.android.builder.core.SplitDimension
 */
public class Split {

    @NonNull
    private final SplitType mType;
    @NonNull
    private final Set<String> mValues;

    Split(
            @NonNull SplitType type,
            @NonNull Set<String> values) {
        mType = type;
        mValues = ImmutableSet.<String>builder().addAll(values).build();
    }

    Split(
            @NonNull SplitType type,
            @NonNull String value) {
        mType = type;
        mValues = ImmutableSet.of(value);
    }

    Split(
            @NonNull SplitType type,
            @NonNull String value,
            @NonNull String value2) {
        mType = type;
        mValues = ImmutableSet.of(value, value2);
    }

    @NonNull
    public SplitType getType() {
        return mType;
    }

    @NonNull
    public Set<String> getValues() {
        return mValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Split split = (Split) o;

        if (mType != split.mType) {
            return false;
        }
        if (!mValues.equals(split.mValues)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = mType.hashCode();
        result = 31 * result + mValues.hashCode();
        return result;
    }
}
