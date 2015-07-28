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
package com.android.build.gradle.tasks

import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.dsl.DexOptions
import com.android.build.gradle.internal.scope.ConventionMappingHelper
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.BaseTask
import com.android.build.gradle.internal.variant.ApkVariantData
import com.android.build.gradle.internal.variant.TestVariantData
import com.android.ide.common.process.LoggedProcessOutputHandler
import com.android.utils.FileUtils
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Callable

import static com.android.builder.core.VariantType.DEFAULT

public class MergeDex extends BaseTask {

    // ----- PUBLIC TASK API -----

    @OutputDirectory
    File outputFolder

    // ----- PRIVATE TASK API -----
    @Input
    String getBuildToolsVersion() {
        getBuildTools().getRevision()
    }

    @InputDirectory
    File inputFolder

    @Nested
    DexOptions dexOptions

    @Input
    boolean multiDexEnabled = false

    @InputFile @Optional
    File mainDexListFile

    /**
     * Actual entry point for the action.
     * Calls out to the doTaskAction as needed.
     */
    @TaskAction
    void taskAction() {
        File outFolder = getOutputFolder()
        FileUtils.emptyFolder(outFolder)

        getBuilder().mergeDexFiles(
                getInputFolder(),
                outFolder,
                getMultiDexEnabled(),
                getMainDexListFile(),
                getDexOptions(),
                new LoggedProcessOutputHandler(getILogger()))
    }


    public static class ConfigAction implements TaskConfigAction<MergeDex> {

        private final VariantScope scope;

        public ConfigAction(VariantScope scope) {
            this.scope = scope;
        }

        @Override
        public String getName() {
            return scope.getTaskName("mergeDex")
        }

        @Override
        public Class<MergeDex> getType() {
            return MergeDex.class;
        }

        @Override
        public void execute(MergeDex dexTask) {
            ApkVariantData variantData = (ApkVariantData) scope.getVariantData();
            final GradleVariantConfiguration config = variantData.getVariantConfiguration();

            boolean isTestForApp = config.getType().isForTesting() && (DefaultGroovyMethods
                    .asType(variantData, TestVariantData.class)).getTestedVariantData()
                    .getVariantConfiguration().getType().equals(DEFAULT);

            boolean isMultiDexEnabled = config.isMultiDexEnabled() && !isTestForApp;
            boolean isLegacyMultiDexMode = config.isLegacyMultiDexMode();

            variantData.mergeDexTask = dexTask;
            dexTask.setAndroidBuilder(scope.getGlobalScope().getAndroidBuilder())
            dexTask.setVariantName(config.getFullName())

            ConventionMappingHelper.map(dexTask, "inputFolder", new Callable<File>() {
                @Override
                public File call() throws Exception {
                    return scope.getDexOutputFolder();
                }
            });

            ConventionMappingHelper.map(dexTask, "outputFolder", new Callable<File>() {
                @Override
                public File call() throws Exception {
                    return scope.getMergeDexOutputFolder();
                }
            });

            dexTask.setDexOptions(scope.getGlobalScope().getExtension().getDexOptions());
            dexTask.setMultiDexEnabled(isMultiDexEnabled);

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
