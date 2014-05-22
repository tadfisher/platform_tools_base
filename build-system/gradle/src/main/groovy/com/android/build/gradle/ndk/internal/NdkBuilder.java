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

package com.android.build.gradle.ndk.internal;

import static com.android.SdkConstants.FN_LOCAL_PROPERTIES;

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.android.build.gradle.ndk.NdkExtension;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closeables;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.nativebinaries.platform.Platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

public class NdkBuilder {
    private NdkExtension ndkExtension;
    private File ndkFolder;


    private static final Map<String, String> PLATFORM_STRING = ImmutableMap.of(
            SdkConstants.ABI_INTEL_ATOM, "x86",
            SdkConstants.ABI_ARMEABI_V7A, "arm-linux-androideabi",
            SdkConstants.ABI_ARMEABI, "arm-linux-androideabi",
            SdkConstants.ABI_MIPS, "mipsel-linux-android");

    private static final Map<String, String> TOOLCHAIN_STRING = ImmutableMap.of(
            "gcc", "",
            "clang", "clang");


    public NdkBuilder(Project project, NdkExtension ndkExtension) {
        ndkFolder = findNdkDir(project);
        this.ndkExtension = ndkExtension;
    }

    @Nullable
    public File getNdkFolder() {
        return ndkFolder;
    }

    NdkExtension getNdkExtension() {
        return ndkExtension;
    }

    /**
     * Returns the sysroot directory for the toolchain.
     */
    String getSysroot(Platform targetPlatform) {
        return ndkFolder + "/platforms/android-" + ndkExtension.getApiLevel()
                + "/arch-" + targetPlatform.getArchitecture().getName();
    }

    /**
     * Determine the location of the NDK directory.
     */
    private File findNdkDir(Project project) {
        File rootDir = project.getRootDir();
        File localProperties = new File(rootDir, FN_LOCAL_PROPERTIES);

        if (localProperties.isFile()) {

            Properties properties = new Properties();
            InputStreamReader reader = null;
            try {
                //noinspection IOResourceOpenedButNotSafelyClosed
                FileInputStream fis = new FileInputStream(localProperties);
                reader = new InputStreamReader(fis, Charsets.UTF_8);
                properties.load(reader);
            } catch (FileNotFoundException ignored) {
                // ignore since we check up front and we don't want to fail on it anyway
                // in case there's an env var.
            } catch (IOException e) {
                throw new RuntimeException("Unable to read ${localProperties}", e);
            } finally {
                Closeables.closeQuietly(reader);
            }

            String ndkDirProp = properties.getProperty("ndk.dir");
            if (ndkDirProp != null) {
                return new File(ndkDirProp);
            }

        } else {
            String envVar = System.getenv("ANDROID_NDK_HOME");
            if (envVar != null) {
                return new File(envVar);
            }
        }
        return null;
    }

    public static String getToolchainName(
            String toolchain,
            String toolchainVersion,
            String platform) {
        return PLATFORM_STRING.get(platform) + "-" + TOOLCHAIN_STRING.get(toolchain)
                + toolchainVersion;
    }


    public File getToolchainPath(String toolchain, String toolchainVersion, String platform) {
        File prebuiltFolder;
        if (toolchain.equals("gcc")) {
            prebuiltFolder = new File(
                    getNdkFolder(),
                    "toolchains/" + getToolchainName(toolchain, toolchainVersion, platform)
                            + "/prebuilt");

        } else if (toolchain.equals("clang")) {
            prebuiltFolder = new File(
                    getNdkFolder(),
                    "toolchains/llvm-" + toolchainVersion + "/prebuilt");
        } else {
            throw new GradleException("Unrecognized toolchain: " + toolchain);
        }
        System.out.println(prebuiltFolder);

        // This should detect the host architecture to determine the path of the prebuilt toolchain
        // instead of assuming there is only one folder in prebuilt directory.
        File[] toolchainFolder = prebuiltFolder.listFiles();
        System.out.println(toolchainFolder);
        if (toolchainFolder.length != 1) {
            throw new GradleException("Unable to find toolchain prebuilt folder in: "
                    + prebuiltFolder);
        }
        return toolchainFolder[0];
        //return new File(toolchainFolder[0], getPrefix(toolchain, platform) + "/bin");
        //return new File(toolchainFolder[0], getPrefix(toolchain, platform));
    }

}
