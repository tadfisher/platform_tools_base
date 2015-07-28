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
import com.android.build.gradle.internal.scope.TaskConfigAction;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A task doing a bytecode transformation
 */
public class TransformTask extends StreamBasedTask {

    private Transform transform;

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
        private final Transform transform;

        @NonNull
        private final String name;

        @NonNull
        private List<Stream> inputStreams;
        @NonNull
        private List<Stream> outputStreams;

        ConfigAction(
                @NonNull String name,
                @NonNull Transform transform,
                @NonNull List<Stream> inputStreams,
                @NonNull List<Stream> outputStreams) {
            this.name = name;
            this.transform = transform;
            this.inputStreams = inputStreams;
            this.outputStreams = outputStreams;
        }

        @NonNull
        @Override
        public String getName() {
            return name;
        }

        @NonNull
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
