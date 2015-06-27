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

import com.android.build.gradle.managed.ExternalBuildConfig;
import com.google.common.collect.Lists;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Exec;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.model.Model;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.RuleSource;
import org.gradle.platform.base.BinaryTasks;
import org.gradle.platform.base.BinaryType;
import org.gradle.platform.base.BinaryTypeBuilder;
import org.gradle.platform.base.ComponentBinaries;
import org.gradle.platform.base.ComponentType;
import org.gradle.platform.base.ComponentTypeBuilder;

/**
 * Plugin for importing projects built with external tools into Android Studio.
 */
public class ExternalNativeComponentModelPlugin implements Plugin<Project> {

    public static final String COMPONENT_NAME = "androidExternalBuild";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(ComponentModelBasePlugin.class);
    }

    @SuppressWarnings({"MethodMayBeStatic", "unused"})
    public static class Rules extends RuleSource {

        @ComponentType
        public void defineComponentType(ComponentTypeBuilder<ExternalNativeComponentSpec> builder) {
            builder.defaultImplementation(ExternalNativeComponentSpec.class);
        }

        @BinaryType
        public void defineBinaryType(BinaryTypeBuilder<ExternalNativeBinarySpec> builder) {
            builder.defaultImplementation(ExternalNativeBinarySpec.class);
        }

        @Model("nativeBuild")
        void createNativeBuildModel(ExternalBuildConfig config) {
            config.setArgs(Lists.<String>newArrayList());
        }

        @Mutate
        void createExternalNativeComponent(
                ModelMap<ExternalNativeComponentSpec> components,
                final ExternalBuildConfig config) {
            components.create(COMPONENT_NAME, new Action<ExternalNativeComponentSpec>() {
                @Override
                public void execute(ExternalNativeComponentSpec component) {
                    component.setConfig(config);
                }
            });
        }

        @ComponentBinaries
        void createExternalNativeBinary(
                final ModelMap<ExternalNativeBinarySpec> binaries,
                final ExternalNativeComponentSpec component) {
            binaries.create(component.getName(), new Action<ExternalNativeBinarySpec>() {
                @Override
                public void execute(ExternalNativeBinarySpec binary) {
                    binary.setConfig(component.getConfig());
                }
            });
        }

        @BinaryTasks
        void createTasks(ModelMap<Task> tasks, final ExternalNativeBinarySpec binary) {
            tasks.create("execute", Exec.class, new Action<Exec>() {
                @Override
                public void execute(Exec exec) {
                    exec.executable(binary.getConfig().getExecutable());
                    exec.args(binary.getConfig().getArgs());
                }
            });
        }
    }
}
