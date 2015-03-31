package com.android.build.gradle.tasks;

import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.annotations.ApkFile;
import com.android.build.gradle.internal.core.GradleVariantConfiguration;
import com.android.build.gradle.internal.dsl.AbiSplitOptions;
import com.android.build.gradle.internal.dsl.PackagingOptions;
import com.android.build.gradle.internal.dsl.SigningConfig;
import com.android.build.gradle.internal.scope.ConventionMappingHelper;
import com.android.build.gradle.internal.scope.TaskConfigAction;
import com.android.build.gradle.internal.scope.VariantOutputScope;
import com.android.build.gradle.internal.tasks.FileSupplier;
import com.android.build.gradle.internal.tasks.IncrementalTask;
import com.android.build.gradle.internal.tasks.ValidateSigningTask;
import com.android.build.gradle.internal.variant.ApkVariantData;
import com.android.build.gradle.internal.variant.ApkVariantOutputData;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.builder.packaging.DuplicateFileException;
import com.android.utils.StringHelper;
import com.google.common.collect.ImmutableSet;

import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.Task;
import org.gradle.api.file.FileTree;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.ParallelizableTask;
import org.gradle.tooling.BuildException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

@ParallelizableTask
public class PackageApplication extends IncrementalTask implements FileSupplier {
    private File resourceFile;

    private File dexFolder;

    private Collection<File> dexedLibraries;

    private File javaResourceDir;

    private Set<File> jniFolders;

    @ApkFile
    private File outputFile;

    private Set<String> abiFilters;

    private Set<File> packagedJars;

    private boolean jniDebugBuild;

    private SigningConfig signingConfig;

    private PackagingOptions packagingOptions;

    // ----- PUBLIC TASK API -----

    @InputFile
    public File getResourceFile() {
        return resourceFile;
    }

    public void setResourceFile(File resourceFile) {
        this.resourceFile = resourceFile;
    }

    @InputDirectory
    public File getDexFolder() {
        return dexFolder;
    }

    public void setDexFolder(File dexFolder) {
        this.dexFolder = dexFolder;
    }

    @InputFiles
    public Collection<File> getDexedLibraries() {
        return dexedLibraries;
    }

    public void setDexedLibraries(Collection<File> dexedLibraries) {
        this.dexedLibraries = dexedLibraries;
    }

    @InputDirectory
    @Optional
    public File getJavaResourceDir() {
        return javaResourceDir;
    }

    public void setJavaResourceDir(File javaResourceDir) {
        this.javaResourceDir = javaResourceDir;
    }

    public Set<File> getJniFolders() {
        return jniFolders;
    }

    public void setJniFolders(Set<File> jniFolders) {
        this.jniFolders = jniFolders;
    }

    @OutputFile
    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    @Input
    @Optional
    public Set<String> getAbiFilters() {
        return abiFilters;
    }

    public void setAbiFilters(Set<String> abiFilters) {
        this.abiFilters = abiFilters;
    }

    // ----- PRIVATE TASK API -----

    @InputFiles
    public Set<File> getPackagedJars() {
        return packagedJars;
    }

    public void setPackagedJars(Set<File> packagedJars) {
        this.packagedJars = packagedJars;
    }

    @Input
    public boolean getJniDebugBuild() {
        return jniDebugBuild;
    }

    public boolean isJniDebugBuild() {
        return jniDebugBuild;
    }

    public void setJniDebugBuild(boolean jniDebugBuild) {
        this.jniDebugBuild = jniDebugBuild;
    }

    @Nested
    @Optional
    public SigningConfig getSigningConfig() {
        return signingConfig;
    }

    public void setSigningConfig(SigningConfig signingConfig) {
        this.signingConfig = signingConfig;
    }

    @Nested
    public PackagingOptions getPackagingOptions() {
        return packagingOptions;
    }

    public void setPackagingOptions(PackagingOptions packagingOptions) {
        this.packagingOptions = packagingOptions;
    }

    @InputFiles
    public FileTree getNativeLibraries() {
        FileTree src = null;
        Set<File> folders = getJniFolders();
        if (!folders.isEmpty()) {
            src = getProject().files(new ArrayList<Object>(folders)).getAsFileTree();
        }

        return src == null ? getProject().files().getAsFileTree() : src;
    }

    @Override
    protected void doFullTaskAction() {
        try {
            final File dir = getJavaResourceDir();
            getBuilder().packageApk(getResourceFile().getAbsolutePath(), getDexFolder(),
                    getDexedLibraries(), getPackagedJars(),
                    (dir == null ? null : dir.getAbsolutePath()), getJniFolders(), getAbiFilters(),
                    getJniDebugBuild(), getSigningConfig(), getPackagingOptions(),
                    getOutputFile().getAbsolutePath());
        } catch (DuplicateFileException e) {
            Logger logger = getLogger();
            logger.error("Error: duplicate files during packaging of APK " + getOutputFile()
                    .getAbsolutePath());
            logger.error("\tPath in archive: " + e.getArchivePath());
            logger.error("\tOrigin 1: " + e.getFile1());
            logger.error("\tOrigin 2: " + e.getFile2());
            logger.error("You can ignore those files in your build.gradle:");
            logger.error("\tandroid {");
            logger.error("\t  packagingOptions {");
            logger.error("\t    exclude \'" + e.getArchivePath() + "\'");
            logger.error("\t  }");
            logger.error("\t}");
            throw new BuildException(e.getMessage(), e);
        } catch (Exception e) {
            throw new BuildException(e.getMessage(), e);
        }

    }

    // ----- FileSupplierTask -----

    @Override
    public File get() {
        return getOutputFile();
    }

    @NonNull
    @Override
    public Task getTask() {
        return this;
    }

    // ----- ConfigAction -----

    public static class ConfigAction implements TaskConfigAction<PackageApplication> {

        private VariantOutputScope scope;

        public ConfigAction(VariantOutputScope scope) {
            this.scope = scope;
        }

        @Override
        public String getName() {
            return "package" + StringHelper.capitalize(scope.getVariantOutputData().getFullName());
        }

        @Override
        public Class<PackageApplication> getType() {
            return PackageApplication.class;
        }

        @Override
        public void execute(PackageApplication packageApp) {
            final ApkVariantData variantData = (ApkVariantData) scope.getVariantScope().getVariantData();
            final ApkVariantOutputData variantOutputData = (ApkVariantOutputData) scope
                    .getVariantOutputData();
            final GradleVariantConfiguration config = scope.getVariantScope().getVariantConfiguration();

            String outputName = variantOutputData.getFullName();
            String outputBaseName = variantOutputData.getBaseName();

            variantOutputData.packageApplicationTask = packageApp;
            packageApp.setAndroidBuilder(scope.getGlobalScope().getAndroidBuilder());

            if (config.isMinifyEnabled() && config.getBuildType().isShrinkResources() && !config
                    .getUseJack()) {
                final ShrinkResources shrinkTask = createShrinkResourcesTask(variantOutputData);

                // When shrinking resources, rather than having the packaging task
                // directly map to the packageOutputFile of ProcessAndroidResources,
                // we insert the ShrinkResources task into the chain, such that its
                // input is the ProcessAndroidResources packageOutputFile, and its
                // output is what the PackageApplication task reads.
                packageApp.dependsOn(shrinkTask);
                ConventionMappingHelper.map(packageApp, "resourceFile", new Callable<File>() {
                    @Override
                    public File call() {
                        return shrinkTask.getCompressedResources();
                    }
                });
            } else {
                ConventionMappingHelper.map(packageApp, "resourceFile", new Callable<File>() {
                    @Override
                    public File call() {
                        return variantOutputData.processResourcesTask.getPackageOutputFile();
                    }
                });
            }

            ConventionMappingHelper.map(packageApp, "dexFolder", new Callable<File>() {
                @Override
                public File call() {
                    if (variantData.dexTask != null) {
                        return variantData.dexTask.getOutputFolder();
                    }

                    if (variantData.javaCompileTask != null) {
                        return variantData.javaCompileTask.getDestinationDir();
                    }

                    return null;
                }
            });
            ConventionMappingHelper.map(packageApp, "dexedLibraries", new Callable<Collection<File>>() {
                @Override
                public Collection<File> call() {
                    if (config.isMultiDexEnabled() && !config.isLegacyMultiDexMode()
                            && variantData.preDexTask != null) {
                        return scope.getGlobalScope().getProject()
                                .fileTree(variantData.preDexTask.getOutputFolder()).getFiles();
                    }

                    return Collections.emptyList();
                }
            });
            ConventionMappingHelper.map(packageApp, "packagedJars", new Callable<Set<File>>() {
                @Override
                public Set<File> call() {
                    return scope.getGlobalScope().getAndroidBuilder().getPackagedJars(config);
                }
            });
            ConventionMappingHelper.map(packageApp, "javaResourceDir", new Callable<File>() {
                @Override
                public File call() {
                    return getOptionalDir(variantData.processJavaResourcesTask.getDestinationDir());
                }
            });
            ConventionMappingHelper.map(packageApp, "jniFolders", new Callable<Set<File>>() {
                @Override
                public Set<File> call() {

                    if (variantData.getSplitHandlingPolicy() ==
                            BaseVariantData.SplitHandlingPolicy.PRE_21_POLICY) {
                        return scope.getVariantScope().getJniFolders();
                    }
                    Set<String> filters = AbiSplitOptions.getAbiFilters(
                            scope.getGlobalScope().getExtension().getSplits().getAbiFilters());
                    return filters.isEmpty() ? scope.getVariantScope().getJniFolders() : Collections.<File>emptySet();

                }
            });

            ConventionMappingHelper.map(packageApp, "abiFilters", new Callable<Set<String>>() {
                @Override
                public Set<String> call() throws Exception {
                    if (variantOutputData.getMainOutputFile().getFilter(com.android.build.OutputFile.ABI) != null) {
                        return ImmutableSet.of(
                                variantOutputData.getMainOutputFile()
                                        .getFilter(com.android.build.OutputFile.ABI));
                    }
                    return config.getSupportedAbis();
                }
            });
            ConventionMappingHelper.map(packageApp, "jniDebugBuild", new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return config.getBuildType().isJniDebuggable();
                }
            });

            SigningConfig sc = (SigningConfig) config.getSigningConfig();
            packageApp.setSigningConfig(sc);
            if (sc != null) {
                String validateSigningTaskName = "validate" + StringHelper.capitalize(sc.getName()) + "Signing";
                ValidateSigningTask validateSigningTask =
                        (ValidateSigningTask) scope.getGlobalScope().getProject().getTasks().findByName(validateSigningTaskName);
                if (validateSigningTask == null) {
                    validateSigningTask =
                            scope.getGlobalScope().getProject().getTasks().create(
                                    "validate" + StringHelper.capitalize(sc.getName()) + "Signing",
                                    ValidateSigningTask.class);
                    validateSigningTask.setAndroidBuilder(scope.getGlobalScope().getAndroidBuilder());
                    validateSigningTask.setSigningConfig(sc);
                }

                packageApp.dependsOn(validateSigningTask);
            }

            ConventionMappingHelper.map(packageApp, "packagingOptions", new Callable<PackagingOptions>() {
                @Override
                public PackagingOptions call() throws Exception {
                    return scope.getGlobalScope().getExtension().getPackagingOptions();
                }
            });

            ConventionMappingHelper.map(packageApp, "outputFile", new Callable<File>() {
                @Override
                public File call() throws Exception {
                    return scope.getPackageApk();
                }
            });
        }

        private ShrinkResources createShrinkResourcesTask(
                final ApkVariantOutputData variantOutputData) {
            BaseVariantData<?> variantData = (BaseVariantData<?>) variantOutputData.variantData;
            ShrinkResources task = scope.getGlobalScope().getProject().getTasks()
                    .create("shrink" + StringGroovyMethods
                            .capitalize(variantOutputData.getFullName())
                            + "Resources", ShrinkResources.class);
            task.setAndroidBuilder(scope.getGlobalScope().getAndroidBuilder());
            task.variantOutputData = variantOutputData;

            final String outputBaseName = variantOutputData.getBaseName();
            task.setCompressedResources(new File(
                    scope.getGlobalScope().getBuildDir() + "/" + FD_INTERMEDIATES + "/resources/" +
                            "resources-" + outputBaseName + "-stripped.ap_"));

            ConventionMappingHelper.map(task, "uncompressedResources", new Callable<File>() {
                @Override
                public File call() {
                    return variantOutputData.processResourcesTask.getPackageOutputFile();
                }
            });

            task.dependsOn(variantData.obfuscationTask, variantOutputData.manifestProcessorTask,
                    variantOutputData.processResourcesTask);

            return task;
        }

        private static File getOptionalDir(File dir) {
            if (dir.isDirectory()) {
                return dir;
            }

            return null;
        }
    }
}
