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

import com.android.annotations.NonNull;
import com.android.build.gradle.integration.common.utils.ApkHelper;
import com.android.ide.common.process.ProcessException;
import com.google.common.truth.ListSubject;
import com.google.common.truth.Truth;

import junit.framework.Assert;

import java.io.File;
import java.util.List;

/**
 * Helper for custom Truth factories.
 */
public class TruthHelper {

    @NonNull
    public static ApkSubject assertThatApk(@NonNull File apk) {
        return Truth.assert_().about(ApkSubjectFactory.apk()).that(apk);
    }

    @NonNull
    public static ZipFileSubject assertThatZip(@NonNull File file) {
        return Truth.assert_().about(ZipFileSubjectFactory.zipFile()).that(file);
    }

    @NonNull
    public static ListSubject assertThatLocalesForApk(@NonNull File apk) throws ProcessException {
        List<String> locales = ApkHelper.getLocales(apk);

        if (locales == null) {
            Assert.fail(String.format("locales not found in badging output for %s", apk));
        }

        return Truth.assertThat(locales);
    }
}
