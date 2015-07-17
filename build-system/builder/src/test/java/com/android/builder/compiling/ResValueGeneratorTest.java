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

package com.android.builder.compiling;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import com.android.builder.internal.ClassFieldImpl;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import org.junit.Test;

import java.io.File;

public class ResValueGeneratorTest {

    @Test
    public void generateResValueFile() throws Exception {
        File genFolder = Files.createTempDir();
        genFolder.deleteOnExit();
        ResValueGenerator resValueGenerator = new ResValueGenerator(genFolder);

        resValueGenerator.addItems(ImmutableList.<Object>of(
                "some comment",
                new ClassFieldImpl("string", "name", "String!"),
                new ClassFieldImpl("integer", "one", "1"),
                new ClassFieldImpl("unknown_type", "b", "valueb")
        ));
        resValueGenerator.generate();

        File resValueFile = new File(genFolder,
                "values/" + ResValueGenerator.RES_VALUE_FILENAME_XML);
        assertTrue(resValueFile + " should exist", resValueFile.exists());

        String xml = Files.toString(resValueFile, Charsets.UTF_8);

        assertThat(xml).contains("<!-- some comment -->");
        assertThat(xml).contains("<string name=\"name\" translatable=\"false\">String!</string>");
        assertThat(xml).contains("<integer name=\"one\">1</integer>");
        assertThat(xml).contains("<item name=\"b\" type=\"unknown_type\">valueb</item>");

    }


}
