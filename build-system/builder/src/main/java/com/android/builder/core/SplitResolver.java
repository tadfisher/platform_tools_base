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

package com.android.builder.core;

import static com.google.common.base.Preconditions.checkState;

import com.android.annotations.NonNull;
import com.android.ide.common.resources.configuration.DensityQualifier;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.resources.Density;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.Set;

/**
 */
public class SplitResolver {

    /**
     * Resolves a Density split dimension based on a given res folder.
     *
     * This returns a ResolvedSplitDimension with a list of Split objects that can be used
     * to generate variants.
     *
     * When using multiple APK, the Split will also contain the "nodpi" value.
     *
     * @param splisplitDimension the unresolved dimension
     * @param resFolder the resource folder
     * @param multiApk whether the Split should be configured for multiple APK.
     *
     * @return the resolved dimension
     */
    public static ResolvedSplitDimension resolveDensity(
            @NonNull SplitDimension splisplitDimension,
            @NonNull File resFolder,
            boolean multiApk) {

        checkState(splisplitDimension.getType() == SplitType.DENSITY, "Wrong SplitType");

        Set<String> densities = Sets.newHashSet();

        Set<String> includes = splisplitDimension.getIncludes();
        Set<String> excludes = splisplitDimension.getExcludes();
        boolean containsAll = includes.contains(SplitDimension.ALL);

        File[] folders = resFolder.listFiles();
        if (folders != null) {
            for (File folder : folders) {
                FolderConfiguration folderConfig = FolderConfiguration
                        .getConfigForFolder(folder.getName());
                if (folderConfig == null) {
                    continue;
                }

                DensityQualifier densityQualifier = folderConfig.getDensityQualifier();
                if (densityQualifier != null) {
                    String value = densityQualifier.getValue().getResourceValue();

                    if (includes.contains(value) || (containsAll && !excludes.contains(value))) {
                        densities.add(value);
                    }
                }
            }
        }

        Set<Split> splits = Sets.newHashSetWithExpectedSize(densities.size());
        final String noDpi = Density.NODPI.getResourceValue();
        for (String density : densities) {
            if (multiApk) {
                splits.add(new Split(SplitType.DENSITY, density, noDpi));
            } else {
                splits.add(new Split(SplitType.DENSITY, density));
            }
        }

        return new ResolvedSplitDimension(SplitType.DENSITY, splits);
    }

    public static ResolvedSplitDimension resolveAbi(
            @NonNull SplitDimension splitDimension) {
        // TODO
        return null;
    }
}
