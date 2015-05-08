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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ExecutionError;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

/**
 * Lazy loading of shards (which correspond to type directories for resource files , eg.
 * layout-land)
 *
 * For its use by MergeWriter, it uses ConcurrentMaps internally, so it is safe to perform any log
 * operation from any thread.
 */
public class MergingLog {

    @NonNull
    private final LoadingCache<String, Map<SourceFile, SourceFile>> mWholeFileMaps =
            CacheBuilder.newBuilder().build(new CacheLoader<String, Map<SourceFile, SourceFile>>() {
                @Override
                public Map<SourceFile, SourceFile> load(@NonNull String shard) {
                    return MergingLogPersistUtil.loadFromSingleFile(mOutputFolder, shard);
                }
            });

    @NonNull
    private final LoadingCache<String, Map<SourceFile, Map<SourcePosition, SourceFilePosition>>>
            mMergedFileMaps = CacheBuilder.newBuilder().build(
            new CacheLoader<String, Map<SourceFile, Map<SourcePosition, SourceFilePosition>>>() {
                @Override
                public Map<SourceFile, Map<SourcePosition, SourceFilePosition>> load(String shard)
                        throws Exception {
                    return MergingLogPersistUtil.loadFromMultiFile(mOutputFolder, shard);
                }
    });

    @NonNull
    private final File mOutputFolder;


    public MergingLog(@NonNull File outputFolder) {
        mOutputFolder = outputFolder;
    }

    public void logCopy(@NonNull SourceFile source, @NonNull SourceFile merged) {
        getWholeFileMap(merged).put(merged, source);
    }

    public void logCopy(@NonNull File source, @NonNull File merged) {
        logCopy(new SourceFile(source), new SourceFile(merged));
    }


    public void logRemove(@NonNull SourceFile merged) {
        getWholeFileMap(merged).remove(merged);
        getMergedFileMap(merged).remove(merged);
    }

    public void logSource(
            @NonNull SourceFile mergedFile,
            @NonNull Map<SourcePosition, SourceFilePosition> map) {
        getMergedFileMap(mergedFile).put(mergedFile, map);
    }


    @NonNull
    private Map<SourceFile, SourceFile> getWholeFileMap(SourceFile file) {
        String shard = getShard(file);
        Map<SourceFile, SourceFile> wholeFileMap = null;
        try {
            return mWholeFileMaps.get(shard);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private Map<SourceFile, Map<SourcePosition, SourceFilePosition>> getMergedFileMap(
            SourceFile file) {
        String shard = getShard(file);
        try {
            return mMergedFileMaps.get(shard);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    @NonNull
    public SourceFile find(@NonNull SourceFile mergedFile) {
        SourceFile sourceFile = getWholeFileMap(mergedFile).get(mergedFile);
        return sourceFile != null ? sourceFile : mergedFile;
    }

    @NonNull
    public SourceFilePosition find(@NonNull final SourceFilePosition mergedFilePosition) {
        SourceFile mergedSourceFile = mergedFilePosition.getFile();
        Map<SourcePosition, SourceFilePosition> positionMap =
                getMergedFileMap(mergedSourceFile).get(mergedSourceFile);
        if (positionMap == null) {
            SourceFile sourceFile = find(mergedSourceFile);
            return new SourceFilePosition(sourceFile, mergedFilePosition.getPosition());
        }
        final SourcePosition position = mergedFilePosition.getPosition();

        // TODO: this is not very efficient, which matters if we start processing debug messages.
        NavigableMap<SourcePosition, SourceFilePosition> sortedMap =
                new TreeMap<SourcePosition, SourceFilePosition>(positionMap);

        /*

        e.g. if we have
        <pre>
                  error1     error2
                   /--/       /--/
        <a> <b key="c"  value="d" /> </a>
        \----------------a---------------\
            \-----------b-----------\
                   \--\
                    c
       </pre>
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

    private static String getShard(@NonNull SourceFile sourceFile) {
        File file = sourceFile.getSourceFile();
        return file != null ? file.getParentFile().getName() : "unknown";
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
                    mMergedFileMaps.asMap().entrySet()) {
                MergingLogPersistUtil
                        .saveToMultiFile(mOutputFolder, entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Map<SourceFile, SourceFile>> entry :
                    mWholeFileMaps.asMap().entrySet()) {
                MergingLogPersistUtil
                        .saveToSingleFile(mOutputFolder, entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
