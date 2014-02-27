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

package com.android.manifmerger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.logging.Logger;

/**
 * merges android manifest files, idempotent.
 */
public class ManifestMerger2 {

    private static final Logger logger = Logger.getLogger(ManifestMerger2.class.getName());

    private final File mainManifestFile;
    private final ImmutableList<File> libraryFiles;
    private final ImmutableList<File> flavorsAndBuildTypeFiles;
    private final ImmutableList<Invoker.Features> mOptionalFeatures;

    private ManifestMerger2(File mainManifestFile,
            ImmutableList<File> libraryFiles,
            ImmutableList<File> flavorsAndBuildTypeFiles,
            ImmutableList<Invoker.Features> optionalFeatures) {
        this.mainManifestFile = mainManifestFile;
        this.libraryFiles = libraryFiles;
        this.flavorsAndBuildTypeFiles = flavorsAndBuildTypeFiles;
        this.mOptionalFeatures = optionalFeatures;
    }

    /**
     * Perform high level ordering of files merging and delegates actual merging to
     * {@link XmlDocument#merge(XmlDocument, com.android.manifmerger.MergingReport.Builder)}
     *
     * @return the merging activity report.
     * @throws MergeFailureException if the merging cannot be completed (for instance, if xml
     * files cannot be loaded).
     */
    private MergingReport merge() throws MergeFailureException {
        // initiate a new merging report
        MergingReport.Builder mergingReportBuilder = new MergingReport.Builder();

        // invariant : xmlDocumentOptional holds the higher priority document and we try to
        // merge in lower priority documents.
        Optional<XmlDocument> xmlDocumentOptional = Optional.absent();
        for (File inputFile : flavorsAndBuildTypeFiles) {
            logger.info("Merging flavors and build manifest " + inputFile.getPath());
            xmlDocumentOptional = merge(xmlDocumentOptional, inputFile, mergingReportBuilder);
        }
        logger.info("Merging main manifest" + mainManifestFile.getPath());
        xmlDocumentOptional = merge(xmlDocumentOptional, mainManifestFile, mergingReportBuilder);
        for (File inputFile : libraryFiles) {
            logger.info("Merging library manifest " + inputFile.getPath());
            xmlDocumentOptional = merge(xmlDocumentOptional, inputFile, mergingReportBuilder);
        }

        if (xmlDocumentOptional.isPresent()) {
            mergingReportBuilder.setMergedDocument(xmlDocumentOptional.get());
        }
        return mergingReportBuilder.build();
    }

    // merge the optionally existing xmlDocument with a lower priority xml file.
    private Optional<XmlDocument> merge(
            Optional<XmlDocument> xmlDocument,
            File lowerPriorityXmlFile,
            MergingReport.Builder mergingReportBuilder) throws MergeFailureException {

        XmlDocument lowerPriorityDocument;
        try {
            lowerPriorityDocument = XmlLoader.load(lowerPriorityXmlFile);
        } catch (Exception e) {
            throw new MergeFailureException(e);
        }
        Optional<XmlDocument> result = xmlDocument.isPresent()
                ? xmlDocument.get().merge(lowerPriorityDocument, mergingReportBuilder)
                : Optional.of(lowerPriorityDocument);

        // if requested, dump each intermediary merging stage into the report.
        if (mOptionalFeatures.contains(Invoker.Features.KEEP_INTERMEDIARY_STAGES)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            result.get().write(byteArrayOutputStream);
            mergingReportBuilder.addMergingStage(byteArrayOutputStream.toString());
        }

        return result;
    }

    public static Invoker newBuilder(File mainManifestFile) {
        return new Invoker(mainManifestFile);
    }

    // a wrapper exception to all sorts of failure exceptions that can be thrown during merging.
    public static class MergeFailureException extends Exception {

        private MergeFailureException(Exception cause) {
            super(cause);
        }
    }

    /**
     * This class will hold all invocation parameters for the manifest merging tool.
     *
     * There are broadly three types of input to the merging tool :
     * <ul>
     *     <li>Build types and flavors overriding manifests</li>
     *     <li>Application main manifest</li>
     *     <li>Library manifest files</li></lib>
     * </ul>
     *
     * Only the main manifest file is a mandatory parameter.
     *
     * High level description of the merging will be as follow :
     * <ol>
     *     <li>Build type and flavors will be merged first in the order they were added. Highest
     *     priority file added first, lowest added last.</li>
     *     <li>Resulting document is merged with lower priority application main manifest file.</li>
     *     <li>Resulting document is merged with each library file manifest file in the order
     *     they were added. Highest priority added first, lowest added last.</li>
     *     <li>Resulting document is returned as results of the merging process.</li>
     * </ol>
     *
     * Check the merging tool <a href="https://goto.google.com/android_mm2">Spec</a> for more
     * details.
     */
    public static final class Invoker {

        /**
         * Optional behavior of the merging tool can be turned on by setting these Features.
         */
        public enum Features {

            /**
             * Keep all intermediary merged files during the merging process. This is particularly
             * useful for debugging/tracing purposes.
             */
            KEEP_INTERMEDIARY_STAGES,

            /**
             * When logging file names, use {@link java.io.File#getName()} rather than
             * {@link java.io.File#getPath()}
             */
            PRINT_SIMPLE_FILENAMES
        }

        private final File mMainManifestFile;
        private final ImmutableList.Builder<File> mLibraryFilesBuilder =
                new ImmutableList.Builder<File>();
        private final ImmutableList.Builder<File> mFlavorsAndBuildTypeFiles =
                new ImmutableList.Builder<File>();
        private final ImmutableList.Builder<Features> mFeaturesBuilder =
                new ImmutableList.Builder<Features>();


        /**
         * Creates a new builder with the mandatory main manifest file.
         * @param mainManifestFile application main manifest file.
         */
        public Invoker(File mainManifestFile) {
            this.mMainManifestFile = mainManifestFile;
        }

        /**
         * Add one library file manifest, will be added last in the list of library files which will
         * make the parameter the lowest priority library manifest file.
         * @param file the library manifest file to add.
         * @return itself.
         */
        public Invoker addLibraryManifest(File file) {
            mLibraryFilesBuilder.add(file);
            return this;
        }

        /**
         * Add several library file manifests at then end of the list which will make them the
         * lowest priority manifest files. The relative priority between all the files passed as
         * parameters will be respected.
         * @param files library manifest files to add last.
         * @return itself.
         */
        public Invoker addLibraryManifests(File... files) {
            mLibraryFilesBuilder.add(files);
            return this;
        }

        /**
         * Add a flavor or build type manifest file last in the list.
         * @param file build type or flavor manifest file
         * @return itself.
         */
        public Invoker addFlavorAndBuildTypeManifest(File file) {
            this.mFlavorsAndBuildTypeFiles.add(file);
            return this;
        }

        /**
         * Add several flavor or build type manifest files last in the list. Relative priorities
         * between the passed files as parameters will be respected.
         * @param files build type of flavor manifest files to add.
         * @return itself.
         */
        public Invoker addFlavorAndBuildTypeManifests(File... files) {
            this.mFlavorsAndBuildTypeFiles.add(files);
            return this;
        }

        /**
         * Sets some optional features for the merge tool.
         *
         * @param features one to many features to set.
         * @return itself.
         */
        public Invoker withFeatures(Features...features) {
            mFeaturesBuilder.add(features);
            return this;
        }

        /**
         * Perform the merging and return the result.
         *
         * @return an instance of {@link com.android.manifmerger.MergingReport} that will give
         * access to all the logging and merging records.
         *
         * This method can be invoked several time and will re-do the file merges.
         *
         * @throws MergeFailureException if the merging cannot be completed successfully.
         */
        public MergingReport merge() throws MergeFailureException {
            ManifestMerger2 manifestMerger =
                    new ManifestMerger2(mMainManifestFile,
                            mLibraryFilesBuilder.build(),
                            mFlavorsAndBuildTypeFiles.build(),
                            mFeaturesBuilder.build());
            return manifestMerger.merge();
        }
    }
}
