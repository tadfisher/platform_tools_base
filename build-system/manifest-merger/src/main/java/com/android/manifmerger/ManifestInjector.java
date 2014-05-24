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

import com.android.annotations.NonNull;
import com.android.utils.ILogger;
import com.google.common.collect.ImmutableMap;

import java.io.File;

/**
 * Capable of injecting system properties and do placeholder replacements in a manifest file.
 */
public class ManifestInjector extends ManifestTask {

    private ManifestInjector(
            @NonNull PlaceholderHandler.KeyBasedValueResolver<SystemProperty> systemPropertiesResolver,
            @NonNull PlaceholderHandler.KeyBasedValueResolver<String> placeHolderValueResolver,
            @NonNull File mainManifestFile, @NonNull ILogger logger) {
        super(systemPropertiesResolver, placeHolderValueResolver, mainManifestFile, logger);
    }

    /**
     * Creates a new invoker object to perform injection.
     * @param manifestFile the manifest file to inject
     * @param logger the logger to log errors and warnings.
     * @return a new {@link Invoker} instance.
     */
    public static Invoker newInvoker(File manifestFile, ILogger logger) {
        return new Invoker(manifestFile, logger);
    }

    /**
     * Invoker class providing convenient set of Builder like methods to configure a
     * ManifestInjector.
     */
    public static final class Invoker extends ManifestTask.Invoker<Invoker> {

        private Invoker(@NonNull File manifestFile, @NonNull ILogger logger) {
            super(manifestFile, logger);
        }

        public MergingReport inject() throws MergeFailureException {
            ImmutableMap<SystemProperty, String> systemProperties = mSystemProperties.build();
            if (systemProperties.containsKey(SystemProperty.PACKAGE)) {
                mPlaceHolders.put("packageName", systemProperties.get(SystemProperty.PACKAGE));
            }

            ManifestInjector manifestInjector =
                    new ManifestInjector(
                            new MapBasedKeyBasedValueResolver<SystemProperty>(systemProperties),
                            new MapBasedKeyBasedValueResolver<String>(mPlaceHolders.build()),
                            mMainManifestFile,
                            mLogger);

            return manifestInjector.inject();
        }
    }

    /**
     * Perform the actual injection by first loading the manifest file.
     */
    private MergingReport inject() throws MergeFailureException {

        // initiate a new merging report
        MergingReport.Builder mergingReportBuilder = new MergingReport.Builder(mLogger);
        SelectorResolver selectors = new SelectorResolver();

        XmlDocument manifest;
        try {
            manifest = XmlLoader.load(selectors,
                    mSystemPropertyResolver, null /* displayName */, mManifestFile);
        } catch (Exception e) {
            throw new ManifestMerger2.MergeFailureException(e);
        }

        mergingReportBuilder.getActionRecorder().recordDefaultNodeAction(
                manifest.getRootNode());

        // do placeholder substitution
        PlaceholderHandler placeholderHandler = new PlaceholderHandler();
        placeholderHandler.visit(
                manifest,
                mPlaceHolderValueResolver,
                mergingReportBuilder);
        if (mergingReportBuilder.hasErrors()) {
            return mergingReportBuilder.build();
        }

        performSystemPropertiesInjection(mergingReportBuilder, manifest);
        mergingReportBuilder.setMergedDocument(manifest);
        return mergingReportBuilder.build();
    }
}
