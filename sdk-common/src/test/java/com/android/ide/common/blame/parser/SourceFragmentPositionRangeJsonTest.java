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
package com.android.ide.common.blame.parser;

import com.android.ide.common.blame.SourcePositionRange;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import junit.framework.TestCase;

public class SourceFragmentPositionRangeJsonTest extends TestCase {

    private Gson gsonSerializer;

    private Gson gsonDeserializer;

    private SourcePositionRange[] mExamples = new SourcePositionRange[]{
            new SourcePositionRange(-1, -1, -1),
            new SourcePositionRange(11, 22, 34),
            new SourcePositionRange(11, 22, -1),
            new SourcePositionRange(11, -1, 34),
            new SourcePositionRange(-1, 22, 34),
            new SourcePositionRange(11, 22, 33, 66, 77, 88),
            new SourcePositionRange(11, 22, -1, 66, 77, -1),
            new SourcePositionRange(11, -1, -1, 66, -1, -1),
            new SourcePositionRange(11, -1, -1, 11, -1, -1),
            new SourcePositionRange(11, 22, 33, 66, 77, 88),
            new SourcePositionRange(-1, -1, 33, -1, -1, 88),
            new SourcePositionRange(-1, -1, 33, -1, -1, 33),
            new SourcePositionRange(11, 22, 33, 11, 22, 33)};

    @Override
    public void setUp() {
        gsonSerializer = new GsonBuilder()
                .registerTypeAdapter(
                        SourcePositionRange.class,
                        new SourcePositionRange.Serializer())
                .create();
        gsonDeserializer = new GsonBuilder()
                .registerTypeAdapter(
                        SourcePositionRange.class,
                        new SourcePositionRange.Deserializer())
                .create();
    }


    public void testSerializeDeserializeRoundtrip() {
        for (SourcePositionRange range : mExamples) {
            testRoundTripExample(range);
        }
    }

    private void testRoundTripExample(SourcePositionRange m1) {
        String json = gsonSerializer.toJson(m1);
        SourcePositionRange m2 =
                gsonDeserializer.fromJson(json, SourcePositionRange.class);
        assertEquals(m1, m2);
    }


    public void testSimpleDeserialize() {
        String json2 = "{\"startLine\":245}";
        SourcePositionRange range2 =
                gsonDeserializer.fromJson(json2, SourcePositionRange.class);
        assertEquals(new SourcePositionRange(245, -1, -1), range2);
    }

    public void testDeserialize() {
        String json
                = "{\"startLine\":11,\"startColumn\":22,\"startOffset\":33,\"endLine\":66,\"endColumn\":77,\"endOffset\":88}";
        SourcePositionRange range =
                gsonDeserializer.fromJson(json, SourcePositionRange.class);
        assertEquals(range, new SourcePositionRange(11, 22, 33, 66, 77, 88));
    }
}
