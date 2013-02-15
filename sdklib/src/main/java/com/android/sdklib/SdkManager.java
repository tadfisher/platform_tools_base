/*
 * Copyright (C) 2008 The Android Open Source Project
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
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.annotations.VisibleForTesting.Visibility;
import com.android.prefs.AndroidLocation;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.local.LocalExtraPkgInfo;
import com.android.sdklib.local.LocalPkgInfo;
import com.android.sdklib.local.LocalPlatformPkgInfo;
import com.android.sdklib.local.LocalSdk;
import com.android.sdklib.repository.FullRevision;
import com.android.utils.ILogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The SDK manager parses the SDK folder and gives access to the content.
 * @see PlatformTarget
 * @see AddOnTarget
 */
public class SdkManager {

    private static final boolean DEBUG = System.getenv("SDKMAN_DEBUG") != null;        //$NON-NLS-1$

//    @Deprecated // moved to local
//    public static final String PROP_VERSION_SDK = "ro.build.version.sdk";              //$NON-NLS-1$
//    @Deprecated // moved to local
//    public static final String PROP_VERSION_CODENAME = "ro.build.version.codename";    //$NON-NLS-1$
//    @Deprecated // moved to local
//    public static final String PROP_VERSION_RELEASE = "ro.build.version.release";      //$NON-NLS-1$
//
//    @Deprecated // moved to local
//    public static final String ADDON_NAME = "name";                                    //$NON-NLS-1$
//    @Deprecated // moved to local
//    public static final String ADDON_VENDOR = "vendor";                                //$NON-NLS-1$
//    @Deprecated // moved to local
//    public static final String ADDON_API = "api";                                      //$NON-NLS-1$
//    @Deprecated // moved to local
//    public static final String ADDON_DESCRIPTION = "description";                      //$NON-NLS-1$
//    @Deprecated // moved to local
//    public static final String ADDON_LIBRARIES = "libraries";                          //$NON-NLS-1$
//    @Deprecated // moved to local
//    public static final String ADDON_DEFAULT_SKIN = "skin";                            //$NON-NLS-1$
//    @Deprecated // moved to local
//    public static final String ADDON_USB_VENDOR = "usb-vendor";                        //$NON-NLS-1$
//    @Deprecated // moved to local
//    public static final String ADDON_REVISION = "revision";                            //$NON-NLS-1$
//    @Deprecated // moved to local
//    public static final String ADDON_REVISION_OLD = "version";                         //$NON-NLS-1$
//
//
//    @Deprecated // moved to local
//    private static final Pattern PATTERN_LIB_DATA = Pattern.compile(
//            "^([a-zA-Z0-9._-]+\\.jar);(.*)$", Pattern.CASE_INSENSITIVE);               //$NON-NLS-1$
//
//     // usb ids are 16-bit hexadecimal values.
//    @Deprecated // moved to local
//    private static final Pattern PATTERN_USB_IDS = Pattern.compile(
//            "^0x[a-f0-9]{4}$", Pattern.CASE_INSENSITIVE);                              //$NON-NLS-1$
//
//    /** List of items in the platform to check when parsing it. These paths are relative to the
//     * platform root folder. */
//    @Deprecated // moved to local
//    private static final String[] sPlatformContentList = new String[] {
//        SdkConstants.FN_FRAMEWORK_LIBRARY,
//        SdkConstants.FN_FRAMEWORK_AIDL,
//    };

    /** Preference file containing the usb ids for adb */
    private static final String ADB_INI_FILE = "adb_usb.ini";                          //$NON-NLS-1$
       //0--------90--------90--------90--------90--------90--------90--------90--------9
       private static final String ADB_INI_HEADER =
        "# ANDROID 3RD PARTY USB VENDOR ID LIST -- DO NOT EDIT.\n" +                   //$NON-NLS-1$
        "# USE 'android update adb' TO GENERATE.\n" +                                  //$NON-NLS-1$
        "# 1 USB VENDOR ID PER LINE.\n";                                               //$NON-NLS-1$

    /** Embedded reference to the new local SDK object. */
    private final LocalSdk mLocalSdk;

    /** Cache of targets from local sdk. See {@link #getTargets()}. */
    private IAndroidTarget[] mCachedTargets;

    /**
     * Create a new {@link SdkManager} instance.
     * External users should use {@link #createManager(String, ILogger)}.
     *
     * @param osSdkPath the location of the SDK.
     */
    @VisibleForTesting(visibility=Visibility.PRIVATE)
    protected SdkManager(@NonNull String osSdkPath) {
        mLocalSdk = new LocalSdk(new File(osSdkPath));
    }

    /**
     * Creates an {@link SdkManager} for a given sdk location.
     * @param osSdkPath the location of the SDK.
     * @param log the ILogger object receiving warning/error from the parsing.
     * @return the created {@link SdkManager} or null if the location is not valid.
     */
    @Nullable
    public static SdkManager createManager(
            @NonNull String osSdkPath,
            @NonNull ILogger log) {
        try {
            SdkManager manager = new SdkManager(osSdkPath);
            manager.reloadSdk(log);

            return manager;
        } catch (Throwable throwable) {
            log.error(throwable, "Error parsing the sdk.");
        }

        return null;
    }

    public LocalSdk getLocalSdk() {
        return mLocalSdk;
    }

    /**
     * Reloads the content of the SDK.
     *
     * @param log the ILogger object receiving warning/error from the parsing.
     */
    public void reloadSdk(@NonNull ILogger log) {
        mCachedTargets = null;
        mLocalSdk.clearLocalPkg(LocalSdk.PKG_ALL);
    }

    /**
     * Checks whether any of the SDK platforms/add-ons/build-tools have changed on-disk
     * since we last loaded the SDK. This does not reload the SDK nor does it
     * change the underlying targets.
     *
     * @return True if at least one directory or source.prop has changed.
     */
    public boolean hasChanged() {
        return hasChanged(null);
    }

    /**
     * Checks whether any of the SDK platforms/add-ons/build-tools have changed on-disk
     * since we last loaded the SDK. This does not reload the SDK nor does it
     * change the underlying targets.
     *
     * @param log An optional logger used to print verbose info on what changed. Can be null.
     * @return True if at least one directory or source.prop has changed.
     */
    public boolean hasChanged(@Nullable ILogger log) {
        return mLocalSdk.hasChanged(LocalSdk.PKG_PLATFORMS |
                                    LocalSdk.PKG_ADDONS |
                                    LocalSdk.PKG_BUILD_TOOLS);
    }

    /**
     * Returns the location of the SDK.
     */
    @NonNull
    public String getLocation() {
        File f = mLocalSdk.getLocation();
        // Our LocalSdk is created with a file path, so we know the location won't be null.
        assert f != null;
        return f.getPath();
    }

    /**
     * Returns the targets (platforms & addons) that are available in the SDK.
     * The target list is created on demand the first time then cached.
     * It will not refreshed unless {@link #reloadSdk(ILogger)} is called.
     * <p/>
     * The array can be empty but not null.
     */
    @NonNull
    public IAndroidTarget[] getTargets() {
        if (mCachedTargets == null) {
            LocalPkgInfo[] pkgsInfos = mLocalSdk.getPkgsInfos(LocalSdk.PKG_PLATFORMS |
                                                              LocalSdk.PKG_ADDONS);
            int n = pkgsInfos.length;
            List<IAndroidTarget> targets = new ArrayList<IAndroidTarget>(n);
            for (int i = 0; i < n; i++) {
                LocalPkgInfo info = pkgsInfos[i];
                assert info instanceof LocalPlatformPkgInfo;
                if (info instanceof LocalPlatformPkgInfo) {
                    targets.add(((LocalPlatformPkgInfo) info).getAndroidTarget());
                }
            }
            mCachedTargets = targets.toArray(new IAndroidTarget[targets.size()]);
        }
        return mCachedTargets;
    }

    /**
     * Returns an unmodifiable set of known build-tools revisions. Can be empty but not null.
     * Deprecated. I don't think anything uses this.
     */
    @Deprecated
    @NonNull
    public Set<FullRevision> getBuildTools() {
        LocalPkgInfo[] pkgs = mLocalSdk.getPkgsInfos(LocalSdk.PKG_BUILD_TOOLS);
        TreeSet<FullRevision> bt = new TreeSet<FullRevision>();
        for (LocalPkgInfo pkg : pkgs) {
            if (pkg.hasFullRevision()) {
                bt.add(pkg.getFullRevision());
            }
        }
        return Collections.unmodifiableSet(bt);
    }

    /**
     * Returns the highest build-tool revision known. Can be null.
     *
     * @return The highest build-tool revision known, or null.
     */
    @Nullable
    public BuildToolInfo getLatestBuildTool() {
        return mLocalSdk.getLatestBuildTool();
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
        return mLocalSdk.getBuildTool(revision);
    }

    /**
     * Returns a target from a hash that was generated by {@link IAndroidTarget#hashString()}.
     *
     * @param hash the {@link IAndroidTarget} hash string.
     * @return The matching {@link IAndroidTarget} or null.
     */
    @Nullable
    public IAndroidTarget getTargetFromHashString(@Nullable String hash) {
        return mLocalSdk.getTargetFromHashString(hash);
    }

    /**
     * Updates adb with the USB devices declared in the SDK add-ons.
     * @throws AndroidLocationException
     * @throws IOException
     */
    public void updateAdb() throws AndroidLocationException, IOException {
        FileWriter writer = null;
        try {
            // get the android prefs location to know where to write the file.
            File adbIni = new File(AndroidLocation.getFolder(), ADB_INI_FILE);
            writer = new FileWriter(adbIni);

            // first, put all the vendor id in an HashSet to remove duplicate.
            HashSet<Integer> set = new HashSet<Integer>();
            IAndroidTarget[] targets = getTargets();
            for (IAndroidTarget target : targets) {
                if (target.getUsbVendorId() != IAndroidTarget.NO_USB_ID) {
                    set.add(target.getUsbVendorId());
                }
            }

            // write file header.
            writer.write(ADB_INI_HEADER);

            // now write the Id in a text file, one per line.
            for (Integer i : set) {
                writer.write(String.format("0x%04x\n", i));                            //$NON-NLS-1$
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Returns the greatest {@link LayoutlibVersion} found amongst all platform
     * targets currently loaded in the SDK.
     * <p/>
     * We only started recording Layoutlib Versions recently in the platform meta data
     * so it's possible to have an SDK with many platforms loaded but no layoutlib
     * version defined.
     *
     * @return The greatest {@link LayoutlibVersion} or null if none is found.
     * @deprecated This does NOT solve the right problem and will be changed later.
     */
    @Deprecated
    @Nullable
    public LayoutlibVersion getMaxLayoutlibVersion() {
        LayoutlibVersion maxVersion = null;

        for (IAndroidTarget target : getTargets()) {
            if (target instanceof PlatformTarget) {
                LayoutlibVersion lv = ((PlatformTarget) target).getLayoutlibVersion();
                if (lv != null) {
                    if (maxVersion == null || lv.compareTo(maxVersion) > 0) {
                        maxVersion = lv;
                    }
                }
            }
        }

        return maxVersion;
    }

    /**
     * Returns a map of the <em>root samples directories</em> located in the SDK/extras packages.
     * No guarantee is made that the extras' samples directory actually contain any valid samples.
     * The only guarantee is that the root samples directory actually exists.
     * The map is { File: Samples root directory => String: Extra package display name. }
     *
     * @return A non-null possibly empty map of extra samples directories and their associated
     *   extra package display name.
     */
    @NonNull
    public Map<File, String> getExtraSamples() {

        LocalPkgInfo[] pkgsInfos = mLocalSdk.getPkgsInfos(LocalSdk.PKG_EXTRAS);
        Map<File, String> samples = new HashMap<File, String>();

        for (LocalPkgInfo info : pkgsInfos) {
            assert info instanceof LocalExtraPkgInfo;

            File root = info.getLocalDir();
            File path = new File(root, SdkConstants.FD_SAMPLES);
            if (path.isDirectory()) {
                samples.put(path, info.getListDescription());
                continue;
            }
            // Some old-style extras simply have a single "sample" directory.
            // Accept it if it contains an AndroidManifest.xml.
            path = new File(root, SdkConstants.FD_SAMPLE);
            if (path.isDirectory() &&
                    new File(path, SdkConstants.FN_ANDROID_MANIFEST_XML).isFile()) {
                samples.put(path, info.getListDescription());
            }
        }

        return samples;
    }

    /**
     * Returns a map of all the extras found in the <em>local</em> SDK with their major revision.
     * <p/>
     * Map keys are in the form "vendor-id/path-id". These ids uniquely identify an extra package.
     * The version is the incremental integer major revision of the package.
     *
     * @return A non-null possibly empty map of { string "vendor/path" => integer major revision }
     */
    @NonNull
    public Map<String, Integer> getExtrasVersions() {
        LocalPkgInfo[] pkgsInfos = mLocalSdk.getPkgsInfos(LocalSdk.PKG_EXTRAS);
        Map<String, Integer> extraVersions = new TreeMap<String, Integer>();

        for (LocalPkgInfo info : pkgsInfos) {
            assert info instanceof LocalExtraPkgInfo;
            if (info instanceof LocalExtraPkgInfo) {
                LocalExtraPkgInfo ei = (LocalExtraPkgInfo) info;
                String vendor = ei.getVendorId();
                String path   = ei.getExtraPath();
                int majorRev  = ei.getMajorRevision().getMajor();

                extraVersions.put(vendor + '/' + path, majorRev);
            }
        }

        return extraVersions;
    }

    /** Returns the platform tools version if installed, null otherwise. */
    @Nullable
    public String getPlatformToolsVersion() {
        LocalPkgInfo info = mLocalSdk.getPkgInfo(LocalSdk.PKG_PLATFORM_TOOLS);
        if (info != null && info.hasFullRevision()) {
            return info.getFullRevision().toShortString();
        }

        return null;
    }


    // -------- private methods ----------

//    /**
//     * Loads the Platforms from the SDK.
//     * Creates the "platforms" folder if necessary.
//     *
//     * @param sdkOsPath Location of the SDK
//     * @param targets the list to fill with the platforms.
//     * @param dirInfos a map to keep information on directories to see if they change later.
//     * @param latestBuildTools the latestBuildTools
//     * @param log the ILogger object receiving warning/error from the parsing.
//     * @throws RuntimeException when the "platforms" folder is missing and cannot be created.
//     */
//    @Deprecated // moved to local
//    private static void loadPlatforms(
//            @NonNull String sdkOsPath,
//            @NonNull ArrayList<IAndroidTarget> targets,
//            @NonNull Map<File, DirInfo> dirInfos,
//                     BuildToolInfo latestBuildTools,
//            @NonNull ILogger log) {
//        File platformFolder = new File(sdkOsPath, SdkConstants.FD_PLATFORMS);
//
//        if (platformFolder.isDirectory()) {
//            File[] platforms  = platformFolder.listFiles();
//
//            if (platforms != null) {
//                for (File platform : platforms) {
//                    PlatformTarget target;
//                    if (platform.isDirectory()) {
//                        target = loadPlatform(sdkOsPath, platform, latestBuildTools, log);
//                        if (target != null) {
//                            targets.add(target);
//                        }
//                        // Remember we visited this file/directory,
//                        // even if we failed to load anything from it.
//                        dirInfos.put(platform, new DirInfo(platform));
//                    } else {
//                        log.warning("Ignoring platform '%1$s', not a folder.", platform.getName());
//                    }
//                }
//            }
//
//            return;
//        }
//
//        // Try to create it or complain if something else is in the way.
//        if (!platformFolder.exists()) {
//            if (!platformFolder.mkdir()) {
//                throw new RuntimeException(
//                        String.format("Failed to create %1$s.",
//                                platformFolder.getAbsolutePath()));
//            }
//        } else {
//            throw new RuntimeException(
//                    String.format("%1$s is not a folder.",
//                            platformFolder.getAbsolutePath()));
//        }
//    }
//
//    /**
//     * Loads a specific Platform at a given location.
//     * @param sdkOsPath Location of the SDK
//     * @param platformFolder the root folder of the platform.
//     * @param latestBuildTools the latestBuildTools
//     * @param log the ILogger object receiving warning/error from the parsing.
//     */
//    @Deprecated // moved to local
//    @Nullable
//    private static PlatformTarget loadPlatform(
//            @NonNull String sdkOsPath,
//            @NonNull File platformFolder,
//                     BuildToolInfo latestBuildTools,
//            @NonNull ILogger log) {
//        FileWrapper buildProp = new FileWrapper(platformFolder, SdkConstants.FN_BUILD_PROP);
//        FileWrapper sourcePropFile = new FileWrapper(platformFolder, SdkConstants.FN_SOURCE_PROP);
//
//        if (buildProp.isFile() && sourcePropFile.isFile()) {
//            Map<String, String> platformProp = new HashMap<String, String>();
//
//            // add all the property files
//            Map<String, String> map = ProjectProperties.parsePropertyFile(buildProp, log);
//            if (map != null) {
//                platformProp.putAll(map);
//            }
//
//            map = ProjectProperties.parsePropertyFile(sourcePropFile, log);
//            if (map != null) {
//                platformProp.putAll(map);
//            }
//
//            FileWrapper sdkPropFile = new FileWrapper(platformFolder, SdkConstants.FN_SDK_PROP);
//            if (sdkPropFile.isFile()) { // obsolete platforms don't have this.
//                map = ProjectProperties.parsePropertyFile(sdkPropFile, log);
//                if (map != null) {
//                    platformProp.putAll(map);
//                }
//            }
//
//            // look for some specific values in the map.
//
//            // api level
//            int apiNumber;
//            String stringValue = platformProp.get(PROP_VERSION_SDK);
//            if (stringValue == null) {
//                log.warning(
//                        "Ignoring platform '%1$s': %2$s is missing from '%3$s'",
//                        platformFolder.getName(), PROP_VERSION_SDK,
//                        SdkConstants.FN_BUILD_PROP);
//                return null;
//            } else {
//                try {
//                     apiNumber = Integer.parseInt(stringValue);
//                } catch (NumberFormatException e) {
//                    // looks like apiNumber does not parse to a number.
//                    // Ignore this platform.
//                    log.warning(
//                            "Ignoring platform '%1$s': %2$s is not a valid number in %3$s.",
//                            platformFolder.getName(), PROP_VERSION_SDK,
//                            SdkConstants.FN_BUILD_PROP);
//                    return null;
//                }
//            }
//
//            // Codename must be either null or a platform codename.
//            // REL means it's a release version and therefore the codename should be null.
//            AndroidVersion apiVersion =
//                new AndroidVersion(apiNumber, platformProp.get(PROP_VERSION_CODENAME));
//
//            // version string
//            String apiName = platformProp.get(PkgProps.PLATFORM_VERSION);
//            if (apiName == null) {
//                apiName = platformProp.get(PROP_VERSION_RELEASE);
//            }
//            if (apiName == null) {
//                log.warning(
//                        "Ignoring platform '%1$s': %2$s is missing from '%3$s'",
//                        platformFolder.getName(), PROP_VERSION_RELEASE,
//                        SdkConstants.FN_BUILD_PROP);
//                return null;
//            }
//
//            // platform rev number & layoutlib version are extracted from the source.properties
//            // saved by the SDK Manager when installing the package.
//
//            int revision = 1;
//            LayoutlibVersion layoutlibVersion = null;
//            try {
//                revision = Integer.parseInt(platformProp.get(PkgProps.PKG_REVISION));
//            } catch (NumberFormatException e) {
//                // do nothing, we'll keep the default value of 1.
//            }
//
//            try {
//                String propApi = platformProp.get(PkgProps.LAYOUTLIB_API);
//                String propRev = platformProp.get(PkgProps.LAYOUTLIB_REV);
//                int llApi = propApi == null ? LayoutlibVersion.NOT_SPECIFIED :
//                                              Integer.parseInt(propApi);
//                int llRev = propRev == null ? LayoutlibVersion.NOT_SPECIFIED :
//                                              Integer.parseInt(propRev);
//                if (llApi > LayoutlibVersion.NOT_SPECIFIED &&
//                        llRev >= LayoutlibVersion.NOT_SPECIFIED) {
//                    layoutlibVersion = new LayoutlibVersion(llApi, llRev);
//                }
//            } catch (NumberFormatException e) {
//                // do nothing, we'll ignore the layoutlib version if it's invalid
//            }
//
//            // api number and name look valid, perform a few more checks
//            if (checkPlatformContent(platformFolder, log) == false) {
//                return null;
//            }
//
//            ISystemImage[] systemImages =
//                getPlatformSystemImages(sdkOsPath, platformFolder, apiVersion);
//
//            // create the target.
//            PlatformTarget target = new PlatformTarget(
//                    sdkOsPath,
//                    platformFolder.getAbsolutePath(),
//                    apiVersion,
//                    apiName,
//                    revision,
//                    layoutlibVersion,
//                    systemImages,
//                    platformProp,
//                    latestBuildTools);
//
//            // need to parse the skins.
//            String[] skins = parseSkinFolder(target.getPath(IAndroidTarget.SKINS));
//            target.setSkins(skins);
//
//            return target;
//        } else {
//            log.warning("Ignoring platform '%1$s': %2$s is missing.",   //$NON-NLS-1$
//                    platformFolder.getName(),
//                    SdkConstants.FN_BUILD_PROP);
//        }
//
//        return null;
//    }
//
//    /**
//     * Get all the system images supported by an add-on target.
//     * For an add-on, we first look for sub-folders in the addon/images directory.
//     * If none are found but the directory exists and is not empty, assume it's a legacy
//     * arm eabi system image.
//     * <p/>
//     * Note that it's OK for an add-on to have no system-images at all, since it can always
//     * rely on the ones from its base platform.
//     *
//     * @param root Root of the add-on target being loaded.
//     * @return an array of ISystemImage containing all the system images for the target.
//     *              The list can be empty but not null.
//    */
//    @Deprecated // moved to local
//    @NonNull
//    private static ISystemImage[] getAddonSystemImages(@NonNull File root) {
//        Set<ISystemImage> found = new TreeSet<ISystemImage>();
//
//        root = new File(root, SdkConstants.OS_IMAGES_FOLDER);
//        File[] files = root.listFiles();
//        boolean hasImgFiles = false;
//
//        if (files != null) {
//            // Look for sub-directories
//            for (File file : files) {
//                if (file.isDirectory()) {
//                    found.add(new SystemImage(
//                            file,
//                            LocationType.IN_PLATFORM_SUBFOLDER,
//                            file.getName()));
//                } else if (!hasImgFiles && file.isFile()) {
//                    if (file.getName().endsWith(".img")) {      //$NON-NLS-1$
//                        hasImgFiles = true;
//                    }
//                }
//            }
//        }
//
//        if (found.size() == 0 && hasImgFiles && root.isDirectory()) {
//            // We found no sub-folder system images but it looks like the top directory
//            // has some img files in it. It must be a legacy ARM EABI system image folder.
//            found.add(new SystemImage(
//                    root,
//                    LocationType.IN_PLATFORM_LEGACY,
//                    SdkConstants.ABI_ARMEABI));
//        }
//
//        return found.toArray(new ISystemImage[found.size()]);
//    }
//
//    /**
//     * Get all the system images supported by a platform target.
//     * For a platform, we first look in the new sdk/system-images folders then we
//     * look for sub-folders in the platform/images directory and/or the one legacy
//     * folder.
//     * If any given API appears twice or more, the first occurrence wins.
//     *
//     * @param sdkOsPath The path to the SDK.
//     * @param root Root of the platform target being loaded.
//     * @param version API level + codename of platform being loaded.
//     * @return an array of ISystemImage containing all the system images for the target.
//     *              The list can be empty but not null.
//     */
//    @Deprecated // moved to local
//    @NonNull
//    private static ISystemImage[] getPlatformSystemImages(
//            @NonNull String sdkOsPath,
//            @NonNull File root,
//            @NonNull AndroidVersion version) {
//        Set<ISystemImage> found = new TreeSet<ISystemImage>();
//        Set<String> abiFound = new HashSet<String>();
//
//        // First look in the SDK/system-image/platform-n/abi folders.
//        // We require/enforce the system image to have a valid properties file.
//        // The actual directory names are irrelevant.
//        // If we find multiple occurrences of the same platform/abi, the first one read wins.
//
//        File[] firstLevelFiles = new File(sdkOsPath, SdkConstants.FD_SYSTEM_IMAGES).listFiles();
//        if (firstLevelFiles != null) {
//            for (File firstLevel : firstLevelFiles) {
//                File[] secondLevelFiles = firstLevel.listFiles();
//                if (secondLevelFiles != null) {
//                    for (File secondLevel : secondLevelFiles) {
//                        try {
//                            File propFile = new File(secondLevel, SdkConstants.FN_SOURCE_PROP);
//                            Properties props = new Properties();
//                            FileInputStream fis = null;
//                            try {
//                                fis = new FileInputStream(propFile);
//                                props.load(fis);
//                            } finally {
//                                if (fis != null) {
//                                    fis.close();
//                                }
//                            }
//
//                            AndroidVersion propsVersion = new AndroidVersion(props);
//                            if (!propsVersion.equals(version)) {
//                                continue;
//                            }
//
//                            String abi = props.getProperty(PkgProps.SYS_IMG_ABI);
//                            if (abi != null && !abiFound.contains(abi)) {
//                                found.add(new SystemImage(
//                                        secondLevel,
//                                        LocationType.IN_SYSTEM_IMAGE,
//                                        abi));
//                                abiFound.add(abi);
//                            }
//                        } catch (Exception ignore) {}
//                    }
//                }
//            }
//        }
//
//        // Then look in either the platform/images/abi or the legacy folder
//        root = new File(root, SdkConstants.OS_IMAGES_FOLDER);
//        File[] files = root.listFiles();
//        boolean useLegacy = true;
//        boolean hasImgFiles = false;
//
//        if (files != null) {
//            // Look for sub-directories
//            for (File file : files) {
//                if (file.isDirectory()) {
//                    useLegacy = false;
//                    String abi = file.getName();
//                    if (!abiFound.contains(abi)) {
//                        found.add(new SystemImage(
//                                file,
//                                LocationType.IN_PLATFORM_SUBFOLDER,
//                                abi));
//                        abiFound.add(abi);
//                    }
//                } else if (!hasImgFiles && file.isFile()) {
//                    if (file.getName().endsWith(".img")) {      //$NON-NLS-1$
//                        hasImgFiles = true;
//                    }
//                }
//            }
//        }
//
//        if (useLegacy && hasImgFiles && root.isDirectory() &&
//                !abiFound.contains(SdkConstants.ABI_ARMEABI)) {
//            // We found no sub-folder system images but it looks like the top directory
//            // has some img files in it. It must be a legacy ARM EABI system image folder.
//            found.add(new SystemImage(
//                    root,
//                    LocationType.IN_PLATFORM_LEGACY,
//                    SdkConstants.ABI_ARMEABI));
//        }
//
//        return found.toArray(new ISystemImage[found.size()]);
//    }
//
//    /**
//     * Loads the Add-on from the SDK.
//     * Creates the "add-ons" folder if necessary.
//     *
//     * @param osSdkPath Location of the SDK
//     * @param targets the list to fill with the add-ons.
//     * @param dirInfos a map to keep information on directories to see if they change later.
//     * @param log the ILogger object receiving warning/error from the parsing.
//     * @throws RuntimeException when the "add-ons" folder is missing and cannot be created.
//     */
//    @Deprecated // moved to local
//    private static void loadAddOns(
//            @NonNull String osSdkPath,
//            @NonNull ArrayList<IAndroidTarget> targets,
//            @NonNull Map<File, DirInfo> dirInfos,
//            @NonNull ILogger log) {
//        File addonFolder = new File(osSdkPath, SdkConstants.FD_ADDONS);
//
//        if (addonFolder.isDirectory()) {
//            File[] addons  = addonFolder.listFiles();
//
//            IAndroidTarget[] targetList = targets.toArray(new IAndroidTarget[targets.size()]);
//
//            if (addons != null) {
//                for (File addon : addons) {
//                    // Add-ons have to be folders. Ignore files and no need to warn about them.
//                    AddOnTarget target;
//                    if (addon.isDirectory()) {
//                        target = loadAddon(addon, targetList, log);
//                        if (target != null) {
//                            targets.add(target);
//                        }
//                        // Remember we visited this file/directory,
//                        // even if we failed to load anything from it.
//                        dirInfos.put(addon, new DirInfo(addon));
//                    }
//                }
//            }
//
//            return;
//        }
//
//        // Try to create it or complain if something else is in the way.
//        if (!addonFolder.exists()) {
//            if (!addonFolder.mkdir()) {
//                throw new RuntimeException(
//                        String.format("Failed to create %1$s.",
//                                addonFolder.getAbsolutePath()));
//            }
//        } else {
//            throw new RuntimeException(
//                    String.format("%1$s is not a folder.",
//                            addonFolder.getAbsolutePath()));
//        }
//    }
//
//    /**
//     * Loads a specific Add-on at a given location.
//     * @param addonDir the location of the add-on directory.
//     * @param targetList The list of Android target that were already loaded from the SDK.
//     * @param log the ILogger object receiving warning/error from the parsing.
//     */
//    @Deprecated // moved to local
//    @Nullable
//    private static AddOnTarget loadAddon(
//            @NonNull File addonDir,
//            @NonNull IAndroidTarget[] targetList,
//            @NonNull ILogger log) {
//        // Parse the addon properties to ensure we can load it.
//        Pair<Map<String, String>, String> infos = parseAddonProperties(addonDir, targetList, log);
//
//        Map<String, String> propertyMap = infos.getFirst();
//        String error = infos.getSecond();
//
//        if (error != null) {
//            log.warning("Ignoring add-on '%1$s': %2$s", addonDir.getName(), error);
//            return null;
//        }
//
//        // Since error==null we're not supposed to encounter any issues loading this add-on.
//        try {
//            assert propertyMap != null;
//
//            String api = propertyMap.get(ADDON_API);
//            String name = propertyMap.get(ADDON_NAME);
//            String vendor = propertyMap.get(ADDON_VENDOR);
//
//            assert api != null;
//            assert name != null;
//            assert vendor != null;
//
//            PlatformTarget baseTarget = null;
//
//            // Look for a platform that has a matching api level or codename.
//            for (IAndroidTarget target : targetList) {
//                if (target.isPlatform() && target.getVersion().equals(api)) {
//                    baseTarget = (PlatformTarget)target;
//                    break;
//                }
//            }
//
//            assert baseTarget != null;
//
//            // get the optional description
//            String description = propertyMap.get(ADDON_DESCRIPTION);
//
//            // get the add-on revision
//            int revisionValue = 1;
//            String revision = propertyMap.get(ADDON_REVISION);
//            if (revision == null) {
//                revision = propertyMap.get(ADDON_REVISION_OLD);
//            }
//            if (revision != null) {
//                revisionValue = Integer.parseInt(revision);
//            }
//
//            // get the optional libraries
//            String librariesValue = propertyMap.get(ADDON_LIBRARIES);
//            Map<String, String[]> libMap = null;
//
//            if (librariesValue != null) {
//                librariesValue = librariesValue.trim();
//                if (librariesValue.length() > 0) {
//                    // split in the string into the libraries name
//                    String[] libraries = librariesValue.split(";");                    //$NON-NLS-1$
//                    if (libraries.length > 0) {
//                        libMap = new HashMap<String, String[]>();
//                        for (String libName : libraries) {
//                            libName = libName.trim();
//
//                            // get the library data from the properties
//                            String libData = propertyMap.get(libName);
//
//                            if (libData != null) {
//                                // split the jar file from the description
//                                Matcher m = PATTERN_LIB_DATA.matcher(libData);
//                                if (m.matches()) {
//                                    libMap.put(libName, new String[] {
//                                            m.group(1), m.group(2) });
//                                } else {
//                                    log.warning(
//                                            "Ignoring library '%1$s', property value has wrong format\n\t%2$s",
//                                            libName, libData);
//                                }
//                            } else {
//                                log.warning(
//                                        "Ignoring library '%1$s', missing property value",
//                                        libName, libData);
//                            }
//                        }
//                    }
//                }
//            }
//
//            // get the abi list.
//            ISystemImage[] systemImages = getAddonSystemImages(addonDir);
//
//            // check whether the add-on provides its own rendering info/library.
//            boolean hasRenderingLibrary = false;
//            boolean hasRenderingResources = false;
//
//            File dataFolder = new File(addonDir, SdkConstants.FD_DATA);
//            if (dataFolder.isDirectory()) {
//                hasRenderingLibrary = new File(dataFolder, SdkConstants.FN_LAYOUTLIB_JAR).isFile();
//                hasRenderingResources = new File(dataFolder, SdkConstants.FD_RES).isDirectory() &&
//                        new File(dataFolder, SdkConstants.FD_FONTS).isDirectory();
//            }
//
//            AddOnTarget target = new AddOnTarget(addonDir.getAbsolutePath(), name, vendor,
//                    revisionValue, description, systemImages, libMap,
//                    hasRenderingLibrary, hasRenderingResources,baseTarget);
//
//            // need to parse the skins.
//            String[] skins = parseSkinFolder(target.getPath(IAndroidTarget.SKINS));
//
//            // get the default skin, or take it from the base platform if needed.
//            String defaultSkin = propertyMap.get(ADDON_DEFAULT_SKIN);
//            if (defaultSkin == null) {
//                if (skins.length == 1) {
//                    defaultSkin = skins[0];
//                } else {
//                    defaultSkin = baseTarget.getDefaultSkin();
//                }
//            }
//
//            // get the USB ID (if available)
//            int usbVendorId = convertId(propertyMap.get(ADDON_USB_VENDOR));
//            if (usbVendorId != IAndroidTarget.NO_USB_ID) {
//                target.setUsbVendorId(usbVendorId);
//            }
//
//            target.setSkins(skins, defaultSkin);
//
//            return target;
//
//        } catch (Exception e) {
//            log.warning("Ignoring add-on '%1$s': error %2$s.",
//                    addonDir.getName(), e.toString());
//        }
//
//        return null;
//    }
//
//    /**
//     * Parses the add-on properties and decodes any error that occurs when loading an addon.
//     *
//     * @param addonDir the location of the addon directory.
//     * @param targetList The list of Android target that were already loaded from the SDK.
//     * @param log the ILogger object receiving warning/error from the parsing.
//     * @return A pair with the property map and an error string. Both can be null but not at the
//     *  same time. If a non-null error is present then the property map must be ignored. The error
//     *  should be translatable as it might show up in the SdkManager UI.
//     */
//    @Deprecated // moved to local
//    @NonNull
//    public
//    static Pair<Map<String, String>, String> parseAddonProperties(
//            @NonNull File addonDir,
//            @NonNull IAndroidTarget[] targetList,
//            @NonNull ILogger log) {
//        Map<String, String> propertyMap = null;
//        String error = null;
//
//        FileWrapper addOnManifest = new FileWrapper(addonDir, SdkConstants.FN_MANIFEST_INI);
//
//        do {
//            if (!addOnManifest.isFile()) {
//                error = String.format("File not found: %1$s", SdkConstants.FN_MANIFEST_INI);
//                break;
//            }
//
//            propertyMap = ProjectProperties.parsePropertyFile(addOnManifest, log);
//            if (propertyMap == null) {
//                error = String.format("Failed to parse properties from %1$s",
//                        SdkConstants.FN_MANIFEST_INI);
//                break;
//            }
//
//            // look for some specific values in the map.
//            // we require name, vendor, and api
//            String name = propertyMap.get(ADDON_NAME);
//            if (name == null) {
//                error = addonManifestWarning(ADDON_NAME);
//                break;
//            }
//
//            String vendor = propertyMap.get(ADDON_VENDOR);
//            if (vendor == null) {
//                error = addonManifestWarning(ADDON_VENDOR);
//                break;
//            }
//
//            String api = propertyMap.get(ADDON_API);
//            PlatformTarget plat = null;
//            if (api == null) {
//                error = addonManifestWarning(ADDON_API);
//                break;
//            }
//
//            // Look for a platform that has a matching api level or codename.
//            PlatformTarget baseTarget = null;
//            for (IAndroidTarget target : targetList) {
//                if (target.isPlatform() && target.getVersion().equals(api)) {
//                    baseTarget = (PlatformTarget)target;
//                    break;
//                }
//            }
//
//            if (baseTarget == null) {
//                error = String.format("Unable to find base platform with API level '%1$s'", api);
//                break;
//            }
//
//            // get the add-on revision
//            String revision = propertyMap.get(ADDON_REVISION);
//            if (revision == null) {
//                revision = propertyMap.get(ADDON_REVISION_OLD);
//            }
//            if (revision != null) {
//                try {
//                    Integer.parseInt(revision);
//                } catch (NumberFormatException e) {
//                    // looks like revision does not parse to a number.
//                    error = String.format("%1$s is not a valid number in %2$s.",
//                                ADDON_REVISION, SdkConstants.FN_BUILD_PROP);
//                    break;
//                }
//            }
//
//        } while(false);
//
//        return Pair.of(propertyMap, error);
//    }
//
//    /**
//     * Converts a string representation of an hexadecimal ID into an int.
//     * @param value the string to convert.
//     * @return the int value, or {@link IAndroidTarget#NO_USB_ID} if the conversion failed.
//     */
//    @Deprecated // moved to local
//    private static int convertId(@Nullable String value) {
//        if (value != null && value.length() > 0) {
//            if (PATTERN_USB_IDS.matcher(value).matches()) {
//                String v = value.substring(2);
//                try {
//                    return Integer.parseInt(v, 16);
//                } catch (NumberFormatException e) {
//                    // this shouldn't happen since we check the pattern above, but this is safer.
//                    // the method will return 0 below.
//                }
//            }
//        }
//
//        return IAndroidTarget.NO_USB_ID;
//    }
//
//    /**
//     * Prepares a warning about the addon being ignored due to a missing manifest value.
//     * This string will show up in the SdkManager UI.
//     *
//     * @param valueName The missing manifest value, for display.
//     */
//    @Deprecated // moved to local
//    @NonNull
//    private static String addonManifestWarning(@NonNull String valueName) {
//        return String.format("'%1$s' is missing from %2$s.",
//                valueName, SdkConstants.FN_MANIFEST_INI);
//    }
//
//    /**
//     * Checks the given platform has all the required files, and returns true if they are all
//     * present.
//     * <p/>This checks the presence of the following files: android.jar, framework.aidl, aapt(.exe),
//     * aidl(.exe), dx(.bat), and dx.jar
//     *
//     * @param platform The folder containing the platform.
//     * @param log Logger.
//     */
//    @Deprecated // moved to local
//    private static boolean checkPlatformContent(
//            @NonNull File platform,
//            @NonNull ILogger log) {
//        for (String relativePath : sPlatformContentList) {
//            File f = new File(platform, relativePath);
//            if (!f.exists()) {
//                log.warning(
//                        "Ignoring platform '%1$s': %2$s is missing.",                  //$NON-NLS-1$
//                        platform.getName(), relativePath);
//                return false;
//            }
//        }
//        return true;
//    }
//
//
//    /**
//     * Parses the skin folder and builds the skin list.
//     * @param osPath The path of the skin root folder.
//     */
//    @Deprecated // moved to local
//    @NonNull
//    private static String[] parseSkinFolder(@NonNull String osPath) {
//        File skinRootFolder = new File(osPath);
//
//        if (skinRootFolder.isDirectory()) {
//            ArrayList<String> skinList = new ArrayList<String>();
//
//            File[] files = skinRootFolder.listFiles();
//
//            for (File skinFolder : files) {
//                if (skinFolder.isDirectory()) {
//                    // check for layout file
//                    File layout = new File(skinFolder, SdkConstants.FN_SKIN_LAYOUT);
//
//                    if (layout.isFile()) {
//                        // for now we don't parse the content of the layout and
//                        // simply add the directory to the list.
//                        skinList.add(skinFolder.getName());
//                    }
//                }
//            }
//
//            return skinList.toArray(new String[skinList.size()]);
//        }
//
//        return new String[0];
//    }
//
//    /**
//     * Returns the {@link AndroidVersion} of the sample in the given folder.
//     *
//     * @param folder The sample's folder.
//     * @param log Logger for errors.
//     * @return An {@link AndroidVersion} or null on error.
//     */
//    @Deprecated // moved to local
//    @Nullable
//    private AndroidVersion getSamplesVersion(
//            @NonNull File folder,
//            @NonNull ILogger log) {
//        File sourceProp = new File(folder, SdkConstants.FN_SOURCE_PROP);
//        try {
//            Properties p = new Properties();
//            FileInputStream fis = null;
//            try {
//                fis = new FileInputStream(sourceProp);
//                p.load(fis);
//            } finally {
//                if (fis != null) {
//                    fis.close();
//                }
//            }
//
//            return new AndroidVersion(p);
//        } catch (FileNotFoundException e) {
//            log.warning("Ignoring sample '%1$s': does not contain %2$s.",              //$NON-NLS-1$
//                    folder.getName(), SdkConstants.FN_SOURCE_PROP);
//        } catch (IOException e) {
//            log.warning("Ignoring sample '%1$s': failed reading %2$s.",                //$NON-NLS-1$
//                    folder.getName(), SdkConstants.FN_SOURCE_PROP);
//        } catch (AndroidVersionException e) {
//            log.warning("Ignoring sample '%1$s': no android version found in %2$s.",   //$NON-NLS-1$
//                    folder.getName(), SdkConstants.FN_SOURCE_PROP);
//        }
//
//        return null;
//    }
//
//    /**
//     * Loads the build-tools from the SDK.
//     * Creates the "build-tools" folder if necessary.
//     *
//     * @param sdkOsPath Location of the SDK
//     * @param infos the map to fill with the build-tools.
//     * @param dirInfos a map to keep information on directories to see if they change later.
//     * @param log the ILogger object receiving warning/error from the parsing.
//     * @throws RuntimeException when the "platforms" folder is missing and cannot be created.
//     */
//    @Deprecated // moved to local
//    private static void loadBuildTools(
//            @NonNull String sdkOsPath,
//            @NonNull Map<FullRevision, BuildToolInfo> infos,
//            @NonNull Map<File, DirInfo> dirInfos,
//            @NonNull ILogger log) {
//        File buildToolsFolder = new File(sdkOsPath, SdkConstants.FD_BUILD_TOOLS);
//
//        if (buildToolsFolder.isDirectory()) {
//            File[] folders  = buildToolsFolder.listFiles();
//            if (folders != null) {
//                for (File subFolder : folders) {
//                    if (subFolder.isDirectory()) {
//                        BuildToolInfo info = loadBuildTool(sdkOsPath, subFolder, log);
//                        if (info != null) {
//                            infos.put(info.getRevision(), info);
//                        }
//                        // Remember we visited this file/directory,
//                        // even if we failed to load anything from it.
//                        dirInfos.put(subFolder, new DirInfo(subFolder));
//                    } else {
//                        log.warning("Ignoring build-tool '%1$s', not a folder.",
//                                subFolder.getName());
//                    }
//                }
//            }
//
//            return;
//        }
//
//        // Try to create it or complain if something else is in the way.
//        if (!buildToolsFolder.exists()) {
//            if (!buildToolsFolder.mkdir()) {
//                throw new RuntimeException(
//                        String.format("Failed to create %1$s.",
//                                buildToolsFolder.getAbsolutePath()));
//            }
//        } else {
//            throw new RuntimeException(
//                    String.format("%1$s is not a folder.",
//                            buildToolsFolder.getAbsolutePath()));
//        }
//    }
//
//    /**
//     * Loads a specific Platform at a given location.
//     * @param sdkOsPath Location of the SDK
//     * @param folder the root folder of the platform.
//     * @param log the ILogger object receiving warning/error from the parsing.
//     */
//    @Deprecated // moved to local
//    @Nullable
//    private static BuildToolInfo loadBuildTool(
//            @NonNull String sdkOsPath,
//            @NonNull File folder,
//            @NonNull ILogger log) {
//        FileOp f = new FileOp();
//
//        File sourcePropFile = new File(folder, SdkConstants.FN_SOURCE_PROP);
//        if (!f.isFile(sourcePropFile)) {
//            log.warning("Ignoring build-tool '%1$s': missing file %2$s",
//                    folder.getName(), SdkConstants.FN_SOURCE_PROP);
//        } else {
//            Properties props = f.loadProperties(sourcePropFile);
//            String revStr = props.getProperty(PkgProps.PKG_REVISION);
//
//            try {
//                FullRevision rev =
//                    FullRevision.parseRevision(props.getProperty(PkgProps.PKG_REVISION));
//
//                BuildToolInfo info = new BuildToolInfo(rev, folder);
//                return info.isValid(log) ? info : null;
//
//            } catch (NumberFormatException e) {
//                log.warning("Ignoring build-tool '%1$s': invalid revision '%2$s'",
//                        folder.getName(), revStr);
//            }
//
//        }
//
//        return null;
//    }

    // -------------

    public static class LayoutlibVersion implements Comparable<LayoutlibVersion> {
        private final int mApi;
        private final int mRevision;

        public static final int NOT_SPECIFIED = 0;

        public LayoutlibVersion(int api, int revision) {
            mApi = api;
            mRevision = revision;
        }

        public int getApi() {
            return mApi;
        }

        public int getRevision() {
            return mRevision;
        }

        @Override
        public int compareTo(@NonNull LayoutlibVersion rhs) {
            boolean useRev = this.mRevision > NOT_SPECIFIED && rhs.mRevision > NOT_SPECIFIED;
            int lhsValue = (this.mApi << 16) + (useRev ? this.mRevision : 0);
            int rhsValue = (rhs.mApi  << 16) + (useRev ? rhs.mRevision  : 0);
            return lhsValue - rhsValue;
        }
    }

//    // -------------
//
//    @Deprecated // moved to local
//    private static class DirInfo {
//        @NonNull
//        private final File mDir;
//        private final long mDirModifiedTS;
//        private final long mPropsModifiedTS;
//        private final long mPropsChecksum;
//
//        /**
//         * Creates a new immutable {@link DirInfo}.
//         *
//         * @param dir The platform/addon directory of the target. It should be a directory.
//         */
//        public DirInfo(@NonNull File dir) {
//            mDir = dir;
//            mDirModifiedTS = dir.lastModified();
//
//            // Capture some info about the source.properties file if it exists.
//            // We use propsModifiedTS == 0 to mean there is no props file.
//            long propsChecksum = 0;
//            long propsModifiedTS = 0;
//            File props = new File(dir, SdkConstants.FN_SOURCE_PROP);
//            if (props.isFile()) {
//                propsModifiedTS = props.lastModified();
//                propsChecksum = getFileChecksum(props);
//            }
//            mPropsModifiedTS = propsModifiedTS;
//            mPropsChecksum = propsChecksum;
//        }
//
//        /**
//         * Checks whether the directory/source.properties attributes have changed.
//         *
//         * @return True if the directory modified timestamp or
//         *  its source.property files have changed.
//         */
//        public boolean hasChanged() {
//            // Does platform directory still exist?
//            if (!mDir.isDirectory()) {
//                return true;
//            }
//            // Has platform directory modified-timestamp changed?
//            if (mDirModifiedTS != mDir.lastModified()) {
//                return true;
//            }
//
//            File props = new File(mDir, SdkConstants.FN_SOURCE_PROP);
//
//            // The directory did not have a props file if target was null or
//            // if mPropsModifiedTS is 0.
//            boolean hadProps = mPropsModifiedTS != 0;
//
//            // Was there a props file and it vanished, or there wasn't and there's one now?
//            if (hadProps != props.isFile()) {
//                return true;
//            }
//
//            if (hadProps) {
//                // Has source.props file modified-timestamp changed?
//                if (mPropsModifiedTS != props.lastModified()) {
//                    return true;
//                }
//                // Had the content of source.props changed?
//                if (mPropsChecksum != getFileChecksum(props)) {
//                    return true;
//                }
//            }
//
//            return false;
//        }
//
//        /**
//         * Computes an adler32 checksum (source.props are small files, so this
//         * should be OK with an acceptable collision rate.)
//         */
//        private static long getFileChecksum(@NonNull File file) {
//            FileInputStream fis = null;
//            try {
//                fis = new FileInputStream(file);
//                Adler32 a = new Adler32();
//                byte[] buf = new byte[1024];
//                int n;
//                while ((n = fis.read(buf)) > 0) {
//                    a.update(buf, 0, n);
//                }
//                return a.getValue();
//            } catch (Exception ignore) {
//            } finally {
//                try {
//                    if (fis != null) {
//                        fis.close();
//                    }
//                } catch(Exception ignore) {}
//            }
//            return 0;
//        }
//
//        /** Returns a visual representation of this object for debugging. */
//        @Override
//        public String toString() {
//            String s = String.format("<DirInfo %1$s TS=%2$d", mDir, mDirModifiedTS);  //$NON-NLS-1$
//            if (mPropsModifiedTS != 0) {
//                s += String.format(" | Props TS=%1$d, Chksum=%2$s",                   //$NON-NLS-1$
//                        mPropsModifiedTS, mPropsChecksum);
//            }
//            return s + ">";                                                           //$NON-NLS-1$
//        }
//    }
}
