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

package com.android.builder.model;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * A Java library.
 */
public interface JavaLibrary {
    /**
     * Returns the library's jar file.
     */
    @NonNull
    File getJarFile();

    /**
     * Returns this library's Maven coordinates of this library.
     */
    @Nullable
    MavenCoordinates getMavenCoordinates();

    /**
     * Returns the direct dependencies of this library.
     */
    @NonNull
    List<? extends JavaLibrary> getDependencies();

    /**
     * Indicates whether this library was requested or resolved.
     *
     * @return {@code true} if this library was requested or {@code false} if it was resolved.
     */
    boolean isRequested();
}
