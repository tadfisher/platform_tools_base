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

package com.android.build.gradle.internal;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.internal.variant.FilteredOutput;
import com.android.resources.Density;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.io.File;
import java.util.List;

public class InstallHelperTest extends TestCase {

    private static final class MockFilteredOutput implements FilteredOutput {

        private final String densityFilter;
        private final String abiFilter;

        MockFilteredOutput(String densityFilter, String abiFilter) {
            this.densityFilter = densityFilter;
            this.abiFilter = abiFilter;
        }

        @Nullable
        @Override
        public String getDensityFilter() {
            return densityFilter;
        }

        @Nullable
        @Override
        public String getAbiFilter() {
            return abiFilter;
        }

        @NonNull
        @Override
        public File getOutputFile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "FilteredOutput{" + densityFilter + ':' + abiFilter + '}';
        }
    }

    public void testSingleOutput() {
        FilteredOutput match;
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(match = getUniversalOutput());

        FilteredOutput result = InstallHelper.getOutput(list, 160, "foo");

        assertEquals(match, result);
    }

    public void testDensityOnlyWithMatch() {
        FilteredOutput match;
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(getUniversalOutput());
        list.add(match = getDensityOutput(160));
        list.add(getDensityOutput(320));

        FilteredOutput result = InstallHelper.getOutput(list, 160, "foo");

        assertEquals(match, result);
    }

    public void testDensityOnlyWithUniversalMatch() {
        FilteredOutput match;
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(match = getUniversalOutput());
        list.add(getDensityOutput(320));
        list.add(getDensityOutput(480));

        FilteredOutput result = InstallHelper.getOutput(list, 160, "foo");

        assertEquals(match, result);
    }

    public void testDensityOnlyWithNoMatch() {
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(getDensityOutput(320));
        list.add(getDensityOutput(480));

        FilteredOutput result = InstallHelper.getOutput(list, 160, "foo");

        assertNull(result);
    }

    public void testAbiOnlyWithMatch() {
        FilteredOutput match;
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(getUniversalOutput());
        list.add(match = getAbiOutput("foo"));
        list.add(getAbiOutput("bar"));

        FilteredOutput result = InstallHelper.getOutput(list, 160, "foo");

        assertEquals(match, result);
    }

    public void testAbiOnlyWithMultiMatch() {
        FilteredOutput match;
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(getUniversalOutput());
        list.add(getAbiOutput("foo"));
        list.add(match = getAbiOutput("bar"));

        // bar is preferred over foo
        FilteredOutput result = InstallHelper.getOutput(list, 160, "bar", "foo");

        assertEquals(match, result);
    }

    public void testAbiOnlyWithUniversalMatch() {
        FilteredOutput match;
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(match = getUniversalOutput());
        list.add(getAbiOutput("foo"));
        list.add(getAbiOutput("bar"));

        FilteredOutput result = InstallHelper.getOutput(list, 160, "zzz");

        assertEquals(match, result);
    }

    public void testAbiOnlyWithNoMatch() {
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(getAbiOutput("foo"));
        list.add(getAbiOutput("bar"));

        FilteredOutput result = InstallHelper.getOutput(list, 160, "zzz");

        assertNull(result);
    }

    public void testMultiFilterWithMatch() {
        FilteredOutput match;
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(getUniversalOutput());
        list.add(getOutput(160, "zzz"));
        list.add(match = getOutput(160, "foo"));
        list.add(getOutput(320, "foo"));

        FilteredOutput result = InstallHelper.getOutput(list, 160, "foo");

        assertEquals(match, result);
    }

    public void testMultiFilterWithUniversalMatch() {
        FilteredOutput match;
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(match = getUniversalOutput());
        list.add(getOutput(320, "zzz"));
        list.add(getOutput(160, "bar"));
        list.add(getOutput(320, "foo"));

        FilteredOutput result = InstallHelper.getOutput(list, 160, "zzz");

        assertEquals(match, result);
    }

    public void testMultiFilterWithNoMatch() {
        List<FilteredOutput> list = Lists.newArrayList();

        list.add(getOutput(320, "zzz"));
        list.add(getOutput(160, "bar"));
        list.add(getOutput(320, "foo"));

        FilteredOutput result = InstallHelper.getOutput(list, 160, "zzz");

        assertNull(result);
    }

    private static FilteredOutput getUniversalOutput() {
        return new MockFilteredOutput(null, null);
    }

    private static FilteredOutput getDensityOutput(int densityFilter) {
        Density densityEnum = Density.getEnum(densityFilter);
        return new MockFilteredOutput(densityEnum.getResourceValue(), null);
    }

    private static FilteredOutput getAbiOutput(String filter) {
        return new MockFilteredOutput( null, filter);
    }

    private static FilteredOutput getOutput(int densityFilter, String abiFilter) {
        Density densityEnum = Density.getEnum(densityFilter);
        return new MockFilteredOutput(densityEnum.getResourceValue(), abiFilter);
    }
}
