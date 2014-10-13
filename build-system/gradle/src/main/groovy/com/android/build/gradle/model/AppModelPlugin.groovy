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
import com.android.annotations.Nullable
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.AndroidSourceDirectorySet
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.BuildTypeData
import com.android.build.gradle.internal.ProductFlavorData
import com.android.build.gradle.internal.VariantManager
import com.android.build.gradle.internal.dsl.BuildTypeDsl
import com.android.build.gradle.internal.dsl.GroupableProductFlavorDsl
import com.android.build.gradle.internal.dsl.SigningConfigDsl
import com.android.build.gradle.internal.dsl.SigningConfigFactory
import com.android.build.gradle.internal.tasks.DependencyReportTask
import com.android.build.gradle.internal.tasks.PrepareSdkTask
import com.android.build.gradle.internal.tasks.SigningReportTask
import com.android.build.gradle.internal.variant.ApplicationVariantFactory
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.internal.variant.VariantFactory
import com.android.build.gradle.ndk.NdkExtension
import com.android.builder.core.BuilderConstants
import com.android.builder.core.DefaultBuildType
import com.android.builder.model.SigningConfig
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.language.base.FunctionalSourceSet
import org.gradle.language.base.LanguageSourceSet
import org.gradle.language.base.ProjectSourceSet
import org.gradle.language.base.internal.LanguageRegistration
import org.gradle.language.base.internal.LanguageRegistry
import org.gradle.language.base.internal.SourceTransformTaskConfig
import org.gradle.model.Model
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.BinarySpec
import org.gradle.platform.base.ComponentSpecContainer
import org.gradle.platform.base.TransformationFileType
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry

import javax.inject.Inject

import static com.android.builder.core.BuilderConstants.DEBUG

public class AppModelPlugin extends BasePlugin implements Plugin<Project> {
    @Inject
    protected AppModelPlugin(Instantiator instantiator, ToolingModelBuilderRegistry registry) {
        super(instantiator, registry);
    }

    @Override
    protected Class<? extends BaseExtension> getExtensionClass() {
        return AppExtension.class
    }

    @Override
    protected VariantFactory getVariantFactory() {
        return new ApplicationVariantFactory(this)
    }


    @Override
    void apply(Project project) {
        super.apply(project)
    }

    /**
     * Replace BasePlugin's apply method for component model.
     */
    @Override
    protected void doApply() {
        project.plugins.apply(AndroidComponentModelPlugin)

        // Add this plugin as an extension so that it can be accessed in model rules for now.
        // Eventually, we can refactor so that BasePlugin is not used extensively through our
        // codebase.
        project.extensions.add("androidPlugin", this);

        configureProject()

        project.plugins.apply(NdkComponentModelPlugin)

        // Setup Android's FunctionalSourceSet.
        project.getExtensions().getByType(LanguageRegistry.class).add(new AndroidSource());
        project.sources.create("main")
        project.sources.all {
                resources(AndroidLanguageSourceSet)
                java(AndroidLanguageSourceSet)
                manifest(AndroidLanguageSourceSet)
                res(AndroidLanguageSourceSet)
                assets(AndroidLanguageSourceSet)
                aidl(AndroidLanguageSourceSet)
                renderscript(AndroidLanguageSourceSet)
                jni(AndroidLanguageSourceSet)
                jniLibs(AndroidLanguageSourceSet)
        }

        // create the config to link a wear apk.
        project.configurations.create(ApplicationVariantFactory.CONFIG_WEAR_APP)
    }

    @RuleSource
    static class Rules {
        @Model
        BasePlugin androidPlugin(ExtensionContainer extensions) {
            return extensions.getByType(AppModelPlugin)
        }

        @Model("android")
        AppExtension androidapp(
                ServiceRegistry serviceRegistry,
                NamedDomainObjectContainer<DefaultBuildType> buildTypeContainer,
                NamedDomainObjectContainer<GroupableProductFlavorDsl> productFlavorContainer,
                NamedDomainObjectContainer<SigningConfig> signingConfigContainer,
                BasePlugin plugin) {
            println "appextension"
            Instantiator instantiator = serviceRegistry.get(Instantiator.class);
            Project project = plugin.getProject()

            AppExtension extension = instantiator.newInstance(AppExtension,
                    plugin, (ProjectInternal) project, instantiator,
                    buildTypeContainer, productFlavorContainer, signingConfigContainer, false)
            plugin.setBaseExtension(extension)

            // Android component model always use new plugin.
            extension.useNewNativePlugin = true

            return extension
        }

        @Model("android.signingConfig")
        NamedDomainObjectContainer<SigningConfig> signingConfig(ServiceRegistry serviceRegistry,
                BasePlugin plugin) {
            Instantiator instantiator = serviceRegistry.get(Instantiator.class);
            Project project = plugin.getProject()
            def signingConfigContainer = project.container(SigningConfig,
                new SigningConfigFactory(instantiator))
            signingConfigContainer.create(DEBUG)
            signingConfigContainer.whenObjectRemoved {
                throw new UnsupportedOperationException("Removing signingConfigs is not supported.")
            }
            return signingConfigContainer
        }

        @Mutate
        void closeProjectSourceSet(ProjectSourceSet sources) {
        }

        @Mutate
        void createAndroidComponents(
                ComponentSpecContainer specContainer,
                AppExtension androidExtension,
                NamedDomainObjectContainer<DefaultBuildType> buildTypeContainer,
                NamedDomainObjectContainer<GroupableProductFlavorDsl> productFlavorContainer,
                NamedDomainObjectContainer<SigningConfig> signingConfigContainer,
                ProjectSourceSet sources,
                BasePlugin plugin) {
            VariantManager variantManager = new VariantManager(
                    plugin.project,
                    plugin,
                    androidExtension,
                    new ApplicationVariantFactory(plugin))

            signingConfigContainer.all { SigningConfig signingConfig ->
                variantManager.addSigningConfig((SigningConfigDsl) signingConfig)
            }
            buildTypeContainer.all { DefaultBuildType buildType ->
                variantManager.addBuildType((BuildTypeDsl) buildType)
            }
            productFlavorContainer.all { GroupableProductFlavorDsl productFlavor ->
                variantManager.addProductFlavor(productFlavor)
            }
            plugin.variantManager = variantManager;

            specContainer.withType(AndroidComponentSpec) { DefaultAndroidComponentSpec spec ->
                spec.extension = androidExtension
                spec.variantManager = variantManager
                spec.signingOverride = plugin.getSigningOverride()

                applyProjectSourceSet(spec, sources, plugin)
            }
        }

        @Mutate
        void createAndroidTasks(
                TaskContainer tasks,
                BinaryContainer binaries,
                ComponentSpecContainer specContainer,
                BasePlugin plugin) {
            DefaultAndroidComponentSpec spec = (DefaultAndroidComponentSpec)specContainer.withType(AndroidComponentSpec)[0]
            VariantManager variantManager = spec.variantManager

            // Create lifecycle tasks.
            Task uninstallAll = tasks.create("uninstallAll")
            uninstallAll.description = "Uninstall all applications."
            uninstallAll.group = INSTALL_GROUP

            Task deviceCheck = tasks.create("deviceCheck")
            deviceCheck.description = "Runs all device checks using Device Providers and Test Servers."
            deviceCheck.group = JavaBasePlugin.VERIFICATION_GROUP

            Task connectedCheck = tasks.create("connectedCheck")
            connectedCheck.description = "Runs all device checks on currently connected devices."
            connectedCheck.group = JavaBasePlugin.VERIFICATION_GROUP

            Task mainPreBuild = tasks.create("preBuild", PrepareSdkTask)
            mainPreBuild.plugin = plugin

            plugin.uninstallAll = uninstallAll
            plugin.deviceCheck = deviceCheck
            plugin.connectedCheck = connectedCheck
            plugin.mainPreBuild = mainPreBuild

            // setup SDK repositories.
            for (File file : plugin.sdkHandler.sdkLoader.repositories) {
                plugin.project.repositories.maven {
                    url = file.toURI()
                }
            }

            plugin.createLintCompileTask();

            // Create tasks for each binaries.
            if (!variantManager.productFlavors.isEmpty()) {
                // there'll be more than one test app, so we need a top level assembleTest
                Task assembleTest = tasks.create("assembleTest");
                assembleTest.setGroup(org.gradle.api.plugins.BasePlugin.BUILD_GROUP);
                assembleTest.setDescription("Assembles all the Test applications");
                plugin.setAssembleTest(assembleTest);
            }

            binaries.withType(AndroidBinary) { DefaultAndroidBinary binary ->
                BaseVariantData variantData = variantManager.createBaseVariantData(
                        binary.buildType,
                        binary.productFlavors,
                        plugin.signingOverride
                )
                variantManager.getVariantDataList().add(variantData);
                variantManager.createTasksForVariantData(tasks, variantData)
            }

            // create the lint tasks.
            plugin.createLintTasks();

            // create the test tasks.
            plugin.createCheckTasks(!variantManager.productFlavors.isEmpty(), false /*isLibrary*/);

            // Create the variant API objects after the tasks have been created!
            variantManager.createApiObjects();

            // Create report tasks.
            def dependencyReportTask = tasks.create("androidDependencies", DependencyReportTask)
            dependencyReportTask.setDescription("Displays the Android dependencies of the project")
            dependencyReportTask.setVariants(variantManager.variantDataList)
            dependencyReportTask.setGroup("Android")

            def signingReportTask = tasks.create("signingReport", SigningReportTask)
            signingReportTask.setDescription("Displays the signing info for each variant")
            signingReportTask.setVariants(variantManager.variantDataList)
            signingReportTask.setGroup("Android")
        }


        private static void applyProjectSourceSet(
                AndroidComponentSpec androidSpec,
                ProjectSourceSet sources,
                BasePlugin plugin) {
            DefaultAndroidComponentSpec spec = (DefaultAndroidComponentSpec)androidSpec
            VariantManager variantManager = spec.variantManager
            for (FunctionalSourceSet source : sources) {
                String name = source.getName();
                AndroidSourceSet androidSource = (
                        name.equals(BuilderConstants.MAIN)
                                ? plugin.mainSourceSet
                                : (name.equals(BuilderConstants.ANDROID_TEST)
                                        ? plugin.testSourceSet
                                        : findAndroidSourceSet(variantManager, name)))

                if (androidSource == null) {
                    continue;
                }

                convertSourceSet(androidSource.getResources(), source.findByName("resource")?.getSource())
                convertSourceSet(androidSource.getJava(), source.findByName("java")?.getSource())
                convertSourceSet(androidSource.getRes(), source.findByName("res")?.getSource())
                convertSourceSet(androidSource.getAssets(), source.findByName("assets")?.getSource())
                convertSourceSet(androidSource.getAidl(), source.findByName("aidl")?.getSource())
                convertSourceSet(androidSource.getRenderscript(), source.findByName("renderscript")?.getSource())
                convertSourceSet(androidSource.getJni(), source.findByName("jni")?.getSource())
                convertSourceSet(androidSource.getJniLibs(), source.findByName("jniLibs")?.getSource())
            }
        }

        private static convertSourceSet(
                AndroidSourceDirectorySet androidDir,
                @Nullable SourceDirectorySet dir) {
            if (dir == null) {
                return
            }
            androidDir.setSrcDirs(dir.getSrcDirs())
            androidDir.include(dir.getIncludes())
            androidDir.exclude(dir.getExcludes())
        }

        @Nullable
        private static AndroidSourceSet findAndroidSourceSet(
                VariantManager variantManager,
                String name) {
            BuildTypeData buildTypeData = variantManager.getBuildTypes().get(name)
            if (buildTypeData != null) {
                return buildTypeData.getSourceSet();
            }

            boolean isTest = name.startsWith(BuilderConstants.ANDROID_TEST)
            name = name.replaceFirst(BuilderConstants.ANDROID_TEST, "")
            ProductFlavorData productFlavorData = variantManager.getProductFlavors().get(name)
            if (productFlavorData != null) {
                return isTest ? productFlavorData.getTestSourceSet() : productFlavorData.getSourceSet();
            }
            return null;
        }

    }

    /**
     * Default Android LanguageRegistration.
     *
     * Allows default LanguageSourceSet to be create until specialized LanguageRegistration is
     * created.
     */
    private static class AndroidSource implements LanguageRegistration<AndroidLanguageSourceSet> {
        public String getName() {
            return "main";
        }

        public Class<AndroidLanguageSourceSet> getSourceSetType() {
            return AndroidLanguageSourceSet.class;
        }

        public Class<? extends AndroidLanguageSourceSet> getSourceSetImplementation() {
            return AndroidLanguageSourceSet.class;
        }

        public Map<String, Class<?>> getBinaryTools() {
            return Collections.emptyMap();
        }

        public Class<? extends TransformationFileType> getOutputType() {
            return null;
        }

        public SourceTransformTaskConfig getTransformTask() {
            return new SourceTransformTaskConfig() {
                public String getTaskPrefix() {
                    return "process";
                }

                public Class<? extends DefaultTask> getTaskType() {
                    return DefaultTask;
                }

                public void configureTask(Task task, BinarySpec binary, LanguageSourceSet sourceSet) {
                }
            };
        }

        public boolean applyToBinary(BinarySpec binary) {
            return binary instanceof AndroidBinary
        }
    }
}
