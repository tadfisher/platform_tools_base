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
import org.gradle.api.tasks.OutputFiles;

import java.io.File;
import java.util.List;

/**
 * A task using Streams
 */
public class StreamBasedTask extends BaseTask {

    protected List<StreamDeclaration> consumedInputStreams;
    protected List<StreamDeclaration> referencedInputStreams;
    protected List<StreamDeclaration> outputStreams;

    @InputFiles
    public List<File> getStreamInputs() {
        List<File> inputs = Lists.newArrayList();
        for (StreamDeclaration s : consumedInputStreams) {
            try {
                inputs.addAll(s.getFiles().call());
            } catch (Exception e) {
                // ?
            }
        }

        for (StreamDeclaration s : referencedInputStreams) {
            try {
                inputs.addAll(s.getFiles().call());
            } catch (Exception e) {
                // ?
            }
        }

        return inputs;
    }

    @OutputDirectories
    public List<File> getStreamOutputFolders() {
        List<File> outputs = Lists.newArrayList();
        for (StreamDeclaration s : outputStreams) {
            if (s.isFolder()) {
                try {
                    outputs.addAll(s.getFiles().call());
                } catch (Exception e) {
                    // ?
                }
            }
        }

        return outputs;
    }

    @OutputFiles
    public List<File> getStreamOutputFiles() {
        List<File> outputs = Lists.newArrayList();
        for (StreamDeclaration s : outputStreams) {
            if (!s.isFolder()) {
                try {
                    outputs.addAll(s.getFiles().call());
                } catch (Exception e) {
                    // ?
                }
            }
        }

        return outputs;
    }
}
