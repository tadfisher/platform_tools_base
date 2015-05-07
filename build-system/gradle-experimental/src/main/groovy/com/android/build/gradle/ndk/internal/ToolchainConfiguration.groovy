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
import com.android.build.gradle.internal.NdkHandler
import org.gradle.api.Action
import org.gradle.nativeplatform.platform.NativePlatform
import org.gradle.nativeplatform.toolchain.Clang
import org.gradle.nativeplatform.toolchain.Gcc
import org.gradle.nativeplatform.toolchain.GccCompatibleToolChain
import org.gradle.nativeplatform.toolchain.GccPlatformToolChain
import org.gradle.nativeplatform.toolchain.NativeToolChainRegistry
import org.gradle.platform.base.PlatformContainer

/**
 * Action to configure toolchain for native binaries.
 */
class ToolchainConfiguration {

    private static final String DEFAULT_GCC32_VERSION="4.6"
    private static final String DEFAULT_GCC64_VERSION="4.9"
    private static final String DEFAULT_LLVM_VERSION="3.4"

    private static final GCC_PREFIX = [
            (SdkConstants.ABI_INTEL_ATOM) : "i686-linux-android",
            (SdkConstants.ABI_INTEL_ATOM64) : "x86_64-linux-android",
            (SdkConstants.ABI_ARMEABI_V7A) : "arm-linux-androideabi",
            (SdkConstants.ABI_ARMEABI) : "arm-linux-androideabi",
            (SdkConstants.ABI_ARM64_V8A) : "aarch64-linux-android",
            (SdkConstants.ABI_MIPS) : "mipsel-linux-android",
            (SdkConstants.ABI_MIPS64) : "mips64el-linux-android"
    ]

    public static void configurePlatforms(PlatformContainer platforms, NdkHandler ndkHandler) {
        for (String abi : ndkHandler.getSupportedAbis()) {
            NativePlatform platform = platforms.maybeCreate(abi, NativePlatform)

            // All we care is the name of the platform.  It doesn't matter what the
            // architecture is, but it must be set to non-x86 so that it does not match
            // the default supported platform.
            platform.architecture "ppc"
            platform.operatingSystem "linux"
        }
    }

    /**
     * Configure toolchain for a platform.
     */
    public static void configureToolchain(
            NativeToolChainRegistry toolchainRegistry,
            String toolchainName,
            NdkHandler ndkHandler) {

        toolchainRegistry.create(
                "ndk-" + toolchainName,
                toolchainName.equals("gcc") ? Gcc : Clang) { GccCompatibleToolChain toolchain ->
            // Configure each platform.
            for (String abi : ndkHandler.getSupportedAbis()) {
                final String platform = abi

                toolchain.target(platform, new Action<GccPlatformToolChain>() {
                    @Override
                    void execute(GccPlatformToolChain targetPlatform) {
                        if (toolchainName.equals("gcc")) {
                            String gccPrefix = NdkHandler.getGccToolchainPrefix(platform)
                            targetPlatform.cCompiler.setExecutable("$gccPrefix-gcc")
                            targetPlatform.cppCompiler.setExecutable("$gccPrefix-g++")
                            targetPlatform.linker.setExecutable("$gccPrefix-g++")
                            targetPlatform.assembler.setExecutable("$gccPrefix-as")
                            targetPlatform.staticLibArchiver.setExecutable("$gccPrefix-ar")
                        }

                        // By default, gradle will use -Xlinker to pass arguments to the linker.
                        // Removing it as it prevents -sysroot from being properly set.
                        targetPlatform.linker.withArguments(new Action<List<String>>() {
                            @Override
                            void execute(List<String> args) {
                                args.removeAll("-Xlinker")
                            }
                        })
                    }
                })
                toolchain.path(ndkHandler.getCCompiler(abi).getParentFile())
            }
        }
    }
}
