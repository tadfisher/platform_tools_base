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
import java.util.Collections;
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

    private static final boolean DEBUG = false;

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
            throw new RuntimeException(String.format(
                    "Unable to add Transform '%s': requested streams not available.",
                    transform.getName()));
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
        List<StreamDeclaration> referencedStreams = grabReferencedStreams(transform);

        if (DEBUG) {
            System.out.println(
                    "ADDED TRANSFORM(" + variantScope.getVariantConfiguration().getFullName()
                            + "):");
            System.out.println("\tName: " + transform.getName());
            System.out.println("\tTask: " + taskName);
            for (StreamDeclaration sd : inputStreams) {
                System.out.println("\tInputStream: " + sd);
            }
            for (StreamDeclaration sd : referencedStreams) {
                System.out.println("\tRef'edStream: " + sd);
            }
            for (StreamDeclaration sd : outputStreams) {
                System.out.println("\tOutputStream: " + sd);
            }
        }

        AndroidTask<TransformTask> task = taskRegistry.create(
                taskFactory,
                new TransformTask.ConfigAction(
                        variantScope.getVariantConfiguration().getFullName(),
                        taskName,
                        transform,
                        inputStreams,
                        referencedStreams,
                        outputStreams));

        for (StreamDeclaration s : inputStreams) {
            task.dependsOn(taskFactory, s.getDependencies());
        }
        for (StreamDeclaration s : referencedStreams) {
            task.dependsOn(taskFactory, s.getDependencies());
        }

        return task;
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
            if (streamTypes.containsAll(s.getTypes())) {
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
            if (s.getTypes().equals(EnumSet.of(streamType)) &&
                    allowedScopes.containsAll(s.getScopes())) {
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
                                    combineTypeNames(input.getTypes()),
                                    combineScopeNames(input.getScopes()),
                                    transform.getName(),
                                    variantDirName)))
                            .setDependency(taskName).build());
                }

                return outputStreams;
            case COMBINED:
            case COMBINED_AS_FILE:
                // create single combined output stream for all types and scopes
                Set<StreamType> types = transform.getOutputTypes();
                Set<StreamScope> scopes = transform.getScopes();

                File folder = new File(buildDir, Joiner.on(File.separator).join(
                        AndroidProject.FD_INTERMEDIATES,
                        "transforms",
                        combineTypeNames(types),
                        combineScopeNames(scopes),
                        transform.getName(),
                        variantDirName));

                StreamDeclarationImpl.Builder builder = StreamDeclarationImpl.builder()
                        .addTypes(types)
                        .addScopes(scopes)
                        .setDependency(taskName);
                if (transform.getTransformType() == TransformType.COMBINED_AS_FILE) {
                    builder.setFolder(false).setFiles(new File(folder, "classes.jar"));
                } else {
                    builder.setFiles(folder);
                }

                return Collections.singletonList(builder.build());
            case NO_OP:
                // put the input streams back into the pipeline.
                return inputStreams;
            default:
                throw new UnsupportedOperationException("Unsupported transform type");
        }
    }

    private static String combineTypeNames(Set<StreamType> types) {
        return Joiner.on("_and_").join(types);
    }

    private static String combineScopeNames(Set<StreamScope> scopes) {
        return Joiner.on("_and_").join(scopes);
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

        Set<StreamType> requestedTypes = transform.getInputTypes();
        Set<StreamScope> requestedScopes = transform.getScopes();
        for (int i = 0 ; i < streams.size();) {
            StreamDeclaration stream = streams.get(i);

            if (requestedScopes.containsAll(stream.getScopes()) &&
                    requestedTypes.containsAll(stream.getTypes())) {
                streamMatches.add(stream);

                streams.remove(i);
            } else {
                i++;
            }
        }

        return streamMatches;
    }

    @NonNull
    private List<StreamDeclaration> grabReferencedStreams(@NonNull Transform transform) {
        List<StreamDeclaration> streamMatches = Lists.newArrayListWithExpectedSize(streams.size());

        Set<StreamType> requestedTypes = transform.getInputTypes();
        Set<StreamScope> requestedScopes = transform.getReferencedScope();
        for (StreamDeclaration stream : streams) {
            if (requestedScopes.containsAll(stream.getScopes()) &&
                    requestedTypes.containsAll(stream.getTypes())) {
                streamMatches.add(stream);
            }
        }

        return streamMatches;
    }
}
