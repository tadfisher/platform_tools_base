/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.build.gradle.api;

import com.android.annotations.NonNull;
import com.android.build.SplitData;
import com.android.build.SplitOutput;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * Represents a resource output from a variant configuration.
 *
 * Depending on split requirements, there can be more than one output from aapt tool and each
 * output file is represented by an instance of this class.
 */
public abstract class ApkOutput {

    @NonNull
    protected final SplitOutput.OutputType outputType;

    @NonNull
    protected final File outputFile;

    protected ApkOutput(@NonNull SplitOutput.OutputType outputType, @NonNull File outputFile) {
        this.outputFile = outputFile;
        this.outputType = outputType;
    }

    @NonNull
    public SplitOutput.OutputType getType() {
        return outputType;
    }

    @NonNull
    public File getOutputFile() {
        return outputFile;
    }

    public Collection<SplitData> getFilters() {
        return ImmutableList.of();
    }

    public static final class SplitApkOutput extends ApkOutput {
        @NonNull private final SplitOutput.FilterType filterType;
        @NonNull private final String splitIdentifier;
        @NonNull private final String splitSuffix;

        public SplitApkOutput(@NonNull SplitOutput.OutputType outputType,
                @NonNull SplitOutput.FilterType filterType,
                @NonNull String splitIdentifier,
                @NonNull String splitSuffix,
                @NonNull File outputFile) {
            super(outputType, outputFile);
            this.filterType = filterType;
            this.splitIdentifier = splitIdentifier;
            this.splitSuffix = splitSuffix;
        }

        /**
         * String identifying the split within its dimension. For instance, for a
         * {@link com.android.build.SplitOutput.FilterType#DENSITY}, a split identifier
         * can be "xxhdpi".
         *
         * @return the split identifier (bounded by its split type).
         */
        @NonNull
        public String getSplitIdentifier() {
            return splitIdentifier;
        }

        /**
         * AAPT can generate filenames that have a suffix passed the root file name + dimension
         * identifier, this suffix is stored here to be able to identify it in the
         * @return the generated file suffix.
         */
        @NonNull
        public String getSplitSuffix() {
            return splitSuffix;
        }

        @NonNull
        public Collection<SplitData> getFilters() {
            return  ImmutableList.<SplitData>builder()
                .add(new SplitData(SplitOutput.FilterType.DENSITY, getSplitIdentifier()))
                .build();
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("OutputType", outputType)
                    .add("FilterType", filterType)
                    .add("SplitIdentifier", splitIdentifier)
                    .add("SplitSuffix", splitSuffix)
                    .add("File", outputFile.getAbsolutePath())
                    .toString();
        }

        @NonNull
        public SplitOutput.FilterType getFilterType() {
            return filterType;
        }

        /**
         * JSON deserializer for loading previously saved instances...
         */
        public static class JsonDeserializer implements com.google.gson.JsonDeserializer<SplitApkOutput> {

            @Override
            public SplitApkOutput deserialize(JsonElement jsonElement, Type type,
                    JsonDeserializationContext jsonDeserializationContext)
                    throws JsonParseException {
                return new SplitApkOutput(
                        SplitOutput.OutputType.valueOf(
                                ((JsonObject) jsonElement).get("outputType").getAsString()),
                        SplitOutput.FilterType.valueOf(
                                ((JsonObject) jsonElement).get("filterType").getAsString()),
                        ((JsonObject) jsonElement).get("splitIdentifier").getAsString(),
                        ((JsonObject) jsonElement).get("splitSuffix").getAsString(),
                        new File(((JsonObject) jsonElement).getAsJsonObject(
                                "outputFile").get("path").getAsString()));
            }
        }
    }

    public static final class MainApkOutput extends ApkOutput {

        public MainApkOutput(File outputFile) {
            super(SplitOutput.OutputType.MAIN, outputFile);
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("OutputType", outputFile)
                    .add("File", outputFile.getAbsolutePath())
                    .toString();
        }
    }

    public static ImmutableList<SplitApkOutput> load(File inputFile)
            throws FileNotFoundException {

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(SplitApkOutput.class,
                new SplitApkOutput.JsonDeserializer());
        Gson gson = gsonBuilder.create();

        ImmutableList.Builder<SplitApkOutput> builder = ImmutableList.builder();

        for (SplitApkOutput vo : gson.fromJson(new FileReader(inputFile),
                SplitApkOutput[].class)) {
            builder.add(vo);
        }
        return builder.build();
    }
}
