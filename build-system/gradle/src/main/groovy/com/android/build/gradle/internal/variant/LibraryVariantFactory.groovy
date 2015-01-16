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

package com.android.build.gradle.internal.variant
import com.android.SdkConstants
import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.internal.BuildTypeData
import com.android.build.gradle.internal.ProductFlavorData
import com.android.build.gradle.internal.TaskManager
import com.android.build.gradle.internal.VariantModel
import com.android.build.gradle.internal.api.LibraryVariantImpl
import com.android.build.gradle.internal.api.LibraryVariantOutputImpl
import com.android.build.gradle.internal.api.ReadOnlyObjectProvider
import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.tasks.MergeFileTask
import com.android.build.gradle.tasks.ExtractAnnotations
import com.android.build.gradle.tasks.MergeResources
import com.android.builder.core.BuilderConstants
import com.android.builder.core.DefaultBuildType
import com.android.builder.core.VariantType
import com.android.builder.dependency.LibraryBundle
import com.android.builder.dependency.LibraryDependency
import com.android.builder.dependency.ManifestDependency
import com.android.builder.model.AndroidLibrary
import com.android.builder.model.MavenCoordinates
import com.google.common.collect.Lists
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.tooling.BuildException

import static com.android.SdkConstants.FN_ANNOTATIONS_ZIP
import static com.android.SdkConstants.LIBS_FOLDER
import static com.android.build.gradle.internal.TaskManager.DIR_BUNDLES
import static com.android.builder.model.AndroidProject.FD_INTERMEDIATES
import static com.android.builder.model.AndroidProject.FD_OUTPUTS
/**
 */
public class LibraryVariantFactory implements VariantFactory<LibraryVariantData> {

    @NonNull
    private final BasePlugin basePlugin
    @NonNull
    private final LibraryExtension extension
    @NonNull
    private final TaskManager taskManager

    public LibraryVariantFactory(
            @NonNull BasePlugin basePlugin,
            @NonNull LibraryExtension extension,
            @NonNull TaskManager taskManager) {
        this.extension = extension
        this.basePlugin = basePlugin
        this.taskManager = taskManager
    }

    @Override
    @NonNull
    public LibraryVariantData createVariantData(
            @NonNull GradleVariantConfiguration variantConfiguration,
            @NonNull Set<String> densities,
            @NonNull Set<String> abis,
            @NonNull Set<String> compatibleScreens) {
        return new LibraryVariantData(basePlugin, variantConfiguration)
    }

    @Override
    @NonNull
    public BaseVariant createVariantApi(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData,
            @NonNull ReadOnlyObjectProvider readOnlyObjectProvider) {
        LibraryVariantImpl variant = basePlugin.getInstantiator().newInstance(
                LibraryVariantImpl.class, variantData, basePlugin, readOnlyObjectProvider)

        // now create the output objects
        List<? extends BaseVariantOutputData> outputList = variantData.getOutputs();
        List<BaseVariantOutput> apiOutputList = Lists.newArrayListWithCapacity(outputList.size());

        for (BaseVariantOutputData variantOutputData : outputList) {
            LibVariantOutputData libOutput = (LibVariantOutputData) variantOutputData;

            LibraryVariantOutputImpl output = basePlugin.getInstantiator().newInstance(
                    LibraryVariantOutputImpl.class, libOutput);

            apiOutputList.add(output);
        }

        variant.addOutputs(apiOutputList);

        return variant
    }

    @NonNull
    @Override
    public VariantType getVariantConfigurationType() {
        return VariantType.LIBRARY
    }

    @Override
    boolean isLibrary() {
        return true
    }

    @Override
    public void createTasks(
            @NonNull BaseVariantData<?> variantData,
            @Nullable Task assembleTask) {
        taskManager.createTasksForVariantData(variantData, assembleTask)
    }

    /***
     * Prevent customization of applicationId or applicationIdSuffix.
     */
    @Override
    public void validateModel(VariantModel model) {
        if (model.getDefaultConfig().getProductFlavor().getApplicationId() != null) {
            throw new GradleException("Library projects cannot set applicationId. " +
                    "applicationId is set to '" +
                    model.getDefaultConfig().getProductFlavor().getApplicationId() +
                    "' in default config.");
        }

        for (BuildTypeData buildType : model.getBuildTypes().values()) {
            if (buildType.getBuildType().getApplicationIdSuffix() != null) {
                throw new GradleException("Library projects cannot set applicationId. " +
                        "applicationIdSuffix is set to '" +
                        buildType.getBuildType().getApplicationIdSuffix() +
                        "' in build type '" + buildType.getBuildType().getName() + "'.");
            }
        }
        for (ProductFlavorData productFlavor : model.getProductFlavors().values()) {
            if (productFlavor.getProductFlavor().getApplicationId() != null) {
                throw new GradleException("Library projects cannot set applicationId. " +
                        "applicationId is set to '" +
                        productFlavor.getProductFlavor().getApplicationId() + "' in flavor '" +
                        productFlavor.getProductFlavor().getName() + "'.");
            }
        }

    }
}
