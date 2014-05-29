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
import com.android.utils.Pair;
import com.android.utils.StdLogger;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * merges android manifest files, idempotent.
 */
@Immutable
public class ManifestMerger2 extends ManifestTask {

    private final ImmutableList<Pair<String, File>> mLibraryFiles;
    private final ImmutableList<File> mFlavorsAndBuildTypeFiles;
    private final ImmutableList<Invoker.Feature> mOptionalFeatures;

    private ManifestMerger2(
            @NonNull ILogger logger,
            @NonNull File mainManifestFile,
            @NonNull ImmutableList<Pair<String, File>> libraryFiles,
            @NonNull ImmutableList<File> flavorsAndBuildTypeFiles,
            @NonNull ImmutableList<Invoker.Feature> optionalFeatures,
            @NonNull KeyBasedValueResolver<String> placeHolderValueResolver,
            @NonNull KeyBasedValueResolver<SystemProperty> systemPropertiesResolver) {
        super(systemPropertiesResolver, placeHolderValueResolver, mainManifestFile, logger);
        this.mLibraryFiles = libraryFiles;
        this.mFlavorsAndBuildTypeFiles = flavorsAndBuildTypeFiles;
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
        MergingReport.Builder mergingReportBuilder = new MergingReport.Builder(mLogger);

        SelectorResolver selectors = new SelectorResolver();
        // invariant : xmlDocumentOptional holds the higher priority document and we try to
        // merge in lower priority documents.
        Optional<XmlDocument> xmlDocumentOptional = Optional.absent();
        for (File inputFile : mFlavorsAndBuildTypeFiles) {
            mLogger.info("Merging flavors and build manifest %s \n", inputFile.getPath());
            xmlDocumentOptional = merge(xmlDocumentOptional,
                    Pair.of((String) null, inputFile),
                    selectors,
                    mergingReportBuilder);
            if (!xmlDocumentOptional.isPresent()) {
                return mergingReportBuilder.build();
            }
        }
        // load all the libraries xml files up front to have a list of all possible node:selector
        // values.
        List<XmlDocument> loadedLibraryDocuments = loadLibraries(selectors);

        mLogger.info("Merging main manifest %s\n", mManifestFile.getPath());
        xmlDocumentOptional = merge(xmlDocumentOptional,
                Pair.of(mManifestFile.getName(), mManifestFile),
                selectors,
                mergingReportBuilder);
        if (!xmlDocumentOptional.isPresent()) {
            return mergingReportBuilder.build();
        }
        for (XmlDocument libraryDocument : loadedLibraryDocuments) {
            mLogger.info("Merging library manifest " + libraryDocument.getSourceLocation());
            xmlDocumentOptional = merge(
                    xmlDocumentOptional, libraryDocument, mergingReportBuilder);
            if (!xmlDocumentOptional.isPresent()) {
                return mergingReportBuilder.build();
            }
        }

        // done with proper merging phase, now we need to trim unwanted elements, placeholder
        // substitution and system properties injection.
        ElementsTrimmer.trim(xmlDocumentOptional.get(), mergingReportBuilder);
        if (mergingReportBuilder.hasErrors()) {
            return mergingReportBuilder.build();
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

        // perform system property injection.
        performSystemPropertiesInjection(mergingReportBuilder, xmlDocumentOptional.get());

        XmlDocument finalMergedDocument = xmlDocumentOptional.get();
        PostValidator.validate(finalMergedDocument, mergingReportBuilder);
        if (mergingReportBuilder.hasErrors()) {
            finalMergedDocument.getRootNode().addMessage(mergingReportBuilder,
                    MergingReport.Record.Severity.WARNING,
                    "Post merge validation failed");
        }
        XmlDocument cleanedDocument =
                ToolsInstructionsCleaner.cleanToolsReferences(
                        finalMergedDocument, mLogger);

        if (cleanedDocument != null) {
            mergingReportBuilder.setMergedDocument(cleanedDocument);
        }

        MergingReport build = mergingReportBuilder.build();
        StdLogger stdLogger = new StdLogger(StdLogger.Level.INFO);
        build.log(stdLogger);
        stdLogger.verbose(build.getMergedDocument().get().prettyPrint());

        return build;
    }

    // merge the optionally existing xmlDocument with a lower priority xml file.
    private Optional<XmlDocument> merge(
            Optional<XmlDocument> xmlDocument,
            Pair<String, File> lowerPriorityXmlFile,
            KeyResolver<String> selectors,
            MergingReport.Builder mergingReportBuilder) throws MergeFailureException {

        XmlDocument lowerPriorityDocument;
        try {
            lowerPriorityDocument = XmlLoader.load(selectors,
                    mSystemPropertyResolver,
                    lowerPriorityXmlFile.getFirst(),
                    lowerPriorityXmlFile.getSecond());
        } catch (Exception e) {
            throw new MergeFailureException(e);
        }
        return merge(xmlDocument, lowerPriorityDocument, mergingReportBuilder);

    }

    // merge the optionally existing xmlDocument with a lower priority xml file.
    private Optional<XmlDocument> merge(
            Optional<XmlDocument> xmlDocument,
            XmlDocument lowerPriorityDocument,
            MergingReport.Builder mergingReportBuilder) throws MergeFailureException {

        MergingReport.Result validationResult = PreValidator
                .validate(mergingReportBuilder, lowerPriorityDocument);
        if (validationResult == MergingReport.Result.ERROR) {
            mergingReportBuilder.addMessage(lowerPriorityDocument.getSourceLocation(), 0, 0,
                    MergingReport.Record.Severity.ERROR,
                    "Validation failed, exiting");
            return Optional.absent();
        }
        Optional<XmlDocument> result;
        if (xmlDocument.isPresent()) {
            result = xmlDocument.get().merge(lowerPriorityDocument, mergingReportBuilder);
        } else {
            mergingReportBuilder.getActionRecorder().recordDefaultNodeAction(
                    lowerPriorityDocument.getRootNode());
            result = Optional.of(lowerPriorityDocument);
        }

        // if requested, dump each intermediary merging stage into the report.
        if (mOptionalFeatures.contains(Invoker.Feature.KEEP_INTERMEDIARY_STAGES)
                && result.isPresent()) {
            mergingReportBuilder.addMergingStage(result.get().prettyPrint());
        }

        return result;
    }

    private List<XmlDocument> loadLibraries(SelectorResolver selectors)
            throws MergeFailureException {

        ImmutableList.Builder<XmlDocument> loadedLibraryDocuments = ImmutableList.builder();
        for (Pair<String, File> libraryFile : mLibraryFiles) {
            mLogger.info("Loading library manifest " + libraryFile.getSecond().getPath());
            XmlDocument libraryDocument;
            try {
                libraryDocument = XmlLoader.load(selectors,
                        mSystemPropertyResolver,
                        libraryFile.getFirst(), libraryFile.getSecond());
            } catch (Exception e) {
                throw new MergeFailureException(e);
            }
            // extract the package name...
            String libraryPackage = libraryDocument.getRootNode().getXml().getAttribute("package");
            // save it in the selector instance.
            if (!Strings.isNullOrEmpty(libraryPackage)) {
                selectors.addSelector(libraryPackage, libraryFile.getFirst());
            }

            loadedLibraryDocuments.add(libraryDocument);
        }
        return loadedLibraryDocuments.build();
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
    public static final class Invoker extends ManifestTask.Invoker<Invoker> {

        /**
         * Optional behavior of the merging tool can be turned on by setting these Feature.
         */
        public enum Feature {

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

        private final ImmutableList.Builder<Pair<String, File>> mLibraryFilesBuilder =
                new ImmutableList.Builder<Pair<String, File>>();
        private final ImmutableList.Builder<File> mFlavorsAndBuildTypeFiles =
                new ImmutableList.Builder<File>();
        private final ImmutableList.Builder<Feature> mFeaturesBuilder =
                new ImmutableList.Builder<Feature>();


        /**
         * Creates a new builder with the mandatory main manifest file.
         * @param mainManifestFile application main manifest file.
         * @param logger the logger interface to use.
         */
        private Invoker(@NonNull File mainManifestFile, @NonNull ILogger logger) {
            super(mainManifestFile, logger);
        }

        /**
         * Add one library file manifest, will be added last in the list of library files which will
         * make the parameter the lowest priority library manifest file.
         * @param file the library manifest file to add.
         * @return itself.
         */
        public Invoker addLibraryManifest(File file) {
            mLibraryFilesBuilder.add(Pair.of(file.getName(), file));
            return this;
        }

        public Invoker addLibraryManifests(List<Pair<String, File>> namesAndFiles) {
            mLibraryFilesBuilder.addAll(namesAndFiles);
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
            for (File file : files) {
                addLibraryManifest(file);
            }
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
        public Invoker withFeatures(Feature...features) {
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

            // provide some free placeholders values.
            ImmutableMap<SystemProperty, String> systemProperties = mSystemProperties.build();
            if (systemProperties.containsKey(SystemProperty.PACKAGE)) {
                mPlaceHolders.put("packageName", systemProperties.get(SystemProperty.PACKAGE));
            }

            ManifestMerger2 manifestMerger =
                    new ManifestMerger2(
                            mLogger,
                            mMainManifestFile,
                            mLibraryFilesBuilder.build(),
                            mFlavorsAndBuildTypeFiles.build(),
                            mFeaturesBuilder.build(),
                            new MapBasedKeyBasedValueResolver<String>(mPlaceHolders.build()),
                            new MapBasedKeyBasedValueResolver<SystemProperty>(
                                    systemProperties));
            return manifestMerger.merge();
        }
    }

    /**
     * Helper class for map based placeholders key value pairs.
     */
    public static class MapBasedKeyBasedValueResolver<T> implements KeyBasedValueResolver<T> {

        private final ImmutableMap<T, String> keyValues;

        public MapBasedKeyBasedValueResolver(Map<T, String> keyValues) {
            this.keyValues = ImmutableMap.copyOf(keyValues);
        }

        @Nullable
        @Override
        public String getValue(@NonNull T key) {
            return keyValues.get(key);
        }
    }
}
