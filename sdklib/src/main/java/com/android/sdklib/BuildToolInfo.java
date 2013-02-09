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

package com.android.sdklib;

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.android.sdklib.repository.FullRevision;
import com.android.utils.ILogger;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.Map;



/**
 * Information on a specific build-tool folder.
 */
public class BuildToolInfo {

    public enum PathId {
        /** OS Path to the target's version of the aapt tool.
          * This is deprecated as aapt is now in the platform tools and not in the platform. */
        AAPT,
        /** OS Path to the target's version of the aidl tool.
          * This is deprecated as aidl is now in the platform tools and not in the platform. */
        AIDL,
        /** OS Path to the target's version of the dx too.<br>
         * This is deprecated as dx is now in the platform tools and not in the platform. */
        DX,
        /** OS Path to the target's version of the dx.jar file.<br>
         * This is deprecated as dx.jar is now in the platform tools and not in the platform. */
        DX_JAR,
        /** OS Path to the "ant" folder which contains the ant build rules (ver 2 and above) */
        ANT,
// FIXME: do we want to put the rs tool in build-tools too?
        /** OS Path to the Renderscript include folder.
          * This is deprecated as this is now in the platform tools and not in the platform. */
        ANDROID_RS,
        /** OS Path to the Renderscript(clang) include folder.
          * This is deprecated as this is now in the platform tools and not in the platform. */
        ANDROID_RS_CLANG,
    }

    /** The build-tool revision. */
    private final FullRevision mRevision;
    /** The path to the build-tool folder specific to this revision. */
    private final File mPath;

    private final Map<PathId, String> mPaths = Maps.newEnumMap(PathId.class);

    public BuildToolInfo(FullRevision revision, File path) {
        mRevision = revision;
        mPath = path;

        add(PathId.ANT, SdkConstants.FD_ANT);

        add(PathId.AAPT, SdkConstants.FN_AAPT);
        add(PathId.AIDL, SdkConstants.FN_AIDL);
        add(PathId.DX, SdkConstants.FN_DX);
        add(PathId.DX_JAR, SdkConstants.FN_DX_JAR);
        add(PathId.ANDROID_RS, SdkConstants.OS_FRAMEWORK_RS);
        add(PathId.ANDROID_RS_CLANG, SdkConstants.OS_FRAMEWORK_RS_CLANG);

    }

    private void add(PathId id, String leaf) {
        File f = new File(mPath, leaf);
        String str = f.getAbsolutePath();
        if (f.isDirectory() && str.charAt(str.length() - 1) != File.separatorChar) {
            str += File.separatorChar;
        }
        mPaths.put(id, str);
    }

    /**
     * Returns the revision.
     */
    public FullRevision getRevision() {
        return mRevision;
    }

    /**
     * Returns the build-tool revision-specific folder.
     * <p/>
     * For compatibility reasons, use {@link #getPath(PathId)} if you need the path to a
     * specific tool.
     */
    File getLocation() {
        return mPath;
    }

    /**
     * Returns the path of a build-tool component.
     *
     * @param pathId the id representing the path to return.
     *          Any of the constants defined in {@link BuildToolInfo} can be used.
     * @return The absolute path for that tool, with a / separator if it's a folder.
     *         Null if the path-id is unknown.
     */
    String getPath(PathId pathId) {
        return mPaths.get(pathId);
    }

    /**
     * Checks whether the build-tool is valid by verifying that the expected binaries
     * are actually present. This checks that all known paths point to a valid file
     * or directory.
     *
     * @param log An optional logger. If non-null, errors will be printed there.
     * @return True if the build-tool folder contains all the expected tools.
     */
    public boolean isValid(@Nullable ILogger log) {
        for (Map.Entry<PathId, String> entry : mPaths.entrySet()) {
            File f = new File(entry.getValue());
            if (!f.exists()) {
                if (log != null) {
                    log.warning("Build-tool %1$s is missing %2$s",
                            mRevision.toString(),
                            entry.getKey());
                }
                return false;
            }
        }
        return true;
    }

}
