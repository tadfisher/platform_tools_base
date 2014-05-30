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

package com.android.build.gradle.internal.dsl

import com.google.common.collect.Sets

/**
 * Class to hold native options
 */
class NativeOptions {

    public static final String[] KNOWN_ABIS = ['armeabi', 'armeabi-v7a', 'x86', 'mips' ]

    Set<String> abis = Sets.newHashSet(Arrays.asList(KNOWN_ABIS))

    boolean splitByAbi = false;
    boolean includeUniversalApk = false;

    public void splitByApi(boolean value) {
        splitByAbi = true
    }

    public void includeUniversalApk(boolean value) {
        includeUniversalApk = true
    }
}
