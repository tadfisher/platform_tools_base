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

package com.android.build.gradle.internal.tasks;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.scope.ConventionMappingHelper;
import com.android.build.gradle.internal.scope.TaskConfigAction;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.builder.signing.SignedJarBuilder;
import com.android.ide.common.packaging.PackagingUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.jar.JarFile;

import groovy.lang.Closure;

/**
 * Extract all packaged jar files java resources into a directory. Each jar file will be extracted
 * in a jar specific folder, and only java resources are extracted.
 */
public class ExtractJavaResourcesTask extends DefaultAndroidTask {

    // the fact we use a SET is not right, we should have an ordered list of jars...
    Set<File> jarInputFiles;

    @OutputDirectory
    public File outputDir;

    @InputFiles
    public Set<File> getJarInputFiles() {
        return jarInputFiles;
    }

    @TaskAction
    public void extractJavaResources(final IncrementalTaskInputs incrementalTaskInputs) {
        final JavaResourceFilter javaResourceFilter = new JavaResourceFilter();

        incrementalTaskInputs.outOfDate(new org.gradle.api.Action<InputFileDetails>() {
            @Override
            public void execute(InputFileDetails inputFileDetails) {
                File inputJar = inputFileDetails.getFile();
                String folderName = inputJar.getName() +
                        inputJar.getPath().hashCode();

                File outputFolder = new File(outputDir, folderName);
                if (!outputFolder.exists() && !outputFolder.mkdirs()) {
                        throw new RuntimeException(
                                "Cannot create folder to extract java resources in for "
                                        + inputJar.getAbsolutePath());
                }

                // create the jar file visitor that will check for out-dated resources.
                FileVisitor fileVisitor = new FileVisitor(this, javaResourceFilter, outputFolder);
                getProject().zipTree(inputJar).visit(fileVisitor);

                // delete the items that were removed since last pass.
                fileVisitor.removeDeletedItems();
            }
        });

        incrementalTaskInputs.removed(new org.gradle.api.Action<InputFileDetails>() {
            @Override
            public void execute(InputFileDetails inputFileDetails) {
                File deletedJar = inputFileDetails.getFile();
                String folderName = deletedJar.getName() +
                        deletedJar.getPath().hashCode();
                File outputFolder = new File(outputDir, folderName);
                if (outputFolder.exists()) {
                    deleteDir(outputFolder);
                }
            }
        });
    }


    private static boolean deleteDir(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDir(file);
                } else {
                    if (!file.delete()) {
                        throw new RuntimeException("Cannot delete file " + file.getPath());
                    }
                }
            }
        }
        return folder.delete();
    }

    /**
     * Visits each input jar entry and depending on the
     * {@link com.android.build.gradle.internal.tasks.ExtractJavaResourcesTask.JavaResourceFilter}
     * filtering will copy matching entries into a extraction folder.
     */
    private final class FileVisitor extends Closure<FileVisitDetails> {

        private final JavaResourceFilter javaResourceFilter;
        private final File outputDir;
        private final FileTree existingResources;
        private final ImmutableList.Builder<String> touchedItems = ImmutableList.builder();
        private final ImmutableSet.Builder<String> visitedItems = ImmutableSet.builder();

        FileVisitor(Object owner, JavaResourceFilter javaResourceFilter, File outputDir) {
            super(owner);
            this.javaResourceFilter = javaResourceFilter;
            this.outputDir = outputDir;
            this.existingResources = getProject().fileTree(outputDir);
        }

        @SuppressWarnings("unused")
        void doCall(FileVisitDetails fileVisitDetails) {
            File f = fileVisitDetails.getFile();
            if (f.isDirectory())
                return;
            File outputFile = fileVisitDetails.getRelativePath().getFile(outputDir);
            Action action = javaResourceFilter.apply(fileVisitDetails.getPath());
            if (action == Action.COPY) {
                visitedItems.add(fileVisitDetails.getPath());
                if (!outputFile.exists() || outputFile.lastModified()
                        < fileVisitDetails.getLastModified()) {
                    fileVisitDetails.copyTo(outputFile);
                    outputFile.setLastModified(fileVisitDetails.getLastModified());
                    touchedItems.add(fileVisitDetails.getPath());
                }
            }
        }

        ImmutableList<String> removeDeletedItems() {
            final ImmutableList.Builder<String> removedItems = ImmutableList.builder();
            final Set<String> visitedItems = this.visitedItems.build();
            existingResources.visit(new Closure<FileVisitDetails>(this) {
                @SuppressWarnings("unused")
                void doCall(FileVisitDetails fileDetails) {
                    if (!fileDetails.isDirectory() &&
                            !visitedItems.contains(fileDetails.getPath())) {

                        File outputFile = fileDetails.getRelativePath().getFile(outputDir);
                        if (outputFile.delete()) {
                            removedItems.add(fileDetails.getPath());
                        }
                    }
                }
            });
            return removedItems.build();
        }

        ImmutableList<String> getTouchedItems() {
            return touchedItems.build();
        }
    }
    /**
     * Define all possible actions for the {@link JavaResourceFilter}
     */
    enum Action {
        /**
         * Copy the file to the output destination.
         */
        COPY,
        /**
         * Ignore the file.
         */
        IGNORE}

    /**
     * Custom {@link SignedJarBuilder.IZipEntryFilter} to filter out everything that is not a standard java
     * resources, and also record whether the zip file contains native libraries.
     * <p/>Used in
     * {@link SignedJarBuilder#writeZip(java.io.InputStream, SignedJarBuilder.IZipEntryFilter, SignedJarBuilder.ZipEntryExtractor)}
     * when we only want the java resources from external jars.
     */
    private static final class JavaResourceFilter {

        /**
         * apply the filtering logic on an abstract archive entry denoted by its path and provide
         * an action to be implemented for the entry.
         * @param archivePath the archive entry path in the archive.
         * @return the action to implement.
         */
        @NonNull
        public Action apply(@NonNull String archivePath) {
            // Manifest files are never merged.
            if (JarFile.MANIFEST_NAME.equals(archivePath)) {
                return Action.IGNORE;
            }

            // split the path into segments.
            String[] segments = archivePath.split("/");

            // empty path? skip to next entry.
            if (segments.length == 0) {
                return Action.IGNORE;
            }

            // Check each folders to make sure they should be included.
            // Folders like CVS, .svn, etc.. should already have been excluded from the
            // jar file, but we need to exclude some other folder (like /META-INF) so
            // we check anyway.
            for (int i = 0 ; i < segments.length - 1; i++) {
                if (!PackagingUtils.checkFolderForPackaging(segments[i])) {
                    return Action.IGNORE;
                }
            }

            // get the file name from the path
            String fileName = segments[segments.length-1];

            return PackagingUtils.checkFileForPackaging(fileName)
                    ? Action.COPY
                    : Action.IGNORE;
        }
    }

    public static class Config implements TaskConfigAction<ExtractJavaResourcesTask> {

        private final VariantScope scope;

        public Config(VariantScope scope) {
            this.scope = scope;
        }

        @Override
        public String getName() {
            return scope.getTaskName("extract", "PackagedLibrariesJavaResources");
        }

        @Override
        public Class<ExtractJavaResourcesTask> getType() {
            return ExtractJavaResourcesTask.class;
        }

        @Override
        public void execute(ExtractJavaResourcesTask extractJavaResourcesTask) {
            ConventionMappingHelper.map(extractJavaResourcesTask, "jarInputFiles",
                    new Callable<Set<File>>() {

                    @Override
                    public Set<File> call() throws Exception {
                        return scope.getVariantConfiguration().getPackagedJars();
                    }
                });
            extractJavaResourcesTask.outputDir = scope.getPackagedJarsJavaResDestinationDir();
        }
    }
}
