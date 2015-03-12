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

package com.android.build.gradle;

import com.android.annotations.NonNull;
import com.android.build.gradle.api.AndroidSourceSet;
import com.android.build.gradle.internal.CompileOptions;
import com.android.build.gradle.internal.coverage.JacocoExtension;
import com.android.build.gradle.internal.dsl.AaptOptions;
import com.android.build.gradle.internal.dsl.AdbOptions;
import com.android.build.gradle.internal.dsl.CoreBuildType;
import com.android.build.gradle.internal.dsl.CoreProductFlavor;
import com.android.build.gradle.internal.dsl.DexOptions;
import com.android.build.gradle.internal.dsl.LintOptions;
import com.android.build.gradle.internal.dsl.PackagingOptions;
import com.android.build.gradle.internal.dsl.Splits;
import com.android.build.gradle.internal.dsl.TestOptions;
import com.android.builder.model.SigningConfig;
import com.android.builder.testing.api.DeviceProvider;
import com.android.builder.testing.api.TestServer;
import com.android.sdklib.repository.FullRevision;

import org.gradle.api.NamedDomainObjectContainer;

import java.util.Collection;
import java.util.List;

import groovy.lang.Closure;

/**
 * User configuration settings for all android plugins.
 */
public interface AndroidConfig {

    String getBuildToolsVersion();

    String getCompileSdkVersion();

    FullRevision getBuildToolsRevision();

    String getDefaultPublishConfig();

    boolean getPublishNonDefault();

    Closure<Void> getVariantFilter();

    AdbOptions getAdbOptions();

    String getResourcePrefix();

    List<String> getFlavorDimensionList();

    boolean getGeneratePureSplits();

    boolean getGeneratePngs();

    boolean getEnforceUniquePackageName();

    CoreProductFlavor getDefaultConfig();

    AaptOptions getAaptOptions();

    CompileOptions getCompileOptions();

    DexOptions getDexOptions();

    JacocoExtension getJacoco();

    LintOptions getLintOptions();

    PackagingOptions getPackagingOptions();

    Splits getSplits();

    TestOptions getTestOptions();

    @NonNull
    List<DeviceProvider> getDeviceProviders();

    @NonNull
    List<TestServer> getTestServers();

    Collection<? extends CoreProductFlavor> getProductFlavors();

    Collection<? extends CoreBuildType> getBuildTypes();

    Collection<? extends SigningConfig> getSigningConfigs();

    NamedDomainObjectContainer<AndroidSourceSet> getSourceSets();

    Boolean getPackageBuildConfig();
}
