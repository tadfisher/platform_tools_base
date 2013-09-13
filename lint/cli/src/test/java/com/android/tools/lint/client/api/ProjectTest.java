/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.tools.lint.client.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.checks.AbstractCheckTest;
import com.android.tools.lint.checks.UnusedResourceDetector;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProjectTest extends AbstractCheckTest {
    @Override
    protected boolean ignoreSystemErrors() {
        return false;
    }

    public void testCycle() throws Exception {
        // Ensure that a cycle in library project dependencies doesn't cause
        // infinite directory traversal
        File master = getProjectDir("MasterProject",
                // Master project
                "multiproject/main-manifest.xml=>AndroidManifest.xml",
                "multiproject/main.properties=>project.properties",
                "multiproject/MainCode.java.txt=>src/foo/main/MainCode.java"
        );
        File library = getProjectDir("LibraryProject",
                // Library project
                "multiproject/library-manifest.xml=>AndroidManifest.xml",
                "multiproject/main.properties=>project.properties", // RECURSIVE - points to self
                "multiproject/LibraryCode.java.txt=>src/foo/library/LibraryCode.java",
                "multiproject/strings.xml=>res/values/strings.xml"
        );

        assertEquals(""
                + "MasterProject/project.properties: Error: Circular library dependencies; check your project.properties files carefully [LintError]\n"
                + "1 errors, 0 warnings\n",

                checkLint(Arrays.asList(master, library)));
    }

    public void testInvalidLibraryReferences1() throws Exception {
        TestClient client = new TestClient();
        File dir = new File("bogus");
        TestProject project1 = new TestProject(client, dir);
        client.registerProject(dir, project1);
        project1.setDirectLibraries(Collections.<Project>singletonList(project1));
        List<Project> libraries = project1.getAllLibraries();
        assertNotNull(libraries);
        assertEquals("Warning: Internal lint error: encountered Project [dir=bogus] more than once", client.getLoggedOutput());
    }

    public void testInvalidLibraryReferences2() throws Exception {
        TestClient client = new TestClient();
        File dir = new File("bogus");
        TestProject project1 = new TestProject(client, dir);
        client.registerProject(dir, project1);
        TestProject project2 = new TestProject(client, dir);
        client.registerProject(dir, project2);
        project2.setDirectLibraries(Collections.<Project>singletonList(project1));
        project1.setDirectLibraries(Collections.<Project>singletonList(project2));
        List<Project> libraries = project1.getAllLibraries();
        assertNotNull(libraries);
        assertEquals("Warning: Internal lint error: encountered Project [dir=bogus] more than once", client.getLoggedOutput());
    }

    private class TestClient extends TestLintClient {
        @SuppressWarnings("StringBufferField")
        private StringBuilder mLog = new StringBuilder();

        @Override
        public void log(@NonNull Severity severity, @Nullable Throwable exception,
                @Nullable String format, @Nullable Object... args) {
            assertNotNull(format);
            mLog.append(severity.getDescription()).append(": ");
            mLog.append(String.format(format, args));
        }

        public String getLoggedOutput() {
            return mLog.toString();
        }
    }

    private static class TestProject extends Project {
        protected TestProject(@NonNull LintClient client, @NonNull File dir) {
            super(client, dir, dir);
        }

        public void setDirectLibraries(List<Project> libraries) {
            mDirectLibraries = libraries;
        }
    }

    @Override
    protected Detector getDetector() {
        return new UnusedResourceDetector();
    }
}
