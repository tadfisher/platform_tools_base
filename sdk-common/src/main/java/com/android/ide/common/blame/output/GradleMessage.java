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
import com.android.ide.common.blame.Position;
import com.google.common.base.Objects;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Message produced by tools invoked when building an Android project.
 */
public class GradleMessage {

    @NonNull
    private final Kind mKind;

    @NonNull
    private final String mText;

    @Nullable
    private final String mSourcePath;

    private final Position mPosition;

    private final String mOriginal;

    public GradleMessage(@NonNull Kind kind, @NonNull String text) {
        this(kind, text, null, new Position(), text);
    }


    public GradleMessage(@NonNull Kind kind,
                         @NonNull String text,
                         @Nullable String sourcePath,
                         @NonNull Position position,
                         @NonNull String original) {
        mKind = kind;
        mText = text;
        mSourcePath = sourcePath;
        mPosition = position;
        mOriginal = original;
    }

    public GradleMessage(@NonNull Kind kind, @NonNull String text, @Nullable String sourcePath, int line, int column) {
        this(kind, text, sourcePath, new Position(line, column, -1), text);
    }

    @NonNull
    public Kind getKind() {
        return mKind;
    }

    @NonNull
    public String getText() {
        return mText;
    }

    @Nullable
    public String getSourcePath() {
        return mSourcePath;
    }

    public int getLineNumber() {
        return mPosition.getStartLine();
    }

    public int getColumn() {
        return mPosition.getStartColumn();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GradleMessage that = (GradleMessage) o;

        return Objects.equal(mPosition, that.mPosition) &&
                mKind == that.mKind &&
                Objects.equal(mSourcePath, that.mSourcePath) &&
                Objects.equal(mText, that.mText);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mPosition, mKind, mSourcePath, mText);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" +
                "kind=" + mKind +
                ", text=\"" + mText + '\"' +
                ", sourcePath=" + mSourcePath +
                ", position=" + mPosition.toString() +
                ']';
    }

    public Object getPosition() {
        return mPosition;
    }

    public String getOriginal() {
        return mOriginal;
    }

    public enum Kind {
        ERROR, WARNING, INFO, STATISTICS, SIMPLE;

        @Nullable
        public static Kind findIgnoringCase(@NonNull String s) {
            for (Kind kind : values()) {
                if (kind.toString().equalsIgnoreCase(s)) {
                    return kind;
                }
            }
            return null;
        }
    }

    public static class Serializer implements JsonSerializer<GradleMessage> {

        @Override
        public JsonElement serialize(GradleMessage gradleMessage, Type type,
                JsonSerializationContext jsonSerializationContext) {
            JsonObject result = new JsonObject();
            result.addProperty("kind", gradleMessage.getKind().toString());
            result.addProperty("text", gradleMessage.getText());
            result.addProperty("sourcePath", gradleMessage.getSourcePath());
            result.add("position", jsonSerializationContext.serialize(gradleMessage.getPosition()));
            result.addProperty("original", gradleMessage.getOriginal());
            return result;
        }
    }
}
