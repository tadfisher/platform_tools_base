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
import com.android.annotations.concurrency.Immutable;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;

@Immutable
public class Message {

    @NonNull
    private final Kind mKind;

    @NonNull
    private final String mText;

    @NonNull
    private final List<SourceFilePosition> mSourceFilePositions;

    @NonNull
    private final String mRawMessage;

    public Message(@NonNull Kind kind, @NonNull String text,
            @NonNull SourceFilePosition... sourceFilePositions) {
        mKind = kind;
        mText = text;
        mRawMessage = text;
        mSourceFilePositions = ImmutableList.copyOf(sourceFilePositions);
    }

    public Message(@NonNull Kind kind, @NonNull String text,
            @NonNull List<SourceFilePosition> sourceFilePositions) {
        mKind = kind;
        mText = text;
        mRawMessage = text;
        mSourceFilePositions = ImmutableList.copyOf(sourceFilePositions);
    }

    public Message(@NonNull Kind kind, @NonNull String text,
            @NonNull SourceFilePosition sourceFilePosition, @NonNull String rawMessage)  {
        mKind = kind;
        mText = text;
        mRawMessage = rawMessage;
        mSourceFilePositions = ImmutableList.of(sourceFilePosition);
    }

    public Message(@NonNull Kind kind, @NonNull String text,
            @NonNull List<SourceFilePosition> sourceFilePositions, @NonNull String rawMessage)  {
        mKind = kind;
        mText = text;
        mRawMessage = rawMessage;
        mSourceFilePositions = sourceFilePositions;
    }

    public Message(Kind kind, String message, String sourcePath, int lineNumber, int columnNumber) {
        this(kind, message, new SourceFilePosition(new SourceFile(new File(sourcePath)), new SourcePosition(lineNumber, columnNumber, -1)));
    }

    @NonNull
    public Kind getKind() {
        return mKind;
    }

    @NonNull
    public String getText() {
        return mText;
    }

    @NonNull
    public List<SourceFilePosition> getSourceFilePositions() {
        return mSourceFilePositions;
    }

    @NonNull
    public String getRawMessage() {
        return mRawMessage;
    }

    @Nullable
    public String getSourcePath() {
        if (mSourceFilePositions.isEmpty()) {
            return null;
        }
        File file = mSourceFilePositions.get(0).getFile().getSourceFile();
        if (file == null) {
            return null;
        }
        return file.getAbsolutePath();
    }

    /**
     * Returns a legacy 1-based line number.
     */
    @Deprecated
    public int getLineNumber() {
        if (mSourceFilePositions.isEmpty()) {
            return -1;
        }
        return mSourceFilePositions.get(0).getPosition().getStartLine() + 1;
    }

    /**
     * Returns a legacy 1-based column number.
     * @return
     */
    @Deprecated
    public int getColumn() {
        if (mSourceFilePositions.isEmpty()) {
            return -1;
        }
        return mSourceFilePositions.get(0).getPosition().getStartColumn() + 1;
    }

    public enum Kind {
        ERROR, WARNING, INFO, STATISTICS, UNKNOWN, SIMPLE;

        public static Kind findIgnoringCase(String s, Kind defaultKind) {
            for (Kind kind : values()) {
                if (kind.toString().equalsIgnoreCase(s)) {
                    return kind;
                }
            }
            return defaultKind;
        }

        @Nullable
        public static Kind findIgnoringCase(String s) {
            return findIgnoringCase(s, null);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof  Message)) {
            return false;
        }
        Message that = (Message) o;
        return mKind == that.mKind &&
                mText.equals(that.mText) &&
                mSourceFilePositions.equals(that.mSourceFilePositions);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mKind, mText, mSourceFilePositions);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("kind", mKind).add("text", mText).add("sources", mSourceFilePositions).toString();
    }
}
