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
import com.android.build.gradle.tasks.AidlCompile;
import com.android.build.gradle.tasks.GenerateBuildConfig;
import com.android.build.gradle.tasks.MergeAssets;
import com.android.build.gradle.tasks.MergeResources;
import com.android.build.gradle.tasks.RenderscriptCompile;
import com.google.common.collect.Maps;

import org.gradle.api.Task;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.compile.AbstractCompile;

import java.util.Map;

/**
 * Registry to store the information about tasks created by the plugin.
 */
public class TaskInfoRegistry {

    @NonNull
    private final Map<TaskType, TaskInfo> registry = Maps.newHashMap();

    public TaskInfoRegistry() {
        // Populate registry with default task info.  TaskInfo can be overwritten if necessary.

        //       type                             class                      prefix      suffix           scope
        register(TaskType.PRE_BUILD,              Task.class,                "preBuild", "",              TaskInfo.Scope.VARIANT);
        register(TaskType.CHECK_MANIFEST,         Task.class,                "check",    "Manifest",      TaskInfo.Scope.VARIANT);
        register(TaskType.AIDL_COMPILE,           AidlCompile.class,         "compile",  "Aidl",          TaskInfo.Scope.VARIANT);
        register(TaskType.RENDERSCRIPT_COMPILE,   RenderscriptCompile.class, "compile",  "Renderscript",  TaskInfo.Scope.VARIANT);
        register(TaskType.MERGE_RESOURCES,        MergeResources.class,      "merge",    "Resources",     TaskInfo.Scope.VARIANT);
        register(TaskType.MERGE_ASSETS,           MergeAssets.class,         "merge",    "Assets",        TaskInfo.Scope.VARIANT);
        register(TaskType.GENERATE_BUILD_CONFIG,  GenerateBuildConfig.class, "generate", "BuildConfig",   TaskInfo.Scope.VARIANT);
        register(TaskType.JAVA_COMPILE,           AbstractCompile.class,     "compile",  "JavaWithJavac", TaskInfo.Scope.VARIANT);
        register(TaskType.OBFUSCATION,            Task.class,                "proguard", "",              TaskInfo.Scope.VARIANT);
        register(TaskType.PROCESS_JAVA_RESOURCES, Copy.class,                "process",  "JavaRes",       TaskInfo.Scope.VARIANT);
        register(TaskType.ASSEMBLE,               Task.class,                "assemble", "",              TaskInfo.Scope.VARIANT);
    }

    private void register(
            @NonNull TaskType type,
            @NonNull Class<?> clazz,
            @NonNull String prefix,
            @NonNull String suffix,
            @NonNull TaskInfo.Scope scope) {
        register(type, new TaskInfo(clazz, prefix, suffix, scope));
    }

    public void register(@NonNull TaskType type, @NonNull TaskInfo info) {
        registry.put(type, info);
    }

    @NonNull
    public TaskInfo get(@NonNull TaskType type) {
        return registry.get(type);
    }

}
