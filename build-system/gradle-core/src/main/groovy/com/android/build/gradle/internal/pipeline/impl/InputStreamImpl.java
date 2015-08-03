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
import com.android.build.gradle.internal.pipeline.InputStream;
import com.android.build.gradle.internal.pipeline.StreamDeclaration;
import com.android.build.gradle.internal.pipeline.StreamScope;
import com.android.build.gradle.internal.pipeline.StreamType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 */
@Immutable
public class InputStreamImpl implements InputStream {

    @NonNull
    private final Set<StreamType> types;
    @NonNull
    private final Set<StreamScope> scopes;
    @NonNull
    private final Collection<File> files;
    @NonNull
    private final Map<File, FileStatus> changedFiles;
    private final boolean isReferencedOnly;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Set<StreamType> types;
        private Set<StreamScope> scopes;
        private Collection<File> files;
        private Map<File, FileStatus> changedFiles;
        private boolean isReferencedOnly = false;

        public InputStream build() {
            return new InputStreamImpl(
                    types,
                    scopes,
                    files,
                    changedFiles != null ? ImmutableMap.copyOf(changedFiles) :
                            ImmutableMap.<File, FileStatus>of(),
                    isReferencedOnly);
        }

        public Builder from(@NonNull StreamDeclaration stream) {
            try {
                types = ImmutableSet.copyOf(stream.getTypes());
                scopes = ImmutableSet.copyOf(stream.getScopes());
                files = stream.getFiles().call();
                return this;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Builder setChangedFiles(Map<File, FileStatus> changedFiles) {
            this.changedFiles = changedFiles;
            return this;
        }

        public Builder setReferencedOnly() {
            isReferencedOnly = true;
            return this;
        }
    }

    private InputStreamImpl(
            @NonNull Set<StreamType> types,
            @NonNull Set<StreamScope> scopes,
            @NonNull Collection<File> files,
            @NonNull Map<File, FileStatus> changedFiles,
            boolean isReferencedOnly) {
        this.types = types;
        this.scopes = scopes;
        this.files = files;
        this.changedFiles = changedFiles;
        this.isReferencedOnly = isReferencedOnly;
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

    @NonNull
    @Override
    public Collection<File> getFiles() {
        return files;
    }

    @NonNull
    @Override
    public Map<File, FileStatus> getChangedFiles() {
        return changedFiles;
    }

    @Override
    public boolean isReferencedOnly() {
        return isReferencedOnly;
    }
}
