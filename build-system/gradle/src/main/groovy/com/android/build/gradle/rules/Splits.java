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

package com.android.build.gradle.rules;

import com.android.annotations.NonNull;
import com.android.resources.Density;
import com.google.common.collect.Sets;

import org.gradle.api.Action;
import org.gradle.internal.reflect.Instantiator;

import java.util.Set;

/**
 */
public class Splits {

    private final SplitData density;

    public Splits(@NonNull Instantiator instantiator) {
        density = instantiator.newInstance(SplitData.class);
    }

    public SplitData getDensity() {
        return density;
    }

    public void density(Action<SplitData> action) {
        action.execute(density);
    }

    @NonNull
    public Set<String> getDensityList() {
        Density[] values = Density.values();
        Set<String> fullList = Sets.newHashSetWithExpectedSize(values.length - 1);
        for (Density value : values) {
            if (value != Density.NODPI) {
                fullList.add(value.getResourceValue());
            }
        }

        return density.computeList(fullList);
    }
}
