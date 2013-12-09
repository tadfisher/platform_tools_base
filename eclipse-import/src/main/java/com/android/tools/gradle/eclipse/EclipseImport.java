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
import static com.android.SdkConstants.FN_BUILD_GRADLE;
import static com.android.SdkConstants.FN_SETTINGS_GRADLE;
import static java.io.File.separator;
import static java.io.File.separatorChar;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.repository.GradleCoordinate;
import com.android.utils.SdkUtils;
import com.android.utils.XmlUtils;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * TODO:
 * <ul>
 *     <li>Migrate SDK folder from local.properties. If should make doubly sure that
 *         the repository you point to contains the app support library and other
 *         libraries that may be needed.</li>
 *     <li>Add a UI
 *     <ul>
 *         <li>If a project to be imported has both eclipse and build.gradle files,
 *         don't show my importer</li>
 *         <li>ability to pick dest dir (explain why making a copy)</li>
 *         <li>ability to pick initial set of projects (in a workspace)</li>
 *         <li>ability to define path variables and define project location if
 *             no workspace is known</li>
 *     </ul>:
 *     <li>Look into other aspects of ADT projects, such as AIDL and RS handling</li>
 *     <li>Handle workspace paths in dependencies, resolve to actual paths</li>
 *     <li>Properly compute module order for settings.gradle</li>
 *     <li>Figure out how I should handle hierarchies of libraries; should I make
 *         a nested set of gradle libraries, or should I flatten?</li>
 *     <li>Switch to FileOp so I can improve unit testing and offer git operations
 *     in Studio</li>
 *     <li>Consider whether I can make this import mechanism work for Maven and plain
 *     sources as well?</li>
 *     <li>Make it optional whether we replace the directory structure with the Gradle one?</li>
 *     <li>If I have a workspace, check to see if there are problem markers and if
 *     so warn that the project may not be buildable</li>
 *     <li>Do I migrate VCS folders (.git, .svn., etc?)</li>
 *     <li>Read SDK home out of local.properties and ask whether to use it or the Studio one
 *     (if they differ), and if the former, ensure it has all the gradle repositories we need</li>
 *     <li>Optional:  at the end of the import, migrate Eclipse settings too --
 *      such as code styles, compiler flags (especially those for the
 *      project), ask about enabling eclipse key bindings, etc?</li>
 *     <li>Look at the Eclipse Gradle export code to see what else I'm missing</li>
 *     <li>In one project.properties file I found "split.density=false"; see if we need
 *     to migrate this</li>
 *     <li>If replaceJars=false, insert *comments* in the source code for potential
 *     replacements such that users don't forget and consider switching in the future</li>
 *     <li>Allow migrating a project in-place? Should implement with FileOp such that
 *     we can map to git operations in IntelliJ.</li>
 *     <li>Figure out if we can reuse fragments from the default freemarker templates for
 *     the code generation part.</li>
 *     <li>Also look at local.properties to resolve SDK location?</li>
 *     <li>Migrate .aidl and .rs files out of source directories and into source set locations</li>
 *     <li>I currently have some hardcoded references to 19 (e.g. build tools); figure out
 *     if I can pick this up from the project, or extract it in some other way</li>
 *     <li>Move instrumentation tests; analyze instrumentation ADT test project, pull out package
 *      info etc and put in Gradle file, then move to instrumentation tests folder</li>
 *     <li>Allow option to preserve module nesting hierarchy</li>
 *     <li>Make it possible to use this wizard to migrate an already exported Eclipse project?</li>
 *     <li>Consider making the export create an HTML file?</li>
 * </ul>
 */
public class EclipseImport {
    public static final String NL = SdkUtils.getLineSeparator();

    public static final String ECLIPSE_DOT_CLASSPATH = ".classpath";
    public static final String ECLIPSE_DOT_PROJECT = ".project";
    public static final String IMPORT_SUMMARY_TXT = "import-summary.txt";
    static final String ANDROID_GRADLE_PLUGIN
            = "com.android.tools.build:gradle:0.7.0-SNAPSHOT";

    private List<ImportedModule> mModules;
    private ImportSummary mSummary;
    private File mWorkspace;
    private File mGradleWrapper;
    private boolean mLowerCaseModules = true;

    /** Whether we should try to replace jars with dependencies */
    private boolean mReplaceJars = true;
    private boolean mReplaceLibs = true;

    public EclipseImport() {
    }

    /** Imports the given projects. Note that this just reads in the project state;
     * it does not actually write out a Gradle project. For that, you should call
     * {@link #exportProject(java.io.File, boolean)}.
     *
     * @param projectDirs the Eclipse project directories to import
     * @throws IOException if something is wrong
     */
    public void importProjects(@NonNull List<File> projectDirs) throws IOException {
        mSummary = new ImportSummary();

        for (File file : projectDirs) {
            if (file.isFile()) {
                assert !file.isDirectory();
                file = file.getParentFile();
            }

            if (isEclipseProjectDir(file)) {
                List<EclipseProject> roots = Lists.newArrayList();
                roots.add(EclipseProject.getProject(this, file));
            } else {
                reportError(null, file, "Not an Eclipse project: " + file);
            }

            guessWorkspace(file);
        }

        // Find unique projects. (We can register projects under multiple paths
        // if the dir and the canonical dir differ, so pick unique values here)
        Set<EclipseProject> projects = Sets.newHashSet(mProjectMap.values());
        mModules = ImportedModule.create(this, projects);
    }

    public static boolean isEclipseProjectDir(@Nullable File file) {
        return file != null && file.isDirectory()
                && new File(file, ECLIPSE_DOT_CLASSPATH).exists()
                && new File(file, ECLIPSE_DOT_PROJECT).exists();
    }

    public static boolean isAdtProjectDir(@Nullable File file) {
        return isEclipseProjectDir(file)
                && new File(file, ANDROID_MANIFEST_XML).exists();
    }

    /** Sets location of Eclipse workspace, if known */
    public EclipseImport setWorkspace(@NonNull File workspace) {
        mWorkspace = workspace;
        assert mWorkspace.exists() : workspace.getPath();
        return this;
    }

    /** Sets location of gradle wrapper to copy into exported project, if known */
    public EclipseImport setGradleWrapperLocation(@NonNull File gradleWrapper) {
        mGradleWrapper = gradleWrapper;
        return this;
    }

    /** Whether import should attempt to replace jars with dependencies */
    public EclipseImport setReplaceJars(boolean replaceJars) {
        mReplaceJars = replaceJars;
        return this;
    }

    /** Whether import should attempt to replace jars with dependencies */
    public boolean isReplaceJars() {
        return mReplaceJars;
    }

    /** Whether import should attempt to replace inlined library projects with dependencies */
    public boolean isReplaceLibs() {
        return mReplaceLibs;
    }

    /** Whether import should attempt to replace inlined library projects with dependencies */
    public void setReplaceLibs(boolean replaceLibs) {
        mReplaceLibs = replaceLibs;
    }

    /** Whether import should lower-case module names from ADT project names */
    public EclipseImport setLowerCaseModules(boolean lowerCase) {
        mLowerCaseModules = lowerCase;
        return this;
    }

    /** Whether import should lower-case module names from ADT project names */
    public boolean isLowerCaseModules() {
        return mLowerCaseModules;
    }

    private void guessWorkspace(@NonNull File projectDir) {
        if (mWorkspace == null) {
            File dir = projectDir.getParentFile();
            while (dir != null) {
                if (isEclipseWorkspaceDir(dir)) {
                    setWorkspace(dir);
                    break;
                }
                dir = dir.getParentFile();
            }
        }
    }

    /**
     * Do we need to know the workspace location in order to work out path variables,
     * locations of dependent libraries etc?
     *
     * To find workspace:
     * <ol>
     * <li>Start Eclipse with -showlocation (doesn't work on OSX)
     * or
     * <li> Choose File->Switch Workspace->Other... and it shows the name of current workspace.
     * </ol>
     */
    public boolean needWorkspace() {
        // Already know it?
        //noinspection VariableNotUsedInsideIf
        if (mWorkspace != null) {
            return false;
        }

        for (EclipseProject project : mProjectMap.values()) {
            if (project.needWorkspaceLocation()) {
                return true;
            }
        }

        return false;
    }

    public void exportProject(@NonNull File destDir, boolean allowNonEmpty) throws IOException {
        mSummary.setDestDir(destDir);
        assert !needWorkspace() || mWorkspace != null;
        createDestDir(destDir, allowNonEmpty);
        createProjectBuildGradle(new File(destDir, FN_BUILD_GRADLE));
        createSettingsGradle(new File(destDir, FN_SETTINGS_GRADLE));

        exportGradleWrapper(destDir);

        // TODO: Figure out if I should "flatten" the module structure or not.
        for (ImportedModule module : mModules) {
            exportModule(new File(destDir, module.getModuleName()), module);
        }

        mSummary.write(new File(destDir, IMPORT_SUMMARY_TXT));
    }

    private void exportGradleWrapper(File destDir) throws IOException {
        if (mGradleWrapper != null && mGradleWrapper.exists()) {
            File gradlewDest = new File(destDir, "gradlew");
            copyDir(new File(mGradleWrapper, "gradlew"), gradlewDest, null);
            boolean madeExecutable = gradlewDest.setExecutable(true);
            if (!madeExecutable) {
                reportWarning(null, gradlewDest, "Could not make gradle wrapper script executable");
            }
            copyDir(new File(mGradleWrapper, "gradlew.bat"), new File(destDir, "gradlew.bat"),
                    null);
            copyDir(new File(mGradleWrapper, "gradle"), new File(destDir, "gradle"), null);
        }
    }

    private void exportModule(File destDir, ImportedModule module) throws IOException {
        mkdirs(destDir);
        createModuleBuildGradle(new File(destDir, FN_BUILD_GRADLE), module);
        module.copyInto(destDir);
    }

    @SuppressWarnings("MethodMayBeStatic")
    void mkdirs(File destDir) throws IOException {
        if (!destDir.exists()) {
            boolean ok = destDir.mkdirs();
            if (!ok) {
                reportError(null, destDir, "Could not make directory " + destDir);
            }
        }
    }

    private static void createModuleBuildGradle(@NonNull File file, ImportedModule module)
            throws IOException {
        EclipseProject project = module.getProject();
        StringBuilder sb = new StringBuilder();

        if (module.isApp() || module.isAndroidLibrary()) {
            appendRepositories(sb, true);

            if (module.isApp()) {
                sb.append("apply plugin: 'android'").append(NL);
            } else {
                assert module.isAndroidLibrary();
                sb.append("apply plugin: 'android-library'").append(NL);
            }
            sb.append("").append(NL);
            sb.append("repositories {").append(NL);
            sb.append("    mavenCentral()").append(NL);
            sb.append("}").append(NL);
            sb.append("").append(NL);
            sb.append("android {").append(NL);
            String compileSdkVersion = Integer.toString(project.getCompileSdkVersion());
            String minSdkVersion = Integer.toString(project.getMinSdkVersion());
            String targetSdkVersion = Integer.toString(project.getTargetSdkVersion());
            sb.append("    compileSdkVersion ").append(compileSdkVersion).append(NL);
            sb.append("    buildToolsVersion \"19.0.0\"").append(NL);
            sb.append("").append(NL);
            sb.append("    defaultConfig {").append(NL);
            sb.append("        minSdkVersion ").append(minSdkVersion).append(NL);
            if (project.getTargetSdkVersion() > 1) {
                sb.append("        targetSdkVersion ").append(targetSdkVersion).append(NL);
            }

            String languageLevel = project.getLanguageLevel();
            if (!languageLevel.equals(EclipseProject.DEFAULT_LANGUAGE_LEVEL)) {
                sb.append("        compileOptions {").append(NL);
                String level = languageLevel.replace('.','_'); // 1.6 => 1_6
                sb.append("            sourceCompatibility JavaVersion.VERSION_").append(level).append(
                        NL);
                sb.append("            targetCompatibility JavaVersion.VERSION_").append(level).append(
                        NL);
                sb.append("        }").append(NL);
            }
            sb.append("    }").append(NL);
            sb.append(NL);

            List<File> localRules = project.getLocalProguardFiles();
            List<File> sdkRules = project.getSdkProguardFiles();
            if (!localRules.isEmpty() || !sdkRules.isEmpty()) {
                // User specified ProGuard rules; replicate exactly
                if (module.isAndroidLibrary()) {
                    sb.append("    release {").append(NL);
                    sb.append("        runProguard true").append(NL);
                    sb.append("        proguardFiles ");
                    sb.append(generateProguardFileList(localRules, sdkRules)).append(NL);
                    sb.append("    }").append(NL);
                } else {
                    sb.append("    buildTypes {").append(NL);
                    sb.append("        release {").append(NL);
                    sb.append("            runProguard true").append(NL);
                    sb.append("            proguardFiles ");
                    sb.append(generateProguardFileList(localRules, sdkRules)).append(NL);
                    sb.append("        }").append(NL);
                    sb.append("    }").append(NL);
                }

            } else {
                // User didn't specify ProGuard rules; put in defaults (but off)
                if (module.isAndroidLibrary()) {
                    sb.append("    release {").append(NL);
                    sb.append("        runProguard false").append(NL);
                    sb.append("        proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.txt'").append(NL);
                    sb.append("    }").append(NL);
                } else {
                    sb.append("    buildTypes {").append(NL);
                    sb.append("        release {").append(NL);
                    sb.append("            runProguard false").append(NL);
                    sb.append("            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.txt'").append(NL);
                    sb.append("        }").append(NL);
                    sb.append("    }").append(NL);
                }
            }
            sb.append("}").append(NL);
            sb.append(NL);
            sb.append("dependencies {").append(NL);
            appendDependencies(sb, module);
            sb.append("}");

        } else if (module.isJavaLibrary()) {
            appendRepositories(sb, false);

            sb.append("apply plugin: 'java'").append(NL);

            String languageLevel = project.getLanguageLevel();
            if (!languageLevel.equals(EclipseProject.DEFAULT_LANGUAGE_LEVEL)) {
                sb.append("sourceCompatibility = \"");
                sb.append(languageLevel);
                sb.append("\"").append(NL);
                sb.append("targetCompatibility = \"");
                sb.append(languageLevel);
                sb.append("\"").append(NL);
            }
        } else {
            assert false : module;
        }


        Files.write(sb.toString(), file, Charsets.UTF_8);
    }

    private static String generateProguardFileList(List<File> localRules, List<File> sdkRules) {
        assert !localRules.isEmpty() || !sdkRules.isEmpty();
        StringBuilder sb = new StringBuilder();
        for (File rule : sdkRules) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("getDefaultProguardFile('");
            sb.append(rule.getName());
            sb.append("')");
        }

        for (File rule : localRules) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("'");
            // Note: project config files are flattened into the module structure (see
            // ImportModule#copyInto handler)
            sb.append(rule.getName());
            sb.append("'");
        }

        return sb.toString();
    }

    private static void appendDependencies(@NonNull StringBuilder sb,
            @NonNull ImportedModule module)
            throws IOException {
        for (GradleCoordinate dependency : module.getDependencies()) {
            sb.append("    compile '").append(dependency.toString()).append("'").append(NL);
        }
        for (File jar : module.getJarDependencies()) {
            String path = jar.getPath().replace(separatorChar, '/'); // Always / in gradle
            sb.append("    compile files('").append(path).append("')").append(NL);
        }
    }

    private static void appendRepositories(@NonNull StringBuilder sb, boolean needAndroidPlugin) {
        sb.append("buildscript {").append(NL);
        sb.append("    repositories {").append(NL);
        sb.append("        mavenCentral()").append(NL);
        sb.append("    }").append(NL);
        if (needAndroidPlugin) {
            sb.append("    dependencies {").append(NL);
            sb.append("        classpath '" + ANDROID_GRADLE_PLUGIN + "'").append(NL);
            sb.append("    }").append(NL);
        }
        sb.append("}").append(NL);
    }

    private static void createProjectBuildGradle(@NonNull File file) throws IOException {
        Files.write("// Top-level build file where you can add configuration options " +
                "common to all sub-projects/modules." + NL, file,
                    Charsets.UTF_8);
    }

    private void createSettingsGradle(@NonNull File file) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (ImportedModule module : mModules) {
            sb.append("include '");
            sb.append(module.getModuleReference());
            sb.append("'");
            sb.append(NL);
        }

        Files.write(sb.toString(), file, Charsets.UTF_8);
    }

    private void createDestDir(@NonNull File destDir, boolean allowNonEmpty) throws IOException {
        if (destDir.exists()) {
          if (!allowNonEmpty) {
              File[] files = destDir.listFiles();
              if (files != null && files.length > 0) {
                  throw new IOException("Destination directory " + destDir + " should be empty");
              }
          }
        } else {
            mkdirs(destDir);
        }
    }

    public static void reportError(
            @Nullable EclipseProject project,
            @Nullable File file,
            @NonNull String message) throws IOException {
        String text = formatMessage(project, file, message);
        throw new IOException(text);
    }

    public void reportWarning(
            @Nullable EclipseProject project,
            @Nullable File file,
            @NonNull String message)  {
        mSummary.reportWarning(project, file, message);
    }

    static String formatMessage(
            @Nullable EclipseProject project,
            @Nullable File file,
            @NonNull String message) {
        StringBuilder sb = new StringBuilder();
        if (project != null) {
            sb.append("Project ").append(project.getName()).append(":");
        }
        if (file != null) {
            sb.append(file.getPath());
            sb.append(':');
        }

        sb.append(message);

        return sb.toString();
    }

    public String resolvePathVariable(String name) throws IOException {
        Properties properties = getJdkSettingsProperties(true);
        assert properties != null; // because mustExist=true, otherwise throws error
        String value = properties.getProperty("org.eclipse.jdt.core.classpathVariable." + name);
        if (value == null) {
            File settings = getSettingsFile();
            reportError(null, settings, "Didn't find path variable " + name + " definition in " +
                    settings);
            return null;
        }

        return value;
    }

    @Nullable
    private Properties getJdkSettingsProperties(boolean mustExist) throws IOException {
        File settings = getSettingsFile();
        if (!settings.exists()) {
            if (mustExist) {
                reportError(null, settings, "Settings file does not exist");
            }
            return null;
        }

        return getProperties(settings);
    }

    private File getSettingsFile() {
        return new File(getWorkspaceLocation(),
                ".plugins" + separator +
                "org.eclipse.core.runtime" + separator +
                ".settings" + separator +
                "org.eclipse.jdt.core.prefs");
    }


    private File getWorkspaceLocation() {
        return mWorkspace;
    }

    static Document getXmlDocument(File file, boolean namespaceAware) throws IOException {
        String xml = Files.toString(file, Charsets.UTF_8);
        Document document = XmlUtils.parseDocumentSilently(xml, namespaceAware);
        if (document == null) {
            throw new IOException("Invalid XML file: " + file.getPath());
        }
        return document;
    }

    static Properties getProperties(File file) throws IOException {
        Properties properties = new Properties();
        FileReader reader = new FileReader(file);
        properties.load(reader);
        Closeables.close(reader, true);
        return properties;
    }

    private Map<File, EclipseProject> mProjectMap = Maps.newHashMap();

    Map<File, EclipseProject> getProjectMap() {
        return mProjectMap;
    }

    private static boolean isEclipseWorkspaceDir(@NonNull File file) {
        return file.isDirectory() &&
                new File(file, ".metadata" + separator + "version.ini").exists();
    }

    public ImportSummary getSummary() {
        return mSummary;
    }

    void registerProject(EclipseProject project) {
        // Register not just this directory but the canonical versions too, since library
        // references in project.properties can be relative and can get canonicalized;
        // we want to make sure that a project known by any of these versions of the paths
        // are treated as the same
        mProjectMap.put(project.getDir(), project);
        mProjectMap.put(project.getDir().getAbsoluteFile(), project);
        mProjectMap.put(project.getCanonicalDir(), project);
    }

    /** Interface used by the {@link #copyDir(java.io.File, java.io.File, CopyHandler)} handler */
    public interface CopyHandler {
        /**
         * Optionally handle the given file; returns true if the file has been
         * handled
         */
        boolean handle(@NonNull File source, @NonNull File dest) throws IOException;
    }

    public void copyDir(@NonNull File source, @NonNull File dest, @Nullable CopyHandler handler)
            throws IOException {
        if (handler != null && handler.handle(source, dest)) {
            return;
        }
        if (source.isDirectory()) {
            mkdirs(dest);
            File[] files = source.listFiles();
            if (files != null) {
                for (File child : files) {
                    copyDir(child, new File(dest, child.getName()), handler);
                }
            }
        } else {
            Files.copy(source, dest);
        }
    }
}