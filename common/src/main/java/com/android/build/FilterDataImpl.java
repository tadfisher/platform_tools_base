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

package com.android.build;

import com.android.annotations.NonNull;

import java.io.Serializable;

/**
 * Implementation of the {@link com.android.build.FilterData} interface
 */
class FilterDataImpl implements FilterData, Serializable {

    private final String identifier;
    private final String filterType;

    FilterDataImpl(String filterType, String identifier) {
        this.identifier = identifier;
        this.filterType = filterType;
    }

    @NonNull
    @Override
    public String getIdentifier() {
        return identifier;
    }

    @NonNull
    @Override
    public String getFilterType() {
        return filterType;
    }
}
