/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint.checks;

import com.android.testutils.SdkTestCase;
import com.android.tools.lint.detector.api.Detector;


public class GoogleMapDetectorTest extends AbstractCheckTest {

    private final SdkTestCase.TestFile mFileSupportMapFragment =
            java("src/com/google/android/gms/maps/SupportMapFragment.java", ""
                            + "package com.google.android.gms.maps;\n"
                            + "\n"
                            + "public class SupportMapFragment {\n"
                            + "    public void getMap(){ return null; }\n"
                            + "}\n"
            );

    private final SdkTestCase.TestFile mFileMapFragment =
            java("src/com/google/android/gms/maps/MapFragment.java", ""
                            + "package com.google.android.gms.maps;\n"
                            + "\n"
                            + "public class MapFragment {\n"
                            + "    public void getMap(){ return null; }\n"
                            + "}\n"
            );

    private final SdkTestCase.TestFile mFileStreetViewPanoramaFragment =
            java("src/com/google/android/gms/maps/StreetViewPanoramaFragment.java", ""
                            + "package com.google.android.gms.maps;\n"
                            + "\n"
                            + "public class StreetViewPanoramaFragment {\n"
                            + "    public void getStreetViewPanorama(){ return null; }\n"
                            + "}\n"
            );

    private final SdkTestCase.TestFile mFileSupportStreetViewPanoramaFragment =
            java("src/com/google/android/gms/maps/SupportStreetViewPanoramaFragment.java", ""
                            + "package com.google.android.gms.maps;\n"
                            + "\n"
                            + "public class SupportStreetViewPanoramaFragment {\n"
                            + "    public void getStreetViewPanorama(){ return null; }\n"
                            + "}\n"
            );

    @Override
    protected Detector getDetector() {
        return new GoogleMapDetector();
    }

    public void testDeprecatedMethodNames() throws Exception {
        String expected =
                "src/test/pkg/GoogleMapTest.java:23: Warning: Replace the call to getMap() with an implementation of getMapAsync(OnMapReadyCallback). [GoogleMapDeprecatedCall]\n"
                        + "        supportMapFragment.getMap();\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/GoogleMapTest.java:24: Warning: Replace the call to getMap() with an implementation of getMapAsync(OnMapReadyCallback). [GoogleMapDeprecatedCall]\n"
                        + "        mapFragment.getMap();\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/GoogleMapTest.java:27: Warning: Replace the call to getStreetViewPanorama() with an implementation of getStreetViewPanoramaAsync(OnStreetViewPanoramaReadyCallback). [GoogleMapDeprecatedCall]\n"
                        + "        supportStreetViewFragment.getStreetViewPanorama();\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/GoogleMapTest.java:28: Warning: Replace the call to getStreetViewPanorama() with an implementation of getStreetViewPanoramaAsync(OnStreetViewPanoramaReadyCallback). [GoogleMapDeprecatedCall]\n"
                        + "        streetViewFragment.getStreetViewPanorama();\n"
                        + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "0 errors, 4 warnings\n";

        // Lint the project with dummy Google Play Services classes.
        String result = lintProject(
                copy("src/test/pkg/GoogleMapTest.java.txt", "src/test/pkg/GoogleMapTest.java"),
                mFileSupportMapFragment, mFileMapFragment, mFileStreetViewPanoramaFragment,
                mFileSupportStreetViewPanoramaFragment);

        assertEquals(expected, result);
    }

}