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
import com.android.sdklib.internal.repository.archives.Archive.Arch;
import com.android.sdklib.internal.repository.archives.Archive.Os;
import com.android.sdklib.internal.repository.packages.ExtraPackage;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.repository.NoPreviewRevision;

import java.io.File;
import java.util.Properties;

public class LocalExtraPkgInfo extends LocalFullRevisionPkgInfo {

    private final @NonNull String mExtraPath;
    private final String mVendorId;

    public LocalExtraPkgInfo(@NonNull LocalSdk localSdk,
                             @NonNull File localDir,
                             @NonNull Properties sourceProps,
                             @NonNull String vendorId,
                             @NonNull String path,
                             @NonNull NoPreviewRevision revision) {
        super(localSdk, localDir, sourceProps, revision);
        mVendorId = vendorId;
        mExtraPath = path;
    }

    @Override
    public int getType() {
        return LocalSdk.PKG_EXTRAS;
    }

    @Override
    public boolean hasPath() {
        return true;
    }

    public String getExtraPath() {
        return mExtraPath;
    }

    public String getVendorId() {
        return mVendorId;
    }

    @Override
    public String getPath() {
        return mVendorId + '/' + mExtraPath;
    }

    @Override
    public Package getPackage() {
        Package pkg = super.getPackage();
        if (pkg == null) {
            try {
                pkg = ExtraPackage.create(
                        null,                       //source
                        getSourceProperties(),      //properties
                        mVendorId,                  //vendor
                        mExtraPath,                 //path
                        0,                          //revision
                        null,                       //license
                        null,                       //description
                        null,                       //descUrl
                        Os.getCurrentOs(),          //archiveOs
                        Arch.getCurrentArch(),      //archiveArch
                        getLocalDir().getPath()     //archiveOsPath
                        );
                setPackage(pkg);
            } catch (Exception e) {
                appendLoadError("Failed to parse package: %1$s", e.toString());
            }
        }
        return pkg;
    }
}

