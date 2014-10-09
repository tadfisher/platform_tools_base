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

package com.android.build.gradle.internal.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.api.GroupableProductFlavor;
import com.android.build.gradle.api.VariantFilter;
import com.android.builder.model.BuildType;
import com.android.builder.model.ProductFlavor;

import java.util.Collections;
import java.util.List;

/**
 * Internal implementation of VariantFilter
 */
public class VariantFilterImpl implements VariantFilter {

    @NonNull
    private final ReadOnlyObjectProvider readOnlyObjectProvider;

    private boolean ignore;

    private ProductFlavor defaultConfig;
    private BuildType buildType;
    private List<? extends GroupableProductFlavor> flavors;

    public VariantFilterImpl(@NonNull ReadOnlyObjectProvider readOnlyObjectProvider) {
        this.readOnlyObjectProvider = readOnlyObjectProvider;
    }

    public void reset(
            @NonNull ProductFlavor defaultConfig,
            @NonNull BuildType buildType,
            @Nullable List<? extends GroupableProductFlavor> flavors) {
        ignore = false;
        this.defaultConfig = defaultConfig;
        this.buildType = buildType;
        this.flavors = flavors;
    }

    public boolean isIgnore() {
        return ignore;
    }

    @Override
    public void setIgnore(boolean ignore) {
        this.ignore = ignore;
    }

    @Override
    @NonNull
    public ProductFlavor getDefaultConfig() {
        return readOnlyObjectProvider.getDefaultConfig(defaultConfig);
    }

    @Override
    @NonNull
    public BuildType getBuildType() {
        return readOnlyObjectProvider.getBuildType(buildType);
    }

    @NonNull
    @Override
    public List<GroupableProductFlavor> getFlavors() {
        return flavors != null ?
                new ImmutableFlavorList(flavors, readOnlyObjectProvider) :
                Collections.<GroupableProductFlavor>emptyList();
    }
}
