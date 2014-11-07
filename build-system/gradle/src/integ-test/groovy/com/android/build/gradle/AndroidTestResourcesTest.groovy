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

import com.android.build.gradle.internal.test.fixture.GradleProjectTestRule
import com.android.build.gradle.internal.test.fixture.app.AndroidTestApp
import com.android.build.gradle.internal.test.fixture.app.HelloWorldApp
import com.android.build.gradle.internal.test.fixture.app.TestSourceFile
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import static org.junit.Assert.*

/**
 * Check resources in androidTest are available in the generated R.java.
 */
class AndroidTestResourcesTest {

    public static final BUILD_GRADLE_ANDROID_SECTION = """
            android {
                compileSdkVersion $GradleProjectTestRule.DEFAULT_COMPILE_SDK_VERSION
                buildToolsVersion "$GradleProjectTestRule.DEFAULT_BUILD_TOOL_VERSION"
            }
            """.stripIndent()

    private static String baseBuildFile

    @ClassRule
    static public GradleProjectTestRule fixture = new GradleProjectTestRule()


    @BeforeClass
    static void setup() {
        AndroidTestApp testApp =  new HelloWorldApp();

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

        // This class exists to prevent the resource from being automatically removed.
        // The test itself is not interesting.
        testApp.addFile(new TestSourceFile("androidTest/java/com/example/helloworld",
                "HelloWorldResourceTest.java\"", """\
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

        baseBuildFile = fixture.getBuildFile().getText("UTF-8")

        testApp.writeSources(fixture.getSourceDir())
    }

    @Test
    public void checkLayoutInRApp() {
        fixture.getBuildFile().setText(baseBuildFile +
                "\napply plugin: 'com.android.application'\n" +
                BUILD_GRADLE_ANDROID_SECTION);
        fixture.execute("assembleDebugTest")
        checkLayoutInR()
    }

    @Test
    public void checkLayoutInRLibrary() {
        fixture.getBuildFile().setText(baseBuildFile +
                "\napply plugin: 'com.android.library'\n" +
                BUILD_GRADLE_ANDROID_SECTION);

        fixture.execute("assembleDebugTest")
        checkLayoutInR()
    }

    private void checkLayoutInR() {
        def rFile = fixture.file("build/generated/source/r/test/debug/" +
                "com/example/helloworld/test/R.java")
        assertTrue("Did not generate R file", rFile.exists())
        def rFileContents = rFile.getText("UTF-8")

        assertTrue("Test/debug R file should contain test_layout_1",
                rFileContents.contains('test_layout_1'))
        assertTrue("Test/debug R file should contain test_layout_1_textview",
                rFileContents.contains('test_layout_1_textview'))
    }
}
