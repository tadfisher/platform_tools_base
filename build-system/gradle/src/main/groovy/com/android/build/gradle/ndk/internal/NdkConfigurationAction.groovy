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

package com.android.build.gradle.ndk.internal

import com.android.builder.model.NdkConfig
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.nativebinaries.BuildType
import org.gradle.nativebinaries.SharedLibraryBinary
import org.gradle.nativebinaries.platform.Platform

/**
 * Configure settings used by the native binaries.
 */
class NdkConfigurationAction implements Action<Project> {


    NdkConfig ndkConfig
    NdkHelper ndkHelper

    NdkConfigurationAction (NdkConfig ndkConfig, NdkHelper ndkHelper) {
        this.ndkConfig = ndkConfig
        this.ndkHelper = ndkHelper
    }

    public void execute(Project project) {
        project.libraries {
            create(ndkConfig.getModuleName())
        }

        configureProperties(project)
    }

    void configureProperties(Project project) {
        project.sources.getByName(ndkConfig.getModuleName()) {
            c {
                source {
                    srcDir "src/main/jni"
                    include "**/*.c"
                }
            }
            cpp {
                source {
                    srcDir "src/main/jni"
                    include "**/*.cpp"
                    include "**/*.cc"
                }
            }
        }

        project.libraries.getByName(ndkConfig.getModuleName()) {
            binaries.withType(SharedLibraryBinary.class) {
                cCompiler.define "ANDROID_NDK"
                cppCompiler.define "ANDROID_NDK"

                sharedLibraryFile = new File(
                        getOutputDirectory(project, buildType, targetPlatform),
                        "/lib" + ndkConfig.getModuleName() + ".so")

                String sysroot = ndkHelper.getSysroot(targetPlatform, ndkConfig.apiLevel)

                cCompiler.args  "--sysroot=$sysroot"
                cppCompiler.args  "--sysroot=$sysroot"
                linker.args "--sysroot=$sysroot"
                FlagConfiguration flagConfig =
                        FlagConfigurationFactory.create(buildType, targetPlatform)

                for (String arg : flagConfig.getCFlags()) {
                    cCompiler.args arg
                }
                for (String arg : flagConfig.getCppFlags()) {
                    cppCompiler.args arg
                }
                for (String arg : flagConfig.getLdFlags()) {
                    linker.args arg
                }

                // Add flags defined in NdkConfig
                if (ndkConfig.getcFlags() != null) {
                    cCompiler.args ndkConfig.getcFlags()
                }

                if (ndkConfig.getCppFlags() != null) {
                    cppCompiler.args ndkConfig.getCppFlags()
                }

                for (String ldLibs : ndkConfig.getLdLibs()) {
                    linker.args "-l$ldLibs"
                }
            }
        }
    }

    /**
     * Return the output directory of the native binary.
     */
    public File getOutputDirectory(Project project, BuildType buildType, Platform platform) {
        new File("$project.buildDir/binaries/",
                "${ndkConfig.getModuleName()}SharedLibrary/$buildType.name/lib/$platform.name")
    }
}
