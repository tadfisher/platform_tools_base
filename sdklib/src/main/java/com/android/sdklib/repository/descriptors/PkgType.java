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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.internal.repository.LocalSdkParser;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.MajorRevision;
import com.android.sdklib.repository.local.LocalSdk;

import java.util.EnumSet;

/**
 * Package types handled by the {@link LocalSdk}.
 * <p/>
 * Integer bit values are provided via {@link #getIntValue()} for backward
 * compatibility with the older {@link LocalSdkParser} class.
 * The integer bit values also indicate the natural ordering of the packages.
 */
public enum PkgType {

    /** Filter the SDK/tools folder.
     *  Has {@link FullRevision}. */
    PKG_TOOLS(0x0001, SdkConstants.FD_TOOLS),

    /** Filter the SDK/platform-tools folder.
     *  Has {@link FullRevision}. */
    PKG_PLATFORM_TOOLS(0x0002, SdkConstants.FD_PLATFORM_TOOLS),

    /** Filter the SDK/build-tools folder.
     *  Has {@link FullRevision}. */
    PKG_BUILD_TOOLS(0x0004, SdkConstants.FD_BUILD_TOOLS),

    /** Filter the SDK/docs folder.
     *  Has {@link MajorRevision}. */
    PKG_DOCS(0x0010, SdkConstants.FD_DOCS),

    /** Filter the SDK/extras folder.
     *  Has {@code Path}. Has {@link MajorRevision}. */
    PKG_EXTRAS(0x0020, SdkConstants.FD_EXTRAS),

    /** Filter the SDK/platforms.
     *  Has {@link AndroidVersion}. Has {@link MajorRevision}. */
    PKG_PLATFORMS(0x0100, SdkConstants.FD_PLATFORMS),

    /** Filter the SDK/sys-images.
     * Has {@link AndroidVersion}. Has {@link MajorRevision}. */
    PKG_SYS_IMAGES(0x0200, SdkConstants.FD_SYSTEM_IMAGES),

    /** Filter the SDK/addons.
     *  Has {@link AndroidVersion}. Has {@link MajorRevision}. */
    PKG_ADDONS(0x0400, SdkConstants.FD_ADDONS),

    /** Filter the SDK/samples folder.
     *  Note: this will not detect samples located in the SDK/extras packages.
     *  Has {@link AndroidVersion}. Has {@link MajorRevision}. */
    PKG_SAMPLES(0x0800, SdkConstants.FD_SAMPLES),

    /** Filter the SDK/sources folder.
     *  Has {@link AndroidVersion}. Has {@link MajorRevision}. */
    PKG_SOURCES(0x1000, SdkConstants.FD_ANDROID_SOURCES);

    /** A collection of all the known PkgTypes. */
    public static final EnumSet<PkgType> PKG_ALL = EnumSet.allOf(PkgType.class);

    /** Integer value matching all available pkg types, for the old LocalSdkParer. */
    public static final int PKG_ALL_INT = 0xFFFF;

    private int mIntValue;
    private String mFolderName;

    PkgType(int intValue, @NonNull String folderName) {
        mIntValue = intValue;
        mFolderName = folderName;
    }

    public int getIntValue() {
        return mIntValue;
    }

    @NonNull
    public String getFolderName() {
        return mFolderName;
    }
}

