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
import com.google.common.collect.Lists;

import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectories;
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.BuildException;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * A task doing a bytecode transformation
 */
public class TransformTask extends DefaultAndroidTask {

    private Transform transform;
    private List<Stream> inputStreams;
    private List<Stream> outputStreams;

    TransformTask() {
    }

    public Transform getTransform() {
        return transform;
    }

    @TaskAction
    void transform() {
        // TODO: handle incremental mode
        transform.transform(inputStreams, outputStreams);
    }

    @InputFiles
    public List<File> getStreamInputs() {
        List<File> inputs = Lists.newArrayList();
        for (Stream s : inputStreams) {
            Object object = s.getInputs();

            if (object instanceof Callable) {
                try {
                    object = ((Callable) object).call();
                } catch (Exception e) {
                    throw new BuildException("", e);
                }
            }

            if (object instanceof File) {
                inputs.add((File) object);
            } else if (object instanceof Collection) {
                inputs.addAll((Collection<File>) object);
            }
        }

        return inputs;
    }

    @OutputDirectories
    public List<File> getStreamOutputs() {
        List<File> inputs = Lists.newArrayList();
        for (Stream s : outputStreams) {
            Object object = s.getInputs();

            if (object instanceof Callable) {
                try {
                    object = ((Callable) object).call();
                } catch (Exception e) {
                    throw new BuildException("", e);
                }
            }

            if (object instanceof File) {
                inputs.add((File) object);
            } else if (object instanceof Collection) {
                inputs.addAll((Collection<File>) object);
            }
        }

        return inputs;
    }

    public static class ConfigAction implements TaskConfigAction<TransformTask> {

        @NonNull
        private final Transform transform;

        @NonNull
        private final String name;

        @NonNull
        private List<Stream> inputStreams;
        @Nullable
        private List<Stream> outputStreams;

        ConfigAction(
                @NonNull String name,
                @NonNull VariantScope scope,
                @NonNull Transform transform,
                @NonNull List<Stream> inputStreams,
                @NonNull List<Stream> outputStreams) {
            this.name = name;
            this.transform = transform;
            this.inputStreams = inputStreams;
            this.outputStreams = outputStreams;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class<TransformTask> getType() {
            return TransformTask.class;
        }

        @Override
        public void execute(TransformTask task) {
            task.transform = transform;
            task.inputStreams = inputStreams;
            task.outputStreams = outputStreams;
        }
    }
}
