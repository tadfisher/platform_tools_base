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

package com.android.test.application

import com.android.builder.model.AndroidProject
import com.android.test.common.category.DeviceTests
import com.android.test.common.fixture.GradleTestProject
import com.android.test.common.fixture.app.AndroidTestApp
import com.android.test.common.fixture.app.HelloWorldApp
import com.android.test.common.fixture.app.TestSourceFile
import com.google.common.base.Joiner
import org.junit.*
import org.junit.experimental.categories.Category

import static org.junit.Assert.*

/**
 * Check resources in androidTest are available in the generated R.java.
 */
class AndroidTestResourcesTest {

    private static final BUILD_GRADLE_ANDROID_SECTION = """
            android {
                compileSdkVersion $GradleTestProject.DEFAULT_COMPILE_SDK_VERSION
                buildToolsVersion "$GradleTestProject.DEFAULT_BUILD_TOOL_VERSION"
            }
            """.stripIndent()


    private static final AndroidTestApp testApp
    static {
        testApp =  new HelloWorldApp()

        testApp.addFile(new TestSourceFile("androidTest/res/layout", "test_layout_1.xml", """\
                <?xml version="1.0" encoding="utf-8"?>
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                              android:layout_width="match_parent"
                              android:layout_height="match_parent"
                              android:orientation="vertical" >
                    <TextView android:id="@+id/test_layout_1_textview"
                              android:layout_width="wrap_content"
                              android:layout_height="wrap_content"
                              android:text="Hello, I am a TextView" />
                </LinearLayout>
                """.stripIndent()))

        // This class exists to prevent the resource from being automatically removed,
        // if we start filtering test resources by default.
        testApp.addFile(new TestSourceFile("androidTest/java/com/example/helloworld",
                "HelloWorldResourceTest.java", """\
                package com.example.helloworld;
                import android.test.ActivityInstrumentationTestCase2;
                import android.test.suitebuilder.annotation.MediumTest;
                import android.widget.TextView;

                public class HelloWorldResourceTest extends
                        ActivityInstrumentationTestCase2<HelloWorld> {
                    private TextView mTextView;

                    public HelloWorldResourceTest() {
                        super("com.example.helloworld", HelloWorld.class);
                    }

                    @Override
                    protected void setUp() throws Exception {
                        super.setUp();
                        final HelloWorld a = getActivity();
                        mTextView = (TextView) a.findViewById(
                                com.example.helloworld.test.R.id.test_layout_1_textview);
                    }

                    @MediumTest
                    public void testPreconditions() {
                        assertNull("Shouldn't find test_layout_1_textview.", mTextView);
                    }
                }
                """.stripIndent()))
    }

    @ClassRule
    public static GradleTestProject appFixture = new GradleTestProject("application", null)
    @ClassRule
    public static GradleTestProject libFixture = new GradleTestProject("library", null)

    /**
     * Use the test app to create an application and a library project.
     */
    @BeforeClass
    static void setup() {
        testApp.writeSources(appFixture.getSourceDir())
        appFixture.getBuildFile() << """
                apply plugin: 'com.android.application'
                """.stripIndent() + BUILD_GRADLE_ANDROID_SECTION
        appFixture.execute("assembleDebugTest")

        testApp.writeSources(libFixture.getSourceDir())
        libFixture.getBuildFile() << """
                apply plugin: 'com.android.library'
                """.stripIndent() + BUILD_GRADLE_ANDROID_SECTION
        libFixture.execute("assembleDebugTest")
    }

    @Test
    public void checkLayoutInRApp() {
        checkLayoutInR(appFixture)
    }

    @Test
    public void checkLayoutInRLibrary() {
        checkLayoutInR(libFixture)
    }

    @Test
    @Category(DeviceTests.class)
    void checkLayoutUseInApp() {
        appFixture.execute("connectedAndroidTest")
    }


    private void checkLayoutInR(GradleTestProject fixture) {
        def rFile = fixture.file(Joiner.on(File.separatorChar).join(
                "build", AndroidProject.FD_GENERATED, "source", "r",
                "test", "debug", "com", "example", "helloworld", "test",  "R.java"))
        assertTrue("Should have generated R file", rFile.exists())
        def rFileContents = rFile.getText("UTF-8")

        assertTrue("Test/debug R file [${rFile.absolutePath}] should contain test_layout_1",
                rFileContents.contains('test_layout_1'))
        assertTrue("Test/debug R file [${rFile.absolutePath}] " +
                        "should contain test_layout_1_textview",
                rFileContents.contains('test_layout_1_textview'))
    }
}
