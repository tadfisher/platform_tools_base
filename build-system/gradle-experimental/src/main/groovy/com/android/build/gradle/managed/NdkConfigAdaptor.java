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
import com.android.build.gradle.internal.core.NdkConfig;
import com.android.build.gradle.ndk.ManagedNdkConfig;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Set;

/**
 * Created by chiur on 3/10/15.
 */
public class NdkConfigAdaptor implements NdkConfig {

    ManagedNdkConfig ndkConfig;

    public NdkConfigAdaptor(@NonNull ManagedNdkConfig ndkConfig) {
        this.ndkConfig = ndkConfig;
    }

    @Nullable
    @Override
    public String getModuleName() {
        return ndkConfig.getModuleName();
    }

    @Nullable
    @Override
    public String getcFlags() {
        return ndkConfig.getCFlags();
    }

    @Nullable
    @Override
    public Collection<String> getLdLibs() {
        return Lists.newArrayList(Iterables.transform(ndkConfig.getLdLibs(),
                new Function<ManagedString, String>() {
                    @Override
                    public String apply(@Nullable ManagedString managedString) {
                        return managedString == null ? null : managedString.getValue();
                    }
                }));
    }

    @Nullable
    @Override
    public Set<String> getAbiFilters() {
        return Sets.newHashSet(Iterables.transform(ndkConfig.getAbiFilters(),
                new Function<ManagedString, String>() {
                    @Override
                    public String apply(@Nullable ManagedString managedString) {
                        return managedString == null ? null : managedString.getValue();
                    }
                }));
    }

    @Nullable
    @Override
    public String getStl() {
        return ndkConfig.getStl();
    }
}
