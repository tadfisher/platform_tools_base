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

import com.android.build.gradle.internal.tasks.DependencyReportTask
import com.android.build.gradle.internal.tasks.SigningReportTask
import com.android.build.gradle.internal.variant.BaseVariantData
import org.gradle.api.NamedDomainObjectContainer
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.internal.variant.ApplicationVariantFactory
import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectIdentifier
import org.gradle.api.plugins.JavaPlugin
import org.gradle.language.base.ProjectSourceSet
import org.gradle.internal.service.ServiceRegistry
import com.android.build.gradle.internal.BadPluginException
import com.android.build.gradle.internal.LibraryCache
import com.android.build.gradle.internal.SdkHandler
import com.android.build.gradle.internal.VariantManager
import com.android.build.gradle.internal.api.AndroidLanguageSourceSet
import com.android.build.gradle.internal.coverage.JacocoPlugin
import com.android.build.gradle.internal.dsl.BuildTypeDsl
import com.android.build.gradle.internal.dsl.BuildTypeFactory
import com.android.build.gradle.internal.dsl.GroupableProductFlavorDsl
import com.android.build.gradle.internal.dsl.GroupableProductFlavorFactory
import com.android.build.gradle.internal.dsl.SigningConfigDsl
import com.android.build.gradle.internal.dsl.SigningConfigFactory
import com.android.build.gradle.internal.model.ModelBuilder
import com.android.build.gradle.internal.tasks.PrepareSdkTask
import com.android.build.gradle.internal.variant.VariantFactory
import com.android.build.gradle.ndk.NdkPlugin
import com.android.build.gradle.tasks.PreDex
import com.android.builder.core.AndroidBuilder
import com.android.builder.core.DefaultBuildType
import com.android.builder.internal.compiler.PreDexCache
import com.android.builder.model.SigningConfig
import com.android.builder.png.PngProcessor
import com.android.ide.common.internal.ExecutorSingleton
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.internal.reflect.Instantiator
import org.gradle.model.Finalize
import org.gradle.model.Model
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.model.collection.CollectionBuilder
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.BinaryType
import org.gradle.platform.base.BinaryTypeBuilder
import org.gradle.platform.base.ComponentSpec
import org.gradle.platform.base.ComponentSpecContainer
import org.gradle.platform.base.ComponentType
import org.gradle.platform.base.ComponentTypeBuilder
import org.gradle.platform.base.component.BaseComponentSpec
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.platform.base.TransformationFileType;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.internal.LanguageRegistration;
import org.gradle.language.base.internal.LanguageRegistry;
import org.gradle.language.base.internal.SourceTransformTaskConfig;
import org.gradle.platform.base.BinarySpec

import static com.android.builder.core.BuilderConstants.DEBUG
import static com.android.builder.core.BuilderConstants.RELEASE
import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES

import javax.inject.Inject

public class AppModelPlugin extends BasePlugin implements Plugin<Project> {
    static AppModelPlugin instance;
    // FIXME: HACK!!! I don't know how to find the plugin in RuleSource.  The proper solution is to
    // refactor Android plugin so that our components is not reliant on the plugin, but that will
    // take a while.
    static AppModelPlugin getPlugin() {
        return instance
    }

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
    public void apply(Project project) {
        this.project = project
        instance = this

        //sdkHandler = new SdkHandler(project.rootDir, logger)
        androidBuilder = new AndroidBuilder(
                project == project.rootProject ? project.name : project.path,
                "Android Gradle", logger, verbose)

        project.apply plugin: JavaBasePlugin

        project.apply plugin: JacocoPlugin
        jacocoPlugin = project.plugins.getPlugin(JacocoPlugin)

        // Register a builder for the custom tooling model
        registry.register(new ModelBuilder());

        if (project.plugins.hasPlugin(NdkPlugin.class)) {
            throw new BadPluginException(
                    "Cannot apply Android native plugin before the Android plugin.")
        }
        project.apply plugin: NdkPlugin

        project.tasks.assemble.description =
                "Assembles all variants of all applications and secondary packages."

        // Under experimentation.
        project.getExtensions().getByType(LanguageRegistry.class).add(new AndroidSource());
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


        // Move this to model rules.
        project.afterEvaluate {
            // get current plugins and look for the default Java plugin.
            if (project.plugins.hasPlugin(JavaPlugin.class)) {
                throw new BadPluginException(
                        "The 'java' plugin has been applied, but it is not compatible with the Android plugins.")
            }
        }

        if (lintVital != null) {
            project.gradle.taskGraph.whenReady { taskGraph ->
                if (taskGraph.hasTask(lintAll)) {
                    lintVital.setEnabled(false)
                }
            }
        }

        // call back on execution. This is called after the whole build is done (not
        // after the current project is done).
        // This is will be called for each (android) projects though, so this should support
        // being called 2+ times.

        project.gradle.buildFinished {
            ExecutorSingleton.shutdown()
            PngProcessor.clearCache()
            sdkHandler.unload()
            PreDexCache.getCache().clear(
                    project.rootProject.file(
                            "${project.rootProject.buildDir}/${FD_INTERMEDIATES}/dex-cache/cache.xml"),
                    logger)
            LibraryCache.getCache().unload()
        }

        project.gradle.taskGraph.whenReady { taskGraph ->
            for (Task task : taskGraph.allTasks) {
                if (task instanceof PreDex) {
                    PreDexCache.getCache().load(
                            project.rootProject.file(
                                    "${project.rootProject.buildDir}/${FD_INTERMEDIATES}/dex-cache/cache.xml"))
                    break;
                }
            }
        }

        // create the config to link a wear apk.
        project.configurations.create(ApplicationVariantFactory.CONFIG_WEAR_APP)
    }

    @RuleSource
    static class Rules {
        @Model
        AppModelPlugin androidPlugin() {
            // FIXME: Need a better way to get the plugin.
            return AppModelPlugin.getPlugin()
        }

        // It may be a better idea to create extension as we normally do and just use component
        // model for creating tasks.
        @Model("android")
        AppExtension androidapp(
                ServiceRegistry serviceRegistry,
                NamedDomainObjectContainer<DefaultBuildType> buildTypeContainer,
                NamedDomainObjectContainer<GroupableProductFlavorDsl> productFlavorContainer,
                NamedDomainObjectContainer<SigningConfig> signingConfigContainer,
                AppModelPlugin plugin) {
            Instantiator instantiator = serviceRegistry.get(Instantiator.class);
            Project project = plugin.getProject()

            AppExtension extension = new AppExtension(
                    plugin, (ProjectInternal) project, instantiator,
                    buildTypeContainer, productFlavorContainer, signingConfigContainer, false)
            plugin.setBaseExtension(extension)

            def ndkPlugin = project.plugins.getPlugin(NdkPlugin)

            extension.setNdkExtension(ndkPlugin.getNdkExtension())

            return extension
        }

        @Model("android.buildTypes")
        NamedDomainObjectContainer<DefaultBuildType> buildTypes(ServiceRegistry serviceRegistry,
                AppModelPlugin plugin) {
            Instantiator instantiator = serviceRegistry.get(Instantiator.class);
            Project project = plugin.getProject()
            def buildTypeContainer = project.container(DefaultBuildType,
                new BuildTypeFactory(instantiator,  project))

            // create default Objects, signingConfig first as its used by the BuildTypes.
            buildTypeContainer.create(DEBUG)
            buildTypeContainer.create(RELEASE)

            buildTypeContainer.whenObjectRemoved {
                throw new UnsupportedOperationException("Removing build types is not supported.")
            }
            return buildTypeContainer
        }

        @Model("android.productFlavors")
        NamedDomainObjectContainer<GroupableProductFlavorDsl> productFlavors(
                ServiceRegistry serviceRegistry,
                AppModelPlugin plugin) {
            Instantiator instantiator = serviceRegistry.get(Instantiator.class);
            Project project = plugin.getProject()
            def productFlavorContainer = project.container(GroupableProductFlavorDsl,
                new GroupableProductFlavorFactory(instantiator, project, project.getLogger()))

            productFlavorContainer.whenObjectRemoved {
                throw new UnsupportedOperationException("Removing product flavors is not supported.")
            }

            return productFlavorContainer
        }

        @Model("android.signingConfig")
        NamedDomainObjectContainer<SigningConfig> signingConfig(ServiceRegistry serviceRegistry,
                AppModelPlugin plugin) {
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

        @Model
        VariantManager createVariantManager(
                //AppExtension extension,
                AppModelPlugin plugin) {
            // AppExtension should part of the input, but this rule doesn't get activated for some
            // reason if I do that.
            AppExtension extension = (AppExtension)(plugin.getExtension());

            VariantManager variantManager = new VariantManager(
                    plugin.project,
                    plugin,
                    extension,
                    new ApplicationVariantFactory(plugin))

            extension.getSigningConfigs().all { SigningConfig signingConfig ->
                variantManager.addSigningConfig((SigningConfigDsl) signingConfig)
            }
            extension.getBuildTypes().all { DefaultBuildType buildType ->
                variantManager.addBuildType((BuildTypeDsl) buildType)
            }
            extension.getProductFlavors().all { GroupableProductFlavorDsl productFlavor ->
                variantManager.addProductFlavor(productFlavor)
            }
            plugin.variantManager = variantManager;

            return variantManager
        }

        @Model
        SdkHandler defineSdkHandler(
                ProjectIdentifier projectId,
                AppModelPlugin androidPlugin) {
            ProjectIdentifier rootProjectId = projectId;
            while (projectId.getParentIdentifier() != null) {
                rootProjectId = projectId.getParentIdentifier();
            }
            androidPlugin.sdkHandler = new SdkHandler(rootProjectId.getProjectDir());
            return androidPlugin.sdkHandler
        }

        @ComponentType
        void defineModelType(ComponentTypeBuilder<AndroidComponentSpec> builder) {
            builder.defaultImplementation(DefaultAndroidComponentSpec)
        }

        @Mutate
        void createAndroidComponents(
                CollectionBuilder<AndroidComponentSpec> androidComponents,
                AppExtension androidExtension,
                SdkHandler sdkHandler,
                VariantManager variantManager) {
            androidComponents.create("android") { AndroidComponentSpecInternal spec ->
                spec.extension = androidExtension
                spec.sdkHandler = sdkHandler
                spec.variantManager = variantManager
            }
        }

        @Mutate
        void applyDefaultSourceConventions(ProjectSourceSet sources, AppExtension extension) {
            sources.all { def androidSourceSet ->
                androidSourceSet.withType(AndroidLanguageSourceSet) { LanguageSourceSet sourceSet ->
                    source {
                        srcDir "src/" + androidSourceSet + "/" + sourceSet.name
                    }
                }
            }
        }

        @BinaryType
        void defineBinaryType(BinaryTypeBuilder<AndroidBinary> builder) {
            builder.defaultImplementation(AndroidBinary)
        }

        // TODO: Convert to @ComponentBinaries when it is implemented.
        @Finalize
        void createBinaries(
                BinaryContainer binaries,
                ComponentSpecContainer specContainer,
                AppModelPlugin plugin) {
            AndroidComponentSpec componentSpec = specContainer.withType(AndroidComponentSpec)[0]
            DefaultAndroidComponentSpec spec = (DefaultAndroidComponentSpec) componentSpec

            VariantManager variantManager = spec.getVariantManager()
            variantManager.createBaseVariantData(plugin.getSigningOverride())

            for (BaseVariantData variantData : variantManager.getVariantDataList()) {
                binaries.create("${variantData.getName()}Binary", AndroidBinary ) { AndroidBinary binary ->
                    binary.setVariantData(variantData);
                }
            }
        }

        @Finalize
        void createAndroidTask(
                TaskContainer tasks,
                BinaryContainer binaries,
                VariantManager variantManager,
                AppModelPlugin plugin) {
            // setup SDK repositories.
            for (File file : plugin.sdkHandler.sdkLoader.repositories) {
                plugin.project.repositories.maven {
                    url = file.toURI()
                }
            }

            binaries.withType(AndroidBinary) { AndroidBinary binary ->
                variantManager.createTasksForVariantData(tasks, binary.getVariantData())
            }

            def dependencyReportTask = tasks.create("androidDependencies", DependencyReportTask)
            dependencyReportTask.setDescription("Displays the Android dependencies of the project")
            dependencyReportTask.setVariants(variantManager.variantDataList)
            dependencyReportTask.setGroup("Android")

            def signingReportTask = tasks.create("signingReport", SigningReportTask)
            signingReportTask.setDescription("Displays the signing info for each variant")
            signingReportTask.setVariants(variantManager.variantDataList)
            signingReportTask.setGroup("Android")

        }

        @Mutate
        void createLifeCycleTasks(TaskContainer tasks, AppModelPlugin plugin) {
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
        }
    }

    ////////////////////////////////////////
    // Experiments with LanguageRegistration
    private static class AndroidSource implements LanguageRegistration<AndroidLanguageSourceSet> {
        public String getName() {
            return "android";
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
                    return null;
                }

                public void configureTask(Task task, BinarySpec binary, LanguageSourceSet sourceSet) {
                }
            };
        }

        public boolean applyToBinary(BinarySpec binary) {
            return false;
        }
    }

}
