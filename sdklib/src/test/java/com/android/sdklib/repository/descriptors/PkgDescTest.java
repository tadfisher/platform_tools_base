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

package com.android.sdklib.repository.descriptors;

import com.android.annotations.NonNull;
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.AndroidVersion.AndroidVersionException;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.MajorRevision;
import com.android.sdklib.repository.NoPreviewRevision;

import junit.framework.TestCase;

public class PkgDescTest extends TestCase {

    public final void testPkgDescTool() {
        PkgDescTool p = new PkgDescTool(new FullRevision(1, 2, 3, 4));

        assertEquals(PkgType.PKG_TOOLS, p.getType());

        assertTrue(p.hasFullRevision());
        assertEquals(new FullRevision(1, 2, 3, 4), p.getFullRevision());

        assertFalse(p.hasMajorRevision());
        assertNull(p.getMajorRevision());

        assertFalse(p.hasAndroidVersion());
        assertNull(p.getAndroidVersion());

        assertFalse(p.hasPath());
        assertNull(p.getPath());

        assertEquals("<PkgDescTool FullRev=1.2.3 rc4>", p.toString());
    }

    public final void testPkgDescPlatformTool() {
        PkgDescPlatformTool p = new PkgDescPlatformTool(new FullRevision(1, 2, 3, 4));

        assertEquals(PkgType.PKG_PLATFORM_TOOLS, p.getType());

        assertTrue(p.hasFullRevision());
        assertEquals(new FullRevision(1, 2, 3, 4), p.getFullRevision());

        assertFalse(p.hasMajorRevision());
        assertNull(p.getMajorRevision());

        assertFalse(p.hasAndroidVersion());
        assertNull(p.getAndroidVersion());

        assertFalse(p.hasPath());
        assertNull(p.getPath());

        assertEquals("<PkgDescPlatformTool FullRev=1.2.3 rc4>", p.toString());

    }

    public final void testPkgDescDoc() {
        PkgDescDoc p = new PkgDescDoc(new MajorRevision(1));

        assertEquals(PkgType.PKG_DOCS, p.getType());

        assertFalse(p.hasFullRevision());
        assertNull(p.getFullRevision());

        assertTrue(p.hasMajorRevision());
        assertEquals(new MajorRevision(1), p.getMajorRevision());

        assertFalse(p.hasAndroidVersion());
        assertNull(p.getAndroidVersion());

        assertFalse(p.hasPath());
        assertNull(p.getPath());

        assertEquals("<PkgDescDoc MajorRev=1>", p.toString());
    }

    public final void testPkgDescBuildTool() {
        PkgDescBuildTool p = new PkgDescBuildTool(new FullRevision(1, 2, 3, 4));

        assertEquals(PkgType.PKG_BUILD_TOOLS, p.getType());

        assertTrue(p.hasFullRevision());
        assertEquals(new FullRevision(1, 2, 3, 4), p.getFullRevision());

        assertFalse(p.hasMajorRevision());
        assertNull(p.getMajorRevision());

        assertFalse(p.hasAndroidVersion());
        assertNull(p.getAndroidVersion());

        assertFalse(p.hasPath());
        assertNull(p.getPath());

        assertEquals("<PkgDescBuildTool FullRev=1.2.3 rc4>", p.toString());
    }

    public final void testPkgDescExtra() {
        PkgDescExtra p = new PkgDescExtra("vendor", "extra_path", new NoPreviewRevision(1, 2, 3));

        assertEquals(PkgType.PKG_EXTRAS, p.getType());

        assertTrue(p.hasFullRevision());
        assertEquals(new FullRevision(1, 2, 3), p.getFullRevision());

        assertFalse(p.hasMajorRevision());
        assertNull(p.getMajorRevision());

        assertFalse(p.hasAndroidVersion());
        assertNull(p.getAndroidVersion());

        assertTrue(p.hasPath());
        assertEquals("vendor/extra_path", p.getPath());

        assertEquals("<PkgDescExtra Path=vendor/extra_path FullRev=1.2.3>", p.toString());
    }

    public final void testPkgDescSource() throws Exception {
        PkgDescSource p = new PkgDescSource(new AndroidVersion("19"), new MajorRevision(1));

        assertEquals(PkgType.PKG_SOURCES, p.getType());

        assertFalse(p.hasFullRevision());
        assertNull(p.getFullRevision());

        assertTrue(p.hasMajorRevision());
        assertEquals(new MajorRevision(1), p.getMajorRevision());

        assertTrue(p.hasAndroidVersion());
        assertEquals(new AndroidVersion("19"), p.getAndroidVersion());

        assertFalse(p.hasPath());
        assertNull(p.getPath());

        assertEquals("<PkgDescSource Android=API 19 MajorRev=1>", p.toString());
    }

    public final void testPkgDescSample() throws Exception {
        PkgDescSample p = new PkgDescSample(new AndroidVersion("19"), new MajorRevision(1));

        assertEquals(PkgType.PKG_SAMPLES, p.getType());

        assertFalse(p.hasFullRevision());
        assertNull(p.getFullRevision());

        assertTrue(p.hasMajorRevision());
        assertEquals(new MajorRevision(1), p.getMajorRevision());

        assertTrue(p.hasAndroidVersion());
        assertEquals(new AndroidVersion("19"), p.getAndroidVersion());

        assertFalse(p.hasPath());
        assertNull(p.getPath());

        assertEquals("<PkgDescSample Android=API 19 MajorRev=1>", p.toString());
    }

    public final void testPkgDescPlatform() throws Exception {
        PkgDescPlatform p = new PkgDescPlatform(new AndroidVersion("19"), new MajorRevision(1));

        assertEquals(PkgType.PKG_PLATFORMS, p.getType());

        assertFalse(p.hasFullRevision());
        assertNull(p.getFullRevision());

        assertTrue(p.hasMajorRevision());
        assertEquals(new MajorRevision(1), p.getMajorRevision());

        assertTrue(p.hasAndroidVersion());
        assertEquals(new AndroidVersion("19"), p.getAndroidVersion());

        assertTrue(p.hasPath());
        assertEquals("android-19", p.getPath());

        assertEquals("<PkgDescPlatform Android=API 19 Path=android-19 MajorRev=1>", p.toString());
    }

    public final void testPkgDescAddon() throws Exception {
        PkgDescAddon p1 = new PkgDescAddon(new AndroidVersion("19"), new MajorRevision(1),
                                             "vendor", "addon_name");

        assertEquals(PkgType.PKG_ADDONS, p1.getType());

        assertFalse(p1.hasFullRevision());
        assertNull(p1.getFullRevision());

        assertTrue(p1.hasMajorRevision());
        assertEquals(new MajorRevision(1), p1.getMajorRevision());

        assertTrue(p1.hasAndroidVersion());
        assertEquals(new AndroidVersion("19"), p1.getAndroidVersion());

        assertTrue(p1.hasPath());
        assertEquals("vendor:addon_name:19", p1.getPath());

        assertEquals("<PkgDescAddon Android=API 19 Path=vendor:addon_name:19 MajorRev=1>",
                     p1.toString());

        // This should generate an error because the add-on has string isn't determined
        PkgDescAddon p2 = new PkgDescAddon(new AndroidVersion("2"), new MajorRevision(4));
        try {
            assertNull(p2.getPath());
            fail("PkgDescAddon.getPath should generate an IllegalArgumentException.");
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }

        // If the add-on hash string isn't determined in the constructor, the implementation
        // should override getPath to compute it lazily when needed.
        PkgDescAddon p3 = new PkgDescAddon(new AndroidVersion("3"), new MajorRevision(5)) {
            @NonNull
            @Override
            public String getPath() {
                try {
                    return AndroidTargetHash.getAddonHashString(
                            "vendor3",
                            "name3",
                            new AndroidVersion("3"));
                } catch (AndroidVersionException e) {
                    fail(); // should not happen, it would mean "3" wasn't parsed as a number
                    return null;
                }
            }
        };
        assertEquals("vendor3:name3:3", p3.getPath());
    }

    public final void testPkgDescSysImg() throws Exception {
        PkgDescSysImg p = new PkgDescSysImg(new AndroidVersion("19"), "eabi", new MajorRevision(1));

        assertEquals(PkgType.PKG_SYS_IMAGES, p.getType());

        assertFalse(p.hasFullRevision());
        assertNull(p.getFullRevision());

        assertTrue(p.hasMajorRevision());
        assertEquals(new MajorRevision(1), p.getMajorRevision());

        assertTrue(p.hasAndroidVersion());
        assertEquals(new AndroidVersion("19"), p.getAndroidVersion());

        assertTrue(p.hasPath());
        assertEquals("eabi", p.getPath());

        assertEquals("<PkgDescSysImg Android=API 19 Path=eabi MajorRev=1>", p.toString());
    }

}
