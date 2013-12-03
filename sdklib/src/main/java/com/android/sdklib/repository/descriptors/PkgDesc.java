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
import com.android.sdklib.repository.NoPreviewRevision;

/**
 * {@link PkgDesc} keeps information on individual SDK packages
 * (both local or remote packages definitions.)
 * <br/>
 * Packages have different attributes depending on their type.
 * <p/>
 * To create a new {@link PkgDesc}, use one of the package-specific constructors
 * provided here.
 * <p/>
 * To query packages capabilities, rely on {@link #getType()} and the {@code PkgDesc.hasXxx()}
 * methods provided in the base {@link PkgDesc}.
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
        builder.append("<PkgDesc");                                             //NON-NLS-1$

        if (hasAndroidVersion()) {
            builder.append(" Android=").append(getAndroidVersion());            //NON-NLS-1$
        }

        if (hasPath()) {
            builder.append(" Path=").append(getPath());                         //NON-NLS-1$
        }

        if (hasFullRevision()) {
            builder.append(" FullRev=").append(getFullRevision());              //NON-NLS-1$
        }

        if (hasMajorRevision()) {
            builder.append(" MajorRev=").append(getMajorRevision());            //NON-NLS-1$
        }

        builder.append('>');
        return builder.toString();
    }

    // ---- Constructors -----

    /**
     * Create a new tool package descriptor.
     *
     * @param revision The revision of the tool package.
     * @return A {@link PkgDesc} describing this tool package.
     */
    @NonNull
    public static PkgDesc newTool(@NonNull FullRevision revision) {
        return new PkgDescFullRevision(revision) {
            @Override
            public PkgType getType() {
                return PkgType.PKG_TOOLS;
            }
        };
    }

    /**
     * Create a new platform-tool package descriptor.
     *
     * @param revision The revision of the platform-tool package.
     * @return A {@link PkgDesc} describing this platform-tool package.
     */
    @NonNull
    public static PkgDesc newPlatformTool(@NonNull FullRevision revision) {
        return new PkgDescFullRevision(revision) {
            @Override
            public PkgType getType() {
                return PkgType.PKG_PLATFORM_TOOLS;
            }
        };
    }

    /**
     * Create a new build-tool package descriptor.
     *
     * @param revision The revision of the build-tool package.
     * @return A {@link PkgDesc} describing this build-tool package.
     */
    @NonNull
    public static PkgDesc newBuildTool(@NonNull FullRevision revision) {
        return new PkgDescFullRevision(revision) {
            @Override
            public PkgType getType() {
                return PkgType.PKG_BUILD_TOOLS;
            }
        };
    }

    /**
     * Create a new doc package descriptor.
     *
     * @param revision The revision of the doc package.
     * @return A {@link PkgDesc} describing this doc package.
     */
    @NonNull
    public static PkgDesc newDoc(@NonNull MajorRevision revision) {
        return new PkgDescMajorRevision(revision) {
            @Override
            public PkgType getType() {
                return PkgType.PKG_DOCS;
            }
        };
    }

    /**
     * Create a new extra package descriptor.
     *
     * @param vendorId The vendor id string of the extra package.
     * @param path The path id string of the extra package.
     * @param revision The revision of the extra package.
     * @return A {@link PkgDesc} describing this extra package.
     */
    @NonNull
    public static PkgDesc newExtra(@NonNull String vendorId,
                                   @NonNull String path,
                                   @NonNull NoPreviewRevision revision) {
        return new PkgDescExtra(vendorId, path, revision);
    }

    /**
     * Create a new platform package descriptor.
     *
     * @param version The android version of the platform package.
     * @param revision The revision of the extra package.
     * @return A {@link PkgDesc} describing this platform package.
     */
    @NonNull
    public static PkgDesc newPlatform(@NonNull AndroidVersion version,
                                      @NonNull MajorRevision revision) {
        return new PkgDescPlatform(version, revision);
    }

    /**
     * Create a new add-on package descriptor.
     * <p/>
     * The vendor id and the name id provided are used to compute the add-on's
     * target hash.
     *
     * @param version The android version of the add-on package.
     * @param revision The revision of the add-on package.
     * @param addonVendor The vendor id of the add-on package.
     * @param addonName The name id of the add-on package.
     * @return A {@link PkgDesc} describing this add-on package.
     */
    @NonNull
    public static PkgDesc newAddon(@NonNull AndroidVersion version,
                                   @NonNull MajorRevision revision,
                                   @NonNull String addonVendor,
                                   @NonNull String addonName) {
        return new PkgDescAddon(version, revision, addonVendor, addonName);
    }

    public interface ITargetHashProvider {
        String getTargetHash();
    }

    /**
     * Create a new platform add-on descriptor where the target hash isn't determined yet.
     *
     * @param version The android version of the add-on package.
     * @param revision The revision of the add-on package.
     * @param targetHashProvider Implements a method that will return the target hash when needed.
     * @return A {@link PkgDesc} describing this add-on package.
     */
    @NonNull
    public static PkgDesc newAddon(@NonNull AndroidVersion version,
                                   @NonNull MajorRevision revision,
                                   @NonNull ITargetHashProvider targetHashProvider) {
        return new PkgDescAddon(version, revision, targetHashProvider);
    }

    /**
     * Create a new system-image package descriptor.
     * <p/>
     * For system-images, {@link PkgDesc#getPath()} returns the ABI.
     *
     * @param version The android version of the system-image package.
     * @param abi The ABI of the system-image package.
     * @param revision The revision of the system-image package.
     * @return A {@link PkgDesc} describing this system-image package.
     */
    @NonNull
    public static PkgDesc newSysImg(@NonNull AndroidVersion version,
                                    @NonNull String abi,
                                    @NonNull MajorRevision revision) {
        return new PkgDescSysImg(version, abi, revision);
    }

    /**
     * Create a new source package descriptor.
     *
     * @param version The android version of the source package.
     * @param revision The revision of the source package.
     * @return A {@link PkgDesc} describing this source package.
     */
    @NonNull
    public static PkgDesc newSource(@NonNull AndroidVersion version,
                                    @NonNull MajorRevision revision) {
        return new PkgDescSource(version, revision);
    }

    /**
     * Create a new sample package descriptor.
     *
     * @param version The android version of the sample package.
     * @param revision The revision of the sample package.
     * @return A {@link PkgDesc} describing this sample package.
     */
    @NonNull
    public static PkgDesc newSample(@NonNull AndroidVersion version,
                                    @NonNull MajorRevision revision) {
        return new PkgDescSample(version, revision);
    }
}

