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

package com.android.build.gradle.internal.pipeline;

import com.android.build.gradle.internal.tasks.BaseTask;
import com.google.common.collect.Lists;

import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectories;

import java.io.File;
import java.util.List;

/**
 * A task using Streams
 */
public class StreamBasedTask extends BaseTask {

    protected List<Stream> inputStreams;
    protected List<Stream> outputStreams;

    @InputFiles
    public List<File> getStreamInputs() {
        List<File> inputs = Lists.newArrayList();
        for (Stream s : inputStreams) {
            try {
                inputs.addAll(s.getFiles().call());
            } catch (Exception e) {
                // ?
            }
        }

        return inputs;
    }

    @OutputDirectories
    public List<File> getStreamOutputs() {
        List<File> outputs = Lists.newArrayList();
        for (Stream s : outputStreams) {
            try {
                outputs.addAll(s.getFiles().call());
            } catch (Exception e) {
                // ?
            }
        }

        return outputs;
    }
}
