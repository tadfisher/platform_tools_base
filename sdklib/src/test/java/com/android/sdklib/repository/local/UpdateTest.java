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

package com.android.sdklib.repository.local;

import com.android.SdkConstants;
import com.android.sdklib.internal.repository.IDescription;
import com.android.sdklib.io.MockFileOp;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.descriptors.IPkgDesc;
import com.android.sdklib.repository.descriptors.PkgDesc;
import com.android.sdklib.repository.descriptors.PkgType;
import com.android.sdklib.repository.local.Update.Result;
import com.android.sdklib.repository.remote.RemotePkgInfo;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import java.io.File;

import junit.framework.TestCase;

public class UpdateTest extends TestCase {

    private MockFileOp mFOp;
    private LocalSdk mLS;
    private Multimap<PkgType, RemotePkgInfo> mRemotePkgs;
    private IDescription mSource;

    @Override
    protected void setUp() {
        mFOp = new MockFileOp();
        mLS = new LocalSdk(mFOp);
        mRemotePkgs = TreeMultimap.create();
        mSource = new IDescription() {
            @Override
            public String getShortDescription() {
                return "source";
            }

            @Override
            public String getLongDescription() {
                return "mock sdk repository source";
            }
        };

        mLS.setLocation(new File("/sdk"));
    }

    public final void testComputeUpdates() {
        addLocalTools("22.3.4", "18");
        addRemoteTools(new FullRevision(23), new FullRevision(19));

        addLocalPlatformTools("1.0.0");
        addRemotePlatformTools(new FullRevision(1, 0, 1));

        Result result = Update.computeUpdates(
                mLS.getPkgsInfos(PkgType.PKG_ALL),
                mRemotePkgs);

        assertNotNull(result);
        assertEquals(
                "[<LocalPlatformToolPkgInfo <PkgDesc Type=platform_tools FullRev=1.0.0> " +
                               "Updated by: <RemotePkgInfo Source:source <PkgDesc Type=platform_tools FullRev=1.0.1>>>, " +
               "<LocalToolPkgInfo <PkgDesc Type=tools FullRev=22.3.4 MinPlatToolsRev=18.0.0> " +
                     "Updated by: <RemotePkgInfo Source:source <PkgDesc Type=tools FullRev=23.0.0 MinPlatToolsRev=19.0.0>>>]",
                result.getUpdatedPkgs().toString());
        result.getNewPkgs();
    }

    //---

    private void addLocalTools(String fullRev, String minPlatToolsRev) {
        mFOp.recordExistingFolder("/sdk/tools");
        mFOp.recordExistingFile("/sdk/tools/source.properties",
                "Pkg.License=Terms and Conditions\n" +
                "Archive.Os=WINDOWS\n" +
                "Pkg.Revision=" + fullRev + "\n" +
                "Platform.MinPlatformToolsRev=" + minPlatToolsRev + "\n" +
                "Pkg.LicenseRef=android-sdk-license\n" +
                "Archive.Arch=ANY\n" +
                "Pkg.SourceUrl=https\\://example.com/repository-8.xml");
        mFOp.recordExistingFile("/sdk/tools/" + SdkConstants.androidCmdName(), "placeholder");
        mFOp.recordExistingFile("/sdk/tools/" + SdkConstants.FN_EMULATOR, "placeholder");
    }

    private void addLocalPlatformTools(String fullRev) {
        mFOp.recordExistingFolder("/sdk/platform-tools");
        mFOp.recordExistingFile("/sdk/platform-tools/source.properties",
                "Pkg.License=Terms and Conditions\n" +
                "Archive.Os=WINDOWS\n" +
                "Pkg.Revision=" + fullRev + "\n" +
                "Pkg.LicenseRef=android-sdk-license\n" +
                "Archive.Arch=ANY\n" +
                "Pkg.SourceUrl=https\\://example.com/repository-8.xml");
    }

    //---

    private void addRemoteTools(FullRevision revision, FullRevision minPlatformToolsRev) {
        IPkgDesc d = PkgDesc.newTool(revision, minPlatformToolsRev);
        RemotePkgInfo r = new RemotePkgInfo(d, mSource);
        mRemotePkgs.put(d.getType(), r);
    }

    private void addRemotePlatformTools(FullRevision revision) {
        IPkgDesc d = PkgDesc.newPlatformTool(revision);
        RemotePkgInfo r = new RemotePkgInfo(d, mSource);
        mRemotePkgs.put(d.getType(), r);
    }

}
