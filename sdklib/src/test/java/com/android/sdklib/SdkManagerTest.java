/*
 * Copyright (C) 2011 The Android Open Source Project
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
import com.android.sdklib.BuildToolInfoTest.BuildToolInfoWrapper;
import com.android.sdklib.ISystemImage.LocationType;
import com.android.sdklib.internal.androidTarget.PlatformTarget;
import com.android.sdklib.io.FileOp;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.NoPreviewRevision;
import com.android.sdklib.repository.descriptors.IdDisplay;
import com.android.sdklib.repository.local.LocalSdk;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Setup will build an SDK Manager local install matching the latest repository-N.xsd. */
public class SdkManagerTest extends SdkManagerTestCase {

    public void testSdkManager_getBuildTools() {
        LocalSdk sdk = getSdk();

        assertEquals(new FullRevision(18, 3, 4, 5),
                sdk.getLatestBuildTool().getRevision());

        // Get infos, first one that doesn't exist returns null.
        assertNull(sdk.getBuildTool(new FullRevision(1)));

        // Now some that exist.
        BuildToolInfo i = sdk.getBuildTool(new FullRevision(3, 0, 0));
        assertEquals(
                "<BuildToolInfo rev=3.0.0, " +
                "mPath=$SDK/build-tools/3.0.0, " +
                "mPaths={" +
                    "AAPT=$SDK/build-tools/3.0.0/aapt, " +
                    "AIDL=$SDK/build-tools/3.0.0/aidl, " +
                    "DX=$SDK/build-tools/3.0.0/dx, " +
                    "DX_JAR=$SDK/build-tools/3.0.0/lib/dx.jar, " +
                    "LLVM_RS_CC=$SDK/build-tools/3.0.0/llvm-rs-cc, " +
                    "ANDROID_RS=$SDK/build-tools/3.0.0/renderscript/include/, " +
                    "ANDROID_RS_CLANG=$SDK/build-tools/3.0.0/renderscript/clang-include/, " +
                    "DEXDUMP=$SDK/build-tools/3.0.0/dexdump}>",
                cleanPath(sdk, i.toString()));

        i = sdk.getBuildTool(new FullRevision(18, 3, 4, 5));
        assertEquals(
                "<BuildToolInfo rev=18.3.4 rc5, " +
                "mPath=$SDK/build-tools/18.3.4 rc5, " +
                "mPaths={" +
                    "AAPT=$SDK/build-tools/18.3.4 rc5/aapt, " +
                    "AIDL=$SDK/build-tools/18.3.4 rc5/aidl, " +
                    "DX=$SDK/build-tools/18.3.4 rc5/dx, " +
                    "DX_JAR=$SDK/build-tools/18.3.4 rc5/lib/dx.jar, " +
                    "LLVM_RS_CC=$SDK/build-tools/18.3.4 rc5/llvm-rs-cc, " +
                    "ANDROID_RS=$SDK/build-tools/18.3.4 rc5/renderscript/include/, " +
                    "ANDROID_RS_CLANG=$SDK/build-tools/18.3.4 rc5/renderscript/clang-include/, " +
                    "DEXDUMP=$SDK/build-tools/18.3.4 rc5/dexdump, " +
                    "BCC_COMPAT=$SDK/build-tools/18.3.4 rc5/bcc_compat, " +
                    "LD_ARM=$SDK/build-tools/18.3.4 rc5/arm-linux-androideabi-ld, " +
                    "LD_X86=$SDK/build-tools/18.3.4 rc5/i686-linux-android-ld, " +
                    "LD_MIPS=$SDK/build-tools/18.3.4 rc5/mipsel-linux-android-ld" +
                    "}>",
                cleanPath(sdk, i.toString()));
    }

    public void testSdkManager_BuildTools_canRunOnJvm() throws IOException {
        LocalSdk sdk = getSdk();
        BuildToolInfo bt = sdk.getBuildTool(new FullRevision(18, 3, 4, 5));
        assertNotNull(bt);

        // By default there is no runtime.properties file and no Runtime.Jvm value.
        // Since there is no requirement, this build-tool package can run everywhere.
        Properties props1 = bt.getRuntimeProps();
        assertTrue(props1.isEmpty());
        assertTrue(bt.canRunOnJvm());

        // We know our tests require at least a JVM 1.5 to run so this build-tool can run here.
        createFileProps("runtime.properties", bt.getLocation(), "Runtime.Jvm", "1.5.0");
        Properties props15 = bt.getRuntimeProps();
        assertFalse(props15.isEmpty());
        assertTrue(bt.canRunOnJvm());

        createFileProps("runtime.properties", bt.getLocation(), "Runtime.Jvm", "42.0.0");
        Properties props42 = bt.getRuntimeProps();
        assertFalse(props42.isEmpty());

        BuildToolInfoWrapper wrap = new BuildToolInfoTest.BuildToolInfoWrapper(bt);

        // Let's assume a real JVM 42.0.0 doesn't exist yet
        wrap.overrideJvmVersion(new NoPreviewRevision(1, 6, 0));
        assertFalse(wrap.canRunOnJvm());

        // Let's assume a real JVM 42.0.0 and above exists
        wrap.overrideJvmVersion(new NoPreviewRevision(42, 0, 0));
        assertTrue(wrap.canRunOnJvm());

        wrap.overrideJvmVersion(new NoPreviewRevision(42, 0, 1));
        assertTrue(wrap.canRunOnJvm());

        wrap.overrideJvmVersion(new NoPreviewRevision(42, 1, 1));
        assertTrue(wrap.canRunOnJvm());

        wrap.overrideJvmVersion(new NoPreviewRevision(43, 1, 1));
        assertTrue(wrap.canRunOnJvm());

    }

    public void testSdkManager_SystemImage() throws Exception {
        LocalSdk sdk = getSdk();
        assertEquals("[PlatformTarget API 0 rev 1]", Arrays.toString(sdk.getTargets()));
        IAndroidTarget t = sdk.getTargets()[0];

        // By default SdkManagerTestCase creates an SDK with one platform containing
        // a legacy armeabi system image.
        assertEquals(
                "[SystemImage tag=default, ABI=armeabi, location in legacy folder='$SDK/platforms/v0_0/images']",
                cleanPath(sdk, Arrays.toString(t.getSystemImages())));

        // 1- add a few "platform subfolders" system images and reload the SDK.
        // This disables the "legacy" mode, which means that although the armeabi
        // target from above is present, it is no longer visible.

        makeSystemImageFolder(new SystemImage(sdk, t,
                LocationType.IN_IMAGES_SUBFOLDER,
                SystemImage.DEFAULT_TAG,
                SdkConstants.ABI_ARMEABI_V7A,
                FileOp.EMPTY_FILE_ARRAY));
        makeSystemImageFolder(new SystemImage(sdk, t,
                LocationType.IN_IMAGES_SUBFOLDER,
                SystemImage.DEFAULT_TAG,
                SdkConstants.ABI_INTEL_ATOM,
                FileOp.EMPTY_FILE_ARRAY));

        sdk = new LocalSdk(sdk.getLocation());
        assertEquals("[PlatformTarget API 0 rev 1]", Arrays.toString(sdk.getTargets()));
        t = sdk.getTargets()[0];

        assertEquals(
                "[SystemImage tag=default, ABI=armeabi-v7a, location in images subfolder='$SDK/platforms/v0_0/images/armeabi-v7a', " +
                 "SystemImage tag=default, ABI=x86, location in images subfolder='$SDK/platforms/v0_0/images/x86']",
                cleanPath(sdk, Arrays.toString(t.getSystemImages())));

        // 2- add arm + arm v7a images using the new SDK/system-images.
        // The x86 one from the platform subfolder is still visible.
        // The armeabi one from the legacy folder is overridden by the new one.
        // The armeabi-v7a one from the platform subfolder is overridden by the new one.

        makeSystemImageFolder(new SystemImage(sdk, t,
                LocationType.IN_SYSTEM_IMAGE,
                SystemImage.DEFAULT_TAG,
                SdkConstants.ABI_ARMEABI,
                FileOp.EMPTY_FILE_ARRAY));
        makeSystemImageFolder(new SystemImage(sdk, t,
                LocationType.IN_SYSTEM_IMAGE,
                SystemImage.DEFAULT_TAG,
                SdkConstants.ABI_ARMEABI_V7A,
                FileOp.EMPTY_FILE_ARRAY));

        sdk = new LocalSdk(sdk.getLocation());
        assertEquals("[PlatformTarget API 0 rev 1]", Arrays.toString(sdk.getTargets()));
        t = sdk.getTargets()[0];

        assertEquals(
                "[SystemImage tag=default, ABI=armeabi, location in system image='$SDK/system-images/android-0/default/armeabi', " +
                 "SystemImage tag=default, ABI=armeabi-v7a, location in system image='$SDK/system-images/android-0/default/armeabi-v7a', " +
                 "SystemImage tag=default, ABI=x86, location in images subfolder='$SDK/platforms/v0_0/images/x86']",
                cleanPath(sdk, Arrays.toString(t.getSystemImages())));

        // 3- add an arm v7a image with a custom tag. It exists in parallel with the default one.

        makeSystemImageFolder(new SystemImage(sdk, t,
                LocationType.IN_SYSTEM_IMAGE,
                new IdDisplay("tag-1", "My Tag 1"),
                SdkConstants.ABI_ARMEABI_V7A,
                FileOp.EMPTY_FILE_ARRAY));

        sdk = new LocalSdk(sdk.getLocation());
        assertEquals("[PlatformTarget API 0 rev 1]", Arrays.toString(sdk.getTargets()));
        t = sdk.getTargets()[0];

        assertEquals(
                "[SystemImage tag=default, ABI=armeabi, location in system image='$SDK/system-images/android-0/default/armeabi', " +
                 "SystemImage tag=default, ABI=armeabi-v7a, location in system image='$SDK/system-images/android-0/default/armeabi-v7a', " +
                 "SystemImage tag=default, ABI=x86, location in images subfolder='$SDK/platforms/v0_0/images/x86', " +
                 "SystemImage tag=tag-1, ABI=armeabi-v7a, location in system image='$SDK/system-images/android-0/tag-1/armeabi-v7a']",
                cleanPath(sdk, Arrays.toString(t.getSystemImages())));
    }

    public void testSdkManager_SystemImage_LegacyOverride() throws Exception {
        LocalSdk sdk = getSdk();
        assertEquals("[PlatformTarget API 0 rev 1]", Arrays.toString(sdk.getTargets()));
        IAndroidTarget t = sdk.getTargets()[0];

        // By default SdkManagerTestCase creates an SDK with one platform containing
        // a legacy armeabi system image.
        assertEquals(
                "[SystemImage tag=default, ABI=armeabi, location in legacy folder='$SDK/platforms/v0_0/images']",
                cleanPath(sdk, Arrays.toString(t.getSystemImages())));

        // Now add a different ABI system image in the new system-images folder.
        // This does not hide the legacy one as long as the ABI type is different
        // (to contrast: having at least one sub-folder in the platform's legacy images folder
        //  will hide the legacy system image. Whereas this does not happen with the new type.)

        makeSystemImageFolder(new SystemImage(sdk, t,
                LocationType.IN_SYSTEM_IMAGE,
                SystemImage.DEFAULT_TAG,
                SdkConstants.ABI_INTEL_ATOM,
                FileOp.EMPTY_FILE_ARRAY));

        sdk = new LocalSdk(sdk.getLocation());
        assertEquals("[PlatformTarget API 0 rev 1]", Arrays.toString(sdk.getTargets()));
        t = sdk.getTargets()[0];

        assertEquals(
                "[SystemImage tag=default, ABI=armeabi, location in legacy folder='$SDK/platforms/v0_0/images', " +
                 "SystemImage tag=default, ABI=x86, location in system image='$SDK/system-images/android-0/default/x86']",
                cleanPath(sdk, Arrays.toString(t.getSystemImages())));

        // Now if we have one new system-image using the same ABI type, it will override the
        // legacy one. This gives us a good path for updates.

        makeSystemImageFolder(new SystemImage(sdk, t,
                LocationType.IN_SYSTEM_IMAGE,
                SystemImage.DEFAULT_TAG,
                SdkConstants.ABI_ARMEABI,
                FileOp.EMPTY_FILE_ARRAY));


        sdk = new LocalSdk(sdk.getLocation());
        assertEquals("[PlatformTarget API 0 rev 1]", Arrays.toString(sdk.getTargets()));
        t = sdk.getTargets()[0];

        assertEquals(
                "[SystemImage tag=default, ABI=armeabi, location in system image='$SDK/system-images/android-0/default/armeabi', " +
                 "SystemImage tag=default, ABI=x86, location in system image='$SDK/system-images/android-0/default/x86']",
                cleanPath(sdk, Arrays.toString(t.getSystemImages())));
    }

    /**
     * Sanitizes the paths used when testing results.
     * <p/>
     * Some methods return absolute paths to the SDK.
     * However the SDK path is actually a randomized location.
     * We clean it by replacing it by the constant '$SDK'.
     * Also all the Windows path separators are converted to unix-like / separators
     * and ".exe" and ".bat" are removed (e.g. for build-tools binaries).
     */
    private String cleanPath(LocalSdk sdk, String string) {
        return string
            .replaceAll(Pattern.quote(sdk.getLocation().getAbsolutePath()), "\\$SDK")  //$NON-NLS-1$
            .replaceAll("\\.(?:bat|exe)", "")                           //$NON-NLS-1$ //$NON-NLS-2$
            .replace('\\', '/');
    }
}
