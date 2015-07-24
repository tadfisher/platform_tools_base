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

package com.android.sdklib.repositorycore.impl.remote;

import com.android.sdklib.repository.SdkRepoConstants;
import com.android.sdklib.repositorycore.api.SdkManager;
import com.android.sdklib.repositorycore.api.SdkPackage;
import com.android.sdklib.repositorycore.api.SdkSchemaExtension;
import com.android.sdklib.repositorycore.api.SdkSource;
import com.android.sdklib.repositorycore.impl.generated.v1.ObjectFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

/**
 * Created by jbakermalone on 7/29/15.
 */
public class GenericSourceProvider implements SdkSourceProvider {
    private final SdkSource mSource;
    private Set<SdkSchemaExtension> mExtension;

    public GenericSourceProvider(SdkManager manager) {
        String baseUrl = System.getenv("SDK_TEST_BASE_URL");
        if (baseUrl == null || baseUrl.length() <= 0 || !baseUrl.endsWith("/")) {
            // TODO: move constant
            baseUrl = SdkRepoConstants.URL_GOOGLE_SDK_SITE;
        }
        mExtension = ImmutableSet.of(manager.getSchemaExtension("http://schemas.android.com/sdk/android/common/01"));

        // TODO: externalize uiname
        // TODO: get active status
        mSource = new SdkSourceImpl(baseUrl, "Android Repository", mExtension, true);

    }

    @Override
    public List<SdkSource> getSources() {
        return ImmutableList.of(mSource);
    }

    @Override
    public boolean addSource(SdkSource source) {
        return false;
    }

    @Override
    public void save() {
        // TODO
    }

    @Override
    public Set<SdkSchemaExtension> getValidExtensions() {
        return mExtension;
    }
}
