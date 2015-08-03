/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.build.gradle.tasks
import com.android.SdkConstants
import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.gradle.internal.PostCompilationData
import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.dsl.DexOptions
import com.android.build.gradle.internal.pipeline.StreamBasedTask
import com.android.build.gradle.internal.pipeline.StreamDeclaration
import com.android.build.gradle.internal.pipeline.StreamType
import com.android.build.gradle.internal.scope.ConventionMappingHelper
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.ApkVariantData
import com.android.build.gradle.internal.variant.TestVariantData
import com.android.ide.common.process.LoggedProcessOutputHandler
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean

import static com.android.builder.core.VariantType.DEFAULT
import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES

public class Dex extends StreamBasedTask {

    // ----- PUBLIC TASK API -----

    @OutputDirectory
    File outputFolder

    @Input @Optional
    List<String> additionalParameters

    boolean enableIncremental = true

    // ----- PRIVATE TASK API -----
    @Input
    String getBuildToolsVersion() {
        getBuildTools().getRevision()
    }

    protected List<StreamDeclaration> preDexStreams;

    @InputFiles
    public List<File> getPreDexStreamInputs() {
        List<File> inputs = Lists.newArrayList();
        for (StreamDeclaration s : preDexStreams) {
            try {
                inputs.addAll(s.getFiles().call());
            } catch (Exception e) {
                // ?
            }
        }

        return inputs;
    }

    @Nested
    DexOptions dexOptions

    @Input
    boolean multiDexEnabled = false

    @Input
    boolean optimize = true

    @InputFile @Optional
    File mainDexListFile

    File tmpFolder

    /**
     * Actual entry point for the action.
     * Calls out to the doTaskAction as needed.
     */
    @TaskAction
    void taskAction(IncrementalTaskInputs inputs) {
        Collection<File> inputFiles = getStreamInputs()

        if (!dexOptions.incremental || !enableIncremental) {
            doTaskAction(inputFiles, false /*incremental*/)
            return
        }

        if (!inputs.isIncremental()) {
            project.logger.info("Unable to do incremental execution: full task run.")
            doTaskAction(inputFiles, false /*incremental*/)
            return
        }

        AtomicBoolean forceFullRun = new AtomicBoolean()

        //noinspection GroovyAssignabilityCheck
        inputs.outOfDate { change ->
            // force full dx run if existing jar file is modified
            // New jar files are fine.
            if (change.isModified() && change.file.path.endsWith(SdkConstants.DOT_JAR)) {
                project.logger.info("Force full dx run: Found updated ${change.file}")
                forceFullRun.set(true)
            }
        }

        //noinspection GroovyAssignabilityCheck
        inputs.removed { change ->
            // force full dx run if existing jar file is removed
            if (change.file.path.endsWith(SdkConstants.DOT_JAR)) {
                project.logger.info("Force full dx run: Found removed ${change.file}")
                forceFullRun.set(true)
            }
        }

        doTaskAction(inputFiles, !forceFullRun.get())
    }

    private void doTaskAction(
            @Nullable Collection<File> inputFiles,
            boolean incremental) {
        File outFolder = getOutputFolder()
        if (!incremental) {
            FileUtils.emptyFolder(outFolder)
        }

        getBuilder().convertByteCode(
                inputFiles,
                project.files(getPreDexStreamInputs()).files,
                outFolder,
                getMultiDexEnabled(),
                getMainDexListFile(),
                getDexOptions(),
                getAdditionalParameters(),
                incremental,
                getOptimize(),
                new LoggedProcessOutputHandler(getILogger()))
    }

    public static class ConfigAction implements TaskConfigAction<Dex> {

        @NonNull
        private final VariantScope variantScope

        public ConfigAction(@NonNull VariantScope variantScope) {
            this.variantScope = variantScope
        }

        @Override
        public String getName() {
            return variantScope.getTaskName("dex")
        }

        @Override
        public Class<Dex> getType() {
            return Dex.class;
        }

        @Override
        public void execute(Dex dexTask) {
            ApkVariantData variantData = (ApkVariantData) variantScope.getVariantData();
            final GradleVariantConfiguration config = variantData.getVariantConfiguration();

            boolean isTestForApp = config.getType().isForTesting() && (DefaultGroovyMethods
                    .asType(variantData, TestVariantData.class)).getTestedVariantData()
                    .getVariantConfiguration().getType().equals(DEFAULT);

            boolean isMultiDexEnabled = config.isMultiDexEnabled() && !isTestForApp;
            boolean isLegacyMultiDexMode = config.isLegacyMultiDexMode();

            variantData.dexTask = dexTask;
            dexTask.setAndroidBuilder(variantScope.getGlobalScope().getAndroidBuilder())
            dexTask.setVariantName(config.getFullName())
            ConventionMappingHelper.map(dexTask, "outputFolder", new Callable<File>() {
                @Override
                public File call() throws Exception {
                    return variantScope.getDexOutputFolder();
                }
            });
            dexTask.setTmpFolder(new File(
                    String.valueOf(variantScope.getGlobalScope().getBuildDir()) + "/" + FD_INTERMEDIATES
                            + "/tmp/dex/" + config.getDirName()));
            dexTask.setDexOptions(variantScope.getGlobalScope().getExtension().getDexOptions());
            dexTask.setMultiDexEnabled(isMultiDexEnabled);
            // dx doesn't work with receving --no-optimize in debug so we disable it for now.
            dexTask.setOptimize(true);//!variantData.variantConfiguration.buildType.debuggable

            // inputs
            dexTask.inputStreams = variantScope.getTransformPipeline().getStreamsByTypes(
                    StreamType.CLASSES)
            dexTask.preDexStreams = variantScope.getTransformPipeline().getStreamsByTypes(StreamType.DEX)

            // TODO
            //ConventionMappingHelper.map(dexTask, "libraries", pcData.getInputLibrariesCallable());

            if (isMultiDexEnabled && isLegacyMultiDexMode) {
                // configure the dex task to receive the generated class list.
                ConventionMappingHelper.map(dexTask, "mainDexListFile", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return variantScope.getMainDexListFile();
                    }
                });
            }

            // no output stream for this class.
            dexTask.outputStreams = ImmutableList.of()
        }
    }
}
