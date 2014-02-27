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
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * merges android manifest files, idempotent.
 */
public class ManifestMerger2 {

    private final static Logger logger = Logger.getLogger(ManifestMerger2.class.getName());

    private final File mainManifestFile;
    private final ImmutableList<File> libraryFiles;
    private final ImmutableList<File> flavorsAndBuildTypeFiles;
    private final ImmutableList<Builder.Features> mOptionalFeatures;

    private ManifestMerger2(File mainManifestFile,
            ImmutableList<File> libraryFiles,
            ImmutableList<File> flavorsAndBuildTypeFiles,
            ImmutableList<Builder.Features> optionalFeatures) {
        this.mainManifestFile = mainManifestFile;
        this.libraryFiles = libraryFiles;
        this.flavorsAndBuildTypeFiles = flavorsAndBuildTypeFiles;
        this.mOptionalFeatures = optionalFeatures;
    }

    private MergingReport merge(OutputStream outputStream) throws MergeFailureException {
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
            logger.info("Merging library manitest " + inputFile.getPath());
            xmlDocumentOptional = merge(xmlDocumentOptional, inputFile, mergingReportBuilder);
        }

        if (xmlDocumentOptional.isPresent()) {
            xmlDocumentOptional.get().write(outputStream);
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
        if (mOptionalFeatures.contains(Builder.Features.KEEP_INTERMEDIARY_STAGES)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            result.get().write(byteArrayOutputStream);
            mergingReportBuilder.addMergingStage(byteArrayOutputStream.toString());
        }

        return result;
    }

    public static Builder newBuilder(File mainManifestFile) {
        return new Builder(mainManifestFile);
    }

    // a wrapper exception to all sorts of failure exceptions that can be thrown during merging.
    public static class MergeFailureException extends Exception {

        private MergeFailureException(Exception cause) {
            super(cause);
        }
    }

    public static final class Builder {

        public enum Features {
            KEEP_INTERMEDIARY_STAGES,
            PRINT_SIMPLE_FILENAMES
        }

        private final File mMainManifestFile;
        private final ImmutableList.Builder<File> mLibraryFilesBuilder =
                new ImmutableList.Builder<File>();
        private final ImmutableList.Builder<File> mFlavorsAndBuildTypeFiles =
                new ImmutableList.Builder<File>();
        private final ImmutableList.Builder<Features> mFeaturesBuilder =
                new ImmutableList.Builder<Features>();

        Builder(File mainManifestFile) {
            this.mMainManifestFile = mainManifestFile;
        }

        public Builder addLibraryManifest(File file) {
            mLibraryFilesBuilder.add(file);
            return this;
        }

        public Builder addLibraryManifests(File... files) {
            mLibraryFilesBuilder.add(files);
            return this;
        }

        public Builder addFlavorAndBuildTypeManifest(File file) {
            this.mFlavorsAndBuildTypeFiles.add(file);
            return this;
        }

        public Builder addFlavorAndBuildTypeManifests(File... files) {
            this.mFlavorsAndBuildTypeFiles.add(files);
            return this;
        }

        public Builder withFeatures(Features...features) {
            mFeaturesBuilder.add(features);
            return this;
        }

        public MergingReport merge(OutputStream outputStream) throws MergeFailureException {
            ManifestMerger2 manifestMerger =
                    new ManifestMerger2(mMainManifestFile,
                            mLibraryFilesBuilder.build(),
                            mFlavorsAndBuildTypeFiles.build(),
                            mFeaturesBuilder.build());
            return manifestMerger.merge(outputStream);
        }
    }
}
