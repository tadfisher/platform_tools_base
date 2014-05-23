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

import com.android.SdkConstants
import com.android.build.gradle.ndk.NdkExtension
import org.gradle.api.Project
import org.gradle.nativebinaries.toolchain.Clang
import org.gradle.nativebinaries.toolchain.Gcc

/**
 * Toolchain configuration for native binaries.
 */
class ToolchainConfiguration {

    private static final GCC_PREFIX = [
            (SdkConstants.ABI_INTEL_ATOM) : "i686-linux-android",
            (SdkConstants.ABI_ARMEABI_V7A) : "arm-linux-androideabi",
            (SdkConstants.ABI_ARMEABI) : "arm-linux-androideabi",
            (SdkConstants.ABI_MIPS) : "mipsel-linux-android"
    ]

    Project project
    NdkBuilder ndkBuilder
    NdkExtension ndkExtension

    ToolchainConfiguration(Project project, NdkBuilder ndkBuilder, NdkExtension ndkExtension) {
        this.project = project
        this.ndkBuilder = ndkBuilder
        this.ndkExtension = ndkExtension
    }

    public void configureToolchains() {
        // Create toolchain for each architecture.  Toolchain for x86 must be created first,
        // otherwise gradle may not choose the correct toolchain for a target platform.  This is
        // because gradle always choose the first toolchain supporting a platform and there is no
        // way to remove x86 support in GCC or Clang toolchain.
        for (String platform : [
                SdkConstants.ABI_INTEL_ATOM,
                SdkConstants.ABI_ARMEABI_V7A,
                SdkConstants.ABI_ARMEABI,
                SdkConstants.ABI_MIPS]) {
            createToolchain(ndkExtension.getToolchain(), ndkExtension.getToolchainVersion(), platform)
        }
    }

    private static String getPrefix(String toolchain, String platform) {
        if (toolchain.equals("gcc")) {
            return GCC_PREFIX.get(platform);
        }
        return "";
    }


    private void createToolchain(String toolchainName, String toolchainVersion, String platform) {
        String name = NdkBuilder.getToolchainName(toolchainName, toolchainVersion, platform)
        String bin = (
                ndkBuilder.getToolchainPath(toolchainName, toolchainVersion, platform).toString()
                + "/bin")

        project.model {
            toolChains {
                "$name"(toolchainName.equals("gcc") ? Gcc : Clang) {
                    addPlatformConfiguration(new DefaultPlatformConfiguration(platform))

                    if (toolchainName.equals("gcc")) {
                        cCompiler.setExecutable("${GCC_PREFIX[platform]}-gcc")
                        cppCompiler.setExecutable("${GCC_PREFIX[platform]}-g++")
                        linker.setExecutable("${GCC_PREFIX[platform]}-g++")
                        assembler.setExecutable("${GCC_PREFIX[platform]}-as")
                        staticLibArchiver.setExecutable("${GCC_PREFIX[platform]}-ar")
                    }

                    // By default, gradle will use -Xlinker to pass arguments to the linker.
                    // Removing it as it prevents -sysroot from being properly set.
                    linker.withArguments { List<String> args ->
                        args.removeAll("-Xlinker")
                    }
                    path bin
                }
            }
        }
    }

}
