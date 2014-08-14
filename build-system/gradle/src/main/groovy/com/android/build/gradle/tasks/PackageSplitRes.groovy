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

import com.android.build.gradle.internal.dsl.SigningConfigDsl
import com.android.build.gradle.internal.tasks.IncrementalTask
import com.google.common.base.Joiner
import groovy.io.FileType
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory

import java.util.regex.Pattern

/**
 * Package each split resources into a specific signed apk file.
 */
class PackageSplitRes extends IncrementalTask {

    @Input
    File inputDirectory

    @OutputDirectory
    File outputDirectory

    @Nested
    Set<String> splits

    @Input
    String outputBaseName

    @Nested @Optional
    SigningConfigDsl signingConfig

    @Override
    protected void doFullTaskAction() {

        String allSplits = Joiner.on('|').join(splits);
        Pattern pattern = Pattern.compile(
                "resources-${outputBaseName}.ap__([${allSplits}].*)")

        inputDirectory.eachFile(FileType.FILES, { inputFile ->

            def matcher = pattern.matcher(inputFile.getName())
            if (matcher.matches()) {
                String apkName = "${project.archivesBaseName}-${outputBaseName}-${matcher.group(1)}"
                apkName = apkName + (signingConfig == null
                    ? "-unsigned.apk"
                    : "-unaligned.apk")
                getBuilder().signApk(inputFile, signingConfig, new File(outputDirectory, apkName))
            }
        })
    }
}
