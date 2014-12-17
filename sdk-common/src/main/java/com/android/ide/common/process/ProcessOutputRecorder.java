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

package com.android.ide.common.process;

import com.android.annotations.NonNull;
import com.google.common.base.Splitter;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Default ProcessOutput that simply records the output.
 */
public class ProcessOutputRecorder implements ProcessOutput {

    private final ByteArrayOutputStream mStandardOutput = new ByteArrayOutputStream();
    private final ByteArrayOutputStream mErrorOutput = new ByteArrayOutputStream();

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

    @NonNull
    public List<String> getOutLines() throws UnsupportedEncodingException {
        String content = mStandardOutput.toString(Charset.defaultCharset().name());
        return Splitter.on('\n').splitToList(content);
    }

    @NonNull
    public List<String> getErrLines() throws UnsupportedEncodingException {
        String content = mErrorOutput.toString(Charset.defaultCharset().name());
        return Splitter.on('\n').splitToList(content);
    }
}
