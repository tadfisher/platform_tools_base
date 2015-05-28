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

package com.android.build.gradle.tasks;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.tasks.AbstractAndroidCompile;
import com.android.build.gradle.internal.tasks.FileSupplier;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.tasks.Job;
import com.android.builder.tasks.JobContext;
import com.android.builder.tasks.Task;
import com.android.ide.common.process.ProcessException;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.repository.FullRevision;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.ParallelizableTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Jack task.
 */
@ParallelizableTask
public class JackTask extends AbstractAndroidCompile
        implements FileSupplier, BinaryFileProviderTask {

    private static final FullRevision JACK_MIN_REV = new FullRevision(21, 1, 0);

    private AndroidBuilder androidBuilder;

    @InputFile
    public File getJackExe() {
        return new File(androidBuilder.getTargetInfo().getBuildTools()
                .getPath(BuildToolInfo.PathId.JACK));
    }

    @Override
    @TaskAction
    public void compile() {
        final Job<Void> job = new Job<Void>(getName(), new Task<Void>() {
            @Override
            public void run(@NonNull Job<Void> job, @NonNull JobContext<Void> context)
                    throws IOException {
                try {
                    JackTask.this.doMinification();
                } catch (ProcessException e) {
                    throw new IOException(e);
                }
            }

        });
        try {
            SimpleWorkQueue.push(job);

            // wait for the task completion.
            job.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

    }

    private void doMinification() throws ProcessException, IOException {

        if (System.getenv("USE_JACK_API") == null ||
                !androidBuilder.convertByteCodeUsingJackApis(
                        getDestinationDir(),
                        getJackFile(),
                        getClasspath().getFiles(),
                        getPackagedLibraries(),
                        getSource().getFiles(),
                        getProguardFiles(),
                        getMappingFile(),
                        getJarJarRuleFiles(),
                        getIncrementalDir(),
                        isMultiDexEnabled(),
                        getMinSdkVersion())) {

            // no incremental support through command line so far.
            androidBuilder.convertByteCodeWithJack(
                    getDestinationDir(),
                    getJackFile(),
                    computeBootClasspath(),
                    getPackagedLibraries(),
                    computeEcjOptionFile(),
                    getProguardFiles(),
                    getMappingFile(),
                    getJarJarRuleFiles(),
                    isMultiDexEnabled(),
                    getMinSdkVersion(),
                    isDebugLog,
                    getJavaMaxHeapSize());
        }

    }

    private File computeEcjOptionFile() throws IOException {
        File folder = getTempFolder();
        //noinspection ResultOfMethodCallIgnored
        folder.mkdirs();
        File file = new File(folder, "ecj-options.txt");

        StringBuilder sb = new StringBuilder();

        for (File sourceFile : getSource().getFiles()) {
            sb.append(sourceFile.getAbsolutePath()).append("\n");
        }

        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();

        Files.write(sb.toString(), file, Charsets.UTF_8);

        return file;
    }

    private String computeBootClasspath() {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        for (File file : getClasspath().getFiles()) {
            if (!first) {
                sb.append(":");
            } else {
                first = false;
            }

            sb.append(file.getAbsolutePath());
        }

        return sb.toString();
    }

    @Override
    @NonNull
    public BinaryFileProviderTask.Artifact getArtifact() {
        return new BinaryFileProviderTask.Artifact(
                BinaryFileProviderTask.BinaryArtifactType.JACK,
                getJackFile());
    }

    @NonNull
    @Override
    public org.gradle.api.Task getTask() {
        return this;
    }

    @Override
    public File get() {
        return getMappingFile();
    }

    public static FullRevision getJACK_MIN_REV() {
        return JACK_MIN_REV;
    }

    public AndroidBuilder getAndroidBuilder() {
        return androidBuilder;
    }

    public void setAndroidBuilder(AndroidBuilder androidBuilder) {
        this.androidBuilder = androidBuilder;
    }

    public boolean getIsVerbose() {
        return isVerbose;
    }

    public void setIsVerbose(boolean isVerbose) {
        this.isVerbose = isVerbose;
    }

    public boolean getIsDebugLog() {
        return isDebugLog;
    }

    public void setIsDebugLog(boolean isDebugLog) {
        this.isDebugLog = isDebugLog;
    }

    @InputFiles
    public Collection<File> getPackagedLibraries() {
        return packagedLibraries;
    }

    public void setPackagedLibraries(Collection<File> packagedLibraries) {
        this.packagedLibraries = packagedLibraries;
    }

    @InputFiles
    @Optional
    public Collection<File> getProguardFiles() {
        return proguardFiles;
    }

    public void setProguardFiles(Collection<File> proguardFiles) {
        this.proguardFiles = proguardFiles;
    }

    @InputFiles
    @Optional
    public Collection<File> getJarJarRuleFiles() {
        return jarJarRuleFiles;
    }

    public void setJarJarRuleFiles(Collection<File> jarJarRuleFiles) {
        this.jarJarRuleFiles = jarJarRuleFiles;
    }

    @Input
    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public File getTempFolder() {
        return tempFolder;
    }

    public void setTempFolder(File tempFolder) {
        this.tempFolder = tempFolder;
    }

    @OutputFile
    public File getJackFile() {
        return jackFile;
    }

    public void setJackFile(File jackFile) {
        this.jackFile = jackFile;
    }

    @OutputFile
    @Optional
    public File getMappingFile() {
        return mappingFile;
    }

    public void setMappingFile(File mappingFile) {
        this.mappingFile = mappingFile;
    }

    @Input
    public boolean isMultiDexEnabled() {
        return multiDexEnabled;
    }

    public void setMultiDexEnabled(boolean multiDexEnabled) {
        this.multiDexEnabled = multiDexEnabled;
    }

    @Input
    public int getMinSdkVersion() {
        return minSdkVersion;
    }

    public void setMinSdkVersion(int minSdkVersion) {
        this.minSdkVersion = minSdkVersion;
    }

    @Input
    @Optional
    public String getJavaMaxHeapSize() {
        return javaMaxHeapSize;
    }

    public void setJavaMaxHeapSize(String javaMaxHeapSize) {
        this.javaMaxHeapSize = javaMaxHeapSize;
    }

    @Input
    @Optional
    public File getIncrementalDir() {
        return incrementalDir;
    }

    public void setIncrementalDir(File incrementalDir) {
        this.incrementalDir = incrementalDir;
    }


    private boolean isVerbose;
    private boolean isDebugLog;

    private Collection<File> packagedLibraries;
    private Collection<File> proguardFiles;
    private Collection<File> jarJarRuleFiles;

    private boolean debug;

    private File tempFolder;
    private File jackFile;

    private File mappingFile;

    private boolean multiDexEnabled;

    private int minSdkVersion;

    private String javaMaxHeapSize;

    private File incrementalDir;


}
