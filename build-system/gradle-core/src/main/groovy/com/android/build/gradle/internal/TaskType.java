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

import com.android.build.gradle.tasks.AidlCompile;
import com.android.build.gradle.tasks.GenerateBuildConfig;
import com.android.build.gradle.tasks.MergeAssets;
import com.android.build.gradle.tasks.MergeResources;
import com.android.build.gradle.tasks.RenderscriptCompile;

import org.gradle.api.tasks.compile.AbstractCompile;

/**
 * Enum of the tasks created by the Android plugin.
 */
public enum TaskType {
    PRE_BUILD,
    CHECK_MANIFEST,
    AIDL_COMPILE,
    RENDERSCRIPT_COMPILE,
    MERGE_RESOURCES,
    MERGE_ASSETS,
    GENERATE_BUILD_CONFIG,
    JAVA_COMPILE,
    OBFUSCATION,
    PROCESS_JAVA_RESOURCES,
    ASSEMBLE;
}
