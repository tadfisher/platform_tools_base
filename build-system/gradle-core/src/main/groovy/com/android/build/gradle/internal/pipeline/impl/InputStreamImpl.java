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

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 */
@Immutable
public class InputStreamImpl implements InputStream {

    @NonNull
    private final StreamType type;
    @NonNull
    private final StreamScope scope;
    @NonNull
    private final Collection<File> files;
    @NonNull
    private final Map<File, FileStatus> changedFiles;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private StreamType type;
        private StreamScope scope;
        private Collection<File> files;
        private Map<File, FileStatus> changedFiles;

        public InputStream build() {
            return new InputStreamImpl(type, scope, files,
                    changedFiles != null ? ImmutableMap.copyOf(changedFiles) :
                            ImmutableMap.<File, FileStatus>of());
        }

        public Builder from(@NonNull StreamDeclaration stream) {
            try {
                type = stream.getType();
                scope = stream.getScope();
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
    }

    private InputStreamImpl(
            @NonNull StreamType type,
            @NonNull StreamScope scope,
            @NonNull Collection<File> files,
            @NonNull Map<File, FileStatus> changedFiles) {
        this.type = type;
        this.scope = scope;
        this.files = files;
        this.changedFiles = changedFiles;
    }

    @NonNull
    @Override
    public StreamType getType() {
        return type;
    }

    @NonNull
    @Override
    public StreamScope getScope() {
        return scope;
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
}
