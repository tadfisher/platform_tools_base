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

package com.android.build.gradle.internal.transforms;

import static com.android.SdkConstants.DOT_CLASS;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.build.gradle.internal.LoggerWrapper;
import com.android.build.gradle.internal.pipeline.InputStream;
import com.android.build.gradle.internal.pipeline.InputStream.FileStatus;
import com.android.build.gradle.internal.pipeline.OutputStream;
import com.android.build.gradle.internal.pipeline.StreamScope;
import com.android.build.gradle.internal.pipeline.StreamType;
import com.android.build.gradle.internal.pipeline.Transform;
import com.android.build.gradle.internal.pipeline.TransformException;
import com.android.build.gradle.internal.pipeline.TransformPipeline;
import com.android.build.gradle.internal.pipeline.TransformType;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.core.DexOptions;
import com.android.ide.common.internal.WaitableExecutor;
import com.android.ide.common.process.LoggedProcessOutputHandler;
import com.android.ide.common.process.ProcessOutputHandler;
import com.android.utils.FileUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * PreDexing as a transform
 */
public class PreDexTransform implements Transform {

    @NonNull
    private final Set<StreamScope> preDexedScopes;
    @NonNull
    private final DexOptions dexOptions;
    private final boolean multiDex;
    @NonNull
    private final AndroidBuilder androidBuilder;
    @NonNull
    private final Logger logger;

    public PreDexTransform(
            @NonNull Set<StreamScope> preDexedScopes,
            @NonNull DexOptions dexOptions,
            boolean multiDex,
            @NonNull AndroidBuilder androidBuilder,
            @NonNull Logger logger) {
        this.preDexedScopes = preDexedScopes;
        this.dexOptions = dexOptions;
        this.multiDex = multiDex;
        this.androidBuilder = androidBuilder;
        this.logger = logger;
    }

    @NonNull
    @Override
    public String getName() {
        return "predex";
    }

    @NonNull
    @Override
    public Set<StreamType> getInputTypes() {
        return TransformPipeline.TYPE_CLASS;
    }

    @NonNull
    @Override
    public Set<StreamType> getOutputTypes() {
        return TransformPipeline.TYPE_DEX;
    }

    @NonNull
    @Override
    public Set<StreamScope> getScopes() {
        return preDexedScopes;
    }

    @NonNull
    @Override
    public Set<StreamScope> getReferencedScopes() {
        return TransformPipeline.EMPTY_SCOPES;
    }

    @NonNull
    @Override
    public TransformType getTransformType() {
        return TransformType.COMBINED;
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileInputs() {
        return ImmutableList.of();
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileOutputs() {
        return ImmutableList.of();
    }

    @NonNull
    @Override
    public Map<String, Object> getParameterInputs() {
        try {
            Map<String, Object> params = Maps.newHashMapWithExpectedSize(3);

            params.put("incremental", dexOptions.getIncremental());
            params.put("jumbo", dexOptions.getJumboMode());
            params.put("multidex", multiDex);

            return params;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public void transform(
            @NonNull List<InputStream> inputs,
            @NonNull List<OutputStream> outputs,
            boolean isIncremental) throws TransformException {

        try {
            // there should be a single output.
            assert outputs.size() == 1;
            OutputStream outputStream = Iterables.getOnlyElement(outputs);
            File outputFolder = outputStream.getFile();

            final Set<String> hashs = Sets.newHashSet();
            final WaitableExecutor<Void> executor = new WaitableExecutor<Void>();
            final List<File> inputFileDetails = Lists.newArrayList();

            if (!isIncremental) {
                // first delete the output folder
                FileUtils.emptyFolder(outputFolder);

                // we need to search for all the files.
                for (InputStream input : inputs) {
                    for (File file : input.getFiles()) {
                        if (file.isFile()) {
                            inputFileDetails.add(file);
                        } else if (file.isDirectory()) {
                            inputFileDetails.addAll(
                                    Files.fileTreeTraverser().postOrderTraversal(file).filter(
                                    new Predicate<File>() {
                                        @Override
                                        public boolean apply(File file) {
                                            return file.getPath().endsWith(DOT_CLASS);
                                        }
                                    }).toList());
                        }
                    }
                }
            } else {
                for (InputStream input : inputs) {
                    for (Entry<File, FileStatus> entry : input.getChangedFiles().entrySet()) {
                        File file = entry.getKey();
                        switch (entry.getValue()) {
                            case ADDED:
                            case CHANGED:
                                inputFileDetails.add(file);
                                break;
                            case REMOVED:
                                File preDexedFile = getDexFileName(outputFolder, file);

                                try {
                                    FileUtils.deleteFolder(preDexedFile);
                                } catch (IOException e) {
                                    logger.info("Could not delete {}\n{}",
                                            preDexedFile, Throwables.getStackTraceAsString(e));
                                }
                                break;
                        }
                    }
                }
            }

            ProcessOutputHandler outputHandler = new LoggedProcessOutputHandler(new LoggerWrapper(logger));
            for (final File file : inputFileDetails) {
                Callable<Void> action = new PreDexTask(outputFolder, file, hashs, outputHandler);
                executor.execute(action);
            }

            executor.waitForTasksWithQuickFail(false);
        } catch (Exception e) {
            throw new TransformException(e);
        }
    }

    private final class PreDexTask implements Callable<Void> {
        private final File outFolder;
        private final File fileToProcess;
        private final Set<String> hashs;
        private final ProcessOutputHandler mOutputHandler;

        private PreDexTask(
                File outFolder,
                File file,
                Set<String> hashs,
                ProcessOutputHandler outputHandler) {
            this.outFolder = outFolder;
            this.fileToProcess = file;
            this.hashs = hashs;
            this.mOutputHandler = outputHandler;
        }

        @Override
        public Void call() throws Exception {
            // TODO remove once we can properly add a library as a dependency of its test.
            String hash = getFileHash(fileToProcess);

            synchronized (hashs) {
                if (hashs.contains(hash)) {
                    return null;
                }

                hashs.add(hash);
            }

            File preDexedFile = getDexFileName(outFolder, fileToProcess);

            if (multiDex) {
                preDexedFile.mkdirs();
            }

            androidBuilder.preDexLibrary(
                    fileToProcess, preDexedFile, multiDex, dexOptions, mOutputHandler);

            return null;
        }
    }

    /**
     * Returns the hash of a file.
     * @param file the file to hash
     */
    private static String getFileHash(@NonNull File file) throws IOException {
        HashCode hashCode = Files.hash(file, Hashing.sha1());
        return hashCode.toString();
    }

    /**
     * Returns a unique File for the pre-dexed library, even
     * if there are 2 libraries with the same file names (but different
     * paths)
     *
     * If multidex is enabled the return File is actually a folder.
     *
     * @param outFolder the output folder.
     * @param inputFile the library.
     */
    @NonNull
    static File getDexFileName(@NonNull File outFolder, @NonNull File inputFile) {
        // get the filename
        String name = inputFile.getName();
        // remove the extension
        int pos = name.lastIndexOf('.');
        if (pos != -1) {
            name = name.substring(0, pos);
        }

        // add a hash of the original file path.
        String input = inputFile.getAbsolutePath();
        HashFunction hashFunction = Hashing.sha1();
        HashCode hashCode = hashFunction.hashString(input, Charsets.UTF_16LE);

        return new File(outFolder, name + "-" + hashCode.toString() + SdkConstants.DOT_JAR);
    }
}
