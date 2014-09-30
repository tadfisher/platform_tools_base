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

package com.android.build.gradle.internal.model;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.FilterData;
import com.android.build.FilterDataImpl;
import com.android.builder.model.AndroidArtifactOutput;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;

/**
 * Implementation of AndroidArtifactOutput that is serializable
 */
public class AndroidArtifactOutputImpl implements AndroidArtifactOutput, Serializable {
    private static final long serialVersionUID = 1L;

    @NonNull
    private final File outputFile;
    @NonNull
    private final File generatedManifest;
    @NonNull
    private final String assembleTaskName;
    private final int versionCode;
    private final OutputType outputType;
    @NonNull
    private final Collection<FilterData> filters;

    AndroidArtifactOutputImpl(
            @NonNull OutputType outputType,
            @NonNull File outputFile,
            @NonNull String assembleTaskName,
            @NonNull File generatedManifest,
            int versionCode,
            @NonNull Collection<FilterData> filters) {
        this.outputType = outputType;
        this.outputFile = outputFile;
        this.generatedManifest = generatedManifest;
        this.assembleTaskName = assembleTaskName;
        this.versionCode = versionCode;
        this.filters = filters;
    }

    @NonNull
    @Override
    public File getOutputFile() {
        return outputFile;
    }

    @NonNull
    @Override
    public String getAssembleTaskName() {
        return assembleTaskName;
    }

    @NonNull
    @Override
    public File getGeneratedManifest() {
        return generatedManifest;
    }

    @Override
    public int getVersionCode() {
        return versionCode;
    }

    @Override
    public OutputType getOutputType() {
        return outputType;
    }

    @NonNull
    @Override
    public Collection<String> getFilterTypes() {
        ImmutableList.Builder<String> splitTypeBuilder = ImmutableList.builder();
        for (FilterData filter : filters) {
            splitTypeBuilder.add(filter.getFilterType());
        }
        return splitTypeBuilder.build();
    }

    @Nullable
    @Override
    public String getFilter(String filterType) {
        for (FilterData filter : filters) {
            if (filter.getFilterType().equals(filterType)) {
                return filter.getIdentifier();
            }
        }
        return null;
    }

    @NonNull
    @Override
    public Collection<FilterData> getFilters() {
        return filters;
    }
}
