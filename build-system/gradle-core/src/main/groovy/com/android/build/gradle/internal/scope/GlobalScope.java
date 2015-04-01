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

package com.android.build.gradle.internal.scope;

import static com.android.builder.model.AndroidProject.FD_OUTPUTS;
import static com.android.builder.model.AndroidProject.PROPERTY_APK_LOCATION;

import com.android.annotations.NonNull;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.internal.SdkHandler;
import com.android.builder.core.AndroidBuilder;

import org.gradle.api.Project;

import java.io.File;

/**
 * A scope containing data for the Android plugin.
 */
public class GlobalScope {
    @NonNull
    private Project project;
    @NonNull
    private AndroidBuilder androidBuilder;
    @NonNull
    private String projectBaseName;
    @NonNull
    private BaseExtension extension;
    private SdkHandler sdkHandler;

    public GlobalScope(
            @NonNull Project project,
            @NonNull AndroidBuilder androidBuilder,
            @NonNull String projectBaseName,
            @NonNull BaseExtension extension,
            @NonNull SdkHandler sdkHandler) {
        this.project = project;
        this.androidBuilder = androidBuilder;
        this.projectBaseName = projectBaseName;
        this.extension = extension;
        this.sdkHandler = sdkHandler;
    }

    @NonNull
    public Project getProject() {
        return project;
    }

    @NonNull
    public BaseExtension getExtension() {
        return extension;
    }

    @NonNull
    public AndroidBuilder getAndroidBuilder() {
        return androidBuilder;
    }

    @NonNull
    public String getProjectBaseName() {
        return projectBaseName;
    }

    @NonNull
    public SdkHandler getSdkHandler() {
        return sdkHandler;
    }

    @NonNull
    public File getBuildDir() {
        return project.getBuildDir();
    }

    @NonNull
    public String getDefaultApkLocation() {
        return getBuildDir() + "/" + FD_OUTPUTS + "/apk";
    }

    @NonNull
    public String getApkLocation() {
        String apkLocation = getDefaultApkLocation();
        if (project.hasProperty(PROPERTY_APK_LOCATION)) {
            apkLocation = (String) project.getProperties().get(PROPERTY_APK_LOCATION);
        }
        return apkLocation;
    }
}
