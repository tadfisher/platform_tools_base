/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.build.gradle

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.internal.dsl.BuildType
import com.android.build.gradle.internal.dsl.GroupableProductFlavor
import com.android.build.gradle.internal.dsl.SigningConfig
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.internal.DefaultDomainObjectSet
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.logging.Logging
import org.gradle.internal.reflect.Instantiator

/**
 * 'android' extension for 'com.android.library' project.
 * This extends {@link BaseExtension}
 */
@CompileStatic
public class LibraryExtension extends BaseExtension {

    private final DefaultDomainObjectSet<LibraryVariant> libraryVariantList =
        new DefaultDomainObjectSet<LibraryVariant>(LibraryVariant.class)

    private Project project

    LibraryExtension(LibraryPlugin plugin, ProjectInternal project, Instantiator instantiator,
            NamedDomainObjectContainer<BuildType> buildTypes,
            NamedDomainObjectContainer<GroupableProductFlavor> productFlavors,
            NamedDomainObjectContainer<SigningConfig> signingConfigs,
            boolean isLibrary) {
        super(plugin, project, instantiator, buildTypes, productFlavors, signingConfigs, isLibrary)
        this.project = project
    }

    /**
     * Returns the list of library variants. Since the collections is built after evaluation,
     * it should be used with Groovy's <code>all</code> iterator to process future items.
     */
    public DefaultDomainObjectSet<LibraryVariant> getLibraryVariants() {
        return libraryVariantList
    }

    @Override
    void addVariant(BaseVariant variant) {
        libraryVariantList.add((LibraryVariant) variant)
    }

    // ---------------
    // TEMP for compatibility
    // STOPSHIP Remove in 1.0

    private boolean packageBuildConfig = true

    public void packageBuildConfig(boolean value) {
        if (!value) {
            BasePlugin.displayDeprecationWarning(
                    Logging.getLogger(""),
                    project,
                    "Support for not packaging BuildConfig is deprecated and will be removed in 1.0")
        }

        packageBuildConfig = value
    }

    public void setPackageBuildConfig(boolean value) {
        packageBuildConfig(value)
    }

    boolean getPackageBuildConfig() {
        return packageBuildConfig
    }
}
