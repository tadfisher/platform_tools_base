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

package com.android.builder.testing.api;

import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the {@link DeviceConfig}
 */
public class DeviceConfigTest extends TestCase {

    public void testParsing() {
        DeviceConfig config = new DeviceConfig(ImmutableList.of("config: foo", "abi: bar"));
        assertEquals("foo:bar", config.getConfigForAllAbis());

        config = new DeviceConfig(ImmutableList.of("config: foo"));
        assertEquals("foo", config.getConfigForAllAbis());

        config = new DeviceConfig(ImmutableList.of("abi: bar"));
        assertEquals("bar", config.getConfigForAllAbis());

        config = new DeviceConfig(ImmutableList.of("config: foo", "abi: bar", "noise: blah", ""));
        assertEquals("foo:bar", config.getConfigForAllAbis());

        config = new DeviceConfig(ImmutableList.of("config: foo", "abi: bar,zar"));
        assertEquals("bar,zar", config.getValue(DeviceConfig.Catetory.ABI).get());

        assertEquals(2, config.getAbis().size());
        List<String> expected = new ArrayList<String>();
        expected.add("foo:bar");
        expected.add("foo:zar");

        for (String abi : config.getAbis()) {
            assertTrue(expected.remove(config.getConfigFor(abi)));
        }
        assertTrue(expected.isEmpty());
    }

}
