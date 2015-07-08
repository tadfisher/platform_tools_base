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

package com.android.build.gradle.integration.common.fixture;

import static org.junit.Assert.assertTrue;

import com.android.annotations.NonNull;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.io.Files;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Allows project files to be modified, but stores their original content, so it can be restored
 * for the next test.
 */
public class TemporaryProjectModification implements Closeable {

    private final GradleTestProject mTestProject;
    private final Map<String, String> mFileContentToRestore = new HashMap<String, String>();


    public TemporaryProjectModification(GradleTestProject testProject) {
        mTestProject = testProject;
    }


    public void replaceFile(
            @NonNull String relativePath,
            @NonNull final String content) throws IOException {
        modifyFile(relativePath, new Function<String, String>() {
            @Override
            public String apply(String input) {
                return content;
            }
        });
    }

    public void replaceInFile(
            @NonNull String relativePath,
            @NonNull final String search,
            @NonNull final String replace) throws IOException {
        modifyFile(relativePath, new Function<String, String>() {
            @Override
            public String apply(String input) {
                return input.replaceAll(search, replace);
            }
        });

    }

    public void modifyFile(
            @NonNull String relativePath,
            @NonNull Function<String, String> modification) throws IOException {
        File file = mTestProject.file(relativePath);

        String currentContent = Files.toString(file, Charsets.UTF_8);

        // We can modify multiple times, but we only want to store the original.
        if (!mFileContentToRestore.containsKey(relativePath)) {
            mFileContentToRestore.put(relativePath, currentContent);
        }

        String newContent = modification.apply(currentContent);

        if (newContent == null) {
            assertTrue("File should have been deleted", file.delete());
        } else {
            Files.write(newContent, file, Charsets.UTF_8);
        }
    }

    /**
     * Returns the project back to its original state.
     */
    @Override
    public void close() throws IOException {
        for (Map.Entry<String, String> entry: mFileContentToRestore.entrySet()) {
            Files.write(entry.getValue(), mTestProject.file(entry.getKey()), Charsets.UTF_8);
        }
        mFileContentToRestore.clear();
    }
}
