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

package com.android.builder.internal.compiler;

import com.android.annotations.NonNull;
import com.android.annotations.concurrency.Immutable;
import com.android.builder.AndroidBuilder;
import com.android.builder.DexOptions;
import com.android.ide.common.internal.CommandLineRunner;
import com.android.ide.common.internal.LoggedErrorException;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.repository.FullRevision;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 */
public class PreDexCache {

    @Immutable
    public static class Item {
        @NonNull
        private final File mSourceFile;
        @NonNull
        private final File mOutputFile;
        @NonNull
        private final HashCode mSourceHash;

        Item(@NonNull File sourceFile, @NonNull File outputFile,
                @NonNull HashCode sourceHash) {
            mSourceFile = sourceFile;
            mOutputFile = outputFile;
            mSourceHash = sourceHash;
        }

        @NonNull
        public File getSourceFile() {
            return mSourceFile;
        }

        @NonNull
        public File getOutputFile() {
            return mOutputFile;
        }

        @NonNull
        public HashCode getSourceHash() {
            return mSourceHash;
        }
    }

    @Immutable
    private static class Key {
        @NonNull
        private final File mSourceFile;
        @NonNull
        private final FullRevision mBuildToolsRevision;

        private static Key of(@NonNull File sourceFile, @NonNull FullRevision buildToolsRevision) {
            return new Key(sourceFile, buildToolsRevision);
        }

        private Key(@NonNull File sourceFile, @NonNull FullRevision buildToolsRevision) {
            mSourceFile = sourceFile;
            mBuildToolsRevision = buildToolsRevision;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Key key = (Key) o;

            if (!mBuildToolsRevision.equals(key.mBuildToolsRevision)) {
                return false;
            }
            if (!mSourceFile.equals(key.mSourceFile)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = mSourceFile.hashCode();
            result = 31 * result + mBuildToolsRevision.hashCode();
            return result;
        }
    }

    private static final PreDexCache sSingleton = new PreDexCache();

    public static PreDexCache getCache() {
        return sSingleton;
    }

    private final Map<Key, Item> mMap = Maps.newHashMap();
    private int mMisses = 0;
    private int mHits = 0;

    @NonNull
    public Item preDexLibrary(
            @NonNull File inputFile,
            @NonNull File outFile,
            @NonNull DexOptions dexOptions,
            @NonNull BuildToolInfo buildToolInfo,
            boolean verbose,
            @NonNull CommandLineRunner commandLineRunner)
            throws IOException, LoggedErrorException, InterruptedException {
        Key itemKey = Key.of(inputFile, buildToolInfo.getRevision());
        // get the item
        Item item = mMap.get(itemKey);

        if (item == null) {
            mMisses++;

            // haven't process this file yet so do it and record it.
            AndroidBuilder.preDexLibrary(inputFile, outFile, dexOptions, buildToolInfo, verbose, commandLineRunner);

            // put it in map.
            item = new Item(inputFile, outFile, Files.hash(inputFile, Hashing.sha1()));
            mMap.put(itemKey, item);
        } else {
            mHits++;

            // file already pre-dex, just copy the output.
            Files.copy(item.getOutputFile(), outFile);
        }

        return item;
    }

    public void clear() {
        if (!mMap.isEmpty()) {
            System.out.println("PREDEX CACHE HITS:   " + mHits);
            System.out.println("PREDEX CACHE MISSES: " + mMisses);
        }
        mMap.clear();
    }
}
