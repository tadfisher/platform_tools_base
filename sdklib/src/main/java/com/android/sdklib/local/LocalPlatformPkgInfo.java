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
import com.android.io.FileWrapper;
import com.android.sdklib.AndroidTargetHash;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISystemImage;
import com.android.sdklib.PlatformTarget;
import com.android.sdklib.SdkManager;
import com.android.sdklib.SdkManager.LayoutlibVersion;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.repository.packages.Package;
import com.android.sdklib.io.IFileOp;
import com.android.sdklib.repository.MajorRevision;
import com.android.sdklib.repository.PkgProps;
import com.android.utils.ILogger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LocalPlatformPkgInfo extends LocalAndroidVersionPkgInfo {

    private final MajorRevision mRevision;
    /** Android target, lazyly loaded from #getAndroidTarget */
    private IAndroidTarget mTarget;
    protected boolean mLoaded;

    public LocalPlatformPkgInfo(@NonNull LocalSdk localSdk,
                                @NonNull File localDir,
                                @NonNull Properties sourceProps,
                                @NonNull AndroidVersion version,
                                @NonNull MajorRevision revision) {
        super(localSdk, localDir, sourceProps, version);
        mRevision = revision;
        //--TODO--mTarget = target;
    }

    @Override
    public int getType() {
        return LocalSdk.PKG_PLATFORMS;
    }

    @NonNull
    public String getTargetHash() {
        return AndroidTargetHash.getPlatformHashString(getAndroidVersion());
    }

    @Nullable
    public IAndroidTarget getAndroidTarget() {
        if (!mLoaded) {
            createPlatformTarget();
            mLoaded = true;
        }
        return mTarget;
    }

    @Override
    public boolean hasMajorRevision() {
        return true;
    }

    @Override
    public MajorRevision getMajorRevision() {
        return mRevision;
    }

    @Override
    public Package getPackage() {
        if (!mLoaded) {
            createPlatformTarget();
            mLoaded = true;
        }

        Package pkg = super.getPackage();
        if (pkg == null) {
            // TODO load on demand
        }
        return pkg;
    }

    /**
     * Creates the PlatformTarget.
     */
    @Nullable
    private void createPlatformTarget() {
        IFileOp fileOp = getLocalSdk().getFileOp();
        File platformFolder = getLocalDir();
        File buildProp = new File(platformFolder, SdkConstants.FN_BUILD_PROP);
        File sourcePropFile = new File(platformFolder, SdkConstants.FN_SOURCE_PROP);

        if (!fileOp.isFile(buildProp) || !fileOp.isFile(sourcePropFile)) {
            appendLoadError("Ignoring platform '%1$s': %2$s is missing.",   //$NON-NLS-1$
                    platformFolder.getName(),
                    SdkConstants.FN_BUILD_PROP);
            return;
        }

        Map<String, String> platformProp = new HashMap<String, String>();

        // add all the property files
        Map<String, String> map = ProjectProperties.parsePropertyStream(
                fileOp.newFileInputStream(buildProp),
                fileOp.getPath(buildProp),
                null /*log*/);
        if (map != null) {
            platformProp.putAll(map);
        }

        map = ProjectProperties.parsePropertyStream(
                fileOp.newFileInputStream(sourcePropFile),
                fileOp.getPath(sourcePropFile),
                null /*log*/);
        if (map != null) {
            platformProp.putAll(map);
        }

        File sdkPropFile = new File(platformFolder, SdkConstants.FN_SDK_PROP);
        if (fileOp.isFile(sdkPropFile)) { // obsolete platforms don't have this.
            map = ProjectProperties.parsePropertyStream(
                    fileOp.newFileInputStream(sdkPropFile),
                    fileOp.getPath(sdkPropFile),
                    null /*log*/);
            if (map != null) {
                platformProp.putAll(map);
            }
        }

        // look for some specific values in the map.

        // api level
        int apiNumber;
        String stringValue = platformProp.get(SdkManager.PROP_VERSION_SDK);
        if (stringValue == null) {
            appendLoadError("Ignoring platform '%1$s': %2$s is missing from '%3$s'",
                    platformFolder.getName(), SdkManager.PROP_VERSION_SDK,
                    SdkConstants.FN_BUILD_PROP);
            return;
        } else {
            try {
                 apiNumber = Integer.parseInt(stringValue);
            } catch (NumberFormatException e) {
                // looks like apiNumber does not parse to a number.
                // Ignore this platform.
                appendLoadError(
                        "Ignoring platform '%1$s': %2$s is not a valid number in %3$s.",
                        platformFolder.getName(), SdkManager.PROP_VERSION_SDK,
                        SdkConstants.FN_BUILD_PROP);
                return;
            }
        }

        // Codename must be either null or a platform codename.
        // REL means it's a release version and therefore the codename should be null.
        AndroidVersion apiVersion =
            new AndroidVersion(apiNumber, platformProp.get(SdkManager.PROP_VERSION_CODENAME));

        // version string
        String apiName = platformProp.get(PkgProps.PLATFORM_VERSION);
        if (apiName == null) {
            apiName = platformProp.get(SdkManager.PROP_VERSION_RELEASE);
        }
        if (apiName == null) {
            appendLoadError(
                    "Ignoring platform '%1$s': %2$s is missing from '%3$s'",
                    platformFolder.getName(), SdkManager.PROP_VERSION_RELEASE,
                    SdkConstants.FN_BUILD_PROP);
            return;
        }

        // platform rev number & layoutlib version are extracted from the source.properties
        // saved by the SDK Manager when installing the package.

        int revision = 1;
        LayoutlibVersion layoutlibVersion = null;
        try {
            revision = Integer.parseInt(platformProp.get(PkgProps.PKG_REVISION));
        } catch (NumberFormatException e) {
            // do nothing, we'll keep the default value of 1.
        }

        try {
            String propApi = platformProp.get(PkgProps.LAYOUTLIB_API);
            String propRev = platformProp.get(PkgProps.LAYOUTLIB_REV);
            int llApi = propApi == null ? LayoutlibVersion.NOT_SPECIFIED :
                                          Integer.parseInt(propApi);
            int llRev = propRev == null ? LayoutlibVersion.NOT_SPECIFIED :
                                          Integer.parseInt(propRev);
            if (llApi > LayoutlibVersion.NOT_SPECIFIED &&
                    llRev >= LayoutlibVersion.NOT_SPECIFIED) {
                layoutlibVersion = new LayoutlibVersion(llApi, llRev);
            }
        } catch (NumberFormatException e) {
            // do nothing, we'll ignore the layoutlib version if it's invalid
        }

        // api number and name look valid, perform a few more checks
        String err = checkPlatformContent(platformFolder);
        if (err != null) {
            appendLoadError("%s", err); //$NLN-NLS-1$
            return;
        }

        ISystemImage[] systemImages = getPlatformSystemImages(platformFolder, apiVersion);

        // create the target.
        mTarget = new PlatformTarget(
                getLocalSdk().getLocation().getPath(),
                platformFolder.getAbsolutePath(),
                apiVersion,
                apiName,
                revision,
                layoutlibVersion,
                systemImages,
                platformProp,
                getLocalSdk().getLatestCompatibleBuildTool());

        // need to parse the skins.
        String[] skins = parseSkinFolder(mTarget.getPath(IAndroidTarget.SKINS));
        mTarget.setSkins(skins);
    }


    /** List of items in the platform to check when parsing it. These paths are relative to the
     * platform root folder. */
    private static final String[] sPlatformContentList = new String[] {
        SdkConstants.FN_FRAMEWORK_LIBRARY,
        SdkConstants.FN_FRAMEWORK_AIDL,
    };

    /**
     * Checks the given platform has all the required files, and returns true if they are all
     * present.
     * <p/>This checks the presence of the following files: android.jar, framework.aidl, aapt(.exe),
     * aidl(.exe), dx(.bat), and dx.jar
     *
     * @param platform The folder containing the platform.
     * @return An error description if platform is rejected; null if no error is detected.
     */
    @NonNull
    private static String checkPlatformContent(@NonNull File platform) {
        for (String relativePath : sPlatformContentList) {
            File f = new File(platform, relativePath);
            if (!f.exists()) {
                return String.format(
                        "Ignoring platform '%1$s': %2$s is missing.",                  //$NON-NLS-1$
                        platform.getName(), relativePath);
            }
        }
        return null;
    }
}
