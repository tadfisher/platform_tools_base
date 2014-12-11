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

package com.android.build.gradle.internal.dsl;

import com.google.common.collect.ImmutableList;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.internal.DynamicPropertyNamer;
import org.gradle.api.logging.Logger;
import org.gradle.internal.reflect.Instantiator;

import java.util.List;

/**
 * Container for product flavors.
 */
public class ProductFlavorContainer
        extends AbstractNamedDomainObjectContainer<GroupableProductFlavor>
        implements NamedDomainObjectContainer<GroupableProductFlavor> {
    GroupableProductFlavorFactory factory;

    List<String> flavorDimensions = ImmutableList.of();

    public ProductFlavorContainer(Instantiator instantiator, Project project, Logger logger) {
        super(GroupableProductFlavor.class, instantiator, new DynamicPropertyNamer());

        factory = new GroupableProductFlavorFactory(instantiator, project, logger);
    }

    public List<String> getFlavorDimensions() {
        return flavorDimensions;
    }

    public void flavorDimensions(String... dimensions) {
        flavorDimensions = ImmutableList.copyOf(dimensions);
    }

    @Override
    protected GroupableProductFlavor doCreate(String name) {
        return factory.create(name);
    }
}
