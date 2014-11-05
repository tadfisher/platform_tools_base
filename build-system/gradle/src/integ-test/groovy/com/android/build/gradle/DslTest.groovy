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



package com.android.build.gradle
import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.build.gradle.internal.test.fixture.GradleProjectTestRule
import com.android.build.gradle.internal.test.fixture.app.HelloWorldApp
import com.android.ide.common.internal.CommandLineRunner
import com.android.ide.common.internal.LoggedErrorException
import com.android.utils.StdLogger
import com.google.common.collect.Lists
import groovy.util.slurpersupport.GPathResult
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class DslTest {

    @Rule
    public GradleProjectTestRule fixture = new GradleProjectTestRule();

    @Before
    public void setup() {
        new HelloWorldApp().writeSources(fixture.getSourceDir())
        fixture.getBuildFile() << """
apply plugin: 'com.android.application'

android {
    compileSdkVersion 19
    buildToolsVersion "20.0.0"
}
"""
    }

    @Test
    public void versionNameSuffix() {
        fixture.getBuildFile() << """
android {
    defaultConfig {
        versionName 'foo'
    }

    buildTypes {
        debug {
            versionNameSuffix '-suffix'
        }
    }
}
"""
        // no need to do a full build. Let's just run the manifest task.
        fixture.execute("processDebugManifest")

        File manifestFile = fixture.file(
                "build/intermediates/manifests/full/debug/AndroidManifest.xml")

        GPathResult xml = new XmlSlurper().parse(manifestFile).declareNamespace(
                android: 'http://schemas.android.com/apk/res/android')

        String versionName = xml.'@android:versionName'.text()

        assertNotNull(versionName)
        assertEquals("foo-suffix", versionName);
    }

    private void checkVersionName(@NonNull File apk, @NonNull String expectedVersionName)
            throws IOException, InterruptedException, LoggedErrorException {
        File aapt = new File(fixture.getSdkDir(), "build-tools/20.0.0/aapt");

        assertTrue("Test requires build-tools 20.0.0", aapt.isFile());

        String[] command = new String[4];
        command[0] = aapt.getPath();
        command[1] = "dump";
        command[2] = "badging";
        command[3] = apk.getPath();

        CommandLineRunner commandLineRunner = new CommandLineRunner(
                new StdLogger(StdLogger.Level.ERROR));

        final List<String> aaptOutput = Lists.newArrayList();

        commandLineRunner.runCmdLine(command, new CommandLineRunner.CommandLineOutput() {
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

        Pattern p = Pattern.compile("^package: name='(.+)' versionCode='([0-9]*)' versionName='(.*)'\$");

        String versionCode = null;
        String versionName = null;

        for (String line : aaptOutput) {
            Matcher m = p.matcher(line);
            if (m.matches()) {
                versionCode = m.group(2);
                versionName = m.group(3);
                break;
            }
        }

        assertNotNull("Unable to determine version code", versionCode);
        assertNotNull("Unable to determine version name", versionName);

        assertEquals(expectedVersionName, versionName);
    }
}
