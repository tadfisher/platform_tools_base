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
import com.android.build.gradle.internal.TaskFactory;
import com.android.build.gradle.internal.scope.AndroidTask;
import com.android.build.gradle.internal.scope.AndroidTaskRegistry;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.utils.StringHelper;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A pipeline of bytecode transforms
 */
public class TransformPipeline {

    @NonNull
    private final AndroidTaskRegistry taskRegistry;
    @NonNull
    private final TaskFactory taskFactory;
    @NonNull
    private final VariantScope variantScope;
    @NonNull
    private final List<Stream> streams;

    private final List<Transform> transforms = Lists.newArrayList();

    public static final class Builder {

        private AndroidTaskRegistry taskRegistry;
        private TaskFactory taskFactory;
        private VariantScope variantScope;
        private final List<Stream> streams = Lists.newArrayList();

        public Builder() {
        }

        public TransformPipeline build() {
            return new TransformPipeline(
                    taskRegistry,
                    taskFactory,
                    variantScope,
                    streams);
        }

        public Builder setTaskRegistry(
                @NonNull AndroidTaskRegistry taskRegistry) {
            this.taskRegistry = taskRegistry;
            return this;
        }

        public Builder setTaskFactory(@NonNull TaskFactory taskFactory) {
            this.taskFactory = taskFactory;
            return this;
        }

        public Builder setVariantScope(@NonNull VariantScope variantScope) {
            this.variantScope = variantScope;
            return this;
        }

        public Builder addStream(@NonNull Stream stream) {
            streams.add(stream);
            return this;
        }

        public Builder addStreams(@NonNull Stream... streams) {
            this.streams.addAll(Arrays.asList(streams));
            return this;
        }

        public Builder addStreams(@NonNull Collection<Stream> streams) {
            this.streams.addAll(streams);
            return this;
        }
    }

    private TransformPipeline(
            @NonNull AndroidTaskRegistry taskRegistry,
            @NonNull TaskFactory taskFactory,
            @NonNull VariantScope variantScope,
            @NonNull List<Stream> streams) {
        this.taskRegistry = taskRegistry;
        this.taskFactory = taskFactory;
        this.variantScope = variantScope;
        this.streams = Lists.newArrayList(streams);
    }

    public void addTransform(@NonNull Transform transform) {
        List<Stream> inputStreams = findStreams(transform);
        if (inputStreams.isEmpty()) {
            // didn't find any match. Means there is a misorder somewhere in the streams.
        }

        String taskName = variantScope.getTaskName("transformWith" + StringHelper
                .capitalize(transform.getName()));

        // create new Stream to match the output of the transform.
        List<Stream> outputStreams = computeOutputStreams(transform, inputStreams, taskName);
        streams.addAll(outputStreams);

        // TODO: we probably need a map from transform to tasks
        transforms.add(transform);

        // create the task...
        // Need to figure out the stream based on the previous transforms... Should probably be dynamic.
        AndroidTask<TransformTask> task = taskRegistry.create(
                taskFactory,
                new TransformTask.ConfigAction(
                        taskName, variantScope, transform, inputStreams, outputStreams));
    }

    @NonNull
    private static List<Stream> computeOutputStreams(
            @NonNull Transform transform,
            @NonNull List<Stream> inputStreams,
            @NonNull String taskName) {
        if (transform.getTransformType() == TransformType.AS_INPUT) {
            // for each input, create a matching output.
            List<Stream> outputStreams = Lists.newArrayListWithCapacity(inputStreams.size());
            for (Stream input : inputStreams) {
                // copy with new location.
                outputStreams.add(new StreamImpl(input.getType(), input.getScope(), taskName));
            }

            return outputStreams;

        } else {
            // create single combined output stream for each code, res.
            Set<StreamType> types = transform.getTypes();
            List<Stream> outputStreams = Lists.newArrayListWithCapacity(types.size());
            if (types.contains(StreamType.CODE)) {
                // create a ALL/CODE Stream
                outputStreams.add(new StreamImpl(StreamType.CODE, StreamScope.ALL, taskName));
            }
            if (types.contains(StreamType.CODE)) {
                // create a ALL/RESOURCES Stream
                outputStreams.add(
                        new StreamImpl(StreamType.RESOURCES, StreamScope.ALL, taskName));
            }

            return outputStreams;
        }
    }

    /**
     * Finds the stream the transform consumes, and return them.
     *
     * This also removes them from the instance list. They will be replaced with the output
     * stream(s) from the transform.
     *
     * @param transform the transform.
     * @return the input streams for the transform.
     */
    @NonNull
    private List<Stream> findStreams(@NonNull Transform transform) {
        List<Stream> streamMatches = Lists.newArrayListWithExpectedSize(streams.size());

        Set<StreamType> types = transform.getTypes();
        StreamScope scope = transform.getScope();
        for (int i = 0 ; i < streams.size();) {
            Stream s = streams.get(i);
            if (types.contains(s.getType()) && s.getScope().match(scope)) {
                streamMatches.add(s);
                streams.remove(i);
            } else {
                i++;
            }
        }

        return streamMatches;
    }
}
