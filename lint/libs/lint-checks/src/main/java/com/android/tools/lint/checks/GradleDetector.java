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

import java.io.File;
import java.util.Map;

/**
 * Checks Gradle files for potential errors
 */
public class GradleDetector extends Detector implements Detector.GradleScanner {
    private static final Implementation IMPLEMENTATION = new Implementation(
            GradleDetector.class,
            Scope.GRADLE_SCOPE);

    /** The main issue discovered by this detector */
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

    /** The main issue discovered by this detector */
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

    private int mCompileSdkVersion;

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

    protected static boolean isInterestingBlock(@NonNull String parent) {
        return parent.equals("defaultConfig")
                || parent.equals("android")
                || parent.equals("debug")
                || parent.equals("dependencies");
    }

    protected static boolean isInterestingProperty(@NonNull String property,
            @SuppressWarnings("UnusedParameters") @NonNull String parent) {
        return property.equals("targetSdkVersion")
                || property.equals("buildToolsVersion")
                || property.equals("compile")
                || property.equals("debugCompile")
                || property.equals("classpath")
                || property.equals("versionName")
                || property.equals("versionCode")
                || property.equals("compileSdkVersion")
                || property.equals("runProguard");
    }

    /** Called with for example "android", "defaultConfig", "minSdkVersion", "7"  */
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
                    context.isEnabled(ManifestDetector.TARGET_NEWER)) {
                    String message =
                            "Not targeting the latest versions of Android; compatibility " +
                            "modes apply. Consider testing and updating this version. " +
                           "Consult the android.os.Build.VERSION_CODES javadoc for details.";
                    context.report(ManifestDetector.TARGET_NEWER, createLocation(context, cookie),
                            message, null);
                }
            }
            if (property.equals("versionName") || property.equals("versionCode") &&
                    !isInteger(value) || !isStringLiteral(value)) {
                // Method call -- make sure it does not match one of the getters in the
                // configuration!
                if (value.equals("getVersionCode") ||
                        value.equals("getVersionName")) {
                    String message = "Bad method name: pick a unique method name which does not "
                            + "conflict with the implicit getters for the defaultConfig "
                            + "properties. For example, try using the prefix \"compute\" "
                            + "instead of \"get\".";
                    context.report(DEPENDENCY, createLocation(context, cookie), message, null);
                }
            }
        } else if (property.equals("compileSdkVersion") && parent.equals("android")) {
            int version = getIntLiteralValue(value, -1);
            if (version > 0) {
                mCompileSdkVersion = version;
            }
        } else if (property.equals("runProguard") && parent.equals("debug")) {
            // TODO: && parentParent == "buildTypes"
            if (SdkConstants.VALUE_TRUE.equals(value)) {
                String message = "Running ProGuard in debug mode does not work. Did you mean "
                        + "to put this in the release configuration?";
                context.report(DEPENDENCY, createLocation(context, cookie), message, null);
            }
        } else if (property.equals("buildToolsVersion") && parent.equals("android")) {
            String versionString = getStringLiteralValue(value);
            if (versionString != null) {
                FullRevision version = FullRevision.parseRevision(versionString);
                FullRevision recommended = new FullRevision(19, 0, 1);
                if (version.compareTo(recommended) < 0) {
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
                if (path.contains("\\\\")) {
                    String message = "Do not use Windows file separators in .gradle files; "
                            + "use / instead";
                    context.report(DEPENDENCY, createLocation(context, cookie), message, null);
                } else if (new File(path.replace('/', File.separatorChar)).isAbsolute()) {
                    String message = "Avoid using absolute paths in .gradle files";
                    context.report(DEPENDENCY, createLocation(context, cookie), message, null);
                }
            } else {
                String dependency = getStringLiteralValue(value);
                if (dependency != null) {
                    GradleCoordinate gc = GradleCoordinate.parseCoordinateString(dependency);
                    if (gc != null) {
                        checkDependency(context, gc, cookie);
                    }
                }
            }
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
        } catch (NumberFormatException nufe) {
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

    protected void checkDependency(
            @NonNull Context context,
            @NonNull GradleCoordinate dependency,
            @NonNull Object cookie) {
        boolean isObsolete = false;
        if ("com.google.guava".equals(dependency.getGroupId()) &&
                "guava".equals(dependency.getArtifactId())) {
            if (isOlderThan(dependency, 15, 0, 0)) {
                isObsolete = true;
            }
        } else if ("com.android.tools.build".equals(dependency.getGroupId()) &&
            "gradle".equals(dependency.getArtifactId())) {
            if (isOlderThan(dependency, 0, 7, 3)) {
                isObsolete = true;
            }
        } else if ("com.android.support".equals(dependency.getGroupId()) &&
                ("support-v4".equals(dependency.getArtifactId()) ||
                "appcompat-v7".equals(dependency.getArtifactId()))) {
            // See if it's a *different* version than the compile SDK!
            if (mCompileSdkVersion > 0 && !dependency.getFullRevision().startsWith(
                    mCompileSdkVersion + ".")) {
                String message = "Should use the same version of the support library as "
                        + "the compileSdkVersion (" + mCompileSdkVersion + "): " +
                        dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" +
                        mCompileSdkVersion + ":+";
                context.report(DEPENDENCY, createLocation(context, cookie), message, null);
            }
        } else if ("com.google.android.gms".equals(dependency.getGroupId()) &&
                "play-services".equals(dependency.getArtifactId())) {
            if (isOlderThan(dependency, 4, 0, 30)) {
                isObsolete = true;
            }
        } else if ("org.apache.httpcomponents".equals(dependency.getGroupId()) &&
                "httpclient".equals(dependency.getArtifactId())) {
            if (isOlderThan(dependency, 4, 3, 1)) {
                isObsolete = true;
            }
        }

        if (isObsolete) {
            String message = "A newer version of " + dependency.getGroupId() + ":" +
                    dependency.getGroupId() + " than " + dependency.getFullRevision() +
                    " is available";
            context.report(DEPENDENCY, createLocation(context, cookie), message, null);
        }
    }

    private static boolean isOlderThan(@NonNull GradleCoordinate dependency, int major, int minor,
            int micro) {
        assert dependency.getGroupId() != null;
        assert dependency.getArtifactId() != null;
        return dependency.compareTo(new GradleCoordinate(dependency.getGroupId(),
                dependency.getArtifactId(), major, minor, micro)) < 0;
    }

    protected Location createLocation(@NonNull Context context, @NonNull Object cookie) {
        return null;
    }
}
