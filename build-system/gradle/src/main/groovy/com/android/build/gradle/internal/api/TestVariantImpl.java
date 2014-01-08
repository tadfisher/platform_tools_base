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

package com.android.build.gradle.internal.api;

import com.android.annotations.NonNull;
import com.android.build.gradle.BasePlugin;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.api.TestVariant;
import com.android.build.gradle.internal.variant.ApkVariantData;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.build.gradle.internal.variant.TestVariantData;

import org.gradle.api.DefaultTask;

import java.util.List;

/**
 * implementation of the {@link TestVariant} interface around an {@link TestVariantData} object.
 */
public class TestVariantImpl extends ApkVariantImpl implements TestVariant {

    @NonNull
    private final TestVariantData variantData;
    @NonNull
    private BaseVariant testedVariant;

    public TestVariantImpl(@NonNull TestVariantData variantData, @NonNull BasePlugin plugin) {
        super(plugin);
        this.variantData = variantData;
    }

    @Override
    @NonNull
    protected BaseVariantData getVariantData() {
        return variantData;
    }

    @Override
    @NonNull
    protected ApkVariantData getApkVariantData() {
        return variantData;
    }

    @Override
    @NonNull
    public BaseVariant getTestedVariant() {
        return testedVariant;
    }

    public void setTestedVariant(@NonNull BaseVariant testedVariant) {
        this.testedVariant = testedVariant;
    }

    @Override
    public DefaultTask getConnectedInstrumentTest() {
        return variantData.connectedTestTask;
    }

    @NonNull
    @Override
    public List<? extends DefaultTask> getProviderInstrumentTests() {
        return variantData.providerTestTaskList;
    }
}
