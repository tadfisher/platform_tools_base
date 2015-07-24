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

import com.android.sdklib.repositorycore.api.SdkSourceCategory;

/**
 * Created by jbakermalone on 7/22/15.
 */
public final class SdkSourceImpl implements com.android.sdklib.repositorycore.api.SdkSource {
    private String mUrl;
    private String mUiName;
    private boolean mEnabled;
    // TODO: auto-derive based on content?
    private SdkSourceCategory mCategory;

    @Override
    public SdkSourceCategory getCategory() {
        return mCategory;
    }

    @Override
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public String getUiName() {
        return mUiName;
    }

    @Override
    public String getUrl() {
        return mUrl;
    }
}
