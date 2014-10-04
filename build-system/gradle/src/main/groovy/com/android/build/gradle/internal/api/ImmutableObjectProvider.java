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
import com.android.build.gradle.api.GroupableProductFlavor;
import com.android.builder.model.BuildType;
import com.android.builder.model.ProductFlavor;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Provides API-ready objects.
 *
 * The main goal of this class is to provide read-only version of internal instances on
 * the fly when requested by the gradle script through the public API.
 */
public class ImmutableObjectProvider {

    private ImmutableProductFlavor immutableDefaultConfig;

    /**
     * Map of immutable build-types. This maps the normal build type to the immutable version.
     */
    @NonNull
    private final Map<BuildType, BuildType> immutableBuildTypes = Maps.newIdentityHashMap();

    /**
     * Map of immutable build-types. This maps the normal product flavor to the immutable version.
     */
    @NonNull
    private final Map<GroupableProductFlavor, GroupableProductFlavor> immutableFlavors = Maps.newIdentityHashMap();

    /**
     * Returns an immutable version of the default config.
     * @param defaultConfig the default config.
     * @return an immutable version.
     */
    @NonNull ProductFlavor getDefaultConfig(@NonNull ProductFlavor defaultConfig) {
        if (immutableDefaultConfig != null) {
            if (immutableDefaultConfig.productFlavor != defaultConfig) {
                throw new IllegalStateException("Different DefaultConfigs passed to ApiObjectProvider");
            }
        } else {
            immutableDefaultConfig = new ImmutableProductFlavor(defaultConfig);
        }

        return immutableDefaultConfig;
    }

    /**
     * Returns an immutable version of a build type.
     * @param buildType the build type.
     * @return an immutable version.
     */
    @NonNull
    public BuildType getBuildType(@NonNull BuildType buildType) {
        BuildType roBuildType = immutableBuildTypes.get(buildType);
        if (roBuildType == null) {
            immutableBuildTypes.put(buildType, roBuildType = new ImmutableBuildType(buildType));
        }

        return roBuildType;
    }

    /**
     * Retuens an immutable version of a groupable product flavor.
     * @param productFlavor the product flavor.
     * @return an immutable version.
     */
    @NonNull
    public GroupableProductFlavor getProductFlavor(@NonNull GroupableProductFlavor productFlavor) {
        GroupableProductFlavor roProductFlavor = immutableFlavors.get(productFlavor);
        if (roProductFlavor == null) {
            immutableFlavors.put(productFlavor,
                    roProductFlavor = new ImmutableGroupableProductFlavor(productFlavor));
        }

        return roProductFlavor;
    }
}
