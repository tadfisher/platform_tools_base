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

import static com.android.manifmerger.PlaceholderHandler.KeyBasedValueResolver;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.concurrency.Immutable;
import com.android.utils.ILogger;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;

/**
 * merges android manifest files, idempotent.
 */
@Immutable
public class ManifestMerger2 {

    private final File mMainManifestFile;
    private final ImmutableList<File> mLibraryFiles;
    private final ImmutableList<File> mFlavorsAndBuildTypeFiles;
    private final ImmutableList<Invoker.Features> mOptionalFeatures;
    private final KeyBasedValueResolver mPlaceHolderValueResolver;
    private final ILogger mLogger;

    private ManifestMerger2(
            @NonNull ILogger logger,
            @NonNull File mainManifestFile,
            @NonNull ImmutableList<File> libraryFiles,
            @NonNull ImmutableList<File> flavorsAndBuildTypeFiles,
            @NonNull ImmutableList<Invoker.Features> optionalFeatures,
            @NonNull KeyBasedValueResolver placeHolderValueResolver) {
        this.mLogger = logger;
        this.mMainManifestFile = mainManifestFile;
        this.mLibraryFiles = libraryFiles;
        this.mFlavorsAndBuildTypeFiles = flavorsAndBuildTypeFiles;
        this.mOptionalFeatures = optionalFeatures;
        this.mPlaceHolderValueResolver = placeHolderValueResolver;
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
        MergingReport.Builder mergingReportBuilder = new MergingReport.Builder(mLogger);

        // invariant : xmlDocumentOptional holds the higher priority document and we try to
        // merge in lower priority documents.
        Optional<XmlDocument> xmlDocumentOptional = Optional.absent();
        for (File inputFile : mFlavorsAndBuildTypeFiles) {
            mLogger.info("Merging flavors and build manifest %s \n", inputFile.getPath());
            xmlDocumentOptional = merge(xmlDocumentOptional, inputFile, mergingReportBuilder);
            if (!xmlDocumentOptional.isPresent()) {
                return mergingReportBuilder.build();
            }
        }
        mLogger.info("Merging main manifest %s\n", mMainManifestFile.getPath());
        xmlDocumentOptional = merge(xmlDocumentOptional, mMainManifestFile, mergingReportBuilder);
        if (!xmlDocumentOptional.isPresent()) {
            return mergingReportBuilder.build();
        }
        for (File inputFile : mLibraryFiles) {
            mLogger.info("Merging library manifest " + inputFile.getPath());
            xmlDocumentOptional = merge(xmlDocumentOptional, inputFile, mergingReportBuilder);
            if (!xmlDocumentOptional.isPresent()) {
                return mergingReportBuilder.build();
            }
        }

        // do placeholder substitution
        PlaceholderHandler placeholderHandler = new PlaceholderHandler();
        placeholderHandler.visit(
                xmlDocumentOptional.get(),
                mPlaceHolderValueResolver,
                mergingReportBuilder);
        if (mergingReportBuilder.hasErrors()) {
            return mergingReportBuilder.build();
        }

        XmlDocument finalMergedDocument = xmlDocumentOptional.get();
        MergingReport.Result validate = PostValidator.validate(
                finalMergedDocument, mergingReportBuilder.getActionRecorder().build(), mLogger);
        if (validate != MergingReport.Result.SUCCESS) {
            mergingReportBuilder.addWarning("Post merge validation failed");
        }
        XmlDocument cleanedDocument =
                ToolsInstructionsCleaner.cleanToolsReferences(
                        finalMergedDocument, mLogger);

        if (cleanedDocument != null) {
            mergingReportBuilder.setMergedDocument(cleanedDocument);
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
        MergingReport.Result validationResult = PreValidator
                .validate(lowerPriorityDocument, mergingReportBuilder.getLogger());
        if (validationResult == MergingReport.Result.ERROR) {
            mergingReportBuilder.addError("Validation failed, exiting");
            return Optional.absent();
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

    /**
     * Creates a new {@link com.android.manifmerger.ManifestMerger2.Invoker} instance to invoke
     * the merging tool.
     *
     * @param mainManifestFile application main manifest file.
     * @param logger the logger interface to use.
     * @return an {@link com.android.manifmerger.ManifestMerger2.Invoker} instance that will allow
     * further customization and trigger the merging tool.
     */
    public static Invoker newInvoker(@NonNull File mainManifestFile, @NonNull ILogger logger) {
        return new Invoker(mainManifestFile, logger);
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
        private KeyBasedValueResolver mKeyBasedValueResolver;
        private final ILogger mLogger;


        /**
         * Creates a new builder with the mandatory main manifest file.
         * @param mainManifestFile application main manifest file.
         * @param logger the logger interface to use.
         */
        private Invoker(@NonNull File mainManifestFile, @NonNull ILogger logger) {
            this.mMainManifestFile = Preconditions.checkNotNull(mainManifestFile);
            this.mLogger = logger;
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
         * Sets the {@link KeyBasedValueResolver} to obtain place holder's values.
         * @param resolver the resolver object.
         * @return itself.
         */
        public Invoker setPlaceHolderResolver(KeyBasedValueResolver resolver) {
            mKeyBasedValueResolver = resolver;
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
            KeyBasedValueResolver keyBasedValueResolver = mKeyBasedValueResolver != null
                    ? mKeyBasedValueResolver
                    : new KeyBasedValueResolver() {
                        @Nullable
                        @Override
                        public String getValue(@NonNull String key) {
                            // this will generate an error if the document contains any
                            // placeholders.
                            return null;
                        }
                    };

            ManifestMerger2 manifestMerger =
                    new ManifestMerger2(
                            mLogger,
                            mMainManifestFile,
                            mLibraryFilesBuilder.build(),
                            mFlavorsAndBuildTypeFiles.build(),
                            mFeaturesBuilder.build(),
                            keyBasedValueResolver);
            return manifestMerger.merge();
        }
    }

    /**
     * Helper class for map based placeholders key value pairs.
     */
    public static class MapBasedKeyBasedValueResolver implements KeyBasedValueResolver {

        private final ImmutableMap<String, String> mKeyValues;

        public MapBasedKeyBasedValueResolver(Map<String, String> keyValues) {
            this.mKeyValues = ImmutableMap.copyOf(keyValues);
        }

        @Nullable
        @Override
        public String getValue(@NonNull String key) {
            return mKeyValues.get(key);
        }
    }
}
