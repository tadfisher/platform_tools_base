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
import com.android.build.gradle.internal.pipeline.impl.StreamDeclarationImpl;
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
import java.util.EnumSet;
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
    private final List<StreamDeclaration> streams = Lists.newArrayList();

    private final List<Transform> transforms = Lists.newArrayList();

    public TransformPipeline(@NonNull AndroidTaskRegistry taskRegistry) {
        this.taskRegistry = taskRegistry;
    }

    @NonNull
    public AndroidTaskRegistry getTaskRegistry() {
        return taskRegistry;
    }

    public void addStream(@NonNull StreamDeclaration stream) {
        streams.add(stream);
    }

    public void addStreams(@NonNull StreamDeclaration... streams) {
        this.streams.addAll(Arrays.asList(streams));
    }

    public void addStreams(@NonNull Collection<StreamDeclaration> streams) {
        this.streams.addAll(streams);
    }

    public AndroidTask<?> addTransform(
            @NonNull TaskFactory taskFactory,
            @NonNull VariantScope variantScope,
            @NonNull Transform transform) {
        List<StreamDeclaration> inputStreams = grabStreams(transform);
        if (inputStreams.isEmpty()) {
            // didn't find any match. Means there is a broken order somewhere in the streams.
        }

        String taskName = variantScope.getTaskName(getTaskNamePrefix(transform));

        // create new Stream to match the output of the transform.
        List<StreamDeclaration> outputStreams = computeOutputStreams(
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
                        variantScope.getVariantConfiguration().getFullName(),
                        taskName,
                        transform,
                        inputStreams,
                        outputStreams));
    }

    @NonNull
    public List<StreamDeclaration> getStreams() {
        return streams;
    }

    public ImmutableList<StreamDeclaration> getStreamsByTypes(@NonNull StreamType streamType) {
        return getStreamsByTypes(EnumSet.of(streamType));
    }

    public ImmutableList<StreamDeclaration> getStreamsByTypes(@NonNull StreamType type1,
            @NonNull StreamType... otherTypes) {
        return getStreamsByTypes(EnumSet.of(type1, otherTypes));
    }

    public ImmutableList<StreamDeclaration> getStreamsByTypes(@NonNull Set<StreamType> streamTypes) {
        ImmutableList.Builder<StreamDeclaration> streamsByType = ImmutableList.builder();
        for (StreamDeclaration s : streams) {
            if (streamTypes.contains(s.getType())) {
                streamsByType.add(s);
            }
        }

        return streamsByType.build();
    }

    public List<StreamDeclaration> getStreamsByTypeAndScope(
            @NonNull StreamType streamType,
            @NonNull Set<StreamScope> allowedScopes) {
        ImmutableList.Builder<StreamDeclaration> streamsByType = ImmutableList.builder();
        for (StreamDeclaration s : streams) {
            if (s.getType() == streamType && allowedScopes.contains(s.getScope())) {
                streamsByType.add(s);
            }
        }

        return streamsByType.build();
    }

    @NonNull
    private static String getTaskNamePrefix(@NonNull Transform transform) {
        StringBuilder sb = new StringBuilder(100);
        sb.append("transform");

        Iterator<StreamType> iterator = transform.getInputTypes().iterator();
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
    private static List<StreamDeclaration> computeOutputStreams(
            @NonNull Transform transform,
            @NonNull List<StreamDeclaration> inputStreams,
            @NonNull String taskName,
            @NonNull String variantDirName,
            @NonNull File buildDir) {
        List<StreamDeclaration> outputStreams;
        switch (transform.getTransformType()) {
            case AS_INPUT:
                // for each input, create a matching output.
                outputStreams = Lists.newArrayListWithCapacity(inputStreams.size());
                for (StreamDeclaration input : inputStreams) {
                    // copy with new location.
                    outputStreams.add(StreamDeclarationImpl.builder()
                            .from(input)
                            .setFiles(new File(buildDir, Joiner.on(File.separator).join(
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
                Set<StreamType> types = transform.getOutputTypes();
                outputStreams = Lists.newArrayListWithCapacity(types.size());
                if (types.contains(StreamType.CLASSES)) {
                    // create a ALL/CLASSES Stream
                    outputStreams.add(StreamDeclarationImpl.builder()
                            .setType(StreamType.CLASSES)
                            .setScope(StreamScope.ALL)
                            .setFiles(new File(buildDir, Joiner.on(File.separator).join(
                                    AndroidProject.FD_INTERMEDIATES,
                                    "transforms",
                                    StreamType.CLASSES.name().toLowerCase(Locale.getDefault()),
                                    StreamScope.ALL.name().toLowerCase(Locale.getDefault()),
                                    transform.getName(),
                                    variantDirName)))
                            .setDependency(taskName).build());
                }
                if (types.contains(StreamType.RESOURCES)) {
                    // create a ALL/RESOURCES Stream
                    outputStreams.add(StreamDeclarationImpl.builder()
                            .setType(StreamType.RESOURCES)
                            .setScope(StreamScope.ALL)
                            .setFiles(new File(buildDir, Joiner.on(File.separator).join(
                                    AndroidProject.FD_INTERMEDIATES,
                                    "transforms",
                                    StreamType.RESOURCES.name().toLowerCase(Locale.getDefault()),
                                    StreamScope.ALL.name().toLowerCase(Locale.getDefault()),
                                    transform.getName(),
                                    variantDirName)))
                            .setDependency(taskName).build());
                }
                if (types.contains(StreamType.DEX)) {
                    // create a ALL/DEX Stream
                    outputStreams.add(StreamDeclarationImpl.builder()
                            .setType(StreamType.DEX)
                            .setScope(StreamScope.ALL)
                            .setFiles(new File(buildDir, Joiner.on(File.separator).join(
                                    AndroidProject.FD_INTERMEDIATES,
                                    "transforms",
                                    StreamType.DEX.name().toLowerCase(Locale.getDefault()),
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
    private List<StreamDeclaration> grabStreams(@NonNull Transform transform) {
        List<StreamDeclaration> streamMatches = Lists.newArrayListWithExpectedSize(streams.size());

        Set<StreamType> types = transform.getInputTypes();
        StreamScope scope = transform.getScope();
        for (int i = 0 ; i < streams.size();) {
            StreamDeclaration s = streams.get(i);
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
