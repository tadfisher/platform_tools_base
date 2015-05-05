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

package com.android.ide.common.res2;

import static org.junit.Assert.assertEquals;

import com.android.ide.common.blame.SourceFilePosition;
import com.android.ide.common.blame.SourcePosition;

import junit.framework.TestCase;

import org.junit.Test;

import java.io.File;

public class MergingExceptionTest {
    @SuppressWarnings({"ThrowableInstanceNeverThrown", "ThrowableResultOfMethodCallIgnored"})
    @Test
    public void testGetMessage() {
        File file = new File("/some/random/path");
        assertEquals("Error: My error message",
                MergingException.withFile(null, "My error message").getMessage());
        assertEquals("Error: My error message",
                MergingException.withFile(null, "Error: My error message").getMessage());
        assertEquals("/some/random/path: Error: My error message",
                MergingException.withFile(file, "My error message", file).getMessage());
        assertEquals("/some/random/path:50: Error: My error message",
                MergingException.wrapException(new Exception("My error message"),
                        new SourceFilePosition(file, new SourcePosition(49, -1, -1)))
                        .getMessage());
        assertEquals("/some/random/path:50:4: Error: My error message",
                MergingException.wrapException(new Exception("My error message"),
                        new SourceFilePosition(file, new SourcePosition(49, 3, -1)))
                        .getMessage());
        assertEquals("/some/random/path:50:4: Error: My error message",
                MergingException.wrapException(new Exception("My error message"),
                        new SourceFilePosition(file, new SourcePosition(49, 3, -1)))
                        .getLocalizedMessage());
        assertEquals("/some/random/path: Error: My error message",
                MergingException.withFile(file, "/some/random/path: My error message")
                        .getMessage());
        assertEquals("/some/random/path: Error: My error message",
                MergingException.withFile(file, "/some/random/path My error message")
                        .getMessage());

        // end of string handling checks
        assertEquals("/some/random/path: Error: ",
                MergingException.withFile(file, "/some/random/path").getMessage());
        assertEquals("/some/random/path: Error: ",
                MergingException.withFile(file, "/some/random/path").getMessage());
        assertEquals("/some/random/path: Error: ",
                MergingException.withFile(file, "/some/random/path:").getMessage());
        assertEquals("/some/random/path: Error: ",
                MergingException.withFile(file, "/some/random/path: ").getMessage());
    }

}
