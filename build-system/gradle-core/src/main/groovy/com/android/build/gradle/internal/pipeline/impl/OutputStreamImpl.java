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

package com.android.build.gradle.internal.pipeline.impl;

import com.android.annotations.NonNull;
import com.android.annotations.concurrency.Immutable;
import com.android.build.gradle.internal.pipeline.OutputStream;
import com.android.build.gradle.internal.pipeline.StreamDeclaration;
import com.android.build.gradle.internal.pipeline.StreamScope;
import com.android.build.gradle.internal.pipeline.StreamType;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.io.File;
import java.util.Set;

/**
 */
@Immutable
public class OutputStreamImpl implements OutputStream {

    @NonNull
    private final Set<StreamType> types;
    @NonNull
    private final Set<StreamScope> scopes;
    private final boolean isFolder;
    @NonNull
    private final File file;

    public static OutputStream convert(@NonNull StreamDeclaration streamDeclaration) {
        try {
            return new OutputStreamImpl(
                    streamDeclaration.getTypes(),
                    streamDeclaration.getScopes(),
                    Iterables.getOnlyElement(streamDeclaration.getFiles().call()),
                    streamDeclaration.isFolder());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OutputStreamImpl(
            @NonNull Set<StreamType> types,
            @NonNull Set<StreamScope> scopes,
            @NonNull File file,
            boolean isFolder) {
        this.types = ImmutableSet.copyOf(types);
        this.scopes = ImmutableSet.copyOf(scopes);
        this.file = file;
        this.isFolder = isFolder;
    }

    @NonNull
    @Override
    public Set<StreamType> getTypes() {
        return types;
    }

    @NonNull
    @Override
    public Set<StreamScope> getScopes() {
        return scopes;
    }

    @Override
    public boolean isFolder() {
        return isFolder;
    }

    @NonNull
    @Override
    public File getFile() {
        return file;
    }
}
