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
import com.android.sdklib.internal.repository.IListDescription;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.MajorRevision;

import java.io.File;

public abstract class LocalPkgInfo implements IListDescription, Comparable<LocalPkgInfo> {

    private Package mPackage;
    private String mLoadError;

    // TODO keep path, keep props
    protected LocalPkgInfo() {
    }

    //---- Attributes ----

    public abstract int getType();

    public boolean hasFullRevision() {
        return false;
    }

    public boolean hasMajorRevision() {
        return false;
    }

    public boolean hasAndroidVersion() {
        return false;
    }

    public boolean hasPath() {
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

    public String getPath() {
        return null;
    }

    //---- Ordering ----

    @Override
    public int compareTo(LocalPkgInfo o) {
        int t1 = getType();
        int t2 = o.getType();
        if (t1 != t2) {
            return t2 - t1;
        }

        if (hasAndroidVersion() && o.hasAndroidVersion()) {
            t1 = getAndroidVersion().compareTo(o.getAndroidVersion());
            if (t1 != 0) {
                return t1;
            }
        }


        if (hasPath() && o.hasPath()) {
            t1 = getPath().compareTo(o.getPath());
            if (t1 != 0) {
                return t1;
            }
        }

        if (hasFullRevision() && o.hasFullRevision()) {
            t1 = getFullRevision().compareTo(o.getFullRevision());
            if (t1 != 0) {
                return t1;
            }
        }

        if (hasMajorRevision() && o.hasMajorRevision()) {
            t1 = getMajorRevision().compareTo(o.getMajorRevision());
            if (t1 != 0) {
                return t1;
            }
        }

        return 0;
    }

    //---- Package Management ----

    /** Returns the local root directory of the package.
     * Since the package info was loaded from the source property file in that directory
     * it's pretty much guaranteed to exist, except the SDK directories could have changed
     * since the last refresh. */
    public File getLocation() {
       return null;
    }

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

}

