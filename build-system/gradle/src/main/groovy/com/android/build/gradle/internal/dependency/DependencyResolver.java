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

package com.android.build.gradle.internal.dependency;

import static com.android.SdkConstants.EXT_ANDROID_PACKAGE;
import static com.android.SdkConstants.EXT_JAR;
import static com.android.builder.core.BuilderConstants.EXT_LIB_ARCHIVE;
import static com.android.builder.model.AndroidProject.PROPERTY_BUILD_MODEL_ONLY;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.model.MavenCoordinatesImpl;
import com.android.builder.dependency.JarDependency;
import com.android.builder.dependency.LibraryDependency;
import com.android.builder.model.AndroidProject;
import com.android.utils.ILogger;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.gradle.api.specs.Specs;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves dependencies and create Library and Jar dependency objects
 * that the rest of the plugin uses.
 */
public class DependencyResolver {

    private static final boolean DEBUG_LOG = true;

    @NonNull
    private final Project project;

    @NonNull
    private final ILogger logger;

    /**
     * All resolved artifacts, mapped to their module version identifiers.
     */
    @NonNull
    private final Map<ModuleVersionIdentifier, List<ResolvedArtifact>> resolvedArtifacts = Maps.newHashMap();

    @NonNull
    private final Map<ModuleVersionIdentifier, List<LibraryDependency>> modules = Maps
            .newHashMap();
    @NonNull
    private final Multimap<LibraryDependency, VariantDependencies> reverseMap = ArrayListMultimap.create();

    @NonNull
    private final Collection<String> unresolvedDependencies = Sets.newHashSet();

    public DependencyResolver(
            @NonNull Project project,
            @NonNull ILogger logger) {
        this.project = project;
        this.logger = logger;
    }

    @NonNull
    public Map<ModuleVersionIdentifier, List<LibraryDependency>> getModules() {
        return modules;
    }

    @NonNull
    public Multimap<LibraryDependency, VariantDependencies> getReverseMap() {
        return reverseMap;
    }

    @NonNull
    public Map<ModuleVersionIdentifier, List<ResolvedArtifact>> getResolvedArtifacts() {
        return resolvedArtifacts;
    }

    @NonNull
    public Collection<String> getUnresolvedDependencies() {
        return unresolvedDependencies;
    }

    public void resolveDependencies(@NonNull VariantDependencies variantDependencies) {
        Configuration compileConfiguration = variantDependencies.getCompileConfiguration();
        Configuration packageConfiguration = variantDependencies.getPackageConfiguration();

        ensureConfigured(compileConfiguration);
        ensureConfigured(packageConfiguration);

        variantDependencies.setChecker(new DependencyChecker(variantDependencies, logger));

        Set<String> currentUnresolvedDependencies = Sets.newHashSet();

        collectResolvedArtifacts(compileConfiguration);
        collectResolvedArtifacts(packageConfiguration);

        List<LibraryDependency> bundles = Lists.newArrayList();
        Map<File, JarDependency> jars = Maps.newHashMap();
        Map<File, JarDependency> localJars = Maps.newHashMap();

        Set<? extends DependencyResult> dependencies = compileConfiguration.getIncoming().getResolutionResult().getRoot().getDependencies();
        for (DependencyResult dependencyResult : dependencies) {
            if (dependencyResult instanceof ResolvedDependencyResult) {
                addDependency(
                        ((ResolvedDependencyResult) dependencyResult).getSelected(),
                        variantDependencies,
                        bundles,
                        jars,
                        modules,
                        reverseMap);
            } else if (dependencyResult instanceof UnresolvedDependencyResult) {
                ComponentSelector attempted
                        = ((UnresolvedDependencyResult) dependencyResult).getAttempted();
                if (attempted != null) {
                    currentUnresolvedDependencies.add(attempted.toString());
                }
            }
        }


        // also need to process local jar files, as they are not processed by the
        // resolvedConfiguration result. This only includes the local jar files for this project.
        for (Dependency dependency : compileConfiguration.getAllDependencies()) {
            if (dependency instanceof SelfResolvingDependency &&
                    !(dependency instanceof ProjectDependency)) {
                Set<File> files = ((SelfResolvingDependency) dependency).resolve();
                for (File f : files) {
                    localJars.put(f, new JarDependency(f, true /*compiled*/, false /*packaged*/,
                            null /*resolvedCoordinates*/));
                }
            }
        }

        if (!compileConfiguration.getResolvedConfiguration().hasError()) {
            // handle package dependencies. We'll refuse aar libs only in package but not
            // in compile and remove all dependencies already in compile to get package-only jar
            // files.

            Set<File> compileFiles = compileConfiguration.getFiles();
            Set<File> packageFiles = packageConfiguration.getFiles();

            for (File f : packageFiles) {
                if (compileFiles.contains(f)) {
                    // if also in compile
                    JarDependency jarDep = jars.get(f);
                    if (jarDep == null) {
                        jarDep = localJars.get(f);
                    }
                    if (jarDep != null) {
                        jarDep.setPackaged(true);
                    }
                    continue;
                }

                if (f.getName().toLowerCase().endsWith(".jar")) {
                    jars.put(f, new JarDependency(f, false /*compiled*/, true /*packaged*/,
                            null /*resolveCoordinates*/));
                } else {
                    throw new RuntimeException("Package-only dependency '" +
                            f.getAbsolutePath() +
                            "' is not supported in project " + project.getName());
                }
            }
        } else if (!currentUnresolvedDependencies.isEmpty()) {
            unresolvedDependencies.addAll(currentUnresolvedDependencies);
        }

        variantDependencies.addLibraries(bundles);
        variantDependencies.addJars(jars.values());
        variantDependencies.addLocalJars(localJars.values());
    }

    /**
     * Collects resolved artifacts from a Configuration object
     *
     * @param configuration the configuration
     */
    private void collectResolvedArtifacts(@NonNull Configuration configuration) {

        // To keep backwards-compatibility, we check first if we have the JVM arg. If not, we look for
        // the project property.
        boolean buildModelOnly = false;
        String val = System.getProperty(PROPERTY_BUILD_MODEL_ONLY);
        if ("true".equalsIgnoreCase(val)) {
            buildModelOnly = true;
        } else if (project.hasProperty(PROPERTY_BUILD_MODEL_ONLY)) {
            Object value = project.getProperties().get(PROPERTY_BUILD_MODEL_ONLY);
            if (value instanceof String) {
                buildModelOnly = Boolean.parseBoolean((String) value);
            }
        }

        Set<ResolvedArtifact> allArtifacts;
        if (buildModelOnly) {
            allArtifacts = configuration.getResolvedConfiguration()
                    .getLenientConfiguration().getArtifacts(Specs.satisfyAll());
        } else {
            allArtifacts = configuration.getResolvedConfiguration().getResolvedArtifacts();
        }

        if (DEBUG_LOG) {
            log("Collecting Artifacts for '%s'", configuration.getName());
        }
        for (ResolvedArtifact artifact : allArtifacts) {
            ModuleVersionIdentifier id = artifact.getModuleVersion().getId();
            if (DEBUG_LOG) {
                log("\tFound: %s:%s:%s -> %s", id.getGroup(), id.getName(), id.getVersion(), artifact.getFile());
            }

            List<ResolvedArtifact> moduleArtifacts = resolvedArtifacts.get(id);
            if (moduleArtifacts == null) {
                moduleArtifacts = Lists.newArrayList();
                resolvedArtifacts.put(id, moduleArtifacts);
            }

            if (!moduleArtifacts.contains(artifact)) {
                moduleArtifacts.add(artifact);
            }
        }
    }

    /**
     * Add dependency for a selected module to several output lists.
     * @param selectedModule the selected module
     * @param bundles
     * @param jars
     * @param modules
     * @param reverseMap
     */
    private void addDependency(
            ResolvedComponentResult selectedModule,
            VariantDependencies variantDependencies,
            Collection<LibraryDependency> bundles,
            Map<File, JarDependency> jars,
            Map<ModuleVersionIdentifier, List<LibraryDependency>> modules,
            Multimap<LibraryDependency, VariantDependencies> reverseMap) {

        ModuleVersionIdentifier selectedModuleId = selectedModule.getModuleVersion();

        if (variantDependencies.getChecker().excluded(selectedModuleId)) {
            return;
        }

        if ("support-annotations".equals(selectedModuleId.getName()) &&
                "com.android.support".equals(selectedModuleId.getGroup())) {
            variantDependencies.annotationsPresent = true;
        }

        List<LibraryDependency> bundlesForThisModule = modules.get(selectedModuleId);

        if (bundlesForThisModule == null) {
            bundlesForThisModule = Lists.newArrayList();
            modules.put(selectedModuleId, bundlesForThisModule);

            // we only care about nested dependencies when it's an Android Library
            // project. Nested jar dependencies can go in the main module flat.
            List<LibraryDependency> nestedBundles = Lists.newArrayList();

            for (DependencyResult dependencyResult : selectedModule.getDependencies()) {
                if (dependencyResult instanceof ResolvedDependencyResult) {
                    addDependency(
                            ((ResolvedDependencyResult)dependencyResult).getSelected(),
                            variantDependencies,
                            nestedBundles,
                            jars,
                            modules,
                            reverseMap);
                }
            }

            // get the collected artifacts for this selected module id
            for (ResolvedArtifact resolvedArtifact : resolvedArtifacts.get(selectedModuleId)) {
                if (EXT_LIB_ARCHIVE.equals(resolvedArtifact.getType())) {
                    String path = selectedModuleId.getGroup() + '/' + selectedModuleId.getName() + '/' + selectedModuleId.getVersion();
                    String name = selectedModuleId.getGroup() + ':' + selectedModuleId.getName() + ':' + selectedModuleId.getVersion();
                    if (resolvedArtifact.getClassifier() != null) {
                        path = path + '/' + resolvedArtifact.getClassifier();
                        name = name + '/' + resolvedArtifact.getClassifier();
                    }
                    File explodedDir = project.file("$project.buildDir/" + AndroidProject.FD_INTERMEDIATES + "/exploded-aar/" + path);
                    LibraryDependencyImpl adep = new LibraryDependencyImpl(
                            resolvedArtifact.getFile(),
                            explodedDir,
                            nestedBundles,
                            name,
                            resolvedArtifact.getClassifier(),
                            null,
                            new MavenCoordinatesImpl(resolvedArtifact));

                    bundlesForThisModule.add(adep);
                    reverseMap.put(adep, variantDependencies);

                } else if (EXT_JAR.equals(resolvedArtifact.getType())) {
                    jars.put(resolvedArtifact.getFile(),
                            new JarDependency(
                                    resolvedArtifact.getFile(),
                                    true /*compiled*/,
                                    false /*packaged*/,
                                    true /*proguarded*/,
                                    new MavenCoordinatesImpl(resolvedArtifact)));
                } else if (EXT_ANDROID_PACKAGE.equals(resolvedArtifact.getType())) {
                    String name = selectedModuleId.getGroup() + ':' + selectedModuleId.getName() + ':' + selectedModuleId.getVersion();
                    if (resolvedArtifact.getClassifier() != null) {
                        name = name + '/' + resolvedArtifact.getClassifier();
                    }

                    // cannot throw this yet, since depending on a secondary artifact in an
                    // Android app will trigger getting the main APK as well.
                    throw new GradleException(
                            "Dependency "
                                    + name
                                    + " on project "
                                    + project.getName()
                                    + " resolves to an APK archive which is not supported"
                                    + " as a compilation dependency. File: "
                                    + resolvedArtifact.getFile());
                }
            }

            if (bundlesForThisModule.isEmpty() && !nestedBundles.isEmpty()) {
                throw new GradleException("Module version $id depends on libraries but is not a library itself");
            }
        } else {
            for (LibraryDependency adep : bundlesForThisModule) {
                reverseMap.put(adep, variantDependencies);
            }
        }

        bundles.addAll(bundlesForThisModule);
    }

    private void ensureConfigured(@NonNull Configuration config) {
        for (ProjectDependency projectDependency : config.getAllDependencies().withType(ProjectDependency.class)) {
            project.evaluationDependsOn(projectDependency.getDependencyProject().getPath());
            ensureConfigured(projectDependency.getProjectConfiguration());
        }
    }

    private void log(String format, Object... args) {
        System.out.println(String.format(format, args));
    }
}
