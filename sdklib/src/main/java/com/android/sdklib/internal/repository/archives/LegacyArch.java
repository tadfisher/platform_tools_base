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
 * The legacy Architecture that this archive can be downloaded on.
 * <p/>
 * This attribute was used for the &lt;archive&gt; element in repo schema 1-9.
 * add-on schema 1-6 and sys-img schema 1-2.
 * Starting with repo schema 10, add-on schema 7 and sys-img schema 3, this is replaced
 * by the &lt;host-bit&gt; and &lt;jvm-bit&gt; elements and {@link ArchFilter}.
 *
 * @see HostOs
 */
public enum LegacyArch {
    ANY("Any"),
    PPC("PowerPC"),
    X86("x86"),
    X86_64("x86_64");

    private final String mUiName;

    private LegacyArch(String uiName) {
        mUiName = uiName;
    }

    /** Returns the UI name of the architecture. */
    public String getUiName() {
        return mUiName;
    }

    /** Returns the XML name of the architecture. */
    public String getXmlName() {
        return toString().toLowerCase(Locale.US);
    }

    /**
     * Returns the current architecture as one of the {@link LegacyArch} enum values or null.
     */
    public static LegacyArch getCurrentArch() {
        // Values listed from http://lopica.sourceforge.net/os.html
        String arch = System.getProperty("os.arch");

        if (arch.equalsIgnoreCase("x86_64") || arch.equalsIgnoreCase("amd64")) {
            return LegacyArch.X86_64;

        } else if (arch.equalsIgnoreCase("x86")
                || arch.equalsIgnoreCase("i386")
                || arch.equalsIgnoreCase("i686")) {
            return LegacyArch.X86;

        } else if (arch.equalsIgnoreCase("ppc") || arch.equalsIgnoreCase("PowerPC")) {
            return LegacyArch.PPC;
        }

        return null;
    }

    /** Returns true if this architecture is compatible with the current one. */
    public boolean isCompatible() {
        if (this == ANY) {
            return true;
        }

        LegacyArch arch = getCurrentArch();
        return this == arch;
    }
}
