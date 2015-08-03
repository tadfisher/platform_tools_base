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
import com.android.build.gradle.internal.pipeline.impl.InputStreamImpl;
import com.android.build.gradle.internal.pipeline.impl.OutputStreamImpl;
import com.android.build.gradle.internal.scope.TaskConfigAction;
import com.google.common.collect.Lists;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A task doing a bytecode transformation
 */
public class TransformTask extends StreamBasedTask {

    private Transform transform;

    public Transform getTransform() {
        return transform;
    }

    @TaskAction
    void transform(IncrementalTaskInputs taskInputs) throws Exception {
        boolean isIncremental = transform.isIncremental() && taskInputs.isIncremental();

        //noinspection ConstantConditions
        if (false) { //isIncremental) {
            // ?
        } else {
            transform.transform(resolveInputStreams(), resolveOutputStreams(), false);
        }
    }

    private List<OutputStream> resolveOutputStreams() {
        List<OutputStream> results = Lists.newArrayListWithCapacity(outputStreams.size());

        for (StreamDeclaration output : outputStreams) {
            results.add(OutputStreamImpl.convert(output));
        }

        return results;
    }

    private List<InputStream> resolveInputStreams() {
        List<InputStream> results = Lists.newArrayListWithCapacity(
                consumedInputStreams.size() + referencedInputStreams.size());

        for (StreamDeclaration input : consumedInputStreams) {
            results.add(InputStreamImpl.builder().from(input).build());
        }

        for (StreamDeclaration input : referencedInputStreams) {
            results.add(InputStreamImpl.builder().from(input).setReferencedOnly().build());
        }

        return results;
    }

    @InputFiles
    public Collection<File> getOtherFileInputs() {
        return transform.getSecondaryFileInputs();
    }

    @OutputFiles
    public Collection<File> getOtherFileOutputs() {
        return transform.getSecondaryFileOutputs();
    }

    @Input
    Map<String, Object> getOtherInputs() {
        return transform.getParameterInputs();
    }

    public static class ConfigAction implements TaskConfigAction<TransformTask> {

        @NonNull
        private final String variantName;
        @NonNull
        private final String taskName;
        @NonNull
        private final Transform transform;
        @NonNull
        private List<StreamDeclaration> consumedInputStreams;
        @NonNull
        private List<StreamDeclaration> referencedInputStreams;
        @NonNull
        private List<StreamDeclaration> outputStreams;

        ConfigAction(
                @NonNull String variantName,
                @NonNull String taskName,
                @NonNull Transform transform,
                @NonNull List<StreamDeclaration> consumedInputStreams,
                @NonNull List<StreamDeclaration> referencedInputStreams,
                @NonNull List<StreamDeclaration> outputStreams) {
            this.variantName = variantName;
            this.taskName = taskName;
            this.transform = transform;
            this.consumedInputStreams = consumedInputStreams;
            this.referencedInputStreams = referencedInputStreams;
            this.outputStreams = outputStreams;
        }

        @NonNull
        @Override
        public String getName() {
            return taskName;
        }

        @NonNull
        @Override
        public Class<TransformTask> getType() {
            return TransformTask.class;
        }

        @Override
        public void execute(TransformTask task) {
            task.transform = transform;
            task.consumedInputStreams = consumedInputStreams;
            task.referencedInputStreams = referencedInputStreams;
            task.outputStreams = outputStreams;
            task.setVariantName(variantName);
        }
    }
}
