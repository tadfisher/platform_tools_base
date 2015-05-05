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

import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.Formatter;
import java.util.List;

/**
 * Exception for errors during merging.
 */
public class MergingException extends Exception {

    public static final String MULTIPLE_ERRORS = "Multiple errors:";

    private final List<Message> mMessages;

    /**
     * For internal use. Creates a new MergingException
     *
     * @param cause    the original exception. May be null.
     * @param message  the first message.
     * @param messages the rest of the messages, may be empty.
     */
    protected MergingException(@Nullable Throwable cause, @NonNull Message message,
            @NonNull Message... messages) {
        super(messages.length == 0 ? message.getText() : MULTIPLE_ERRORS, cause);
        mMessages = ImmutableList.<Message>builder().add(message).add(messages).build();
    }

    /**
     * Creates a new MergingException
     *
     * @param cause    the cause
     * @param message  the first message.
     */
    public static MergingException wrapException(@NonNull Throwable cause, @NonNull Message message) {
        return new MergingException(cause, message);
    }

    /**
     * Creates a new MergingException with the specified cause and source file position.
     *
     * @param throwable the cause. The message text will be specified by {@link
     *                  Throwable#getLocalizedMessage()}
     * @param position  the source file position that caused the error.
     */
    public static MergingException wrapException(@NonNull Throwable throwable,
            @NonNull SourceFilePosition position) {
        String message = throwable.getLocalizedMessage();
        return new MergingException(throwable, new Message(Kind.ERROR, message, position));
    }

    /**
     * Creates a new MergingException with the specified cause and source file.
     *
     * @param throwable the cause. The message text will be specified by {@Link
     *                  Throwable.getLocalizedMessage()}
     * @param file      the source file that caused the error.
     */
    public static MergingException wrapException(@NonNull Throwable throwable, @NonNull File file) {
        return wrapException(throwable, new SourceFilePosition(file, SourcePosition.UNKNOWN));
    }

    /**
     * Creates a new MergingException with the specified cause and source file, extracting the
     * position from the specified {@link SAXParseException}.
     *
     * @param exception the cause. The sourcePosition will be extracted from the SaxParseException.
     *                  The message text will be specified by {@Link Throwable.getLocalizedMessage()}
     * @param file      the source file being parsed.
     */
    public static MergingException wrapSaxParseException(@NonNull SAXParseException exception,
            @NonNull File file) {
        String message = exception.getLocalizedMessage();
        final SourceFilePosition position;
        int lineNumber = exception.getLineNumber();
        if (lineNumber != -1) {
            // Convert positions to be 0-based for SourceFilePosition.
            position = new SourceFilePosition(file, new SourcePosition(
                    lineNumber - 1, exception.getColumnNumber() - 1, -1));
        } else {
            position = new SourceFilePosition(file, SourcePosition.UNKNOWN);
        }
        return wrapException(exception, position);
    }

    /**
     * Creates a new MergingException with the specified file and message.
     *
     * @param file      the source file. May be null if unknown.
     * @param msgFormat s an optional error format. It will be processed using a {@link Formatter}
     *                  with the provided arguments.
     * @param args      provides the arguments for errorFormat.
     */
    public static MergingException withFile(@Nullable File file, @NonNull String msgFormat,
            @NonNull Object... args) {
        String message = String.format(msgFormat, args);
        SourceFile sourceFile = file == null ? SourceFile.UNKNOWN : new SourceFile(file);
        SourceFilePosition position = new SourceFilePosition(sourceFile, SourcePosition.UNKNOWN);
        return new MergingException(null, new Message(Kind.ERROR, message, position));
    }

    /**
     * Computes the error message to display for this error
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        for (Message message : mMessages) {
            List<SourceFilePosition> sourceFilePositions = message.getSourceFilePositions();
            if (sourceFilePositions.size() > 1 || !sourceFilePositions.get(0)
                    .equals(SourceFilePosition.UNKNOWN)) {
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
