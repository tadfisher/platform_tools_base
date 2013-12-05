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

package com.android.sdklib.repository.remote;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.internal.repository.IDescription;
import com.android.sdklib.repository.descriptors.PkgDesc;
import com.android.sdklib.repository.local.LocalPkgInfo;


/**
 * This class provides information on a remote package available for download
 * via a remote SDK repository server.
 */
public class RemotePkgInfo {

    /** Information on the package provided by the remote server. */
    @NonNull
    private final PkgDesc mPkgDesc;

    /** Local package being updated; null if this is not an update. */
    @Nullable
    private LocalPkgInfo mLocalPkg;

    /** Source identifier of the package. */
    @NonNull
    private final IDescription mSourceUri;

    public RemotePkgInfo(@NonNull PkgDesc pkgDesc, @NonNull IDescription sourceUri) {
        mPkgDesc = pkgDesc;
        mSourceUri = sourceUri;
    }

    /** Information on the package provided by the remote server. */
    @NonNull
    public PkgDesc getPkgDesc() {
        return mPkgDesc;
    }

    /**
     * Returns the source identifier of the remote package.
     * This is an opaque object that can return its own description.
     */
    @NonNull
    public IDescription getSourceUri() {
        return mSourceUri;
    }

    /**
     * True if this remote package is an update for a local package.
     * When true, {@link #getLocalPkg()} is non-null.
     */
    public boolean isLocalUpdate() {
        return mLocalPkg != null;
    }

    /**
     * The local package updated by this remote package.
     * Null when {@link #isLocalUpdate()} is false.
     */
    @Nullable
    public LocalPkgInfo getLocalPkg() {
        return mLocalPkg;
    }

    /** Internal setter for the local package. */
    @Nullable
    void setLocalPkg(LocalPkgInfo localPkg) {
        mLocalPkg = localPkg;
    }

}
