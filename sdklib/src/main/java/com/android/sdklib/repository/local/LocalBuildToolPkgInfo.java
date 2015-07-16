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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.repository.PkgProps;
import com.android.sdklib.repository.PreciseRevision;
import com.android.sdklib.repository.descriptors.IPkgDesc;
import com.android.sdklib.repository.descriptors.PkgDesc;
import com.android.sdklib.repository.descriptors.PkgType;

import java.io.File;
import java.util.Properties;

public class LocalBuildToolPkgInfo extends LocalPkgInfo {


    @Nullable
    private final BuildToolInfo mBuildToolInfo;
    @NonNull
    private final IPkgDesc mDesc;

    public LocalBuildToolPkgInfo(@NonNull LocalSdk localSdk,
            @NonNull File localDir,
            @NonNull Properties sourceProps,
            @NonNull PreciseRevision revision,
            @Nullable BuildToolInfo btInfo) {
        super(localSdk, localDir, sourceProps);
        if (sourceProps.containsKey(PkgProps.PKG_ID)) {
            mDesc = PkgDesc.Builder
                    .newGeneric(PkgType.PKG_BUILD_TOOLS, sourceProps.getProperty(PkgProps.PKG_ID),
                            null, revision, null).create();
        } else {
            mDesc = PkgDesc.Builder.newBuildTool(revision).create();
        }
        mBuildToolInfo = btInfo;
    }

    @NonNull
    @Override
    public IPkgDesc getDesc() {
        return mDesc;
    }

    @Nullable
    public BuildToolInfo getBuildToolInfo() {
        return mBuildToolInfo;
    }
}
