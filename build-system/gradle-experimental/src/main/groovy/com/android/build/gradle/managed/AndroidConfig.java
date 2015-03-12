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

package com.android.build.gradle.managed;

import com.android.build.gradle.internal.CompileOptions;
import com.android.build.gradle.internal.coverage.JacocoExtension;
import com.android.build.gradle.internal.dsl.AaptOptions;
import com.android.build.gradle.internal.dsl.AdbOptions;
import com.android.build.gradle.internal.dsl.DexOptions;
import com.android.build.gradle.internal.dsl.LintOptions;
import com.android.build.gradle.internal.dsl.PackagingOptions;
import com.android.build.gradle.internal.dsl.Splits;
import com.android.build.gradle.internal.dsl.TestOptions;
import com.android.build.gradle.model.AndroidComponentModelSourceSet;
import com.android.builder.testing.api.DeviceProvider;
import com.android.builder.testing.api.TestServer;
import com.android.sdklib.repository.FullRevision;

import org.gradle.model.Managed;
import org.gradle.model.Unmanaged;
import org.gradle.model.collection.ManagedSet;

import java.util.List;

import groovy.lang.Closure;

/**
 * Component model for all Android plugin.
 */
@Managed
public interface AndroidConfig {

    String getBuildToolsVersion();
    void setBuildToolsVersion(String buildToolsVersion);

    String getCompileSdkVersion();
    void setCompileSdkVersion(String compileSdkVersion);

    @Unmanaged
    FullRevision getBuildToolsRevision();
    void setBuildToolsRevision(FullRevision fullRevision);

    ProductFlavor getDefaultConfig();

    @Unmanaged
    List<DeviceProvider> getDeviceProviders();
    void setDeviceProviders(List<DeviceProvider> providers);

    @Unmanaged
    List<TestServer> getTestServers();
    void setTestServers(List<TestServer> providers);

    String getDefaultPublishConfig();
    void setDefaultPublishConfig(String defaultPublishConfig);

    Boolean getPublishNonDefault();
    void setPublishNonDefault(Boolean publishNonDefault);

    @Unmanaged
    Closure<Void> getVariantFilter();
    void setVariantFilter(Closure<Void> filter);

    String getResourcePrefix();
    void setResourcePrefix(String resourcePrefix);

    Boolean getGeneratePureSplits();
    void setGeneratePureSplits(Boolean generateSplits);

    Boolean getPreprocessResources();
    void setPreprocessResources(Boolean preprocessResources);

    ManagedSet<BuildType> getBuildTypes();

    ManagedSet<ProductFlavor> getProductFlavors();

    ManagedSet<SigningConfig> getSigningConfigs();

    @Unmanaged
    AndroidComponentModelSourceSet getSources();
    void setSources(AndroidComponentModelSourceSet sources);

    NdkConfig getNdk();

    @Unmanaged
    AdbOptions getAdbOptions();
    void setAdbOptions(AdbOptions adbOptions);

    @Unmanaged
    AaptOptions getAaptOptions();
    void setAaptOptions(AaptOptions aaptOptions);

    @Unmanaged
    CompileOptions getCompileOptions();
    void setCompileOptions(CompileOptions compileOptions);

    @Unmanaged
    DexOptions getDexOptions();
    void setDexOptions(DexOptions dexOptions);

    @Unmanaged
    JacocoExtension getJacoco();
    void setJacoco(JacocoExtension jacoco);

    @Unmanaged
    LintOptions getLintOptions();
    void setLintOptions(LintOptions lintOptions);

    @Unmanaged
    PackagingOptions getPackagingOptions();
    void setPackagingOptions(PackagingOptions packagingOptions);

    @Unmanaged
    TestOptions getTestOptions();
    void setTestOptions(TestOptions testOptions);

    @Unmanaged
    Splits getSplits();
    void setSplits(Splits splits);
}
