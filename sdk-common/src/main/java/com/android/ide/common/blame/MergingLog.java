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
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Lazy loading of shards (which correspond to type directories for resource files , eg.
 * layout-land)
 */
public class MergingLog {

    private final Map<String, Map<SourceFile, SourceFile>> mWholeFileMaps = Maps.newHashMap();

    private final Map<String, Map<SourceFile, Map<SourcePosition, SourceFilePosition>>>
            mMergedFileMaps =
            Maps.newHashMap();

    private final File mOutputFolder;


    public MergingLog(@NonNull File outputFolder) {
        mOutputFolder = outputFolder;

    }

    public void logCopy(String shard, SourceFile source, SourceFile merged) {
        getWholeFileMap(shard).put(merged, source);
    }

    public void logCopy(String shard, File source, File merged) {
        logCopy(shard,  new SourceFile(source), new SourceFile(merged));
    }


    public void logRemove(String shard, SourceFile merged) {
        getWholeFileMap(shard).remove(merged);
        getMergedFileMap(shard).remove(merged);
    }

    /**
     *
     * @param shard to avoid loading one large file into memory for an incremental merge.
     *              Must be computable from only the mergedFile.
     * @param mergedFile
     * @param map
     */
    public void logSource(
            @NonNull String shard,
            @NonNull SourceFile mergedFile,
            @NonNull Map<SourcePosition, SourceFilePosition> map) {
        getMergedFileMap(shard).put(mergedFile, map);
    }


    @NonNull
    private Map<SourceFile, SourceFile> getWholeFileMap(String shard) {
        Map<SourceFile, SourceFile> wholeFileMap = mWholeFileMaps.get(shard);
        if (wholeFileMap == null) {
            // Attempt to load from file
            wholeFileMap = MergingLogs.loadFromSingleFile(mOutputFolder, shard);
            mWholeFileMaps.put(shard, wholeFileMap);
        }
        return wholeFileMap;
    }

    @NonNull
    private Map<SourceFile, Map<SourcePosition, SourceFilePosition>> getMergedFileMap(String shard) {
        Map<SourceFile, Map<SourcePosition, SourceFilePosition>> mergedFileMap =
                mMergedFileMaps.get(shard);
        if (mergedFileMap == null) {
            mergedFileMap = MergingLogs.loadFromMultiFile(mOutputFolder, shard);
            mMergedFileMaps.put(shard, mergedFileMap);
        }
        return mergedFileMap;
    }


    public SourceFile find(String shard, SourceFile mergedFile) {
        SourceFile sourceFile = getWholeFileMap(shard).get(mergedFile);
        return sourceFile != null ? sourceFile : mergedFile;
    }

    public SourceFilePosition find(String shard, final SourceFilePosition mergedFilePosition) {
        Map<SourcePosition, SourceFilePosition> positionMap =
                getMergedFileMap(shard).get(mergedFilePosition.getFile());
        if (positionMap == null) {
            SourceFile sourceFile = getWholeFileMap(shard).get(mergedFilePosition.getFile());
            if (sourceFile == null) {
                return mergedFilePosition;
            }
            return new SourceFilePosition(sourceFile, SourcePosition.UNKNOWN);
        }
        final SourcePosition position = mergedFilePosition.getPosition();

        // TODO: this is not very efficient, which matters if we start processing debug messages.
        NavigableMap<SourcePosition, SourceFilePosition> sortedMap =
                new TreeMap<SourcePosition, SourceFilePosition>(positionMap);

        /*

        e.g. if we have

                  error1     error2
                   /--/       /--/
        <a> <b key="c"  value="d" /> </a>
        \-----------a----------\
            \------b------\
                   \--\
                    c

       we want to find c for error 1 and b for error 2.
         */

        // get the element just before this one.
        @Nullable
        Map.Entry<SourcePosition, SourceFilePosition> candidate = sortedMap.floorEntry(position);

        // Don't traverse the whole file.
        // This is the product of the depth and breadth of nesting that can be handled.
        int patience = 20;
        // check if it encompasses the error position.
        SourcePosition key;
        while (candidate != null && position.compareEnd(key = candidate.getKey()) > 0 ) {
            patience--;
            if (patience == 0) {
                candidate = null;
                break;
            }
            candidate = sortedMap.lowerEntry(key);
        }

        if (candidate == null) {
            // we failed to fina a link, return where we were.
            return mergedFilePosition;
        }

        return candidate.getValue();

    }

    public void write() {
        if (!mOutputFolder.isDirectory() && !mOutputFolder.mkdirs()) {
            throw new RuntimeException(
                    String.format("Could not create merging log directory at %s", mOutputFolder));
        }

        // This is intrinsically incremental, any shards that were touched were loaded, and so
        // will be saved. Empty map will result in the deletion of the file.

        try {

            for (Map.Entry<String, Map<SourceFile, Map<SourcePosition, SourceFilePosition>>> entry :
                    mMergedFileMaps.entrySet()) {
                MergingLogs.saveToMultiFile(mOutputFolder, entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Map<SourceFile, SourceFile>> entry :
                    mWholeFileMaps.entrySet()) {
                MergingLogs.saveToSingleFile(mOutputFolder, entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
