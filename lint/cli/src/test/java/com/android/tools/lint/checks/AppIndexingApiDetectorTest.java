/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class AppIndexingApiDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new AppIndexingApiDetector();
    }

    public void testOk() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject(
                        "appindexing_ok.xml=>AndroidManifest.xml"));
    }

    public void testCheckDataNode() throws Exception {
        assertEquals(
                "AndroidManifest.xml:28: Warning: At least one android:scheme attribute should be set for the intent filter [AppIndexing]\n"
                        + "                <data android:pathPrefix=\"gizmos\" />\n"
                        + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "AndroidManifest.xml:28: Warning: android:host attribute must be set for the intent filter [AppIndexing]\n"
                        + "                <data android:pathPrefix=\"gizmos\" />\n"
                        + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "AndroidManifest.xml:28: Warning: android:pathPrefix attribute should start with / [AppIndexing]\n"
                        + "                <data android:pathPrefix=\"gizmos\" />\n"
                        + "                      ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 3 warnings\n",
                lintProject(
                        "appindexing_wrong_data.xml=>AndroidManifest.xml"));
    }

    public void testNoActivity() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject(
                        "appindexing_no_activity.xml=>AndroidManifest.xml"));
    }

    public void testNoActionView() throws Exception {
        assertEquals(
                "AndroidManifest.xml:10: Warning: Need an <action> tag that specifies the ACTION_VIEW intent action. [AppIndexing]\n"
                        + "        <activity\n"
                        + "        ^\n"
                        + "AndroidManifest.xml:20: Warning: Need an <action> tag that specifies the ACTION_VIEW intent action. [AppIndexing]\n"
                        + "        <activity\n"
                        + "        ^\n"
                        + "0 errors, 2 warnings\n",
                lintProject(
                        "appindexing_no_action_view.xml=>AndroidManifest.xml"));
    }

    public void testNotBrowsable() throws Exception {
        assertEquals(
                "AndroidManifest.xml:25: Warning: Activity supporting ACTION_VIEW is not set as browsable [AppIndexing]\n"
                        + "            <intent-filter android:label=\"@string/title_activity_fullscreen\">\n"
                        + "            ^\n"
                        + "0 errors, 1 warnings\n",
                lintProject(
                        "appindexing_not_browsable.xml=>AndroidManifest.xml"));
    }
}
