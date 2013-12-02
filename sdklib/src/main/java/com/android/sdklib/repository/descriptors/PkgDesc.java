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
import com.android.annotations.Nullable;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.MajorRevision;

/**
 * {@link PkgDesc} keeps information on individual SDK packages.
 * <br/>
 * Packages have different attributes depending on their type.
 * To create a new {@link PkgDesc}, use one of the package-specific constructors
 * for example {@link PkgDescTool}'s constructor. However when testing packages
 * capabilities, rely on {@link #getType()} and the {@code PkgDesc.hasXxx()} methods
 * rather than on the class name.
 * <br/>
 */
public abstract class PkgDesc implements Comparable<PkgDesc> {

    /**
     * Returns the type of the package.
     * @return Returns one of the {@link PkgType} constants.
     */
    public abstract PkgType getType();

    /**
     * Indicates whether this package type has a {@link FullRevision}.
     * @return True if this package type has a {@link FullRevision}.
     */
    public boolean hasFullRevision() {
        return false;
    }

    /**
     * Indicates whether this package type has a {@link MajorRevision}.
     * @return True if this package type has a {@link MajorRevision}.
     */
    public boolean hasMajorRevision() {
        return false;
    }

    /**
     * Indicates whether this package type has a {@link AndroidVersion}.
     * @return True if this package type has a {@link AndroidVersion}.
     */
    public boolean hasAndroidVersion() {
        return false;
    }

    /**
     * Indicates whether this package type has a path.
     * @return True if this package type has a path.
     */
    public boolean hasPath() {
        return false;
    }

    /**
     * Returns the package's {@link FullRevision} or null.
     * @return A non-null value if {@link #hasFullRevision()} is true; otherwise a null value.
     */
    @Nullable
    public FullRevision getFullRevision() {
        return null;
    }

    /**
     * Returns the package's {@link MajorRevision} or null.
     * @return A non-null value if {@link #hasMajorRevision()} is true; otherwise a null value.
     */
    @Nullable
    public MajorRevision getMajorRevision() {
        return null;
    }

    /**
     * Returns the package's {@link AndroidVersion} or null.
     * @return A non-null value if {@link #hasAndroidVersion()} is true; otherwise a null value.
     */
    @Nullable
    public AndroidVersion getAndroidVersion() {
        return null;
    }

    /**
     * Returns the package's path string or null.
     * @return A non-null value if {@link #hasPath()} is true; otherwise a null value.
     */
    @Nullable
    public String getPath() {
        return null;
    }

    //---- Ordering ----

    @Override
    public int compareTo(@NonNull PkgDesc o) {
        int t1 = getType().getIntValue();
        int t2 = o.getType().getIntValue();
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

    /** String representation for debugging purposes. */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<");
        builder.append(this.getClass().getSimpleName());

        if (hasAndroidVersion()) {
            builder.append(" Android=").append(getAndroidVersion());
        }

        if (hasPath()) {
            builder.append(" Path=").append(getPath());
        }

        if (hasFullRevision()) {
            builder.append(" FullRev=").append(getFullRevision());
        }

        if (hasMajorRevision()) {
            builder.append(" MajorRev=").append(getMajorRevision());
        }

        builder.append(">");
        return builder.toString();
    }
}

