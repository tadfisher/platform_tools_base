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
import com.android.build.gradle.internal.test.fixture.app.HelloWorldApp
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import static org.junit.Assert.*

/**
 * Check resources in androidTest are available in the generated R.java.
 */
class AndroidTestResourcesTest {

    private static final LAYOUT_FILE_CONTENT =  """\
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                          android:layout_width="match_parent"
                          android:layout_height="match_parent"
                          android:orientation="vertical" >
                <TextView android:id="@+id/test_layout_1_textview"
                          android:layout_width="wrap_content"
                          android:layout_height="wrap_content"
                          android:text="Hello, I am a TextView" />
                <Button android:id="@+id/test_layout_1_button"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Hello, I am a Button" />
            </LinearLayout>
        """.stripIndent()

    private static final TEST_CLASS_CONTENT = """\
            package com.example.helloworld;
            import android.test.ActivityInstrumentationTestCase2;
            import android.test.suitebuilder.annotation.MediumTest;
            import android.widget.TextView;
            import android.widget.Toast;
            import java.util.concurrent.atomic.AtomicBoolean;

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
            """.stripIndent()

    @ClassRule
    static public GradleProjectTestRule fixture = new GradleProjectTestRule();

    @BeforeClass
    static void setup() {
        new HelloWorldApp().writeSources(fixture.getSourceDir())
        
        File layout_file = new File(fixture.getSourceDir(),
                "androidTest/res/layout/test_layout_1.xml")
        layout_file.getParentFile().mkdirs();
        layout_file << LAYOUT_FILE_CONTENT

        File test_class = new File(fixture.getSourceDir(),
                "androidTest/java/com/example/helloworld/HelloWorldResourceTest.java")
        test_class.getParentFile().mkdirs()
        test_class << TEST_CLASS_CONTENT
    }

    @Test
    public void checkLayoutInRApp() {
        fixture.getBuildFile() << """\
            apply plugin: 'com.android.application'

            android {
                compileSdkVersion 19
                buildToolsVersion "20.0.0"
            }
        """.stripIndent()
        fixture.execute("assembleDebugTest")
        checkLayoutInR()
    }

    @Test
    public void checkLayoutInRLibrary() {
        fixture.getBuildFile() << """\
            apply plugin: 'com.android.application'

            android {
                compileSdkVersion 19
                buildToolsVersion "20.0.0"
            }
        """.stripIndent()

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
