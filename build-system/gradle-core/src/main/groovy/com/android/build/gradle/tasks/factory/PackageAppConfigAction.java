package com.android.build.gradle.tasks.factory;

import com.android.build.OutputFile;
import com.android.build.gradle.internal.StringHelper;
import com.android.build.gradle.internal.TaskManager;
import com.android.build.gradle.internal.core.GradleVariantConfiguration;
import com.android.build.gradle.internal.dsl.AbiSplitOptions;
import com.android.build.gradle.internal.dsl.PackagingOptions;
import com.android.build.gradle.internal.dsl.SigningConfig;
import com.android.build.gradle.internal.scope.ConventionMappingHelper;
import com.android.build.gradle.internal.scope.VariantOutputScope;
import com.android.build.gradle.internal.tasks.ValidateSigningTask;
import com.android.build.gradle.internal.variant.ApkVariantData;
import com.android.build.gradle.internal.variant.ApkVariantOutputData;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.build.gradle.tasks.PackageApplication;
import com.android.build.gradle.tasks.ShrinkResources;
import com.google.common.collect.ImmutableSet;

import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.gradle.api.Action;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES;

/**
 * Action to configue a PackageApplication task.
 */
public class PackageAppConfigAction implements Action<PackageApplication> {

    private VariantOutputScope scope;

    public PackageAppConfigAction(VariantOutputScope scope) {
        this.scope = scope;
    }

    @Override
    public void execute(PackageApplication packageApp) {
        final ApkVariantData variantData = (ApkVariantData) scope.getVariantData();
        final ApkVariantOutputData variantOutputData = (ApkVariantOutputData) scope
                .getVariantOutputData();
        final GradleVariantConfiguration config = scope.getVariantConfiguration();

        String outputName = variantOutputData.getFullName();
        String outputBaseName = variantOutputData.getBaseName();

        variantOutputData.packageApplicationTask = packageApp;
        packageApp.setAndroidBuilder(scope.getAndroidBuilder());

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
                    return scope.getProject()
                            .fileTree(variantData.preDexTask.getOutputFolder()).getFiles();
                }

                return Collections.emptyList();
            }
        });
        ConventionMappingHelper.map(packageApp, "packagedJars", new Callable<Set<File>>() {
            @Override
            public Set<File> call() {
                return scope.getAndroidBuilder().getPackagedJars(config);
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
                    return scope.getJniFolders();
                }
                Set<String> filters = AbiSplitOptions.getAbiFilters(
                        scope.getExtension().getSplits().getAbiFilters());
                return filters.isEmpty() ? scope.getJniFolders() : Collections.<File>emptySet();

            }
        });

        ConventionMappingHelper.map(packageApp, "abiFilters", new Callable<Set<String>>() {
            @Override
            public Set<String> call() throws Exception {
                if (variantOutputData.getMainOutputFile().getFilter(OutputFile.ABI) != null) {
                    return ImmutableSet.
                            of(variantOutputData.getMainOutputFile().getFilter(OutputFile.ABI));
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
                    (ValidateSigningTask) scope.getProject().getTasks().findByName(validateSigningTaskName);
            if (validateSigningTask == null) {
                validateSigningTask =
                        scope.getProject().getTasks().create(
                                "validate" + StringHelper.capitalize(sc.getName()) + "Signing",
                                ValidateSigningTask.class);
                validateSigningTask.setAndroidBuilder(scope.getAndroidBuilder());
                validateSigningTask.setSigningConfig(sc);
            }

            packageApp.dependsOn(validateSigningTask);
        }

        ConventionMappingHelper.map(packageApp, "packagingOptions", new Callable<PackagingOptions>() {
            @Override
            public PackagingOptions call() throws Exception {
                return scope.getExtension().getPackagingOptions();
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
        ShrinkResources task = scope.getProject().getTasks()
                .create("shrink" + StringGroovyMethods.capitalize(variantOutputData.getFullName())
                        + "Resources", ShrinkResources.class);
        task.setAndroidBuilder(scope.getAndroidBuilder());
        task.variantOutputData = variantOutputData;

        final String outputBaseName = variantOutputData.getBaseName();
        task.setCompressedResources(new File(
                scope.getBuildDir() + "/" + FD_INTERMEDIATES + "/resources/" +
                        "resources-" + outputBaseName + "-stripped.ap_"));

        ConventionMappingHelper.map(task, "uncompressedResources", new Callable<File>() {
            @Override
            public File call() {
                return variantOutputData.processResourcesTask.getPackageOutputFile();
            }
        });

        task.dependsOn(variantData.obfuscationTask, variantOutputData.manifestProcessorTask,
                variantOutputData.processResourcesTask);

        return ((ShrinkResources) (task));
    }

    private static File getOptionalDir(File dir) {
        if (dir.isDirectory()) {
            return dir;
        }

        return null;
    }
}
