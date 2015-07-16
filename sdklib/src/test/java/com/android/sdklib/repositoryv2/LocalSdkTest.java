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

package com.android.sdklib.repositoryv2;

import com.android.SdkConstants;
import com.android.sdklib.io.MockFileOp;
import com.android.sdklib.repository.PreciseRevision;
import com.android.sdklib.repository.local.LocalPkgInfo;
import com.android.sdklib.repository.local.LocalSdk;
import com.android.sdklib.repositorycore.api.LocalPackage;
import com.android.sdklib.repositorycore.api.SdkManager;
import com.android.sdklib.repositorycore.api.SdkSchemaExtension;
import com.android.sdklib.repositorycore.impl.generated.v1.RepositoryType;
import com.android.sdklib.repositorycore.impl.local.LocalPackageImpl;
import com.android.sdklib.repositorycore.impl.local.LocalSdkImpl;
import com.android.sdklib.repositorycore.impl.manager.SdkManagerImpl;
import com.android.sdklib.repositoryv2.generated.repository.v1.ToolDetailsType;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Created by jbakermalone on 7/31/15.
 */
public class LocalSdkTest extends TestCase {
    public void testParseLegacy() throws URISyntaxException, FileNotFoundException {
        MockFileOp mockFop = new MockFileOp();
        mockFop.recordExistingFolder("/sdk/tools");
        mockFop.recordExistingFile("/sdk/tools/source.properties",
                "Pkg.License=Terms and Conditions\n" +
                        "Archive.Os=WINDOWS\n" +
                        "Pkg.Revision=22.3.4\n" +
                        "Platform.MinPlatformToolsRev=18\n" +
                        "Pkg.LicenseRef=android-sdk-license\n" +
                        "Archive.Arch=ANY\n" +
                        "Pkg.SourceUrl=https\\://example.com/repository-8.xml");
        mockFop.recordExistingFile("/sdk/tools/" + SdkConstants.androidCmdName(), "placeholder");
        mockFop.recordExistingFile("/sdk/tools/" + SdkConstants.FN_EMULATOR, "placeholder");

        File root = new File("/sdk");
        SdkManager mgr = SdkManagerImpl.getInstance(null, null, null, null);

        LocalSdkImpl sdk = new LocalSdkImpl(root, mgr, new LegacyLocalSdk(root, mockFop), mockFop);
        Map<String, LocalPackage> packages = sdk.getPackages();
        assertEquals(1, packages.size());
        LocalPackage local = packages.get("tools");
        assertTrue(local.getTypeDetails() instanceof ToolDetailsType);
        assertEquals("Terms and Conditions", local.getLicense().getLicense());
        assertEquals(new PreciseRevision(22, 3, 4, 0), local.getRevision());
    }

    public void testRewriteLegacy() throws URISyntaxException, FileNotFoundException {
        MockFileOp mockFop = new MockFileOp();
        mockFop.recordExistingFolder("/sdk/tools");
        mockFop.recordExistingFile("/sdk/tools/source.properties",
                "Pkg.License=Terms and Conditions\n" +
                        "Archive.Os=WINDOWS\n" +
                        "Pkg.Revision=22.3.4\n" +
                        "Platform.MinPlatformToolsRev=18\n" +
                        "Pkg.LicenseRef=android-sdk-license\n" +
                        "Archive.Arch=ANY\n" +
                        "Pkg.SourceUrl=https\\://example.com/repository-8.xml");
        mockFop.recordExistingFile("/sdk/tools/" + SdkConstants.androidCmdName(), "placeholder");
        mockFop.recordExistingFile("/sdk/tools/" + SdkConstants.FN_EMULATOR, "placeholder");

        File root = new File("/sdk");
        SdkManager mgr = SdkManagerImpl.getInstance(null, null, null, null);

        Class repoOf = com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory.class;
        Class addonOf = com.android.sdklib.repositoryv2.generated.addon.v1.ObjectFactory.class;
        Class sysimgOf = com.android.sdklib.repositoryv2.generated.sysimg.v1.ObjectFactory.class;
        SdkSchemaExtension ex1 = new SdkSchemaExtension(addonOf, new File(
                AndroidLocalSdkHandler.class.getResource("sdk-addon-08.xsd").toURI()));
        SdkSchemaExtension ex2 = new SdkSchemaExtension(repoOf, new File(
                AndroidLocalSdkHandler.class.getResource("sdk-repository-12.xsd").toURI()));
        SdkSchemaExtension ex3 = new SdkSchemaExtension(sysimgOf, new File(
                AndroidLocalSdkHandler.class.getResource("sdk-sys-img-04.xsd").toURI()));

        mgr.registerSchemaExtension(ex1);
        mgr.registerSchemaExtension(ex2);
        mgr.registerSchemaExtension(ex3);

        LocalSdkImpl sdk = new LocalSdkImpl(root, mgr, new LegacyLocalSdk(root, mockFop), mockFop);
        // Cause the packages to be loaded. This will write out package.xml for the legacy package.
        sdk.getPackages();

        // Now read the new package
        RepositoryType repo = SdkSchemaExtension.unmarshal(
                mockFop.newFileInputStream(new File("/sdk/tools/package.xml")),
                ImmutableList.of(ex2));
        assertEquals(1, repo.getPackage().size());
        LocalPackageImpl local = new LocalPackageImpl(repo.getPackage().get(0));
        assertTrue(local.getTypeDetails() instanceof ToolDetailsType);
        assertEquals("Terms and Conditions", local.getLicense().getLicense());
        assertEquals(new PreciseRevision(22, 3, 4, 0), local.getRevision());

    }

}
