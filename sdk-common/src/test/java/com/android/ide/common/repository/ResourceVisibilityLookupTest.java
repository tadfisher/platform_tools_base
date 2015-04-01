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
package com.android.ide.common.repository;

import com.android.builder.model.AndroidArtifact;
import com.android.builder.model.AndroidLibrary;
import com.android.ide.common.resources.ResourceUrl;
import com.android.resources.ResourceType;
import com.android.testutils.TestUtils;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ResourceVisibilityLookupTest extends TestCase {
    public void test() throws IOException {
        AndroidLibrary library = TestUtils.createMockLibrary(
                ""
                        + "int dimen activity_horizontal_margin 0x7f030000\n"
                        + "int dimen activity_vertical_margin 0x7f030001\n"
                        + "int id action_settings 0x7f060000\n"
                        + "int layout activity_main 0x7f020000\n"
                        + "int menu menu_main 0x7f050000\n"
                        + "int string action_settings 0x7f040000\n"
                        + "int string app_name 0x7f040001\n"
                        + "int string hello_world 0x7f040002",
                ""
                        + ""
                        + "dimen activity_vertical\n"
                        + "id action_settings\n"
                        + "layout activity_main\n"
        );

        ResourceVisibilityLookup visibility = ResourceVisibilityLookup.create(library);
        assertTrue(visibility.isPrivate(ResourceType.DIMEN, "activity_horizontal_margin"));
        assertFalse(visibility.isPrivate(ResourceType.ID, "action_settings"));
        assertFalse(visibility.isPrivate(ResourceType.LAYOUT, "activity_main"));
        //noinspection ConstantConditions
        assertTrue(visibility.isPrivate(ResourceUrl.parse("@dimen/activity_horizontal_margin")));

        assertFalse(visibility.isPrivate(ResourceType.DIMEN, "activity_vertical")); // public
        assertFalse(visibility.isPrivate(ResourceType.DIMEN, "unknown")); // not in this library
    }

    public void testAllPrivate() throws IOException {
        AndroidLibrary library = TestUtils.createMockLibrary(
                ""
                        + "int dimen activity_horizontal_margin 0x7f030000\n"
                        + "int dimen activity_vertical_margin 0x7f030001\n"
                        + "int id action_settings 0x7f060000\n"
                        + "int layout activity_main 0x7f020000\n"
                        + "int menu menu_main 0x7f050000\n"
                        + "int string action_settings 0x7f040000\n"
                        + "int string app_name 0x7f040001\n"
                        + "int string hello_world 0x7f040002",
                ""
        );

        ResourceVisibilityLookup visibility = ResourceVisibilityLookup.create(library);
        assertTrue(visibility.isPrivate(ResourceType.DIMEN, "activity_horizontal_margin"));
        assertTrue(visibility.isPrivate(ResourceType.ID, "action_settings"));
        assertTrue(visibility.isPrivate(ResourceType.LAYOUT, "activity_main"));
        assertTrue(visibility.isPrivate(ResourceType.DIMEN, "activity_vertical_margin"));

        assertFalse(visibility.isPrivate(ResourceType.DIMEN, "unknown")); // not in this library
    }

    public void testNotDeclared() throws IOException {
        AndroidLibrary library = TestUtils.createMockLibrary("", null);

        ResourceVisibilityLookup visibility = ResourceVisibilityLookup.create(library);
        assertFalse(visibility.isPrivate(ResourceType.DIMEN, "activity_horizontal_margin"));
        assertFalse(visibility.isPrivate(ResourceType.ID, "action_settings"));
        assertFalse(visibility.isPrivate(ResourceType.LAYOUT, "activity_main"));
        assertFalse(visibility.isPrivate(ResourceType.DIMEN, "activity_vertical"));

        assertFalse(visibility.isPrivate(ResourceType.DIMEN, "unknown")); // not in this library
    }

    public void testCombined() throws IOException {
        AndroidLibrary library1 = TestUtils.createMockLibrary(
                ""
                        + "int dimen activity_horizontal_margin 0x7f030000\n"
                        + "int dimen activity_vertical_margin 0x7f030001\n"
                        + "int id action_settings 0x7f060000\n"
                        + "int layout activity_main 0x7f020000\n"
                        + "int menu menu_main 0x7f050000\n"
                        + "int string action_settings 0x7f040000\n"
                        + "int string app_name 0x7f040001\n"
                        + "int string hello_world 0x7f040002",
                ""
        );
        AndroidLibrary library2 = TestUtils.createMockLibrary(
                ""
                        + "int layout foo 0x7f030001\n"
                        + "int layout bar 0x7f060000\n",
                ""
                        + "layout foo\n"
        );

        List<AndroidLibrary> androidLibraries = Arrays.asList(library1, library2);
        ResourceVisibilityLookup visibility = ResourceVisibilityLookup
                .create(androidLibraries, null);
        assertTrue(visibility.isPrivate(ResourceType.DIMEN, "activity_horizontal_margin"));
        assertTrue(visibility.isPrivate(ResourceType.ID, "action_settings"));
        assertTrue(visibility.isPrivate(ResourceType.LAYOUT, "activity_main"));
        assertTrue(visibility.isPrivate(ResourceType.DIMEN, "activity_vertical_margin"));
        assertFalse(visibility.isPrivate(ResourceType.LAYOUT, "foo"));
        assertTrue(visibility.isPrivate(ResourceType.LAYOUT, "bar"));

        assertFalse(visibility.isPrivate(ResourceType.DIMEN, "unknown")); // not in this library
    }

    public void testDependency() throws IOException {
        AndroidLibrary library1 = TestUtils.createMockLibrary(
                ""
                        + "int dimen activity_horizontal_margin 0x7f030000\n"
                        + "int dimen activity_vertical_margin 0x7f030001\n"
                        + "int id action_settings 0x7f060000\n"
                        + "int layout activity_main 0x7f020000\n"
                        + "int menu menu_main 0x7f050000\n"
                        + "int string action_settings 0x7f040000\n"
                        + "int string app_name 0x7f040001\n"
                        + "int string hello_world 0x7f040002",
                ""
        );
        AndroidLibrary library2 = TestUtils.createMockLibrary(
                ""
                        + "int layout foo 0x7f030001\n"
                        + "int layout bar 0x7f060000\n",
                ""
                        + "layout foo\n",
                Collections.singletonList(library1)
        );

        List<AndroidLibrary> androidLibraries = Arrays.asList(library1, library2);
        ResourceVisibilityLookup visibility = ResourceVisibilityLookup
                .create(androidLibraries, null);
        assertTrue(visibility.isPrivate(ResourceType.DIMEN, "activity_horizontal_margin"));
        assertTrue(visibility.isPrivate(ResourceType.ID, "action_settings"));
        assertTrue(visibility.isPrivate(ResourceType.LAYOUT, "activity_main"));
        assertTrue(visibility.isPrivate(ResourceType.DIMEN, "activity_vertical_margin"));
        assertFalse(visibility.isPrivate(ResourceType.LAYOUT, "foo"));
        assertTrue(visibility.isPrivate(ResourceType.LAYOUT, "bar"));

        assertFalse(visibility.isPrivate(ResourceType.DIMEN, "unknown")); // not in this library
    }

    public void testManager() throws IOException {
        AndroidLibrary library = TestUtils.createMockLibrary(
                ""
                        + "int dimen activity_horizontal_margin 0x7f030000\n"
                        + "int dimen activity_vertical_margin 0x7f030001\n"
                        + "int id action_settings 0x7f060000\n"
                        + "int layout activity_main 0x7f020000\n"
                        + "int menu menu_main 0x7f050000\n"
                        + "int string action_settings 0x7f040000\n"
                        + "int string app_name 0x7f040001\n"
                        + "int string hello_world 0x7f040002",
                ""
        );
        ResourceVisibilityLookup.Provider provider = new ResourceVisibilityLookup.Provider();
        assertSame(provider.get(library), provider.get(library));
        assertTrue(provider.get(library).isPrivate(ResourceType.DIMEN,
                "activity_horizontal_margin"));

        AndroidArtifact artifact = TestUtils.createMockArtifact(Collections.singletonList(library));
        assertSame(provider.get(artifact), provider.get(artifact));
        assertTrue(provider.get(artifact).isPrivate(ResourceType.DIMEN,
                "activity_horizontal_margin"));
    }
}