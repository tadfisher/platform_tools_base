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

package com.android.build.gradle.internal.pipeline;

import com.android.annotations.NonNull;
import com.android.annotations.concurrency.Immutable;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Callables;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 */
@Immutable
public class StreamImpl implements Stream {

    @NonNull
    private final StreamType type;
    @NonNull
    private final StreamScope scope;
    @NonNull
    private final Callable<Collection<File>> inputs;
    @NonNull
    private final List<Object> dependencies;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private StreamType type;
        private StreamScope scope;
        private Callable<Collection<File>> inputs;
        private List<Object> dependencies;

        public Stream build() {
            return new StreamImpl(type, scope, inputs, dependencies);
        }

        public Builder from(@NonNull Stream stream) {
            type = stream.getType();
            scope = stream.getScope();
            inputs = stream.getFiles();
            dependencies = stream.getDependencies();
            return this;
        }

        public Builder setType(@NonNull StreamType type) {
            this.type = type;
            return this;
        }

        public Builder setScope(@NonNull StreamScope scope) {
            this.scope = scope;
            return this;
        }

        public Builder setInputs(@NonNull Collection<File> inputFiles) {
            this.inputs = Callables.returning(inputFiles);
            return this;
        }

        public Builder setInputs(@NonNull final File inputFile) {
            this.inputs = new Callable<Collection<File>>() {
                @Override
                public Collection<File> call() {
                    return ImmutableList.of(inputFile);
                }
            };
            return this;
        }

        public Builder setInputs(@NonNull Callable<Collection<File>> inputCallable) {
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
    }

    private StreamImpl(
            @NonNull StreamType type,
            @NonNull StreamScope scope,
            @NonNull Callable<Collection<File>> inputs,
            @NonNull List<Object> dependencies) {
        this.type = type;
        this.scope = scope;
        this.inputs = inputs;
        this.dependencies = dependencies;
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
    public Callable<Collection<File>> getFiles() {
        return inputs;
    }

    @NonNull
    @Override
    public List<Object> getDependencies() {
        return dependencies;
    }
}
