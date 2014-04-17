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
package com.android.build.gradle.ndk

import com.android.build.gradle.api.AndroidSourceDirectorySet
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import com.android.build.gradle.ndk.internal.NdkBuilder
import com.android.build.gradle.ndk.internal.NdkConfigurationAction
import com.android.build.gradle.ndk.internal.NdkExtensionConventionAction
import com.android.build.gradle.ndk.internal.ToolchainConfigurationAction
import com.android.builder.core.VariantConfiguration
import com.android.builder.model.BuildType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.gradle.internal.Actions
import org.gradle.internal.reflect.Instantiator
import org.gradle.nativebinaries.internal.ProjectSharedLibraryBinary

import javax.inject.Inject

/**
 * Plugin for Android NDK applications.
 */
class NdkPlugin implements Plugin<Project> {
    protected Project project
    private NdkExtension extension
    private NdkBuilder ndkBuilder
    private ProjectConfigurationActionContainer configurationActions

    protected Instantiator instantiator

    @Inject
    public NdkPlugin(
            ProjectConfigurationActionContainer configurationActions,
            Instantiator instantiator) {
        this.configurationActions = configurationActions
        this.instantiator = instantiator
    }

    public Instantiator getInstantiator() {
        instantiator
    }

    public NdkExtension getNdkExtension() {
        extension
    }

    void apply(Project project) {
        this.project = project

        def sourceSetContainers = project.container(AndroidSourceDirectorySet) { name ->
            instantiator.newInstance(DefaultAndroidSourceDirectorySet, name, project.fileResolver)
        }
        extension = project.extensions.create("android_ndk", NdkExtension, sourceSetContainers)
        ndkBuilder = new NdkBuilder(project, extension)

        project.apply plugin: 'c'
        project.apply plugin: 'cpp'

        configurationActions.add(Actions.composite(
                new NdkExtensionConventionAction(),
                new ToolchainConfigurationAction(ndkBuilder, extension),
                new NdkConfigurationAction(ndkBuilder, extension)))
    }

    /**
     * Return the native binary tasks for a VariantConfiguration.
     */
    public List<Task> getNdkTasks(VariantConfiguration variantConfig) {
        if (variantConfig.getType() == VariantConfiguration.Type.TEST) {
            // Do not return tasks for test variants as test source set is not supported at the
            // moment.
            return []
        }
        List<ProjectSharedLibraryBinary> binaries =
                project.binaries.withType(ProjectSharedLibraryBinary).matching  { binary ->
                        binary.buildType.name.equals(variantConfig.getBuildType().getName()) &&
                                (variantConfig.getNdkConfig().getAbiFilters() == null ||
                                        variantConfig.getNdkConfig().getAbiFilters().contains(
                                                binary.targetPlatform.name))
        }
        binaries.collect { ProjectSharedLibraryBinary binary ->
            project.tasks.getByName(binary.getNamingScheme().getLifecycleTaskName())
        }
    }

    /**
     * Return the output directory of the native binary tasks for a VariantConfiguration.
     */
    public File getOutputDirectory(VariantConfiguration variantConfig) {
        BuildType buildType = variantConfig.buildType
        new File("$project.buildDir/binaries/${extension.getModuleName()}SharedLibrary/",
                (variantConfig.type == VariantConfiguration.Type.TEST) ? "test/" : ""
                        + "$buildType.name/lib")
    }
}
