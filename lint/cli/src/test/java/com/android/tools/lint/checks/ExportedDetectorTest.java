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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class ExportedDetectorTest extends AbstractCheckTest {

    @Override
    protected Detector getDetector() {
        return new ExportedDetector();
    }

    public void testNoWarningOnExplicitlyExportedTrueService() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject("exporteddetector_service_explicit_true.xml=>AndroidManifest.xml"));

    }

    public void testNoWarningOnExplicitlyExportedFalseService() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject("exporteddetector_service_explicit_false.xml=>AndroidManifest.xml"));

    }

    public void testWarningOnServiceWithNoExportAttribute() throws Exception {
        assertEquals(
                "AndroidManifest.xml:26: Warning: Component does not explicitly set android:exported [ImplicitExported]\n"
                        + "        <service>\n"
                        + "        ^\n"
                        + "0 errors, 1 warnings\n",
                lintProject("exporteddetector_service_implicit.xml=>AndroidManifest.xml"));

    }

    public void testNoWarningOnExplicitlyExportedTrueActivity() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject("exporteddetector_activity_explicit_true.xml=>AndroidManifest.xml"));

    }

    public void testNoWarningOnExplicitlyExportedFalseActivity() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject("exporteddetector_activity_explicit_false.xml=>AndroidManifest.xml"));

    }

    public void testWarningOnActivityWithNoExportAttribute() throws Exception {
        assertEquals(
                "AndroidManifest.xml:10: Warning: Component does not explicitly set android:exported [ImplicitExported]\n"
                        + "        <activity>\n"
                        + "        ^\n"
                        + "0 errors, 1 warnings\n",
                lintProject("exporteddetector_activity_implicit.xml=>AndroidManifest.xml"));

    }

    public void testNoWarningOnExplicitlyExportedTrueProvider() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject("exporteddetector_provider_explicit_true.xml=>AndroidManifest.xml"));

    }

    public void testNoWarningOnExplicitlyExportedFalseProvider() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject("exporteddetector_provider_explicit_false.xml=>AndroidManifest.xml"));

    }

    public void testWarningOnProviderWithNoExportAttribute() throws Exception {
        assertEquals(
                "AndroidManifest.xml:10: Warning: Component does not explicitly set android:exported [ImplicitExported]\n"
                        + "        <provider>\n"
                        + "        ^\n"
                        + "0 errors, 1 warnings\n",
                lintProject("exporteddetector_provider_implicit.xml=>AndroidManifest.xml"));

    }

    public void testNoWarningOnExplicitlyExportedTrueReceiver() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject("exporteddetector_receiver_explicit_true.xml=>AndroidManifest.xml"));

    }

    public void testNoWarningOnExplicitlyExportedFalseReceiver() throws Exception {
        assertEquals(
                "No warnings.",
                lintProject("exporteddetector_receiver_explicit_false.xml=>AndroidManifest.xml"));

    }

    public void testWarningOnReceiverWithNoExportAttribute() throws Exception {
        assertEquals(
                "AndroidManifest.xml:10: Warning: Component does not explicitly set android:exported [ImplicitExported]\n"
                        + "        <receiver>\n"
                        + "        ^\n"
                        + "0 errors, 1 warnings\n",
                lintProject("exporteddetector_receiver_implicit.xml=>AndroidManifest.xml"));

    }
}
