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

import org.gradle.api.Action;
import org.gradle.nativebinaries.platform.Platform
import org.gradle.nativebinaries.toolchain.ToolChain
import org.gradle.nativebinaries.toolchain.TargetPlatformConfiguration

/**
 * A generic PlatformConfiguration that supports a platform with a specific name.
 */
class DefaultPlatformConfiguration implements TargetPlatformConfiguration {
    private String platformName

    public DefaultPlatformConfiguration(String platformName) {
        this.platformName =  platformName
    }

    public boolean supportsPlatform(Platform targetPlatform) {
        platformName.equals(targetPlatform.name)
    }

    public List<String> getAssemblerArgs() {
        Collections.emptyList()
    }

    public List<String> getCppCompilerArgs() {
        Collections.emptyList()
    }

    public List<String> getCCompilerArgs() {
        Collections.emptyList()
    }

    public List<String> getObjectiveCppCompilerArgs() {
        Collections.emptyList()
    }

    public List<String> getObjectiveCCompilerArgs() {
        Collections.emptyList()
    }

    public List<String> getStaticLibraryArchiverArgs() {
        Collections.emptyList()
    }

    public List<String> getLinkerArgs() {
        Collections.emptyList()
    }
}
