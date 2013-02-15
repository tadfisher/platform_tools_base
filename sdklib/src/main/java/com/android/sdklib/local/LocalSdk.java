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
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.AndroidVersion.AndroidVersionException;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.repository.packages.PackageParserUtils;
import com.android.sdklib.io.FileOp;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.MajorRevision;
import com.android.sdklib.repository.NoPreviewRevision;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

    /** Filter the SDK/tools folder.
     *  Has {@link FullRevision}. */
    public static final int PKG_TOOLS          = 0x0001;
    /** Filter the SDK/platform-tools folder.
     *  Has {@link FullRevision}. */
    public static final int PKG_PLATFORM_TOOLS = 0x0002;
    /** Filter the SDK/build-tools folder.
     *  Has {@link FullRevision}. */
    public static final int PKG_BUILD_TOOLS    = 0x0004;

    /** Filter the SDK/docs folder.
     *  Has {@link MajorRevision}. */
    public static final int PKG_DOCS           = 0x0010;
    /** Filter the SDK/extras folder.
     *  Has {@link MajorRevision}. */
    public static final int PKG_EXTRAS         = 0x0020;

    /** Filter the SDK/platforms.
     *  Has {@link AndroidVersion}. Has {@link MajorRevision}. */
    public static final int PKG_PLATFORMS      = 0x0100;
    /** Filter the SDK/sys-images.
     * Has {@link AndroidVersion}. Has {@link MajorRevision}. */
    public static final int PKG_SYS_IMAGES     = 0x0200;
    /** Filter the SDK/addons.
     *  Has {@link AndroidVersion}. Has {@link MajorRevision}. */
    public static final int PKG_ADDONS         = 0x0400;
    /** Filter the SDK/samples folder.
     *  Note: this will not detect samples located in the SDK/extras packages.
     *  Has {@link AndroidVersion}. Has {@link MajorRevision}. */
    public static final int PKG_SAMPLES        = 0x0800;
    /** Filter the SDK/sources folder.
     *  Has {@link AndroidVersion}. Has {@link MajorRevision}. */
    public static final int PKG_SOURCES        = 0x1000;

    /** Location of the SDK. Maybe null. Can be changed. */
    private File mSdkRoot;
    /** File operation object. (Used for overriding in mock testing.) */
    private final FileOp mFileOp;
    /** List of package information loaded so far. Lazily populated. */
    private final Multimap<Integer, LocalPkgInfo> mLocalPackages = TreeMultimap.create();
    /** Directories already parsed into {@link #mLocalPackages}. */
    private final Multimap<Integer, DirInfo> mVisitedDirs = HashMultimap.create();

    private final static Map<Integer, String> sFolderName = Maps.newHashMap();

    static {
        sFolderName.put(PKG_TOOLS,          SdkConstants.FD_TOOLS);
        sFolderName.put(PKG_PLATFORM_TOOLS, SdkConstants.FD_PLATFORM_TOOLS);
        sFolderName.put(PKG_BUILD_TOOLS,    SdkConstants.FD_BUILD_TOOLS);
        sFolderName.put(PKG_DOCS,           SdkConstants.FD_DOCS);
        sFolderName.put(PKG_PLATFORMS,      SdkConstants.FD_PLATFORMS);
        sFolderName.put(PKG_SYS_IMAGES,     SdkConstants.FD_SYSTEM_IMAGES);
        sFolderName.put(PKG_ADDONS,         SdkConstants.FD_ADDONS);
        sFolderName.put(PKG_SAMPLES,        SdkConstants.FD_SAMPLES);
        sFolderName.put(PKG_EXTRAS,         SdkConstants.FD_EXTRAS);
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

    public FileOp getFileOp() {
        return mFileOp;
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

    /**
     * Clear the tracked visited folders & the cached {@link LocalPkgInfo} for the
     * given filter types.
     *
     * @param filters An OR of the PKG_ constants or {@link #PKG_ALL} to clear everything.
     */
    public void clearLocalPkg(int filters) {
        int minf = Integer.lowestOneBit(filters);
        for (int filter = minf; filters != 0 && filter <= PKG_ALL; filter <<= 1) {
            if ((filters & filter) == 0) {
                continue;
            }
            filters ^= filter;
            mVisitedDirs.removeAll(filter);
            mLocalPackages.removeAll(filter);
        }
    }

    /**
     * Check the tracked visited folders to see if anything has changed for the
     * requested filter types.
     * This does not refresh or reload any package information.
     *
     * @param filters An OR of the PKG_ constants or {@link #PKG_ALL} to clear everything.
     */
    public boolean hasChanged(int filters) {
        int minf = Integer.lowestOneBit(filters);
        for (int filter = minf; filters != 0 && filter <= PKG_ALL; filter <<= 1) {
            if ((filters & filter) == 0) {
                continue;
            }
            filters ^= filter;

            for(DirInfo dirInfo : mVisitedDirs.get(filter)) {
                if (dirInfo.hasChanged()) {
                    return true;
                }
            }
        }

        return false;
    }



    //--------- Generic querying ---------

    /**
     * Retrieves information on a package identified by an {@link AndroidVersion}.
     *
     * @param filter {@link #PKG_PLATFORMS}, {@link #PKG_ADDONS}, {@link #PKG_SYS_IMAGES},
     *               {@link #PKG_SAMPLES} or {@link #PKG_SOURCES},
     * @param version The {@link AndroidVersion} specific for this package type.
     * @return An existing package information or null if not found.
     */
    public LocalPkgInfo getPkgInfo(int filter, AndroidVersion version) {
        for (LocalPkgInfo pkg : getPkgsInfos(filter)) {
            if (pkg instanceof LocalAndroidVersionPkgInfo) {
                LocalAndroidVersionPkgInfo p = (LocalAndroidVersionPkgInfo) pkg;
                if (p.getAndroidVersion().equals(version)) {
                    return p;
                }
            }
        }

        return null;
    }

    /**
     * Retrieves information on a package identified by its {@link FullRevision}.
     * <p/>
     * Note that {@link #PKG_TOOLS} and {@link #PKG_PLATFORM_TOOLS} are unique in a local SDK
     * so you'll want to use {@link #getPkgInfo(int)} to retrieve them instead.
     *
     * @param filter {@link #PKG_TOOLS}, {@link #PKG_PLATFORM_TOOLS} or {@link #PKG_BUILD_TOOLS}.
     * @param revision The {@link FullRevision} uniquely identifying this package.
     * @return An existing package information or null if not found.
     */
    public LocalPkgInfo getPkgInfo(int filter, FullRevision revision) {
        for (LocalPkgInfo pkg : getPkgsInfos(filter)) {
            if (pkg instanceof LocalFullRevisionPkgInfo) {
                LocalFullRevisionPkgInfo p = (LocalFullRevisionPkgInfo) pkg;
                if (p.getFullRevision().equals(revision)) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves information on a package identified by its {@link MajorRevision}.
     * <p/>
     * Note that {@link #PKG_DOCS} is unique in a local SDK
     * so you'll want to use {@link #getPkgInfo(int)} to retrieve it instead.
     * <p/>
     * Extras are better retrieved using #TODO
     *
     * @param filter {@link #PKG_DOCS} or {@link #PKG_EXTRAS}.
     * @param revision The {@link MajorRevision} uniquely identifying this package.
     * @return An existing package information or null if not found.
     */
    public LocalPkgInfo getPkgInfo(int filter, MajorRevision revision) {
       for (LocalPkgInfo pkg : getPkgsInfos(filter)) {
           if (pkg instanceof LocalMajorRevisionPkgInfo) {
               LocalMajorRevisionPkgInfo p = (LocalMajorRevisionPkgInfo) pkg;
               if (p.getFullRevision().equals(revision)) {
                   return p;
               }
           }
       }
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

        if (!mVisitedDirs.containsEntry(filter, uniqueDir)) {
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
            mVisitedDirs.put(filter, new DirInfo(uniqueDir));
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
     * <p/>
     * To force the LocalSdk parser to load <b>everything</b>, simply call this method
     * with the {@link #PKG_ALL} argument to load all the known package types.
     *
     * @param filters One or more of {@link #PKG_ADDONS}, {@link #PKG_PLATFORMS},
     *                               {@link #PKG_BUILD_TOOLS}, {@link #PKG_EXTRAS},
     *                               {@link #PKG_SOURCES}, {@link #PKG_SYS_IMAGES}
     * @return A list (possibly empty) of matching installed packages. Never returns null.
     */
    public LocalPkgInfo[] getPkgsInfos(int filters) {

        ArrayList<LocalPkgInfo> list = Lists.newArrayList();

        int minf = Integer.lowestOneBit(filters);

        for (int filter = minf; filters != 0 && filter <= PKG_ALL; filter <<= 1) {
            if ((filters & filter) == 0) {
                continue;
            }
            filters ^= filter;

            switch(filter) {
            case PKG_TOOLS:
            case PKG_PLATFORM_TOOLS:
            case PKG_DOCS:
                LocalPkgInfo info = getPkgInfo(filter);
                if (info != null) {
                    list.add(info);
                }
                break;

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

                if (!mVisitedDirs.containsEntry(filter, rootDir)) {
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
                    mVisitedDirs.put(filter, new DirInfo(rootDir));
                    list.addAll(existing);
                }
                break;
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
        LocalPkgInfo[] pkgs = getPkgsInfos(PKG_BUILD_TOOLS);

        // Note: the pkgs come from a TreeMultimap so they should already be sorted.
        // Just in case, sort them again, which quick sort should be a mostly-no-op.
        Arrays.sort(pkgs);

        // LocalBuildToolPkgInfo's comparator sorts on its FullRevision so we just
        // need to take the latest element.
        if (pkgs.length > 0) {
            LocalPkgInfo pkg = pkgs[pkgs.length - 1];
            if (pkg instanceof LocalBuildToolPkgInfo) {
                return ((LocalBuildToolPkgInfo) pkg).getBuildToolInfo();
            }
        }
        return null;
    }

    /**
     * Returns the {@link BuildToolInfo} for the given revision.
     *
     * @param revision The requested revision.
     * @return A {@link BuildToolInfo}. Can be null if {@code revision} is null or is
     *  not part of the known set returned by {@link #getPkgsInfos(PKG_BUILD_TOOLS)}.
     */
    @Nullable
    public BuildToolInfo getBuildTool(@Nullable FullRevision revision) {
        LocalPkgInfo pkg = getPkgInfo(PKG_BUILD_TOOLS, revision);
        if (pkg instanceof LocalBuildToolPkgInfo) {
            return ((LocalBuildToolPkgInfo) pkg).getBuildToolInfo();
        }
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

        boolean isPlatform = AndroidTargetHash.isPlatform(hash);
        LocalPkgInfo[] pkgs = getPkgsInfos(isPlatform ? PKG_PLATFORMS : PKG_ADDONS);

        for (LocalPkgInfo pkg : pkgs) {
            if (pkg instanceof LocalPlatformPkgInfo) {
                IAndroidTarget target = ((LocalPlatformPkgInfo) pkg).getAndroidTarget();
                if (target != null && hash.equals(AndroidTargetHash.getTargetHashString(target))) {
                    return target;
                }
            }
        }

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

        LocalToolPkgInfo info = new LocalToolPkgInfo(this, toolFolder, props, rev);

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

        LocalPlatformToolPkgInfo info = new LocalPlatformToolPkgInfo(this, ptFolder, props, rev);
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

        LocalDocPkgInfo info = new LocalDocPkgInfo(this, docFolder, props, rev);

        // To start with, a doc folder should have an "index.html" to be acceptable.
        // We don't actually check the content of the file.
        if (!mFileOp.isFile(new File(docFolder, "index.html"))) {
            info.appendLoadError("Missing index.html");
        }
        return info;
    }

    private void scanBuildTools(File collectionDir, Collection<LocalPkgInfo> outCollection) {
        // The build-tool root folder contains a list of per-revision folders.
        for (File buildToolDir : mFileOp.listFiles(collectionDir)) {
            if (!mFileOp.isDirectory(buildToolDir) ||
                    mVisitedDirs.containsEntry(PKG_BUILD_TOOLS, buildToolDir)) {
                continue;
            }
            mVisitedDirs.put(PKG_BUILD_TOOLS, new DirInfo(buildToolDir));

            Properties props = parseProperties(new File(buildToolDir, SdkConstants.FN_SOURCE_PROP));
            FullRevision rev = PackageParserUtils.getPropertyFullRevision(props);
            if (rev == null) {
                continue; // skip, no revision
            }

            BuildToolInfo btInfo = new BuildToolInfo(rev, buildToolDir);
            LocalBuildToolPkgInfo pkgInfo =
                new LocalBuildToolPkgInfo(this, buildToolDir, props, rev, btInfo);
            outCollection.add(pkgInfo);
        }
    }

    private void scanPlatforms(File collectionDir, Collection<LocalPkgInfo> outCollection) {
        for (File platformDir : mFileOp.listFiles(collectionDir)) {
            if (!mFileOp.isDirectory(platformDir) ||
                    !mVisitedDirs.containsEntry(PKG_PLATFORMS, platformDir)) {
                continue;
            }
            mVisitedDirs.put(PKG_PLATFORMS, new DirInfo(platformDir));

            Properties props = parseProperties(new File(platformDir, SdkConstants.FN_SOURCE_PROP));
            MajorRevision rev = PackageParserUtils.getPropertyMajorRevision(props);
            if (rev == null) {
                continue; // skip, no revision
            }

            try {
                AndroidVersion vers = new AndroidVersion(props);

                LocalPlatformPkgInfo pkgInfo =
                    new LocalPlatformPkgInfo(this, platformDir, props, vers, rev);
                outCollection.add(pkgInfo);

            } catch (AndroidVersionException e) {
                continue; // skip invalid or missing android version.
            }
        }
    }

    private void scanAddons(File collectionDir, Collection<LocalPkgInfo> outCollection) {
        for (File addonDir : mFileOp.listFiles(collectionDir)) {
            if (!mFileOp.isDirectory(addonDir) ||
                    !mVisitedDirs.containsEntry(PKG_ADDONS, addonDir)) {
                continue;
            }
            mVisitedDirs.put(PKG_ADDONS, new DirInfo(addonDir));

            Properties props = parseProperties(new File(addonDir, SdkConstants.FN_SOURCE_PROP));
            MajorRevision rev = PackageParserUtils.getPropertyMajorRevision(props);
            if (rev == null) {
                continue; // skip, no revision
            }

            try {
                AndroidVersion vers = new AndroidVersion(props);

                LocalPlatformPkgInfo pkgInfo =
                    new LocalPlatformPkgInfo(this, addonDir, props, vers, rev);
                outCollection.add(pkgInfo);

            } catch (AndroidVersionException e) {
                continue; // skip invalid or missing android version.
            }
        }
    }

    private void scanSysImages(File collectionDir, Collection<LocalPkgInfo> outCollection) {
        for (File platformDir : mFileOp.listFiles(collectionDir)) {
            if (!mFileOp.isDirectory(platformDir) ||
                    mVisitedDirs.containsEntry(PKG_SYS_IMAGES, platformDir)) {
                continue;
            }
            mVisitedDirs.put(PKG_SYS_IMAGES, new DirInfo(platformDir));

            for (File abiDir : mFileOp.listFiles(platformDir)) {
                if (!mFileOp.isDirectory(abiDir) ||
                        mVisitedDirs.containsEntry(PKG_SYS_IMAGES, abiDir)) {
                    continue;
                }
                mVisitedDirs.put(PKG_SYS_IMAGES, new DirInfo(abiDir));

                Properties props = parseProperties(new File(abiDir, SdkConstants.FN_SOURCE_PROP));
                MajorRevision rev = PackageParserUtils.getPropertyMajorRevision(props);
                if (rev == null) {
                    continue; // skip, no revision
                }

                try {
                    AndroidVersion vers = new AndroidVersion(props);

                    LocalSysImgPkgInfo pkgInfo =
                        new LocalSysImgPkgInfo(this, abiDir, props, vers, abiDir.getName(), rev);
                    outCollection.add(pkgInfo);

                } catch (AndroidVersionException e) {
                    continue; // skip invalid or missing android version.
                }
            }
        }
    }

    private void scanSamples(File collectionDir, Collection<LocalPkgInfo> outCollection) {
        for (File platformDir : mFileOp.listFiles(collectionDir)) {
            if (!mFileOp.isDirectory(platformDir) ||
                    !mVisitedDirs.containsEntry(PKG_SAMPLES, platformDir)) {
                continue;
            }
            mVisitedDirs.put(PKG_SAMPLES, new DirInfo(platformDir));

            Properties props = parseProperties(new File(platformDir, SdkConstants.FN_SOURCE_PROP));
            MajorRevision rev = PackageParserUtils.getPropertyMajorRevision(props);
            if (rev == null) {
                continue; // skip, no revision
            }

            try {
                AndroidVersion vers = new AndroidVersion(props);

                LocalSamplePkgInfo pkgInfo =
                    new LocalSamplePkgInfo(this, platformDir, props, vers, rev);
                outCollection.add(pkgInfo);
            } catch (AndroidVersionException e) {
                continue; // skip invalid or missing android version.
            }
        }
    }

    private void scanSources(File collectionDir, Collection<LocalPkgInfo> outCollection) {
        // The build-tool root folder contains a list of per-revision folders.
        for (File platformDir : mFileOp.listFiles(collectionDir)) {
            if (!mFileOp.isDirectory(platformDir) ||
                    mVisitedDirs.containsEntry(PKG_SOURCES, platformDir)) {
                continue;
            }
            mVisitedDirs.put(PKG_SOURCES, new DirInfo(platformDir));

            Properties props = parseProperties(new File(platformDir, SdkConstants.FN_SOURCE_PROP));
            MajorRevision rev = PackageParserUtils.getPropertyMajorRevision(props);
            if (rev == null) {
                continue; // skip, no revision
            }

            try {
                AndroidVersion vers = new AndroidVersion(props);

                LocalSourcePkgInfo pkgInfo =
                    new LocalSourcePkgInfo(this, platformDir, props, vers, rev);
                outCollection.add(pkgInfo);
            } catch (AndroidVersionException e) {
                continue; // skip invalid or missing android version.
            }
        }
    }

    private void scanExtras(File collectionDir, Collection<LocalPkgInfo> outCollection) {
        for (File vendorDir : mFileOp.listFiles(collectionDir)) {
            if (!vendorDir.isDirectory() || mVisitedDirs.containsEntry(PKG_EXTRAS, vendorDir)) {
                continue;
            }
            mVisitedDirs.put(PKG_EXTRAS, new DirInfo(vendorDir));

            for (File extraDir : mFileOp.listFiles(vendorDir)) {
                if (!mFileOp.isDirectory(extraDir) ||
                        mVisitedDirs.containsEntry(PKG_EXTRAS, extraDir)) {
                    continue;
                }
                mVisitedDirs.put(PKG_EXTRAS, new DirInfo(extraDir));

                Properties props = parseProperties(new File(extraDir, SdkConstants.FN_SOURCE_PROP));
                NoPreviewRevision rev = PackageParserUtils.getPropertyNoPreviewRevision(props);
                if (rev == null) {
                    continue; // skip, no revision
                }

                LocalExtraPkgInfo pkgInfo = new LocalExtraPkgInfo(this,
                                                                  extraDir,
                                                                  props,
                                                                  vendorDir.getName(),
                                                                  extraDir.getName(),
                                                                  rev);
                outCollection.add(pkgInfo);
            }
        }
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

        @Override
        public int hashCode() {
            return mDir.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return mDir.equals(obj);
        };
    }

}
