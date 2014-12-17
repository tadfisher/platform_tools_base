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

package com.android.build.gradle.internal.process;

import com.android.annotations.NonNull;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessOutput;

import org.gradle.api.Project;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 */
public class ProcessHelper {

    @NonNull
    public ProcessOutput createProcessOutput() {
        return new ProcessOutput() {
            private final OutputStream mStandardOutput = new ByteArrayOutputStream();
            private final OutputStream mErrorOutput = new ByteArrayOutputStream();

            @NonNull
            @Override
            public OutputStream getStandardOutput() {
                return mStandardOutput;
            }

            @NonNull
            @Override
            public OutputStream getErrorOutput() {
                return mErrorOutput;
            }
        };
    }

    public void logProcessOutput(
            @NonNull ProcessOutput processOutput,
            @NonNull Project project) throws ProcessException {
        OutputStream stdout = processOutput.getStandardOutput();
        if (stdout instanceof ByteArrayOutputStream) {
            String content = getString((ByteArrayOutputStream) stdout);
            project.getLogger().info(content);
        }

        OutputStream stderr = processOutput.getStandardOutput();
        if (stderr instanceof ByteArrayOutputStream) {
            String content = getString((ByteArrayOutputStream) stderr);
            project.getLogger().error(content);
        }
    }

    private static String getString(@NonNull ByteArrayOutputStream stream) throws ProcessException {
        try {
            return stream.toString(Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) {
            throw new ProcessException(e);
        }
    }
}
