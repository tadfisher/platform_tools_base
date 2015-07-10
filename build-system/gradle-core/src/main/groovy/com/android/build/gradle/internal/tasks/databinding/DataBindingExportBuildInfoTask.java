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

package com.android.build.gradle.internal.tasks.databinding;

import com.android.build.gradle.internal.scope.TaskConfigAction;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.variant.LibraryVariantData;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import android.databinding.tool.LayoutXmlProcessor;
import android.databinding.tool.processing.Scope;

import java.io.File;

/**
 * Task to pass environment info to javac
 */
public class DataBindingExportBuildInfoTask extends DefaultTask {
    private LayoutXmlProcessor xmlProcessor;
    private File sdkDir;
    private File xmlOutFolder;
    private File exportClassListTo;
    private boolean printMachineReadableErrors;
    @TaskAction
    public void exportInfo() {
        System.out.println("exporting build info");
        xmlProcessor.writeInfoClass(sdkDir, xmlOutFolder, exportClassListTo,
                getLogger().isDebugEnabled(), printMachineReadableErrors);
        Scope.assertNoError();
    }

    public LayoutXmlProcessor getXmlProcessor() {
        return xmlProcessor;
    }

    public void setXmlProcessor(LayoutXmlProcessor xmlProcessor) {
        this.xmlProcessor = xmlProcessor;
    }

    public File getSdkDir() {
        return sdkDir;
    }

    public void setSdkDir(File sdkDir) {
        this.sdkDir = sdkDir;
    }

    public File getXmlOutFolder() {
        return xmlOutFolder;
    }

    public void setXmlOutFolder(File xmlOutFolder) {
        this.xmlOutFolder = xmlOutFolder;
    }

    public File getExportClassListTo() {
        return exportClassListTo;
    }

    public void setExportClassListTo(File exportClassListTo) {
        this.exportClassListTo = exportClassListTo;
    }

    public boolean isPrintMachineReadableErrors() {
        return printMachineReadableErrors;
    }

    public void setPrintMachineReadableErrors(boolean printMachineReadableErrors) {
        this.printMachineReadableErrors = printMachineReadableErrors;
    }

    public static class ConfigAction implements TaskConfigAction<DataBindingExportBuildInfoTask> {
        private final VariantScope variantScope;
        private final boolean printMachineReadableErrors;
        public ConfigAction(VariantScope scope, boolean printMachineReadableErrors) {
            variantScope = scope;
            this.printMachineReadableErrors = printMachineReadableErrors;
        }

        @Override
        public String getName() {
            return variantScope.getTaskName("dataBindingExportBuildInfo");
        }

        @Override
        public Class<DataBindingExportBuildInfoTask> getType() {
            return DataBindingExportBuildInfoTask.class;
        }

        @Override
        public void execute(DataBindingExportBuildInfoTask task) {
            task.setXmlProcessor(variantScope.getVariantData().getLayoutXmlProcessor());
            task.setSdkDir(variantScope.getGlobalScope().getSdkHandler().getSdkFolder());
            task.setXmlOutFolder(variantScope.getLayoutInfoOutputForDataBinding());
            boolean isLib = variantScope.getVariantData() instanceof LibraryVariantData;
            task.setExportClassListTo(isLib
                    ? variantScope.getGeneratedClassListOutputFileForDataBinding() : null);
            task.setPrintMachineReadableErrors(printMachineReadableErrors);
            // TODO
            variantScope.getVariantData().registerJavaGeneratingTask(task,
                    variantScope.getClassOutputForDataBinding());
        }
    }
}