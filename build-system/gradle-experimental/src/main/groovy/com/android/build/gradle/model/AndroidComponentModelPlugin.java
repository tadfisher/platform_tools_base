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

import static com.android.build.gradle.model.ModelConstants.ANDROID_COMPONENT_SPEC;
import static com.android.builder.core.VariantType.ANDROID_TEST;
import static com.android.builder.core.VariantType.UNIT_TEST;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.ProductFlavorCombo;
import com.android.build.gradle.internal.TaskInfo;
import com.android.build.gradle.internal.TaskInfoRegistry;
import com.android.build.gradle.internal.TaskType;
import com.android.build.gradle.managed.AndroidConfig;
import com.android.build.gradle.managed.BuildType;
import com.android.build.gradle.managed.ProductFlavor;
import com.android.build.gradle.managed.adaptor.BuildTypeAdaptor;
import com.android.build.gradle.managed.adaptor.ProductFlavorAdaptor;
import com.android.builder.core.BuilderConstants;
import com.android.sdklib.repository.FullRevision;
import com.android.utils.StringHelper;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.ClosureBackedAction;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.language.base.ProjectSourceSet;
import org.gradle.language.base.internal.registry.LanguageRegistration;
import org.gradle.language.base.internal.registry.LanguageRegistry;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.model.Defaults;
import org.gradle.model.Finalize;
import org.gradle.model.Model;
import org.gradle.model.Mutate;
import org.gradle.model.Path;
import org.gradle.model.RuleSource;
import org.gradle.model.collection.CollectionBuilder;
import org.gradle.model.collection.ManagedSet;
import org.gradle.platform.base.BinaryContainer;
import org.gradle.platform.base.BinaryType;
import org.gradle.platform.base.BinaryTypeBuilder;
import org.gradle.platform.base.ComponentBinaries;
import org.gradle.platform.base.ComponentSpecContainer;
import org.gradle.platform.base.ComponentType;
import org.gradle.platform.base.ComponentTypeBuilder;
import org.gradle.platform.base.LanguageType;
import org.gradle.platform.base.LanguageTypeBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.lang.Closure;

/**
 * Plugin to set up infrastructure for other android plugins.
 */
public class AndroidComponentModelPlugin implements Plugin<Project> {

    /**
     * The name of ComponentSpec created with android component model plugin.
     */
    public static final String COMPONENT_NAME = "android";

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(ComponentModelBasePlugin.class);
    }

    @SuppressWarnings("MethodMayBeStatic")
    public static class Rules extends RuleSource {

        @LanguageType
        public void registerLanguage(LanguageTypeBuilder<AndroidLanguageSourceSet> builder) {
            builder.setLanguageName("android");
            builder.defaultImplementation(AndroidLanguageSourceSet.class);
        }

        /**
         * Create "android" model block.
         */
        @Model("android")
        public void android(AndroidConfig androidModel) {
        }

        @Defaults
        public void androidModelSources(AndroidConfig androidModel,
                @Path("androidSources") AndroidComponentModelSourceSet sources) {
            androidModel.setSources(sources);
        }

        @Finalize
        public void finalizeAndroidModel(AndroidConfig androidModel) {
            if (androidModel.getBuildToolsRevision() == null
                    && androidModel.getBuildToolsVersion() != null) {
                androidModel.setBuildToolsRevision(
                        FullRevision.parseRevision(androidModel.getBuildToolsVersion()));
            }

            if (androidModel.getCompileSdkVersion() != null
                    && !androidModel.getCompileSdkVersion().startsWith("android-")
                    && Ints.tryParse(androidModel.getCompileSdkVersion()) != null) {
                androidModel.setCompileSdkVersion("android-" + androidModel.getCompileSdkVersion());
            }

        }

        @Defaults
        public void createDefaultBuildTypes(
                @Path("android.buildTypes") ManagedSet<BuildType> buildTypes) {
            buildTypes.create(new Action<BuildType>() {
                @Override
                public void execute(BuildType buildType) {
                    buildType.setName(BuilderConstants.DEBUG);
                    buildType.setIsDebuggable(true);
                    buildType.setIsEmbedMicroApp(false);
                }
            });
            buildTypes.create(new Action<BuildType>() {
                @Override
                public void execute(BuildType buildType) {
                    buildType.setName(BuilderConstants.RELEASE);
                }
            });
        }

        @Model
        public List<ProductFlavorCombo> createProductFlavorCombo(
                @Path("android.productFlavors") ManagedSet<ProductFlavor> productFlavors) {
            // TODO: Create custom product flavor container to manually configure flavor dimensions.
            Set<String> flavorDimensionList = Sets.newHashSet();
            for (ProductFlavor flavor : productFlavors) {
                if (flavor.getDimension() != null) {
                    flavorDimensionList.add(flavor.getDimension());
                }
            }

            return ProductFlavorCombo.createCombinations(
                    Lists.newArrayList(flavorDimensionList),
                    Iterables.transform(productFlavors,
                            new Function<ProductFlavor, com.android.builder.model.ProductFlavor>() {
                                @Override
                                public com.android.builder.model.ProductFlavor apply(ProductFlavor productFlavor) {
                                    return new ProductFlavorAdaptor(productFlavor);
                                }
                            }));
        }

        @ComponentType
        public void defineComponentType(ComponentTypeBuilder<AndroidComponentSpec> builder) {
            builder.defaultImplementation(DefaultAndroidComponentSpec.class);
        }

        @Mutate
        public void createAndroidComponents(
                CollectionBuilder<AndroidComponentSpec> androidComponents) {
            androidComponents.create(COMPONENT_NAME);
        }

        @Model(ANDROID_COMPONENT_SPEC)
        public AndroidComponentSpec createAndroidComponentSpec(ComponentSpecContainer specs) {
            return (AndroidComponentSpec) specs.getByName(COMPONENT_NAME);
        }

        @Model
        public AndroidComponentModelSourceSet androidSources(ServiceRegistry serviceRegistry) {
            Instantiator instantiator = serviceRegistry.get(Instantiator.class);
            return new AndroidComponentModelSourceSet(instantiator);
        }

        /**
         * Create all source sets for each AndroidBinary.
         */
        @Mutate
        public void createVariantSourceSet(
                @Path("android.sources") final AndroidComponentModelSourceSet sources,
                @Path("android.buildTypes") final ManagedSet<BuildType> buildTypes,
                @Path("android.productFlavors") ManagedSet<ProductFlavor> flavors,
                List<ProductFlavorCombo> flavorGroups, ProjectSourceSet projectSourceSet,
                LanguageRegistry languageRegistry) {
            sources.setProjectSourceSet(projectSourceSet);
            for (LanguageRegistration languageRegistration : languageRegistry) {
                sources.registerLanguage(languageRegistration);
            }

            // Create main source set.
            sources.create("main");
            sources.create(ANDROID_TEST.getPrefix());
            sources.create(UNIT_TEST.getPrefix());

            for (BuildType buildType : buildTypes) {
                sources.maybeCreate(buildType.getName());

                for (ProductFlavorCombo group: flavorGroups) {
                    sources.maybeCreate(group.getName());
                    if (!group.getFlavorList().isEmpty()) {
                        sources.maybeCreate(
                                group.getName() + StringHelper.capitalize(buildType.getName()));
                    }

                }

            }
            if (flavorGroups.size() != flavors.size()) {
                // If flavorGroups and flavors are the same size, there is at most 1 flavor
                // dimension.  So we don't need to reconfigure the source sets for flavorGroups.
                for (ProductFlavor flavor: flavors) {
                    sources.maybeCreate(flavor.getName());
                }
            }
        }

        @Finalize
        public void setDefaultSrcDir(
                @Path("android.sources") AndroidComponentModelSourceSet sourceSet) {
            sourceSet.setDefaultSrcDir();
        }

        @BinaryType
        public void defineBinaryType(BinaryTypeBuilder<AndroidBinary> builder) {
            builder.defaultImplementation(DefaultAndroidBinary.class);
        }

        @ComponentBinaries
        public void createBinaries(
                final CollectionBuilder<AndroidBinary> binaries,
                @Path("android.buildTypes") ManagedSet<BuildType> buildTypes,
                List<ProductFlavorCombo> flavorCombos, AndroidComponentSpec spec) {
            if (flavorCombos.isEmpty()) {
                flavorCombos.add(new ProductFlavorCombo());
            }

            for (final BuildType buildType : buildTypes) {
                for (final ProductFlavorCombo flavorCombo : flavorCombos) {
                    binaries.create(getBinaryName(buildType, flavorCombo),
                            new Action<AndroidBinary>() {
                                @Override
                                public void execute(AndroidBinary androidBinary) {
                                    DefaultAndroidBinary binary = (DefaultAndroidBinary) androidBinary;
                                    binary.setBuildType(new BuildTypeAdaptor(buildType));
                                    binary.setProductFlavors(flavorCombo.getFlavorList());
                                }
                            });
                }
            }
        }

        @Model
        TaskInfoRegistry createTaskInfoRegistry() {
            return new TaskInfoRegistry();
        }

        @Mutate
        void executeUserTaskConfiguration(
                @NonNull final CollectionBuilder<Task> tasks,
                @NonNull final BinaryContainer binaries,
                @NonNull final TaskInfoRegistry taskInfoRegistry) {
            binaries.withType(AndroidBinary.class, new Action<AndroidBinary>() {
                @Override
                public void execute(AndroidBinary androidBinary) {
                    DefaultAndroidBinary binary = (DefaultAndroidBinary) androidBinary;
                    for (Map.Entry<TaskType, Closure> entry : binary.getTaskConfigClosures().entries()) {
                        TaskInfo taskInfo = taskInfoRegistry.get(entry.getKey());
                        String taskName = taskInfo.getPrefix()
                                + StringHelper.capitalize(binary.getFullName())
                                + taskInfo.getSuffix();
                        tasks.named(taskName, new ClosureBackedAction<Task>(entry.getValue()));
                    }
                }
            });
        }

        private static String getBinaryName(BuildType buildType, ProductFlavorCombo flavorCombo) {
            if (flavorCombo.getFlavorList().isEmpty()) {
                return buildType.getName();
            } else {
                return flavorCombo.getName() + StringHelper.capitalize(buildType.getName());
            }

        }
    }
}
