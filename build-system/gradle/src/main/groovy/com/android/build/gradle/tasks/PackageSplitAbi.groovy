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

import com.android.annotations.NonNull
import com.android.build.FilterData
import com.android.build.OutputFile
import com.android.build.gradle.api.ApkOutputFile
import com.android.build.gradle.internal.dsl.PackagingOptionsImpl
import com.android.build.gradle.internal.dsl.SigningConfigDsl
import com.android.build.gradle.internal.tasks.BaseTask
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.util.concurrent.Callables
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Created by jedo on 10/28/14.
 */
class PackageSplitAbi extends BaseTask {

    ImmutableList<ApkOutputFile> mOutputFiles;

    @Input
    File inputDirectory

    @OutputDirectory
    File outputDirectory

    @Nested
    Set<String> splits

    @Input
    String outputBaseName

    @Input
    boolean jniDebuggable

    @Nested @Optional
    SigningConfigDsl signingConfig

    @Nested
    PackagingOptionsImpl packagingOptions

    @Input
    Collection<File> jniFolders;

    @NonNull
    public synchronized  ImmutableList<ApkOutputFile> getOutputSplitFiles() {

        if (mOutputFiles == null) {
            ImmutableList.Builder<ApkOutputFile> builder = ImmutableList.builder();
            if (outputDirectory.exists() && outputDirectory.listFiles().length > 0) {
                final Pattern pattern = Pattern.compile(
                        "${project.archivesBaseName}-${outputBaseName}-(.*)")
                for (File file : outputDirectory.listFiles()) {
                    Matcher matcher = pattern.matcher(file.getName());
                    if (matcher.matches() && isAbiSplit(file.getName())) {
                        builder.add(new ApkOutputFile(
                                OutputFile.OutputType.SPLIT,
                                ImmutableList.<FilterData> of(FilterData.Builder.build(
                                        OutputFile.ABI,
                                        matcher.group(1))),
                                Callables.returning(file)));
                    }
                }
            } else {
                // the project has not been built yet so we extrapolate what the package step result
                // might look like. So far, we only handle density splits, eventually we will need
                // to disambiguate.
                for (String split : splits) {
                    ApkOutputFile apkOutput = new ApkOutputFile(
                            OutputFile.OutputType.SPLIT,
                            ImmutableList.<FilterData>of(
                                    FilterData.Builder.build(OutputFile.ABI,
                                            "${project.archivesBaseName}-${outputBaseName}-${split}")),
                            Callables.returning(new File(outputDirectory, split)))
                    builder.add(apkOutput)
                }
            }
            mOutputFiles = builder.build()
        }
        return mOutputFiles;
    }

    private boolean isAbiSplit(String fileName) {
        for (String abi : splits) {
            if (fileName.contains(abi)) {
                return true;
            }
        }
        return false;
    }

    @TaskAction
    protected void doFullTaskAction() {

        // resources- and .ap_ should be shared in a setting somewhere. see BasePlugin:1206
        final Pattern pattern = Pattern.compile(
                "resources-${outputBaseName}-(.*).ap_")
        for (File file : inputDirectory.listFiles()) {
            Matcher matcher = pattern.matcher(file.getName());
            if (matcher.matches()) {
                ApkOutputFile outputFile = new ApkOutputFile(
                        OutputFile.OutputType.SPLIT,
                        ImmutableList.<FilterData> of(FilterData.Builder.build(
                                OutputFile.ABI,
                                matcher.group(1))),
                        Callables.returning(file));

                String apkName = "${project.archivesBaseName}-${outputBaseName}-" +
                        "${outputFile.getSplitIdentifiers('-' as char)}"
                apkName = apkName + (signingConfig == null
                        ? "-unsigned.apk"
                        : "-unaligned.apk")

                File outFile = new File(outputDirectory, apkName);
                getBuilder().packageApk(
                        file.absolutePath,
                        null, /* dexFolder */
                        null, /* dexedLibraries */
                        ImmutableList.of(),
                        null, /* getJavaResourceDir */
                        getJniFolders(),
                        ImmutableSet.of(matcher.group(1)),
                        getJniDebuggable(),
                        getSigningConfig(),
                        getPackagingOptions(),
                        outFile.absolutePath)
            }
        }
    }
}
