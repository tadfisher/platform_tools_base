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

package com.android.build.gradle.internal.variant

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.BuildTypeData
import com.android.build.gradle.internal.ProductFlavorData
import com.android.build.gradle.internal.api.DefaultAndroidSourceSet
import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.dsl.BuildType
import com.android.build.gradle.internal.dsl.GroupableProductFlavor
import com.android.build.gradle.internal.dsl.ProductFlavor
import com.android.build.gradle.internal.dsl.SigningConfig
import com.android.build.gradle.TestExtension
import com.android.builder.core.AndroidBuilder
import com.android.builder.core.VariantType
import com.android.builder.model.SourceProvider
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.internal.reflect.Instantiator

import static com.android.builder.core.BuilderConstants.DEBUG

/**
 * Customization of ApplicationVariantFactory for test-only projects.
 */
public class TestVariantFactory extends ApplicationVariantFactory {

    public TestVariantFactory(
            @NonNull Instantiator instantiator,
            @NonNull AndroidBuilder androidBuilder,
            @NonNull BaseExtension extension) {
        super(instantiator, androidBuilder, extension)
    }

    @Override
    public boolean hasTestScope() {
        return false
    }

    @Override
    public void preVariantWork(Project project) {
        TestExtension testExtension = (TestExtension) extension

        String path = testExtension.targetProjectPath
        if (path == null) {
            throw new GradleException("targetProjectPath cannot be null in test project ${project.name}")
        }

        if (testExtension.targetVariant == null) {
            throw new GradleException("targetVariant cannot be null in test project ${project.name}")
        }

        String variant = "${testExtension.targetVariant}-classes"

        DependencyHandler handler = project.getDependencies()
        handler.add("provided", handler.project(path: path, configuration: variant))
    }

    @Override
    public void createDefaultComponents(
            @NonNull NamedDomainObjectContainer<BuildType> buildTypes,
            @NonNull NamedDomainObjectContainer<GroupableProductFlavor> productFlavors,
            @NonNull NamedDomainObjectContainer<SigningConfig> signingConfigs) {
        // don't call super as we don't want the default app version.
        // must create signing config first so that build type 'debug' can be initialized
        // with the debug signing config.
        signingConfigs.create(DEBUG);
        buildTypes.create(DEBUG);
    }

    @NonNull
    @Override
    TestModuleGradleVariantConfiguration createVariantConfig(
            @NonNull ProductFlavorData<ProductFlavor> defaultConfigData,
            @NonNull BuildTypeData buildTypeData,
            @Nullable SigningConfig signingOverride) {

        return new TestModuleGradleVariantConfiguration(
                defaultConfigData.getProductFlavor(),
                defaultConfigData.getSourceSet(),
                buildTypeData.getBuildType(),
                buildTypeData.getSourceSet(),
                getVariantConfigurationType(),
                signingOverride);
    }

    private class TestModuleGradleVariantConfiguration extends GradleVariantConfiguration {

        TestModuleGradleVariantConfiguration(
                @NonNull ProductFlavor defaultConfig,
                @NonNull SourceProvider defaultSourceProvider,
                @NonNull BuildType buildType,
                @Nullable SourceProvider buildTypeSourceProvider,
                @NonNull VariantType type,
                @Nullable SigningConfig signingConfigOverride) {
            super(defaultConfig, defaultSourceProvider, buildType, buildTypeSourceProvider, type,
                    signingConfigOverride)
        }
    }
}
