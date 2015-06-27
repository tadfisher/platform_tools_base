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
import com.android.build.gradle.model.AndroidComponentModelSourceSet;
import com.android.build.gradle.model.AndroidLanguageSourceSet;
import com.android.builder.model.NativeToolchain;

import org.gradle.model.Managed;
import org.gradle.model.Unmanaged;

import java.io.File;
import java.util.List;

/**
 * Configuration model for ExternalNativeComponentModelPlugin.
 */
@Managed
public interface ExternalBuildConfig {

    String getExecutable();
    void setExecutable(String executable);

    @Unmanaged
    List<String> getArgs();
    void setArgs(List<String> args);

    //NativeToolchain getToolchain();

    /**
     * Returns the full path of the C compiler.
     *
     * @return the C compiler path.
     */
    File getCCompilerExecutable();
    void setCCompilerExecutable(@NonNull File executable);

    /**
     * Returns the full path of the C++ compiler.
     *
     * @return the C++ compiler path.
     */
    File getCppCompilerExecutable();
    void setCppCompilerExecutable(@NonNull File executable);

    /**
     * The C Flags
     */
    @Unmanaged
    List<String> getCFlags();
    void setCFlags(@NonNull List<String> cFlags);

    /**
     * The C++ Flags
     */
    @Unmanaged
    List<String> getCppFlags();
    void setCppFlags(@NonNull List<String> cppFlags);

    @Unmanaged
    List<File> getDebuggableLibraryFolders();
    void setDebuggableLibraryFolders(@NonNull List<File> debuggableLibraryFolders);

    @Unmanaged
    AndroidLanguageSourceSet getSource();
    void setSource(AndroidLanguageSourceSet source);
}
