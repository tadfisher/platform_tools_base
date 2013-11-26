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
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.MajorRevision;

public class PkgDescAddon extends PkgDescPlatform {

    public static final String ADDON_NAME         = "name";                 //$NON-NLS-1$
    public static final String ADDON_VENDOR       = "vendor";               //$NON-NLS-1$
    public static final String ADDON_API          = "api";                  //$NON-NLS-1$
    public static final String ADDON_DESCRIPTION  = "description";          //$NON-NLS-1$
    public static final String ADDON_LIBRARIES    = "libraries";            //$NON-NLS-1$
    public static final String ADDON_DEFAULT_SKIN = "skin";                 //$NON-NLS-1$
    public static final String ADDON_USB_VENDOR   = "usb-vendor";           //$NON-NLS-1$
    public static final String ADDON_REVISION     = "revision";             //$NON-NLS-1$
    public static final String ADDON_REVISION_OLD = "version";              //$NON-NLS-1$

    private final String mAddonPath;

    /**
     * Creates an add-on pkg description where the target path (add-on hash string) isn't
     * determined yet. Implementations <em>MUST</em> override {@link #getPath()} to return
     * a non-null target path.
     */
    public PkgDescAddon(@NonNull AndroidVersion version,
                        @NonNull MajorRevision revision) {
        super(version, revision);
        mAddonPath = null;
    }

    public PkgDescAddon(@NonNull AndroidVersion version,
                        @NonNull MajorRevision revision,
                        @NonNull String addonVendor,
                        @NonNull String addonName) {
        super(version, revision);
        mAddonPath = AndroidTargetHash.getAddonHashString(addonVendor, addonName, version);
    }

    @Override
    public int getType() {
        return PkgDesc.PKG_ADDONS;
    }

    /** The "path" of a Add-on is its Target Hash. */
    @NonNull
    @Override
    public String getPath() {
        if (mAddonPath == null) {
            throw new IllegalArgumentException(
                    "Implementation MUST override PkgDescAddon.getPath " +      //$NON-NLS-1$
                    "to compute the add-on hash string");                       //$NON-NLS-1$
        }
        return mAddonPath;
    }
}
