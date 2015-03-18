/*
 * Copyright (C) 2012 The Android Open Source Project
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
import com.android.build.gradle.internal.tasks.FileSupplier
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction

@ParallelizableTask
public class ZipAlign extends DefaultTask implements FileSupplier {

    // ----- PUBLIC TASK API -----

    @OutputFile
    File outputFile

    @InputFile
    File inputFile

    // ----- PRIVATE TASK API -----

    @InputFile
    File zipAlignExe

    @TaskAction
    void zipAlign() {
        project.exec {
            executable = getZipAlignExe()
            args '-f', '4'
            args getInputFile()
            args getOutputFile()
        }
    }

    // ----- FileSupplierTask -----

    @Override
    File get() {
        return getOutputFile()
    }

    @NonNull
    @Override
    Task getTask() {
        return this
    }
}
