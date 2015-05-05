/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.ide.common.res2;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.blame.Message;
import com.android.ide.common.blame.Message.Kind;
import com.android.ide.common.blame.SourceFile;
import com.android.ide.common.blame.SourceFilePosition;
import com.android.ide.common.blame.SourcePosition;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.List;

/**
 * Exception for errors during merging.
 *
 *
 */
public class MergingException extends Exception {

    public static final String MULTIPLE_ERRORS = "Multiple errors:";

    private final List<Message> mMessages;

    private MergingException(@Nullable Throwable cause, @NonNull ImmutableList<Message> messages) {
        super(messages.size() == 1 ? messages.get(0).getText() : MULTIPLE_ERRORS, cause);
        mMessages = messages;
    }

    protected MergingException(@Nullable Throwable cause, @NonNull Message... messages) {
        super(messages.length == 1 ? messages[0].getText() : MULTIPLE_ERRORS, cause);
        mMessages = ImmutableList.copyOf(messages);
    }

    public MergingException(@NonNull String message, @NonNull SourceFilePosition position) {
        super(message, null);
        mMessages = ImmutableList.of(new Message(Kind.ERROR, message, position));
    }

    public MergingException(@NonNull String message, @Nullable File file) {
        this(message, file != null ? new SourceFilePosition(file, SourcePosition.UNKNOWN)
                : SourceFilePosition.UNKNOWN);
    }

    private MergingException(@NonNull List<Message> messages) {
        super(messages.size() == 1 ? messages.get(0).getText() : "Multiple errors have occurred:");
        mMessages = ImmutableList.copyOf(messages);
    }

    public MergingException(@NonNull String message, @NonNull DataItem... items) {
        super(message, null);
        ImmutableList.Builder<SourceFilePosition> positions = ImmutableList.builder();
        for (DataItem item: items) {
            positions.add(fromDataItem(item));
        }
        mMessages = ImmutableList.of(new Message(Kind.ERROR, message, message, positions.build()));
    }

    protected static SourceFilePosition fromDataItem(DataItem item) {
        DataFile dataFile = item.getSource();
        if (dataFile == null) {
            return SourceFilePosition.UNKNOWN;
        }
        File f = dataFile.getFile();
        return new SourceFilePosition(new SourceFile(f, item.getKey()), SourcePosition.UNKNOWN);
        // TODO: find position in file.
    }


    public static MergingException wrapException(@NonNull Throwable throwable) {
        String message = throwable.getLocalizedMessage();
        return new MergingException(throwable,
                new Message(Kind.ERROR, message, SourceFilePosition.UNKNOWN));
    }

    public static MergingException wrapException(@NonNull Throwable throwable, @NonNull File file) {
        return  wrapException(throwable, new SourceFilePosition(file, SourcePosition.UNKNOWN));
    }

    public static MergingException wrapException(@NonNull Throwable throwable, @NonNull SourceFilePosition position) {
        String message = throwable.getLocalizedMessage();
        return new MergingException(throwable, new Message(Kind.ERROR, message, position));
    }

    public static MergingException wrapSaxParseException(@NonNull SAXParseException exception,
            @NonNull File file) {
        String message = exception.getLocalizedMessage();
        final SourceFilePosition position;
        int lineNumber = exception.getLineNumber();
        if (lineNumber != -1) {
            position = new SourceFilePosition(file, new SourcePosition(
                    lineNumber - 1, exception.getColumnNumber() - 1, -1));
        } else {
            position = new SourceFilePosition(file, SourcePosition.UNKNOWN);
        }
        return new MergingException(exception, new Message(Kind.ERROR, message, position));
    }


    /**
     * Add the file if it hasn't already been included in the list of file positions.
     */
    public MergingException setFile(@NonNull File file) {
        for (Message m : mMessages) {
            for (SourceFilePosition filePosition : m.getSourceFilePositions()) {
                if (file.equals(filePosition.getFile().getSourceFile())) {
                    return this;
                }
            }
        }
        // Otherwise insert or replace in each message
        ImmutableList.Builder<Message> newMessages = ImmutableList.builder();
        for (Message m : mMessages) {
            List<SourceFilePosition> positions = m.getSourceFilePositions();
            if (positions.size() == 1 && positions.get(0).getFile().equals(SourceFile.UNKNOWN)) {
                newMessages.add(new Message(m.getKind(), m.getText(), m.getRawMessage(),
                        new SourceFilePosition(file, positions.get(0).getPosition())));

                if (m.getSourceFilePositions().contains(SourceFilePosition.UNKNOWN)) {
                    for (SourceFilePosition filePosition : m.getSourceFilePositions()) {
                        if (file.equals(filePosition.getFile().getSourceFile())) {
                            return this;
                        }
                    }
                }
            }
        }
        Throwable cause = getCause();
        if (cause == null) {
            return new MergingException(getCause(), newMessages.build());
        } else {
            return new MergingException(newMessages.build());
        }
    }

    /**
     * Computes the error message to display for this error
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        for (Message message: mMessages) {
            List<SourceFilePosition> sourceFilePositions = message.getSourceFilePositions();
            if (sourceFilePositions.size() > 1 || !sourceFilePositions.get(0).equals(SourceFilePosition.UNKNOWN)) {
                sb.append(Joiner.on('\t').join(sourceFilePositions));
            }

            String text = message.getText();
            if (sb.length() > 0) {
                sb.append(':').append(' ');

                // ALWAYS insert the string "Error:" between the path and the message.
                // This is done to make the error messages more simple to detect
                // (since a generic path: message pattern can match a lot of output, basically
                // any labeled output, and we don't want to do file existence checks on any random
                // string to the left of a colon.)
                if (!text.startsWith("Error: ")) {
                    sb.append("Error: ");
                }
            } else if (!text.contains("Error: ")) {
                sb.append("Error: ");
            }

            // If the error message already starts with the path, strip it out.
            // This avoids redundant looking error messages you can end up with
            // like for example for permission denied errors where the error message
            // string itself contains the path as a prefix:
            //    /my/full/path: /my/full/path (Permission denied)
            if (sourceFilePositions.size() == 1) {
                File file = sourceFilePositions.get(0).getFile().getSourceFile();
                if (file != null) {
                    String path = file.getAbsolutePath();
                    if (text.startsWith(path)) {
                        int stripStart = path.length();
                        if (text.length() > stripStart && text.charAt(stripStart) == ':') {
                            stripStart++;
                        }
                        if (text.length() > stripStart && text.charAt(stripStart) == ' ') {
                            stripStart++;
                        }
                        text = text.substring(stripStart);
                    }
                }
            }

            sb.append(text);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
