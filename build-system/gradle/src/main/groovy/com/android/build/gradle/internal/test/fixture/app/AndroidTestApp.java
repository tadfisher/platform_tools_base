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

package com.android.build.gradle.internal.test.fixture.app;

import com.android.build.gradle.internal.test.fixture.TestSourceFile;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Interface for an Android test application.
 */
public interface AndroidTestApp {
    TestSourceFile getFile(String filename);

    TestSourceFile getFile(String filename, String path);

    Collection<TestSourceFile> getAllSourceFiles();

    void writeSources(File sourceDir) throws IOException;
}
