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

package com.android.build.gradle.tasks

import com.android.build.gradle.internal.dsl.AaptOptionsImpl
import com.android.build.gradle.internal.tasks.BaseTask
import com.android.builder.core.AaptPackageCommandBuilder
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Generates all metadata (like AndroidManifest.xml) necessary for a ABI dimension split APK.
 */
class GenerateSplitAbiRes extends BaseTask {

    @Input
    String applicationId

    @Input
    int versionCode

    @Input
    @Optional
    String versionName

    @Input
    String outputBaseName

    @Nested
    Set<String> splits

    @OutputDirectory
    File outputDirectory

    @Input
    boolean debuggable

    @Nested
    AaptOptionsImpl aaptOptions

    @TaskAction
    protected void doFullTaskAction() {

        for (String split : splits) {
            String resPackageFileName = new File(outputDirectory, "resources-${outputBaseName}-${split}.ap_")

            File tmpDirectory = new File(outputDirectory, "${outputBaseName}")
            tmpDirectory.mkdirs()

            File tmpFile = new File(tmpDirectory, "AndroidManifest.xml")

            OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8");
            if (versionName == null) {
                versionName = String.valueOf(versionCode)
            }
            try {
                fileWriter.append(
                        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                                "      package=\"" + applicationId + "\"\n" +
                                "      android:versionCode=\"" + versionCode + "\"\n" +
                                "      android:versionName=\"" + versionName + "\"\n" +
                                "      split=\"lib_${outputBaseName}\">\n" +
                                "       <uses-sdk android:minSdkVersion=\"21\"/>\n" +
                                "</manifest> ")
                fileWriter.flush()
            } finally {
                fileWriter.close()
            }

            AaptPackageCommandBuilder aaptPackageCommandBuilder =
                    new AaptPackageCommandBuilder(tmpFile, getAaptOptions())
                        .setDebuggable(getDebuggable())
                        .setResPackageOutput(resPackageFileName);

            getBuilder().processResources(aaptPackageCommandBuilder, false /* enforceUniquePackageName */)
        }
    }
}
