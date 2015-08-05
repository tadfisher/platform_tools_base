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
import com.android.build.gradle.internal.pipeline.StreamDeclaration;
import com.android.build.gradle.internal.pipeline.StreamScope;
import com.android.build.gradle.internal.pipeline.StreamType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Callables;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 */
@Immutable
public class StreamDeclarationImpl implements StreamDeclaration {

    @NonNull
    private final Set<StreamType> types;
    @NonNull
    private final Set<StreamScope> scopes;
    @NonNull
    private final Callable<Collection<File>> files;
    @NonNull
    private final List<Object> dependencies;
    private final boolean isFolder;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Set<StreamType> types = Sets.newHashSet();
        private Set<StreamScope> scopes = Sets.newHashSet();
        private Callable<Collection<File>> inputs;
        private List<Object> dependencies;
        private boolean isFolder = true;

        public StreamDeclaration build() {
            return new StreamDeclarationImpl(
                    Sets.immutableEnumSet(types),
                    Sets.immutableEnumSet(scopes),
                    inputs,
                    dependencies != null ? dependencies : ImmutableList.of(),
                    isFolder);
        }

        public Builder from(@NonNull StreamDeclaration stream) {
            types.addAll(stream.getTypes());
            scopes.addAll(stream.getScopes());
            inputs = stream.getFiles();
            dependencies = stream.getDependencies();
            return this;
        }

        public Builder copyWithRestrictedTypes(
                @NonNull StreamDeclaration stream,
                @NonNull Set<StreamType> types) {
            this.types.addAll(types);
            scopes.addAll(stream.getScopes());
            inputs = stream.getFiles();
            dependencies = stream.getDependencies();
            return this;
        }

        public Builder addTypes(@NonNull Set<StreamType> types) {
            this.types.addAll(types);
            return this;
        }

        public Builder addTypes(@NonNull StreamType... types) {
            this.types.addAll(Arrays.asList(types));
            return this;
        }

        public Builder addType(@NonNull StreamType type) {
            this.types.add(type);
            return this;
        }

        public Builder addScopes(@NonNull Set<StreamScope> scopes) {
            this.scopes.addAll(scopes);
            return this;
        }

        public Builder addScopes(@NonNull StreamScope... scopes) {
            this.scopes.addAll(Arrays.asList(scopes));
            return this;
        }

        public Builder addScope(@NonNull StreamScope scope) {
            this.scopes.add(scope);
            return this;
        }

        public Builder setFiles(@NonNull Collection<File> inputFiles) {
            this.inputs = Callables.returning(inputFiles);
            return this;
        }

        public Builder setFiles(@NonNull final File file) {
            this.inputs = Callables.returning((Collection<File>) ImmutableList.of(file));
            return this;
        }

        public Builder setFiles(@NonNull Callable<Collection<File>> inputCallable) {
            this.inputs = inputCallable;
            return this;
        }

        public Builder setDependencies(@NonNull List<Object> dependencies) {
            this.dependencies = ImmutableList.copyOf(dependencies);
            return this;
        }

        public Builder setDependency(@NonNull Object dependency) {
            this.dependencies = ImmutableList.of(dependency);
            return this;
        }

        public Builder setFolder(boolean isFolder) {
            this.isFolder = isFolder;
            return this;
        }
    }

    private StreamDeclarationImpl(
            @NonNull Set<StreamType> types,
            @NonNull Set<StreamScope> scopes,
            @NonNull Callable<Collection<File>> files,
            @NonNull List<Object> dependencies,
            boolean isFolder) {
        this.types = types;
        this.scopes = scopes;
        this.files = files;
        this.dependencies = dependencies;
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

    @NonNull
    @Override
    public Callable<Collection<File>> getFiles() {
        return files;
    }

    @Override
    public boolean isFolder() {
        return isFolder;
    }

    @NonNull
    @Override
    public List<Object> getDependencies() {
        return dependencies;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("StreamDeclarationImpl{");
        sb.append("types=").append(types);
        sb.append(", scopes=").append(scopes);
        try {
            sb.append(", files=").append(files.call());
        } catch (Exception e) {
        }
        sb.append(", dependencies=").append(dependencies);
        sb.append(", isFolder=").append(isFolder);
        sb.append('}');
        return sb.toString();
    }
}
