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
package com.android.ide.common.blame.output;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.blame.Message;
import com.android.ide.common.blame.MessageJsonSerializer;
import com.android.ide.common.blame.MessageReceiver;
import com.android.ide.common.blame.SourceFilePosition;
import com.android.utils.ILogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MessageLogger implements MessageReceiver {

    public static final String STDOUT_ERROR_TAG = "AGPBI: ";

    public enum ErrorFormatMode {
        MACHINE_PARSABLE, HUMAN_READABLE
    }

    @NonNull
    private final ILogger mLogger;

    @NonNull
    private final ErrorFormatMode mErrorFormatMode;

    @Nullable
    private final Gson mGson;

    public MessageLogger(
            @NonNull ILogger logger, @NonNull ErrorFormatMode errorFormatMode) {
        mLogger = logger;
        mErrorFormatMode = errorFormatMode;
        if (errorFormatMode == ErrorFormatMode.MACHINE_PARSABLE) {
            mGson = createGson();
        } else {
            mGson = null;
        }
    }

    @Override
    public void receiveMessage(Message message) {
        String messageString = serializeMessage(message, mErrorFormatMode, mGson);
        outputMessage(message.getKind(), messageString);
    }

    private static String serializeMessage(
            @NonNull Message message,
            @NonNull ErrorFormatMode errorFormatMode,
            @Nullable Gson gson) {
        StringBuilder errorStringBuilder = new StringBuilder();
        if (errorFormatMode == ErrorFormatMode.HUMAN_READABLE) {
            for (SourceFilePosition pos : message.getSourceFilePositions()) {
                errorStringBuilder.append(pos.toString());
                errorStringBuilder.append(' ');
            }
            if (errorStringBuilder.length() > 0) {
                errorStringBuilder.append(": ");
            }
            errorStringBuilder.append(message.getText());
            if (!message.getRawMessage().equals(message.getText())) {
                errorStringBuilder.append(" (Original Message: ")
                        .append(message.getRawMessage()).append(')');
            }

            errorStringBuilder.append('\n');

        } else {
            // gson is not null when errorFormatMode == MACHINE_PARSABLE
            //noinspection ConstantConditions
            errorStringBuilder.append(STDOUT_ERROR_TAG)
                    .append(gson.toJson(message)).append("\n");
        }

        return errorStringBuilder.toString();
    }

    private void outputMessage(Message.Kind kind, String messageString) {
        switch (kind) {
            case ERROR:
                mLogger.error(null, messageString);
            case WARNING:
                mLogger.warning(messageString);
                break;
            case INFO:
                mLogger.info(messageString);
                break;
            case STATISTICS:
                mLogger.verbose(messageString);
                break;
            case UNKNOWN:
                mLogger.info(messageString);
                break;
            case SIMPLE:
                mLogger.info(messageString);
                break;
        }
    }

    private static Gson createGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        MessageJsonSerializer.registerTypeAdapters(gsonBuilder);
        return gsonBuilder.create();
    }
}
