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
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.MajorRevision;

public class LocalPlatformPkgInfo extends LocalAndroidVersionPkgInfo {

    private final MajorRevision mRevision;
    //--TODO--private final IAndroidTarget mTarget; -- load on demand?

    public LocalPlatformPkgInfo(@NonNull AndroidVersion version, @NonNull MajorRevision revision) {
        super(version);
        mRevision = revision;
        //--TODO--mTarget = target;
    }

    @Override
    public int getType() {
        return LocalSdk.PKG_PLATFORMS;
    }

//--TODO-- get/setter
//    @Nullable
//    public IAndroidTarget getAndroidTarget() {
//        return mTarget;
//    }

    @Override
    public boolean hasMajorRevision() {
        return true;
    }

    @Override
    public MajorRevision getMajorRevision() {
        return mRevision;
    }
}
