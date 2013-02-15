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
import com.android.sdklib.internal.repository.IListDescription;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.MajorRevision;

import java.io.File;

public abstract class LocalPkgInfo implements IListDescription {

    private Package mPackage;

    protected LocalPkgInfo() {
    }

    //---- Attributes ----

    public boolean hasFullRevision() {
        return false;
    }

    public boolean hasMajorRevision() {
        return false;
    }

    public boolean hasAndroidVersion() {
        return false;
    }

    public FullRevision getFullRevision() {
        return null;
    }

    public MajorRevision getMajorRevision() {
        return null;
    }

    public AndroidVersion getAndroidVersion() {
        return null;
    }

    public String getExtraPath() {
        return null;
    }

    //---- Package Management ----

    /** Returns the local root directory of the package.
     * Since the package info was loaded from the sourec property file in that directory
     * it's pretty much guaranteed to exist, except the SDK directories could have changed
     * since the last refresh. */
    public File getLocation() {
       return null;
    }

    /** A "broken" package is installed but is not fully operational.
     * For example an addon that lacks its underlying platform or a tool package
     * that lacks some of its binaries or essentially files.
     * <p/>
     * Operational code should generally ignore broken packages.
     * Only the SDK Updater cares about displaying them so that they can be fixed.
     */
    public boolean isBroken() {
        // TODO this is only determined *after* we load the Package object
        return false;
    }

    void setPackage(Package pkg) {
        mPackage = pkg;
    }

    public Package getPackage() {
        if (mPackage == null) {
            // TODO lazily load package information
        }
        assert mPackage != null;
        return mPackage;
    }

    @Override
    public String getListDescription() {
        return getPackage().getListDescription();
    }

    // ------ Tools-related objects ------

    public static class ToolInfo extends LocalPkgInfo {
        private final @NonNull FullRevision mRevision;

        public ToolInfo(@NonNull FullRevision revision) {
            mRevision = revision;
        }

        @Override
        public boolean hasFullRevision() {
            return true;
        }

        @Override
        public FullRevision getFullRevision() {
            return mRevision;
        }
    }

    public static class PlatformToolInfo extends ToolInfo {
        public PlatformToolInfo(@NonNull FullRevision revision) {
            super(revision);
        }
    }

    public static class BuildToolInfo extends ToolInfo {
        private final com.android.sdklib.BuildToolInfo mBuildToolInfo;

        public BuildToolInfo(@NonNull FullRevision revision, @Nullable com.android.sdklib.BuildToolInfo btInfo) {
            super(revision);
            mBuildToolInfo = btInfo;
        }

        public com.android.sdklib.BuildToolInfo getBuildToolInfo() {
            return mBuildToolInfo;
        }
    }

    // ------ Platform-related objects ------

    private abstract static class AndroidVersionInfo extends LocalPkgInfo {
        private final AndroidVersion mVersion;

        public AndroidVersionInfo(@NonNull AndroidVersion version) {
            mVersion = version;
        }

        @Override
        public boolean hasAndroidVersion() {
            return true;
        }

        @Override
        public AndroidVersion getAndroidVersion() {
            return mVersion;
        }
    }

    public static class PlatformInfo extends AndroidVersionInfo {
        private final IAndroidTarget mTarget;

        public PlatformInfo(@NonNull AndroidVersion version, @Nullable IAndroidTarget target) {
            super(version);
            mTarget = target;
        }

        public IAndroidTarget getAndroidTarget() {
            return mTarget;
        }
    }

    public static class AddonInfo extends PlatformInfo {

        public AddonInfo(@NonNull AndroidVersion version, @Nullable IAndroidTarget target) {
            super(version, target);
        }
    }

    public static class SystemImageInfo extends AndroidVersionInfo {
        public SystemImageInfo(
                @NonNull String abi,
                @NonNull AndroidVersion version) {
            super(version);
        }

        // TODO expand as needed
    }

    public static class SourceInfo extends AndroidVersionInfo {
        public SourceInfo(@NonNull AndroidVersion version) {
            super(version);
        }

        // TODO expand as needed
    }

    public static class SampleInfo extends AndroidVersionInfo {
        public SampleInfo(@NonNull AndroidVersion version) {
            super(version);
        }

        // TODO expand as needed
    }

    // ------ Other objects ------

    public static class DocInfo extends LocalPkgInfo {

    }

    public static class ExtraInfo extends LocalPkgInfo {

        private final @NonNull String mExtraPath;
        private final String mVendorId;

        public ExtraInfo(@NonNull String vendorId, @NonNull String path) {
            mVendorId = vendorId;
            mExtraPath = path;
        }

        @Override
        public boolean hasMajorRevision() {
            return true;
        }

        @Override
        public MajorRevision getMajorRevision() {
            // TODO
            return null;
        }

        @Override
        public String getExtraPath() {
            return mExtraPath;
        }

        public String getVendorId() {
            return mVendorId;
        }
    }

}

