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

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import android.databinding.tool.LayoutXmlProcessor;
import android.databinding.tool.processing.Scope;

import java.io.File;

import javax.xml.bind.JAXBException;

/**
 * Exports the processed layout information for the annotation processor.
 */
public class DataBindingExportLayoutInfoTask extends DefaultTask {
    LayoutXmlProcessor xmlProcessor;
    File xmlOutFolder;
    @TaskAction
    public void doIt() throws JAXBException {
        xmlProcessor.writeLayoutInfoFiles(xmlOutFolder);
        Scope.assertNoError();
    }

    public LayoutXmlProcessor getXmlProcessor() {
        return xmlProcessor;
    }

    public void setXmlProcessor(LayoutXmlProcessor xmlProcessor) {
        this.xmlProcessor = xmlProcessor;
    }

    public File getXmlOutFolder() {
        return xmlOutFolder;
    }

    public void setXmlOutFolder(File xmlOutFolder) {
        this.xmlOutFolder = xmlOutFolder;
    }

    public static class ConfigAction implements TaskConfigAction<DataBindingExportLayoutInfoTask> {
        private final VariantScope variantScope;
        public ConfigAction(VariantScope variantScope) {
            this.variantScope = variantScope;
        }

        @Override
        public String getName() {
            return variantScope.getTaskName("dataBindingExportLayoutInfo");
        }

        @Override
        public Class<DataBindingExportLayoutInfoTask> getType() {
            return DataBindingExportLayoutInfoTask.class;
        }

        @Override
        public void execute(DataBindingExportLayoutInfoTask task) {
            task.setXmlProcessor(variantScope.getVariantData().getLayoutXmlProcessor());
            task.setXmlOutFolder(variantScope.getLayoutInfoOutputForDataBinding());
        }
    }
}
