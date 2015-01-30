/*
 * Copyright (C) 2015 The Android Open Source Project
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

import com.android.build.FilterData
import com.android.build.gradle.api.ApkOutputFile
import com.android.build.gradle.internal.tasks.BaseTask
import com.android.build.gradle.internal.tasks.OutputFileTask
import com.google.common.collect.ImmutableList

/**
 * Common code for all split related tasks
 */
abstract class SplitRelatedTask extends BaseTask {

    /**
     * Calculates the list of output files, coming from the list of input files, mangling the
     * output file name.
     */
    public abstract List<ApkOutputFile> getOutputSplitFiles()

    /**
     * Returns the list of split information for this task. Each split is a unique combination of
     * filter type and identifier.
     */
    public abstract List<FilterData> getSplitsData()

    /**
     * Returns a list of {@link OutputFileTask} for each split file that will return
     * the split APK.
     * @return
     */
    List<OutputFileTask> getOutputTasks() {
        ImmutableList.Builder<OutputFileTask> tasks = ImmutableList.builder();
        for (FilterData filterData : getSplitsData()) {
            tasks.add(new OutputFileTask() {

                @Override
                File getOutputFile() {
                    getOutputSplitFiles().find({
                        filterData.identifier.equals(it.getFilter(filterData.filterType))
                    }).getOutputFile()
                }
            })
        }
        return tasks.build()
    }
}
