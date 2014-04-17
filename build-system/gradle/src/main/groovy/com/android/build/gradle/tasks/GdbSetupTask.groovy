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

package com.android.build.gradle.tasks

import com.android.build.gradle.ndk.NdkExtension
import com.android.build.gradle.ndk.internal.NdkBuilder
import com.google.common.base.Charsets
import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.nativebinaries.BuildType
import org.gradle.nativebinaries.platform.Platform

class GdbSetupTask extends DefaultTask {
    NdkBuilder ndkBuilder
    NdkExtension ndkExtension
    BuildType buildType
    Platform platform

    @OutputFile
    File gdbsetupFile

    @Input
    boolean debuggable

    @OutputDirectory
    File outputFolder

    @Input
    boolean ndkRenderScriptMode

    @Input
    boolean ndkCygwinMode

    GdbSetupTask(NdkExtension ndkExtension, BuildType buildType, Platform platform) {
        this.ndkExtension = ndkExtension
        this.buildType = buildType
        this.platform = platform
    }

    @TaskAction
    void taskAction() {
        println "GdbSetupTask: $platform.name"

        StringBuilder sb = new StringBuilder()

        "/usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/build/ndk/arm/debug/lib/armeabi-v7a/gdb.setup"
        sb.append(
                "set solib-search-path /usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/build/ndk/arm/debug/obj/local/armeabi-v7a")
        sb.append("directory ")
        sb.append("
        /usr/local/google/home/chiur/dev/android-ndk-r9d/platforms/android-19/arch-arm/usr/include
        /usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/src/main/jni
        /usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/src/arm/jni
        /usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/src/debug/jni
        /usr/local/google/home/chiur/dev/work2/tools/base/build-system/tests/ndkSanAngeles/src/armDebug/jni
        ../../build/ndk/arm/debug /usr/local/google/home/chiur/dev/android-ndk-r9d/sources/cxx-stl/stlport")


        Files.write(sb.toString(), gdbsetupFile, Charsets.UTF_8)
    }

    private File getOutputFile() {
        new File(outputFolder, "gdb.setup")
    }
}
