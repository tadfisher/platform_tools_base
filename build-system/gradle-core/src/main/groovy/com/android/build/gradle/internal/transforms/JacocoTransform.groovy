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

package com.android.build.gradle.internal.transforms

import com.android.annotations.NonNull
import com.android.build.gradle.internal.coverage.JacocoPlugin
import com.android.build.gradle.internal.pipeline.InputStream
import com.android.build.gradle.internal.pipeline.OutputStream
import com.android.build.gradle.internal.pipeline.StreamScope
import com.android.build.gradle.internal.pipeline.StreamType
import com.android.build.gradle.internal.pipeline.Transform
import com.android.build.gradle.internal.pipeline.TransformException
import com.android.build.gradle.internal.pipeline.TransformType
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.scope.VariantScopeImpl
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.google.common.util.concurrent.Callables
import org.gradle.util.GUtil

import java.util.concurrent.Callable

/**
 * Jacoco Transform
 */
public class JacocoTransform implements Transform {

    @NonNull
    private final Callable<Collection<File>> jacocoClasspath
    @NonNull
    private final VariantScopeImpl scope

    public JacocoTransform(@NonNull VariantScope scope) {
        this.scope = scope
        this.jacocoClasspath = Callables.<Collection<File>>returning(
                scope.getGlobalScope().getProject().getConfigurations()
                        .getByName(JacocoPlugin.ANT_CONFIGURATION_NAME).getFiles())
    }

    @NonNull
    @Override
    public String getName() {
        return "jacoco"
    }

    @NonNull
    @Override
    public Set<StreamType> getInputTypes() {
        return Sets.immutableEnumSet(StreamType.CLASSES)
    }

    @NonNull
    @Override
    public Set<StreamType> getOutputTypes() {
        return Sets.immutableEnumSet(StreamType.CLASSES)
    }

    @NonNull
    @Override
    public Set<StreamScope> getScopes() {
        // only run on the project classes
        return Sets.immutableEnumSet(StreamScope.PROJECT)
    }

    @Override
    public Set<StreamScope> getReferencedScope() {
        return ImmutableSet.copyOf(EnumSet.noneOf(StreamScope.class))
    }

    @NonNull
    @Override
    public TransformType getTransformType() {
        // does not combine multiple input stream.
        return TransformType.AS_INPUT
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileInputs() {
        try {
            return jacocoClasspath.call()
        } catch (Exception e) {
            return Collections.emptyList()
        }
    }

    @Override
    Collection<File> getSecondaryFileOutputs() {
        return Collections.emptyList();
    }

    @NonNull
    @Override
    public Map<String, Object> getParameterInputs() {
        return Collections.emptyMap()
    }

    @Override
    boolean isIncremental() {
        return false
    }

    public void transform(
            @NonNull List<InputStream> inputs,
            @NonNull List<OutputStream> outputs,
            boolean isIncremental) throws TransformException {
        AntBuilder antBuilder = scope.globalScope.project.ant

        assert outputs.size() == 1
        File outputDir = outputs.get(0).file
        outputDir.deleteDir()
        outputDir.mkdirs()

        assert inputs.size() == 1

        antBuilder.taskdef(name: 'instrumentWithJacoco',
                classname: 'org.jacoco.ant.InstrumentTask',
                classpath: GUtil.asPath(jacocoClasspath.call()))
        antBuilder.instrumentWithJacoco(destdir: outputDir) {
            fileset(dir: getInputDir(inputs))
        }
    }

    protected static File getInputDir(List<InputStream> inputs) {
        return inputs.get(0).files.first()
    }
}
