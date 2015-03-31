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

package com.android.build.gradle.internal.scope;

import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.core.GradleVariantConfiguration;
import com.android.build.gradle.internal.variant.ApkVariantData;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.build.gradle.internal.variant.BaseVariantOutputData;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.core.VariantConfiguration;
import com.google.common.collect.Sets;

import org.gradle.api.Project;

import java.io.File;
import java.util.Collection;
import java.util.Set;

/**
 * A scope containing data for a specific variant.
 */
public class VariantScope extends GlobalScope {

    private BaseVariantData<? extends BaseVariantOutputData> variantData;
    private Collection<Object> ndkBuildable;
    private Collection<File> ndkOutputDirectories;

    public VariantScope(VariantScope other) {
        this(other, other.variantData, other.ndkBuildable, other.ndkOutputDirectories);
    }

    public VariantScope(
            GlobalScope globalScope,
            BaseVariantData<? extends BaseVariantOutputData> variantData,
            Collection<Object> ndkBuildable,
            Collection<File> ndkOutputDirectories) {
        super(globalScope);
        this.variantData = variantData;
        this.ndkBuildable = ndkBuildable;
        this.ndkOutputDirectories = ndkOutputDirectories;
    }

    public BaseVariantData<? extends BaseVariantOutputData> getVariantData() {
        return variantData;
    }

    public GradleVariantConfiguration getVariantConfiguration() {
        return variantData.getVariantConfiguration();
    }

    public Collection<Object> getNdkBuildable() {
        return ndkBuildable;
    }

    public Collection<File> getNdkOutputDirectories() {
        return ndkOutputDirectories;
    }

    public Set<File> getJniFolders() {
        VariantConfiguration config = getVariantConfiguration();
        ApkVariantData apkVariantData = (ApkVariantData) variantData;
        // for now only the project's compilation output.
        Set<File> set = Sets.newHashSet();
        set.addAll(getNdkOutputDirectories());
        set.add(apkVariantData.renderscriptCompileTask.getLibOutputDir());
        set.addAll(config.getLibraryJniFolders());
        set.addAll(config.getJniLibsList());

        if (config.getMergedFlavor().getRenderscriptSupportModeEnabled() != null &&
                config.getMergedFlavor().getRenderscriptSupportModeEnabled()) {
            File rsLibs = getAndroidBuilder().getSupportNativeLibFolder();
            if (rsLibs != null && rsLibs.isDirectory()) {
                set.add(rsLibs);
            }
        }
        return set;
    }
}
