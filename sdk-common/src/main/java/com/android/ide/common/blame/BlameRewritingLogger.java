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

package com.android.ide.common.blame;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.blame.output.GradleMessage;
import com.android.ide.common.blame.parser.ToolOutputParser;
import com.android.ide.common.blame.parser.aapt.AaptOutputParser;
import com.android.utils.ILogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class BlameRewritingLogger implements ILogger {

    public static enum ErrorFormatMode {
        MACHINE_PARSABLE, HUMAN_READABLE
    }

    public static final String STDOUT_ERROR_TAG = "Android Gradle Plugin - Build Issue: ";

    private final ILogger mLogger;

    private ErrorFormatMode mErrorFormatMode;

    private final ToolOutputParser mParser;

    private final Gson mGson;

    public BlameRewritingLogger(@NonNull ILogger logger, @NonNull ErrorFormatMode errorFormatMode) {
        this.mLogger = logger;
        mErrorFormatMode = errorFormatMode;
        mParser = new ToolOutputParser(new AaptOutputParser(), logger);
        mGson = setUpGson();
    }

    private Gson setUpGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(SourceFragmentPositionRange.class,
                new SourceFragmentPositionRange.Serializer());
        if (mErrorFormatMode == ErrorFormatMode.HUMAN_READABLE) {
            gsonBuilder.setPrettyPrinting();
        }
        return gsonBuilder.create();
    }

    @Override
    public void error(@Nullable Throwable t, @Nullable String msgFormat, Object... args) {
        mLogger.error(t, rewriteMessages(msgFormat), args);
    }

    @Override
    public void warning(@NonNull String msgFormat, Object... args) {
        mLogger.warning(rewriteMessages(msgFormat), args);
    }

    @Override
    public void info(@NonNull String msgFormat, Object... args) {
        mLogger.info(msgFormat, args);
    }

    @Override
    public void verbose(@NonNull String msgFormat, Object... args) {
        mLogger.verbose(msgFormat, args);
    }

    private String rewriteMessages(@NonNull String originalMessage) {
        List<GradleMessage> messages = mParser.parseToolOutput(originalMessage);

        if (messages.isEmpty()) {
            return originalMessage;
        }

        StringBuilder errorStringBuilder = new StringBuilder();
        for (GradleMessage message: messages) {
            errorStringBuilder.append(STDOUT_ERROR_TAG)
                    .append(mGson.toJson(message));
        }
        return errorStringBuilder.toString();
    }
}
