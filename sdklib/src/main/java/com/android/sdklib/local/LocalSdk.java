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
import com.android.annotations.VisibleForTesting;
import com.android.annotations.VisibleForTesting.Visibility;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.SdkManager;
import com.android.sdklib.io.FileOp;
import com.android.sdklib.repository.FullRevision;
import com.google.common.collect.Lists;

import java.io.File;
import java.util.List;

/**
 * This class keeps information on the current locally installed SDK.
 * It tries to lazily load information as much as possible.
 * <p/>
 * Apps/libraries that use it are encouraged to keep an existing instance around
 * (using a singleton or similar mechanism).
 *
 * @version 2 of the {@link SdkManager} class, essentially.
 */
public class LocalSdk {

    /** Location of the SDK. Maybe null. Can be changed. */
    private File mSdkRoot;
    /** File operation object. (Used for overriding in mock testing.) */
    private final FileOp mFileOp;
    /** List of package information loaded so far. Lazily populated. */
    private final List<LocalPkgInfo> mPackages = Lists.newArrayList();

    /** Filter all SDK folders. */
    public static final int PKG_ALL            = 0xFFFF;
    /** Filter the SDK/tools folder. */
    public static final int PKG_TOOLS          = 0x0001;
    /** Filter the SDK/platform-tools folder */
    public static final int PKG_PLATFORM_TOOLS = 0x0002;
    /** Filter the SDK/docs folder. */
    public static final int PKG_DOCS           = 0x0004;
    /** Filter the SDK/platforms. */
    public static final int PKG_PLATFORMS      = 0x0010;
    /** Filter the SDK/addons. */
    public static final int PKG_ADDONS         = 0x0020;
    /** Filter the SDK/samples folder.
     * Note: this will not detect samples located in the SDK/extras packages. */
    public static final int PKG_SAMPLES        = 0x0100;
    /** Filter the SDK/sources folder. */
    public static final int PKG_SOURCES        = 0x0200;
    /** Filter the SDK/extras folder. */
    public static final int PKG_EXTRAS         = 0x0400;
    /** Filter the SDK/build-tools folder. */
    public static final int PKG_BUILD_TOOLS    = 0x0800;

    /**
     * Creates an initial LocalSdk instance with an unknown location.
     */
    public LocalSdk() {
        mFileOp = new FileOp();
    }

    /**
     * Creates an initial LocalSdk instance for a known SDK location.
     */
    public LocalSdk(@NonNull File sdkRoot) {
        this();
        setLocation(sdkRoot);
    }

    /**
     * Creates an initial LocalSdk instance with an unknown location.
     * This is designed for unit tests to override the {@link FileOp} being used.
     *
     * @param fileOp The alternate {@link FileOp} to use for all file-based interactions.
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    protected LocalSdk(@NonNull FileOp fileOp) {
        mFileOp = fileOp;
    }

    public void setLocation(@NonNull File sdkRoot) {
        assert sdkRoot != null;
        mSdkRoot = sdkRoot;
        // TODO reload existing info
        sendSdkChanged();
    }

    /** Location of the SDK. Maybe null. Can be changed. */
    @Nullable
    public File getLocation() {
        return mSdkRoot;
    }

    /**
     *
     * @param filter {@link #PKG_PLATFORMS}, {@link #PKG_ADDONS}, {@link #PKG_SAMPLES}.
     * @param version
     * @return
     */
    public LocalPkgInfo getPkgInfo(int filter, AndroidVersion version) {
        return null;
    }

    /**
     *
     * @param filter .
     * @param version
     * @return
     */
    public LocalPkgInfo getPkgInfo(int filter, FullRevision revision) {
        return null;
    }

    /**
     * For unique local packages.
     * @param filter {@link #PKG_TOOLS} or {@link #PKG_PLATFORM_TOOLS} or {@link #PKG_DOCS}.
     * @return
     */
    public LocalPkgInfo getPkgInfo(int filter) {
        return null;
    }

    /**
     * Retrieve all the info about the requested package type.
     *
     * @param filter
     * @return
     */
    public LocalPkgInfo[] getPkgsInfos(int filter) {
        return new LocalPkgInfo[0];
    }


    //---------- Events ---------

    public interface OnSdkChanged {
        public void onSdkChanged();
    }

    private void sendSdkChanged() {
        // TODO Auto-generated method stub

    }
}
