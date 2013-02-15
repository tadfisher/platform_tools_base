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
import com.android.sdklib.repository.MajorRevision;

public class LocalExtraPkgInfo extends LocalMajorRevisionPkgInfo {

    private final @NonNull String mExtraPath;
    private final String mVendorId;

    public LocalExtraPkgInfo(@NonNull String vendorId,
                             @NonNull String path,
                             @NonNull MajorRevision revision) {
        super(revision);
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
        return mExtraPath + '/' + mVendorId;
    }
}

