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

package com.android.tools.lint;

import com.android.tools.lint.checks.AbstractCheckTest;
import com.android.tools.lint.checks.SdCardDetector;
import com.android.tools.lint.client.api.IJavaParser;
import com.android.tools.lint.detector.api.Detector;

public class EcjParserTest extends AbstractCheckTest {
    public void testTryCatchHang() throws Exception {
        // Ensure that we're really using this parser
        IJavaParser javaParser = createClient().getJavaParser();
        assertNotNull(javaParser);
        assertTrue(javaParser.getClass().getName(), javaParser instanceof EcjParser);

        // See https://code.google.com/p/projectlombok/issues/detail?id=573#c6
        // With lombok.ast 0.2.1 and the parboiled-based Java parser this test will hang forever.
        assertEquals(
                "No warnings.",

                lintProject("src/test/pkg/TryCatchHang.java.txt=>src/test/pkg/TryCatchHang.java"));
    }

    @Override
    protected Detector getDetector() {
        return new SdCardDetector();
    }
}
