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

package com.android.build.gradle.managed;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.ApiVersion;

/**
 * Created by chiur on 3/9/15.
 */
public class ApiVersionAdaptor implements ApiVersion {

    private final ManagedApiVersion apiVersion;

    public static boolean isEmpty(ManagedApiVersion apiVersion) {
        return apiVersion.getApiLevel() == null &&
                apiVersion.getApiString() == null &&
                apiVersion.getCodename() == null;
    }

    public ApiVersionAdaptor(ManagedApiVersion apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public int getApiLevel() {
        return apiVersion.getApiLevel() == null ? 0 : apiVersion.getApiLevel();
    }

    @Nullable
    @Override
    public String getCodename() {
        return apiVersion.getCodename();
    }

    @NonNull
    @Override
    public String getApiString() {
        return apiVersion.getApiString();
    }
}
