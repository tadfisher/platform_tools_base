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

import com.android.build.gradle.internal.variant.ApkVariantData;
import com.android.build.gradle.internal.variant.BaseVariantOutputData;

import java.io.File;

/**
 * A scope containing data for a specific variant.
 */
public class VariantOutputScope extends VariantScope {

    private BaseVariantOutputData variantOutputData;

    public VariantOutputScope(
            VariantScope variantScope,
            BaseVariantOutputData variantOutputData) {
        super(variantScope);
        this.variantOutputData = variantOutputData;
    }

    public BaseVariantOutputData getVariantOutputData() {
        return variantOutputData;
    }

    public File getPackageApk() {
        ApkVariantData apkVariantData = (ApkVariantData) getVariantData();

        boolean signedApk = apkVariantData.isSigned();
        String apkName = signedApk ?
                getProjectBaseName() + "-" + variantOutputData.getBaseName() + "-unaligned.apk" :
                getProjectBaseName() + "-" + variantOutputData.getBaseName() + "-unsigned.apk";

        // if this is the final task then the location is
        // the potentially overridden one.
        if (!signedApk || !apkVariantData.getZipAlignEnabled()) {
            return getProject().file(getApkLocation() + "/" + apkName);
        } else {
            // otherwise default one.
            return getProject().file(getDefaultApkLocation() + "/" + apkName);
        }
    }
}
