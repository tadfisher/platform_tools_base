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
import org.gradle.api.Project
import org.gradle.nativebinaries.toolchain.Gcc

/**
 * Toolchain configuration for native binaries.
 */
class ToolchainConfiguration {

    private static final PLATFORM_STRING = [
            (SdkConstants.CPU_ARCH_INTEL_ATOM) : "x86",
            (SdkConstants.CPU_ARCH_ARM) : "arm-linux-androideabi",
            (SdkConstants.CPU_ARCH_MIPS) : "mipsel-linux-android"
    ]

    private static final GCC_PREFIX = [
            (SdkConstants.CPU_ARCH_INTEL_ATOM) : "i686-linux-android",
            (SdkConstants.CPU_ARCH_ARM) : "arm-linux-androideabi",
            (SdkConstants.CPU_ARCH_MIPS) : "mipsel-linux-android"
    ]

    private static final TOOLCHAIN_STRING = [
            "gcc" : "",
            "clang" : "clang3.4"
    ]

    private static final String DEFAULT_TOOLCHAIN = "gcc"
    private static final String DEFAULT_TOOLCHAIN_VERSION = "4.6"

    Project project
    NdkHelper ndkHelper

    ToolchainConfiguration(Project project, NdkHelper ndkHelper) {
        this.project = project
        this.ndkHelper = ndkHelper
    }

    public void configureToolchains() {
        // Create toolchain for each architecture.  Toolchain for x86 must be created first,
        // otherwise gradle may not choose the correct toolchain for a target platform.  This is
        // gradle always choose the first toolchain supporting a platform and there is no way to
        // remove x86 support in GCC or Clang toolchain.
        for (String architecture : [SdkConstants.CPU_ARCH_INTEL_ATOM, SdkConstants.CPU_ARCH_ARM]) {
            for (String toolchain : [DEFAULT_TOOLCHAIN]) {
                createToolchain(toolchain, DEFAULT_TOOLCHAIN_VERSION, architecture)
            }
        }
    }

    static private String getPrefix(String toolchain, String architecture) {
        if (toolchain.equals("gcc")) {
            return GCC_PREFIX[architecture]
        }
        return ""
    }

    private void createToolchain(String toolchain, String toolchainVersion, String architecture) {
        String name = getToolchainName(toolchain, toolchainVersion, architecture)
        String bin = getToolchainPath(toolchain, toolchainVersion, architecture).toString()
        project.model {
            toolChains {
                "$name"(Gcc) {
                    if (architecture.equals("arm")) {
                        addPlatformConfiguration(new ArmPlatformConfiguration())
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

    static private String getToolchainName(
            String toolchain,
            String toolchainVersion,
            String architecture) {
        PLATFORM_STRING[architecture] + "-" + TOOLCHAIN_STRING[toolchain] + toolchainVersion
    }

    private File getToolchainPath(String toolchain, String toolchainVersion, String architecture) {
        File prebuiltFolder = new File(
                ndkHelper.getNdkFolder(),
                "toolchains/${getToolchainName(toolchain, toolchainVersion, architecture)}/prebuilt")

        // This should detect the host architecture to determine the path of the prebuilt toolchain
        // instead of assuming there is only one folder in prebuilt directory.
        File[] toolchainFolder = prebuiltFolder.listFiles()
        new File(toolchainFolder[0], "${getPrefix(toolchain, architecture)}/bin")
    }
}
