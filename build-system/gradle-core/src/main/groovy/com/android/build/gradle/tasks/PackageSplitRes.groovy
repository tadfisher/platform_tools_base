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
import com.android.build.gradle.internal.model.FilterDataImpl
import com.android.builder.model.SigningConfig
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Callables
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Package each split resources into a specific signed apk file.
 */
@ParallelizableTask
class PackageSplitRes extends SplitRelatedTask {

    @Input
    Set<String> densitySplits

    @Input
    Set<String> languageSplits

    @Input
    String outputBaseName

    @Nested @Optional
    SigningConfig signingConfig

    @InputFiles
    List<File> getInputFiles() {
        ImmutableList.Builder<File> builder = ImmutableList.builder();
        forEachInputFile { split, file ->
            builder.add(file)
        }
        return builder.build()
    }

    @OutputFiles
    public List<File> getOutputFiles() {
        getOutputSplitFiles()*.getOutputFile()
    }

    /**
     * This directories are not officially input/output to the task as
     * they are shared among tasks. To be parallelizable, we must only
     * define our I/O in terms of files...
     */
    File inputDirectory
    File outputDirectory

    /**
     * Calculates the list of output files, coming from the list of input files, mangling the
     * output file name.
     */
    public List<ApkOutputFile> getOutputSplitFiles() {
        ImmutableList.Builder<ApkOutputFile> builder = ImmutableList.builder();
        forEachInputFile { String split, File file ->
            // find the split identification, if null, the split is not requested any longer.
            FilterData filterData = null;
            for (String density : densitySplits) {
                if (split.startsWith(density)) {
                    filterData = FilterDataImpl.build(
                            OutputFile.FilterType.DENSITY.toString(), density)
                }
            }
            if (languageSplits.contains(split)) {
                filterData = FilterDataImpl.build(
                        OutputFile.FilterType.LANGUAGE.toString(), split);
            }
            if (filterData != null) {
                builder.add(new ApkOutputFile(OutputFile.OutputType.SPLIT,
                        ImmutableList.of(filterData),
                        Callables.<File>returning(
                                new File(outputDirectory, this.getOutputFileNameForSplit(split)))))
            }
        }
        return builder.build();
    }

    @TaskAction
    protected void doFullTaskAction() {

        forEachInputFile { String split, File file ->
            File outFile = new File(outputDirectory, this.getOutputFileNameForSplit(split));
            getBuilder().signApk(file, signingConfig, outFile)
        }
    }

    /**
     * Runs the closure for each task input file, providing the split identifier (possibly with
     * a suffix generated by aapt) and the input file handle.
     * @param closure groovy closure to run on each input file.
     */
    public void forEachInputFile(Closure closure) {
        Pattern resourcePattern = Pattern.compile(
                "resources-${outputBaseName}.ap__(.*)")

        // resources- and .ap_ should be shared in a setting somewhere. see BasePlugin:1206
        for (File file : inputDirectory.listFiles()) {
            Matcher match = resourcePattern.matcher(file.getName());
            if (match.matches() && !match.group(1).isEmpty() && isValidSplit(match.group(1))) {
                closure(match.group(1), file)
            }
        }
    }

    /**
     * Returns true if the passed split identifier is a valid identifier (valid mean it is a
     * requested split for this task). A density split identifier can be suffixed with characters
     * added by aapt.
     */
    private boolean isValidSplit(@NonNull String splitWithOptionalSuffix) {
        for (String density : densitySplits) {
            if (splitWithOptionalSuffix.startsWith(density)) {
                return true;
            }
        }
        return languageSplits.contains(splitWithOptionalSuffix);
    }

    String getOutputFileNameForSplit(String split) {
        String apkName = "${project.archivesBaseName}-${outputBaseName}_${split}"
        return apkName + (signingConfig == null ? "-unsigned.apk" : "-unaligned.apk")
    }

    @Override
    List<FilterData> getSplitsData() {
        ImmutableList.Builder<FilterData> filterDataBuilder = ImmutableList.builder();
        addAllFilterData(filterDataBuilder, densitySplits, OutputFile.FilterType.DENSITY);
        addAllFilterData(filterDataBuilder, languageSplits, OutputFile.FilterType.LANGUAGE);
        return filterDataBuilder.build();
    }
}
