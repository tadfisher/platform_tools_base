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

package com.android.build.gradle.internal

import com.android.SdkConstants
import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.OutputFile
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.coverage.JacocoInstrumentTask
import com.android.build.gradle.internal.coverage.JacocoPlugin
import com.android.build.gradle.internal.coverage.JacocoReportTask
import com.android.build.gradle.internal.dependency.LibraryDependencyImpl
import com.android.build.gradle.internal.dependency.ManifestDependencyImpl
import com.android.build.gradle.internal.dependency.SymbolFileProviderImpl
import com.android.build.gradle.internal.dependency.VariantDependencies
import com.android.build.gradle.internal.dsl.AbiSplitOptions
import com.android.build.gradle.internal.dsl.SigningConfig
import com.android.build.gradle.internal.publishing.ApkPublishArtifact
import com.android.build.gradle.internal.publishing.MappingPublishArtifact
import com.android.build.gradle.internal.publishing.MetadataPublishArtifact
import com.android.build.gradle.internal.scope.AndroidTask
import com.android.build.gradle.internal.scope.AndroidTaskRegistry
import com.android.build.gradle.internal.scope.GlobalScope
import com.android.build.gradle.internal.scope.VariantOutputScope
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.AndroidReportTask
import com.android.build.gradle.internal.tasks.CheckManifest
import com.android.build.gradle.internal.tasks.DependencyReportTask
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestLibraryTask
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask
import com.android.build.gradle.internal.tasks.FileSupplier
import com.android.build.gradle.internal.tasks.GenerateApkDataTask
import com.android.build.gradle.internal.tasks.InstallVariantTask
import com.android.build.gradle.internal.tasks.MockableAndroidJarTask
import com.android.build.gradle.internal.tasks.PrepareDependenciesTask
import com.android.build.gradle.internal.tasks.SigningReportTask
import com.android.build.gradle.internal.tasks.SourceSetsTask
import com.android.build.gradle.internal.tasks.TestServerTask
import com.android.build.gradle.internal.tasks.UninstallTask
import com.android.build.gradle.internal.tasks.multidex.CreateMainDexList
import com.android.build.gradle.internal.tasks.multidex.CreateManifestKeepList
import com.android.build.gradle.internal.tasks.multidex.JarMergingTask
import com.android.build.gradle.internal.tasks.multidex.RetraceMainDexList
import com.android.build.gradle.internal.test.TestDataImpl
import com.android.build.gradle.internal.test.report.ReportType
import com.android.build.gradle.internal.variant.ApkVariantData
import com.android.build.gradle.internal.variant.ApkVariantOutputData
import com.android.build.gradle.internal.variant.ApplicationVariantData
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.internal.variant.BaseVariantOutputData
import com.android.build.gradle.internal.variant.LibraryVariantData
import com.android.build.gradle.internal.variant.TestVariantData
import com.android.build.gradle.tasks.AidlCompile
import com.android.build.gradle.tasks.AndroidJarTask
import com.android.build.gradle.tasks.AndroidProGuardTask
import com.android.build.gradle.tasks.CompatibleScreensManifest
import com.android.build.gradle.tasks.Dex
import com.android.build.gradle.tasks.GenerateBuildConfig
import com.android.build.gradle.tasks.GeneratePngsFromVectorDrawablesTask
import com.android.build.gradle.tasks.GenerateResValues
import com.android.build.gradle.tasks.GenerateSplitAbiRes
import com.android.build.gradle.tasks.JackTask
import com.android.build.gradle.tasks.JillTask
import com.android.build.gradle.tasks.Lint
import com.android.build.gradle.tasks.MergeAssets
import com.android.build.gradle.tasks.MergeManifests
import com.android.build.gradle.tasks.MergeResources
import com.android.build.gradle.tasks.NdkCompile
import com.android.build.gradle.tasks.PackageApplication
import com.android.build.gradle.tasks.PackageSplitAbi
import com.android.build.gradle.tasks.PackageSplitRes
import com.android.build.gradle.tasks.PreCompilationVerificationTask
import com.android.build.gradle.tasks.PreDex
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.build.gradle.tasks.ProcessManifest
import com.android.build.gradle.tasks.ProcessTestManifest
import com.android.build.gradle.tasks.RenderscriptCompile
import com.android.build.gradle.tasks.ShrinkResources
import com.android.build.gradle.tasks.SplitZipAlign
import com.android.build.gradle.tasks.ZipAlign
import com.android.build.gradle.tasks.factory.JavaCompileConfigAction
import com.android.build.gradle.tasks.factory.ProGuardTaskConfigAction
import com.android.builder.core.AndroidBuilder
import com.android.builder.core.VariantConfiguration
import com.android.builder.core.VariantType
import com.android.builder.dependency.LibraryDependency
import com.android.builder.internal.testing.SimpleTestCallable
import com.android.builder.model.ApiVersion
import com.android.builder.model.ProductFlavor
import com.android.builder.model.SourceProvider
import com.android.builder.png.VectorDrawableRenderer
import com.android.builder.testing.ConnectedDeviceProvider
import com.android.builder.testing.TestData
import com.android.builder.testing.api.DeviceProvider
import com.android.builder.testing.api.TestServer
import com.android.resources.Density
import com.android.sdklib.AndroidTargetHash
import com.android.sdklib.BuildToolInfo
import com.android.sdklib.IAndroidTarget
import com.android.sdklib.SdkVersionInfo
import com.google.common.base.CharMatcher
import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.reporting.ConfigurableReport
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestTaskReports
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import proguard.gradle.ProGuardTask

import static com.android.SdkConstants.FN_ANDROID_MANIFEST_XML
import static com.android.builder.core.BuilderConstants.CONNECTED
import static com.android.builder.core.BuilderConstants.DEVICE
import static com.android.builder.core.BuilderConstants.FD_ANDROID_RESULTS
import static com.android.builder.core.BuilderConstants.FD_ANDROID_TESTS
import static com.android.builder.core.BuilderConstants.FD_FLAVORS
import static com.android.builder.core.BuilderConstants.FD_FLAVORS_ALL
import static com.android.builder.core.BuilderConstants.FD_REPORTS
import static com.android.builder.core.VariantType.ANDROID_TEST
import static com.android.builder.core.VariantType.DEFAULT
import static com.android.builder.core.VariantType.UNIT_TEST
import static com.android.builder.model.AndroidProject.FD_GENERATED
import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES
import static com.android.builder.model.AndroidProject.FD_OUTPUTS
import static com.android.builder.model.AndroidProject.PROPERTY_APK_LOCATION
import static com.android.sdklib.BuildToolInfo.PathId.ZIP_ALIGN

/**
 * Manages tasks creation.
 */
@CompileStatic
abstract class TaskManager {

    public static final String FILE_JACOCO_AGENT = 'jacocoagent.jar'

    public static final String DEFAULT_PROGUARD_CONFIG_FILE = 'proguard-android.txt'

    public final static String DIR_BUNDLES = "bundles";

    public static final String INSTALL_GROUP = "Install"

    public static final String BUILD_GROUP = BasePlugin.BUILD_GROUP

    public static final String ANDROID_GROUP = "Android"

    protected Project project

    protected AndroidBuilder androidBuilder

    private DependencyManager dependencyManager

    protected SdkHandler sdkHandler

    protected BaseExtension extension

    protected ToolingModelBuilderRegistry toolingRegistry

    private final GlobalScope globalScope

    private AndroidTaskRegistry androidTasks = new AndroidTaskRegistry();

    private Logger logger

    protected boolean isNdkTaskNeeded = true

    // Task names
    // TODO: Convert to AndroidTask.
    private static final String MAIN_PREBUILD = "preBuild"

    private static final String UNINSTALL_ALL = "uninstallAll"

    private static final String DEVICE_CHECK = "deviceCheck"

    protected static final String CONNECTED_CHECK = "connectedCheck"

    private static final String ASSEMBLE_ANDROID_TEST = "assembleAndroidTest"

    private static final String SOURCE_SETS = "sourceSets"

    private static final String LINT = "lint"

    // Tasks
    private Copy jacocoAgentTask

    public Task lintCompile

    public MockableAndroidJarTask createMockableJar

    public TaskManager(
            Project project,
            AndroidBuilder androidBuilder,
            BaseExtension extension,
            SdkHandler sdkHandler,
            DependencyManager dependencyManager,
            ToolingModelBuilderRegistry toolingRegistry) {
        this.project = project
        this.androidBuilder = androidBuilder
        this.sdkHandler = sdkHandler
        this.extension = extension
        this.toolingRegistry = toolingRegistry
        this.dependencyManager = dependencyManager
        logger = Logging.getLogger(this.class)

        globalScope = new GlobalScope(
                project,
                androidBuilder,
                getArchivesBaseName(project),
                extension,
                sdkHandler);
    }

    private boolean isVerbose() {
        return project.logger.isEnabled(LogLevel.INFO)
    }

    private boolean isDebugLog() {
        return project.logger.isEnabled(LogLevel.DEBUG)
    }

    /**
     * Creates the tasks for a given BaseVariantData.
     * @param variantData the non-null BaseVariantData.
     * @param assembleTask an optional assembleTask to be used. If null, a new one is created.
     */
    abstract public void createTasksForVariantData(
            @NonNull TaskFactory tasks,
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData)

    /**
     * Returns a collection of buildables that creates native object.
     *
     * A buildable is considered to be any object that can be used as the argument to
     * Task.dependsOn.  This could be a Task or a BuildableModelElement (e.g. BinarySpec).
     */
    protected Collection<Object> getNdkBuildable(BaseVariantData variantData) {
        return Collections.singleton(variantData.ndkCompileTask)
    }

    /**
     * Returns the directories of the NDK buildables.
     */
    protected Collection<File> getNdkOutputDirectories(BaseVariantData variantData) {
        return Collections.singleton(variantData.ndkCompileTask.soFolder)
    }

    private BaseExtension getExtension() {
        return extension
    }

    public void resolveDependencies(
            @NonNull VariantDependencies variantDeps,
            @Nullable VariantDependencies testedVariantDeps) {
        dependencyManager.resolveDependencies(variantDeps, testedVariantDeps)
    }

    /**
     * Create tasks before the evaluation (on plugin apply). This is useful for tasks that
     * could be referenced by custom build logic.
     */
    public void createTasksBeforeEvaluate(TaskFactory tasks) {
        tasks.create(UNINSTALL_ALL) {
            it.description = "Uninstall all applications."
            it.group = INSTALL_GROUP
        }

        tasks.create(DEVICE_CHECK) {
            it.description = "Runs all device checks using Device Providers and Test Servers."
            it.group = JavaBasePlugin.VERIFICATION_GROUP
        }

        tasks.create(CONNECTED_CHECK) {
            it.description = "Runs all device checks on currently connected devices."
            it.group = JavaBasePlugin.VERIFICATION_GROUP
        }

        tasks.create(MAIN_PREBUILD)

        tasks.create(SOURCE_SETS, SourceSetsTask) {
            it.description = "Prints out all the source sets defined in this project."
            it.group = ANDROID_GROUP
        }

        tasks.create(ASSEMBLE_ANDROID_TEST) {
            it.setGroup(BasePlugin.BUILD_GROUP);
            it.setDescription("Assembles all the Test applications.");
        }

        tasks.create(LINT, Lint) {
            it.description = "Runs lint on all variants."
            it.group = JavaBasePlugin.VERIFICATION_GROUP
            it.setLintOptions(getExtension().lintOptions)
            it.setSdkHome(sdkHandler.getSdkFolder())
            it.setToolingRegistry(toolingRegistry)
        }
        tasks.named(JavaBasePlugin.CHECK_TASK_NAME) {
            it.dependsOn LINT
        }
    }

    public void createMockableJarTask() {
        createMockableJar = project.tasks.create("mockableAndroidJar", MockableAndroidJarTask)
        createMockableJar.group = BUILD_GROUP
        createMockableJar.description = "Creates a version of android.jar that's suitable for unit tests."

        conventionMapping(createMockableJar).map("androidJar") {
            new File(androidBuilder.target.getPath(IAndroidTarget.ANDROID_JAR))
        }

        CharMatcher safeCharacters = CharMatcher.JAVA_LETTER_OR_DIGIT.or(CharMatcher.anyOf('-.'))
        String sdkName = safeCharacters.negate().replaceFrom(extension.compileSdkVersion, '-')

        conventionMapping(createMockableJar).map("outputFile") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/mockable-${sdkName}.jar")
        }

        conventionMapping(createMockableJar).map("returnDefaultValues") {
            extension.testOptions.unitTests.returnDefaultValues
        }
    }

    public void createMergeAppManifestsTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {

        VariantConfiguration config = variantData.variantConfiguration
        ProductFlavor mergedFlavor = config.mergedFlavor

        ApplicationVariantData appVariantData = variantData as ApplicationVariantData
        Set<String> screenSizes = appVariantData.getCompatibleScreens()

        // loop on all outputs. The only difference will be the name of the task, and location
        // of the generated manifest
        for (BaseVariantOutputData vod : variantData.outputs) {
            final CompatibleScreensManifest csmTask =
                    (vod.getMainOutputFile().getFilter(OutputFile.DENSITY) != null
                            && !screenSizes.isEmpty()) ?
                            createCompatibleScreensManifest(vod, screenSizes) :
                            null

            // create final var inside the loop to ensure the closures will work.
            final BaseVariantOutputData variantOutputData = vod

            String outputName = variantOutputData.fullName.capitalize()
            String outputDirName = variantOutputData.dirName

            def processManifestTask = project.tasks.create(
                    "process${outputName}Manifest",
                    MergeManifests)

            variantOutputData.manifestProcessorTask = processManifestTask

            processManifestTask.androidBuilder = androidBuilder

            processManifestTask.dependsOn variantData.prepareDependenciesTask
            if (variantData.generateApkDataTask != null) {
                processManifestTask.dependsOn variantData.generateApkDataTask
            }
            if (csmTask != null) {
                processManifestTask.dependsOn csmTask
            }

            processManifestTask.variantConfiguration = config
            if (variantOutputData instanceof ApkVariantOutputData) {
                processManifestTask.variantOutputData = variantOutputData as ApkVariantOutputData
            }

            conventionMapping(processManifestTask).map("libraries") {
                List<ManifestDependencyImpl> manifests =
                        getManifestDependencies(config.directLibraries)

                if (variantData.generateApkDataTask != null &&
                    variantData.getVariantConfiguration().getBuildType().isEmbedMicroApp()) {
                    manifests.add(new ManifestDependencyImpl(
                            variantData.generateApkDataTask.getManifestFile(), []))
                }

                if (csmTask != null) {
                    manifests.add(
                            new ManifestDependencyImpl(csmTask.getManifestFile(), []))
                }

                return manifests
            }

            conventionMapping(processManifestTask).map("minSdkVersion") {
                if (androidBuilder.isPreviewTarget()) {
                    return androidBuilder.getTargetCodename()
                }

                mergedFlavor.minSdkVersion?.apiString
            }

            conventionMapping(processManifestTask).map("targetSdkVersion") {
                if (androidBuilder.isPreviewTarget()) {
                    return androidBuilder.getTargetCodename()
                }

                return mergedFlavor.targetSdkVersion?.apiString
            }

            conventionMapping(processManifestTask).map("maxSdkVersion") {
                if (androidBuilder.isPreviewTarget()) {
                    return null
                }

                return mergedFlavor.maxSdkVersion
            }

            conventionMapping(processManifestTask).map("manifestOutputFile") {
                project.file(
                        "$project.buildDir/${FD_INTERMEDIATES}/manifests/full/" +
                                "${outputDirName}/AndroidManifest.xml")
            }

            conventionMapping(processManifestTask).map("reportFile") {
                project.file(
                        "$project.buildDir/${FD_OUTPUTS}/logs/manifest-merger-${config.baseName}-report.txt")
            }
        }
    }

    private CompatibleScreensManifest createCompatibleScreensManifest(
            @NonNull BaseVariantOutputData variantOutputData,
            @NonNull Set<String> screenSizes) {

        CompatibleScreensManifest csmTask = project.tasks.create(
                "create${variantOutputData.fullName.capitalize()}CompatibleScreensManifest",
                CompatibleScreensManifest)

        csmTask.screenDensity = variantOutputData.getMainOutputFile().getFilter(OutputFile.DENSITY)
        csmTask.screenSizes = screenSizes

        conventionMapping(csmTask).map("manifestFile") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/manifests/density/" +
                            "${variantOutputData.dirName}/AndroidManifest.xml")
        }

        return csmTask;
    }

    @CompileDynamic
    private static ConventionMapping conventionMapping(Task task) {
        task.conventionMapping
    }

    /**
     * Using the BaseConventionPlugin does not work for archivesBaseName dynamic attribute,
     * revert to a dynamic property invocation.
     */
    @CompileDynamic
    private static String getArchivesBaseName(Project project){
        project.archivesBaseName
    }

    public void createMergeLibManifestsTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @NonNull String manifestOutDir) {
        VariantConfiguration config = variantData.variantConfiguration

        // get single output for now.
        BaseVariantOutputData variantOutputData = variantData.outputs.get(0)

        def processManifest = project.tasks.create(
                "process${variantData.variantConfiguration.fullName.capitalize()}Manifest",
                ProcessManifest)
        variantOutputData.manifestProcessorTask = processManifest
        processManifest.androidBuilder = androidBuilder

        processManifest.dependsOn variantData.prepareDependenciesTask
        processManifest.variantConfiguration = config

        ProductFlavor mergedFlavor = config.mergedFlavor

        conventionMapping(processManifest).map("minSdkVersion") {
            if (androidBuilder.isPreviewTarget()) {
                return androidBuilder.getTargetCodename()
            }
            return mergedFlavor.minSdkVersion?.apiString
        }

        conventionMapping(processManifest).map("targetSdkVersion") {
            if (androidBuilder.isPreviewTarget()) {
                return androidBuilder.getTargetCodename()
            }

            return mergedFlavor.targetSdkVersion?.apiString
        }

        conventionMapping(processManifest).map("maxSdkVersion") {
            if (androidBuilder.isPreviewTarget()) {
                return null
            }

            return mergedFlavor.maxSdkVersion
        }

        conventionMapping(processManifest).map("manifestOutputFile") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/${manifestOutDir}/" +
                            "${variantData.variantConfiguration.dirName}/AndroidManifest.xml")
        }

        conventionMapping(processManifest).map("aaptFriendlyManifestOutputFile") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/${manifestOutDir}/" +
                            "${variantData.variantConfiguration.dirName}/aapt/AndroidManifest.xml")
        }
    }

    protected void createProcessTestManifestTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @NonNull String manifestOurDir) {
        def processTestManifestTask;
        VariantConfiguration config = variantData.variantConfiguration
        processTestManifestTask = project.tasks.create(
                "process${variantData.variantConfiguration.fullName.capitalize()}Manifest",
                ProcessTestManifest)
        conventionMapping(processTestManifestTask).map("testManifestFile") {
            config.getMainManifest()
        }
        conventionMapping(processTestManifestTask).map("tmpDir") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/${manifestOurDir}/tmp")
        }

        // get single output for now.
        BaseVariantOutputData variantOutputData = variantData.outputs.get(0)

        variantOutputData.manifestProcessorTask = processTestManifestTask
        processTestManifestTask.dependsOn variantData.prepareDependenciesTask

        processTestManifestTask.androidBuilder = androidBuilder

        conventionMapping(processTestManifestTask).map("testApplicationId") {
            config.applicationId
        }
        conventionMapping(processTestManifestTask).map("minSdkVersion") {
            if (androidBuilder.isPreviewTarget()) {
                return androidBuilder.getTargetCodename()
            }

            config.minSdkVersion?.apiString
        }
        conventionMapping(processTestManifestTask).map("targetSdkVersion") {
            if (androidBuilder.isPreviewTarget()) {
                return androidBuilder.getTargetCodename()
            }

            return config.targetSdkVersion?.apiString
        }
        conventionMapping(processTestManifestTask).map("testedApplicationId") {
            config.testedApplicationId
        }
        conventionMapping(processTestManifestTask).map("instrumentationRunner") {
            config.instrumentationRunner
        }
        conventionMapping(processTestManifestTask).map("handleProfiling") {
            config.handleProfiling
        }
        conventionMapping(processTestManifestTask).map("functionalTest") {
            config.functionalTest
        }
        conventionMapping(processTestManifestTask).map("libraries") {
            getManifestDependencies(config.directLibraries)
        }
        conventionMapping(processTestManifestTask).map("manifestOutputFile") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/${manifestOurDir}/${variantData.variantConfiguration.dirName}/AndroidManifest.xml")
        }
        conventionMapping(processTestManifestTask).map("placeholdersValues") {
            variantData.getVariantConfiguration().getManifestPlaceholders()
        }
    }

    public void createRenderscriptTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        GradleVariantConfiguration config = variantData.variantConfiguration

        // get single output for now.
        BaseVariantOutputData variantOutputData = variantData.outputs.get(0)

        def renderscriptTask = project.tasks.create(
                "compile${variantData.variantConfiguration.fullName.capitalize()}Renderscript",
                RenderscriptCompile)
        variantData.renderscriptCompileTask = renderscriptTask
        if (config.type.isForTesting()) {
            renderscriptTask.dependsOn variantOutputData.manifestProcessorTask
        } else {
            renderscriptTask.dependsOn variantData.checkManifestTask
        }

        ProductFlavor mergedFlavor = config.mergedFlavor
        boolean ndkMode = config.renderscriptNdkModeEnabled

        variantData.resourceGenTask.dependsOn renderscriptTask
        // only put this dependency if rs will generate Java code
        if (!ndkMode) {
            variantData.sourceGenTask.dependsOn renderscriptTask
        }

        renderscriptTask.dependsOn variantData.prepareDependenciesTask
        renderscriptTask.androidBuilder = androidBuilder

        conventionMapping(renderscriptTask).map("targetApi") {
            int targetApi = mergedFlavor.renderscriptTargetApi != null ?
                    mergedFlavor.renderscriptTargetApi : -1
            ApiVersion apiVersion = config.getMinSdkVersion()
            if (apiVersion != null) {
                int minSdk = apiVersion.apiLevel
                if (apiVersion.codename != null) {
                    minSdk = SdkVersionInfo.getApiByBuildCode(apiVersion.codename, true)
                }

                return targetApi > minSdk ? targetApi : minSdk
            }

            return targetApi
        }

        renderscriptTask.supportMode = config.renderscriptSupportModeEnabled
        renderscriptTask.ndkMode = ndkMode
        renderscriptTask.debugBuild = config.buildType.renderscriptDebuggable
        renderscriptTask.optimLevel = config.buildType.renderscriptOptimLevel

        conventionMapping(renderscriptTask).map("sourceDirs") { config.renderscriptSourceList }
        conventionMapping(renderscriptTask).map("importDirs") { config.renderscriptImports }

        conventionMapping(renderscriptTask).map("sourceOutputDir") {
            project.file(
                    "$project.buildDir/${FD_GENERATED}/source/rs/${variantData.variantConfiguration.dirName}")
        }
        conventionMapping(renderscriptTask).map("resOutputDir") {
            project.file(
                    "$project.buildDir/${FD_GENERATED}/res/rs/${variantData.variantConfiguration.dirName}")
        }
        conventionMapping(renderscriptTask).map("objOutputDir") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/rs/${variantData.variantConfiguration.dirName}/obj")
        }
        conventionMapping(renderscriptTask).map("libOutputDir") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/rs/${variantData.variantConfiguration.dirName}/lib")
        }
        conventionMapping(renderscriptTask).map("ndkConfig") { config.ndkConfig }
    }

    public void createMergeResourcesTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            final boolean process9Patch) {
        MergeResources mergeResourcesTask = basicCreateMergeResourcesTask(
                variantData,
                "merge",
                "$project.buildDir/${FD_INTERMEDIATES}/res/${variantData.variantConfiguration.dirName}",
                true /*includeDependencies*/,
                process9Patch)
        variantData.mergeResourcesTask = mergeResourcesTask
    }

    public MergeResources basicCreateMergeResourcesTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @NonNull String taskNamePrefix,
            @NonNull String outputLocation,
            final boolean includeDependencies,
            final boolean process9Patch) {
        MergeResources mergeResourcesTask = project.tasks.create(
                "$taskNamePrefix${variantData.variantConfiguration.fullName.capitalize()}Resources",
                MergeResources)

        mergeResourcesTask.dependsOn variantData.prepareDependenciesTask,
                variantData.resourceGenTask
        mergeResourcesTask.androidBuilder = androidBuilder
        mergeResourcesTask.incrementalFolder = project.file(
                "$project.buildDir/${FD_INTERMEDIATES}/incremental/${taskNamePrefix}Resources/${variantData.variantConfiguration.dirName}")

        mergeResourcesTask.process9Patch = process9Patch
        mergeResourcesTask.crunchPng = extension.aaptOptions.getCruncherEnabled()

        conventionMapping(mergeResourcesTask).
                map("useNewCruncher") { getExtension().aaptOptions.useNewCruncher }

        conventionMapping(mergeResourcesTask).map("inputResourceSets") {
            List<File> generatedResFolders = Lists.newArrayList(
                    variantData.renderscriptCompileTask.getResOutputDir(),
                    variantData.generateResValuesTask.getResOutputDir())
            if (variantData.extraGeneratedResFolders != null) {
                generatedResFolders += variantData.extraGeneratedResFolders
            }
            if (variantData.generateApkDataTask != null &&
                    variantData.getVariantConfiguration().getBuildType().isEmbedMicroApp()) {
                generatedResFolders.add(variantData.generateApkDataTask.getResOutputDir())
            }
            variantData.variantConfiguration.getResourceSets(generatedResFolders,
                    includeDependencies)
        }

        conventionMapping(mergeResourcesTask).map("outputDir") { project.file(outputLocation) }

        return mergeResourcesTask
    }

    public void createMergeAssetsTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @Nullable String outputLocation,
            final boolean includeDependencies) {
        if (outputLocation == null) {
            outputLocation =
                    "$project.buildDir/${FD_INTERMEDIATES}/assets/${variantData.variantConfiguration.dirName}"
        }

        VariantConfiguration variantConfig = variantData.variantConfiguration

        def mergeAssetsTask = project.tasks.create(
                "merge${variantConfig.fullName.capitalize()}Assets",
                MergeAssets)
        variantData.mergeAssetsTask = mergeAssetsTask

        mergeAssetsTask.dependsOn variantData.prepareDependenciesTask, variantData.assetGenTask
        mergeAssetsTask.androidBuilder = androidBuilder
        mergeAssetsTask.incrementalFolder =
                project.file(
                        "$project.buildDir/${FD_INTERMEDIATES}/incremental/mergeAssets/${variantConfig.dirName}")

        conventionMapping(mergeAssetsTask).map("inputAssetSets") {
            def generatedAssets = []
            if (variantData.copyApkTask != null) {
                generatedAssets.add(variantData.copyApkTask.destinationDir)
            }
            variantConfig.getAssetSets(generatedAssets, includeDependencies)
        }
        conventionMapping(mergeAssetsTask).map("outputDir") { project.file(outputLocation) }
    }

    public void createBuildConfigTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        def generateBuildConfigTask = project.tasks.create(
                "generate${variantData.variantConfiguration.fullName.capitalize()}BuildConfig",
                GenerateBuildConfig)

        variantData.generateBuildConfigTask = generateBuildConfigTask

        VariantConfiguration variantConfiguration = variantData.variantConfiguration

        variantData.sourceGenTask.dependsOn generateBuildConfigTask
        if (variantConfiguration.type.isForTesting()) {
            // in case of a test project, the manifest is generated so we need to depend
            // on its creation.

            // For test apps there should be a single output, so we get it.
            BaseVariantOutputData variantOutputData = variantData.outputs.get(0)

            generateBuildConfigTask.dependsOn variantOutputData.manifestProcessorTask
        } else {
            generateBuildConfigTask.dependsOn variantData.checkManifestTask
        }

        generateBuildConfigTask.androidBuilder = androidBuilder

        conventionMapping(generateBuildConfigTask).map("buildConfigPackageName") {
            variantConfiguration.originalApplicationId
        }

        conventionMapping(generateBuildConfigTask).map("appPackageName") {
            variantConfiguration.applicationId
        }

        conventionMapping(generateBuildConfigTask).map("versionName") {
            variantConfiguration.versionName
        }

        conventionMapping(generateBuildConfigTask).map("versionCode") {
            variantConfiguration.versionCode
        }

        conventionMapping(generateBuildConfigTask).map("debuggable") {
            variantConfiguration.buildType.isDebuggable()
        }

        conventionMapping(generateBuildConfigTask).map("buildTypeName") {
            variantConfiguration.buildType.name
        }

        conventionMapping(generateBuildConfigTask).map("flavorName") {
            variantConfiguration.flavorName
        }

        conventionMapping(generateBuildConfigTask).map("flavorNamesWithDimensionNames") {
            variantConfiguration.flavorNamesWithDimensionNames
        }

        conventionMapping(generateBuildConfigTask).map("items") {
            variantConfiguration.buildConfigItems
        }

        conventionMapping(generateBuildConfigTask).map("sourceOutputDir") {
            project.file(
                    "$project.buildDir/${FD_GENERATED}/source/buildConfig/${variantData.variantConfiguration.dirName}")
        }
    }

    public void createGenerateResValuesTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        GenerateResValues generateResValuesTask = project.tasks.create(
                "generate${variantData.variantConfiguration.fullName.capitalize()}ResValues",
                GenerateResValues)
        variantData.generateResValuesTask = generateResValuesTask
        variantData.resourceGenTask.dependsOn generateResValuesTask

        VariantConfiguration variantConfiguration = variantData.variantConfiguration

        generateResValuesTask.androidBuilder = androidBuilder

        conventionMapping(generateResValuesTask).map("items") {
            variantConfiguration.resValues
        }

        conventionMapping(generateResValuesTask).map("resOutputDir") {
            project.file(
                    "$project.buildDir/${FD_GENERATED}/res/generated/${variantData.variantConfiguration.dirName}")
        }
    }

    public void createBackportResourcesTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        String variantName = variantData.variantConfiguration.fullName.capitalize()

        variantData.backportResourcesTask = project.tasks.create("backport${variantName}Resources")

        int minSdk = variantData.variantConfiguration.minSdkVersion.getApiLevel()
        if (extension.generatePngs  && minSdk < VectorDrawableRenderer.MIN_SDK_WITH_VECTOR_SUPPORT) {
            GeneratePngsFromVectorDrawablesTask generatePngs = project.tasks.create(
                    "generate${variantName}Pngs",
                    GeneratePngsFromVectorDrawablesTask)
            variantData.generatePngsFromVectorDrawablesTask = generatePngs

            generatePngs.dependsOn variantData.mergeResourcesTask

            File resDirectory = variantData.mergeResourcesTask.outputDir

            generatePngs.xmlFiles =  project.fileTree(
                    dir: resDirectory,
                    includes: ["drawable*/*.xml"])
            generatePngs.outputResDirectory = resDirectory
            // TODO: configure this in the extension.
            generatePngs.densitiesToGenerate = [Density.HIGH, Density.XHIGH]

            variantData.backportResourcesTask.dependsOn variantData.generatePngsFromVectorDrawablesTask
        }

    }


    public void createProcessResTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            boolean generateResourcePackage) {
        createProcessResTask(variantData,
                "$project.buildDir/${FD_INTERMEDIATES}/symbols/${variantData.variantConfiguration.dirName}",
                generateResourcePackage)
    }

    public void createProcessResTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @NonNull final String symbolLocation,
            boolean generateResourcePackage) {

        VariantConfiguration config = variantData.variantConfiguration

        // loop on all outputs. The only difference will be the name of the task, and location
        // of the generated data.
        for (BaseVariantOutputData vod : variantData.outputs) {
            // create final var inside the loop to ensure the closures will work.
            final BaseVariantOutputData variantOutputData = vod

            String outputName = variantOutputData.fullName.capitalize()
            String outputBaseName = variantOutputData.baseName

            ProcessAndroidResources processResources = project.tasks.create(
                    "process${outputName}Resources",
                    ProcessAndroidResources)

            variantOutputData.processResourcesTask = processResources

            processResources.dependsOn variantOutputData.manifestProcessorTask,
                    variantData.mergeResourcesTask, variantData.mergeAssetsTask,
                    variantData.backportResourcesTask
            processResources.androidBuilder = androidBuilder

            if (variantData.getSplitHandlingPolicy() ==
                    BaseVariantData.SplitHandlingPolicy.RELEASE_21_AND_AFTER_POLICY) {

                Set<String> filters = new HashSet<String>(
                        getExtension().getSplits().getDensityFilters());
                filters.addAll(getExtension().getSplits().getLanguageFilters());
                filters = removeAllNullEntries(filters);
                processResources.splits = filters;
            }

            // only generate code if the density filter is null, and if we haven't generated
            // it yet (if you have abi + density splits, then several abi output will have no
            // densityFilter)
            if (variantOutputData.getMainOutputFile().getFilter(OutputFile.DENSITY) == null
                    && variantData.generateRClassTask == null) {
                variantData.generateRClassTask = processResources
                variantData.sourceGenTask.dependsOn processResources
                processResources.enforceUniquePackageName = getExtension().getEnforceUniquePackageName()

                conventionMapping(processResources).map("libraries") {
                    getTextSymbolDependencies(config.allLibraries)
                }
                conventionMapping(processResources).map("packageForR") {
                    config.originalApplicationId
                }

                // TODO: unify with generateBuilderConfig, compileAidl, and library packaging somehow?
                conventionMapping(processResources).map("sourceOutputDir") {
                    project.file(
                            "$project.buildDir/${FD_GENERATED}/source/r/${config.dirName}")
                }

                conventionMapping(processResources).map("textSymbolOutputDir") {
                    project.file(symbolLocation)
                }

                if (config.buildType.isMinifyEnabled()) {
                    if (config.buildType.shrinkResources && config.useJack) {
                        LoggingUtil.displayWarning(logger, project,
                                "shrinkResources does not yet work with useJack=true")
                    }
                    conventionMapping(processResources).map("proguardOutputFile") {
                        project.file(
                                "$project.buildDir/${FD_INTERMEDIATES}/proguard-rules/${config.dirName}/aapt_rules.txt")
                    }
                } else if (config.buildType.shrinkResources) {
                    LoggingUtil.displayWarning(logger, project,
                            "To shrink resources you must also enable ProGuard")
                }
            }

            conventionMapping(processResources).map("manifestFile") {
                variantOutputData.manifestProcessorTask.getOutputFile()
            }

            conventionMapping(processResources).map("resDir") {
                variantData.mergeResourcesTask.outputDir
            }

            conventionMapping(processResources).map("assetsDir") {
                variantData.mergeAssetsTask.outputDir
            }

            if (generateResourcePackage) {
                conventionMapping(processResources).map("packageOutputFile") {
                    project.file(
                            "$project.buildDir/${FD_INTERMEDIATES}/resources/resources-${outputBaseName}.ap_")
                }
            }

            conventionMapping(processResources).map("type") { config.type }
            conventionMapping(processResources).map("debuggable") { config.buildType.debuggable }
            conventionMapping(processResources).map("aaptOptions") { getExtension().aaptOptions }
            conventionMapping(processResources).
                    map("pseudoLocalesEnabled") { config.buildType.pseudoLocalesEnabled }

            conventionMapping(processResources).map("resourceConfigs") {
                return config.mergedFlavor.resourceConfigurations
            }
            conventionMapping(processResources).map("preferredDensity") {
                variantOutputData.getMainOutputFile().getFilter(OutputFile.DENSITY)
            }

        }
    }

    /**
     * Creates the split resources packages task if necessary. AAPT will produce split packages
     * for all --split provided parameters. These split packages should be signed and moved
     * unchanged to the APK build output directory.
     * @param variantData the variant configuration.
     */
    public void createSplitResourcesTasks(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {

        assert variantData.getSplitHandlingPolicy() ==
                BaseVariantData.SplitHandlingPolicy.RELEASE_21_AND_AFTER_POLICY;

        VariantConfiguration config = variantData.variantConfiguration
        Set<String> densityFilters =
                removeAllNullEntries(getExtension().getSplits().getDensityFilters());
        Set<String> abiFilters = removeAllNullEntries(getExtension().getSplits().getAbiFilters());
        Set<String> languageFilters =
                removeAllNullEntries(getExtension().getSplits().getLanguageFilters());

        def outputs = variantData.outputs;
        if (outputs.size() != 1) {
            throw new RuntimeException("In release 21 and later, there can be only one main APK, " +
                    "found " + outputs.size());
        }

        BaseVariantOutputData variantOutputData = outputs.get(0);
        variantOutputData.packageSplitResourcesTask =
                project.tasks.create("package${config.fullName.capitalize()}SplitResources",
                        PackageSplitRes);
        variantOutputData.packageSplitResourcesTask.inputDirectory =
                variantOutputData.processResourcesTask.packageOutputFile.getParentFile()
        variantOutputData.packageSplitResourcesTask.densitySplits = densityFilters
        variantOutputData.packageSplitResourcesTask.languageSplits = languageFilters
        variantOutputData.packageSplitResourcesTask.outputBaseName = config.baseName
        variantOutputData.packageSplitResourcesTask.signingConfig =
                (SigningConfig) config.signingConfig
        variantOutputData.packageSplitResourcesTask.outputDirectory =
                new File("$project.buildDir/${FD_INTERMEDIATES}/splits/${config.dirName}")
        variantOutputData.packageSplitResourcesTask.androidBuilder = androidBuilder
        variantOutputData.packageSplitResourcesTask.dependsOn variantOutputData.processResourcesTask

        SplitZipAlign zipAlign = project.tasks.
                create("zipAlign${config.fullName.capitalize()}SplitPackages", SplitZipAlign)
        conventionMapping(zipAlign).map("zipAlignExe") {
            String path = androidBuilder.targetInfo?.buildTools?.getPath(ZIP_ALIGN)
            if (path != null) {
                return new File(path)
            }

            return null
        }

        zipAlign.outputDirectory = new File("$project.buildDir/outputs/apk")
        conventionMapping(zipAlign).map("densityOrLanguageInputFiles") {
            return  variantOutputData.packageSplitResourcesTask.getOutputFiles()
        }
        zipAlign.outputBaseName = config.baseName
        zipAlign.abiFilters = abiFilters
        zipAlign.languageFilters = languageFilters
        zipAlign.densityFilters = densityFilters
        File metadataDirectory = new File(zipAlign.outputDirectory.getParentFile(), "metadata")
        zipAlign.apkMetadataFile = new File(metadataDirectory, "${config.fullName}.mtd")
        ((ApkVariantOutputData) variantOutputData).splitZipAlign = zipAlign
        zipAlign.dependsOn(variantOutputData.packageSplitResourcesTask)
    }

    public void createSplitAbiTasks(@NonNull ApplicationVariantData variantData) {

        assert variantData.getSplitHandlingPolicy() ==
                BaseVariantData.SplitHandlingPolicy.RELEASE_21_AND_AFTER_POLICY;

        VariantConfiguration config = variantData.variantConfiguration
        Set<String> filters = AbiSplitOptions.getAbiFilters(
                getExtension().getSplits().getAbiFilters())
        if (filters.isEmpty()) {
            return;
        }
        def outputs = variantData.outputs;
        if (outputs.size() != 1) {
            throw new RuntimeException("In release 21 and later, there can be only one main APK, " +
                    "found " + outputs.size());
        }

        BaseVariantOutputData variantOutputData = outputs.get(0);
        // first create the split APK resources.
        GenerateSplitAbiRes generateSplitAbiRes = project.tasks.
                create("generate${config.fullName.capitalize()}SplitAbiRes",
                        GenerateSplitAbiRes)
        generateSplitAbiRes.androidBuilder = androidBuilder

        generateSplitAbiRes.outputDirectory =
                new File("$project.buildDir/${FD_INTERMEDIATES}/abi/${config.dirName}")
        generateSplitAbiRes.splits = filters
        generateSplitAbiRes.outputBaseName = config.baseName
        generateSplitAbiRes.applicationId = config.getApplicationId()
        generateSplitAbiRes.versionCode = config.getVersionCode()
        generateSplitAbiRes.versionName = config.getVersionName()
        generateSplitAbiRes.debuggable = {
            config.buildType.debuggable
        }
        conventionMapping(generateSplitAbiRes).map("aaptOptions") {
            getExtension().aaptOptions
        }
        generateSplitAbiRes.dependsOn variantOutputData.processResourcesTask

        // then package those resources with the appropriate JNI libraries.
        variantOutputData.packageSplitAbiTask =
                project.tasks.create("package${config.fullName.capitalize()}SplitAbi",
                        PackageSplitAbi);
        variantOutputData.packageSplitAbiTask.inputFiles = generateSplitAbiRes.getOutputFiles()
        variantOutputData.packageSplitAbiTask.splits = filters
        variantOutputData.packageSplitAbiTask.outputBaseName = config.baseName
        variantOutputData.packageSplitAbiTask.signingConfig =
                (SigningConfig) config.signingConfig
        variantOutputData.packageSplitAbiTask.outputDirectory =
                new File("$project.buildDir/${FD_INTERMEDIATES}/splits/${config.dirName}")
        variantOutputData.packageSplitAbiTask.androidBuilder = androidBuilder
        variantOutputData.packageSplitAbiTask.dependsOn generateSplitAbiRes
        variantOutputData.packageSplitAbiTask.dependsOn getNdkBuildable(variantData)

        conventionMapping(variantOutputData.packageSplitAbiTask).map("jniFolders") {
            getJniFolders(variantData);
        }
        conventionMapping(variantOutputData.packageSplitAbiTask).
                map("jniDebuggable") { config.buildType.jniDebuggable }
        conventionMapping(variantOutputData.packageSplitAbiTask).
                map("packagingOptions") { getExtension().packagingOptions }

        ((ApkVariantOutputData) variantOutputData).splitZipAlign.abiInputFiles.addAll(
                variantOutputData.packageSplitAbiTask.getOutputFiles())

        ((ApkVariantOutputData) variantOutputData).splitZipAlign.dependsOn variantOutputData.packageSplitAbiTask
    }

    /**
     * Calculate the list of folders that can contain jni artifacts for this variant.
     * @param variantData the variant
     * @return a potentially empty list of directories that exist or not and that may contains
     * native resources.
     */
    @NonNull
    public Set<File> getJniFolders(@NonNull ApkVariantData variantData) {
        VariantConfiguration config = variantData.variantConfiguration
        // for now only the project's compilation output.
        Set<File> set = Sets.newHashSet()
        set.addAll(getNdkOutputDirectories(variantData))
        set.addAll(variantData.renderscriptCompileTask.libOutputDir)
        set.addAll(config.libraryJniFolders)
        set.addAll(config.jniLibsList)

        if (config.mergedFlavor.renderscriptSupportModeEnabled) {
            File rsLibs = androidBuilder.getSupportNativeLibFolder()
            if (rsLibs != null && rsLibs.isDirectory()) {
                set.add(rsLibs);
            }
        }

        return set
    }

    public void createProcessJavaResTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        VariantConfiguration variantConfiguration = variantData.variantConfiguration

        Copy processResources = project.tasks.create(
                "process${variantData.variantConfiguration.fullName.capitalize()}JavaRes",
                ProcessResources);
        variantData.processJavaResourcesTask = processResources

        // set the input
        processResources.from(((AndroidSourceSet) variantConfiguration.defaultSourceSet).resources.
                getSourceFiles())

        if (variantConfiguration.type != ANDROID_TEST) {
            processResources.from(
                    ((AndroidSourceSet) variantConfiguration.buildTypeSourceSet).resources.
                            getSourceFiles())
        }
        if (variantConfiguration.hasFlavors()) {
            for (SourceProvider flavorSourceSet : variantConfiguration.flavorSourceProviders) {
                processResources.
                        from(((AndroidSourceSet) flavorSourceSet).resources.getSourceFiles())
            }
        }

        conventionMapping(processResources).map("destinationDir") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/javaResources/${variantData.variantConfiguration.dirName}")
        }
    }

    public void createAidlTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @Nullable File parcelableDir) {
        VariantConfiguration variantConfiguration = variantData.variantConfiguration

        def compileTask = project.tasks.create(
                "compile${variantData.variantConfiguration.fullName.capitalize()}Aidl",
                AidlCompile)
        variantData.aidlCompileTask = compileTask

        variantData.sourceGenTask.dependsOn compileTask
        variantData.aidlCompileTask.dependsOn variantData.prepareDependenciesTask

        compileTask.androidBuilder = androidBuilder
        compileTask.incrementalFolder =
                project.file(
                        "$project.buildDir/${FD_INTERMEDIATES}/incremental/aidl/${variantData.variantConfiguration.dirName}")

        conventionMapping(compileTask).map("sourceDirs") { variantConfiguration.aidlSourceList }
        conventionMapping(compileTask).map("importDirs") { variantConfiguration.aidlImports }

        conventionMapping(compileTask).map("sourceOutputDir") {
            project.file(
                    "$project.buildDir/${FD_GENERATED}/source/aidl/${variantData.variantConfiguration.dirName}")
        }
        compileTask.aidlParcelableDir = parcelableDir
    }

    public void createJackAndUnitTestVerificationTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @NonNull BaseVariantData<? extends BaseVariantOutputData> testedVariantData) {

        PreCompilationVerificationTask verificationTask = project.tasks.create(
                "preCompile${variantData.variantConfiguration.fullName.capitalize()}Java",
                PreCompilationVerificationTask)
        verificationTask.useJack = testedVariantData.getVariantConfiguration().getUseJack()
        verificationTask.testSourceFiles = variantData.getJavaSources()
        variantData.javaCompileTask.dependsOn verificationTask
    }

    public void createJavaCompileTask(
            @NonNull final TaskFactory tasks,
            @NonNull final VariantScope scope) {
        BaseVariantData<? extends BaseVariantOutputData> variantData = scope.variantData;
        AndroidTask<JavaCompile> javaCompileTask = androidTasks.create(tasks,
                "compile${scope.variantData.variantConfiguration.fullName.capitalize()}Java",
                JavaCompile,
                new JavaCompileConfigAction(scope));
        scope.javaCompileTask = javaCompileTask;
        javaCompileTask.optionalDependsOn(tasks, variantData.sourceGenTask)

        // Create jar task for uses by external modules.
        if (variantData.variantDependency.classesConfiguration != null) {
            tasks.create("package${variantData.variantConfiguration.fullName.capitalize()}JarArtifact", Jar) { Jar jar ->
                variantData.classesJarTask = jar
                jar.dependsOn javaCompileTask.name

                // add the class files (whether they are instrumented or not.
                jar.from({ scope.getJavaOutputDir() })

                jar.destinationDir = scope.getJavaOutputDir();
                jar.archiveName = "classes.jar"
            }
        }
    }

    public void createGenerateMicroApkDataTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @NonNull Configuration config) {
        GenerateApkDataTask task = project.tasks.create(
                "handle${variantData.variantConfiguration.fullName.capitalize()}MicroApk",
                GenerateApkDataTask)

        variantData.generateApkDataTask = task

        task.androidBuilder = androidBuilder
        conventionMapping(task).map("resOutputDir") {
            project.file(
                    "$project.buildDir/${FD_GENERATED}/res/microapk/${variantData.variantConfiguration.dirName}")
        }
        conventionMapping(task).map("apkFile") {
            // only care about the first one. There shouldn't be more anyway.
            config.getFiles().iterator().next()
        }
        conventionMapping(task).map("manifestFile") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/${FD_GENERATED}/manifests/microapk/${variantData.variantConfiguration.dirName}/${FN_ANDROID_MANIFEST_XML}")
        }
        conventionMapping(task).map("mainPkgName") {
            variantData.variantConfiguration.getApplicationId()
        }

        conventionMapping(task).map("minSdkVersion") {
            variantData.variantConfiguration.getMinSdkVersion().apiLevel
        }

        conventionMapping(task).map("targetSdkVersion") {
            variantData.variantConfiguration.getTargetSdkVersion().apiLevel
        }

        task.dependsOn config

        // the merge res task will need to run after this one.
        variantData.resourceGenTask.dependsOn task
    }

    public void createNdkTasks(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        NdkCompile ndkCompile = project.tasks.create(
                "compile${variantData.variantConfiguration.fullName.capitalize()}Ndk",
                NdkCompile)

        ndkCompile.dependsOn variantData.preBuildTask

        ndkCompile.androidBuilder = androidBuilder
        ndkCompile.ndkDirectory = sdkHandler.getNdkFolder()
        variantData.ndkCompileTask = ndkCompile
        variantData.compileTask.dependsOn variantData.ndkCompileTask

        GradleVariantConfiguration variantConfig = variantData.variantConfiguration

        if (variantConfig.mergedFlavor.renderscriptNdkModeEnabled) {
            ndkCompile.ndkRenderScriptMode = true
            ndkCompile.dependsOn variantData.renderscriptCompileTask
        } else {
            ndkCompile.ndkRenderScriptMode = false
        }

        conventionMapping(ndkCompile).map("sourceFolders") {
            List<File> sourceList = variantConfig.jniSourceList
            if (variantConfig.mergedFlavor.renderscriptNdkModeEnabled) {
                sourceList.add(variantData.renderscriptCompileTask.sourceOutputDir)
            }

            return sourceList
        }

        conventionMapping(ndkCompile).map("generatedMakefile") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/ndk/${variantData.variantConfiguration.dirName}/Android.mk")
        }

        conventionMapping(ndkCompile).map("ndkConfig") { variantConfig.ndkConfig }

        conventionMapping(ndkCompile).map("debuggable") {
            variantConfig.buildType.jniDebuggable
        }

        conventionMapping(ndkCompile).map("objFolder") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/ndk/${variantData.variantConfiguration.dirName}/obj")
        }
        conventionMapping(ndkCompile).map("soFolder") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/ndk/${variantData.variantConfiguration.dirName}/lib")
        }
    }

    /**
     * Creates the tasks to build unit tests.
     *
     * @param variantData the test variant
     */
    void createUnitTestVariantTasks(
            @NonNull TaskFactory tasks,
            @NonNull TestVariantData variantData) {
        BaseVariantData testedVariantData = variantData.getTestedVariantData() as BaseVariantData
        variantData.assembleVariantTask.dependsOn createMockableJar
        VariantScope variantScope = createVariantScope(variantData);

        createPreBuildTasks(variantData)
        createProcessJavaResTask(variantData)
        createCompileAnchorTask(variantData)
        createJavaCompileTask(tasks, variantScope);
        createJackAndUnitTestVerificationTask(variantData, testedVariantData)
        createUnitTestTask(tasks, variantData)

        // This hides the assemble unit test task from the task list.
        variantData.assembleVariantTask.group = null
    }

    /**
     * Creates the tasks to build android tests.
     *
     * @param variantData the test variant
     */
    public void createAndroidTestVariantTasks(
            @NonNull TaskFactory tasks,
            @NonNull TestVariantData variantData) {
        BaseVariantData<? extends BaseVariantOutputData> testedVariantData =
                variantData.
                        getTestedVariantData() as BaseVariantData<? extends BaseVariantOutputData>

        // get single output for now (though this may always be the case for tests).
        BaseVariantOutputData variantOutputData = variantData.outputs.get(0)
        BaseVariantOutputData testedVariantOutputData = testedVariantData.outputs.get(0)

        createAnchorTasks(variantData)

        // Add a task to process the manifest
        createProcessTestManifestTask(variantData, "manifests")

        // Add a task to create the res values
        createGenerateResValuesTask(variantData)

        // Add a task to compile renderscript files.
        createRenderscriptTask(variantData)

        // Add a task to merge the resource folders
        createMergeResourcesTask(variantData, true /*process9Patch*/)

        // Add a task to merge the assets folders
        createMergeAssetsTask(variantData, null /*default location*/, true /*includeDependencies*/)

        if (testedVariantData.variantConfiguration.type == VariantType.LIBRARY) {
            // in this case the tested library must be fully built before test can be built!
            if (testedVariantOutputData.assembleTask != null) {
                variantOutputData.manifestProcessorTask.dependsOn testedVariantOutputData.assembleTask
                variantData.mergeResourcesTask.dependsOn testedVariantOutputData.assembleTask
            }
        }

        // Add a task to create the BuildConfig class
        createBuildConfigTask(variantData)

        createBackportResourcesTask(variantData)

        // Add a task to generate resource source files
        createProcessResTask(variantData, true /*generateResourcePackage*/)

        // process java resources
        createProcessJavaResTask(variantData)

        createAidlTask(variantData, null /*parcelableDir*/)

        // Variant scope should be create at the function, but there is currently a dependencies
        // on the NdkCompile tasks, and the scope mechanism does not support lazy evaluation yet.
        VariantScope variantScope = createVariantScope(variantData);

        // Add NDK tasks
        if (isNdkTaskNeeded) {
            createNdkTasks(variantData)
        }
        variantScope.setNdkBuildable(getNdkBuildable(variantData))
        variantScope.setNdkOutputDirectories(getNdkOutputDirectories(variantData))

        // Add a task to compile the test application
        if (variantData.getVariantConfiguration().useJack) {
            createJackTask(variantData, testedVariantData);
        } else {
            //createJavaCompileTask(variantData, testedVariantData)
            createJavaCompileTask(tasks, variantScope)
            createPostCompilationTasks(tasks, variantScope)
        }

        createPackagingTask(tasks, variantScope, false /*publishApk*/)

        tasks.named(ASSEMBLE_ANDROID_TEST) {
            it.dependsOn variantOutputData.assembleTask
        }

        createConnectedTestForVariantData(tasks, variantData, TestType.APPLICATION)
    }

    // TODO - should compile src/lint/java from src/lint/java and jar it into build/lint/lint.jar
    public void createLintCompileTask() {
        lintCompile = project.tasks.create("compileLint", Task)
        File outputDir = new File("$project.buildDir/${FD_INTERMEDIATES}/lint")

        lintCompile.doFirst {
            // create the directory for lint output if it does not exist.
            if (!outputDir.exists()) {
                boolean mkdirs = outputDir.mkdirs();
                if (!mkdirs) {
                    throw new GradleException("Unable to create lint output directory.")
                }
            }
        }
    }

    /** Is the given variant relevant for lint? */
    private static boolean isLintVariant(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> baseVariantData) {
        // Only create lint targets for variants like debug and release, not debugTest
        VariantConfiguration config = baseVariantData.variantConfiguration
        // TODO: re-enable with Jack when possible
        return !config.getType().isForTesting() && !config.useJack;
    }

    // Add tasks for running lint on individual variants. We've already added a
    // lint task earlier which runs on all variants.
    public void createLintTasks(TaskFactory tasks, VariantScope scope) {
        final BaseVariantData<? extends BaseVariantOutputData> baseVariantData = scope.variantData
        if (!isLintVariant(baseVariantData)) {
            return;
        }

        // wire the main lint task dependency.
        tasks.named(LINT) { it
            it.dependsOn(lintCompile)
            if (baseVariantData.javaCompileTask != null) {
                it.dependsOn(baseVariantData.javaCompileTask)
            }
            if (scope.javaCompileTask != null) {
                it.dependsOn(scope.javaCompileTask.name)
            }
        }

        String variantName = baseVariantData.variantConfiguration.fullName
        def capitalizedVariantName = variantName.capitalize()
        AndroidTask<Lint> variantLintCheck = androidTasks.create(tasks,
                "lint" + capitalizedVariantName, Lint) { Lint variantLintCheck ->
            variantLintCheck.dependsOn lintCompile

            // Note that we don't do "lint.dependsOn lintCheck"; the "lint" target will
            // on its own run through all variants (and compare results), it doesn't delegate
            // to the individual tasks (since it needs to coordinate data collection and
            // reporting)
            variantLintCheck.setLintOptions(getExtension().lintOptions)
            variantLintCheck.setSdkHome(sdkHandler.getSdkFolder())
            variantLintCheck.setVariantName(variantName)
            variantLintCheck.setToolingRegistry(toolingRegistry)
            variantLintCheck.description = "Runs lint on the " + capitalizedVariantName + " build."
            variantLintCheck.group = JavaBasePlugin.VERIFICATION_GROUP
        }
        variantLintCheck.optionalDependsOn(tasks,
                baseVariantData.javaCompileTask,
                scope.javaCompileTask)
    }

    private void createLintVitalTask(@NonNull ApkVariantData variantData) {
        assert getExtension().lintOptions.checkReleaseBuilds
        // TODO: re-enable with Jack when possible
        if (!variantData.variantConfiguration.buildType.debuggable &&
                !variantData.variantConfiguration.useJack) {
            String variantName = variantData.variantConfiguration.fullName
            def capitalizedVariantName = variantName.capitalize()
            def taskName = "lintVital" + capitalizedVariantName
            Lint lintReleaseCheck = project.tasks.create(taskName, Lint)
            // TODO: Make this task depend on lintCompile too (resolve initialization order first)
            optionalDependsOn(lintReleaseCheck, variantData.javaCompileTask)
            lintReleaseCheck.setLintOptions(getExtension().lintOptions)
            lintReleaseCheck.setSdkHome(sdkHandler.getSdkFolder())
            lintReleaseCheck.setVariantName(variantName)
            lintReleaseCheck.setToolingRegistry(toolingRegistry)
            lintReleaseCheck.setFatalOnly(true)
            lintReleaseCheck.description = "Runs lint on just the fatal issues in the " +
                    capitalizedVariantName + " build."
            variantData.assembleVariantTask.dependsOn lintReleaseCheck

            // If lint is being run, we do not need to run lint vital.
            project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                if (taskGraph.hasTask(LINT)) {
                    lintReleaseCheck.setEnabled(false)
                }
            }
        }
    }

    private void createUnitTestTask(@NonNull TaskFactory tasks, @NonNull TestVariantData variantData) {
        BaseVariantData testedVariantData = variantData.testedVariantData as BaseVariantData

        Test runTestsTask = project.tasks.create(
                UNIT_TEST.prefix +
                        testedVariantData.variantConfiguration.fullName.capitalize(),
                Test)
        runTestsTask.group = JavaBasePlugin.VERIFICATION_GROUP
        runTestsTask.description = "Run unit tests for the " +
                "$testedVariantData.variantConfiguration.fullName build."

        fixTestTaskSources(runTestsTask)

        runTestsTask.dependsOn variantData.assembleVariantTask

        AbstractCompile testCompileTask = variantData.javaCompileTask
        runTestsTask.testClassesDir = testCompileTask.destinationDir

        conventionMapping(runTestsTask).map("classpath") {
            project.files(
                    testCompileTask.classpath,
                    testCompileTask.outputs.files,
                    variantData.processJavaResourcesTask.outputs,
                    testedVariantData.processJavaResourcesTask.outputs,
                    androidBuilder.bootClasspath.findAll {
                        it.name != SdkConstants.FN_FRAMEWORK_LIBRARY
                    },
                    // Mockable JAR is last, to make sure you can shadow the classes with
                    // dependencies.
                    createMockableJar.outputFile)
        }

        // Put the variant name in the report path, so that different testing tasks don't
        // overwrite each other's reports.
        TestTaskReports testTaskReports = runTestsTask.reports
        for (ConfigurableReport report in [testTaskReports.junitXml, testTaskReports.html]) {
            report.destination = new File(report.destination, testedVariantData.name)
        }

        tasks.named(JavaPlugin.TEST_TASK_NAME) { Task test ->
            test.dependsOn runTestsTask
        }

        extension.testOptions.unitTests.applyConfiguration(runTestsTask)
    }

    @CompileDynamic
    private static void fixTestTaskSources(@NonNull Test testTask) {
        // We are running in afterEvaluate, so the JavaBasePlugin has already added a
        // callback to add test classes to the list of source files of the newly created task.
        // The problem is that we haven't configured the test classes yet (JavaBasePlugin
        // assumes all Test tasks are fully configured at this point), so we have to remove the
        // "directory null" entry from source files and add the right value.
        //
        // This is an ugly hack, since we assume sourceFiles is an instance of
        // DefaultConfigurableFileCollection.
        testTask.inputs.sourceFiles.from.clear()
    }

    public void createTopLevelTestTasks(TaskFactory tasks, boolean hasFlavors) {
        List<String> reportTasks = Lists.newArrayListWithExpectedSize(2)

        List<DeviceProvider> providers = getExtension().deviceProviders

        String connectedRootName = "${CONNECTED}${ANDROID_TEST.suffix}"
        String defaultReportsDir = "$project.buildDir/$FD_REPORTS/$FD_ANDROID_TESTS"
        String defaultResultsDir = "$project.buildDir/${FD_OUTPUTS}/$FD_ANDROID_RESULTS"

        // If more than one flavor, create a report aggregator task and make this the parent
        // task for all new connected tasks.  Otherwise, create a top level connectedAndroidTest
        // DefaultTask.
        if (hasFlavors) {
            tasks.create(connectedRootName, AndroidReportTask) { AndroidReportTask mainConnectedTask ->
                mainConnectedTask.group = JavaBasePlugin.VERIFICATION_GROUP
                mainConnectedTask.description =
                        "Installs and runs instrumentation tests for all flavors on connected devices."
                mainConnectedTask.reportType = ReportType.MULTI_FLAVOR
                conventionMapping(mainConnectedTask).map("resultsDir") {
                    String rootLocation = extension.testOptions.resultsDir ?: defaultResultsDir
                    project.file("$rootLocation/connected/$FD_FLAVORS_ALL")
                }
                conventionMapping(mainConnectedTask).map("reportsDir") {
                    String rootLocation = extension.testOptions.reportDir ?: defaultReportsDir
                    project.file("$rootLocation/connected/$FD_FLAVORS_ALL")
                }

            }
            reportTasks.add(connectedRootName)
        } else {
            tasks.create(connectedRootName) { Task connectedTask ->
                connectedTask.group = JavaBasePlugin.VERIFICATION_GROUP
                connectedTask.description =
                        "Installs and runs instrumentation tests for all flavors on connected devices."
            }
        }
        tasks.named(CONNECTED_CHECK) {
            it.dependsOn connectedRootName
        }

        String mainProviderTaskName =  "${DEVICE}${ANDROID_TEST.suffix}"
        // if more than one provider tasks, either because of several flavors, or because of
        // more than one providers, then create an aggregate report tasks for all of them.
        if (providers.size() > 1 || hasFlavors) {
            tasks.create(mainProviderTaskName, AndroidReportTask) { AndroidReportTask mainProviderTask ->
                mainProviderTask.group = JavaBasePlugin.VERIFICATION_GROUP
                mainProviderTask.description =
                        "Installs and runs instrumentation tests using all Device Providers."
                mainProviderTask.reportType = ReportType.MULTI_FLAVOR

                conventionMapping(mainProviderTask).map("resultsDir") {
                    String rootLocation = extension.testOptions.resultsDir ?: defaultResultsDir

                    project.file("$rootLocation/devices/$FD_FLAVORS_ALL")
                }
                conventionMapping(mainProviderTask).map("reportsDir") {
                    String rootLocation = extension.testOptions.reportDir ?: defaultReportsDir
                    project.file("$rootLocation/devices/$FD_FLAVORS_ALL")
                }
            }
            reportTasks.add(mainProviderTaskName)
        } else {
            tasks.create(mainProviderTaskName) { Task providerTask ->
                providerTask.group = JavaBasePlugin.VERIFICATION_GROUP
                providerTask.description =
                        "Installs and runs instrumentation tests using all Device Providers."
            }
        }

        tasks.named(DEVICE_CHECK) {
            it.dependsOn mainProviderTaskName
        }

        // Create top level unit test tasks.
        tasks.create(JavaPlugin.TEST_TASK_NAME) { Task unitTestTask ->
            unitTestTask.group = JavaBasePlugin.VERIFICATION_GROUP
            unitTestTask.description = "Run unit tests for all variants."
        }
        tasks.named(JavaBasePlugin.CHECK_TASK_NAME) { Task check ->
            check.dependsOn JavaPlugin.TEST_TASK_NAME
        }

        // If gradle is launched with --continue, we want to run all tests and generate an
        // aggregate report (to help with the fact that we may have several build variants, or
        // or several device providers).
        // To do that, the report tasks must run even if one of their dependent tasks (flavor
        // or specific provider tasks) fails, when --continue is used, and the report task is
        // meant to run (== is in the task graph).
        // To do this, we make the children tasks ignore their errors (ie they won't fail and
        // stop the build).
        if (!reportTasks.isEmpty() && project.gradle.startParameter.continueOnFailure) {
            project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
                for (String reportTask : reportTasks) {
                    if (taskGraph.hasTask(reportTask)) {
                        tasks.named(reportTask) { AndroidReportTask task ->
                            task.setWillRun()
                        }
                    }
                }
            }
        }
    }

    public enum TestType {

        APPLICATION(DeviceProviderInstrumentTestTask.class),
        LIBRARY(DeviceProviderInstrumentTestLibraryTask.class),

        final private Class<? extends DeviceProviderInstrumentTestTask> taskType;

        TestType(Class<? extends DeviceProviderInstrumentTestTask> provider) {
            taskType = provider;
        }
    }

    protected void createConnectedTestForVariantData(
            TaskFactory tasks,
            final TestVariantData testVariantData,
            TestType testType) {
        BaseVariantData<? extends BaseVariantOutputData> baseVariantData =
                testVariantData.testedVariantData as BaseVariantData

        // get single output for now
        BaseVariantOutputData variantOutputData = baseVariantData.outputs.get(0)
        BaseVariantOutputData testVariantOutputData = testVariantData.outputs.get(0)

        String connectedRootName = "${CONNECTED}${ANDROID_TEST.suffix}"

        String connectedTaskName =
                "${connectedRootName}${baseVariantData.variantConfiguration.fullName.capitalize()}"

        TestData testData = new TestDataImpl(testVariantData)
        BaseVariantData<? extends BaseVariantOutputData> testedVariantData =
                baseVariantData as BaseVariantData
        // create the check tasks for this test
        // first the connected one.
        ImmutableList<Task> artifactsTasks = ImmutableList.of(
                testVariantData.outputs.get(0).assembleTask,
                testedVariantData.assembleVariantTask)

        DeviceProviderInstrumentTestTask connectedTask =
                createDeviceProviderInstrumentTestTask(
                        connectedTaskName,
                        "Installs and runs the tests for ${baseVariantData.description} on connected devices.",
                        testType.taskType,
                        testData,
                        artifactsTasks,
                        new ConnectedDeviceProvider(sdkHandler.getSdkInfo().adb),
                        CONNECTED
                )

        tasks.named(connectedRootName) {
            it.dependsOn connectedTask
        }
        testVariantData.connectedTestTask = connectedTask

        if (baseVariantData.variantConfiguration.buildType.isTestCoverageEnabled()) {
            def reportTask = project.tasks.create(
                    "create${baseVariantData.variantConfiguration.fullName.capitalize()}CoverageReport",
                    JacocoReportTask)
            reportTask.reportName = baseVariantData.variantConfiguration.fullName
            conventionMapping(reportTask).map("jacocoClasspath") {
                project.configurations[JacocoPlugin.ANT_CONFIGURATION_NAME]
            }
            conventionMapping(reportTask).map("coverageFile") {
                new File(connectedTask.getCoverageDir(),
                        SimpleTestCallable.FILE_COVERAGE_EC)
            }
            conventionMapping(reportTask).map("classDir") {
                return baseVariantData.javaCompileTask.destinationDir
            }
            conventionMapping(reportTask).
                    map("sourceDir") { baseVariantData.getJavaSourceFoldersForCoverage() }

            conventionMapping(reportTask).map("reportDir") {
                project.file(
                        "$project.buildDir/$FD_REPORTS/coverage/${baseVariantData.variantConfiguration.dirName}")
            }

            reportTask.dependsOn connectedTask
            tasks.named(connectedRootName) {
                it.dependsOn reportTask
            }
        }

        String mainProviderTaskName = "${DEVICE}${ANDROID_TEST.suffix}"

        List<DeviceProvider> providers = getExtension().deviceProviders

        boolean hasFlavors = baseVariantData.variantConfiguration.hasFlavors()

        // now the providers.
        for (DeviceProvider deviceProvider : providers) {
            DeviceProviderInstrumentTestTask providerTask =
                    createDeviceProviderInstrumentTestTask(
                            hasFlavors ?
                                    "${deviceProvider.name}${ANDROID_TEST.suffix}${baseVariantData.variantConfiguration.fullName.capitalize()}" :
                                    "${deviceProvider.name}${ANDROID_TEST.suffix}",
                            "Installs and runs the tests for Build '${baseVariantData.variantConfiguration.fullName}' using Provider '${deviceProvider.name.capitalize()}'.",
                            testType.taskType,
                            testData,
                            artifactsTasks,
                            deviceProvider,
                            "$DEVICE/$deviceProvider.name"
                    )

            tasks.named(mainProviderTaskName) {
                it.dependsOn providerTask
            }
            testVariantData.providerTestTaskList.add(providerTask)

            if (!deviceProvider.isConfigured()) {
                providerTask.enabled = false;
            }
        }

        // now the test servers
        // don't use an auto loop as it'll break the closure inside.
        List<TestServer> servers = getExtension().testServers
        for (TestServer testServer : servers) {
            def serverTask = project.tasks.create(
                    hasFlavors ?
                            "${testServer.name}${"upload".capitalize()}${baseVariantData.variantConfiguration.fullName}" :
                            "${testServer.name}${"upload".capitalize()}",
                    TestServerTask)

            serverTask.description =
                    "Uploads APKs for Build '${baseVariantData.variantConfiguration.fullName}' to Test Server '${testServer.name.capitalize()}'."
            serverTask.group = JavaBasePlugin.VERIFICATION_GROUP
            serverTask.dependsOn testVariantOutputData.assembleTask,
                    variantOutputData.assembleTask

            serverTask.testServer = testServer

            conventionMapping(serverTask).
                    map("testApk") { testVariantOutputData.outputFile }
            if (!(baseVariantData instanceof LibraryVariantData)) {
                conventionMapping(serverTask).
                        map("testedApk") { variantOutputData.outputFile }
            }

            conventionMapping(serverTask).
                    map("variantName") { baseVariantData.variantConfiguration.fullName }

            tasks.named(DEVICE_CHECK) {
                it.dependsOn serverTask
            }

            if (!testServer.isConfigured()) {
                serverTask.enabled = false;
            }
        }
    }

    protected DeviceProviderInstrumentTestTask createDeviceProviderInstrumentTestTask(
            @NonNull String taskName,
            @NonNull String description,
            @NonNull Class<? extends DeviceProviderInstrumentTestTask> taskClass,
            @NonNull TestData testData,
            @NonNull List<Task> artifactsTasks,
            @NonNull DeviceProvider deviceProvider,
            @NonNull String subFolder) {
        DeviceProviderInstrumentTestTask testTask = project.tasks.create(
                taskName,
                taskClass as Class<DeviceProviderInstrumentTestTask>)

        testTask.description = description
        testTask.group = JavaBasePlugin.VERIFICATION_GROUP

        for (Task task : artifactsTasks) {
            testTask.dependsOn task
        }

        testTask.androidBuilder = androidBuilder
        testTask.testData = testData
        testTask.flavorName = testData.getFlavorName()
        testTask.deviceProvider = deviceProvider
        testTask.installOptions = getExtension().getAdbOptions().getInstallOptions();

        conventionMapping(testTask).map("resultsDir") {
            String rootLocation = getExtension().testOptions.resultsDir != null ?
                    getExtension().testOptions.resultsDir :
                    "$project.buildDir/${FD_OUTPUTS}/$FD_ANDROID_RESULTS"

            String flavorFolder = testData.getFlavorName()
            if (!flavorFolder.isEmpty()) {
                flavorFolder = "$FD_FLAVORS/" + flavorFolder
            }

            project.file("$rootLocation/$subFolder/$flavorFolder")
        }

        conventionMapping(testTask).map("adbExec") {
            return sdkHandler.getSdkInfo().getAdb()
        }

        conventionMapping(testTask).map("splitSelectExec") {
            String path = androidBuilder.targetInfo?.buildTools?.getPath(
                    BuildToolInfo.PathId.SPLIT_SELECT)
            if (path != null) {
                File splitSelectExe = new File(path)
                return splitSelectExe.exists() ? splitSelectExe : null;
            } else {
                return null;
            }
        }
        testTask.processExecutor = androidBuilder.getProcessExecutor()


        conventionMapping(testTask).map("reportsDir") {
            String rootLocation = getExtension().testOptions.reportDir != null ?
                    getExtension().testOptions.reportDir :
                    "$project.buildDir/$FD_REPORTS/$FD_ANDROID_TESTS"

            String flavorFolder = testData.getFlavorName()
            if (!flavorFolder.isEmpty()) {
                flavorFolder = "$FD_FLAVORS/" + flavorFolder
            }

            project.file("$rootLocation/$subFolder/$flavorFolder")
        }
        conventionMapping(testTask).map("coverageDir") {
            String rootLocation = "$project.buildDir/${FD_OUTPUTS}/code-coverage"

            String flavorFolder = testData.getFlavorName()
            if (!flavorFolder.isEmpty()) {
                flavorFolder = "$FD_FLAVORS/" + flavorFolder
            }

            project.file("$rootLocation/$subFolder/$flavorFolder")
        }

        return testTask
    }

    /**
     * Class to hold data to setup the many optional
     * post-compilation steps.
     */
    public static class PostCompilationData {

        List<?> classGeneratingTask

        List<?> libraryGeneratingTask

        Closure<List<File>> inputFiles

        Closure<File> inputDir

        Closure<List<File>> inputLibraries
    }

    public void createJarTask(TaskFactory tasks, @NonNull final VariantScope scope) {
        BaseVariantData variantData = scope.variantData;

        GradleVariantConfiguration config = variantData.variantConfiguration
        tasks.create("jar${config.fullName.capitalize()}Classes", AndroidJarTask) { AndroidJarTask jarTask ->
            //        AndroidJarTask jarTask = project.tasks.create(
            //                "jar${config.fullName.capitalize()}Classes",
            //                AndroidJarTask)

            jarTask.setArchiveName("classes.jar");
            jarTask.setDestinationDir(new File(
                    "$scope.globalScope.buildDir/${FD_INTERMEDIATES}/packaged/${config.dirName}/"))
            jarTask.from(scope.javaOutputDir);
            jarTask.dependsOn scope.javaCompileTask.name
            variantData.binayFileProviderTask = jarTask
        }
    }

    /**
     * Creates the post-compilation tasks for the given Variant.
     *
     * These tasks create the dex file from the .class files, plus optional intermediary steps
     * like proguard and jacoco
     *
     * @param variantData the variant data.
     */
    public void createPostCompilationTasks(TaskFactory tasks, @NonNull final VariantScope scope) {
        ApkVariantData variantData = (ApkVariantData) scope.variantData;
        GradleVariantConfiguration config = variantData.variantConfiguration

        // data holding dependencies and input for the dex. This gets updated as new
        // post-compilation steps are inserted between the compilation and dx.
        PostCompilationData pcData = new PostCompilationData()
        pcData.classGeneratingTask = [scope.javaCompileTask.name]
        pcData.libraryGeneratingTask =
                [variantData.variantDependency.packageConfiguration.buildDependencies]
        pcData.inputFiles = {
            variantData.javaCompileTask.outputs.files.files as List
        }
        pcData.inputDir = {
            scope.javaOutputDir
        }
        pcData.inputLibraries = {
            scope.globalScope.androidBuilder.getPackagedJars(config) as List
        }

        // ---- Code Coverage first -----
        boolean isTestCoverageEnabled = config.buildType.isTestCoverageEnabled() &&
                !config.type.isForTesting()
        if (isTestCoverageEnabled) {
            pcData = createJacocoTask(tasks, scope, pcData);
        }

        boolean isTestForApp = config.type.isForTesting() &&
                (variantData as TestVariantData).testedVariantData.variantConfiguration.type ==
                DEFAULT
        boolean isMinifyEnabled = config.isMinifyEnabled()
        boolean isMultiDexEnabled = config.isMultiDexEnabled() && !isTestForApp
        boolean isLegacyMultiDexMode = config.isLegacyMultiDexMode()

        // ----- Minify next ----
        File outFile = maybeCreateProguardTasks(tasks, scope, pcData);
        if (outFile != null) {
            pcData.inputFiles = { [outFile] }
            pcData.inputLibraries = { [] }
        } else if ((getExtension().dexOptions.preDexLibraries && !isMultiDexEnabled) ||
                (isMultiDexEnabled && !isLegacyMultiDexMode)) {

            AndroidTask<PreDex> preDexTask =
                    androidTasks.create(tasks, new PreDex.ConfigAction(scope, pcData))

            // update dependency.
            preDexTask.dependsOn(tasks, pcData.libraryGeneratingTask)
            pcData.libraryGeneratingTask = [preDexTask.name] as List<Object>

            // update inputs
            if (isMultiDexEnabled) {
                pcData.inputLibraries = { [] }

            } else {
                pcData.inputLibraries = {
                    project.fileTree(scope.getPreDexOutputDir()).files as List
                }
            }
        }

        AndroidTask<CreateMainDexList> createMainDexListTask = null;
        AndroidTask<RetraceMainDexList> retraceTask = null;

        // ----- Multi-Dex support
        if (isMultiDexEnabled && isLegacyMultiDexMode) {
            if (!isMinifyEnabled) {
                // create a task that will convert the output of the compilation
                // into a jar. This is needed by the multi-dex input.
                AndroidTask<JarMergingTask> jarMergingTask = androidTasks.create(tasks,
                        new JarMergingTask.ConfigAction(scope, pcData));

                // update dependencies
                jarMergingTask.optionalDependsOn(
                        tasks,
                        pcData.classGeneratingTask,
                        pcData.libraryGeneratingTask)
                pcData.libraryGeneratingTask = [jarMergingTask.name]
                pcData.classGeneratingTask = [jarMergingTask.name]

                // Update the inputs
                pcData.inputFiles = { [scope.getJarMergingOutputFile()] }
                pcData.inputDir = null
                pcData.inputLibraries = { [] }
            }

            // ----------
            // Create a task to collect the list of manifest entry points which are
            // needed in the primary dex
            AndroidTask<CreateManifestKeepList> manifestKeepListTask = androidTasks.create(tasks,
                    new CreateManifestKeepList.ConfigAction(scope, pcData))

            // ----------
            // Create a proguard task to shrink the classes to manifest components
            AndroidTask<ProGuardTask> proguardComponentsTask =
                    androidTasks.create(tasks, new ProGuardTaskConfigAction(scope, pcData))

            // update dependencies
            proguardComponentsTask.dependsOn tasks, manifestKeepListTask
            proguardComponentsTask.optionalDependsOn(tasks,
                    pcData.classGeneratingTask,
                    pcData.libraryGeneratingTask)

            // ----------
            // Compute the full list of classes for the main dex file
            createMainDexListTask = androidTasks.create(tasks, new CreateMainDexList.ConfigAction(scope, pcData))
            createMainDexListTask.dependsOn(tasks, proguardComponentsTask)
            //createMainDexListTask.dependsOn { proguardMainDexTask }

            // ----------
            // If proguard is enabled, create a de-obfuscated list to aid debugging.
            if (isMinifyEnabled) {
                retraceTask = androidTasks.create(tasks,
                        new RetraceMainDexList.ConfigAction(scope, pcData))
                retraceTask.dependsOn(tasks, variantData.obfuscationTask, createMainDexListTask)
            }
        }

        AndroidTask<Dex> dexTask = androidTasks.create(tasks, new Dex.ConfigAction(scope, pcData));
        scope.setDexTask(dexTask);

        // dependencies, some of these could be null
        dexTask.optionalDependsOn(tasks,
                pcData.classGeneratingTask,
                pcData.libraryGeneratingTask,
                createMainDexListTask,
                retraceTask)
    }

    public PostCompilationData createJacocoTask(
            @NonNull TaskFactory tasks,
            @NonNull VariantScope scope,
            @NonNull final PostCompilationData pcData) {
        AndroidTask<JacocoInstrumentTask> jacocoTask = androidTasks.create(tasks,
                new JacocoInstrumentTask.ConfigAction(scope, pcData));

        jacocoTask.optionalDependsOn(tasks, pcData.classGeneratingTask)

        Copy agentTask = getJacocoAgentTask()
        jacocoTask.dependsOn(tasks, agentTask)

        // update dependency.
        PostCompilationData pcData2 = new PostCompilationData()
        pcData2.classGeneratingTask = [jacocoTask.name]
        pcData2.libraryGeneratingTask = [pcData.libraryGeneratingTask, agentTask]

        // update inputs
        pcData2.inputFiles = {
            project.files(scope.variantData.jacocoInstrumentTask.getOutputDir()).files as List
        }
        pcData2.inputDir = {
            scope.variantData.jacocoInstrumentTask.getOutputDir()
        }
        pcData2.inputLibraries = {
            [pcData.inputLibraries.call(), [new File(agentTask.destinationDir, FILE_JACOCO_AGENT)]].
                    flatten() as List
        }

        return pcData2
    }

    private static ProGuardTask createShrinkingProGuardTask(
            @NonNull Project project,
            @NonNull String name) {
        ProGuardTask task = project.tasks.create(name, ProGuardTask)

        task.dontobfuscate()
        task.dontoptimize()
        task.dontpreverify()
        task.dontwarn()
        task.forceprocessing()

        return task;
    }

    public void createJackTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @Nullable BaseVariantData<? extends BaseVariantOutputData> testedVariantData) {

        GradleVariantConfiguration config = variantData.variantConfiguration

        // ----- Create Jill tasks -----
        JillTask jillRuntimeTask = project.tasks.create(
                "jill${config.fullName.capitalize()}RuntimeLibraries",
                JillTask)

        jillRuntimeTask.androidBuilder = androidBuilder
        jillRuntimeTask.dexOptions = getExtension().dexOptions

        conventionMapping(jillRuntimeTask).map("inputLibs") {
            androidBuilder.getBootClasspath()
        }
        conventionMapping(jillRuntimeTask).map("outputFolder") {
            project.file(
                    "${project.buildDir}/${FD_INTERMEDIATES}/jill/${config.dirName}/runtime")
        }

        // ----

        JillTask jillPackagedTask = project.tasks.create(
                "jill${config.fullName.capitalize()}PackagedLibraries",
                JillTask)

        jillPackagedTask.dependsOn variantData.variantDependency.packageConfiguration.buildDependencies
        jillPackagedTask.androidBuilder = androidBuilder
        jillPackagedTask.dexOptions = getExtension().dexOptions

        conventionMapping(jillPackagedTask).map("inputLibs") {
            androidBuilder.getPackagedJars(config)
        }
        conventionMapping(jillPackagedTask).map("outputFolder") {
            project.file(
                    "${project.buildDir}/${FD_INTERMEDIATES}/jill/${config.dirName}/packaged")
        }

        // ----- Create Jack Task -----
        JackTask compileTask = project.tasks.create(
                "compile${config.fullName.capitalize()}JavaWithJack",
                JackTask)
        compileTask.isVerbose = isVerbose()
        compileTask.isDebugLog = isDebugLog()

        // Jack is compiling and also providing the binary and mapping files.
        variantData.javaCompileTask = compileTask
        variantData.mappingFileProviderTask = compileTask
        variantData.binayFileProviderTask = compileTask

        variantData.javaCompileTask.dependsOn variantData.sourceGenTask, jillRuntimeTask, jillPackagedTask
        variantData.compileTask.dependsOn variantData.javaCompileTask
        // TODO - dependency information for the compile classpath is being lost.
        // Add a temporary approximation
        compileTask.dependsOn variantData.variantDependency.compileConfiguration.buildDependencies

        compileTask.androidBuilder = androidBuilder
        conventionMapping(compileTask).
                map("javaMaxHeapSize") { getExtension().dexOptions.getJavaMaxHeapSize() }

        compileTask.source = variantData.getJavaSources()

        compileTask.multiDexEnabled = config.isMultiDexEnabled()
        compileTask.minSdkVersion = config.minSdkVersion.apiLevel

        // if the tested variant is an app, add its classpath. For the libraries,
        // it's done automatically since the classpath includes the library output as a normal
        // dependency.
        if (testedVariantData instanceof ApplicationVariantData) {
            JackTask jackTask = (JackTask) testedVariantData.javaCompileTask
            conventionMapping(compileTask).map("classpath") {
                project.fileTree(jillRuntimeTask.outputFolder) +
                        jackTask.classpath +
                        project.fileTree(jackTask.jackFile)
            }
        } else {
            conventionMapping(compileTask).map("classpath") {
                project.fileTree(jillRuntimeTask.outputFolder)
            }
        }

        conventionMapping(compileTask).map("packagedLibraries") {
            project.fileTree(jillPackagedTask.outputFolder).files
        }

        conventionMapping(compileTask).map("destinationDir") {
            project.file("$project.buildDir/${FD_INTERMEDIATES}/dex/${config.dirName}")
        }

        conventionMapping(compileTask).map("jackFile") {
            project.file(
                    "$project.buildDir/${FD_INTERMEDIATES}/packaged/${config.dirName}/classes.zip")
        }

        conventionMapping(compileTask).map("tempFolder") {
            project.file("$project.buildDir/${FD_INTERMEDIATES}/tmp/jack/${config.dirName}")
        }
        if (config.isMinifyEnabled()) {
            conventionMapping(compileTask).map("proguardFiles") {
                // since all the output use the same resources, we can use the first output
                // to query for a proguard file.
                BaseVariantOutputData variantOutputData = variantData.outputs.get(0)

                List<File> proguardFiles = config.getProguardFiles(true /*includeLibs*/,
                        [getExtension().getDefaultProguardFile(DEFAULT_PROGUARD_CONFIG_FILE)])
                File proguardResFile = variantOutputData.processResourcesTask.proguardOutputFile
                if (proguardResFile != null) {
                    proguardFiles.add(proguardResFile)
                }
                // for tested app, we only care about their aapt config since the base
                // configs are the same files anyway.
                if (testedVariantData != null) {
                    // use single output for now.
                    proguardResFile =
                            testedVariantData.outputs.get(0).processResourcesTask.proguardOutputFile
                    if (proguardResFile != null) {
                        proguardFiles.add(proguardResFile)
                    }
                }

                return proguardFiles
            }

            compileTask.mappingFile = project.file(
                    "${project.buildDir}/${FD_OUTPUTS}/mapping/${variantData.variantConfiguration.dirName}/mapping.txt")
        }

        configureLanguageLevel(compileTask)
    }

    /**
     * Configures the source and target language level of a compile task. If the user has set it
     * explicitly, we obey the setting. Otherwise we change the default language level based on the
     * compile SDK version.
     *
     * <p>This method modifies getExtension().compileOptions, to propagate the language level to Studio.
     */
    private void configureLanguageLevel(AbstractCompile compileTask) {
        def compileOptions = getExtension().compileOptions
        JavaVersion javaVersionToUse

        Integer compileSdkLevel =
                AndroidTargetHash.getVersionFromHash(getExtension().compileSdkVersion)?.apiLevel
        switch (compileSdkLevel) {
            case null:  // Default to 1.6 if we fail to parse compile SDK version.
            case 0..20:
                javaVersionToUse = JavaVersion.VERSION_1_6
                break
            default:
                javaVersionToUse = JavaVersion.VERSION_1_7
                break
        }

        def jdkVersion = JavaVersion.toVersion(System.getProperty("java.specification.version"))
        if (jdkVersion < javaVersionToUse) {
            logger.info(
                    "Default language level for 'compileSdkVersion $compileSdkLevel' is " +
                            "$javaVersionToUse, but the JDK used is $jdkVersion, so the JDK " +
                            "language level will be used.")
            javaVersionToUse = jdkVersion
        }

        compileOptions.defaultJavaVersion = javaVersionToUse

        conventionMapping(compileTask).map("sourceCompatibility") {
            compileOptions.sourceCompatibility.toString()
        }
        conventionMapping(compileTask).map("targetCompatibility") {
            compileOptions.targetCompatibility.toString()
        }
    }

    /**
     * Creates the final packaging task, and optionally the zipalign task (if the variant is signed)
     * @param variantData
     * @param assembleTask an optional assembleTask to be used. If null a new one is created. The
     *                assembleTask is always set in the Variant.
     * @param publishApk if true the generated APK gets published.
     */
    public void createPackagingTask(
            @NonNull TaskFactory tasks,
            @NonNull VariantScope variantScope,
            boolean publishApk) {
        ApkVariantData variantData = (ApkVariantData) variantScope.getVariantData()

        GradleVariantConfiguration config = variantData.variantConfiguration
        boolean signedApk = variantData.isSigned()
        // use a dynamic property invocation due to gradle issue.
        String defaultLocation = "$project.buildDir/${FD_OUTPUTS}/apk"
        String apkLocation = defaultLocation
        if (project.hasProperty(PROPERTY_APK_LOCATION)) {
            apkLocation = project.getProperties().get(PROPERTY_APK_LOCATION)
        }
        SigningConfig sc = (SigningConfig) config.signingConfig

        boolean multiOutput = variantData.outputs.size() > 1

        // loop on all outputs. The only difference will be the name of the task, and location
        // of the generated data.
        for (ApkVariantOutputData vod : variantData.outputs) {
            VariantOutputScope variantOutputScope = new VariantOutputScope(variantScope, vod);

            // create final var inside the loop to ensure the closures will work.
            final ApkVariantOutputData variantOutputData = vod

            String outputName = variantOutputData.fullName

            // When shrinking resources, rather than having the packaging task
            // directly map to the packageOutputFile of ProcessAndroidResources,
            // we insert the ShrinkResources task into the chain, such that its
            // input is the ProcessAndroidResources packageOutputFile, and its
            // output is what the PackageApplication task reads.
            AndroidTask<ShrinkResources> shrinkTask = null;

            if (config.isMinifyEnabled() && config.getBuildType().isShrinkResources() && !config
                    .getUseJack()) {
                shrinkTask = androidTasks.create(tasks,
                        new ShrinkResources.ConfigAction(variantOutputScope));
                shrinkTask.dependsOn(tasks,
                        variantData.obfuscationTask,
                        variantOutputData.manifestProcessorTask,
                        variantOutputData.processResourcesTask);
            }

            AndroidTask<PackageApplication> packageApp = androidTasks.create(tasks,
                    new PackageApplication.ConfigAction(variantOutputScope));

            packageApp.dependsOn(tasks,
                    variantOutputData.processResourcesTask,
                    variantData.processJavaResourcesTask,
                    variantOutputScope.getVariantScope().getNdkBuildable());

            packageApp.optionalDependsOn(
                    tasks,
                    shrinkTask,
                    variantOutputScope.variantScope.dexTask,
                    variantOutputScope.variantScope.javaCompileTask,
                    variantData.javaCompileTask,  // TODO: Remove when Jack is converted to AndroidTask.
                    variantOutputData.packageSplitResourcesTask,
                    variantOutputData.packageSplitAbiTask);

            AndroidTask appTask = packageApp

            if (signedApk) {
                if (variantData.zipAlignEnabled) {
                    AndroidTask<ZipAlign> zipAlignTask = androidTasks.create(
                            tasks,
                            new ZipAlign.ConfigAction(variantOutputScope));
                    zipAlignTask.dependsOn(tasks, packageApp);
                    if (variantOutputData.splitZipAlign != null) {
                        zipAlignTask.dependsOn(tasks, variantOutputData.splitZipAlign)
                    }
                    appTask = zipAlignTask
                }
            }

            assert variantData.assembleVariantTask != null

            // Add an assemble task
            if (multiOutput) {
                // create a task for this output
                variantOutputData.assembleTask = createAssembleTask(variantOutputData)

                // variant assemble task depends on each output assemble task.
                variantData.assembleVariantTask.dependsOn variantOutputData.assembleTask
            } else {
                // single output
                variantOutputData.assembleTask = variantData.assembleVariantTask
            }

            if (!signedApk && variantOutputData.packageSplitResourcesTask != null) {
                // in case we are not signing the resulting APKs and we have some pure splits
                // we should manually copy them from the intermediate location to the final
                // apk location unmodified.
                Copy copyTask = project.tasks.create(
                        "copySplit${outputName.capitalize()}",
                        Copy)
                copyTask.destinationDir = new File(apkLocation as String);
                copyTask.from(variantOutputData.packageSplitResourcesTask.getOutputDirectory())
                variantOutputData.assembleTask.dependsOn(copyTask)
                copyTask.mustRunAfter(appTask.name)
            }

            variantOutputData.assembleTask.dependsOn appTask.name

            if (publishApk) {
                String projectBaseName = globalScope.projectBaseName;

                // if this variant is the default publish config or we also should publish non
                // defaults, proceed with declaring our artifacts.
                if (getExtension().defaultPublishConfig.equals(outputName)) {
                    appTask.configure(tasks) { FileSupplier packageTask ->
                        project.artifacts.add("default", new ApkPublishArtifact(
                                projectBaseName,
                                null,
                                packageTask))
                    }

                    for (FileSupplier outputFileProvider :
                            variantOutputData.getSplitOutputFileSuppliers()) {
                        project.artifacts.add("default", new ApkPublishArtifact(
                                projectBaseName,
                                null,
                                outputFileProvider))
                    }

                    if (variantOutputData.getMetadataFile() != null) {
                        project.artifacts.add("default-metadata",
                                new MetadataPublishArtifact(projectBaseName, null,
                                        variantOutputData.getMetadataFile()));
                    }

                    if (variantData.getMappingFileProvider() != null) {
                        project.artifacts.add("default-mapping",
                                new MappingPublishArtifact(projectBaseName,
                                        null,
                                        variantData.getMappingFileProvider()));
                    }
                }

                if (getExtension().publishNonDefault) {
                    appTask.configure(tasks) { FileSupplier packageTask ->
                        project.artifacts.add(
                                variantData.variantDependency.publishConfiguration.name,
                                new ApkPublishArtifact(
                                        projectBaseName,
                                        null,
                                        packageTask))
                    }

                    for (FileSupplier outputFileProvider :
                            variantOutputData.getSplitOutputFileSuppliers()) {
                        project.artifacts.add(
                                variantData.variantDependency.publishConfiguration.name,
                                new ApkPublishArtifact(
                                    projectBaseName,
                                    null,
                                    outputFileProvider))
                    }

                    if (variantOutputData.getMetadataFile() != null) {
                        project.artifacts.add(
                                variantData.variantDependency.metadataConfiguration.name,
                                new MetadataPublishArtifact(projectBaseName, null,
                                        variantOutputData.getMetadataFile()));
                    }

                    if (variantData.getMappingFileProvider() != null) {
                        project.artifacts.add(
                                variantData.variantDependency.mappingConfiguration.name,
                                new MappingPublishArtifact(projectBaseName,
                                        null,
                                        variantData.getMappingFileProvider()));
                    }

                    if (variantData.classesJarTask != null) {
                        project.artifacts.add(
                                variantData.variantDependency.classesConfiguration.name,
                                variantData.classesJarTask)
                    }
                }
            }
        }

        // create install task for the variant Data. This will deal with finding the
        // right output if there are more than one.
        // Add a task to install the application package
        if (signedApk) {
            AndroidTask<InstallVariantTask> installTask = androidTasks.create(
                    tasks,
                    "install${config.fullName.capitalize()}",
                    InstallVariantTask,
                    new InstallVariantTask.ConfigAction(variantScope));
            installTask.dependsOn(tasks, variantData.assembleVariantTask)
        }


        if (getExtension().lintOptions.checkReleaseBuilds) {
            createLintVitalTask(variantData)
        }

        // add an uninstall task
        AndroidTask<UninstallTask> uninstallTask = androidTasks.create(
                tasks,
                "uninstall${variantData.variantConfiguration.fullName.capitalize()}",
                UninstallTask,
                new UninstallTask.ConfigAction(variantScope));
        tasks.named(UNINSTALL_ALL) {
            it.dependsOn uninstallTask.name
        }
    }

    public Task createAssembleTask(
            @NonNull BaseVariantOutputData variantOutputData) {
        Task assembleTask = project.tasks.
                create("assemble${variantOutputData.fullName.capitalize()}")
        return assembleTask
    }

    public Task createAssembleTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        Task assembleTask = project.tasks.
                create("assemble${variantData.variantConfiguration.fullName.capitalize()}")
        assembleTask.description = "Assembles the ${variantData.description}."
        assembleTask.group = BUILD_GROUP
        return assembleTask
    }

    public Copy getJacocoAgentTask() {
        if (jacocoAgentTask == null) {
            jacocoAgentTask = project.tasks.create("unzipJacocoAgent", Copy)
            jacocoAgentTask.from {
                project.configurations.getByName(JacocoPlugin.AGENT_CONFIGURATION_NAME).
                        collect { project.zipTree(it) }
            }
            jacocoAgentTask.include FILE_JACOCO_AGENT
            jacocoAgentTask.into "$project.buildDir/${FD_INTERMEDIATES}/jacoco"
        }

        return jacocoAgentTask
    }

    /**
     * creates a zip align. This does not use convention mapping,
     * and is meant to let other plugin create zip align tasks.
     *
     * @param name the name of the task
     * @param inputFile the input file
     * @param outputFile the output file
     *
     * @return the task
     */
    @NonNull
    ZipAlign createZipAlignTask(
            @NonNull String name,
            @NonNull File inputFile,
            @NonNull File outputFile) {
        // Add a task to zip align application package
        def zipAlignTask = project.tasks.create(name, ZipAlign)

        zipAlignTask.inputFile = inputFile
        zipAlignTask.outputFile = outputFile
        conventionMapping(zipAlignTask).map("zipAlignExe") {
            String path = androidBuilder.targetInfo?.buildTools?.getPath(ZIP_ALIGN)
            if (path != null) {
                return new File(path)
            }

            return null
        }

        return zipAlignTask
    }

    /**
     * Creates the proguarding task for the given Variant if necessary.
     * @param variantData the variant data.
     * @param testedVariantData optional. variant data representing the tested variant, null if the
     *                          variant is not a test variant
     * @return null if the proguard task was not created, otherwise the expected outputFile.
     */
    @Nullable
    public File maybeCreateProguardTasks(
            TaskFactory tasks,
            VariantScope scope,
            @NonNull final PostCompilationData pcData) {
        if (!scope.variantData.getVariantConfiguration().isMinifyEnabled()) {
            return null;
        }

        AndroidTask<AndroidProGuardTask> proguardTask =
                androidTasks.create(tasks, new AndroidProGuardTask.ConfigAction(scope, pcData));

        // update dependency.
        proguardTask.optionalDependsOn(tasks, pcData.classGeneratingTask, pcData.libraryGeneratingTask)
        pcData.libraryGeneratingTask = [proguardTask.name]
        pcData.classGeneratingTask = [proguardTask.name]

        // Return output file.
        return scope.getProguardOutputFile();
    }

    public void createReportTasks(
            List<BaseVariantData<? extends BaseVariantOutputData>> variantDataList) {
        def dependencyReportTask = project.tasks.create("androidDependencies", DependencyReportTask)
        dependencyReportTask.setDescription("Displays the Android dependencies of the project.")
        dependencyReportTask.setVariants(variantDataList)
        dependencyReportTask.setGroup(ANDROID_GROUP)

        def signingReportTask = project.tasks.create("signingReport", SigningReportTask)
        signingReportTask.setDescription("Displays the signing info for each variant.")
        signingReportTask.setVariants(variantDataList)
        signingReportTask.setGroup(ANDROID_GROUP)
    }

    public void createAnchorTasks(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        createPreBuildTasks(variantData)

        // also create sourceGenTask
        variantData.sourceGenTask = project.tasks.create(
                "generate${variantData.variantConfiguration.fullName.capitalize()}Sources")
        // and resGenTask
        variantData.resourceGenTask = project.tasks.create(
                "generate${variantData.variantConfiguration.fullName.capitalize()}Resources")
        variantData.assetGenTask = project.tasks.create(
                "generate${variantData.variantConfiguration.fullName.capitalize()}Assets")
        // and compile task
        createCompileAnchorTask(variantData)
    }

    private void createPreBuildTasks(BaseVariantData<? extends BaseVariantOutputData> variantData) {
        variantData.preBuildTask = project.tasks.create(
                "pre${variantData.variantConfiguration.fullName.capitalize()}Build")
        variantData.preBuildTask.dependsOn MAIN_PREBUILD

        def prepareDependenciesTask = project.tasks.create(
                "prepare${variantData.variantConfiguration.fullName.capitalize()}Dependencies",
                PrepareDependenciesTask)

        variantData.prepareDependenciesTask = prepareDependenciesTask
        prepareDependenciesTask.dependsOn variantData.preBuildTask

        prepareDependenciesTask.androidBuilder = androidBuilder
        prepareDependenciesTask.variant = variantData

        // for all libraries required by the configurations of this variant, make this task
        // depend on all the tasks preparing these libraries.
        VariantDependencies configurationDependencies = variantData.variantDependency
        prepareDependenciesTask.addChecker(configurationDependencies.checker)

        for (LibraryDependencyImpl lib : configurationDependencies.libraries) {
            dependencyManager.addDependencyToPrepareTask(variantData, prepareDependenciesTask, lib)
        }
    }

    private void createCompileAnchorTask(
            BaseVariantData<? extends BaseVariantOutputData> variantData) {
        variantData.compileTask = project.tasks.create(
                "compile${variantData.variantConfiguration.fullName.capitalize()}Sources")
        variantData.compileTask.group = BUILD_GROUP

        variantData.assembleVariantTask.dependsOn variantData.compileTask
    }

    public void createCheckManifestTask(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        String name = variantData.variantConfiguration.fullName
        variantData.checkManifestTask = project.tasks.create(
                "check${name.capitalize()}Manifest",
                CheckManifest)
        variantData.checkManifestTask.dependsOn variantData.preBuildTask

        variantData.prepareDependenciesTask.dependsOn variantData.checkManifestTask

        variantData.checkManifestTask.variantName = name
        conventionMapping(variantData.checkManifestTask).map("manifest") {
            variantData.variantConfiguration.getDefaultSourceSet().manifestFile
        }
    }

    public static void optionalDependsOn(@NonNull Task main, Task... dependencies) {
        for (Task dependency : dependencies) {
            if (dependency != null) {
                main.dependsOn dependency
            }
        }
    }

    public static void optionalDependsOn(@NonNull Task main, @NonNull List<?> dependencies) {
        for (Object dependency : dependencies) {
            if (dependency != null) {
                main.dependsOn dependency
            }
        }
    }

    private static <T> Set<T> removeAllNullEntries(Set<T> input) {
        HashSet<T> output = new HashSet<T>();
        for (T element : input) {
            if (element != null) {
                output.add(element);
            }
        }
        return output;
    }

    @NonNull
    private List<ManifestDependencyImpl> getManifestDependencies(
            List<LibraryDependency> libraries) {

        List<ManifestDependencyImpl> list = Lists.newArrayListWithCapacity(libraries.size())

        for (LibraryDependency lib : libraries) {
            // get the dependencies
            List<ManifestDependencyImpl> children = getManifestDependencies(lib.dependencies)
            list.add(new ManifestDependencyImpl(lib.getName(), lib.manifest, children))
        }

        return list
    }

    @NonNull
    protected static List<SymbolFileProviderImpl> getTextSymbolDependencies(
            List<LibraryDependency> libraries) {

        List<SymbolFileProviderImpl> list = Lists.newArrayListWithCapacity(libraries.size())

        for (LibraryDependency lib : libraries) {
            list.add(new SymbolFileProviderImpl(lib.manifest, lib.symbolFile))
        }

        return list
    }

    protected VariantScope createVariantScope(BaseVariantData variantData) {
        variantData.scope = new VariantScope(globalScope, variantData)
        return variantData.scope
    }
}
