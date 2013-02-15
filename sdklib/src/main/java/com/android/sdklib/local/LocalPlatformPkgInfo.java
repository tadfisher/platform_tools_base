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
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;

public class LocalPlatformPkgInfo extends LocalAndroidVersionPkgInfo {
    private final IAndroidTarget mTarget;

    public LocalPlatformPkgInfo(@NonNull AndroidVersion version, @Nullable IAndroidTarget target) {
        super(version);
        mTarget = target;
    }

    @Override
    public int getType() {
        return LocalSdk.PKG_PLATFORMS;
    }

    public IAndroidTarget getAndroidTarget() {
        return mTarget;
    }
}
