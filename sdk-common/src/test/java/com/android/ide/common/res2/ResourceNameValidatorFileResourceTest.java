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

package com.android.ide.common.res2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.android.annotations.Nullable;
import com.android.resources.ResourceType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ResourceNameValidatorFileResourceTest {

    @Parameterized.Parameters(name="file={0}, resourceType={1} gives error {2}")
    public static Collection<Object[]> expected() {
        final String MUST_CONTAIN_ONLY_ETC =
                " is not a valid file-based resource name character: File-based resource names must contain only lowercase a-z, 0-9, or underscore";
        final String THE_FILE_NAME_MUST_END_WITH_XML_OR_PNG =
                "The file name must end with .xml or .png";
        return Arrays.asList(new Object[][] {
                //{ resourceName, resourceType, sourceFile, expectedException }
                { "", ResourceType.ANIMATOR, "Resource must have a name"},
                { "foo.png", ResourceType.DRAWABLE, null},
                { "foo.xml", ResourceType.DRAWABLE, null},
                { "foo.9.png", ResourceType.DRAWABLE, null},
                { "foo.other.png", ResourceType.DRAWABLE, "'.'" + MUST_CONTAIN_ONLY_ETC},
                { "foo.xml", ResourceType.XML, null},
                { "foo.png", ResourceType.XML, "The file name must end with .xml"},
                { "foo.8.xml", ResourceType.DRAWABLE, "'.'" + MUST_CONTAIN_ONLY_ETC},
                { "foo", ResourceType.DRAWABLE, THE_FILE_NAME_MUST_END_WITH_XML_OR_PNG},
                { "foo.txt", ResourceType.RAW, null},
                { "foo", ResourceType.RAW, null},
                { "foo", ResourceType.RAW, null},
                { "foo.txt", ResourceType.DRAWABLE, THE_FILE_NAME_MUST_END_WITH_XML_OR_PNG},
                { "Foo.png", ResourceType.DRAWABLE,
                        "File-based resource names must start with a lowercase letter"},
                { "foo$.png", ResourceType.DRAWABLE, "'$'" + MUST_CONTAIN_ONLY_ETC},
                { "enum.png", ResourceType.DRAWABLE,
                        "enum is not a valid name (reserved Java keyword)"},
        });
    }

    @Parameterized.Parameter(value=0)
    public String mSourceFileName;

    @Parameterized.Parameter(value=1)
    public ResourceType mResourceType;

    @Parameterized.Parameter(value=2)
    public String mExpectedErrorMessage;


    @Test
    public void validate() {
        String errorMessage = null;
        File file = new File(mSourceFileName);
        try {
            ResourceNameValidator.validate(file, mResourceType);
        } catch (MergingException e) {
            errorMessage = e.getMessage();
        }
        assertErrorMessageCorrect(mExpectedErrorMessage, errorMessage, file);
    }


    public static void assertErrorMessageCorrect(@Nullable String expected, @Nullable String actual, @Nullable File file) {
        if (expected == null) {
            assertNull("Was not expecting error ", actual);
        } else {
            assertNotNull("Was expecting error " + expected + " but passed", actual);
            if (file == null) {
                assertEquals("Error: " + expected, actual);
            } else {
                assertEquals(file.getAbsolutePath() + ": Error: " + expected, actual);
            }
        }
    }
}
