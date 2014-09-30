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

package com.android.build;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.io.File;
import java.util.Collection;

/**
 * An output with an associated set of filters.
 */
public interface SplitOutput {

    /**
     * An object representing the lack of filter.
     */
    public static final String NO_FILTER = null;

    /**
     * Type of package file, either the main APK or a pure split APK file containing resources for
     * a particular split dimension.
     */
    public enum OutputType {
        MAIN, FULL_SPLIT, SPLIT
    }

    /**
     * Split dimension type
     */
    public enum FilterType {
        DENSITY, ABI, LANGUAGE
    }

    /**
     * Returns the output file for this artifact's output.
     * Depending on whether the project is an app or a library project, this could be an apk or
     * an aar file.
     *
     * For test artifact for a library project, this would also be an apk.
     *
     * @return the output file.
     */
    @NonNull
    File getOutputFile();

    /**
     * The density filter if applicable.
     * @return the density filter or null if not applicable.
     */
    @Deprecated
    @Nullable
    String getDensityFilter();

    /**
     * The ABI filter if applicable.
     * @return the ABI filter or null if not applicable.
     */
    @Deprecated
    @Nullable
    String getAbiFilter();

    /**
     * Returns the output type of the referenced APK.
     */
    OutputType getOutputType();

    /**
     * Returns the split dimensions the referenced APK was created with.
     */
    @NonNull
    public Collection<FilterType> getFilterTypes();

    /**
     * Returns all the split information used to create the APK.
     */
    @NonNull
    public Collection<SplitData> getFilters();

    /**
     * Returns the split identifier (like "hdpi" for a density split) given the split dimension.
     * @param filterType the split dimension used to create the APK.
     * @return the split identifier or null if there was not split of that dimension.
     */
    @Nullable
    public String getFilter(FilterType filterType);

    /**
     * The output versionCode.
     *
     * In case of multi-apk, the version code of each apk is different.
     *
     * @return the versionCode
     */
    int getVersionCode();
}
