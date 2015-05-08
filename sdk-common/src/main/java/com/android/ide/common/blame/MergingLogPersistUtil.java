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
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class MergingLogPersistUtil {

    private static final SourceFileJsonTypeAdapter mSourceFileJsonTypeAdapter
            = new SourceFileJsonTypeAdapter();

    private static final SourcePositionJsonTypeAdapter mSourcePositionJsonTypeAdapter
            = new SourcePositionJsonTypeAdapter();

    private static final SourceFilePositionJsonSerializer mSourceFilePositionJsonTypeAdapter
            = new SourceFilePositionJsonSerializer();



    private static File getMultiFile(File folder, String shard) {
        return new File (new File(folder, "multi"), shard + ".json");
    }

    private static File getSingleFile(File folder, String shard) {
        return new File (new File(folder, "single"), shard + ".json");
    }

    static void saveToMultiFile(
            @NonNull File folder,
            @NonNull String shard,
            @NonNull Map<SourceFile, Map<SourcePosition, SourceFilePosition>> map)
            throws IOException {
        File file = getMultiFile(folder, shard);
        file.getParentFile().mkdir();
        JsonWriter out =
                new JsonWriter(Files.newWriter(file, Charsets.UTF_8));
        out.setIndent("    ");
        out.beginArray();
        for (Map.Entry<SourceFile, Map<SourcePosition, SourceFilePosition>> entry : map.entrySet()) {
            out.beginObject().name("outputFile");
            mSourceFileJsonTypeAdapter.write(out, entry.getKey());
            out.name("map");
            out.beginArray();
            for (Map.Entry<SourcePosition, SourceFilePosition> innerEntry: entry.getValue().entrySet()) {
                out.beginObject();
                out.name("to");
                mSourcePositionJsonTypeAdapter.write(out, innerEntry.getKey());
                out.name("from");
                mSourceFilePositionJsonTypeAdapter.write(out, innerEntry.getValue());
                out.endObject();
            }
            out.endArray();
            out.endObject();
        }
        out.endArray();
        out.close();
    }


    static Map<SourceFile, Map<SourcePosition, SourceFilePosition>> loadFromMultiFile(
            @NonNull File folder,
            @NonNull String shard) {
        Map<SourceFile, Map<SourcePosition, SourceFilePosition>> map = Maps.newConcurrentMap();
        JsonReader reader = null;
        File file = getMultiFile(folder, shard);
        if (!file.exists()) {
            return map;
        }
        try {
            reader = new JsonReader(Files.newReader(file, Charsets.UTF_8));
        } catch (FileNotFoundException e) {
            // Shouldn't happen unless it disappears under us.
            return map;
        }
        try {
            reader.beginArray();
            while (reader.peek() != JsonToken.END_ARRAY) {
                reader.beginObject();
                SourceFile toFile = SourceFile.UNKNOWN;
                Map<SourcePosition, SourceFilePosition> innerMap = Maps.newLinkedHashMap();
                while (reader.peek() != JsonToken.END_OBJECT) {
                    final String name = reader.nextName();
                    if (name.equals("outputFile")) {
                        toFile = mSourceFileJsonTypeAdapter.read(reader);
                    } else if (name.equals("map")) {
                        reader.beginArray();
                        while (reader.peek() != JsonToken.END_ARRAY) {
                            reader.beginObject();
                            SourceFilePosition from = null;
                            SourcePosition to = null;
                            while (reader.peek() != JsonToken.END_OBJECT) {
                                final String innerName = reader.nextName();
                                if (innerName.equals("from")) {
                                    from = mSourceFilePositionJsonTypeAdapter.read(reader);
                                } else if (innerName.equals("to")) {
                                    to = mSourcePositionJsonTypeAdapter.read(reader);
                                } else {
                                    throw new IOException(
                                            String.format("Unexpected property: %s", innerName));
                                }
                            }
                            if (from == null || to == null) {
                                throw new IOException("Each record must contain both from and to.");
                            }
                            innerMap.put(to, from);
                            reader.endObject();
                        }
                        reader.endArray();
                    } else {
                        throw new IOException(String.format("Unexpected property: %s", name));
                    }
                }
                map.put(toFile, innerMap);
                reader.endObject();
            }
            reader.endArray();
            reader.close();
            return map;
        } catch (IOException e) {
            try {
                reader.close();
            } catch (IOException e2) {
                // well, we tried.
            }
            // TODO: trigger a non-incremental merge if this happens.
            throw new RuntimeException(e);
        }
    }

    static void saveToSingleFile(
            @NonNull File folder,
            @NonNull String shard,
            @NonNull Map<SourceFile, SourceFile> map)
            throws IOException {
        File file = getSingleFile(folder, shard);
        file.getParentFile().mkdir();
        JsonWriter out = new JsonWriter(Files.newWriter(file, Charsets.UTF_8));
        out.setIndent("    ");
        out.beginArray();
        for (Map.Entry<SourceFile, SourceFile> entry : map.entrySet()) {
            out.beginObject();
            out.name("merged");
            mSourceFileJsonTypeAdapter.write(out, entry.getKey());
            out.name("source");
            mSourceFileJsonTypeAdapter.write(out, entry.getValue());
            out.endObject();
        }
        out.endArray();
        out.close();
    }

    static Map<SourceFile, SourceFile> loadFromSingleFile(
            @NonNull File folder,
            @NonNull String shard) {
        Map<SourceFile, SourceFile> fileMap = Maps.newConcurrentMap();
        JsonReader reader = null;
        File file = getSingleFile(folder, shard);
        if (!file.exists()) {
            return fileMap;
        }
        try {
            reader = new JsonReader(Files.newReader(file, Charsets.UTF_8));
        } catch (FileNotFoundException e) {
            // Shouldn't happen unless it disappears under us.
            return fileMap;
        }
        try {
            reader.beginArray();
            while (reader.peek() != JsonToken.END_ARRAY) {
                reader.beginObject();
                SourceFile merged = SourceFile.UNKNOWN;
                SourceFile source = SourceFile.UNKNOWN;
                while (reader.peek() != JsonToken.END_OBJECT) {
                    String name = reader.nextName();
                    if (name.equals("merged")) {
                        merged = mSourceFileJsonTypeAdapter.read(reader);
                    } else if (name.equals("source")) {
                        source = mSourceFileJsonTypeAdapter.read(reader);
                    } else {
                        throw new IOException(String.format("Unexpected property: %s", name));
                    }
                }
                reader.endObject();
                fileMap.put(merged, source);
            }
            reader.endArray();
            return fileMap;
        } catch (IOException e) {
            try {
                reader.close();
            } catch (IOException e2) {
                // well, we tried.
            }
            // TODO: trigger a non-incremental merge if this happens.
            throw new RuntimeException(e);
        }
    }
}
