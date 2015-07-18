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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 */
public class JacocoTransform implements Transform {

    @NonNull
    @Override
    public String getName() {
        return "jacoco";
    }

    @NonNull
    @Override
    public Set<StreamType> getTypes() {
        return EnumSet.of(StreamType.CODE);
    }

    @NonNull
    @Override
    public StreamScope getScope() {
        // only run on the project classes
        return StreamScope.PROJECT;
    }

    @NonNull
    @Override
    public TransformType getTransformType() {
        // does not combine multiple input stream.
        return TransformType.AS_INPUT;
    }

    @Override
    public void transform(@NonNull List<Stream> inputs, @NonNull List<Stream> outputs) {

    }
}
