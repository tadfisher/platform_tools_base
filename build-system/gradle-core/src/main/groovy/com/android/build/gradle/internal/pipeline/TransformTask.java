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
import com.android.annotations.Nullable;
import com.android.build.gradle.internal.scope.TaskConfigAction;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.tasks.DefaultAndroidTask;
import com.android.utils.StringHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.List;

/**
 * A task doing a bytecode transformation
 */
public class TransformTask extends DefaultAndroidTask {

    private Transform transform;
    private List<Stream> inputStreams;
    private Stream outputStream;

    TransformTask() {
    }

    public Transform getTransform() {
        return transform;
    }

    @TaskAction
    void transform() {
        // TODO: handle incremental mode
        transform.transform(inputStreams, outputStream);
    }

    @InputFiles
    public List<File> getStreamInputs() {
        List<File> inputs = Lists.newArrayList();
        for (Stream s : inputStreams) {
            inputs.addAll(s.getCodeInputs());
            inputs.addAll(s.getResInputs());
        }

        return inputs;
    }

    @OutputDirectories
    public List<File> getStreamOutputs() {
        if (outputStream != null) {
            assert outputStream.getCodeOutput() != null;
            assert outputStream.getResOutput() != null;
            return ImmutableList.<File>builder()
                    .add(outputStream.getCodeOutput())
                    .add(outputStream.getResOutput())
                    .build();
        }

        List<File> outputs = Lists.newArrayList();
        for (Stream s : inputStreams) {
            assert s.getCodeOutput() != null;
            assert s.getResOutput() != null;
            outputs.add(s.getCodeOutput());
            outputs.add(s.getResOutput());
        }

        return outputs;
    }

    public static class ConfigAction implements TaskConfigAction<TransformTask> {

        @NonNull
        private final VariantScope scope;
        @NonNull
        private final Transform transform;
        @NonNull
        private List<Stream> inputStreams;
        @Nullable
        private Stream outputStream;

        ConfigAction(
                @NonNull VariantScope scope,
                @NonNull Transform transform,
                @NonNull List<Stream> inputStreams,
                @Nullable Stream outputStream) {
            this.scope = scope;
            this.transform = transform;
            this.inputStreams = inputStreams;
            this.outputStream = outputStream;
        }

        @Override
        public String getName() {
            return scope.getTaskName("transformWith" + StringHelper.capitalize(transform.getName()));
        }

        @Override
        public Class<TransformTask> getType() {
            return TransformTask.class;
        }

        @Override
        public void execute(TransformTask task) {
            task.transform = transform;
            task.inputStreams = inputStreams;
            task.outputStream = outputStream;
        }
    }
}
