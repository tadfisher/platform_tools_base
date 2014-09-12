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

package com.android.build.gradle.internal.test;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.internal.InstallHelper;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.build.gradle.internal.variant.BaseVariantOutputData;
import com.android.build.gradle.internal.variant.FilteredOutput;
import com.android.builder.core.VariantConfiguration;
import com.android.builder.model.ApiVersion;
import com.android.builder.testing.TestData;

import java.io.File;

/**
 */
public class TestDataImpl implements TestData {

    @NonNull
    private final BaseVariantData<? extends BaseVariantOutputData> variantData;
    @NonNull
    private final VariantConfiguration variantConfig;

    public TestDataImpl(@NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        this.variantData = variantData;
        this.variantConfig = variantData.getVariantConfiguration();
    }

    @NonNull
    @Override
    public String getApplicationId() {
        return variantConfig.getApplicationId();
    }

    @Nullable
    @Override
    public String getTestedApplicationId() {
        return variantConfig.getTestedApplicationId();
    }

    @NonNull
    @Override
    public String getInstrumentationRunner() {
        return variantConfig.getInstrumentationRunner();
    }

    @NonNull
    @Override
    public Boolean getHandleProfiling() {
        return variantConfig.getHandleProfiling();
    }

    @NonNull
    @Override
    public Boolean getFunctionalTest() {
        return variantConfig.getFunctionalTest();
    }

    @Override
    public boolean isTestCoverageEnabled() {
        return variantConfig.isTestCoverageEnabled();
    }

    @Override
    public ApiVersion getMinSdkVersion() {
        return variantConfig.getMinSdkVersion();
    }

    @Nullable
    @Override
    public File getApk(int density, @NonNull String... abis) {
        FilteredOutput output = InstallHelper.getOutput(variantData.getOutputs(), density, abis);
        if (output != null) {
            return output.getOutputFile();
        }

        return null;
    }
}
