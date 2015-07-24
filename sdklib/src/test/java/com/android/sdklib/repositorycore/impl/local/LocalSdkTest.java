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

package com.android.sdklib.repositorycore.impl.local;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.sdklib.io.MockFileOp;
import com.android.sdklib.repository.PreciseRevision;
import com.android.sdklib.repositorycore.api.LocalPackage;
import com.android.sdklib.repositorycore.api.ProgressRunner;
import com.android.sdklib.repositorycore.api.SdkLoadedCallback;
import com.android.sdklib.repositorycore.api.SdkManager;
import com.android.sdklib.repositorycore.api.SdkSchemaExtension;
import com.android.sdklib.repositorycore.impl.generated.v1.ObjectFactory;
import com.android.sdklib.repositorycore.impl.remote.SdkSourceProvider;
import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jbakermalone on 7/31/15.
 */
public class LocalSdkTest extends TestCase {
    public void testParseGeneric() {
        MockFileOp mockFop = new MockFileOp();
        mockFop.recordExistingFolder("/sdk/random");
        mockFop.recordExistingFile("/sdk/random/package.xml",
                "<sdk:repository\n"
                        + "        xmlns:sdk=\"http://schemas.android.com/sdk/android/common/01\"\n"
                        + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                        + "\n"
                        + "    <license type=\"text\" id=\"license1\">\n"
                        + "        This is the license\n"
                        + "        for this platform.\n"
                        + "    </license>\n"
                        + "\n"
                        + "    <package path=\"random\">\n"
                        + "        <revision>\n"
                        + "            <major>3</major>\n"
                        + "        </revision>\n"
                        + "        <uiName>The first Android platform ever</uiName>\n"
                        + "        <type-details xsi:type=\"sdk:genericDetailsType\"/>\n"
                        + "        <uses-license ref=\"license1\"/>\n"
                        + "        <dependencies>\n"
                        + "            <dependency id=\"tools\">\n"
                        + "                <min-revision>\n"
                        + "                    <major>2</major>\n"
                        + "                    <micro>1</micro>\n"
                        + "                </min-revision>\n"
                        + "            </dependency>\n"
                        + "        </dependencies>\n"
                        + "    </package>\n"
                        + "</sdk:repository>"
                );
        mockFop.recordExistingFile("/sdk/tools/" + SdkConstants.androidCmdName(), "placeholder");
        mockFop.recordExistingFile("/sdk/tools/" + SdkConstants.FN_EMULATOR, "placeholder");

        SdkManager dummy = new SdkManager() {
            @Override
            public void registerSourceProvider(SdkSourceProvider provider) {}

            @Override
            public Set<SdkSourceProvider> getSourceProviders() {
                return null;
            }

            @Override
            public Map<String, SdkSchemaExtension> getSchemaExtensions() {
                try {
                    return ImmutableMap.of("foo", new SdkSchemaExtension(ObjectFactory.class, new File(
                            SdkManager.class.getResource("sdk-common-01.xsd").toURI())));
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public SdkSchemaExtension getSchemaExtension(String namespace) {
                return null;
            }

            @Override
            public void registerSchemaExtension(SdkSchemaExtension extension) {}

            @Override
            public boolean load(long timeoutMs, boolean canBeCancelled,
                    @NonNull List<SdkLoadedCallback> onLocalComplete,
                    @NonNull List<SdkLoadedCallback> onSuccess, @NonNull List<Runnable> onError,
                    boolean forceRefresh, ProgressRunner runner, boolean sync) {
                return false;
            }
        };
        LocalSdkImpl sdk = new LocalSdkImpl(new File("/sdk"), dummy, null, mockFop);
        LocalPackage p = sdk.getPackages().get("random");
        assertEquals(p.getRevision(), new PreciseRevision(3));
        // TODO: validate package
    }
}
