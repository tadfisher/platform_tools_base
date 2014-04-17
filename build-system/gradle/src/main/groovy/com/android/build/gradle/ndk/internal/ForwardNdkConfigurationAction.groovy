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

import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.BaseExtension;
import com.android.build.gradle.BasePlugin;
import com.android.build.gradle.LibraryPlugin;
import com.android.build.gradle.ndk.NdkExtension
import com.android.builder.core.DefaultBuildType
import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import org.gradle.api.Action;
import org.gradle.api.Project;

/**
 * Created by chiur on 6/27/14.
 */
public class ForwardNdkConfigurationAction implements Action<Project>{

    public void execute(Project project) {
        BasePlugin androidPlugin = project.getPlugins().findPlugin(AppPlugin.class);
        if (androidPlugin == null) {
            androidPlugin = project.getPlugins().findPlugin(LibraryPlugin.class);
        }
        if (androidPlugin == null) {
            return;
        }
        NdkExtension extension = project.getExtensions().getByType(NdkExtension.class);
        BaseExtension androidExtension = androidPlugin.getExtension();
        if (extension.getCompileSdkVersion() == null) {
            // Retrieve compileSdkVersion from Android plugin if it is not set for the NDK plugin.
            extension.setCompileSdkVersion(androidExtension.getCompileSdkVersion());
        }

//        mergeNdkExtension(extension, androidExtension.getNdk())

        androidExtension.getBuildTypes().all { BuildType buildType ->
            project.model {
                buildTypes {
                    maybeCreate(buildType.name)
                }
            }
        }

        androidExtension.getProductFlavors().all { ProductFlavor flavor ->
            project.model {
                flavors {
                    maybeCreate(flavor.name)
                }
            }
        }
    }

    private static void mergeNdkExtension(NdkExtension ext1, NdkExtension ext2) {
        ext1.moduleName = ext1.moduleName ?: ext2.moduleName
        ext1.compileSdkVersion(ext1.compileSdkVersion ?: ext2.compileSdkVersion)
        ext1.cFlags = [ext1.cFlags, ext2.cFlags].join(" ")
        ext1.cppFlags = [ext1.cppFlags, ext2.cppFlags].join(" ")
        if (ext2.ldLibs != null) {
            ext1.ldLibs?.addAll(ext2.ldLibs ?: [])
        }
        ext1.toolchain = ext1.toolchain ?: ext2.toolchain
        ext1.toolchainVersion = ext1.toolchainVersion ?: ext2.toolchainVersion
        ext1.stl = ext1.stl ?: ext2.stl
        ext1.renderscriptNdkMode = ext1.renderscriptNdkMode ?: ext2.renderscriptNdkMode
        if (ext2.getSourceSets() != null) {
            ext1.getSourceSets()?.addAll(ext2.getSourceSets())
        }
    }
}
