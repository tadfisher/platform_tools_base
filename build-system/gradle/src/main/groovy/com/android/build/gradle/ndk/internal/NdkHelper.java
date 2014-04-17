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

import com.android.annotations.Nullable;
import com.google.common.base.Charsets;
import com.google.common.io.Closeables;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.nativebinaries.platform.Platform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class NdkHelper {
    private File ndkFolder;

    public NdkHelper(Project project) {
        ndkFolder = findNdkDir(project);
    }

    @Nullable
    public File getNdkFolder() {
        return ndkFolder;
    }

    private String getArchString(Platform targetPlatform) {
        String archName = targetPlatform.getArchitecture().getName();
        if (archName.equals("x86")) {
            return "arch-x86";
        } else if (archName.equals("arm")) {
            return "arch-arm";
        } else if (archName.equals("mips")) {
            return "arch-mips";
        }
        throw new GradleException("Unrecognized platform: " + archName);
    }

    String getSysroot(Platform targetPlatform, String apiLevel) {
        return ndkFolder + "/platforms/android-" + apiLevel + "/" + getArchString(targetPlatform);
    }

    /*
    "arm-linux-androideabi-4.6",
    "arm-linux-androideabi-4.8",
    "arm-linux-androideabi-clang3.3",
    "arm-linux-androideabi-clang3.4"
    mipsel-linux-android-4.6
    mipsel-linux-android-4.8
    mipsel-linux-android-clang3.3
    mipsel-linux-android-clang3.4
    x86-4.6
    x86-4.8
    x86-clang3.3
    x86-clang3.4
    */

    /*
    String getToolChainPath(String toolchain) {
        return ndkFolder + "/toolchains/" + toolchain + "/prebuilt/" + linux-x86_64/arm-linux-androideabi/bin"
    }
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
}
