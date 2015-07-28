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

import static com.android.utils.StringHelper.capitalize;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.TaskFactory;
import com.android.build.gradle.internal.scope.AndroidTask;
import com.android.build.gradle.internal.scope.AndroidTaskRegistry;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.builder.model.AndroidProject;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A pipeline of transforms for bytecode and resources.
 *
 * This is not a real pipeline as the actual execution is handled by Gradle through the tasks.
 * Instead it's a mean to more easily configure a series of transforms that consume each other's
 * inputs when several of these transform are optional.
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

    public static Builder builder() {
        return new Builder();
    }
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

    @NonNull
    public AndroidTaskRegistry getTaskRegistry() {
        return taskRegistry;
    }

    @NonNull
    public TaskFactory getTaskFactory() {
        return taskFactory;
    }

    @NonNull
    public VariantScope getVariantScope() {
        return variantScope;
    }

    public AndroidTask<?> addTransform(@NonNull Transform transform) {
        List<Stream> inputStreams = grabStreams(transform);
        if (inputStreams.isEmpty()) {
            // didn't find any match. Means there is a broken order somewhere in the streams.
        }

        String taskName = variantScope.getTaskName(getTaskNamePrefix(transform));

        // create new Stream to match the output of the transform.
        List<Stream> outputStreams = computeOutputStreams(
                transform, inputStreams, taskName,
                variantScope.getVariantConfiguration().getDirName(),
                variantScope.getGlobalScope().getBuildDir());
        streams.addAll(outputStreams);

        // TODO: we probably need a map from transform to tasks
        transforms.add(transform);

        // create the task...
        // Need to figure out the stream based on the previous transforms... Should probably be dynamic.

        return taskRegistry.create(
                taskFactory,
                new TransformTask.ConfigAction(
                        taskName, transform, inputStreams, outputStreams));
    }

    @NonNull
    public List<Stream> getStreams() {
        return streams;
    }

    public List<Stream> getStreamsByType(@NonNull StreamType streamType) {
        ImmutableList.Builder<Stream> streamsByType = ImmutableList.builder();
        for (Stream s : streams) {
            if (s.getType() == streamType) {
                streamsByType.add(s);
            }
        }

        return streamsByType.build();
    }

    @NonNull
    private static String getTaskNamePrefix(@NonNull Transform transform) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("transform");

        Iterator<StreamType> iterator = transform.getTypes().iterator();
        // there's always at least one
        sb.append(capitalize(iterator.next().name().toLowerCase(Locale.getDefault())));
        while (iterator.hasNext()) {
            sb.append("And").append(capitalize(
                    iterator.next().name().toLowerCase(Locale.getDefault())));
        }

        sb.append("With").append(capitalize(transform.getName()));

        return sb.toString();
    }

    @NonNull
    private static List<Stream> computeOutputStreams(
            @NonNull Transform transform,
            @NonNull List<Stream> inputStreams,
            @NonNull String taskName,
            @NonNull String variantDirName,
            @NonNull File buildDir) {
        List<Stream> outputStreams;
        switch (transform.getTransformType()) {
            case AS_INPUT:
                // for each input, create a matching output.
                outputStreams = Lists.newArrayListWithCapacity(inputStreams.size());
                for (Stream input : inputStreams) {
                    // copy with new location.
                    outputStreams.add(StreamImpl.builder()
                            .from(input)
                            .setInputs(new File(buildDir, Joiner.on(File.separator).join(
                                    AndroidProject.FD_INTERMEDIATES,
                                    "transforms",
                                    input.getType().name().toLowerCase(Locale.getDefault()),
                                    input.getScope().name().toLowerCase(Locale.getDefault()),
                                    transform.getName(),
                                    variantDirName)))
                            .setDependency(taskName).build());
                }

                return outputStreams;
            case COMBINED:
                // create single combined output stream for each code, res.
                Set<StreamType> types = transform.getTypes();
                outputStreams = Lists.newArrayListWithCapacity(types.size());
                if (types.contains(StreamType.CODE)) {
                    // create a ALL/CODE Stream
                    outputStreams.add(StreamImpl.builder()
                            .setType(StreamType.CODE)
                            .setScope(StreamScope.ALL)
                            .setInputs(new File(buildDir, Joiner.on(File.separator).join(
                                    AndroidProject.FD_INTERMEDIATES,
                                    "transforms",
                                    StreamType.CODE.name().toLowerCase(Locale.getDefault()),
                                    StreamScope.ALL.name().toLowerCase(Locale.getDefault()),
                                    transform.getName(),
                                    variantDirName)))
                            .setDependency(taskName).build());
                }
                if (types.contains(StreamType.CODE)) {
                    // create a ALL/RESOURCES Stream
                    outputStreams.add(StreamImpl.builder()
                            .setType(StreamType.RESOURCES)
                            .setScope(StreamScope.ALL)
                            .setInputs(new File(buildDir, Joiner.on(File.separator).join(
                                    AndroidProject.FD_INTERMEDIATES,
                                    "transforms",
                                    StreamType.RESOURCES.name().toLowerCase(Locale.getDefault()),
                                    StreamScope.ALL.name().toLowerCase(Locale.getDefault()),
                                    transform.getName(),
                                    variantDirName)))
                            .setDependency(taskName).build());
                }

                return outputStreams;
            case NO_OP:
                // put the input streams back into the pipeline.
                return Lists.newArrayList(inputStreams);
            default:
                throw new UnsupportedOperationException("Unsupported transform type");
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
    private List<Stream> grabStreams(@NonNull Transform transform) {
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
