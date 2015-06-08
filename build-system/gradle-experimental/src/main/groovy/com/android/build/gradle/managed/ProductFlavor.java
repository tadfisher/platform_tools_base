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

package com.android.build.gradle.managed;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import org.gradle.api.Named;
import org.gradle.model.Managed;
import org.gradle.model.ModelSet;
import org.gradle.model.Unmanaged;

import java.io.File;
import java.util.List;
import java.util.Set;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * A Managed product flavor.
 *
 * TODO: Convert Unmanaged Collection to Managed type when Gradle provides ModelSet for basic class.
 */
@Managed
public interface ProductFlavor extends Named {

    @Nullable
    String getDimension();
    void setDimension(String dimension);

    @NonNull
    ModelSet<ClassField> getBuildConfigFields();

    @NonNull
    ModelSet<ClassField> getResValues();

    @Unmanaged
    Set<File> getProguardFiles();
    void setProguardFiles(Set<File> files);

    @Unmanaged
    Set<File> getConsumerProguardFiles();
    void setConsumerProguardFiles(Set<File> files);

    @Unmanaged
    Set<File> getTestProguardFiles();
    void setTestProguardFiles(Set<File> files);

    // TODO: Add the commented fields.
    //Map<String, Object> getManifestPlaceholders();

    @Nullable
    Boolean getMultiDexEnabled();
    void setMultiDexEnabled(Boolean multiDexEnabled);

    @Nullable
    File getMultiDexKeepFile();
    void setMultiDexKeepFile(File multiDexKeepFile);

    @Nullable
    File getMultiDexKeepProguard();
    void setMultiDexKeepProguard(File multiDexKeepProguard);

    @Nullable
    String getApplicationId();
    void setApplicationId(String applicationId);

    @Nullable
    Integer getVersionCode();
    void setVersionCode(Integer versionCode);

    @Nullable
    String getVersionName();
    void setVersionName(String versionName);

    @Nullable
    ApiVersion getMinSdkVersion();

    @Nullable
    ApiVersion getTargetSdkVersion();

    @Nullable
    Integer getMaxSdkVersion();
    void setMaxSdkVersion(Integer maxSdkVersion);

    @Nullable
    Integer getRenderscriptTargetApi();
    void setRenderscriptTargetApi(Integer renderscriptTargetApi);

    @Nullable
    Boolean getRenderscriptSupportModeEnabled();
    void setRenderscriptSupportModeEnabled(Boolean renderscriptSupportModeEnabled);

    @Nullable
    Boolean getRenderscriptNdkModeEnabled();
    void setRenderscriptNdkModeEnabled(Boolean renderscriptNdkModeEnabled);

    @Nullable
    String getTestApplicationId();
    void setTestApplicationId(String testApplicationId);

    @Nullable
    String getTestInstrumentationRunner();
    void setTestInstrumentationRunner(String testInstrumentationRunner);

    @Nullable
    Boolean getTestHandleProfiling();
    void setTestHandleProfiling(Boolean testHandleProfiling);

    @Nullable
    Boolean getTestFunctionalTest();
    void setTestFunctionalTest(Boolean testFunctionalTest);

    @Unmanaged
    @Nullable
    Set<String> getResourceConfigurations();
    void setResourceConfigurations(Set<String> resourceConfigurations);

    SigningConfig getSigningConfig();
    void setSigningConfig(SigningConfig signingConfig);

    Boolean getUseJack();
    void setUseJack(Boolean useJack);

    NdkConfig getNdkConfig();

    @Unmanaged
    List<File> getJarJarRuleFiles();
    void setJarJarRuleFiles(List<File> jarJarRuleFiles);
}
