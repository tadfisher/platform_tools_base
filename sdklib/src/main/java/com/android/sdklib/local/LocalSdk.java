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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.annotations.VisibleForTesting.Visibility;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.AndroidVersion.AndroidVersionException;
import com.android.sdklib.internal.repository.archives.Archive.Arch;
import com.android.sdklib.internal.repository.archives.Archive.Os;
import com.android.sdklib.internal.repository.packages.BuildToolPackage;
import com.android.sdklib.internal.repository.packages.DocPackage;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.internal.repository.packages.PackageParserUtils;
import com.android.sdklib.internal.repository.packages.PlatformToolPackage;
import com.android.sdklib.internal.repository.packages.SourcePackage;
import com.android.sdklib.internal.repository.packages.ToolPackage;
import com.android.sdklib.io.FileOp;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.MajorRevision;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.Adler32;

/**
 * This class keeps information on the current locally installed SDK.
 * It tries to lazily load information as much as possible.
 * <p/>
 * Apps/libraries that use it are encouraged to keep an existing instance around
 * (using a singleton or similar mechanism).
 *
 * @version 2 of the {@code SdkManager} class, essentially.
 */
public class LocalSdk {

    /** Filter all SDK folders. */
    public static final int PKG_ALL            = 0xFFFF;
    /** Filter the SDK/tools folder. */
    public static final int PKG_TOOLS          = 0x0001;
    /** Filter the SDK/platform-tools folder */
    public static final int PKG_PLATFORM_TOOLS = 0x0002;
    /** Filter the SDK/build-tools folder. */
    public static final int PKG_BUILD_TOOLS    = 0x0004;
    /** Filter the SDK/docs folder. */
    public static final int PKG_DOCS           = 0x0008;
    /** Filter the SDK/platforms. */
    public static final int PKG_PLATFORMS      = 0x0010;
    /** Filter the SDK/sys-images. */
    public static final int PKG_SYS_IMAGES     = 0x0020;
    /** Filter the SDK/addons. */
    public static final int PKG_ADDONS         = 0x0040;
    /** Filter the SDK/samples folder.
     * Note: this will not detect samples located in the SDK/extras packages. */
    public static final int PKG_SAMPLES        = 0x0080;
    /** Filter the SDK/sources folder. */
    public static final int PKG_SOURCES        = 0x0100;
    /** Filter the SDK/extras folder. */
    public static final int PKG_EXTRAS         = 0x0200;

    /** Location of the SDK. Maybe null. Can be changed. */
    private File mSdkRoot;
    /** File operation object. (Used for overriding in mock testing.) */
    private final FileOp mFileOp;
    /** List of package information loaded so far. Lazily populated. */
    private final Multimap<Integer, LocalPkgInfo> mLocalPackages = TreeMultimap.create();
    /** Directories already parsed into {@link #mLocalPackages}. */
    private final HashMap<File, DirInfo> mVisitedDirs = Maps.newHashMap();

    private final static Map<Integer, String> sFolderName = Maps.newHashMap();

    static {
        sFolderName.put(PKG_TOOLS, SdkConstants.FD_TOOLS);
        sFolderName.put(PKG_PLATFORM_TOOLS, SdkConstants.FD_PLATFORM_TOOLS);
        sFolderName.put(PKG_BUILD_TOOLS, SdkConstants.FD_BUILD_TOOLS);
        sFolderName.put(PKG_DOCS, SdkConstants.FD_DOCS);
        sFolderName.put(PKG_PLATFORMS, SdkConstants.FD_PLATFORMS);
        sFolderName.put(PKG_SYS_IMAGES, SdkConstants.FD_SYSTEM_IMAGES);
        sFolderName.put(PKG_ADDONS, SdkConstants.FD_ADDONS);
        sFolderName.put(PKG_SAMPLES, SdkConstants.FD_SAMPLES);
        sFolderName.put(PKG_EXTRAS, SdkConstants.FD_EXTRAS);
    }

    /**
     * Creates an initial LocalSdk instance with an unknown location.
     */
    public LocalSdk() {
        mFileOp = new FileOp();
    }

    /**
     * Creates an initial LocalSdk instance for a known SDK location.
     */
    public LocalSdk(@NonNull File sdkRoot) {
        this();
        setLocation(sdkRoot);
    }

    /**
     * Creates an initial LocalSdk instance with an unknown location.
     * This is designed for unit tests to override the {@link FileOp} being used.
     *
     * @param fileOp The alternate {@link FileOp} to use for all file-based interactions.
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    protected LocalSdk(@NonNull FileOp fileOp) {
        mFileOp = fileOp;
    }

    public void setLocation(@NonNull File sdkRoot) {
        assert sdkRoot != null;
        mSdkRoot = sdkRoot;
        // TODO reload existing info
        sendSdkChanged();
    }

    /** Location of the SDK. Maybe null. Can be changed. */
    @Nullable
    public File getLocation() {
        return mSdkRoot;
    }

    //--------- Generic querying ---------

    /**
     *
     * @param filter {@link #PKG_PLATFORMS}, {@link #PKG_ADDONS}, {@link #PKG_SAMPLES}.
     * @param version
     * @return
     */
    public LocalPkgInfo getPkgInfo(int filter, AndroidVersion version) {
        return null;
    }

    /**
     *
     * @param filter .
     * @param version
     * @return
     */
    public LocalPkgInfo getPkgInfo(int filter, FullRevision revision) {
        return null;
    }

    /**
     * For unique local packages.
     * Returns the cached LocalPkgInfo for the requested type.
     * Loads it from disk if not cached.
     *
     * @param filter {@link #PKG_TOOLS} or {@link #PKG_PLATFORM_TOOLS} or {@link #PKG_DOCS}.
     * @return null if the package is not installed.
     */
    public LocalPkgInfo getPkgInfo(int filter) {
        switch(filter) {
        case PKG_TOOLS:
        case PKG_PLATFORM_TOOLS:
        case PKG_DOCS:
            break;
        default:
            return null;
        }

        Collection<LocalPkgInfo> existing = mLocalPackages.get(filter);
        assert existing.size() == 0 || existing.size() == 1;
        if (existing.size() > 0) {
            return existing.iterator().next();
        }

        File uniqueDir = new File(mSdkRoot, sFolderName.get(filter));
        LocalPkgInfo info = null;

        if (!mVisitedDirs.containsKey(uniqueDir)) {
            switch(filter) {
            case PKG_TOOLS:
                info = scanTools(uniqueDir);
                break;
            case PKG_PLATFORM_TOOLS:
                info = scanPlatformTools(uniqueDir);
                break;
            case PKG_DOCS:
                info = scanDoc(uniqueDir);
                break;
            }
        }

        if (uniqueDir != null) {
            // Whether we have found a valid pkg or not, this directory has been visited.
            mVisitedDirs.put(uniqueDir, new DirInfo(uniqueDir));
        }
        if (info != null) {
            mLocalPackages.put(filter, info);
        }

        return info;
    }

    /**
     * Retrieve all the info about the requested package type.
     * This is used for the package types that have one or more instances, each with different
     * versions.
     *
     * @param filter One or more of {@link #PKG_ADDONS}, {@link #PKG_PLATFORMS},
     *                              {@link #PKG_BUILD_TOOLS}, {@link #PKG_EXTRAS},
     *                              {@link #PKG_SOURCES}, {@link #PKG_SYS_IMAGES}
     * @return
     */
    public LocalPkgInfo[] getPkgsInfos(int filter) {

        ArrayList<LocalPkgInfo> list = Lists.newArrayList();

        for (int type = 1; type <= PKG_ALL; type <<= 1) {
            if ((filter & type) == 0) {
                continue;
            }

            switch(filter) {
            case PKG_TOOLS:
            case PKG_PLATFORM_TOOLS:
            case PKG_DOCS:
                LocalPkgInfo info = getPkgInfo(type);
                if (info != null) {
                    list.add(info);
                }

            case PKG_BUILD_TOOLS:
            case PKG_PLATFORMS:
            case PKG_SYS_IMAGES:
            case PKG_ADDONS:
            case PKG_SAMPLES:
            case PKG_SOURCES:
            case PKG_EXTRAS:

                Collection<LocalPkgInfo> existing = mLocalPackages.get(filter);
                if (existing.size() > 0) {
                    list.addAll(existing);
                    continue;
                }

                File rootDir = new File(mSdkRoot, sFolderName.get(filter));

                if (!mVisitedDirs.containsKey(rootDir)) {
                    switch(filter) {
                    case PKG_BUILD_TOOLS:
                        scanBuildTools(rootDir, existing);
                        break;
                    case PKG_PLATFORMS:
                        scanPlatforms(rootDir, existing);
                        break;
                    case PKG_SYS_IMAGES:
                        scanSysImages(rootDir, existing);
                        break;
                    case PKG_ADDONS:
                        scanAddons(rootDir, existing);
                        break;
                    case PKG_SAMPLES:
                        scanSamples(rootDir, existing);
                        break;
                    case PKG_SOURCES:
                        scanSources(rootDir, existing);
                        break;
                    case PKG_EXTRAS:
                        scanExtras(rootDir, existing);
                        break;
                    }
                    mVisitedDirs.put(rootDir, new DirInfo(rootDir));
                    list.addAll(existing);
                }
            }
        }

        return list.toArray(new LocalPkgInfo[list.size()]);
    }

    //---------- Package-specific querying --------

    /**
     * Returns the highest build-tool revision known. Can be null.
     *
     * @return The highest build-tool revision known, or null.
     */
    @Nullable
    public BuildToolInfo getLatestBuildTool() {
        return null;
    }


    /**
     * Returns the {@link BuildToolInfo} for the given revision.
     *
     * @param revision The requested revision.
     * @return A {@link BuildToolInfo}. Can be null if {@code revision} is null or is
     *  not part of the known set returned by {@link #getBuildTools()}.
     */
    @Nullable
    public BuildToolInfo getBuildTool(@Nullable FullRevision revision) {
        return null;
    }

    /**
     * Returns a target from a hash that was generated by {@link IAndroidTarget#hashString()}.
     *
     * @param hash the {@link IAndroidTarget} hash string.
     * @return The matching {@link IAndroidTarget} or null.
     */
    @Nullable
    public IAndroidTarget getTargetFromHashString(@Nullable String hash) {
        return null;
    }

    //---------- Events ---------

    public interface OnSdkChanged {
        public void onSdkChanged(LocalSdk sdk);
    }

    private void sendSdkChanged() {
        // TODO Auto-generated method stub

    }

    // -------------

    /**
     * Try to find a tools package at the given location.
     * Returns null if not found.
     */
    private LocalToolPkgInfo scanTools(File toolFolder) {
        // Can we find some properties?
        Properties props = parseProperties(new File(toolFolder, SdkConstants.FN_SOURCE_PROP));
        FullRevision rev = PackageParserUtils.getPropertyFullRevision(props);
        if (rev == null) {
            return null;
        }

        LocalToolPkgInfo info = new LocalToolPkgInfo(rev);

        // We're not going to check that all tools are present. At the very least
        // we should expect to find android and an emulator adapted to the current OS.
        boolean hasEmulator = false;
        boolean hasAndroid = false;
        String android1 = SdkConstants.androidCmdName().replace(".bat", ".exe");
        String android2 = android1.indexOf('.') == -1 ? null : android1.replace(".exe", ".bat");
        File[] files = mFileOp.listFiles(toolFolder);
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (SdkConstants.FN_EMULATOR.equals(name)) {
                    hasEmulator = true;
                }
                if (android1.equals(name) || (android2 != null && android2.equals(name))) {
                    hasAndroid = true;
                }
            }
        }
        if (!hasAndroid) {
            info.appendLoadError("Missing %1$s", SdkConstants.androidCmdName());
        }
        if (!hasEmulator) {
            info.appendLoadError("Missing %1$s", SdkConstants.FN_EMULATOR);
        }

        // Create our package. use the properties if we found any.
        try {
            Package pkg = ToolPackage.create(
                    null,                       //source
                    props,                      //properties
                    0,                          //revision
                    null,                       //license
                    "Tools",                    //description
                    null,                       //descUrl
                    Os.getCurrentOs(),          //archiveOs
                    Arch.getCurrentArch(),      //archiveArch
                    toolFolder.getPath()        //archiveOsPath
                    );
            info.setPackage(pkg);
        } catch (Exception e) {
            info.appendLoadError("Failed to parse package: %1$s", e.toString());
        }

        return info;
    }

    /**
     * Try to find a platform-tools package at the given location.
     * Returns null if not found.
     */
    private LocalPlatformToolPkgInfo scanPlatformTools(File ptFolder) {
        // Can we find some properties?
        Properties props = parseProperties(new File(ptFolder, SdkConstants.FN_SOURCE_PROP));
        FullRevision rev = PackageParserUtils.getPropertyFullRevision(props);
        if (rev == null) {
            return null;
        }

        LocalPlatformToolPkgInfo info = new LocalPlatformToolPkgInfo(rev);

        // Create our package. use the properties if we found any.
        try {
            Package pkg = PlatformToolPackage.create(
                    null,                           //source
                    props,                          //properties
                    0,                              //revision
                    null,                           //license
                    "Platform Tools",               //description
                    null,                           //descUrl
                    Os.getCurrentOs(),              //archiveOs
                    Arch.getCurrentArch(),          //archiveArch
                    ptFolder.getPath()              //archiveOsPath
                    );
            info.setPackage(pkg);
        } catch (Exception e) {
            info.appendLoadError("Failed to parse package: %1$s", e.toString());
        }

        return info;
    }

    /**
     * Try to find a docs package at the given location.
     * Returns null if not found.
     */
    private LocalDocPkgInfo scanDoc(File docFolder) {
        // Can we find some properties?
        Properties props = parseProperties(new File(docFolder, SdkConstants.FN_SOURCE_PROP));
        MajorRevision rev = PackageParserUtils.getPropertyMajorRevision(props);
        if (rev == null) {
            return null;
        }

        LocalDocPkgInfo info = new LocalDocPkgInfo(rev);

        // To start with, a doc folder should have an "index.html" to be acceptable.
        // We don't actually check the content of the file.
        if (!mFileOp.isFile(new File(docFolder, "index.html"))) {
            info.appendLoadError("Missing index.html");
        }

        try {
            Package pkg = DocPackage.create(
                    null,                       //source
                    props,                      //properties
                    0,                          //apiLevel
                    null,                       //codename
                    0,                          //revision
                    null,                       //license
                    null,                       //description
                    null,                       //descUrl
                    Os.getCurrentOs(),          //archiveOs
                    Arch.getCurrentArch(),      //archiveArch
                    docFolder.getPath()         //archiveOsPath
                    );
            info.setPackage(pkg);
        } catch (Exception e) {
            info.appendLoadError("Failed to parse package: %1$s", e.toString());
        }

        return info;
    }

    private void scanBuildTools(File collectionDir, Collection<LocalPkgInfo> outCollection) {
        File[] subDirs = mFileOp.listFiles(collectionDir);
        if (subDirs == null) {
            return;
        }

        // The build-tool root folder contains a list of revisioned folders.
        for (File buildToolDir : subDirs) {
            if (mFileOp.isDirectory(buildToolDir) && !mVisitedDirs.containsKey(buildToolDir)) {
                mVisitedDirs.put(buildToolDir, new DirInfo(buildToolDir));

                // Ignore empty directories
                File[] srcFiles = mFileOp.listFiles(buildToolDir);
                if (srcFiles == null || srcFiles.length <= 0) {
                    continue;
                }
                Properties props =
                    parseProperties(new File(buildToolDir, SdkConstants.FN_SOURCE_PROP));
                FullRevision rev = PackageParserUtils.getPropertyFullRevision(props);
                if (rev == null) {
                    continue; // skip, no revision
                }

                BuildToolInfo btInfo = new BuildToolInfo(rev, buildToolDir);
                LocalBuildToolPkgInfo pkgInfo = new LocalBuildToolPkgInfo(rev, btInfo);
                outCollection.add(pkgInfo);

                try {
                    Package pkg = BuildToolPackage.create(buildToolDir, props);
                    pkgInfo.setPackage(pkg);
                } catch (Exception e) {
                    pkgInfo.appendLoadError("Failed to parse package: %1$s", e.toString());
                }
            }
        }
    }

    private void scanPlatforms(File collectionDir, Collection<LocalPkgInfo> outCollection) {
    }

    private void scanSysImages(File collectionDir, Collection<LocalPkgInfo> outCollection) {
    }

    private void scanAddons(File collectionDir, Collection<LocalPkgInfo> outCollection) {
    }

    private void scanSamples(File collectionDir, Collection<LocalPkgInfo> outCollection) {
    }

    private void scanSources(File collectionDir, Collection<LocalPkgInfo> outCollection) {
        File[] subDirs = mFileOp.listFiles(collectionDir);
        if (subDirs == null) {
            return;
        }

        // The build-tool root folder contains a list of revisioned folders.
        for (File platformDir : subDirs) {
            if (mFileOp.isDirectory(platformDir) && !mVisitedDirs.containsKey(platformDir)) {
                mVisitedDirs.put(platformDir, new DirInfo(platformDir));

                // Ignore empty directories
                File[] srcFiles = mFileOp.listFiles(platformDir);
                if (srcFiles == null || srcFiles.length <= 0) {
                    continue;
                }
                Properties props =
                    parseProperties(new File(platformDir, SdkConstants.FN_SOURCE_PROP));
                MajorRevision rev = PackageParserUtils.getPropertyMajorRevision(props);
                if (rev == null) {
                    continue; // skip, no revision
                }

                try {
                    AndroidVersion vers = new AndroidVersion(props);

                    LocalSourcePkgInfo pkgInfo = new LocalSourcePkgInfo(vers, rev);
                    outCollection.add(pkgInfo);

                    try {
                        Package pkg = SourcePackage.create(platformDir, props);
                        pkgInfo.setPackage(pkg);
                    } catch (Exception e) {
                        pkgInfo.appendLoadError("Failed to parse package: %1$s", e.toString());
                    }
                } catch (AndroidVersionException e) {
                    continue; // skip invalid or missing android version.
                }
            }
        }
    }

    private void scanExtras(File collectionDir, Collection<LocalPkgInfo> outCollection) {
    }

    /**
     * Parses the given file as properties file if it exists.
     * Returns null if the file does not exist, cannot be parsed or has no properties.
     */
    private Properties parseProperties(File propsFile) {
        InputStream fis = null;
        try {
            if (mFileOp.exists(propsFile)) {
                fis = mFileOp.newFileInputStream(propsFile);

                Properties props = new Properties();
                props.load(fis);

                // To be valid, there must be at least one property in it.
                if (props.size() > 0) {
                    return props;
                }
            }
        } catch (IOException e) {
            // Ignore
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {}
            }
        }
        return null;
    }

    // -------------

    private class DirInfo {
        @NonNull
        private final File mDir;
        private final long mDirModifiedTS;
        private final long mPropsModifiedTS;
        private final long mPropsChecksum;

        /**
         * Creates a new immutable {@link DirInfo}.
         *
         * @param dir The platform/addon directory of the target. It should be a directory.
         */
        public DirInfo(@NonNull File dir) {
            mDir = dir;
            mDirModifiedTS = mFileOp.lastModified(dir);

            // Capture some info about the source.properties file if it exists.
            // We use propsModifiedTS == 0 to mean there is no props file.
            long propsChecksum = 0;
            long propsModifiedTS = 0;
            File props = new File(dir, SdkConstants.FN_SOURCE_PROP);
            if (mFileOp.isFile(props)) {
                propsModifiedTS = mFileOp.lastModified(props);
                propsChecksum = getFileChecksum(props);
            }
            mPropsModifiedTS = propsModifiedTS;
            mPropsChecksum = propsChecksum;
        }

        /**
         * Checks whether the directory/source.properties attributes have changed.
         *
         * @return True if the directory modified timestamp or
         *  its source.property files have changed.
         */
        public boolean hasChanged() {
            // Does platform directory still exist?
            if (!mFileOp.isDirectory(mDir)) {
                return true;
            }
            // Has platform directory modified-timestamp changed?
            if (mDirModifiedTS != mFileOp.lastModified(mDir)) {
                return true;
            }

            File props = new File(mDir, SdkConstants.FN_SOURCE_PROP);

            // The directory did not have a props file if target was null or
            // if mPropsModifiedTS is 0.
            boolean hadProps = mPropsModifiedTS != 0;

            // Was there a props file and it vanished, or there wasn't and there's one now?
            if (hadProps != mFileOp.isFile(props)) {
                return true;
            }

            if (hadProps) {
                // Has source.props file modified-timestamp changed?
                if (mPropsModifiedTS != mFileOp.lastModified(props)) {
                    return true;
                }
                // Had the content of source.props changed?
                if (mPropsChecksum != getFileChecksum(props)) {
                    return true;
                }
            }

            return false;
        }

        /**
         * Computes an adler32 checksum (source.props are small files, so this
         * should be OK with an acceptable collision rate.)
         */
        private long getFileChecksum(@NonNull File file) {
            InputStream fis = null;
            try {
                fis = mFileOp.newFileInputStream(file);
                Adler32 a = new Adler32();
                byte[] buf = new byte[1024];
                int n;
                while ((n = fis.read(buf)) > 0) {
                    a.update(buf, 0, n);
                }
                return a.getValue();
            } catch (Exception ignore) {
            } finally {
                try {
                    if (fis != null) {
                        fis.close();
                    }
                } catch(Exception ignore) {}
            }
            return 0;
        }

        /** Returns a visual representation of this object for debugging. */
        @Override
        public String toString() {
            String s = String.format("<DirInfo %1$s TS=%2$d", mDir, mDirModifiedTS);  //$NON-NLS-1$
            if (mPropsModifiedTS != 0) {
                s += String.format(" | Props TS=%1$d, Chksum=%2$s",                   //$NON-NLS-1$
                        mPropsModifiedTS, mPropsChecksum);
            }
            return s + ">";                                                           //$NON-NLS-1$
        }
    }

}
