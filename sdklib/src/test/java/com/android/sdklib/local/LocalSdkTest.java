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

package com.android.sdklib.local;

import com.android.SdkConstants;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.io.MockFileOp;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.MajorRevision;

import java.io.File;

import junit.framework.TestCase;

public class LocalSdkTest extends TestCase {

    public final void testLocalSdkTest_getLocation() {
        MockFileOp fop = new MockFileOp();
        LocalSdk ls = new LocalSdk(fop);
        assertNull(ls.getLocation());
        ls.setLocation(new File("/sdk"));
        assertEquals(new File("/sdk"), ls.getLocation());
    }

    public final void testLocalSdkTest_getPkgInfo_Tools() {
        MockFileOp fop = new MockFileOp();
        LocalSdk ls = new LocalSdk(fop);
        ls.setLocation(new File("/sdk"));

        // check empty
        assertNull(ls.getPkgInfo(LocalSdk.PKG_TOOLS));

        // setup fake tools
        ls.clearLocalPkg(LocalSdk.PKG_ALL);
        fop.recordExistingFolder("/sdk/tools");
        fop.recordExistingFile("/sdk/tools/source.properties",
                "Pkg.License=Terms and Conditions\n" +
                "Archive.Os=WINDOWS\n" +
                "Pkg.Revision=22.3.4\n" +
                "Platform.MinPlatformToolsRev=18\n" +
                "Pkg.LicenseRef=android-sdk-license\n" +
                "Archive.Arch=ANY\n" +
                "Pkg.SourceUrl=https\\://dl-ssl.google.com/android/repository/repository-8.xml");
        fop.recordExistingFile("/sdk/tools/" + SdkConstants.androidCmdName(), "placeholder");
        fop.recordExistingFile("/sdk/tools/" + SdkConstants.FN_EMULATOR, "placeholder");

        LocalPkgInfo pi = ls.getPkgInfo(LocalSdk.PKG_TOOLS);
        assertNotNull(pi);
        assertTrue(pi instanceof LocalToolPkgInfo);
        assertEquals(new File("/sdk/tools"), pi.getLocalDir());
        assertSame(ls, pi.getLocalSdk());
        assertEquals(null, pi.getLoadError());
        assertEquals(new FullRevision(22, 3, 4), pi.getFullRevision());

        Package pkg = pi.getPackage();
        assertNotNull(pkg);
    }

    public final void testLocalSdkTest_getPkgInfo_PlatformTools() {
        MockFileOp fop = new MockFileOp();
        LocalSdk ls = new LocalSdk(fop);
        ls.setLocation(new File("/sdk"));

        // check empty
        assertNull(ls.getPkgInfo(LocalSdk.PKG_PLATFORM_TOOLS));

        // setup fake tools
        ls.clearLocalPkg(LocalSdk.PKG_ALL);
        fop.recordExistingFolder("/sdk/platform-tools");
        fop.recordExistingFile("/sdk/platform-tools/source.properties",
                "Pkg.License=Terms and Conditions\n" +
                "Archive.Os=WINDOWS\n" +
                "Pkg.Revision=18.19.20\n" +
                "Pkg.LicenseRef=android-sdk-license\n" +
                "Archive.Arch=ANY\n" +
                "Pkg.SourceUrl=https\\://dl-ssl.google.com/android/repository/repository-8.xml");

        LocalPkgInfo pi = ls.getPkgInfo(LocalSdk.PKG_PLATFORM_TOOLS);
        assertNotNull(pi);
        assertTrue(pi instanceof LocalPlatformToolPkgInfo);
        assertEquals(new File("/sdk/platform-tools"), pi.getLocalDir());
        assertSame(ls, pi.getLocalSdk());
        assertEquals(null, pi.getLoadError());
        assertEquals(new FullRevision(18, 19, 20), pi.getFullRevision());

        Package pkg = pi.getPackage();
        assertNotNull(pkg);
    }

    public final void testLocalSdkTest_getPkgInfo_Docs() {
        MockFileOp fop = new MockFileOp();
        LocalSdk ls = new LocalSdk(fop);
        ls.setLocation(new File("/sdk"));

        // check empty
        assertNull(ls.getPkgInfo(LocalSdk.PKG_DOCS));

        // setup fake tools
        ls.clearLocalPkg(LocalSdk.PKG_ALL);
        fop.recordExistingFolder("/sdk/docs");
        fop.recordExistingFile("/sdk/docs/source.properties",
                "Pkg.License=Terms and Conditions\n" +
                "Archive.Os=ANY\n" +
                "AndroidVersion.ApiLevel=18\n" +
                "Pkg.Revision=2\n" +
                "Pkg.LicenseRef=android-sdk-license\n" +
                "Archive.Arch=ANY\n" +
                "Pkg.SourceUrl=https\\://dl-ssl.google.com/android/repository/repository-8.xml");
        fop.recordExistingFile("/sdk/docs/index.html", "placeholder");

        LocalPkgInfo pi = ls.getPkgInfo(LocalSdk.PKG_DOCS);
        assertNotNull(pi);
        assertTrue(pi instanceof LocalDocPkgInfo);
        assertEquals(new File("/sdk/docs"), pi.getLocalDir());
        assertSame(ls, pi.getLocalSdk());
        assertEquals(null, pi.getLoadError());
        assertEquals(new MajorRevision(2), pi.getMajorRevision());

        Package pkg = pi.getPackage();
        assertNotNull(pkg);
    }

}
