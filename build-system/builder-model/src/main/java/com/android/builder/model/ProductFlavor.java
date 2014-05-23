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

package com.android.builder.model;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.util.Collection;

/**
 * a Product Flavor. This is only the configuration of the flavor.
 *
 * It does not include the sources or the dependencies. Those are available on the container
 * or in the artifact info.
 *
 * @see ProductFlavorContainer
 * @see BaseArtifact#getDependencies()
 */
public interface ProductFlavor extends BaseConfig {

    /**
     * Returns the name of the flavor.
     *
     * @return the name of the flavor.
     */
    @Override
    @NonNull
    String getName();

    /**
     * Returns the name of the product flavor. This is only the value set on this product flavor.
     * To get the final package name, use {@link AndroidArtifact#getPackageName()}.
     *
     * @return the package name.
     */
    @Nullable
    String getPackageName();

    /**
     * Returns the version code. This is only the value set on this product flavor.
     * To get the final value, use {@link Variant#getMergedFlavor()}
     *
     * @return the version code, or -1 if not specified
     */
    int getVersionCode();

    /**
     * Returns the version name. This is only the value set on this product flavor.
     * To get the final value, use {@link Variant#getMergedFlavor()} as well as
     * {@link BuildType#getVersionNameSuffix()}
     *
     * @return the version name.
     */
    @Nullable
    String getVersionName();

    /**
     * Returns the minSdkVersion. This is only the value set on this product flavor.
     *
     * @return the minSdkVersion, or -1 if not specified
     */
    int getMinSdkVersion();

    /**
     * Returns the targetSdkVersion. This is only the value set on this product flavor.
     *
     * @return the targetSdkVersion, or -1 if not specified
     */
    int getTargetSdkVersion();

    /**
     * Returns the renderscript target api. This is only the value set on this product flavor.
     * TODO: make final renderscript target api available through the model
     *
     * @return the renderscript target api, or -1 if not specified
     */
    int getRenderscriptTargetApi();

    /**
     * Returns whether the renderscript code should be compiled in support mode to
     * make it compatible with older versions of Android.
     *
     * @return true if support mode is enabled.
     */
    boolean getRenderscriptSupportMode();

    /**
     * Returns whether the renderscript code should be compiled to generate C/C++ bindings.
     * @return true for C/C++ generation, false for Java
     */
    boolean getRenderscriptNdkMode();

    /**
     * Returns the test package name. This is only the value set on this product flavor.
     * To get the final value, use {@link Variant#getTestArtifactInfo()} and
     * {@link AndroidArtifact#getPackageName()}
     *
     * @return the test package name.
     */
    @Nullable
    String getTestPackageName();

    /**
     * Returns the test instrumentation runner. This is only the value set on this product flavor.
     * TODO: make test instrumentation runner available through the model.
     *
     * @return the test package name.
     */
    @Nullable
    String getTestInstrumentationRunner();

    /**
     * Returns the handlingProfile value. This is only the value set on this product flavor.
     *
     *  @return the handlingProfile value.
     */
    @Nullable
    Boolean getTestHandleProfiling();

    /**
     * Returns the functionalTest value. This is only the value set on this product flavor.
     *
     * @return the functionalTest value.
     */
    @Nullable
    Boolean getTestFunctionalTest();

    /**
     * Returns the NDK configuration.
     * @return the ndk config.
     */
    @Nullable
    NdkConfig getNdkConfig();

    /**
     * Returns the resource configuration for this variant.
     * TODO implement this.
     *
     * This is the list of -c parameters for aapt.
     *
     * @return the resource configuration options.
     */
    @NonNull
    Collection<String> getResourceConfigurations();
}
