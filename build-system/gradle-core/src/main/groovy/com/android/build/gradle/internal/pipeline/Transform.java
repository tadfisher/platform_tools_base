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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A bytecode transform
 */
public interface Transform {

    @NonNull
    String getName();

    /** The consumed input types */
    @NonNull
    Set<StreamType> getInputTypes();

    @NonNull
    Set<StreamType> getOutputTypes();

    @NonNull
    Set<StreamScope> getScopes();

    @NonNull
    Set<StreamScope> getReferencedScope();

    @NonNull
    TransformType getTransformType();

    @NonNull
    Collection<File> getSecondaryFileInputs();

    @NonNull
    Collection<File> getSecondaryFileOutputs();

    @NonNull
    Map<String, Object> getParameterInputs();

    boolean isIncremental();

    void transform(
            @NonNull List<InputStream> inputs,
            @NonNull List<OutputStream> outputs,
            boolean isIncremental) throws TransformException;
}
