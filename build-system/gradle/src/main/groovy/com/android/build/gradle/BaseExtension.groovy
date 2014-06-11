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

package com.android.build.gradle

import com.android.annotations.NonNull
import com.android.builder.core.DefaultBuildType
import com.android.builder.core.DefaultProductFlavor
import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import com.android.builder.model.SourceProvider
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

/**
 * Created by chiur on 6/10/14.
 */
class BaseExtension {
    String target
    private AndroidExtension extension
    private BasePlugin plugin

    final NamedDomainObjectContainer<DefaultProductFlavor> productFlavors
    final NamedDomainObjectContainer<DefaultBuildType> buildTypes

    BaseExtension(
            BasePlugin plugin,
            @NonNull NamedDomainObjectContainer<DefaultBuildType> buildTypes,
            @NonNull NamedDomainObjectContainer<DefaultProductFlavor> productFlavors) {
        this.plugin = plugin
        this.buildTypes = buildTypes
        this.productFlavors = productFlavors
    }


    public String getCompileSdkVersion() {
        return target
    }

    void compileSdkVersion(String target) {
//        plugin.checkTasksAlreadyCreated()
        this.target = target
    }

    void compileSdkVersion(int apiLevel) {
        compileSdkVersion("android-" + apiLevel)
    }

    void setCompileSdkVersion(int apiLevel) {
        compileSdkVersion(apiLevel)
    }

    void setCompileSdkVersion(String target) {
        compileSdkVersion(target)
    }

    void setAndroidExtension(AndroidExtension extension) {
        this.extension = extension
    }

    def propertyMissing(String name) {
        if (extension != null) {
            return extension."$name"
        } else {
            throw new MissingPropertyException(name, this.class)
        }
    }

    def methodMissing(String name, args) {
        if (extension != null) {
            extension."$name"(*args)
        } else {
            throw new MissingMethodException(name, this.class, args)
        }
    }

    void buildTypes(Action<? super NamedDomainObjectContainer<DefaultBuildType>> action) {
        plugin.checkTasksAlreadyCreated()
        action.execute(buildTypes)
    }

    void productFlavors(Action<? super NamedDomainObjectContainer<DefaultProductFlavor>> action) {
        plugin.checkTasksAlreadyCreated()
        action.execute(productFlavors)
    }

    public void registerBuildTypeSourceProvider(
            @NonNull String name,
            @NonNull BuildType buildType,
            @NonNull SourceProvider sourceProvider) {
        plugin.registerBuildTypeSourceProvider(name, buildType, sourceProvider)
    }

    public void registerProductFlavorSourceProvider(
            @NonNull String name,
            @NonNull ProductFlavor productFlavor,
            @NonNull SourceProvider sourceProvider) {
        plugin.registerProductFlavorSourceProvider(name, productFlavor, sourceProvider)
    }

}
