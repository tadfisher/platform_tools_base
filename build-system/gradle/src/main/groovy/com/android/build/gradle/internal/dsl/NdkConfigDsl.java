/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.NdkConfig;
import com.google.common.collect.Sets;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Implementation of NdkConfig to be used in the gradle DSL.
 */
public class NdkConfigDsl implements NdkConfig, Serializable {
    private static final long serialVersionUID = 1L;

    private String moduleName;
    private String cFlags;
    private Set<String> ldLibs;
    private String stl;

    public NdkConfigDsl() {
    }

    public NdkConfigDsl(@NonNull NdkConfigDsl ndkConfig) {
        moduleName = ndkConfig.moduleName;
        cFlags = ndkConfig.cFlags;
        setLdLibs(ndkConfig.ldLibs);
    }

    @Override
    @Input @Optional
    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    @Input @Optional
    public String getcFlags() {
        return cFlags;
    }

    public void setcFlags(String cFlags) {
        this.cFlags = cFlags;
    }

    @Override
    @Input @Optional
    public Set<String> getLdLibs() {
        return ldLibs;
    }

    @NonNull
    public NdkConfigDsl ldLibs(String lib) {
        if (ldLibs == null) {
            ldLibs = Sets.newHashSet();
        }
        ldLibs.add(lib);
        return this;
    }

    @NonNull
    public NdkConfigDsl ldLibs(String... libs) {
        if (ldLibs == null) {
            ldLibs = Sets.newHashSetWithExpectedSize(libs.length);
        }
        Collections.addAll(ldLibs, libs);
        return this;
    }

    @NonNull
    public NdkConfigDsl setLdLibs(Collection<String> libs) {
        if (libs != null) {
            if (ldLibs == null) {
                ldLibs = Sets.newHashSetWithExpectedSize(libs.size());
            } else {
                ldLibs.clear();
            }
            for (String filter : libs) {
                ldLibs.add(filter);
            }
        } else {
            ldLibs = null;
        }
        return this;
    }

    @Override
    @Nullable
    public String getStl() {
        return stl;
    }

    public void setStl(String stl) {
        this.stl = stl;
    }
}
