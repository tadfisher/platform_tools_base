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

package com.android.build.gradle.internal.test.fixture.app

import com.android.build.gradle.internal.test.fixture.SourceFile

/**
 * Created by chiur on 8/13/14.
 */
public class HelloWorldJniApp extends AbstractAndroidTestApp implements AndroidTestApp {
    @Override
    public Collection<SourceFile> getJavaSource() {
        return [
                new SourceFile("main/java/com/example/hellojni", "HelloJni.java",
"""
package com.example.hellojni;

import android.app.Activity;
import android.widget.TextView;
import android.os.Bundle;

public class HelloJni extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Create a TextView and set its content.
         * the text is retrieved by calling a native
         * function.
         */
        TextView  tv = new TextView(this);
        tv.setText( stringFromJNI() );
        setContentView(tv);
    }

    /* A native method that is implemented by the
     * 'hello-jni' native library, which is packaged
     * with this application.
     */
    public native String  stringFromJNI();

    /* this is used to load the 'hello-jni' library on application
     * startup. The library has already been unpacked into
     * /data/data/com.example.hellojni/lib/libhello-jni.so at
     * installation time by the package manager.
     */
    static {
        System.loadLibrary("hello-jni");
    }
}
"""),
        ];
    }

    @Override
    public Collection<SourceFile> getJniSource() {
        return [
                new SourceFile("main/jni", "hello-jni.c",
"""
#include <string.h>
#include <jni.h>

/* This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/hellojni/HelloJni.java
 */
jstring
Java_com_example_hellojni_HelloJni_stringFromJNI(JNIEnv* env, jobject thiz)
{
    return (*env)->NewStringUTF(env, "hello world!");
}
""")
        ];
    }

    @Override
    public Collection<SourceFile> getResSource() {
        return [
                new SourceFile("main/res/values", "strings.xml",
"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">HelloJni</string>
</resources>
""")
        ];
    }

    @Override
    public SourceFile getManifest() {
        return new SourceFile("main", "AndroidManifest.xml",
"""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
      package="com.example.hellojni"
      android:versionCode="1"
      android:versionName="1.0">

    <uses-sdk android:minSdkVersion="3" />
    <application android:label="@string/app_name">
        <activity android:name=".HelloJni"
                  android:label="@string/app_name">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""");
    }

    public Collection<SourceFile> getAndroidTest() {
        return [
                new SourceFile("androidTest/java/com/example/hellojni", "HelloJniTest.java",
"""
package com.example.hellojni;

import android.test.ActivityInstrumentationTestCase;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.example.hellojni.HelloJniTest \
 * com.example.hellojni.tests/android.test.InstrumentationTestRunner
 */
public class HelloJniTest extends ActivityInstrumentationTestCase<HelloJni> {

    public HelloJniTest() {
        super("com.example.hellojni", HelloJni.class);
    }


    public void testJniName() {
        final HelloJni a = getActivity();
        // ensure a valid handle to the activity has been returned
        assertNotNull(a);

        assertTrue("hello world!".equals(a.stringFromJNI()));
    }
}
""")
        ];
    }
}
