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

package com.android.sdklib.repository.local;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.internal.repository.IListDescription;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.repository.descriptors.PkgDesc;

import java.io.File;
import java.util.Properties;

public abstract class LocalPkgInfo implements IListDescription, Comparable<LocalPkgInfo> {

    private final LocalSdk mLocalSdk;
    private final File mLocalDir;
    private final Properties mSourceProperties;

    private Package mPackage;
    private String mLoadError;

    protected LocalPkgInfo(@NonNull LocalSdk   localSdk,
                           @NonNull File       localDir,
                           @NonNull Properties sourceProps) {
        mLocalSdk = localSdk;
        mLocalDir = localDir;
        mSourceProperties = sourceProps;
    }

    //---- Attributes ----

    @NonNull
    public LocalSdk getLocalSdk() {
        return mLocalSdk;
    }

    @NonNull
    public File getLocalDir() {
        return mLocalDir;
    }

    @NonNull
    public Properties getSourceProperties() {
        return mSourceProperties;
    }

    @Nullable
    public String getLoadError() {
        return mLoadError;
    }

    // ----

    /** Returns the {@link PkgDesc} describing this package. */
    @NonNull
    public abstract PkgDesc getDesc();


    //---- Ordering ----

    @Override
    public int compareTo(@NonNull LocalPkgInfo o) {
        int t1 = getDesc().getType().getIntValue();
        int t2 = o.getDesc().getType().getIntValue();
        if (t1 != t2) {
            return t2 - t1;
        }

        if (getDesc().hasAndroidVersion() && o.getDesc().hasAndroidVersion()) {
            t1 = getDesc().getAndroidVersion().compareTo(o.getDesc().getAndroidVersion());
            if (t1 != 0) {
                return t1;
            }
        }


        if (getDesc().hasPath() && o.getDesc().hasPath()) {
            t1 = getDesc().getPath().compareTo(o.getDesc().getPath());
            if (t1 != 0) {
                return t1;
            }
        }

        if (getDesc().hasFullRevision() && o.getDesc().hasFullRevision()) {
            t1 = getDesc().getFullRevision().compareTo(o.getDesc().getFullRevision());
            if (t1 != 0) {
                return t1;
            }
        }

        if (getDesc().hasMajorRevision() && o.getDesc().hasMajorRevision()) {
            t1 = getDesc().getMajorRevision().compareTo(o.getDesc().getMajorRevision());
            if (t1 != 0) {
                return t1;
            }
        }

        return 0;
    }

    /** String representation for debugging purposes. */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<");
        builder.append(this.getClass().getSimpleName());

        if (getDesc().hasAndroidVersion()) {
            builder.append(" Android=").append(getDesc().getAndroidVersion());
        }

        if (getDesc().hasPath()) {
            builder.append(" Path=").append(getDesc().getPath());
        }

        if (getDesc().hasFullRevision()) {
            builder.append(" FullRev=").append(getDesc().getFullRevision());
        }

        if (getDesc().hasMajorRevision()) {
            builder.append(" MajorRev=").append(getDesc().getMajorRevision());
        }

        builder.append(">");
        return builder.toString();
    }

    //---- Package Management ----

    /** A "broken" package is installed but is not fully operational.
     *
     * For example an addon that lacks its underlying platform or a tool package
     * that lacks some of its binaries or essentially files.
     * <p/>
     * Operational code should generally ignore broken packages.
     * Only the SDK Updater cares about displaying them so that they can be fixed.
     */
    public boolean hasLoadError() {
        return mLoadError != null;
    }

    void appendLoadError(@NonNull String format, Object...params) {
        String loadError = String.format(format, params);
        if (mLoadError == null) {
            mLoadError = loadError;
        } else {
            mLoadError = mLoadError + '\n' + loadError;
        }
    }

    void setPackage(@Nullable Package pkg) {
        mPackage = pkg;
    }

    @Nullable
    public Package getPackage() {
        return mPackage;
    }

    @NonNull
    @Override
    public String getListDescription() {
        Package pkg = getPackage();
        return pkg == null ? "" : pkg.getListDescription();
    }

}

