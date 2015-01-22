/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.build.gradle.integration.common.truth;

import static com.google.common.truth.Truth.assertThat;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.annotations.concurrency.Immutable;
import com.android.build.gradle.integration.common.utils.ApkHelper;
import com.android.build.gradle.integration.common.utils.SdkHelper;
import com.android.builder.core.ApkInfoParser;
import com.android.ide.common.process.DefaultProcessExecutor;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessExecutor;
import com.android.ide.common.process.ProcessInfoBuilder;
import com.android.utils.StdLogger;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.google.common.truth.CollectionSubject;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.IterableSubject;
import com.google.common.truth.ListSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;

import junit.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Truth support for apk files.
 */
public class ApkSubject extends Subject<ApkSubject, File> {

    private static final Pattern PATTERN_CLASS_DESC = Pattern.compile(
            "^Class descriptor\\W*:\\W*'(L.+;)'$");

    private static final Pattern PATTERN_MAX_SDK_VERSION = Pattern.compile(
            "^maxSdkVersion\\W*:\\W*'(.+)'$");

    public ApkSubject(FailureStrategy failureStrategy, File subject) {
        super(failureStrategy, subject);
    }

    @NonNull
    public ListSubject locales() throws ProcessException {
        File apk = getSubject();
        List<String> locales = ApkHelper.getLocales(apk);

        if (locales == null) {
            Assert.fail(String.format("locales not found in badging output for %s", apk));
        }

        return assertThat(locales);
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void containsClass(String className) throws IOException, ProcessException {
        if (!checkForClass(className)) {
            failWithRawMessage("'%s' does not contain '%s'", getDisplaySubject(), className);
        }
    }

    public void doesNotContainClass(String className) throws IOException, ProcessException {
        if (checkForClass(className)) {
            failWithRawMessage("'%s' unexpectedly contains '%s'", getDisplaySubject(), className);
        }
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void containsEntry(String entryName) throws IOException {
        ZipFile zipFile = new ZipFile(getSubject());
        try {
            ZipEntry zipEntry = zipFile.getEntry(entryName);
            if (zipEntry == null) {
                failWithRawMessage("%s does not contain '%s'", getDisplaySubject(), entryName);
            }
        } finally {
            zipFile.close();
        }
    }

    /**
     * Returns a {@link IterableSubject} of all the APK entries which name matches the passed
     * regular expression.
     *
     * @param conformingTo a regular expression to match entries we are interested in.
     * @return a {@link IterableSubject} propositions for matching entries.
     * @throws IOException of the zip file cann be opened.
     */
    public IterableSubject<? extends IterableSubject<?, String, List<String>>, String, List<String>>
        entries(String conformingTo) throws IOException {

        ImmutableList.Builder<String> entries = ImmutableList.builder();
        Pattern pattern = Pattern.compile(conformingTo);
        ZipFile zipFile = new ZipFile(getSubject());
        try {
            Enumeration<? extends ZipEntry> zipFileEntries = zipFile.entries();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry zipEntry = zipFileEntries.nextElement();
                if (pattern.matcher(zipEntry.getName()).matches()) {
                    entries.add(zipEntry.getName());
                }
            }
        } finally {
            zipFile.close();
        }
        return assertThat(entries.build());
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void hasVersionCode(int versionCode) throws ProcessException {
        File apk = getSubject();

        ApkInfoParser.ApkInfo apkInfo = getApkInfo(apk);

        Integer actualVersionCode = apkInfo.getVersionCode();
        if (actualVersionCode == null) {
            failWithRawMessage("Unable to query %s for versionCode", getDisplaySubject());
        }

        if (!apkInfo.getVersionCode().equals(versionCode)) {
            failWithBadResults("has versionCode", versionCode, "is", actualVersionCode);
        }
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void hasVersionName(String versionName) throws ProcessException {
        File apk = getSubject();

        ApkInfoParser.ApkInfo apkInfo = getApkInfo(apk);

        String actualVersionName = apkInfo.getVersionName();
        if (actualVersionName == null) {
            failWithRawMessage("Unable to query %s for versionName", getDisplaySubject());
        }

        if (!apkInfo.getVersionName().equals(versionName)) {
            failWithBadResults("has versionName", versionName, "is", actualVersionName);
        }
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void hasMaxSdkVersion(int maxSdkVersion) throws ProcessException {

        List<String> output = ApkHelper.getApkBadging(getSubject());

        checkMaxSdkVersion(output, maxSdkVersion);
    }

    @Override
    protected String getDisplaySubject() {
        String name = (internalCustomName() == null) ? "" : "\"" + internalCustomName() + "\" ";
        return name + "<" + getSubject().getName() + ">";
    }

    /**
     * Returns true if the provided class is present in the file.
     * @param expectedClassName the class name in the format Lpkg1/pk2/Name;
     */
    private boolean checkForClass(
            @NonNull String expectedClassName)
            throws ProcessException, IOException {
        // get the dexdump exec
        File dexDump = SdkHelper.getDexDump();

        ProcessExecutor executor = new DefaultProcessExecutor(
                new StdLogger(StdLogger.Level.ERROR));

        ProcessInfoBuilder builder = new ProcessInfoBuilder();
        builder.setExecutable(dexDump);
        builder.addArgs(getSubject().getAbsolutePath());

        List<String> output = ApkHelper.runAndGetOutput(builder.createProcess(), executor);

        for (String line : output) {
            Matcher m = PATTERN_CLASS_DESC.matcher(line.trim());
            if (m.matches()) {
                String className = m.group(1);
                if (expectedClassName.equals(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    @NonNull
    private static ApkInfoParser.ApkInfo getApkInfo(File apk) throws ProcessException {
        ProcessExecutor processExecutor = new DefaultProcessExecutor(
                new StdLogger(StdLogger.Level.ERROR));
        ApkInfoParser parser = new ApkInfoParser(SdkHelper.getAapt(), processExecutor);
        return parser.parseApk(apk);
    }

    @VisibleForTesting
    void checkMaxSdkVersion(List<String> output, int maxSdkVersion) {
        for (String line : output) {
            Matcher m = PATTERN_MAX_SDK_VERSION.matcher(line.trim());
            if (m.matches()) {
                String actual = m.group(1);
                try {
                    Integer i = Integer.parseInt(actual);
                    if (!i.equals(maxSdkVersion)) {
                        failWithBadResults("has maxSdkVersion", maxSdkVersion, "is", i);
                    }
                    return;
                } catch (NumberFormatException e) {
                    failureStrategy.fail(
                            String.format(
                                    "maxSdkVersion in badging for %s is not a number: %s",
                                    getDisplaySubject(), actual),
                            e);
                }
            }
        }

        failWithRawMessage("maxSdkVersion not found in badging output for %s", getDisplaySubject());
    }
}
