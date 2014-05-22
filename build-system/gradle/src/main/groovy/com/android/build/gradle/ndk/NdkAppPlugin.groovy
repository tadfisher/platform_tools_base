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

import com.android.SdkConstants
import com.android.build.gradle.api.AndroidSourceDirectorySet
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet
import com.android.build.gradle.ndk.internal.NdkExtensionConventionAction
import com.android.builder.BuilderConstants
import com.android.builder.VariantConfiguration

import com.android.builder.model.BuildType
import com.android.build.gradle.ndk.internal.NdkConfigurationAction
import com.android.build.gradle.ndk.internal.NdkBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskCollection
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.gradle.internal.Actions
import org.gradle.internal.reflect.Instantiator
import org.gradle.nativebinaries.tasks.LinkSharedLibrary

import javax.inject.Inject

/**
 * Plugin for Android NDK applications.
 */
class NdkAppPlugin implements Plugin<Project> {
    protected Project project
    private NdkExtension extension
    private NdkBuilder ndkBuilder
    private ProjectConfigurationActionContainer configurationActions
    private NdkConfigurationAction configAction

    protected Instantiator instantiator

    @Inject
    public NdkAppPlugin(
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
        project.apply plugin: 'assembler'

        project.model {
            buildTypes {
                maybeCreate(BuilderConstants.DEBUG)
                maybeCreate(BuilderConstants.RELEASE)
            }
        }

        // Configure all platforms.  Currently missing support for mips.
        project.model {
            platforms {
                "$SdkConstants.ABI_INTEL_ATOM" {
                    architecture SdkConstants.CPU_ARCH_INTEL_ATOM
                }
                "$SdkConstants.ABI_ARMEABI" {
                    architecture SdkConstants.CPU_ARCH_ARM
                }
                "$SdkConstants.ABI_ARMEABI_V7A" {
                    architecture SdkConstants.CPU_ARCH_ARM
                }
            }

        }

        configurationActions.add(Actions.composite(
                new NdkExtensionConventionAction(),
                new NdkConfigurationAction(extension, ndkBuilder)))
    }

    /**
     * Return the expected native binary tasks for a VariantConfiguration.
     */
    public TaskCollection getNdkTasks(VariantConfiguration variantConfig) {
        project.tasks.withType(LinkSharedLibrary).matching { task ->
//            ((variantConfig.getBuildType().isDebuggable()
//                    ? task.name.contains("Debug")
//                    : task.name.contains("Release"))
            (task.name.contains(variantConfig.getBuildType().getName().capitalize())
            && (variantConfig.getNdkConfig().getAbiFilters() == null
                    || variantConfig.getNdkConfig().getAbiFilters().contains(
                            task.targetPlatform.name)))
        }
    }

    /**
     * Return the expected location of the native binary for a VariantConfiguration.
     */
    public File getOutputDirectory(VariantConfiguration variantConfig) {
        BuildType buildType = variantConfig.buildType
        new File("$project.buildDir/binaries/${extension.getModuleName()}SharedLibrary/",
                (variantConfig.type == VariantConfiguration.Type.TEST) ? "test/" : ""
                        + "$buildType.name/lib")
    }
}
