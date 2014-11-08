package com.android.test.application

import com.android.SdkConstants
import com.android.annotations.Nullable
import com.android.build.gradle.BasePlugin
import com.android.builder.model.AndroidProject
import com.android.test.common.fixture.GradleTestProject
import com.android.test.common.fixture.app.AbstractAndroidTestApp
import com.android.test.common.fixture.app.AndroidTestApp
import com.android.test.common.fixture.app.HelloWorldApp
import com.android.test.common.fixture.app.TestSourceFile
import com.google.common.base.Joiner
import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.apache.tools.ant.taskdefs.condition.And
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static org.junit.Assert.*
 /**
  * Integration test to check that libraries included directly as jar files are correctly handled
  * when using proguard.
 */
class ProguardAarPackagingTest {
    @ClassRule
    static public GradleTestProject fixture =
            GradleTestProject.builder().withName("mainProject").create();
    @ClassRule
    static public GradleTestProject libInJarFixture =
            GradleTestProject.builder().withName("libInJar").create();

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
                    compileSdkVersion $GradleTestProject.DEFAULT_COMPILE_SDK_VERSION
                    buildToolsVersion "$GradleTestProject.DEFAULT_BUILD_TOOL_VERSION"

                    buildTypes {
                        release {
                            minifyEnabled true
                        }
                    }
                }
                """.stripIndent()


        JavaProject javaProject = new JavaProject()
        javaProject.writeSources(libInJarFixture.getSourceDir())
        libInJarFixture.buildFile << "apply plugin: 'java'"
        libInJarFixture.execute("assemble")

        // Copy the generated jar into the android project.
        fixture.file("libs").mkdirs()
        FileUtils.copyFile(JavaProject.getJarFile(libInJarFixture),
                fixture.file("libs/libinjar.jar"))
    }

    @Test
    public void checkDebug() {
        fixture.execute("assembleDebug")
        ZipFile aar = new ZipFile(fixture.file(
                Joiner.on(File.separatorChar).join("build", AndroidProject.FD_OUTPUTS, "aar",
                        fixture.name + "-debug" + SdkConstants.DOT_AAR)))
        assertNotNull("debug build arr should contain libinjar",
                aar.getEntry("libs/libinjar.jar"))

        assertFalse("Classes.jar in debug AAR should not contain LibInJar",
                classesJarInAarContainsLibInJar(aar))

    }

    @Test
    public void checkRelease() {
        fixture.execute("assembleRelease")
        ZipFile aar = new ZipFile(fixture.file(
                Joiner.on(File.separatorChar).join("build", AndroidProject.FD_OUTPUTS, "aar",
                        fixture.name + "-release" + SdkConstants.DOT_AAR)))
        assertNull("release build arr should not contain libinjar",
                aar.getEntry("libs/libinjar.jar"))

        assertTrue("Classes.jar in release AAR should contain some of LibInJar",
                classesJarInAarContainsLibInJar(aar))
    }

    private boolean classesJarInAarContainsLibInJar(ZipFile aar) {
        // Extract the classes.jar from the aar file.
        File tempClassesJarFile = fixture.file("temp-classes.jar")
        try {
            Files.asByteSink(tempClassesJarFile).writeFrom(
                    aar.getInputStream(aar.getEntry("classes.jar")))
            ZipFile classesJar = new ZipFile(tempClassesJarFile)
            // Check whether it contains some of libinjar.
            for (ZipEntry entry : classesJar.entries()) {
                // Proguard can name the classes however it likes, so this just checks for the package.
                if (entry.name.startsWith("com/example/libinjar")) {
                    return true
                }
            } // Failed to find.
            return false
        } finally {
            tempClassesJarFile.delete()
        }
    }

    /**
     * This is used to generate a jar to be included as a local dependency
     * via compile fileTree(dir: 'libs', include: '*.jar').
     */
    private static class JavaProject extends AbstractAndroidTestApp {

        static final TestSourceFile JAVA_SOURCE =
                new TestSourceFile("main/java/com/example/libinjar", "LibInJar.java", """\
                        package com.example.libinjar;

                        public class LibInJar {
                            public static void method() {
                                throw new UnsupportedOperationException("Not implemented");
                            }
                        }
                        """.stripIndent())

        static File getJarFile(GradleTestProject fixture) {
            return new File(fixture.getTestDir(), "build" + File.separatorChar +
                    "libs" + File.separatorChar + fixture.getName() + ".jar")
        }

        JavaProject() {
            addFile(JAVA_SOURCE)
        }
    }
}
