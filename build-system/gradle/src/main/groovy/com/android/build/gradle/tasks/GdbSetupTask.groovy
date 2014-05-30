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
import com.android.build.gradle.ndk.NdkPlugin
import com.android.build.gradle.ndk.internal.NdkBuilder
import com.android.builder.core.BuilderConstants
import com.google.common.base.Charsets
import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.nativebinaries.BuildType
import org.gradle.nativebinaries.platform.Platform

/**
 * Task to create gdb.setup for native code debugging.
 */
class GdbSetupTask extends DefaultTask {
    @Input
    NdkBuilder builder

    @Input
    NdkExtension extension

    @Input
    Platform platform

    @Input
    BuildType type

    @TaskAction
    void taskAction() {
        File outputFolder = builder.getOutputDirectory(type, platform)
        File gdbSetupFile = new File(outputFolder, "gdb.setup")

        StringBuilder sb = new StringBuilder()

        sb.append("set solib-search-path ${outputFolder.toString()}\n")
        sb.append("directory ")
        sb.append("${builder.getSysroot(platform)}/usr/include ")
        List<String> sources =
                extension.sourceSets.getByName(BuilderConstants.MAIN).srcDirs*.toString() +
                        "${builder.getNdkDirectory()}/sources/cxx-stl/stlport"
        sb.append(sources.join(" "))

        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }
        Files.write(sb.toString(), gdbSetupFile, Charsets.UTF_8)
    }
}
