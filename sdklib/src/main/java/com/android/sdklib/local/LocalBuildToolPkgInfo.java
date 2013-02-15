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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.internal.repository.packages.BuildToolPackage;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.repository.FullRevision;

import java.io.File;
import java.util.Properties;

public class LocalBuildToolPkgInfo extends LocalFullRevisionPkgInfo {

    private final BuildToolInfo mBuildToolInfo;

    public LocalBuildToolPkgInfo(@NonNull LocalSdk localSdk,
                                 @NonNull File localDir,
                                 @NonNull Properties sourceProps,
                                 @NonNull FullRevision revision,
                                 @Nullable BuildToolInfo btInfo) {
        super(localSdk, localDir, sourceProps, revision);
        mBuildToolInfo = btInfo;
    }

    @Override
    public int getType() {
        return LocalSdk.PKG_BUILD_TOOLS;
    }

    public BuildToolInfo getBuildToolInfo() {
        return mBuildToolInfo;
    }

    @Override
    public Package getPackage() {
        Package pkg = super.getPackage();
        if (pkg == null) {
            try {
                pkg = BuildToolPackage.create(getLocalDir(), getSourceProperties());
                setPackage(pkg);
            } catch (Exception e) {
                appendLoadError("Failed to parse package: %1$s", e.toString());
            }
        }
        return pkg;
    }
}
