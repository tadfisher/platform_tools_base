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

import com.google.common.base.Objects;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class Position {

    private final int mStartLine;

    private final int mStartColumn;

    private final int mStartOffset;

    private final int mEndLine;

    private final int mEndColumn;

    private final int mEndOffset;

    public Position(int startLine, int startColumn, int startOffset, int endLine, int endColumn,
            int endOffset) {
        mStartLine = startLine;
        mStartColumn = startColumn;
        mStartOffset = startOffset;
        mEndLine = endLine;
        mEndColumn = endColumn;
        mEndOffset = endOffset;
    }

    public Position(int lineNumber, int column, int offset) {
        mStartLine = mEndLine = lineNumber;
        mStartColumn = mEndColumn = column;
        mStartOffset = mEndOffset = offset;
    }

    public Position() {
        mStartLine = mStartColumn = mStartOffset = mEndLine = mEndColumn = mEndOffset = -1;
    }

    /**
     * Outputs positions as human-readable formatted strings.
     *
     * e.g.
     * <pre>84
     * 84-86
     * 84:5
     * 84:5-28
     * 85:5-86:47</pre>
     * @return a human readable position.
     */
    @Override
    public String toString() {
        StringBuilder sB = new StringBuilder();
        sB.append(mStartLine);
        if (mStartColumn!=-1) {
            sB.append(':');
            sB.append(mStartColumn); }
        if (mEndLine!=-1) {

            if (mEndLine==mStartLine) {
                if (mEndColumn!=-1 && mEndColumn!=mStartColumn) {
                    sB.append('-');
                    sB.append(mEndColumn);
                }
            } else {
                sB.append('-');
                sB.append(mEndLine);
                if (mEndColumn != -1) {
                    sB.append(':');
                    sB.append(mEndColumn);
                }
            }
        }
        return sB.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Position) {
            Position other = (Position) obj;

            return other.mStartLine == mStartLine &&
                    other.mStartColumn == mStartColumn &&
                    other.mStartOffset == mStartOffset &&
                    other.mEndLine == mEndLine &&
                    other.mEndColumn == mEndColumn &&
                    other.mEndOffset == mStartOffset;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects
                .hashCode(mStartLine, mStartColumn, mStartOffset, mEndLine, mEndColumn, mEndOffset);
    }

    public int getStartLine() {
        return mStartLine;
    }

    public int getStartColumn() {
        return mStartColumn;
    }

    public int getStartOffset() {
        return mStartOffset;
    }


    public int getEndLine() {
        return mEndLine;
    }

    public int getEndColumn() {
        return mEndColumn;
    }

    public int getEndOffset() {
        return mEndOffset;
    }

    public static class Serializer implements JsonSerializer<Position> {

        @Override
        public JsonElement serialize(Position position, Type type,
                JsonSerializationContext jsonSerializationContext) {
            JsonObject result = new JsonObject();
            if (position.getStartLine() != -1) {
                result.addProperty("startLine", position.getStartLine());
            }
            if (position.getStartColumn() != -1) {
                result.addProperty("startColumn", position.getStartColumn());
            }
            if (position.getStartOffset() != -1) {
                result.addProperty("startOffset", position.getStartOffset());
            }
            if (position.getEndLine() != -1 && position.getEndLine() != position.getStartLine()) {
                result.addProperty("endLine", position.getEndLine());
            }
            if (position.getEndColumn() != -1 && position.getEndColumn() != position.getStartColumn()) {
                result.addProperty("endColumn", position.getEndColumn());
            }
            if (position.getEndOffset() != -1 && position.getEndOffset() != position.getStartOffset()) {
                result.addProperty("endOffset", position.getEndOffset());
            }
            return result;
        }
    }

    public static class Deserializer implements JsonDeserializer<Position> {

        @Override
        public Position deserialize(JsonElement jsonElement, Type type,
                JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject object = jsonElement.getAsJsonObject();
            int startLine = object.has("startLine") ? object.get("startLine").getAsInt() : -1;
            int startColumn = object.has("startColumn") ? object.get("startColumn").getAsInt() : -1;
            int startOffset = object.has("startOffset") ? object.get("startOffset").getAsInt() : -1;
            int endLine = object.has("endLine") ? object.get("endLine").getAsInt() : startLine;
            int endColumn = object.has("endColumn") ? object.get("endColumn").getAsInt() : startColumn;
            int endOffset = object.has("endOffset") ? object.get("endOffset").getAsInt() : startOffset;
            return new Position(startLine, startColumn, startOffset, endLine, endColumn, endOffset);
        }
    }
}
