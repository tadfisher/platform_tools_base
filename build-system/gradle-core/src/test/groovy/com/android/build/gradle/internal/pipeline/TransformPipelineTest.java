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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.TaskFactory;
import com.android.build.gradle.internal.core.GradleVariantConfiguration;
import com.android.build.gradle.internal.pipeline.impl.StreamDeclarationImpl;
import com.android.build.gradle.internal.scope.AndroidTask;
import com.android.build.gradle.internal.scope.AndroidTaskRegistry;
import com.android.build.gradle.internal.scope.GlobalScope;
import com.android.build.gradle.internal.scope.VariantScope;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TransformPipelineTest {

    @Mock
    private TaskFactory taskFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void simpleTransform() {
        VariantScope variantScope = getVariantScope();

        TransformPipeline transformPipeline = new TransformPipeline(new AndroidTaskRegistry());

        // create a stream and add it to the pipeline
        StreamDeclaration projectClass = StreamDeclarationImpl.builder()
                .addType(StreamType.CLASSES)
                .addScope(StreamScope.PROJECT)
                .setFiles(new File("my file"))
                .setDependency("my dependency")
                .build();
        transformPipeline.addStream(projectClass);

        // add a new transform

        Transform t = transformBuilder()
                .setInputTypes(StreamType.CLASSES)
                .setScopes(StreamScope.PROJECT)
                .setTransformType(TransformType.AS_INPUT)
                .build();

        // add the transform
        AndroidTask<TransformTask> task = transformPipeline.addTransform(
                taskFactory, variantScope, t);

        // get the new stream
        List<StreamDeclaration> streams = transformPipeline.getStreams();
        assertEquals(1, streams.size());

        ImmutableList<StreamDeclaration> classStreams = transformPipeline
                .getStreamsByTypes(StreamType.CLASSES);
        assertEquals(1, classStreams.size());
        StreamDeclaration classStream = Iterables.getOnlyElement(classStreams);
        assertEquals(EnumSet.of(StreamType.CLASSES), classStream.getTypes());
        assertEquals(Collections.singletonList(variantScope.getTaskName("")), classStream.getDependencies());

        // check the stream was consumed.
        assertFalse(streams.contains(projectClass));

        // check the task contains the stream
        // TODO?
    }

    @Test
    public void referencedScope() {
        VariantScope variantScope = getVariantScope();

        TransformPipeline transformPipeline = new TransformPipeline(new AndroidTaskRegistry());

        // create streams and add them to the pipeline
        StreamDeclaration projectClass = StreamDeclarationImpl.builder()
                .addType(StreamType.CLASSES)
                .addScope(StreamScope.PROJECT)
                .setFiles(new File("my file"))
                .setDependency("my dependency")
                .build();
        transformPipeline.addStream(projectClass);

        StreamDeclaration libClasses = StreamDeclarationImpl.builder()
                .addType(StreamType.CLASSES)
                .addScope(StreamScope.EXTERNAL_LIBRARIES)
                .setFiles(new File("my file"))
                .setDependency("my dependency")
                .build();
        transformPipeline.addStream(libClasses);


        // add a new transform
        Transform t = transformBuilder()
                .setInputTypes(StreamType.CLASSES)
                .setScopes(StreamScope.PROJECT)
                .setReferencedScopes(StreamScope.EXTERNAL_LIBRARIES)
                .setTransformType(TransformType.AS_INPUT)
                .build();

        // add the transform
        AndroidTask<TransformTask> task = transformPipeline.addTransform(
                taskFactory, variantScope, t);

        // get the new stream
        List<StreamDeclaration> streams = transformPipeline.getStreams();
        assertEquals(2, streams.size());

        // check the class stream was consumed.
        assertFalse(streams.contains(projectClass));
        // check the referenced stream is still present
        assertTrue(streams.contains(libClasses));

        // check the task contains the stream
        // TODO?
    }

    @Test
    public void splitStream() throws Exception {
        VariantScope variantScope = getVariantScope();

        TransformPipeline transformPipeline = new TransformPipeline(new AndroidTaskRegistry());

        // create streams and add them to the pipeline
        StreamDeclaration projectClassAndResources = StreamDeclarationImpl.builder()
                .addTypes(StreamType.CLASSES, StreamType.RESOURCES)
                .addScope(StreamScope.PROJECT)
                .setFiles(new File("my file"))
                .setDependency("my dependency")
                .build();
        transformPipeline.addStream(projectClassAndResources);


        // add a new transform
        Transform t = transformBuilder()
                .setInputTypes(StreamType.CLASSES)
                .setScopes(StreamScope.PROJECT)
                .setTransformType(TransformType.AS_INPUT)
                .build();

        // add the transform
        AndroidTask<TransformTask> task = transformPipeline.addTransform(
                taskFactory, variantScope, t);

        // get the new stream
        List<StreamDeclaration> streams = transformPipeline.getStreams();
        assertEquals(2, streams.size());

        // check the class stream was consumed.
        assertFalse(streams.contains(projectClassAndResources));

        // check we now have 2 streams, one for classes and one for resources.
        // the one for resources should match projectClassAndResources for location and dependency.
        ImmutableList<StreamDeclaration> classStreams = transformPipeline
                .getStreamsByTypes(StreamType.CLASSES);
        assertEquals(1, classStreams.size());
        StreamDeclaration classStream = Iterables.getOnlyElement(classStreams);
        assertEquals(EnumSet.of(StreamType.CLASSES), classStream.getTypes());

        ImmutableList<StreamDeclaration> resStreams = transformPipeline
                .getStreamsByTypes(StreamType.RESOURCES);
        assertEquals(1, resStreams.size());
        // check content
        StreamDeclaration resStream = Iterables.getOnlyElement(resStreams);
        assertEquals(EnumSet.of(StreamType.RESOURCES), resStream.getTypes());
        assertEquals(projectClassAndResources.getDependencies(), resStream.getDependencies());
        assertEquals(projectClassAndResources.getFiles().call(), resStream.getFiles().call());

        // check the task contains the stream
        // TODO?
    }

    @Test
    public void combinedScopes() throws Exception {
        VariantScope variantScope = getVariantScope();

        TransformPipeline transformPipeline = new TransformPipeline(new AndroidTaskRegistry());

        // create streams and add them to the pipeline
        StreamDeclaration projectClass = StreamDeclarationImpl.builder()
                .addType(StreamType.CLASSES)
                .addScope(StreamScope.PROJECT)
                .setFiles(new File("my file"))
                .setDependency("my dependency")
                .build();
        transformPipeline.addStream(projectClass);

        StreamDeclaration libClasses = StreamDeclarationImpl.builder()
                .addType(StreamType.CLASSES)
                .addScope(StreamScope.EXTERNAL_LIBRARIES)
                .setFiles(new File("my file"))
                .setDependency("my dependency")
                .build();
        transformPipeline.addStream(libClasses);


        // add a new transform
        Transform t = transformBuilder()
                .setInputTypes(StreamType.CLASSES)
                .setScopes(StreamScope.PROJECT, StreamScope.EXTERNAL_LIBRARIES)
                .setTransformType(TransformType.COMBINED)
                .build();

        // add the transform
        AndroidTask<TransformTask> task = transformPipeline.addTransform(
                taskFactory, variantScope, t);

        // get the new stream
        List<StreamDeclaration> streams = transformPipeline.getStreams();
        assertEquals(1, streams.size());

        // check the class stream was consumed.
        assertFalse(streams.contains(projectClass));
        assertFalse(streams.contains(libClasses));

        // check we now have 1 streams, containing both scopes.
        ImmutableList<StreamDeclaration> classStreams = transformPipeline
                .getStreamsByTypes(StreamType.CLASSES);
        assertEquals(1, classStreams.size());
        StreamDeclaration classStream = Iterables.getOnlyElement(classStreams);
        assertEquals(EnumSet.of(StreamType.CLASSES), classStream.getTypes());
        assertEquals(EnumSet.of(StreamScope.PROJECT, StreamScope.EXTERNAL_LIBRARIES), classStream.getScopes());

        // check the task contains the stream
        // TODO?
    }

    @NonNull
    private static VariantScope getVariantScope() {
        GradleVariantConfiguration mockConfig = mock(GradleVariantConfiguration.class);
        when(mockConfig.getDirName()).thenReturn("config dir name");

        GlobalScope globalScope = mock(GlobalScope.class);
        when(globalScope.getBuildDir()).thenReturn(new File("build dir"));

        VariantScope variantScope = mock(VariantScope.class);
        when(variantScope.getVariantConfiguration()).thenReturn(mockConfig);
        when(variantScope.getGlobalScope()).thenReturn(globalScope);
        when(variantScope.getTaskName(Mockito.anyString())).thenReturn("task name");
        return variantScope;
    }

    private static Builder transformBuilder() {
        return new Builder();
    }

    private static final class Builder {
        private String name;
        private final Set<StreamType> inputTypes = EnumSet.noneOf(StreamType.class);
        private Set<StreamType> outputTypes;
        private final Set<StreamScope> scopes = EnumSet.noneOf(StreamScope.class);
        private final Set<StreamScope> refedScopes = EnumSet.noneOf(StreamScope.class);
        private TransformType transformType;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setInputTypes(@NonNull StreamType... types) {
            inputTypes.addAll(Arrays.asList(types));
            return this;
        }

        public Builder setOutputTypes(@NonNull StreamType... types) {
            if (outputTypes == null) {
                outputTypes = EnumSet.noneOf(StreamType.class);
            }
            outputTypes.addAll(Arrays.asList(types));
            return this;
        }

        public Builder setScopes(@NonNull StreamScope... scopes) {
            this.scopes.addAll(Arrays.asList(scopes));
            return this;
        }

        public Builder setReferencedScopes(@NonNull StreamScope... scopes) {
            this.refedScopes.addAll(Arrays.asList(scopes));
            return this;
        }

        public Builder setTransformType(
                TransformType transformType) {
            this.transformType = transformType;
            return this;
        }

        @NonNull
        Transform build() {
            final String name = this.name != null ? this.name : "transform name";
            Assert.assertFalse(this.inputTypes.isEmpty());
            final Set<StreamType> inputTypes = Sets.immutableEnumSet(this.inputTypes);
            final Set<StreamType> outputTypes = this.outputTypes != null ? Sets.immutableEnumSet(this.outputTypes) : inputTypes;
            final Set<StreamScope> scopes = Sets.immutableEnumSet(this.scopes);
            final Set<StreamScope> refedScopes = Sets.immutableEnumSet(this.scopes);
            final TransformType transformType = this.transformType;

            return new Transform() {
                @NonNull
                @Override
                public String getName() {
                    return name;
                }

                @NonNull
                @Override
                public Set<StreamType> getInputTypes() {
                    return inputTypes;
                }

                @NonNull
                @Override
                public Set<StreamType> getOutputTypes() {
                    return outputTypes;
                }

                @NonNull
                @Override
                public Set<StreamScope> getScopes() {
                    return scopes;
                }

                @NonNull
                @Override
                public Set<StreamScope> getReferencedScopes() {
                    return refedScopes;
                }

                @NonNull
                @Override
                public TransformType getTransformType() {
                    return transformType;
                }

                @NonNull
                @Override
                public Collection<File> getSecondaryFileInputs() {
                    return Collections.emptyList();
                }

                @NonNull
                @Override
                public Collection<File> getSecondaryFileOutputs() {
                    return Collections.emptyList();
                }

                @NonNull
                @Override
                public Map<String, Object> getParameterInputs() {
                    return Collections.emptyMap();
                }

                @Override
                public boolean isIncremental() {
                    return false;
                }

                @Override
                public void transform(@NonNull List<InputStream> inputs,
                        @NonNull List<OutputStream> outputs,
                        boolean isIncremental) throws TransformException {
                }
            };
        }
    }
}