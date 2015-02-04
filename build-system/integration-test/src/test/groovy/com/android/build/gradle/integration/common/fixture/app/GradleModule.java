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

package com.android.build.gradle.integration.common.fixture.app;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Creates a gradle-based module.
 */
public abstract class GradleModule {

    private final File location;
    private final String path;
    private final List<? extends GradleModule> projectDeps;

    public abstract String getBuildGradleContent();
    public abstract void createFiles() throws IOException;

    protected GradleModule(File location, String path, List<? extends GradleModule> projectDeps) {
        this.location = location;
        this.path = path;
        this.projectDeps = projectDeps;
    }

    public void create() throws IOException {
        location.mkdirs();

        setupBuildGradle();
        createFiles();
    }

    public File getLocation() {
        return location;
    }

    public String getPath() {
        return path;
    }

    public List<? extends GradleModule> getProjectDeps() {
        return projectDeps;
    }

    public void setupBuildGradle() throws IOException {
        String content = getBuildGradleContent();

        Files.write(content, new File(location, "build.gradle"), Charset.defaultCharset());
    }
}
