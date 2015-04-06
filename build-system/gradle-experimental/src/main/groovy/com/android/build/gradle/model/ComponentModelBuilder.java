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

package com.android.build.gradle.model;

import static com.android.build.gradle.model.ModelConstants.ANDROID_BUILDER;
import static com.android.build.gradle.model.ModelConstants.ANDROID_COMPONENT_SPEC;
import static com.android.build.gradle.model.ModelConstants.EXTRA_MODEL_INFO;
import static com.android.build.gradle.model.ModelConstants.IS_APPLICATION;
import static com.android.build.gradle.model.ModelConstants.TASK_MANAGER;

import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.ExtraModelInfo;
import com.android.build.gradle.internal.TaskManager;
import com.android.build.gradle.internal.VariantManager;
import com.android.build.gradle.internal.model.ModelBuilder;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.model.AndroidProject;

import org.gradle.api.Project;
import org.gradle.model.internal.core.ModelPath;
import org.gradle.model.internal.registry.ModelRegistry;
import org.gradle.model.internal.type.ModelType;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

/**
 * ToolingModelBuilder for creating AndroidProject in the component model plugin.
 */
public class ComponentModelBuilder implements ToolingModelBuilder {

    ModelBuilder modelBuilder;
    ModelRegistry registry;

    public ComponentModelBuilder(ModelRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(AndroidProject.class.getName());
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        if (modelBuilder == null) {
            modelBuilder = createModelBuilder();
        }
        return modelBuilder.buildAll(modelName, project);
    }

    private ModelBuilder createModelBuilder() {
        AndroidBuilder androidBuilder = registry.realize(
                new ModelPath(ANDROID_BUILDER),
                ModelType.of(AndroidBuilder.class));
        DefaultAndroidComponentSpec componentSpec = (DefaultAndroidComponentSpec) registry.realize(
                new ModelPath(ANDROID_COMPONENT_SPEC),
                ModelType.of(AndroidComponentSpec.class));
        VariantManager variantManager = componentSpec.getVariantManager();
        TaskManager taskManager = registry.realize(
                new ModelPath(TASK_MANAGER),
                ModelType.of(TaskManager.class));
        BaseExtension extension = componentSpec.getExtension();
        ExtraModelInfo extraModelInfo = registry.realize(
                new ModelPath(EXTRA_MODEL_INFO),
                ModelType.of(ExtraModelInfo.class));
        Boolean isApplication = registry.realize(
                new ModelPath(IS_APPLICATION),
                ModelType.of(Boolean.class));
        return new ModelBuilder(
                androidBuilder, variantManager, taskManager,
                extension, extraModelInfo, !isApplication);
    }
}
