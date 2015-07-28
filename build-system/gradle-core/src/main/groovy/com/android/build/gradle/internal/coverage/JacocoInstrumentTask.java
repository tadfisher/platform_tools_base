/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.build.gradle.internal.coverage;

import com.android.build.gradle.internal.scope.ConventionMappingHelper;
import com.android.build.gradle.internal.scope.TaskConfigAction;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.PostCompilationData;
import com.android.build.gradle.internal.tasks.IncrementalTask;
import com.android.ide.common.res2.FileStatus;
import com.android.utils.FileUtils;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;

import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.OfflineInstrumentationAccessGenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Simple Jacoco instrument task that calls the Ant version.
 */
public class JacocoInstrumentTask extends IncrementalTask {

    private File inputDir;

    private File outputDir;

    /**
     * Classpath containing Jacoco classes for use by the task.
     */
    private FileCollection jacocoClasspath;

    @Override
    protected boolean isIncremental() {
        return true;
    }

    @Override
    protected void doFullTaskAction() throws IOException {
        File outDir = getOutputDir();
        FileUtils.emptyFolder(outDir);

        Iterable<File> files = Files.fileTreeTraverser().preOrderTraversal(getInputDir()).filter(
                new Predicate<File>() {
                    @Override
                    public boolean apply(File file) {
                        return file.isFile();
                    }
                });
        Map<File, FileStatus> changedInputs = Maps.newHashMap();
        for (File file: files) {
            changedInputs.put(file, FileStatus.NEW);
        }
        doIncrementalTaskAction(changedInputs);

    }

    @Override
    protected void doIncrementalTaskAction(Map<File, FileStatus> changedInputs) throws IOException {
        Instrumenter instrumenter = new Instrumenter(new OfflineInstrumentationAccessGenerator());

        String inputPath = getInputDir().getAbsolutePath();
        File outputDir = getOutputDir().getAbsoluteFile();

        for (Map.Entry<File, FileStatus> changedInput: changedInputs.entrySet()) {
            File outputFile = new File(
                    outputDir,
                    changedInput.getKey().getAbsolutePath().substring(inputPath.length() + 1));
            switch (changedInput.getValue()) {
                case REMOVED:
                    outputFile.delete();
                    break;
                case NEW:
                    // fall through
                case CHANGED:
                    InputStream inputStream = null;
                    try {
                        inputStream = Files.asByteSource(changedInput.getKey())
                                .openBufferedStream();
                        outputFile.getParentFile().mkdirs();
                        Files.write(instrumenter.instrument(
                                inputStream,
                                changedInput.getKey().toString()), outputFile);
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
            }
        }
    }

    @InputDirectory
    public File getInputDir() {
        return inputDir;
    }

    public void setInputDir(File inputDir) {
        this.inputDir = inputDir;
    }

    @OutputDirectory
    public File getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    @InputFiles
    public FileCollection getJacocoClasspath() {
        return jacocoClasspath;
    }

    public void setJacocoClasspath(FileCollection jacocoClasspath) {
        this.jacocoClasspath = jacocoClasspath;
    }

    public static class ConfigAction implements TaskConfigAction<JacocoInstrumentTask> {

        private VariantScope scope;
        private PostCompilationData pcData;

        public ConfigAction(VariantScope scope, PostCompilationData pcData) {
            this.scope = scope;
            this.pcData = pcData;
        }

        @Override
        public String getName() {
            return scope.getTaskName("instrument");
        }

        @Override
        public Class<JacocoInstrumentTask> getType() {
            return JacocoInstrumentTask.class;
        }

        @Override
        public void execute(JacocoInstrumentTask jacocoTask) {

            ConventionMappingHelper.map(jacocoTask, "jacocoClasspath", new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return scope.getGlobalScope().getProject().getConfigurations().getByName(JacocoPlugin.ANT_CONFIGURATION_NAME);
                }
            });

            // can't directly use the existing inputFiles closure as we need the dir instead :\
            ConventionMappingHelper.map(jacocoTask, "inputDir", pcData.getInputDirCallable());

            jacocoTask.setVariantName(scope.getVariantConfiguration().getFullName());
            jacocoTask.setOutputDir(scope.getCoverageInstrumentedClassesFolder());

            scope.getVariantData().jacocoInstrumentTask = jacocoTask;
        }
    }
}
