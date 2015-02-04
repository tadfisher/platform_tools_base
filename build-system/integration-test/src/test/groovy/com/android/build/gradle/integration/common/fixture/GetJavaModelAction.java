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

package com.android.build.gradle.integration.common.fixture;

import com.google.common.collect.Maps;

import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.gradle.tooling.model.idea.IdeaProject;

import java.util.Map;

/**
 * a Build Action that returns all the models for all the Gradle projects
 */
public class GetJavaModelAction implements BuildAction<Map<String, IdeaProject>> {

    @Override
    public Map<String, IdeaProject> execute(BuildController buildController) {
        GradleBuild gradleBuild = buildController.getBuildModel();
        DomainObjectSet<? extends BasicGradleProject> projects = gradleBuild.getProjects();

        Map<String, IdeaProject> modelMap = Maps.newHashMapWithExpectedSize(projects.size());

        for (BasicGradleProject project : projects) {
            IdeaProject model = buildController.findModel(project, IdeaProject.class);
            if (model != null) {
                modelMap.put(project.getPath(), model);
            }
        }

        return modelMap;
    }
}
