/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.build.gradle;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidProject;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.gradle.api.Project;

import java.util.Map;

/**
 * Determines if various options, triggered from the command line or environment, are set.
 */
public class AndroidGradleOptions {

    private static final String PROPERTY_TEST_RUNNER_ARGS =
            "android.testInstrumentationRunnerArguments.";

    // TODO: Drop the "com." prefix, for consistency.
    private static final String PROPERTY_BENCHMARK_NAME = "com.android.benchmark.name";
    private static final String PROPERTY_BENCHMARK_MODE = "com.android.benchmark.mode";

    @NonNull
    public static Map<String, String> getExtraInstrumentationTestRunnerArgs(@NonNull Project project) {
        Map<String, String> argsMap = Maps.newHashMap();
        for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
            if (entry.getKey().startsWith(PROPERTY_TEST_RUNNER_ARGS)) {
                String argName = entry.getKey().substring(PROPERTY_TEST_RUNNER_ARGS.length());
                String argValue = entry.getValue().toString();

                argsMap.put(argName, argValue);
            }
        }

        return argsMap;
    }

    @Nullable
    public static String getBenchmarkName(@NonNull Project project) {
        return getString(project, PROPERTY_BENCHMARK_NAME);
    }

    @Nullable
    public static String getBenchmarkMode(@NonNull Project project) {
        return getString(project, PROPERTY_BENCHMARK_MODE);
    }

    public static boolean invokedFromIde(@NonNull Project project) {
        return getBoolean(project, AndroidProject.PROPERTY_INVOKED_FROM_IDE);
    }

    public static boolean buildModelOnly(@NonNull Project project) {
        return getBoolean(project, AndroidProject.PROPERTY_BUILD_MODEL_ONLY);
    }

    public static boolean buildModelOnlyAdvanced(@NonNull Project project) {
        return getBoolean(project, AndroidProject.PROPERTY_BUILD_MODEL_ONLY_ADVANCED);
    }

    @Nullable
    public static String getApkLocation(@NonNull Project project) {
        return getString(project, AndroidProject.PROPERTY_APK_LOCATION);
    }

    @NonNull
    public static String getSigningStoreFile(@NonNull Project project) {
        return Preconditions.checkNotNull(
                getString(project, AndroidProject.PROPERTY_SIGNING_STORE_FILE),
                "Property %s not set.",
                AndroidProject.PROPERTY_SIGNING_STORE_FILE);
    }

    @Nullable
    public static String getSigningStorePassword(@NonNull Project project) {
        return getString(project, AndroidProject.PROPERTY_SIGNING_STORE_PASSWORD);
    }

    @Nullable
    public static String getSigningKeyAlias(@NonNull Project project) {
        return getString(project, AndroidProject.PROPERTY_SIGNING_KEY_ALIAS);
    }

    @Nullable
    public static String getSigningKeyPassword(@NonNull Project project) {
        return getString(project, AndroidProject.PROPERTY_SIGNING_KEY_PASSWORD);
    }

    @Nullable
    public static String getSigningStoreType(@NonNull Project project) {
        return getString(project, AndroidProject.PROPERTY_SIGNING_STORE_TYPE);
    }
    
    @Nullable
    public static String getArchivesBaseName(@NonNull Project project) {
        return getString(project, "archivesBaseName");
    }

    @Nullable
    private static String getString(@NonNull Project project, String propertyName) {
        return (String) project.getProperties().get(propertyName);
    }

    private static boolean getBoolean(
            @NonNull Project project,
            @NonNull String propertyName) {
        if (project.hasProperty(propertyName)) {
            Object value = project.getProperties().get(propertyName);
            if (value instanceof String) {
                return Boolean.parseBoolean((String) value);
            }
        }

        return false;
    }

}
