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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class SetTextDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new SetTextDetector();
    }

    public void test() throws Exception {
        assertEquals(
            "src/test/pkg/CustomScreen.java:17: Warning: Do not concatenate text displayed with setText. Use resource string with placeholders. [ConcatenationInSetText]\n" +
            "    view.setText(Integer.toString(50) + \"%\");\n" +
            "                 ~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "src/test/pkg/CustomScreen.java:18: Warning: Do not concatenate text displayed with setText. Use resource string with placeholders. [ConcatenationInSetText]\n" +
            "    view.setText(Double.toString(12.5) + \" miles\");\n" +
            "                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "src/test/pkg/CustomScreen.java:21: Warning: Do not concatenate text displayed with setText. Use resource string with placeholders. [ConcatenationInSetText]\n" +
            "    btn.setText(\"User \" + getUserName());\n" +
            "                ~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "src/test/pkg/CustomScreen.java:13: Warning: String literal in setText can not be translated. Use Android resources instead. [LiteralInSetText]\n" +
            "    view.setText(\"Hardcoded\");\n" +
            "                 ~~~~~~~~~~~\n" +
            "src/test/pkg/CustomScreen.java:18: Warning: String literal in setText can not be translated. Use Android resources instead. [LiteralInSetText]\n" +
            "    view.setText(Double.toString(12.5) + \" miles\");\n" +
            "                                         ~~~~~~~~\n" +
            "src/test/pkg/CustomScreen.java:21: Warning: String literal in setText can not be translated. Use Android resources instead. [LiteralInSetText]\n" +
            "    btn.setText(\"User \" + getUserName());\n" +
            "                ~~~~~~~\n" +
            "src/test/pkg/CustomScreen.java:17: Warning: Number formatting does not take into account locale settings. Consider using String.format instead. [NumberInSetText]\n" +
            "    view.setText(Integer.toString(50) + \"%\");\n" +
            "                 ~~~~~~~~~~~~~~~~~~~~\n" +
            "src/test/pkg/CustomScreen.java:18: Warning: Number formatting does not take into account locale settings. Consider using String.format instead. [NumberInSetText]\n" +
            "    view.setText(Double.toString(12.5) + \" miles\");\n" +
            "                 ~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 8 warnings\n",

            lintProject(
                    "bytecode/.classpath=>.classpath",
                    "project.properties19=>project.properties",
                    "bytecode/AndroidManifest.xml=>AndroidManifest.xml",
                    "res/layout/onclick.xml=>res/layout/onclick.xml",
                    "bytecode/CustomScreen.java.txt=>src/test/pkg/CustomScreen.java",
                    "bytecode/CustomScreen.class.data=>bin/classes/test/pkg/CustomScreen.class"
                    ));
    }
}
