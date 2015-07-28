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
package com.android.build.gradle.tasks
import com.android.annotations.NonNull
import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.dsl.DexOptions
import com.android.build.gradle.internal.pipeline.Stream
import com.android.build.gradle.internal.pipeline.StreamBasedTask
import com.android.build.gradle.internal.pipeline.StreamType
import com.android.build.gradle.internal.pipeline.TransformPipeline
import com.android.build.gradle.internal.scope.ConventionMappingHelper
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.ApkVariantData
import com.android.build.gradle.internal.variant.TestVariantData
import com.android.builder.core.AndroidBuilder
import com.android.ide.common.process.LoggedProcessOutputHandler
import com.android.utils.FileUtils
import com.google.common.base.Charsets
import com.google.common.collect.ImmutableList
import com.google.common.collect.Sets
import com.google.common.hash.HashCode
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import java.util.concurrent.Callable

import static com.android.SdkConstants.DOT_CLASS
import static com.android.SdkConstants.DOT_JAR
import static com.android.SdkConstants.EXT_CLASS
import static com.android.builder.core.VariantType.DEFAULT

public class IncrementalDex extends StreamBasedTask {

    // ----- PUBLIC TASK API -----

    @OutputDirectory
    File outputFolder

    @Input @Optional
    List<String> additionalParameters

    boolean enableIncremental = true

    // ----- PRIVATE TASK API -----
    @Input
    String getBuildToolsVersion() {
        getBuildTools().getRevision()
    }

    File tmpFolder;

    @Nested
    DexOptions dexOptions

    @Input
    boolean multiDexEnabled = false

    @Input
    boolean optimize = true

    /**
     * Actual entry point for the action.
     * Calls out to the doTaskAction as needed.
     */
    @TaskAction
    void taskAction(IncrementalTaskInputs inputs) {
        File outFolder = getOutputFolder()

        if (!inputs.isIncremental()) {
            project.logger.info("Unable to do incremental execution: full task run.")

            FileUtils.emptyFolder(outFolder)

            Set<File> filesToProcess = Sets.newHashSet()
            for (Stream s : inputStreams) {
                Collection<File> files = s.getFiles().call()
                for (File file : files) {
                    if (file.isDirectory()) {
                        filesToProcess.addAll(AndroidBuilder.getLeafFolders(EXT_CLASS,
                                Collections.singletonList(file)))
                    } else if (file.isFile()) {
                        filesToProcess.add(file)
                    }
                }
            }

            doTaskAction(outFolder, filesToProcess, Collections.emptySet())
            return
        }

        Set<File> filesToProcess = Sets.newHashSet()
        Set<File> filesToRemove = Sets.newHashSet()

        //noinspection GroovyAssignabilityCheck
        inputs.outOfDate { change ->
            File file = change.file
            String path = file.path
            if (path.endsWith(DOT_JAR)) {
                filesToProcess.add(file)
            } else if (path.endsWith(DOT_CLASS)) {
                filesToProcess.add(file.getParentFile())
            }
        }

        //noinspection GroovyAssignabilityCheck
        inputs.removed { change ->
            File file = change.file
            String path = file.path
            if (path.endsWith(DOT_JAR)) {
                filesToRemove.add(file)
            } else if (path.endsWith(DOT_CLASS)) {
                File parentFile = file.getParentFile()
                if (!filesToRemove.contains(parentFile) && !filesToProcess.contains(parentFile)) {
                    // if the parent doesn't exist or if it's empty, remove the dex corresponding to the parent.
                    if (!parentFile.isDirectory()) {
                        filesToRemove.add(parentFile)
                    } else {
                        File[] children = parentFile.listFiles()
                        if (children == null || children.length == 0) {
                            filesToRemove.add(parentFile)
                        } else {
                            boolean foundFile = false
                            for (File child : children) {
                                if (child.isFile() && child.path.endsWith(DOT_CLASS)) {
                                    foundFile = true
                                    break
                                }
                            }
                            if (foundFile) {
                                filesToProcess.add(parentFile)
                            } else {
                                filesToRemove.add(parentFile)
                            }
                        }
                    }
                }
            }
        }

        doTaskAction(outFolder, filesToProcess, filesToRemove)
    }

    private void doTaskAction(
            @NonNull File outFolder,
            @NonNull Set<File> filesToProcess,
            @NonNull Set<File> filesToRemove) {

        for (File remove : filesToRemove) {
            getDexFileName(outFolder, remove).delete()
        }

        for (File process : filesToProcess) {
            File target = getDexFileName(outFolder, process)

            println "PROCESSING: " + process

            List<File> inputs = null;
            if (process.isFile()) {
                inputs = Collections.singletonList(process)
            } else {
                File[] classFiles = process.listFiles(new FilenameFilter() {
                    @Override
                    boolean accept(File file, String name) {
                        return name.endsWith(DOT_CLASS)
                    }
                })

                tmpFolder.mkdirs()

                if (classFiles != null) {/*
                    File jarFile = new File(tmpFolder, target.getName())
                    JarOutputStream outputJar = new JarOutputStream(new FileOutputStream(jarFile))
                    byte[] buffer = new byte[4096];
                    for (File inputFile : classFiles) {
                        println "\tINPUT: " + inputFile
                        String jarPath = null;
                        for (Stream s : inputStreams) {
                            for (File file : s.files.call()) {
                                if (inputFile.getPath().startsWith(file.getPath())) {
                                    jarPath = inputFile.getPath().substring(file.getPath().length() + 1);
                                    break;
                                }
                            }
                        }
                        println "\tPATH: " + jarPath

                        // Get an input stream on the file.
                        FileInputStream fis = new FileInputStream(inputFile);
                        try {
                            // create the zip entry
                            JarEntry entry = new JarEntry(jarPath);
                            entry.setMethod(ZipEntry.STORED);
                            entry.setSize(inputFile.length());
                            entry.setCrc(0x00000000FFFFFFFF & Files.hash(inputFile, Hashing.crc32()).asInt());
                            entry.setTime(inputFile.lastModified());

                            // add the entry to the jar archive
                            outputJar.putNextEntry(entry);

                            // read the content of the entry from the input stream, and write it into the archive.
                            int count;
                            while ((count = fis.read(buffer)) != -1) {
                                outputJar.write(buffer, 0, count);
                            }

                            // close the entry for this file
                            outputJar.closeEntry();
                        } finally {

                        }
                    }
                    outputJar.close()
                    inputs = Collections.singletonList(jarFile);*/

                    inputs = Arrays.asList(classFiles)
                }
            }

            if (inputs != null) {
                getBuilder().convertByteCode(
                        inputs,
                        target,
                        getDexOptions(),
                        getAdditionalParameters(),
                        new LoggedProcessOutputHandler(getILogger()))
            }
        }
    }

    /**
     * Returns a unique File for the dex file, even
     * if there are 2 libraries with the same file names (but different
     * paths)
     *
     * @param outFolder the output folder.
     * @param inputFile the library
     * @return
     */
    @NonNull
    static File getDexFileName(@NonNull File outFolder, @NonNull File inputFile) {
        // get the filename
        String name = inputFile.getName()
        // remove the extension
        int pos = name.lastIndexOf('.')
        if (pos != -1) {
            name = name.substring(0, pos)
        }

        // add a hash of the original file path.
        String input = inputFile.getAbsolutePath();
        HashFunction hashFunction = Hashing.sha1()
        HashCode hashCode = hashFunction.hashString(input, Charsets.UTF_16LE)

        return new File(outFolder, name + "-" + hashCode.toString() + DOT_JAR)
    }

    public static class ConfigAction implements TaskConfigAction<IncrementalDex> {

        private final TransformPipeline transformPipeline

        public ConfigAction(@NonNull TransformPipeline transformPipeline) {
            this.transformPipeline = transformPipeline
        }

        @Override
        public String getName() {
            return transformPipeline.getVariantScope().getTaskName("dex")
        }

        @Override
        public Class<IncrementalDex> getType() {
            return IncrementalDex.class;
        }

        @Override
        public void execute(IncrementalDex dexTask) {
            VariantScope scope = transformPipeline.getVariantScope();
            ApkVariantData variantData = (ApkVariantData) scope.getVariantData();
            final GradleVariantConfiguration config = variantData.getVariantConfiguration();

            boolean isTestForApp = config.getType().isForTesting() && (DefaultGroovyMethods
                    .asType(variantData, TestVariantData.class)).getTestedVariantData()
                    .getVariantConfiguration().getType().equals(DEFAULT);

            boolean isMultiDexEnabled = config.isMultiDexEnabled() && !isTestForApp;

            variantData.dexTask = dexTask;

            dexTask.setAndroidBuilder(scope.getGlobalScope().getAndroidBuilder())
            dexTask.setVariantName(config.getFullName())

            ConventionMappingHelper.map(dexTask, "outputFolder", new Callable<File>() {
                @Override
                public File call() throws Exception {
                    return scope.getDexOutputFolder();
                }
            });

            dexTask.setDexOptions(scope.getGlobalScope().getExtension().getDexOptions());
            dexTask.setMultiDexEnabled(isMultiDexEnabled);
            // dx doesn't work with receving --no-optimize in debug so we disable it for now.
            dexTask.setOptimize(true);//!variantData.variantConfiguration.buildType.debuggable

            // inputs
            dexTask.inputStreams = ImmutableList.copyOf(transformPipeline.getStreamsByType(
                    StreamType.CODE))
            dexTask.outputStreams = ImmutableList.of()

            dexTask.tmpFolder = new File(scope.getGlobalScope().getIntermediatesDir(), "tmp");
        }
    }
}
