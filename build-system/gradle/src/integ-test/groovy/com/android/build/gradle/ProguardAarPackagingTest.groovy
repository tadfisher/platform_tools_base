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

import com.android.annotations.Nullable
import com.android.build.gradle.internal.model.JavaArtifactImpl
import com.android.build.gradle.internal.test.category.DeviceTests
import com.android.build.gradle.internal.test.fixture.GradleProjectTestRule
import com.android.build.gradle.internal.test.fixture.app.AndroidTestApp
import com.android.build.gradle.internal.test.fixture.app.HelloWorldApp
import com.android.build.gradle.internal.test.fixture.app.TestSourceFile
import com.google.common.collect.Lists
import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.experimental.categories.Category

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static org.junit.Assert.*

/**
 * Integration test of the native plugin with multiple variants.
 */
class ProguardAarPackagingTest {

    @ClassRule
    static public GradleProjectTestRule fixture = new GradleProjectTestRule();

    @BeforeClass
    static public void setup() {
        AndroidTestApp testApp = new HelloWorldApp();

        TestSourceFile oldHelloWorld = testApp.getFile("HelloWorld.java")
        testApp.removeFile(oldHelloWorld)
        testApp.addFile(new TestSourceFile(oldHelloWorld.path, oldHelloWorld.name, """\
                package com.example.helloworld;

                import com.example.libinjar.LibInJar;

                import android.app.Activity;
                import android.os.Bundle;

                public class HelloWorld extends Activity {
                    /** Called when the activity is first created. */
                    @Override
                    public void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.main);
                        LibInJar.method();
                    }
                }
                """.stripIndent()))


        testApp.writeSources(fixture.getSourceDir())

        fixture.getBuildFile() << """\
                apply plugin: 'com.android.library'

                dependencies {
                    compile fileTree(dir: 'libs', include: '*.jar')
                }

                android {
                    compileSdkVersion $GradleProjectTestRule.DEFAULT_COMPILE_SDK_VERSION
                    buildToolsVersion "$GradleProjectTestRule.DEFAULT_BUILD_TOOL_VERSION"

                    resourcePrefix 'lib1_'

                    defaultConfig {
                        minSdkVersion 14
                        targetSdkVersion 15

                    }
                    buildTypes {
                        release {
                            minifyEnabled true
                        }
                    }
                }

                task(assembleLibInJar) {

                }

                """.stripIndent()

        // Make the jar manually
        File buildJarProject = fixture.file("libinjar")
        buildJarProject.mkdirs()
        JavaProject javaProject = new JavaProject(buildJarProject)
        // Copy the generated jar into the android project.
        fixture.file("libs").mkdirs()
        FileUtils.copyFile(javaProject.getJarFile(), fixture.file("libs/libinjar.jar"))
    }

    @Test
    public void checkDebug() {
        fixture.execute("assembleDebug")
        ZipFile aar = new ZipFile(fixture.file("build/outputs/aar/" +
                "com.android.build.gradle.ProguardAarPackagingTest-debug.aar"))
        assertNotNull("debug build arr should contain libinjar",
                aar.getEntry("libs/libinjar.jar"))

        assertFalse("Classes.jar in debug AAR should not contain LibInJar",
                classesJarInAarContainsLibInJar(aar))

    }

    @Test
    public void checkRelease() {
        fixture.execute("assembleRelease")
        ZipFile aar = new ZipFile(fixture.file("build/outputs/aar/" +
                "com.android.build.gradle.ProguardAarPackagingTest-release.aar"))
        assertNull("release build arr should not contain libinjar",
                aar.getEntry("libs/libinjar.jar"))

        assertTrue("Classes.jar in release AAR should contain some of LibInJar",
                classesJarInAarContainsLibInJar(aar))


    }

    private boolean classesJarInAarContainsLibInJar(ZipFile aar) {
        // Extract the classes.jar from the aar file.
        File tempClassesJarFile = fixture.file("temp-classes.jar")
        Files.asByteSink(tempClassesJarFile).writeFrom(
                aar.getInputStream(aar.getEntry("classes.jar")))
        ZipFile classesJar = new ZipFile(tempClassesJarFile)
        // Check whether it contains some of libinjar.
        for (ZipEntry entry : classesJar.entries()) {
            if(entry.name.startsWith("com/example/libinjar")) {
                tempClassesJarFile.delete()
                return true
            }
        }
        tempClassesJarFile.delete()
        return false
    }


    private static class JavaProject {
        private final File testDir;
        JavaProject(File testDir) {
            this.testDir = testDir;
            File libJavaSource = new File(testDir,
                    "src/main/java/com/example/libinjar/LibInJar.java");
            libJavaSource.getParentFile().mkdirs()
            libJavaSource << """\
                    package com.example.libinjar;

                    public class LibInJar {
                        public static void method() {
                            throw new UnsupportedOperationException("Not implemented");
                        }
                    }
                    """.stripIndent()

            new File(testDir, "build.gradle") << "apply plugin: 'java'"

        }

        File getJarFile() {
            execute(null, "assemble")
            return new File(testDir, "build/libs/${testDir.getName()}.jar")
        }


        public void execute(@Nullable OutputStream stdout, String ... tasks) {
        GradleConnector connector = GradleConnector.newConnector();

        ProjectConnection connection = connector
                .useGradleVersion(BasePlugin.GRADLE_TEST_VERSION)
                .forProjectDirectory(testDir)
                .connect();
        try {
            connection.newBuild().forTasks(tasks)
                    .setStandardOutput(stdout)
                    .withArguments(["-i","-u"].toArray(new String[2])).run();
        } finally {
            connection.close();
        }
    }
    }

}
