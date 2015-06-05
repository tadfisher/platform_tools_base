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

package com.android.build.gradle.internal.model;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.NdkHandler;
import com.android.build.gradle.internal.core.Abi;
import com.android.build.gradle.internal.core.NdkConfig;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.build.gradle.internal.variant.BaseVariantOutputData;
import com.android.builder.model.NativeLibrary;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of DefaultNativeLibraryFactory.
 */
public class DefaultNativeLibraryFactory implements NativeLibraryFactory {

    NdkHandler ndkHandler;

    public DefaultNativeLibraryFactory(NdkHandler ndkHandler) {
        this.ndkHandler = ndkHandler;
    }

    @Override
    public NativeLibrary create(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @NonNull String toolchainName, @NonNull Abi abi) {
        NdkConfig ndkConfig = variantData.getVariantConfiguration().getNdkConfig();

        String sysrootFlag = "--sysroot=" + ndkHandler.getSysroot(abi);
        List<String> cFlags = ndkConfig.getcFlags() == null
                ? ImmutableList.of(sysrootFlag)
                : ImmutableList.of(sysrootFlag, ndkConfig.getcFlags());

        // The DSL currently do not support all options available in the model such as the
        // include dirs and the defines.  Therefore, just pass an empty collection for now.
        return new NativeLibraryImpl(
                ndkConfig.getModuleName(),
                toolchainName,
                abi.getName(),
                Collections.<File>emptyList(),  /*cIncludeDirs*/
                Collections.<File>emptyList(),  /*cppIncludeDirs*/
                Collections.<File>emptyList(),  /*cSystemIncludeDirs*/
                ndkHandler.getStlIncludes(ndkConfig.getStl(), abi),
                Collections.<String>emptyList(),  /*cDefines*/
                Collections.<String>emptyList(),  /*cppDefines*/
                cFlags,
                cFlags,  // TODO: NdkConfig should allow cppFlags to be set separately.
                ImmutableList.of(variantData.getScope().getNdkDebuggableLibraryFolders(abi)));
    }
}
