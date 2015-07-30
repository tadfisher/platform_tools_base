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
package com.android.build.gradle.tasks;

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.android.build.gradle.internal.core.GradleVariantConfiguration;
import com.android.build.gradle.internal.dsl.DexOptions;
import com.android.build.gradle.internal.scope.ConventionMappingHelper;
import com.android.build.gradle.internal.scope.TaskConfigAction;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.tasks.BaseTask;
import com.android.build.gradle.internal.variant.ApkVariantData;
import com.android.build.gradle.internal.variant.TestVariantData;
import com.android.ide.common.process.LoggedProcessOutputHandler;
import com.android.ide.common.process.ProcessException;
import com.android.utils.FileUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import com.android.build.gradle.internal.PostCompilationData;
import com.google.common.base.Preconditions;

import org.gradle.api.Action;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.android.builder.core.VariantType.DEFAULT;
import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES;

public class Dex extends BaseTask {

    // ----- PUBLIC TASK API -----

    private File outputFolder;

    private List<String> additionalParameters;

    private boolean enableIncremental = true;

    // ----- PRIVATE TASK API -----
    @Input
    public String getBuildToolsVersion() {
        return getBuildTools().getRevision().toString();
    }

    private Collection<File> inputFiles;

    private File inputDir;

    private Collection<File> libraries;

    private DexOptions dexOptions;

    private boolean multiDexEnabled = false;

    private boolean optimize = true;

    private File mainDexListFile;

    private File tmpFolder;

    /**
     * Actual entry point for the action.
     * Calls out to the doTaskAction as needed.
     */
    @TaskAction
    public void taskAction(final IncrementalTaskInputs inputs)
            throws InterruptedException, ProcessException, IOException {
        final Collection<File> inputFiles = getInputFiles();
        final File inputDir = getInputDir();

        if (!dexOptions.getIncremental() || !isEnableIncremental()) {
            doTaskAction(inputFiles, inputDir, false /*incremental*/);
            return;
        }

        if (!inputs.isIncremental()) {
            getProject().getLogger().info("Unable to do incremental execution: full task run.");
            doTaskAction(inputFiles, inputDir, false /*incremental*/);
            return;
        }

        final AtomicBoolean forceFullRun = new AtomicBoolean();

        inputs.outOfDate(new Action<InputFileDetails>() {
            @Override
            public void execute(InputFileDetails change) {
                // force full dx run if existing jar file is modified
                // New jar files are fine.
                if (change.isModified() && change.getFile().getPath().endsWith(SdkConstants.DOT_JAR)) {
                    getProject().getLogger().info("Force full dx run: Found updated ${change.file}");
                    forceFullRun.set(true);
                }
            }
        });

        inputs.removed(new Action<InputFileDetails>() {
            @Override
            public void execute(InputFileDetails change) {
                // force full dx run if existing jar file is removed
                if (change.getFile().getPath().endsWith(SdkConstants.DOT_JAR)) {
                    getProject().getLogger()
                            .info("Force full dx run: Found removed ${change.file}");
                    forceFullRun.set(true);
                }
            }
        });

        doTaskAction(inputFiles, inputDir, !forceFullRun.get());
    }

    private void doTaskAction(
            @Nullable Collection<File> inputFiles,
            @Nullable File inputDir,
            boolean incremental) throws IOException, ProcessException, InterruptedException {

        if (inputFiles == null && inputDir == null) {
            throw new RuntimeException(String.format(
                    "Dex task '%s': inputDir and inputFiles cannot both be null", getName()));
        }

        File outFolder = getOutputFolder();
        if (!incremental) {
            FileUtils.emptyFolder(outFolder);
        }

        File tmpFolder = getTmpFolder();
        tmpFolder.mkdirs();

        // if some of our .jar input files exist, just reset the inputDir to null
        if (inputFiles != null) {
            for (File inputFile : inputFiles) {
                if (inputFile.exists()) {
                    inputDir = null;
                }
            }
        }
        if (inputDir != null) {
            inputFiles = getProject().files(inputDir).getFiles();
        }


        getBuilder().convertByteCode(
                inputFiles,
                getLibraries(),
                outFolder,
                isMultiDexEnabled(),
                getMainDexListFile(),
                getDexOptions(),
                getAdditionalParameters(),
                tmpFolder,
                incremental,
                isOptimize(),
                new LoggedProcessOutputHandler(getILogger()));
    }

    @OutputDirectory
    public File getOutputFolder() {
        return outputFolder;
    }

    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    @Input @Optional
    public List<String> getAdditionalParameters() {
        return additionalParameters;
    }

    public void setAdditionalParameters(List<String> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    public boolean isEnableIncremental() {
        return enableIncremental;
    }

    public void setEnableIncremental(boolean enableIncremental) {
        this.enableIncremental = enableIncremental;
    }

    @InputFiles @Optional
    public Collection<File> getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(Collection<File> inputFiles) {
        this.inputFiles = inputFiles;
    }

    @InputDirectory @Optional
    public File getInputDir() {
        return inputDir;
    }

    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
    }

    @InputFiles
    public Collection<File> getLibraries() {
        return libraries;
    }

    public void setLibraries(Collection<File> libraries) {
        this.libraries = libraries;
    }

    @Nested
    public DexOptions getDexOptions() {
        return dexOptions;
    }

    public void setDexOptions(DexOptions dexOptions) {
        this.dexOptions = dexOptions;
    }

    @Input
    public boolean isMultiDexEnabled() {
        return multiDexEnabled;
    }

    public void setMultiDexEnabled(boolean multiDexEnabled) {
        this.multiDexEnabled = multiDexEnabled;
    }

    @Input
    public boolean isOptimize() {
        return optimize;
    }

    public void setOptimize(boolean optimize) {
        this.optimize = optimize;
    }

    @InputFile @Optional
    public File getMainDexListFile() {
        return mainDexListFile;
    }

    public void setMainDexListFile(File mainDexListFile) {
        this.mainDexListFile = mainDexListFile;
    }

    public File getTmpFolder() {
        return tmpFolder;
    }

    public void setTmpFolder(File tmpFolder) {
        this.tmpFolder = tmpFolder;
    }

    public static class ConfigAction implements TaskConfigAction<Dex> {

        private final VariantScope scope;

        private final PostCompilationData pcData;

        public ConfigAction(VariantScope scope, PostCompilationData pcData) {
            this.scope = scope;
            this.pcData = pcData;
        }

        @Override
        public String getName() {
            return scope.getTaskName("dex");
        }

        @Override
        public Class<Dex> getType() {
            return Dex.class;
        }

        @Override
        public void execute(Dex dexTask) {
            ApkVariantData variantData = (ApkVariantData) scope.getVariantData();
            final GradleVariantConfiguration config = variantData.getVariantConfiguration();

            boolean isTestForApp = config.getType().isForTesting() && (DefaultGroovyMethods
                    .asType(variantData, TestVariantData.class)).getTestedVariantData()
                    .getVariantConfiguration().getType().equals(DEFAULT);

            boolean isMultiDexEnabled = config.isMultiDexEnabled() && !isTestForApp;
            boolean isLegacyMultiDexMode = config.isLegacyMultiDexMode();

            variantData.dexTask = dexTask;
            dexTask.setAndroidBuilder(scope.getGlobalScope().getAndroidBuilder());
            dexTask.setVariantName(config.getFullName());
            ConventionMappingHelper.map(dexTask, "outputFolder", new Callable<File>() {
                @Override
                public File call() throws Exception {
                    return scope.getDexOutputFolder();
                }
            });
            dexTask.setTmpFolder(new File(
                    String.valueOf(scope.getGlobalScope().getBuildDir()) + "/" + FD_INTERMEDIATES
                            + "/tmp/dex/" + config.getDirName()));
            dexTask.setDexOptions(scope.getGlobalScope().getExtension().getDexOptions());
            dexTask.setMultiDexEnabled(isMultiDexEnabled);
            // dx doesn't work with receving --no-optimize in debug so we disable it for now.
            dexTask.setOptimize(true);//!variantData.variantConfiguration.buildType.debuggable

            // inputs
            if (pcData.getInputDirCallable() != null) {
                ConventionMappingHelper.map(dexTask, "inputDir", pcData.getInputDirCallable());
            }
            ConventionMappingHelper.map(dexTask, "inputFiles", pcData.getInputFilesCallable());
            ConventionMappingHelper.map(dexTask, "libraries", pcData.getInputLibrariesCallable());

            if (isMultiDexEnabled && isLegacyMultiDexMode) {
                // configure the dex task to receive the generated class list.
                ConventionMappingHelper.map(dexTask, "mainDexListFile", new Callable<File>() {
                    @Override
                    public File call() throws Exception {
                        return scope.getMainDexListFile();
                    }
                });
            }
        }
    }
}
