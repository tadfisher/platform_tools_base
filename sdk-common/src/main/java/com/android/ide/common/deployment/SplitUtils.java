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

package com.android.ide.common.deployment;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ddmlib.IDevice;
import com.android.resources.Density;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.List;

/**
 * Utilities methods to filter split APKs based on the deployment device characteristics.
 */
public class SplitUtils {

    private static final String PROP_DEVICE_DENSITY = "ro.sf.lcd_density";
    private static final String PROP_USER_LANGUAGE = "persist.sys.language";
    private static final String PROP_USER_COUNTRY = "persist.sys.country";


    public List<File> filter(IDevice iDevice, File[] allAPKs) {
        List<ApkInfo> apkInfos = introspect(allAPKs);
        String deviceDensity = getDeviceDensity(iDevice);
        ImmutableList.Builder<File> filteredAPKs = ImmutableList.builder();
        for (ApkInfo apkInfo : apkInfos) {
            if (apkInfo.mApkType == ApkType.MAIN || (apkInfo.mDensity != null &&
                    String.valueOf(apkInfo.mDensity.getDpiValue()).equals(deviceDensity))) {
                filteredAPKs.add(apkInfo.mApk);
            }
        }
        // we should check that we have at least one density (lower one I suppose).
        return filteredAPKs.build();
    }

    public List<ApkInfo> introspect(File[] apks) {
        ImmutableList.Builder<ApkInfo> characteristicBuilder = ImmutableList.builder();
        for (File apk : apks) {
            // this is non optimal, consider reading the <split> attribute in the
            // APK's AndroidManifest.xml file
            String fileName = apk.getName();
            String densityValue = fileName.substring(fileName.lastIndexOf('-') + 1,
                    fileName.length() - ".apk".length());
            Density density = Density.getEnum(densityValue);
            if (density != null) {
                characteristicBuilder.add(new ApkInfo(ApkType.SPLIT, density, apk));
            } else {
                characteristicBuilder.add(new ApkInfo(ApkType.MAIN, null /* density */, apk));
            }
        }
        return characteristicBuilder.build();
    }

    private String getDeviceDensity(IDevice iDevice) {
        return iDevice.getProperty(PROP_DEVICE_DENSITY);
    }

    private enum ApkType {
        MAIN, SPLIT
    }

    private static class ApkInfo {
        @NonNull
        private final ApkType mApkType;
        @Nullable
        private final Density mDensity;
        @NonNull
        private final File mApk;

        private ApkInfo(@NonNull ApkType apkType, @Nullable Density density,
                @NonNull File apk) {
            mApkType = apkType;
            mDensity = density;
            mApk = apk;
        }
    }

}
