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

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * An Android module, empty (just an empty manifest)
 */
class AndroidGradleModule extends GradleModule {

    public AndroidGradleModule(File location, String path, List<? extends GradleModule> projectDeps) {
        super(location, path, projectDeps);
    }

    @Override
    public String getBuildGradleContent() {
        List<? extends GradleModule> projectDeps = getProjectDeps();
        String content =
                "apply plugin: 'com.android.library'\n" +
                "\n" +
                        "android {\n" +
                        "  compileSdkVersion 21\n" +
                        "  buildToolsVersion '21.0.0'\n" +
                        "}\n" +
                        "\n" +
                "dependencies {\n";

        for (GradleModule dep : projectDeps) {
            content += "  compile project('" + dep.getPath() + "')\n";
        }
        content += "}\n";

        return content;
    }

    @Override
    public void createFiles() throws IOException {
        File root = getLocation();
        File src = new File(root, "src");
        File main = new File(src, "main");
        main.mkdirs();

        File manifest = new File(main, "AndroidManifest.xml");

        Files.write(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    package=\"com" + getPath().replace(':', '.') + "\" />\n",
                manifest,
                Charsets.UTF_8);
    }
}
