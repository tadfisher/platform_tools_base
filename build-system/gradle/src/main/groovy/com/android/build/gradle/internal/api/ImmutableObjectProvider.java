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
import com.android.builder.model.BuildType;
import com.android.builder.model.ProductFlavor;
import com.android.builder.model.SigningConfig;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Provides Immutable versions of BuildType,  Product flavors and Signing Config so that
 * they can safely be exposed through the variant API.
 *
 * The class creates them on the fly and caches them so that they are only created when a
 * Gradle script/plugin queries for them.
 */
public class ImmutableObjectProvider {

    private ImmutableProductFlavor immutableDefaultConfig;

    /**
     * Map of immutable build-types. This maps the normal build type to the immutable version.
     */
    @NonNull
    private final Map<BuildType, BuildType> immutableBuildTypes = Maps.newIdentityHashMap();

    /**
     * Map of immutable ProductFlavor. This maps the normal flavor to the immutable version.
     */
    @NonNull
    private final Map<GroupableProductFlavor, GroupableProductFlavor> immutableFlavors = Maps.newIdentityHashMap();

    /**
     * Map of immutable SigningConfig. This maps the normal config to the immutable version.
     */
    @NonNull
    private final Map<SigningConfig, SigningConfig> immutableSigningConfig = Maps.newIdentityHashMap();

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
            immutableDefaultConfig = new ImmutableProductFlavor(defaultConfig, this);
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
        BuildType immutableBuildType = immutableBuildTypes.get(buildType);
        if (immutableBuildType == null) {
            immutableBuildTypes.put(buildType,
                    immutableBuildType = new ImmutableBuildType(buildType, this));
        }

        return immutableBuildType;
    }

    /**
     * Retuens an immutable version of a groupable product flavor.
     * @param productFlavor the product flavor.
     * @return an immutable version.
     */
    @NonNull
    public GroupableProductFlavor getProductFlavor(@NonNull GroupableProductFlavor productFlavor) {
        GroupableProductFlavor immutableProductFlavor = immutableFlavors.get(productFlavor);
        if (immutableProductFlavor == null) {
            immutableFlavors.put(productFlavor,
                    immutableProductFlavor = new ImmutableGroupableProductFlavor(
                            productFlavor, this));
        }

        return immutableProductFlavor;
    }

    /**
     * Returns an immutable version of a signing config.
     * @param signingConfig the signing config.
     * @return an immutable version.
     */
    @Nullable
    public SigningConfig getSigningConfig(@Nullable SigningConfig signingConfig) {
        if (signingConfig == null) {
            return null;
        }

        SigningConfig immutablesigningConfig = immutableSigningConfig.get(signingConfig);
        if (immutablesigningConfig == null) {
            immutableSigningConfig.put(signingConfig,
                    immutablesigningConfig = new ImmutableSigningConfig(signingConfig));
        }

        return immutablesigningConfig;
    }
}
