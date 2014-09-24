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

import com.android.build.gradle.internal.dsl.BuildTypeFactory
import com.android.build.gradle.internal.dsl.GroupableProductFlavorDsl
import com.android.build.gradle.internal.dsl.GroupableProductFlavorFactory
import com.android.builder.core.BuilderConstants
import com.android.builder.core.DefaultBuildType
import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import org.gradle.model.Model
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.platform.base.BinaryContainer
import org.gradle.platform.base.BinaryType
import org.gradle.platform.base.BinaryTypeBuilder

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
            Instantiator instantiator = serviceRegistry.get(Instantiator.class)
            def productFlavorContainer = project.container(GroupableProductFlavorDsl,
                    new GroupableProductFlavorFactory(instantiator, project, project.getLogger()))

            productFlavorContainer.whenObjectRemoved {
                throw new UnsupportedOperationException(
                        "Removing product flavors is not supported.")
            }

            return productFlavorContainer
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
            createAndroidBinaries(binaries, buildTypes.asList(), productFlavors.asList())
        }
    }

    private static void createBinary(
            BinaryContainer binaries,
            DefaultBuildType buildType,
            List<GroupableProductFlavorDsl> flavors) {
        binaries.create(getBinaryName(buildType, flavors), AndroidBinary) {
            DefaultAndroidBinary binary = (DefaultAndroidBinary) it
            binary.buildType = buildType
            binary.productFlavors = flavors
        }
    }

    private static String getBinaryName(BuildType buildType, List<ProductFlavor> flavors) {
        StringBuilder sb = new StringBuilder()
        List<String> dimensions = flavors*.name + buildType.name
        boolean first = true;
        for (String dim : dimensions) {
            sb.append(first ? dim : dim.capitalize())
            first = false
        }
        sb.append("Binary");
        return sb.toString()
    }

    private static void createAndroidBinaries(
            BinaryContainer binaries,
            List<DefaultBuildType> buildTypes,
            List<GroupableProductFlavorDsl> productFlavors) {
        // TODO: Create custom product flavor container to manually configure flavor dimensions.
        List<String> flavorDimensionList = productFlavors*.flavorDimension.unique();

        if (flavorDimensionList.size() < 2) {
            for (DefaultBuildType buildType : buildTypes) {
                if (productFlavors.isEmpty()) {
                    createBinary(binaries, buildType, [])
                } else {
                    for (GroupableProductFlavorDsl flavor : productFlavors) {
                        createBinary(binaries, buildType, [flavor])
                    }
                }
            }
        } else {
            // need to group the flavor per dimension.
            // First a map of dimension -> list(ProductFlavor)
            ArrayListMultimap<String, GroupableProductFlavorDsl> map = ArrayListMultimap.create();
            for (GroupableProductFlavorDsl flavor : productFlavors) {
                String flavorDimension = flavor.getFlavorDimension();

                if (flavorDimension == null) {
                    throw new RuntimeException(String.format(
                            "Flavor '$flavor.name' has no flavor dimension.",));
                }
                map.put(flavorDimension, flavor);
            }

            for (DefaultBuildType buildType : buildTypes) {
                List<GroupableProductFlavorDsl> flavorList =
                        Lists.newArrayListWithCapacity(flavorDimensionList.size());
                createMultiFlavoredBinaries(binaries, buildType, flavorList, 0, flavorDimensionList,
                        map)
            }
        }
    }

    private static void createMultiFlavoredBinaries(
            BinaryContainer binaries,
            DefaultBuildType buildType,
            List<GroupableProductFlavorDsl> flavors,
            int index,
            List<String> flavorDimensionList,
            ListMultimap<String, GroupableProductFlavorDsl> map) {
        if (index == flavorDimensionList.size()) {
            createBinary(binaries, buildType, flavors);
            return;
        }

        // fill the array at the current index.
        // get the dimension name that matches the index we are filling.
        String dimension = flavorDimensionList.get(index);

        // from our map, get all the possible flavors in that dimension.
        List<? extends GroupableProductFlavorDsl> flavorList = map.get(dimension);

        // loop on all the flavors to add them to the current index and recursively fill the next
        // indices.
        for (GroupableProductFlavorDsl flavor : flavorList) {
            flavors.set(index, flavor)
            createMultiFlavoredBinaries(binaries, buildType, flavors, index + 1,
                    flavorDimensionList, map)
        }
    }
}
