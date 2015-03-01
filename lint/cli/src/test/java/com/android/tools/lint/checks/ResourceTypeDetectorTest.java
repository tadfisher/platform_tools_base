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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class ResourceTypeDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ResourceTypeDetector();
    }

    public void testFlow() throws Exception {
        assertEquals(""
                + "src/p1/p2/Flow.java:18: Error: Wrong resource type; expected drawable but received string [ResourceType]\n"
                + "        resources.getDrawable(R.string.my_string); // ERROR\n"
                + "                              ~~~~~~~~~~~~~~~~~~\n"
                + "src/p1/p2/Flow.java:22: Error: Wrong resource type; expected drawable but received string [ResourceType]\n"
                + "        myMethod(R.string.my_string); // ERROR\n"
                + "                 ~~~~~~~~~~~~~~~~~~\n"
                + "2 errors, 0 warnings\n",

                lintProject("src/p1/p2/Flow.java.txt=>src/p1/p2/Flow.java",
                        "src/android/support/annotation/DrawableRes.java.txt=>src/android/support/annotation/DrawableRes.java"));
    }

    public void testColorAsDrawable() throws Exception {
        assertEquals(
                "No warnings.",

                lintProject("src/p1/p2/ColorAsDrawable.java.txt=>src/p1/p2/ColorAsDrawable.java"));
    }

}
