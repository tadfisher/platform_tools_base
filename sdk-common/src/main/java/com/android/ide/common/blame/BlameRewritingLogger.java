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

    private final ILogger logger;
    private final ToolOutputParser parser;

    public BlameRewritingLogger(@NonNull ILogger logger) {
        this.logger = logger;
        parser = new ToolOutputParser(new AaptOutputParser(), logger);
    }

    @Override
    public void error(@Nullable Throwable t, @Nullable String msgFormat, Object... args) {
        logger.error(t, rewriteMessage(msgFormat), args);
    }

    @Override
    public void warning(@NonNull String msgFormat, Object... args) {
        logger.warning(rewriteMessage(msgFormat), args);
    }

    @Override
    public void info(@NonNull String msgFormat, Object... args) {
        logger.info(msgFormat, args);
    }

    @Override
    public void verbose(@NonNull String msgFormat, Object... args) {
        logger.verbose(msgFormat, args);
    }

    public String rewriteMessage(@NonNull String originalMessage) {
        List<GradleMessage> messages = parser.parseToolOutput(originalMessage);

        if (messages.isEmpty()) {
            return originalMessage;
        }

        StringBuilder builder = new StringBuilder();
        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(GradleMessage.class, new GradleMessage.Serializer());
        gsonBuilder.registerTypeAdapter(Position.class, new Position.Serializer());
        Gson gson = gsonBuilder.create();
        return "ERROR: " + gson.toJson(messages) + gson.toJson(originalMessage);
    }
}
