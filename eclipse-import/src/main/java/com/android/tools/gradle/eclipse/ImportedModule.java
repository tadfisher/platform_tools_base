/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.tools.gradle.eclipse;

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.BIN_FOLDER;
import static com.android.SdkConstants.DOT_AIDL;
import static com.android.SdkConstants.DOT_FS;
import static com.android.SdkConstants.DOT_RS;
import static com.android.SdkConstants.DOT_RSH;
import static com.android.SdkConstants.FD_AIDL;
import static com.android.SdkConstants.FD_ASSETS;
import static com.android.SdkConstants.FD_JAVA;
import static com.android.SdkConstants.FD_MAIN;
import static com.android.SdkConstants.FD_RENDERSCRIPT;
import static com.android.SdkConstants.FD_RES;
import static com.android.SdkConstants.FN_PROJECT_PROPERTIES;
import static com.android.SdkConstants.GEN_FOLDER;
import static com.android.SdkConstants.LIBS_FOLDER;
import static com.android.SdkConstants.SRC_FOLDER;
import static com.android.tools.gradle.eclipse.EclipseImport.ECLIPSE_DOT_CLASSPATH;
import static com.android.tools.gradle.eclipse.EclipseImport.ECLIPSE_DOT_PROJECT;
import static java.io.File.separator;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.repository.GradleCoordinate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class ImportedModule implements Comparable<ImportedModule> {

    private static final String APPCOMPAT_DEP = "com.android.support:appcompat-v7:+";
    private static final String GRID_LAYOUT_DEP = "com.android.support:gridlayout-v7:+";
    private static final String ACTIONBAR_SHERLOCK_DEP
            = "com.actionbarsherlock:actionbarsherlock:4.4.0@aar";

    // TODO: Or -- pick the version (19 here) based on the compile sdk?
    //SUPPORT_LIB_DEP = "com.android.support:support-v4:19.0.0";
    private static final String SUPPORT_LIB_DEP = "com.android.support:support-v4:+";

    private final EclipseImport mImporter;
    private final EclipseProject mProject;
    private final List<GradleCoordinate> mDependencies = Lists.newArrayList();
    private final List<File> mJarDependencies = Lists.newArrayList();
    private List<GradleCoordinate> mReplaceWithDependencies;

    public ImportedModule(@NonNull EclipseImport importer, @NonNull EclipseProject project) {
        mImporter = importer;
        mProject = project;

        initDependencies();
        initReplaceWithDependency();
    }

    private void initDependencies() {
        for (File jar : mProject.getJarPaths()) {
            if (mImporter.isReplaceJars()) {
                GradleCoordinate dependency = guessDependency(jar);
                if (dependency != null) {
                    mDependencies.add(dependency);
                    mImporter.getSummary().replacedJar(jar, dependency);
                    continue;
                }
            }
            mJarDependencies.add(jar);
        }
    }

    @Nullable
    public static GradleCoordinate guessDependency(@NonNull File jar) {
        // Make guesses based on library. For now, we do simple name checks, but
        // later consider looking at jar contents, md5 sums etc, especially to
        // pick up exact version numbers of popular libraries
        String name = jar.getName().toLowerCase(Locale.US);
        if (name.equals("android-support-v4.jar")) {
            return GradleCoordinate.parseCoordinateString(SUPPORT_LIB_DEP);
        } else if (name.equals("android-support-v7-gridlayout.jar")) {
            return GradleCoordinate.parseCoordinateString(GRID_LAYOUT_DEP);
        } else if (name.equals("android-support-v7-appcompat.jar")) {
            return GradleCoordinate.parseCoordinateString(APPCOMPAT_DEP);
        } else if (name.equals("com_actionbarsherlock.jar") ||
                name.equalsIgnoreCase("actionbarsherlock.jar")) {
            return GradleCoordinate.parseCoordinateString(ACTIONBAR_SHERLOCK_DEP);
        } else if (name.equals("guava.jar") || name.startsWith("guava-")) {
            // TODO: If name.startsWith("guava-") try to pick a version number, e.g.
            // from guava-11.0.1.jar use version 11, and from guava-r09.jar use version 9
            return GradleCoordinate.parseCoordinateString("com.google.guava:guava:15.0");
        } else {
            // TODO: Other app support libraries like mediarouter
            // TODO: gcm.jar
            // TODO: gson-2.1.jar
            // TODO: protobuf
            // TODO: libGoogleAnalytics.jar
            // TODO: libGoogleAnalyticsV2.jar
            // TODO: play services:  com.google.android.gms:play-services:4.0.30
            // TODO: junit
            // TODO: joda-time-2.1.jar
            // TODO: volley.jar
            // TODO: robotium-solo-3.1.jar
            // TODO: roboguice-2.0b4.jar
            //etc etc
        }

        return null;
    }


    /**
     * See if this is a library that looks like a known dependency; if so, just
     * use a dependency instead of the library
     */
    private void initReplaceWithDependency() {
        if (mProject.isLibrary() && mImporter.isReplaceLibs()) {
            String pkg = mProject.getPackage();
            if (pkg != null) {
                if (mProject.getName().equals("ActionBarSherlock") ||
                        pkg.equals("com.actionbarsherlock")) {
                    mReplaceWithDependencies = Arrays.asList(
                            GradleCoordinate.parseCoordinateString(ACTIONBAR_SHERLOCK_DEP),
                            GradleCoordinate.parseCoordinateString(SUPPORT_LIB_DEP));
                } else if (pkg.equals("android.support.v7.gridlayout")) {
                    mReplaceWithDependencies = Collections.singletonList(
                            GradleCoordinate.parseCoordinateString(GRID_LAYOUT_DEP));
                }

                if (mReplaceWithDependencies != null) {
                    mImporter.getSummary().replacedLib(mProject, mReplaceWithDependencies);
                }
            }
        }
    }

    public boolean isReplacedWithDependency() {
        return mReplaceWithDependencies != null && !mReplaceWithDependencies.isEmpty();
    }

    public List<GradleCoordinate> getReplaceWithDependencies() {
        return mReplaceWithDependencies;
    }

    public String getModuleName() {
        String moduleName = mProject.getName();
        if (mImporter.isLowerCaseModules()) {
            moduleName = moduleName.toLowerCase(Locale.getDefault());
        }
        return moduleName;
    }

    public String getModuleReference() {
        return ':' + getModuleName();
    }

    public boolean isApp() {
        return mProject.isAndroidProject() && !mProject.isLibrary();
    }

    public boolean isAndroidLibrary() {
        return mProject.isAndroidProject() && mProject.isLibrary();
    }

    public boolean isJavaLibrary() {
        return !mProject.isAndroidProject();
    }

    public void copyInto(@NonNull File destDir) throws IOException {
        ImportSummary summary = mImporter.getSummary();

        Set<File> copied = Sets.newHashSet();

        final File main = new File(destDir, SRC_FOLDER + separator + FD_MAIN);
        mImporter.mkdirs(main);
        if (mProject.isAndroidProject()) {
            File srcManifest = mProject.getManifestFile();
            if (srcManifest.exists()) {
                File destManifest = new File(main, ANDROID_MANIFEST_XML);
                Files.copy(srcManifest, destManifest);
                summary.reportMoved(mProject, srcManifest, destManifest);
                recordCopiedFile(copied, srcManifest);
            }
            File srcRes = mProject.getResourceDir();
            if (srcRes.exists()) {
                File destRes = new File(main, FD_RES);
                mImporter.mkdirs(destRes);
                mImporter.copyDir(srcRes, destRes, null);
                summary.reportMoved(mProject, srcRes, destRes);
                recordCopiedFile(copied, srcRes);
            }
            File srcAssets = mProject.getAssetsDir();
            if (srcAssets.exists()) {
                File destAssets = new File(main, FD_ASSETS);
                mImporter.mkdirs(destAssets);
                mImporter.copyDir(srcAssets, destAssets, null);
                summary.reportMoved(mProject, srcAssets, destAssets);
                recordCopiedFile(copied, srcAssets);
            }
        }

        for (final File src : mProject.getSourcePaths()) {
            final File srcJava = mProject.resolveFile(src);
            File destJava = new File(main, FD_JAVA);
            // Merge all the separate source folders into a single one; they aren't allowed
            // to contain source file conflicts anyway
            mImporter.mkdirs(destJava);
            mImporter.copyDir(srcJava, destJava, new EclipseImport.CopyHandler() {
                // Handle moving .rs/.rsh/.fs files to main/rs/ and .aidl files to the
                // corresponding aidl package under main/aidl
                @Override
                public boolean handle(@NonNull File source, @NonNull File dest)
                        throws IOException {
                    String sourcePath = source.getPath();
                    if (sourcePath.endsWith(DOT_AIDL)) {
                        File aidlDir = new File(main, FD_AIDL);
                        File relative = EclipseProject.computeRelativePath(srcJava, source);
                        if (relative == null) {
                            relative = EclipseProject.computeRelativePath(
                                    srcJava.getCanonicalFile(), source);
                        }
                        if (relative != null) {
                            File destAidl = new File(aidlDir, relative.getPath());
                            mImporter.mkdirs(destAidl.getParentFile());
                            Files.copy(source, destAidl);
                            mImporter.getSummary().reportMoved(mProject, source, destAidl);
                            return true;
                        }
                    } else if (sourcePath.endsWith(DOT_RS) ||
                            sourcePath.endsWith(DOT_RSH) ||
                            sourcePath.endsWith(DOT_FS)) {
                        // Copy to flattened rs dir
                        // TODO: Ensure the file names are unique!
                        File destRs = new File(main, FD_RENDERSCRIPT + separator +
                                source.getName());
                        mImporter.mkdirs(destRs.getParentFile());
                        Files.copy(source, destRs);
                        mImporter.getSummary().reportMoved(mProject, source, destRs);
                        return true;
                    }
                    return false;
                }
            });
            summary.reportMoved(mProject, srcJava, destJava);
            recordCopiedFile(copied, srcJava);
        }

        if (mProject.isAndroidProject()) {
            for (File srcProguard : mProject.getLocalProguardFiles()) {
                File destProguard = new File(destDir, srcProguard.getName());
                if (!destProguard.exists()) {
                    Files.copy(srcProguard, destProguard);
                    summary.reportMoved(mProject, srcProguard, destProguard);
                    recordCopiedFile(copied, srcProguard);
                } else {
                    mImporter.reportWarning(mProject, destProguard,
                            "Local proguard config file name is not unique");
                }
            }
        }

        reportIgnored(copied);
    }

    private static void recordCopiedFile(@NonNull Set<File> copied, @NonNull File file)
            throws IOException {
        copied.add(file);
        copied.add(file.getCanonicalFile());
    }

    private void reportIgnored(Set<File> copied) throws IOException {
        File canonicalDir = mProject.getCanonicalDir();

        // Ignore output folder (if not under bin/ as usual)
        File outputDir = mProject.getOutputDir();
        if (outputDir != null) {
            copied.add(mProject.resolveFile(outputDir).getCanonicalFile());
        }

        // These files are either not useful (bin, gen) or already handled (project metadata files)
        copied.add(new File(canonicalDir, BIN_FOLDER));
        copied.add(new File(canonicalDir, GEN_FOLDER));
        copied.add(new File(canonicalDir, ECLIPSE_DOT_CLASSPATH));
        copied.add(new File(canonicalDir, ECLIPSE_DOT_PROJECT));
        copied.add(new File(canonicalDir, FN_PROJECT_PROPERTIES));
        copied.add(new File(canonicalDir, FN_PROJECT_PROPERTIES));
        copied.add(new File(canonicalDir, LIBS_FOLDER));

        reportIgnored(canonicalDir, copied, 0);
    }

    private void reportIgnored(@NonNull File file, @NonNull Set<File> copied, int depth)
            throws IOException {
        if (depth > 0) {
            if (copied.contains(file)) {
                return;
            }
            File relative = EclipseProject.computeRelativePath(mProject.getCanonicalDir(), file);
            if (relative == null) {
                relative = file;
            }
            mImporter.getSummary().reportIgnored(mProject, relative);
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    reportIgnored(child, copied, depth + 1);
                }
            }
        }
    }

    public List<File> getJarDependencies() {
        return mJarDependencies;
    }

    public List<GradleCoordinate> getDependencies() {
        return mDependencies;
    }

    public EclipseProject getProject() {
        return mProject;
    }

    /**
     * Creates a list of modules from the given set of projects. The returned list
     * is in dependency order.
     */
    public static List<ImportedModule> create(
            @NonNull EclipseImport importer,
            @NonNull Collection<EclipseProject> projects) {
        List<ImportedModule> modules = Lists.newArrayList();
        List<ImportedModule> replacedByDependencies = Lists.newArrayList();

        for (EclipseProject project : projects) {
            ImportedModule module = new ImportedModule(importer, project);
            if (module.isReplacedWithDependency()) {
                replacedByDependencies.add(module);
            } else {
                modules.add(module);
            }
        }

        // Some libraries may be replaced by just a dependency (for example,
        // instead of copying in a whole copy of ActionBarSherlock, just
        // replace by the corresponding dependency.
        for (ImportedModule replaced : replacedByDependencies) {
            assert replaced.getReplaceWithDependencies() != null;
            EclipseProject project = replaced.getProject();
            for (ImportedModule module : modules) {
                if (module.getProject().getDirectLibraries().contains(project)) {
                    module.mDependencies.addAll(replaced.getReplaceWithDependencies());
                }
            }
        }

        // Sort by dependency order
        Collections.sort(modules);

        return modules;
    }

    // Sort by dependency order
    @Override
    public int compareTo(@NonNull ImportedModule other) {
        if (mProject.getAllLibraries().contains(other.getProject())) {
            return 1;
        } else if (other.getProject().getAllLibraries().contains(mProject)) {
            return -1;
        } else {
            return getModuleName().compareTo(other.getModuleName());
        }
    }
}
