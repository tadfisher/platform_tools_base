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

package com.android.build.gradle.internal.tasks;

import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.api.AndroidSourceSet;
import com.google.common.collect.Lists;

import org.gradle.api.Project;
import org.gradle.api.tasks.diagnostics.AbstractReportTask;
import org.gradle.api.tasks.diagnostics.internal.ReportRenderer;
import org.gradle.api.tasks.diagnostics.internal.TextReportRenderer;
import org.gradle.logging.StyledTextOutput;
import org.gradle.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Prints out the DSL names and directory names of available source sets.
 */
public class SourceSetsTask extends AbstractReportTask {

    private final TextReportRenderer mRenderer = new TextReportRenderer();

    @Override
    protected ReportRenderer getRenderer() {
        return mRenderer;
    }

    @Override
    protected void generate(Project project) throws IOException {
        BaseExtension extension = project.getExtensions().findByType(BaseExtension.class);
        if (extension != null) {
            for (AndroidSourceSet sourceSet : extension.getSourceSets()) {
                mRenderer.getBuilder().subheading(sourceSet.getName());
                List<String> relativePaths = Lists.newArrayList();

                for (File file : sourceSet.getJava().getSrcDirs()) {
                    relativePaths.add(project.getRootProject().relativePath(file));
                }

                renderKeyValue("Compile configuration: ", sourceSet.getCompileConfigurationName());
                renderKeyValue("Java sources: ", String.format("[%s]", CollectionUtils.join(", ", relativePaths)));
                renderKeyValue("build.gradle name: ", "android.sourceSets." + sourceSet.getName());

                mRenderer.getTextOutput().println();
            }
        }

        mRenderer.complete();
    }

    private void renderKeyValue(String o, String o1) {
        mRenderer.getTextOutput()
                .withStyle(StyledTextOutput.Style.Identifier)
                .text(o);

        mRenderer.getTextOutput()
                .withStyle(StyledTextOutput.Style.Info)
                .text(o1);

        mRenderer.getTextOutput().println();
    }
}
