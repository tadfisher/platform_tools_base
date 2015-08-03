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

package com.android.build.gradle.internal.pipeline;

import com.android.annotations.NonNull;

import java.io.File;
import java.util.Set;

/**
 * The output of a transform, as used during the actual Transform.
 */
public interface OutputStream {

    /**
     * The types of the output. If the transform combines the streams, then
     * the output can represent multiple types.
     */
    @NonNull
    Set<StreamType> getTypes();

    /**
     * The scopes of the output. If the transform combines the streams, then
     * the output can represent multiple scopes.
     */
    @NonNull
    Set<StreamScope> getScopes();

    boolean isFolder();
    /**
     * The file or folder to write the output to.
     */
    @NonNull
    File getFile();
}
