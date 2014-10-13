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

package com.android.build.gradle.model

import com.android.build.gradle.internal.ProductFlavorGroup
import com.android.build.gradle.internal.dsl.BuildTypeFactory
import com.android.build.gradle.internal.dsl.GroupableProductFlavorDsl
import com.android.build.gradle.internal.dsl.GroupableProductFlavorFactory
import com.android.builder.core.BuilderConstants
import com.android.builder.core.DefaultBuildType
import com.android.builder.model.BuildType
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.model.Model
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.model.collection.CollectionBuilder
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.BinaryType
import org.gradle.platform.base.BinaryTypeBuilder
import org.gradle.platform.base.ComponentType
import org.gradle.platform.base.ComponentTypeBuilder

/**
 * Plugin to set up infrastructure for other android plugins.
 */
public class AndroidComponentModelPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        // Add project as an extension so that it can be used in model rules until Gradle provides
        // methods to replace project.file and project.container.
        project.extensions.add("projectModel", project)
    }

    @RuleSource
    static class Rules {

        @Model
        Project projectModel(ExtensionContainer extensions) {
            return extensions.getByType(Project)
        }

        @Model("android.buildTypes")
        NamedDomainObjectContainer<DefaultBuildType> createBuildTypes(
                ServiceRegistry serviceRegistry,
                Project project) {
            println "buildtype"
            Instantiator instantiator = serviceRegistry.get(Instantiator.class)
            def buildTypeContainer = project.container(DefaultBuildType,
                    new BuildTypeFactory(instantiator, project, project.getLogger()))

            // create default Objects, signingConfig first as its used by the BuildTypes.
            buildTypeContainer.create(BuilderConstants.DEBUG)
            buildTypeContainer.create(BuilderConstants.RELEASE)

            buildTypeContainer.whenObjectRemoved {
                throw new UnsupportedOperationException("Removing build types is not supported.")
            }
            return buildTypeContainer
        }

        @Model("android.productFlavors")
        NamedDomainObjectContainer<GroupableProductFlavorDsl> createProductFlavors(
                ServiceRegistry serviceRegistry,
                Project project) {
            println "productflavor"
            Instantiator instantiator = serviceRegistry.get(Instantiator.class)
            def productFlavorContainer = project.container(GroupableProductFlavorDsl,
                    new GroupableProductFlavorFactory(instantiator, project, project.getLogger()))

            productFlavorContainer.whenObjectRemoved {
                throw new UnsupportedOperationException(
                        "Removing product flavors is not supported.")
            }

            return productFlavorContainer
        }

        @ComponentType
        void defineComponentType(ComponentTypeBuilder<AndroidComponentSpec> builder) {
            builder.defaultImplementation(DefaultAndroidComponentSpec)
        }

        @Mutate
        void createAndroidComponents(
                CollectionBuilder<AndroidComponentSpec> androidComponents) {
            androidComponents.create("main")
        }

        @BinaryType
        void defineBinaryType(BinaryTypeBuilder<AndroidBinary> builder) {
            builder.defaultImplementation(DefaultAndroidBinary)
        }

        // TODO: Convert to @ComponentBinaries when it is implemented.
        @Mutate
        void createBinaries(
                BinaryContainer binaries,
                NamedDomainObjectContainer<DefaultBuildType> buildTypes,
                NamedDomainObjectContainer<GroupableProductFlavorDsl> productFlavors) {
            // TODO: Create custom product flavor container to manually configure flavor dimensions.
            List<String> flavorDimensionList = productFlavors*.flavorDimension.unique();

            List<ProductFlavorGroup> flavorGroups =
                    ProductFlavorGroup.createGroupList(flavorDimensionList, productFlavors)

            // Create a default ProductFlavorGroup with no flavor if productFlavors is empty.
            if (flavorGroups.isEmpty()) {
                flavorGroups.add(new ProductFlavorGroup());
            }

            for (def buildType : buildTypes) {
                for (def flavorGroup : flavorGroups) {
                    DefaultAndroidBinary binary = (DefaultAndroidBinary) binaries.create(
                            getBinaryName(buildType, flavorGroup), AndroidBinary)
                    binary.buildType = buildType
                    binary.productFlavors = flavorGroup.flavorList
                }
            }
        }
    }

    private static String getBinaryName(BuildType buildType, ProductFlavorGroup flavorGroup) {
        return buildType.name + flavorGroup.name.capitalize()
    }
}
