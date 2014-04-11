/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdklib.internal.repository.archives;

import java.util.Locale;


/**
 * The legacy OS that this archive can be downloaded on.
 * <p/>
 * This attribute was used for the &lt;archive&gt; element in repo schema 1-9.
 * add-on schema 1-6 and sys-img schema 1-2.
 * Starting with repo schema 10, add-on schema 7 and sys-img schema 3, this is replaced
 * by the &lt;host-os&gt; element and {@link ArchFilter}.
 *
 * @see HostOs
 */
public enum LegacyOs {
    ANY("Any"),
    LINUX("Linux"),
    MACOSX("MacOS X"),
    WINDOWS("Windows");

    private final String mUiName;

    private LegacyOs(String uiName) {
        mUiName = uiName;
    }

    /** Returns the UI name of the OS. */
    public String getUiName() {
        return mUiName;
    }

    /** Returns the XML name of the OS. */
    public String getXmlName() {
        return toString().toLowerCase(Locale.US);
    }

    /**
     * Returns the current OS as one of the {@link LegacyOs} enum values or null.
     */
    public static LegacyOs getCurrentOs() {
        String os = System.getProperty("os.name");          //$NON-NLS-1$
        if (os.startsWith("Mac")) {                         //$NON-NLS-1$
            return LegacyOs.MACOSX;

        } else if (os.startsWith("Windows")) {              //$NON-NLS-1$
            return LegacyOs.WINDOWS;

        } else if (os.startsWith("Linux")) {                //$NON-NLS-1$
            return LegacyOs.LINUX;
        }

        return null;
    }

    /** Returns true if this OS is compatible with the current one. */
    public boolean isCompatible() {
        if (this == ANY) {
            return true;
        }

        LegacyOs os = getCurrentOs();
        return this == os;
    }
}
