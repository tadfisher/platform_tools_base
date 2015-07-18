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
import com.google.common.collect.Iterables;

import java.io.File;

/**
 */
@Immutable
public class OutputStreamImpl implements OutputStream {

    @NonNull
    private final StreamType type;
    @NonNull
    private final StreamScope scope;
    @NonNull
    private final File folder;

    public static OutputStream convert(@NonNull StreamDeclaration streamDeclaration) {
        try {
            return new OutputStreamImpl(streamDeclaration.getType(),
                    streamDeclaration.getScope(),
                    Iterables.getOnlyElement(streamDeclaration.getFiles().call()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OutputStreamImpl(
            @NonNull StreamType type,
            @NonNull StreamScope scope,
            @NonNull File folder) {
        this.type = type;
        this.scope = scope;
        this.folder = folder;
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
    public File getFolder() {
        return folder;
    }
}
