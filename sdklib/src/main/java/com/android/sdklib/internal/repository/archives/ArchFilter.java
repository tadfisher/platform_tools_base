/*
 * Copyright (C) 2014 The Android Open Source Project
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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.repository.NoPreviewRevision;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchFilter {

    private final HostOs mHostOs;
    private final BitSize mHostBits;
    private final BitSize mJvmBits;
    private final NoPreviewRevision mMinJvmVersion;

    /**
     * Creates a new {@link ArchFilter} with the specified filter attributes.
     * <p/>
     * This filters represents the attributes requires for a package's {@link Archive} to
     * be installable on the current architecture. Not all fields are required -- those that
     * are not specified imply there is no limitation on that particular attribute.
     *
     *
     * @param hostOs The host OS or null if there's no limitation for this package.
     * @param hostBits The host bit size or null if there's no limitation for this package.
     * @param jvmBits The JVM bit size or null if there's no limitation for this package.
     * @param minJvmVersion The minimal JVM version required by this package
     *                      or null if there's no limitation for this package.
     */
    public ArchFilter(@Nullable HostOs  hostOs,
                      @Nullable BitSize hostBits,
                      @Nullable BitSize jvmBits,
                      @Nullable NoPreviewRevision minJvmVersion) {
        mHostOs = hostOs;
        mHostBits = hostBits;
        mJvmBits = jvmBits;
        mMinJvmVersion = minJvmVersion;
    }

    /** @return the host OS or null if there's no limitation for this package. */
    @Nullable
    public HostOs getHostOS() {
        return mHostOs;
    }

    /** @return the host bit size or null if there's no limitation for this package. */
    @Nullable
    public BitSize getHostBits() {
        return mHostBits;
    }

    /** @return the JVM bit size or null if there's no limitation for this package. */
    @Nullable
    public BitSize getJvmBits() {
        return mJvmBits;
    }

    /** @return the minimal JVM version required by this package
     *          or null if there's no limitation for this package. */
    @Nullable
    public NoPreviewRevision getMinJvmVersion() {
        return mMinJvmVersion;
    }

    /**
     * Checks whether {@code this} {@link ArchFilter} is compatible with the right-hand side one.
     * <p/>
     * Typically this is used to check whether "this downloaded package is compatible with the
     * current architecture", which would be expressed as:
     * <pre>
     * DownloadedArchive.filter.isCompatibleWith(ArhFilter.getCurrent())
     * </pre>
     *
     * @param required The requirements to meet.
     * @return True if this filter meets or exceeds the given requirements.
     */
    public boolean isCompatibleWith(@NonNull ArchFilter required) {
        if (mHostOs != null
                && required.mHostOs != null
                && !mHostOs.equals(required.mHostOs)) {
            return false;
        }

        if (mHostBits != null
                && required.mHostBits != null
                && !mHostBits.equals(required.mHostBits)) {
            return false;
        }

        if (mJvmBits != null
                && required.mJvmBits != null
                && !mJvmBits.equals(required.mJvmBits)) {
            return false;
        }

        if (mMinJvmVersion != null
                && required.mMinJvmVersion != null
                && mMinJvmVersion.compareTo(required.mMinJvmVersion) < 0) {
            return false;
        }

        return true;
    }

    /**
     * Returns an {@link ArchFilter} that represents the current host platform.
     * @return an {@link ArchFilter} that represents the current host platform.
     */
    @NonNull
    public static ArchFilter getCurrent() {
        String os = System.getProperty("os.name");          //$NON-NLS-1$
        HostOs  hostOS = null;
        if (os.startsWith("Mac")) {                         //$NON-NLS-1$
            hostOS = HostOs.MACOSX;
        } else if (os.startsWith("Windows")) {              //$NON-NLS-1$
            hostOS = HostOs.WINDOWS;
        } else if (os.startsWith("Linux")) {                //$NON-NLS-1$
            hostOS = HostOs.LINUX;
        }

        BitSize jvmBits;
        String arch = System.getProperty("os.arch");        //$NON-NLS-1$

        if (arch.equalsIgnoreCase("x86_64")   ||            //$NON-NLS-1$
                arch.equalsIgnoreCase("ia64") ||            //$NON-NLS-1$
                arch.equalsIgnoreCase("amd64")) {           //$NON-NLS-1$
            jvmBits = BitSize._64;
        } else {
            jvmBits = BitSize._32;
        }

        // TODO figure out the host bit size.
        // When jvmBits is 64 we know it's surely 64
        // but that's not necessarily obvious when jvmBits is 32.
        BitSize hostBits = jvmBits;

        NoPreviewRevision minJvmVersion = null;
        String javav = System.getProperty("java.version");              //$NON-NLS-1$
        // java Version is typically in the form "1.2.3_45" and we just need to keep up to "1.2.3"
        // since our revision numbers are in 3-parts form (1.2.3).
        Pattern p = Pattern.compile("((\\d+)(\\.\\d+)?(\\.\\d+)?).*");  //$NON-NLS-1$
        Matcher m = p.matcher(javav);
        if (m.matches()) {
            minJvmVersion = NoPreviewRevision.parseRevision(m.group(1));
        }

        return new ArchFilter(hostOS, hostBits, jvmBits, minJvmVersion);
    }
}
