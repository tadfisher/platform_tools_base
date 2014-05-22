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

package com.android.build.gradle.ndk.internal;

import com.android.build.gradle.api.AndroidSourceDirectorySet;
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet;
import com.android.build.gradle.ndk.NdkExtension;
import com.android.builder.BuilderConstants;

import org.gradle.api.Action;
import org.gradle.api.internal.project.ProjectInternal;

public class NdkExtensionConventionAction implements Action<ProjectInternal> {
    private static final String DEFAULT_TOOLCHAIN = "gcc";
    private static final String DEFAULT_GCC_VERSION = "4.6";
    private static final String DEFAULT_CLANG_VERSION = "3.3";

    @Override
    public void execute(ProjectInternal project) {
        NdkExtension extension = project.getExtensions().getByType(NdkExtension.class);

        if (extension.getToolchain() == null) {
            extension.setToolchain(DEFAULT_TOOLCHAIN);
        }
        if (extension.getToolchainVersion() == null) {
            // Currently supports gcc and clang.
            extension.setToolchainVersion(
                    extension.getToolchain().equals("gcc")
                            ? DEFAULT_GCC_VERSION
                            : DEFAULT_CLANG_VERSION);
        }

        AndroidSourceDirectorySet sourceSet =
                extension.getSourceSets().maybeCreate(BuilderConstants.MAIN);
        if (sourceSet.getSrcDirs().isEmpty()) {
            sourceSet.srcDir("src/" + sourceSet.getName() + "/jni");
        }
    }
}
