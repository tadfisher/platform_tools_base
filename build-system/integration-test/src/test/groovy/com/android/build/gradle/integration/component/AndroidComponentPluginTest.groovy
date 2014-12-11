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

package com.android.build.gradle.integration.component

import aQute.libg.generics.Create
import com.android.build.gradle.integration.common.fixture.GradleTestProject
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

/**
 * Test AndroidComponentModelPlugin.
 */
class AndroidComponentPluginTest {
    @Rule
    public GradleTestProject project = GradleTestProject.builder().create();

    @Test
    void assemble() {
        project.buildFile << """
import com.android.build.gradle.model.AndroidComponentModelPlugin
apply plugin: AndroidComponentModelPlugin

model {
    android.buildTypes {
        custom
    }
    android.productFlavors {
        flavor1
        flavor2
    }
}
"""
        project.execute("assemble")
    }

    @Test
    void flavors() {
        project.buildFile << """
import com.android.build.gradle.model.AndroidComponentModelPlugin
apply plugin: AndroidComponentModelPlugin

model {
    androidBuildTypes {
        custom
    }
    androidProductFlavors {
        flavorDimensions "abi", "price"
        free {
            flavorDimension "price"
        }
        premium {
            flavorDimension "price"
        }
        x86 {
            flavorDimension "abi"
        }
        arm {
            flavorDimension "abi"
        }
    }
}
"""
        Collection<String> tasks = project.getTasks()
        def expectedTasks = [
                "armFreeCustom",
                "armFreeDebug",
                "armFreeRelease",
                "armPremiumCustom",
                "armPremiumDebug",
                "armPremiumRelease",
                "assemble",
                "x86FreeCustom",
                "x86FreeDebug",
                "x86FreeRelease",
                "x86PremiumCustom",
                "x86PremiumDebug",
                "x86PremiumRelease"]
        expectedTasks.each {
            assert tasks.contains(it)
        }
    }
}
