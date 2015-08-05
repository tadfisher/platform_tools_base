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

package com.android.build.gradle.internal.transforms;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.pipeline.InputStream;
import com.android.build.gradle.internal.pipeline.OutputStream;
import com.android.build.gradle.internal.pipeline.StreamScope;
import com.android.build.gradle.internal.pipeline.StreamType;
import com.android.build.gradle.internal.pipeline.Transform;
import com.android.build.gradle.internal.pipeline.TransformException;
import com.android.build.gradle.internal.pipeline.TransformPipeline;
import com.android.build.gradle.internal.pipeline.TransformType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Transform to merge all the Java resources
 */
public class MergeJavaTransform implements Transform {

    @NonNull
    @Override
    public String getName() {
        return "mergeJavaRes";
    }

    @NonNull
    @Override
    public Set<StreamType> getInputTypes() {
        return TransformPipeline.TYPE_RESOURCES;
    }

    @NonNull
    @Override
    public Set<StreamType> getOutputTypes() {
        return TransformPipeline.TYPE_RESOURCES;
    }

    @NonNull
    @Override
    public Set<StreamScope> getScopes() {
        return Sets.immutableEnumSet(
                StreamScope.PROJECT,
                StreamScope.PROJECT_LOCAL_DEPS,
                StreamScope.SUB_PROJECTS,
                StreamScope.SUB_PROJECTS_LOCAL_DEPS,
                StreamScope.EXTERNAL_LIBRARIES);
    }

    @NonNull
    @Override
    public Set<StreamScope> getReferencedScopes() {
        return TransformPipeline.EMPTY_SCOPES;
    }

    @NonNull
    @Override
    public TransformType getTransformType() {
        return TransformType.COMBINED;
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileInputs() {
        return ImmutableList.of();
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileOutputs() {
        return ImmutableList.of();
    }

    @NonNull
    @Override
    public Map<String, Object> getParameterInputs() {
        // TODO the inputs that controls the merge.
        return ImmutableMap.of();
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(@NonNull List<InputStream> inputs, @NonNull List<OutputStream> outputs,
            boolean isIncremental) throws TransformException {

    }
}
