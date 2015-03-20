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

package com.android.build.gradle.internal.tasks;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.AndroidAsciiReportRenderer;
import com.android.build.gradle.internal.variant.BaseVariantData;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.logging.StyledTextOutputFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class DependencyReportTask extends DefaultTask {

    private AndroidAsciiReportRenderer mRenderer = new AndroidAsciiReportRenderer();

    private Set<BaseVariantData> mVariants = new HashSet<BaseVariantData>();

    @TaskAction
    public void generate() throws IOException {
        mRenderer.setOutput(getServices().get(StyledTextOutputFactory.class).create(getClass()));

        SortedSet<BaseVariantData> sortedConfigurations = new TreeSet<BaseVariantData>(
                new Comparator<BaseVariantData>() {
                    @Override
            public int compare(BaseVariantData conf1, BaseVariantData conf2) {
                return conf1.getName().compareTo(conf2.getName());
            }
        });
        sortedConfigurations.addAll(getVariants());
        for (BaseVariantData variant : sortedConfigurations) {
            mRenderer.startVariant(variant);
            mRenderer.render(variant);
        }
    }

    /**
     * Returns the configurations to generate the report for. Default to all configurations of
     * this task's containing project.
     *
     * @return the configurations.
     */
    public Set<BaseVariantData> getVariants() {
        return mVariants;
    }

    /**
     * Sets the variants to generate the report for.
     *
     * @param variants the variants. Must not be null.
     */
    public void setVariants(@NonNull Collection<BaseVariantData> variants) {
        this.mVariants.addAll(variants);
    }

    public void setVariants(
            Set<BaseVariantData> variants) {
        mVariants = variants;
    }

    public AndroidAsciiReportRenderer getRenderer() {
        return mRenderer;
    }

    public void setRenderer(@NonNull AndroidAsciiReportRenderer renderer) {
        this.mRenderer = renderer;
    }}
