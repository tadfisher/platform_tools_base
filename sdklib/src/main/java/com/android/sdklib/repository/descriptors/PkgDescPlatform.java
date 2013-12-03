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
import com.android.sdklib.repository.MajorRevision;

/**
 * Implementation detail of {@link PkgDesc} for platforms.
 * Do not use this class directly.
 * To create an instance use {@link PkgDesc#newPlatform(AndroidVersion, MajorRevision)} instead.
 */
class PkgDescPlatform extends PkgDescAndroidVersion {

    private final MajorRevision mRevision;

    PkgDescPlatform(@NonNull AndroidVersion version,
                    @NonNull MajorRevision revision) {
        super(version);
        mRevision = revision;
    }

    @Override
    public PkgType getType() {
        return PkgType.PKG_PLATFORMS;
    }

    @Override
    public boolean hasPath() {
        return true;
    }

    /** The "path" of a Platform is its Target Hash. */
    @Override
    public String getPath() {
        return AndroidTargetHash.getPlatformHashString(getAndroidVersion());
    }

    @Override
    public boolean hasMajorRevision() {
        return true;
    }

    @Override
    public MajorRevision getMajorRevision() {
        return mRevision;
    }
}
