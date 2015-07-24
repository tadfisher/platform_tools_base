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
import com.android.sdklib.repositorycore.api.SdkPackage;
import com.android.sdklib.repositorycore.api.SdkSource;
import com.android.sdklib.repositorycore.api.SdkSourceCategory;
import com.android.sdklib.repositorycore.impl.generated.ObjectFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;

/**
 * Created by jbakermalone on 7/29/15.
 */
public class RepoSourceSource implements SdkSourcesSource {
    static final SdkSourceCategory REPO_CATEGORY = new SdkSourceCategory(ObjectFactory.class,
            new File(SdkPackage.class.getResource("sdk-common-01.xsd").toURI()));
    static final List<SdkSource> SOURCE;

    static {
        String baseUrl = System.getenv("SDK_TEST_BASE_URL");
        if (baseUrl == null || baseUrl.length() <= 0 || !baseUrl.endsWith("/")) {
            // TODO: move constant
            baseUrl = SdkRepoConstants.URL_GOOGLE_SDK_SITE;
        }

        // TODO: externalize uiname
        SOURCE = ImmutableList.of(
                new SdkSourceImpl(baseUrl, "Android Repository", )));

    }
    @Override
    public List<SdkSource> getSources() {

    }

    @Override
    public boolean addSource(SdkSource source) {
        return false;
    }

    @Override
    public void save() {

    }
}
