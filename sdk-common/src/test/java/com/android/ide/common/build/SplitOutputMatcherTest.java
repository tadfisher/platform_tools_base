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

package com.android.ide.common.build;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.FilterData;
import com.android.build.OutputFile;
import com.android.resources.Density;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SplitOutputMatcherTest extends TestCase {

    /**
     * Helper to run InstallHelper.computeMatchingOutput with variable ABI list.
     */
    private static OutputFile computeBestOutput(
            @NonNull List<? extends OutputFile> outputs,
            int density,
            @NonNull String... abis) {
        return SplitOutputMatcher.computeBestOutput(
                outputs, null, density, Arrays.asList(abis));
    }

    private static OutputFile computeBestOutput(
            @NonNull List<? extends OutputFile> outputs,
            @NonNull Set<String> variantAbis,
            int density,
            @NonNull String... abis) {
        return SplitOutputMatcher.computeBestOutput(
                outputs, variantAbis, density, Arrays.asList(abis));
    }

    /**
     * Fake implementation of FilteredOutput
     */

    private static final class FakeSplitOutput implements OutputFile {

        private final String densityFilter;
        private final String abiFilter;
        private final int versionCode;

        FakeSplitOutput(String densityFilter, String abiFilter, int versionCode) {
            this.densityFilter = densityFilter;
            this.abiFilter = abiFilter;
            this.versionCode = versionCode;
        }

        @Override
        public String getOutputType() {
            return OutputFile.FULL_SPLIT;
        }

        @NonNull
        @Override
        public Collection<String> getFilterTypes() {
            ImmutableList.Builder<String> splitTypeBuilder = ImmutableList.builder();
            if (densityFilter != null) {
                splitTypeBuilder.add(OutputFile.DENSITY);
            }
            if (abiFilter != null) {
                splitTypeBuilder.add(OutputFile.ABI);
            }
            return splitTypeBuilder.build();
        }

        @Nullable
        @Override
        public String getFilter(String filterType) {
            if (densityFilter != null && filterType.equals(OutputFile.DENSITY)) {
                return densityFilter;
            } else if (abiFilter != null && filterType.equals(OutputFile.ABI)) {
                return abiFilter;
            }
            return null;
        }

        @NonNull
        @Override
        public Collection<FilterData> getFilters() {
            ImmutableList.Builder<FilterData> filters = ImmutableList.builder();
            if (densityFilter != null) {
                filters.add(FilterData.Builder.build(OutputFile.DENSITY, densityFilter));
            }
            if (abiFilter != null) {
                filters.add(FilterData.Builder.build(OutputFile.ABI, abiFilter));
            }
            return filters.build();
        }

        @NonNull
        @Override
        public File getOutputFile() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getVersionCode() {
            return versionCode;
        }

        @Override
        public String toString() {
            return "FilteredOutput{" + densityFilter + ':' + abiFilter + ':' + versionCode + '}';
        }
    }

    public void testSingleOutput() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(1));

        OutputFile result = computeBestOutput(list, 160, "foo");

        assertEquals(match, result);
    }

    public void testDensityOnlyWithMatch() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        list.add(getUniversalOutput(1));
        list.add(match = getDensityOutput(160, 2));
        list.add(getDensityOutput(320, 3));

        OutputFile result = computeBestOutput(list, 160, "foo");

        assertEquals(match, result);
    }

    public void testDensityOnlyWithUniversalMatch() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(3));
        list.add(getDensityOutput(320, 2));
        list.add(getDensityOutput(480, 1));

        OutputFile result = computeBestOutput(list, 160, "foo");

        assertEquals(match, result);
    }

    public void testDensityOnlyWithNoMatch() {
        List<OutputFile> list = Lists.newArrayList();

        list.add(getDensityOutput(320, 1));
        list.add(getDensityOutput(480, 2));

        OutputFile result = computeBestOutput(list, 160, "foo");

        assertNull(result);
    }

    public void testDensityOnlyWithCustomDeviceDensity() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(1));
        list.add(getDensityOutput(320, 2));
        list.add(getDensityOutput(480, 3));

        OutputFile result = computeBestOutput(list, 1, "foo");

        assertEquals(match, result);
    }


    public void testAbiOnlyWithMatch() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        list.add(getUniversalOutput(1));
        list.add(match = getAbiOutput("foo", 2));
        list.add(getAbiOutput("bar", 3));

        OutputFile result = computeBestOutput(list, 160, "foo");

        assertEquals(match, result);
    }

    public void testAbiOnlyWithMultiMatch() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        // test where the versionCode match the abi order
        list.add(getUniversalOutput(1));
        list.add(getAbiOutput("foo", 2));
        list.add(match = getAbiOutput("bar", 3));

        // bar is preferred over foo
        OutputFile result = computeBestOutput(list, 160, "bar", "foo");

        assertEquals(match, result);
    }

    public void testAbiOnlyWithMultiMatch2() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        // test where the versionCode does not match the abi order
        list.add(getUniversalOutput(1));
        list.add(getAbiOutput("foo", 2));
        list.add(match = getAbiOutput("bar", 3));

        // bar is preferred over foo
        OutputFile result = computeBestOutput(list, 160, "foo", "bar");

        assertEquals(match, result);
    }

    public void testAbiOnlyWithUniversalMatch() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(1));
        list.add(getAbiOutput("foo", 2));
        list.add(getAbiOutput("bar", 3));

        OutputFile result = computeBestOutput(list, 160, "zzz");

        assertEquals(match, result);
    }

    public void testAbiOnlyWithNoMatch() {
        List<OutputFile> list = Lists.newArrayList();

        list.add(getAbiOutput("foo", 1));
        list.add(getAbiOutput("bar", 2));

        OutputFile result = computeBestOutput(list, 160, "zzz");

        assertNull(result);
    }

    public void testMultiFilterWithMatch() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        list.add(getUniversalOutput(1));
        list.add(getOutput(160, "zzz",2));
        list.add(match = getOutput(160, "foo", 4));
        list.add(getOutput(320, "foo", 3));

        OutputFile result = computeBestOutput(list, 160, "foo");

        assertEquals(match, result);
    }

    public void testMultiFilterWithUniversalMatch() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(4));
        list.add(getOutput(320, "zzz", 3));
        list.add(getOutput(160, "bar", 2));
        list.add(getOutput(320, "foo", 1));

        OutputFile result = computeBestOutput(list, 160, "zzz");

        assertEquals(match, result);
    }

    public void testMultiFilterWithNoMatch() {
        List<OutputFile> list = Lists.newArrayList();

        list.add(getOutput(320, "zzz", 1));
        list.add(getOutput(160, "bar", 2));
        list.add(getOutput(320, "foo", 3));

        OutputFile result = computeBestOutput(list, 160, "zzz");

        assertNull(result);
    }

    public void testVariantLevelAbiFilter() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        list.add(match = getUniversalOutput(1));
        OutputFile result = computeBestOutput(list, Sets.newHashSet("bar", "foo"), 160, "foo", "zzz");

        assertEquals(match, result);
    }

    public void testWrongVariantLevelAbiFilter() {
        List<OutputFile> list = Lists.newArrayList();

        list.add(getUniversalOutput(1));

        OutputFile result = computeBestOutput(list, Sets.newHashSet("bar", "foo"), 160, "zzz");

        assertNull(result);
    }

    public void testDensitySplitPlugVariantLevelAbiFilter() {
        OutputFile match;
        List<OutputFile> list = Lists.newArrayList();

        list.add(getUniversalOutput(1));
        list.add(getDensityOutput(240, 2));
        list.add(match = getDensityOutput(320, 3));
        list.add(getDensityOutput(480, 4));

        OutputFile result = computeBestOutput(list, Sets.newHashSet("bar", "foo"), 320, "foo", "zzz");

        assertEquals(match, result);
    }

    private static OutputFile getUniversalOutput(int versionCode) {
        return new FakeSplitOutput(null, null, versionCode);
    }

    private static OutputFile getDensityOutput(int densityFilter, int versionCode) {
        Density densityEnum = Density.getEnum(densityFilter);
        return new FakeSplitOutput(densityEnum.getResourceValue(), null, versionCode);
    }

    private static OutputFile getAbiOutput(String filter, int versionCode) {
        return new FakeSplitOutput( null, filter, versionCode);
    }

    private static OutputFile getOutput(int densityFilter, String abiFilter, int versionCode) {
        Density densityEnum = Density.getEnum(densityFilter);
        return new FakeSplitOutput(densityEnum.getResourceValue(), abiFilter, versionCode);
    }
}
