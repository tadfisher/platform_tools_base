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

package com.android.sdklib;


import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.local.LocalSdk;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class SdkManagerTest7 extends SdkManagerTestCase {

    /** Setup will build an SDK Manager local install matching a repository-7.xsd. */
    @Override
    public void setUp() throws Exception {
        super.setUp(7);
    }

    public void testSdkManager_getBuildTools() {
        // There is no build-tools folder in this repository.
        LocalSdk sdk = getSdk();

        assertNull(sdk.getBuildTool(new FullRevision(1)));
        assertNull(sdk.getBuildTool(new FullRevision(3, 0, 0)));
        assertNull(sdk.getBuildTool(new FullRevision(12, 3, 4, 5)));
    }
}
