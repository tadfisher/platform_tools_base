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

import com.android.utils.SourceFragmentPositionRange;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Created by cmw on 4/9/15.
 */
public class SourceFragmentPositionRangeSerializer
        implements JsonSerializer<SourceFragmentPositionRange>,
        JsonDeserializer<SourceFragmentPositionRange> {

    private static final String START_LINE = "startLine";
    private static final String START_COLUMN = "startColumn";
    private static final String START_OFFSET = "startOffset";
    private static final String END_LINE = "endLine";
    private static final String END_COLUMN = "endColumn";
    private static final String END_OFFSET = "endOffset";

    @Override
    public JsonElement serialize(SourceFragmentPositionRange position, Type type,
            JsonSerializationContext jsonSerializationContext) {
        JsonObject result = new JsonObject();
        if (position.getStartLine() != -1) {
            result.addProperty(START_LINE, position.getStartLine());
        }
        if (position.getStartColumn() != -1) {
            result.addProperty(START_COLUMN, position.getStartColumn());
        }
        if (position.getStartOffset() != -1) {
            result.addProperty(START_OFFSET, position.getStartOffset());
        }
        if (position.getEndLine() != -1 && position.getEndLine() != position.getStartLine()) {
            result.addProperty(END_LINE, position.getEndLine());
        }
        if (position.getEndColumn() != -1 && position.getEndColumn() != position
                .getStartColumn()) {
            result.addProperty(END_COLUMN, position.getEndColumn());
        }
        if (position.getEndOffset() != -1 && position.getEndOffset() != position
                .getStartOffset()) {
            result.addProperty(END_OFFSET, position.getEndOffset());
        }
        return result;
    }

    @Override
    public SourceFragmentPositionRange deserialize(JsonElement jsonElement, Type type,
            JsonDeserializationContext jsonDeserializationContext) throws
            JsonParseException {
        JsonObject object = jsonElement.getAsJsonObject();
        int startLine = object.has(START_LINE) ? object.get(START_LINE).getAsInt() : -1;
        int startColumn = object.has(START_COLUMN) ? object.get(START_COLUMN).getAsInt() : -1;
        int startOffset = object.has(START_OFFSET) ? object.get(START_OFFSET).getAsInt() : -1;
        int endLine = object.has(END_LINE) ? object.get(END_LINE).getAsInt() : startLine;
        int endColumn = object.has(END_COLUMN) ? object.get(END_COLUMN).getAsInt()
                : startColumn;
        int endOffset = object.has(END_OFFSET) ? object.get(END_OFFSET).getAsInt()
                : startOffset;
        return new SourceFragmentPositionRange(startLine, startColumn, startOffset, endLine,
                endColumn, endOffset);
    }
}

