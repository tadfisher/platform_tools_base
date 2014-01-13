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
package com.android.tools.lint.checks;

import static com.android.SdkConstants.FD_EXTRAS;
import static com.android.SdkConstants.FD_M2_REPOSITORY;
import static com.android.ide.common.repository.GradleCoordinate.COMPARE_PLUS_HIGHER;
import static com.android.tools.lint.checks.ManifestDetector.TARGET_NEWER;
import static java.io.File.separator;
import static java.io.File.separatorChar;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.repository.GradleCoordinate;
import com.android.ide.common.sdk.SdkVersionInfo;
import com.android.sdklib.repository.FullRevision;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Checks Gradle files for potential errors
 */
public class GradleDetector extends Detector implements Detector.GradleScanner {
    private static final Implementation IMPLEMENTATION = new Implementation(
            GradleDetector.class,
            Scope.GRADLE_SCOPE);

    /** Obsolete dependencies */
    public static final Issue DEPENDENCY = Issue.create(
            "GradleDependency", //$NON-NLS-1$
            "Obsolete Gradle Dependency",
            "Looks for old or obsolete Gradle library dependencies",
            "This detector looks for usages of libraries where the version you are using " +
            "is not the current stable release. Using older versions is fine, and there are " +
            "cases where you deliberately want to stick with an older version. However, " +
            "you may simply not be aware that a more recent version is available, and that is " +
            "what this lint check helps find.",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Invalid or dangerous paths */
    public static final Issue PATH = Issue.create(
            "GradlePath", //$NON-NLS-1$
            "Gradle Path Issues",
            "Looks for Gradle path problems such as using platform specific path separators",
            "Gradle build scripts are meant to be cross platform, so file paths use " +
            "Unix-style path separators (a forward slash) rather than Windows path separators " +
            "(a backslash). Similarly, to keep projects portable and repeatable, avoid " +
            "using absolute paths on the system; keep files within the project instead. To " +
            "share code between projects, consider creating an android-library and an AAR " +
            "dependency",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Constructs the IDE support struggles with */
    public static final Issue IDE_SUPPORT = Issue.create(
            "GradleIdeError", //$NON-NLS-1$
            "Gradle IDE Support Issues",
            "Looks for constructs in Gradle files which affect IDE usage",
            "Gradle is highly flexible, and there are things you can do in Gradle files which " +
            "can make it hard or impossible for IDEs to properly handle the project. This lint " +
            "check looks for constructs that potentially break IDE support.",
            Category.CORRECTNESS,
            4,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Using + in versions */
    public static final Issue PLUS = Issue.create(
            "GradleDynamicVersion", //$NON-NLS-1$
            "Gradle Dynamic Version",
            "Looks for dependencies using a dynamic version rather than a fixed version",
            "Using `+` in dependencies lets you automatically pick up the latest available " +
            "version rather than a specific, named version. However, this is not recommended; " +
            "your builds are not repeatable; you may have tested with a slightly different " +
            "version than what the build server used. (Using a dynamic version as the major " +
            "version number is more problematic than using it in the minor version position.)",
            Category.CORRECTNESS,
            4,
            Severity.ERROR,
            IMPLEMENTATION).setEnabledByDefault(false);

    /** Accidentally calling a getter instead of your own methods */
    public static final Issue GRADLE_GETTER = Issue.create(
            "GradleGetter", //$NON-NLS-1$
            "Gradle Implicit Getter Call",
            "Identifies accidental calls to implicit getters",
            "Gradle will let you replace specific constants in your build scripts with method " +
            "calls, so you can for example dynamically compute a version string based on your " +
            "current version control revision number, rather than hardcoding a number.\n" +
            "\n" +
            "When computing a version name, it's tempting to for example call the method to do " +
            "that `getVersionName`. However, when you put that method call inside the " +
            "`defaultConfig` block, you will actually be calling the Groovy getter for the "  +
            "`versionName` property instead. Therefore, you need to name your method something " +
            "which does not conflict with the existing implicit getters. Consider using " +
            "`compute` as a prefix instead of `get`.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Using incompatible versions */
    public static final Issue COMPATIBILITY = Issue.create(
            "GradleCompatible", //$NON-NLS-1$
            "Incompatible Gradle Versions",
            "Ensures that tool and library versions are compatible",

            "There are some combinations of libraries, or tools and libraries, that are " +
            "incompatible, or can lead to bugs. One such incompatibility is compiling with " +
            "a version of the Android support libraries that is not the latest version (or in " +
            "particular, a version lower than your `targetSdkVersion`.)",

            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            IMPLEMENTATION);

    private int mCompileSdkVersion;
    private int mTargetSdkVersion;

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    // ---- Implements Detector.GradleScanner ----

    @Override
    public void visitBuildScript(@NonNull Context context, Map<String, Object> sharedData) {
    }

    @SuppressWarnings("UnusedDeclaration")
    protected static boolean isInterestingBlock(@NonNull String parent) {
        return parent.equals("defaultConfig")
                || parent.equals("android")
                || parent.equals("dependencies");
    }

    @SuppressWarnings("UnusedDeclaration")
    protected static boolean isInterestingProperty(@NonNull String property,
            @SuppressWarnings("UnusedParameters") @NonNull String parent) {
        return property.equals("targetSdkVersion")
                || property.equals("buildToolsVersion")
                || property.equals("compile")
                || property.equals("debugCompile")
                || property.equals("classpath")
                || property.equals("versionName")
                || property.equals("versionCode")
                || property.equals("compileSdkVersion");
    }

    /** Called with for example "android", "defaultConfig", "minSdkVersion", "7"  */
    @SuppressWarnings("UnusedDeclaration")
    protected void checkDslPropertyAssignment(
        @NonNull Context context,
        @NonNull String property,
        @NonNull String value,
        @NonNull String parent,
        @NonNull Object cookie) {
        if (parent.equals("defaultConfig")) {
            if (property.equals("targetSdkVersion")) {
                int version = getIntLiteralValue(value, -1);
                if (version > 0 && version < SdkVersionInfo.HIGHEST_KNOWN_API &&
                    context.isEnabled(TARGET_NEWER)) {
                    String message =
                            "Not targeting the latest versions of Android; compatibility " +
                            "modes apply. Consider testing and updating this version. " +
                           "Consult the android.os.Build.VERSION_CODES javadoc for details.";
                    context.report(TARGET_NEWER, createLocation(context, cookie),
                            message, null);
                }
                if (version > 0) {
                    mTargetSdkVersion = version;
                    checkTargetCompatibility(context, cookie);
                }
            }
            if (property.equals("versionName") || property.equals("versionCode") &&
                    !isInteger(value) || !isStringLiteral(value)) {
                // Method call -- make sure it does not match one of the getters in the
                // configuration!
                if ((value.equals("getVersionCode") ||
                        value.equals("getVersionName")) && context.isEnabled(GRADLE_GETTER)) {
                    String message = "Bad method name: pick a unique method name which does not "
                            + "conflict with the implicit getters for the defaultConfig "
                            + "properties. For example, try using the prefix compute- "
                            + "instead of get-.";
                    context.report(GRADLE_GETTER, createLocation(context, cookie), message, null);
                }
            }
        } else if (property.equals("compileSdkVersion") && parent.equals("android")) {
            int version = getIntLiteralValue(value, -1);
            if (version > 0) {
                mCompileSdkVersion = version;
                checkTargetCompatibility(context, cookie);
            }
        } else if (property.equals("buildToolsVersion") && parent.equals("android")) {
            String versionString = getStringLiteralValue(value);
            if (versionString != null) {
                FullRevision version = FullRevision.parseRevision(versionString);
                FullRevision recommended = new FullRevision(19, 0, 1);
                if (version.compareTo(recommended) < 0  && context.isEnabled(DEPENDENCY)) {
                    String message = "Old buildToolsVersion; recommended version is " + recommended
                            + " or later";
                    context.report(DEPENDENCY, createLocation(context, cookie), message, null);
                }
            }
        } else if (parent.equals("dependencies") &&
                (property.equals("compile")
                        || property.equals("debugCompile")
                        || property.equals("classpath"))) {
            if (value.startsWith("files('") && value.endsWith("')")) {
                String path = value.substring("files('".length(), value.length() - 2);
                if (path.contains("\\\\") && context.isEnabled(PATH)) {
                    String message = "Do not use Windows file separators in .gradle files; "
                            + "use / instead";
                    context.report(PATH, createLocation(context, cookie), message, null);
                } else if (new File(path.replace('/', File.separatorChar)).isAbsolute() &&
                        context.isEnabled(PATH)) {
                    String message = "Avoid using absolute paths in .gradle files";
                    context.report(PATH, createLocation(context, cookie), message, null);
                }
            } else {
                String dependency = getStringLiteralValue(value);
                if (dependency != null) {
                    GradleCoordinate gc = GradleCoordinate.parseCoordinateString(dependency);
                    if (gc != null) {
                        checkDependency(context, gc, cookie);

                        if (!dependency.startsWith(SdkConstants.GRADLE_PLUGIN_NAME)
                                && gc.acceptsGreaterRevisions()
                                && context.isEnabled(PLUS)) {
                            String message = "Avoid using + in version numbers; can lead "
                                    + "to unpredictable and  unrepeatable builds";
                            context.report(DEPENDENCY, createLocation(context, cookie), message,
                                    null);
                        }
                    }
                }
            }
        }
    }

    private void checkTargetCompatibility(Context context, Object cookie) {
        if (mCompileSdkVersion > 0 && mTargetSdkVersion > 0
                && mCompileSdkVersion > mTargetSdkVersion) {
            String message = "The targetSdkVersion should not be higher than the compileSdkVersion";
            context.report(DEPENDENCY, createLocation(context, cookie), message, null);
        }
    }

    @Nullable
    private static String getStringLiteralValue(@NonNull String value) {
        if (value.length() > 2 && (value.startsWith("'") && value.endsWith("'") ||
                value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }

        return null;
    }

    private static int getIntLiteralValue(@NonNull String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean isInteger(String token) {
        return token.matches("\\d+");
    }

    private static boolean isStringLiteral(String token) {
        return token.startsWith("\"") && token.endsWith("\"") ||
                token.startsWith("'") && token.endsWith("'");
    }

    private void checkDependency(
            @NonNull Context context,
            @NonNull GradleCoordinate dependency,
            @NonNull Object cookie) {
        if ("com.android.support".equals(dependency.getGroupId()) &&
                ("support-v4".equals(dependency.getArtifactId()) ||
                        "appcompat-v7".equals(dependency.getArtifactId()))) {
            checkSupportLibraries(context, dependency, cookie);
            return;
        } else if ("com.google.android.gms".equals(dependency.getGroupId()) &&
                "play-services".equals(dependency.getArtifactId())) {
            checkPlayServices(context, dependency, cookie);
            return;
        }

        boolean isObsolete = false;
        if ("com.android.tools.build".equals(dependency.getGroupId()) &&
                "gradle".equals(dependency.getArtifactId())) {
            if (isOlderThan(dependency, 0, 7, 3)) {
                isObsolete = true;
            }
        } else if ("com.google.guava".equals(dependency.getGroupId()) &&
                "guava".equals(dependency.getArtifactId())) {
            if (isOlderThan(dependency, 15, 0, 0)) {
                isObsolete = true;
            }
        } else if ("com.google.code.gson".equals(dependency.getGroupId()) &&
                "gson".equals(dependency.getArtifactId())) {
            if (isOlderThan(dependency, 2, 2, 4)) {
                isObsolete = true;
            }
        } else if ("org.apache.httpcomponents".equals(dependency.getGroupId()) &&
                "httpclient".equals(dependency.getArtifactId())) {
            if (isOlderThan(dependency, 4, 3, 1)) {
                isObsolete = true;
            }
        }

        if (isObsolete && context.isEnabled(DEPENDENCY)) {
            String message = "A newer version of " + dependency.getGroupId() + ":" +
                    dependency.getArtifactId() + " than " + dependency.getFullRevision() +
                    " is available";
            context.report(DEPENDENCY, createLocation(context, cookie), message, null);
        }
    }

    private void checkSupportLibraries(Context context, GradleCoordinate dependency,
            Object cookie) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        assert groupId != null && artifactId != null;

        // See if the support library version is lower than the targetSdkVersion
        if (mTargetSdkVersion > 0 && dependency.getMajorVersion() < mTargetSdkVersion &&
                dependency.getMajorVersion() != GradleCoordinate.PLUS_REV &&
                context.isEnabled(COMPATIBILITY)) {
            String message = "The support library should not use a lower version ("
                + dependency.getMajorVersion() + ") than the targetSdkVersion ("
                    + mCompileSdkVersion + ")";
            context.report(COMPATIBILITY, createLocation(context, cookie), message, null);
        }

        // Check to make sure you have the Android support repository installed
        File sdkHome = context.getClient().getSdkHome();
        if (sdkHome != null) {
            File repository = new File(sdkHome, FD_EXTRAS + separator + "android" + separator
                    + FD_M2_REPOSITORY);
            if (!repository.exists()) {
                if (context.isEnabled(DEPENDENCY)) {
                    context.report(DEPENDENCY, createLocation(context, cookie),
                            "Dependency on a support library, but the SDK installation does not "
                                + "have the \"Extras > Android Support Repository\" installed. "
                                + "Open the SDK manager and install it.", null);
                }
            } else {
                checkLocalMavenVersions(context, dependency, cookie, groupId, artifactId,
                        repository);
            }
        }
    }

    private void checkPlayServices(Context context, GradleCoordinate dependency, Object cookie) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        assert groupId != null && artifactId != null;

        // Check to make sure you have the Google repository installed
        File sdkHome = context.getClient().getSdkHome();
        if (sdkHome != null) {
            File repository = new File(sdkHome, FD_EXTRAS + separator + "google" + separator
                    + FD_M2_REPOSITORY);
            if (!repository.exists()) {
                if (context.isEnabled(DEPENDENCY)) {
                    context.report(DEPENDENCY, createLocation(context, cookie),
                            "Dependency on Play Services, but the SDK installation does not "
                                    + "have the \"Extras > Google Repository\" installed. "
                                    + "Open the SDK manager and install it.", null);
                }
            } else {
                checkLocalMavenVersions(context, dependency, cookie, groupId, artifactId,
                        repository);
            }
        }
    }

    private void checkLocalMavenVersions(Context context, GradleCoordinate dependency,
            Object cookie, String groupId, String artifactId, File repository) {
        File versionDir = new File(repository,
                groupId.replace('.', separatorChar) + separator + artifactId);
        File[] versions = versionDir.listFiles();
        if (versions != null) {
            List<GradleCoordinate> versionCoordinates = Lists.newArrayList();
            for (File dir : versions) {
                if (!dir.isDirectory()) {
                    continue;
                }
                GradleCoordinate gc = GradleCoordinate.parseCoordinateString(
                        groupId + ":" + artifactId + ":" + dir.getName());
                if (gc != null) {
                    versionCoordinates.add(gc);
                }
            }
            if (!versionCoordinates.isEmpty()) {
                GradleCoordinate max = Collections.max(versionCoordinates, COMPARE_PLUS_HIGHER);
                if (COMPARE_PLUS_HIGHER.compare(dependency, max) < 0
                        && context.isEnabled(DEPENDENCY)) {
                    String message = "A newer version of " + groupId
                            + ":" + artifactId + " than " +
                            dependency.getFullRevision() + " is available: " +
                            max.getFullRevision();
                    context.report(DEPENDENCY, createLocation(context, cookie),
                            message, null);
                }
            }
        }
    }

    private static boolean isOlderThan(@NonNull GradleCoordinate dependency, int major, int minor,
            int micro) {
        assert dependency.getGroupId() != null;
        assert dependency.getArtifactId() != null;
        return COMPARE_PLUS_HIGHER.compare(dependency,
                new GradleCoordinate(dependency.getGroupId(),
                        dependency.getArtifactId(), major, minor, micro)) < 0;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedParameters"})
    protected Location createLocation(@NonNull Context context, @NonNull Object cookie) {
        return null;
    }
}
