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

package com.android.builder.core;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.ide.common.internal.CommandLineRunner;
import com.android.ide.common.internal.LoggedErrorException;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to parse an APK with aapt to gather information
 */
public class ApkInfoParser {

    @NonNull
    private final File mAaptFile;
    @NonNull
    private final CommandLineRunner mCommandLineRunner;

    /**
     * Information about an APK
     */
    public static final class ApkInfo {
        @NonNull
        private final String mPackageName;
        @Nullable
        private final Integer mVersionCode;
        @Nullable
        private final String mVersionName;

        private ApkInfo(@NonNull String packageName, int versionCode, String versionName) {
            mPackageName = packageName;
            mVersionCode = versionCode;
            mVersionName = versionName;
        }

        @NonNull
        public String getPackageName() {
            return mPackageName;
        }

        @Nullable
        public Integer getVersionCode() {
            return mVersionCode;
        }

        @Nullable
        public String getVersionName() {
            return mVersionName;
        }

        @Override
        public String toString() {
            return "ApkInfo{" +
                    "mPackageName='" + mPackageName + '\'' +
                    ", mVersionCode=" + mVersionCode +
                    ", mVersionName='" + mVersionName + '\'' +
                    '}';
        }
    }

    public ApkInfoParser(@NonNull File aaptFile, @NonNull CommandLineRunner commandLineRunner) {
        mAaptFile = aaptFile;
        mCommandLineRunner = commandLineRunner;
    }

    @NonNull
    public ApkInfo parseApk(@NonNull File apkFile)
            throws InterruptedException, LoggedErrorException, IOException {

        if (!mAaptFile.isFile()) {
            throw new IllegalStateException(
                    "aapt is missing from location: " + mAaptFile.getAbsolutePath());
        }

        final List<String> aaptOutput = getAaptOutput(apkFile);

        return getApkInfo(aaptOutput);
    }

    @VisibleForTesting
    static ApkInfo getApkInfo(List<String> aaptOutput) {
        Pattern p = Pattern.compile("^package: name='([^']+)' versionCode='([0-9]*)' versionName='([^']*)'.*$");

        String pkgName = null, versionCode = null, versionName = null;

        for (String line : aaptOutput) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                pkgName = m.group(1);
                versionCode = m.group(2);
                versionName = m.group(3);
                break;
            }
        }

        if (pkgName == null) {
            throw new RuntimeException("Failed to find apk information with aapt");
        }

        return new ApkInfo(pkgName, Integer.parseInt(versionCode), versionName);
    }

    private List<String> getAaptOutput(@NonNull File apkFile)
            throws IOException, InterruptedException, LoggedErrorException {
        // launch aapt: create the command line
        ArrayList<String> command = Lists.newArrayList();

        command.add(mAaptFile.getAbsolutePath());
        command.add("dump");
        command.add("badging");
        command.add(apkFile.getPath());

        final List<String> aaptOutput = Lists.newArrayList();

        mCommandLineRunner.runCmdLine(command, new CommandLineRunner.CommandLineOutput() {
            @Override
            public void out(@Nullable String line) {
                if (line != null) {
                    aaptOutput.add(line);
                }
            }
            @Override
            public void err(@Nullable String line) {
                super.err(line);

            }
        }, null /*env vars*/);
        return aaptOutput;
    }
}
