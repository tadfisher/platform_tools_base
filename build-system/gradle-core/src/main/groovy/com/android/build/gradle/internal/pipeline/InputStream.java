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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public interface InputStream {

    enum FileStatus { ADDED, CHANGED, REMOVED }

    @NonNull
    Set<StreamType> getTypes();

    @NonNull
    Set<StreamScope> getScopes();

    @NonNull
    Collection<File> getFiles();

    @NonNull
    Map<File, FileStatus> getChangedFiles();

    boolean isReferencedOnly();
}
