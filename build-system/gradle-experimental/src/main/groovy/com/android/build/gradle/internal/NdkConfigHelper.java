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

package com.android.build.gradle.internal;

import com.android.annotations.NonNull;
import com.android.build.gradle.managed.NdkConfig;
import com.android.build.gradle.managed.NdkOptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.gradle.model.Unmanaged;

import java.util.List;
import java.util.Set;

/**
 * Created by chiur on 6/18/15.
 */
public class NdkConfigHelper {
    public static void init(NdkOptions ndk) {
        ndk.setCFlags(Lists.<String>newArrayList());
        ndk.setCppFlags(Lists.<String>newArrayList());
        ndk.setLdLibs(Lists.<String>newArrayList());
        ndk.setAbiFilters(Sets.<String>newHashSet());
    }

    public static void merge(NdkOptions base, NdkOptions other) {
        if (other.getModuleName() != null) {
            base.setModuleName(other.getModuleName());
        }

        if (other.getAbiFilters() != null) {
            base.getAbiFilters().addAll(other.getAbiFilters());
        }

        if (other.getCFlags() != null) {
            base.getCFlags().addAll(other.getCFlags());
        }
        if (other.getCppFlags() != null) {
            base.getCppFlags().addAll(other.getCppFlags());
        }
        if (other.getLdLibs() != null) {
            base.getLdLibs().addAll(other.getLdLibs());
        }
        if (other.getStl() != null) {
            base.setStl(other.getStl());
        }
        if (other.getRenderscriptNdkMode() != null) {
            base.setRenderscriptNdkMode(other.getRenderscriptNdkMode());
        }
    }

    /*
    public static void merge(NdkOptions base, com.android.build.gradle.internal.dsl.NdkOptions other) {
        if (other.getModuleName() != null) {
            base.setModuleName(other.getModuleName());
        }

        if (other.getAbiFilters() != null) {
            base.getAbiFilters().addAll(other.getAbiFilters());
        }

        if (other.getcFlags() != null) {
            base.getCFlags().add(other.getcFlags());
        }
        if (other.getCppFlags() != null) {
            base.getCppFlags().add(other.getCppFlags());
        }
        if (other.getLdLibs() != null) {
            base.getLdLibs().addAll(other.getLdLibs());
        }
        if (other.getStl() != null) {
            base.setStl(other.getStl());
        }
    }
    */
}
