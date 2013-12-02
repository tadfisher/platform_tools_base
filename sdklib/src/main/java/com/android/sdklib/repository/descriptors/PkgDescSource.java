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
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.MajorRevision;

/**
 * Local source package, for a given platform's {@link AndroidVersion}.
 * The package itself has a major revision.
 * There should be only one for a given android platform version.
 */
public class PkgDescSource extends PkgDescAndroidVersion {

    @NonNull
    private final MajorRevision mRevision;

    public PkgDescSource(@NonNull AndroidVersion version,
                         @NonNull MajorRevision revision) {
        super(version);
        mRevision = revision;
    }

    @NonNull
    @Override
    public PkgType getType() {
        return PkgType.PKG_SOURCES;
    }

    @Override
    public boolean hasMajorRevision() {
        return true;
    }

    @NonNull
    @Override
    public MajorRevision getMajorRevision() {
        return mRevision;
    }
}
