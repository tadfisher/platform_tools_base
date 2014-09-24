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


package com.android.build.gradle.model

import com.android.build.gradle.api.GroupableProductFlavor
import com.android.build.gradle.internal.ProductFlavorCombo
import com.android.build.gradle.internal.dsl.GroupableProductFlavorDsl
import com.android.build.gradle.ndk.NdkExtension
import com.android.build.gradle.ndk.internal.NdkConfigurationAction
import com.android.build.gradle.ndk.internal.NdkExtensionConventionAction
import com.android.build.gradle.ndk.internal.NdkHandler
import com.android.build.gradle.ndk.internal.ToolchainConfigurationAction
import com.android.builder.core.DefaultBuildType
import com.android.builder.core.VariantConfiguration
import com.android.builder.model.BuildType
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectIdentifier
import org.gradle.api.tasks.TaskContainer
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.language.base.ProjectSourceSet
import org.gradle.model.Finalize
import org.gradle.model.Model
import org.gradle.model.Mutate
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.BuildTypeContainer
import org.gradle.nativeplatform.FlavorContainer
import org.gradle.nativeplatform.NativeLibraryBinarySpec
import org.gradle.nativeplatform.NativeLibrarySpec
import org.gradle.nativeplatform.SharedLibraryBinarySpec
import org.gradle.nativeplatform.StaticLibraryBinary
import org.gradle.nativeplatform.internal.DefaultSharedLibraryBinarySpec
import org.gradle.nativeplatform.toolchain.NativeToolChainRegistry
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.ComponentSpecContainer
import org.gradle.platform.base.PlatformContainer

/**
 * Plugin for Android NDK applications.
 */
class NdkComponentModelPlugin implements Plugin<Project> {
    private Project project

    void apply(Project project) {
        this.project = project

        project.plugins.apply(AndroidComponentModelPlugin)

        project.apply plugin: 'c'
        project.apply plugin: 'cpp'

        // Remove static library tasks from assemble
        project.binaries.withType(StaticLibraryBinary) {
            it.buildable = false
        }
    }

    @RuleSource
    static class Rules {
        @Model("android.ndk")
        NdkExtension createAndroidNdk(ServiceRegistry serviceRegistry) {
            Instantiator instantiator = serviceRegistry.get(Instantiator.class)
            return instantiator.newInstance(NdkExtension)
        }

        @Model
        NdkHandler ndkHandler(ProjectIdentifier projectId, NdkExtension extension) {
            while (projectId.parentIdentifier != null) {
                projectId = projectId.parentIdentifier
            }
            return new NdkHandler(projectId.projectDir, extension)
        }

        @Finalize
        void setDefaultNdkExtensionValue(NdkExtension extension) {
            NdkExtensionConventionAction.setExtensionDefault(extension)
        }

        @Mutate
        void createAndroidPlatforms(PlatformContainer platforms, NdkHandler ndkHandler) {
            // Create android platforms.
            ToolchainConfigurationAction.configurePlatforms(platforms, ndkHandler)
        }

        @Mutate
        void createToolchains(
                NativeToolChainRegistry toolchains,
                NdkExtension ndkExtension,
                NdkHandler ndkHandler) {
            // Create toolchain for each ABI.
            ToolchainConfigurationAction.configureToolchain(
                    toolchains,
                    ndkExtension.getToolchain(),
                    ndkExtension.getToolchainVersion(),
                    ndkHandler)
        }

        @Mutate
        void createNativeBuildTypes(
                BuildTypeContainer nativeBuildTypes,
                NamedDomainObjectContainer<DefaultBuildType> androidBuildTypes) {
            for (def buildType : androidBuildTypes) {
                nativeBuildTypes.maybeCreate(buildType.name)
            }
        }

        @Mutate
        void createNativeFlavors(
                FlavorContainer nativeFlavors,
                List<ProductFlavorCombo> androidFlavorGroups) {
            for (def group : androidFlavorGroups) {
                nativeFlavors.maybeCreate(group.name)
            }
        }

        @Mutate
        void createNativeLibrary(
                ComponentSpecContainer specs,
                NdkExtension extension,
                NdkHandler ndkHandler,
                ProjectSourceSet sources,
                @Path("buildDir") File buildDir) {
            if (extension.moduleName != null) {
                specs.withType(DefaultAndroidComponentSpec) { androidSpec ->
                    NativeLibrarySpec library =
                            specs.create(extension.moduleName, NativeLibrarySpec)
                    androidSpec.nativeLibrary = library
                    NdkConfigurationAction.configureProperties(
                            library, sources, buildDir, extension, ndkHandler)
                }
            }
        }

        @Mutate
        void createAdditionalTasksForNatives(
                TaskContainer tasks,
                ComponentSpecContainer specs,
                NdkExtension extension,
                NdkHandler ndkHandler,
                @Path("buildDir") File buildDir) {
            specs.withType(DefaultAndroidComponentSpec) { androidSpec ->
                if (androidSpec.nativeLibrary != null) {
                    androidSpec.nativeLibrary.binaries.withType(DefaultSharedLibraryBinarySpec) { binary ->
                        NdkConfigurationAction.createTasks(
                                tasks, binary, buildDir, extension, ndkHandler)
                    }
                }
            }
        }

        @Mutate
        void attachNativeBinaryToAndroid(
                BinaryContainer binaries,
                ComponentSpecContainer specs,
                NdkExtension extension) {
            if (extension.moduleName != null) {
                NativeLibrarySpec library =
                        specs.withType(NativeLibrarySpec).getByName(extension.moduleName);
                binaries.withType(DefaultAndroidBinary) { binary ->
                    def nativeBinaries = getNativeBinaries(library, binary.buildType, binary.productFlavors)
                    binary.getNativeBinaries().addAll(nativeBinaries)
                }
            }
        }

        /**
         * Create all source sets for each AndroidBinary.
         */
        @Mutate
        void configureNativeSourceSet(
                ProjectSourceSet sources,
                NamedDomainObjectContainer<DefaultBuildType> buildTypes,
                NamedDomainObjectContainer<GroupableProductFlavorDsl> flavors,
                List<ProductFlavorCombo> flavorGroups,
                NdkExtension ndkExtension) {
            NdkConfigurationAction.configureSources(sources, "main", ndkExtension)
            for (def buildType : buildTypes) {
                NdkConfigurationAction.configureSources(sources, buildType.name, ndkExtension)
            }
            for (def group : flavorGroups) {
                NdkConfigurationAction.configureSources(sources, group.name, ndkExtension)
                if (!group.flavorList.isEmpty()) {
                    for (def buildType : buildTypes) {
                        NdkConfigurationAction.configureSources(
                                sources, group.name + buildType.name.capitalize(), ndkExtension)
                    }
                }
            }
            if (flavorGroups.size() != flavors.size()) {
                // If flavorGroups and flavors are the same size, there is at most 1 flavor
                // dimension.  So we don't need to reconfigure the source sets for flavorGroups.
                for (def flavor : flavors) {
                    NdkConfigurationAction.configureSources(sources, flavor.name, ndkExtension)
                }
            }
        }

        @Finalize
        void attachNativeTasksToAssembleTasks(BinaryContainer binaries) {
            binaries.withType(DefaultAndroidBinary) { binary ->
                if (binary.targetAbi.isEmpty())  {
                    binary.builtBy(binary.nativeBinaries)
                } else {
                    binary.nativeBinaries.each { nativeBinary ->
                        if (binary.targetAbi.contains(nativeBinary.targetPlatform.name)) {
                            binary.builtBy(nativeBinary)
                        }
                    }
                }
            }
        }

        /**
         * Remove unintended tasks created by Gradle native plugin from task list.
         *
         * Gradle native plugins creates static library tasks automatically.  This method removes them
         * to avoid cluttering the task list.
         */
        @Mutate
        void hideNativeTasks(TaskContainer tasks, BinaryContainer binaries) {
            // Gradle do not support a way to remove created tasks.  The best workaround is to clear the
            // group of the task and have another task depends on it.  Therefore, we have to create
            // a dummy task to depend on all the tasks that we do not want to show up on the task
            // list. The dummy task dependsOn itself, effectively making it non-executable and
            // invisible unless the --all option is use.
            Task nonExecutableTask = tasks.create("nonExecutableTask")
            nonExecutableTask.dependsOn nonExecutableTask
            nonExecutableTask.description =
                    "Dummy task to hide other unwanted tasks in the task list."

            binaries.withType(NativeLibraryBinarySpec) { binary ->
                Task buildTask = binary.getBuildTask()
                nonExecutableTask.dependsOn buildTask
                buildTask.group = null
            }
        }
    }

    private static Collection<SharedLibraryBinarySpec> getNativeBinaries(
            NativeLibrarySpec library,
            BuildType buildType,
            List<? extends GroupableProductFlavor> productFlavors) {
        ProductFlavorCombo flavorGroup = new ProductFlavorCombo(productFlavors);
        library.binaries.withType(SharedLibraryBinarySpec).matching { binary ->
            (binary.buildType.name.equals(buildType.name)
                    && ((productFlavors.isEmpty() && binary.flavor.name.equals("default"))
                        || binary.flavor.name.equals(flavorGroup.name)))
        }
    }

    /**
     * Return library binaries for a VariantConfiguration.
     */
    public Collection<DefaultSharedLibraryBinarySpec> getBinaries(
            VariantConfiguration variantConfig) {
        if (variantConfig.getType() == VariantConfiguration.Type.TEST) {
            // Do not return binaries for test variants as test source set is not supported at the
            // moment.
            return []
        }

        project.binaries.withType(DefaultSharedLibraryBinarySpec).matching { binary ->
            (binary.buildType.name.equals(variantConfig.getBuildType().getName())
                    && (binary.flavor.name.equals(variantConfig.getFlavorName())
                            || (binary.flavor.name.equals("default")
                                    && variantConfig.getFlavorName().isEmpty()))
                    && (variantConfig.getNdkConfig().getAbiFilters() == null
                            || variantConfig.getNdkConfig().getAbiFilters().contains(
                                    binary.targetPlatform.name)))
        }
    }

    /**
     * Return the output directory of the native binary tasks for a VariantConfiguration.
     */
    public Collection<File> getOutputDirectories(VariantConfiguration variantConfig) {
        // Return the parent's parent directory of the binaries' output.
        // A binary's output file is set to something in the form of
        // "/path/to/lib/platformName/libmodulename.so".  We want to return "/path/to/lib".
        (getBinaries(variantConfig)
                *.getPrimaryOutput()
                *.getParentFile()
                *.getParentFile()
                .unique())
    }
}

