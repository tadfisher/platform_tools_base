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
import com.android.annotations.concurrency.Immutable;
import com.android.build.gradle.tasks.AidlCompile;
import com.android.build.gradle.tasks.GenerateBuildConfig;
import com.android.build.gradle.tasks.MergeAssets;
import com.android.build.gradle.tasks.MergeResources;
import com.android.build.gradle.tasks.RenderscriptCompile;

import org.gradle.api.Task;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.compile.AbstractCompile;

/**
 * Information about tasks that may be created.
 */
@Immutable
public class TaskInfo {
    enum Scope {
        GLOBAL,
        VARIANT,
        VARIANT_OUTPUT
    };

    @NonNull
    private final Class<?> type;
    @NonNull
    private final String prefix;
    @NonNull
    private final String suffix;
    @NonNull
    private final Scope scope;

    TaskInfo(
            @NonNull Class<?> type,
            @NonNull String prefix,
            @NonNull String suffix,
            @NonNull Scope scope) {
        this.type = type;
        this.prefix = prefix;
        this.suffix = suffix;
        this.scope = scope;
    }

    @NonNull
    public Class<?> getType() {
        return type;
    }

    @NonNull
    public String getPrefix() {
        return prefix;
    }

    @NonNull
    public String getSuffix() {
        return suffix;
    }

    @NonNull
    public Scope getScope() {
        return scope;
    }
}
