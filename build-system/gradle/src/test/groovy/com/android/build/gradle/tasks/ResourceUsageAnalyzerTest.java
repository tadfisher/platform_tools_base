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

package com.android.build.gradle.tasks;

import static java.io.File.separatorChar;

import com.android.annotations.NonNull;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class ResourceUsageAnalyzerTest extends TestCase {

    public void test() throws Exception {
        File dir = Files.createTempDir();

        File mapping = createFile(dir, "app/build/proguard/release/mapping.txt", ""
                + "com.example.shrinkunittest.app.MainActivity -> com.example.shrinkunittest.app.MainActivity:\n"
                + "    void onCreate(android.os.Bundle) -> onCreate\n"
                + "    boolean onCreateOptionsMenu(android.view.Menu) -> onCreateOptionsMenu\n"
                + "    boolean onOptionsItemSelected(android.view.MenuItem) -> onOptionsItemSelected");


        byte[] classesJarByteCode = new byte[] {
                (byte)80, (byte)75, (byte)3, (byte)4, (byte)20, (byte)0, (byte)8, (byte)0,
                (byte)8, (byte)0, (byte)12, (byte)88, (byte)-62, (byte)68, (byte)0, (byte)0,
                (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
                (byte)0, (byte)0, (byte)49, (byte)0, (byte)0, (byte)0, (byte)99, (byte)111,
                (byte)109, (byte)47, (byte)101, (byte)120, (byte)97, (byte)109, (byte)112, (byte)108,
                (byte)101, (byte)47, (byte)115, (byte)104, (byte)114, (byte)105, (byte)110, (byte)107,
                (byte)117, (byte)110, (byte)105, (byte)116, (byte)116, (byte)101, (byte)115, (byte)116,
                (byte)47, (byte)97, (byte)112, (byte)112, (byte)47, (byte)77, (byte)97, (byte)105,
                (byte)110, (byte)65, (byte)99, (byte)116, (byte)105, (byte)118, (byte)105, (byte)116,
                (byte)121, (byte)46, (byte)99, (byte)108, (byte)97, (byte)115, (byte)115, (byte)117,
                (byte)-110, (byte)-53, (byte)78, (byte)-37, (byte)64, (byte)20, (byte)-122, (byte)-1,
                (byte)-63, (byte)14, (byte)14, (byte)97, (byte)32, (byte)36, (byte)36, (byte)-31,
                (byte)82, (byte)110, (byte)-31, (byte)-46, (byte)58, (byte)-15, (byte)-62, (byte)82,
                (byte)-73, (byte)-127, (byte)74, (byte)-112, (byte)-107, (byte)-91, (byte)-94, (byte)46,
                (byte)-112, (byte)88, (byte)-80, (byte)-77, (byte)-30, (byte)1, (byte)70, (byte)36,
                (byte)-29, (byte)40, (byte)-98, (byte)-92, (byte)101, (byte)-59, (byte)-85, (byte)-16,
                (byte)0, (byte)108, (byte)-70, (byte)73, (byte)-44, (byte)46, (byte)-6, (byte)0,
                (byte)60, (byte)20, (byte)-22, (byte)-103, (byte)-60, (byte)80, (byte)110, (byte)-99,
                (byte)-111, (byte)-50, (byte)57, (byte)-93, (byte)-1, (byte)59, (byte)23, (byte)-23,
                (byte)-52, (byte)-3, (byte)-61, (byte)-17, (byte)63, (byte)0, (byte)62, (byte)-61,
                (byte)-77, (byte)110, (byte)44, (byte)-64, (byte)-70, (byte)113, (byte)-116, (byte)-55,
                (byte)2, (byte)14, (byte)-74, (byte)28, (byte)84, (byte)29, (byte)108, (byte)59,
                (byte)-40, (byte)-55, (byte)-63, (byte)70, (byte)-34, (byte)-104, (byte)69, (byte)99,
                (byte)74, (byte)57, (byte)100, (byte)80, (byte)-52, (byte)17, (byte)81, (byte)48,
                (byte)-90, (byte)60, (byte)-117, (byte)105, (byte)44, (byte)112, (byte)108, (byte)96,
                (byte)-103, (byte)99, (byte)23, (byte)21, (byte)-114, (byte)61, (byte)44, (byte)113,
                (byte)124, (byte)-60, (byte)42, (byte)-57, (byte)39, (byte)124, (byte)-32, (byte)-88,
                (byte)97, (byte)-99, (byte)-93, (byte)-114, (byte)21, (byte)6, (byte)-53, (byte)-83,
                (byte)5, (byte)12, (byte)-21, (byte)110, (byte)-19, (byte)107, (byte)-88, (byte)-94,
                (byte)94, (byte)44, (byte)35, (byte)127, (byte)32, (byte)-59, (byte)119, (byte)-1,
                (byte)88, (byte)-88, (byte)126, (byte)-96, (byte)-50, (byte)-37, (byte)-95, (byte)22,
                (byte)-67, (byte)-58, (byte)-104, (byte)58, (byte)101, (byte)-80, (byte)-35, (byte)-64,
                (byte)-72, (byte)37, (byte)55, (byte)120, (byte)11, (byte)55, (byte)-116, (byte)82,
                (byte)113, (byte)-97, (byte)-124, (byte)56, (byte)-15, (byte)-113, (byte)-6, (byte)42,
                (byte)106, (byte)-117, (byte)-41, (byte)-62, (byte)-77, (byte)-116, (byte)51, (byte)-122,
                (byte)-43, (byte)119, (byte)-124, (byte)64, (byte)-117, (byte)-50, (byte)88, (byte)-100,
                (byte)-34, (byte)-105, (byte)74, (byte)-22, (byte)47, (byte)-44, (byte)-72, (byte)25,
                (byte)71, (byte)-126, (byte)-95, (byte)-12, (byte)-120, (byte)-122, (byte)-35, (byte)-82,
                (byte)127, (byte)-40, (byte)-46, (byte)114, (byte)32, (byte)-11, (byte)53, (byte)-61,
                (byte)-54, (byte)127, (byte)39, (byte)103, (byte)40, (byte)-65, (byte)91, (byte)-99,
                (byte)-63, (byte)107, (byte)-59, (byte)29, (byte)95, (byte)-4, (byte)8, (byte)59,
                (byte)-35, (byte)-74, (byte)-16, (byte)-109, (byte)-53, (byte)-98, (byte)84, (byte)87,
                (byte)125, (byte)-22, (byte)-91, (byte)69, (byte)-94, (byte)-57, (byte)-43, (byte)-113,
                (byte)67, (byte)-87, (byte)-2, (byte)117, (byte)-104, (byte)-71, (byte)16, (byte)-38,
                (byte)-28, (byte)5, (byte)17, (byte)67, (byte)-98, (byte)-30, (byte)-105, (byte)61,
                (byte)28, (byte)57, (byte)9, (byte)25, (byte)-78, (byte)-79, (byte)106, (byte)-10,
                (byte)-60, (byte)56, (byte)92, (byte)124, (byte)12, (byte)-65, (byte)117, (byte)-75,
                (byte)-116, (byte)85, (byte)98, (byte)82, (byte)104, (byte)-100, (byte)88, (byte)-91,
                (byte)111, (byte)83, (byte)-18, (byte)68, (byte)-76, (byte)69, (byte)75, (byte)11,
                (byte)42, (byte)58, (byte)-97, (byte)8, (byte)-35, (byte)-116, (byte)-107, (byte)22,
                (byte)74, (byte)-97, (byte)-46, (byte)-96, (byte)-88, (byte)-46, (byte)18, (byte)109,
                (byte)-104, (byte)99, (byte)-125, (byte)-103, (byte)53, (byte)-110, (byte)-35, (byte)-92,
                (byte)87, (byte)-127, (byte)60, (byte)35, (byte)-97, (byte)-87, (byte)-113, (byte)-112,
                (byte)-3, (byte)-103, (byte)2, (byte)-76, (byte)-47, (byte)84, (byte)94, (byte)-58,
                (byte)20, (byte)93, (byte)-128, (byte)-41, (byte)-67, (byte)17, (byte)102, (byte)-22,
                (byte)69, (byte)54, (byte)-60, (byte)-36, (byte)-124, (byte)98, (byte)112, (byte)-79,
                (byte)-10, (byte)68, (byte)89, (byte)41, (byte)53, (byte)4, (byte)47, (byte)78,
                (byte)121, (byte)67, (byte)-52, (byte)-38, (byte)119, (byte)41, (byte)69, (byte)31,
                (byte)35, (byte)-91, (byte)-86, (byte)-60, (byte)-48, (byte)-17, (byte)67, (byte)-39,
                (byte)-5, (byte)-123, (byte)121, (byte)-122, (byte)-125, (byte)-75, (byte)-94, (byte)117,
                (byte)-117, (byte)-116, (byte)125, (byte)103, (byte)74, (byte)-25, (byte)38, (byte)56,
                (byte)-2, (byte)2, (byte)80, (byte)75, (byte)7, (byte)8, (byte)39, (byte)-48,
                (byte)-69, (byte)-98, (byte)-101, (byte)1, (byte)0, (byte)0, (byte)-86, (byte)2,
                (byte)0, (byte)0, (byte)80, (byte)75, (byte)1, (byte)2, (byte)20, (byte)0,
                (byte)20, (byte)0, (byte)8, (byte)0, (byte)8, (byte)0, (byte)12, (byte)88,
                (byte)-62, (byte)68, (byte)39, (byte)-48, (byte)-69, (byte)-98, (byte)-101, (byte)1,
                (byte)0, (byte)0, (byte)-86, (byte)2, (byte)0, (byte)0, (byte)49, (byte)0,
                (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
                (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
                (byte)99, (byte)111, (byte)109, (byte)47, (byte)101, (byte)120, (byte)97, (byte)109,
                (byte)112, (byte)108, (byte)101, (byte)47, (byte)115, (byte)104, (byte)114, (byte)105,
                (byte)110, (byte)107, (byte)117, (byte)110, (byte)105, (byte)116, (byte)116, (byte)101,
                (byte)115, (byte)116, (byte)47, (byte)97, (byte)112, (byte)112, (byte)47, (byte)77,
                (byte)97, (byte)105, (byte)110, (byte)65, (byte)99, (byte)116, (byte)105, (byte)118,
                (byte)105, (byte)116, (byte)121, (byte)46, (byte)99, (byte)108, (byte)97, (byte)115,
                (byte)115, (byte)80, (byte)75, (byte)5, (byte)6, (byte)0, (byte)0, (byte)0,
                (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)95, (byte)0, (byte)0,
                (byte)0, (byte)-6, (byte)1, (byte)0, (byte)0, (byte)0, (byte)0
        };
        File classesJar = createFile(dir,
                "app/build/classes-proguard/release/classes.jar", classesJarByteCode);

        File rDir = new File(dir, "app/build/source/r/release".replace('/', separatorChar));
        //noinspection ResultOfMethodCallIgnored
        rDir.mkdirs();

        File rJava = createFile(rDir, "com/example/shrinkunittest/app/R.java", ""
                + "/* AUTO-GENERATED FILE.  DO NOT MODIFY.\n"
                + " *\n"
                + " * This class was automatically generated by the\n"
                + " * aapt tool from the resource data it found.  It\n"
                + " * should not be modified by hand.\n"
                + " */\n"
                + "\n"
                + "package com.example.shrinkunittest.app;\n"
                + "\n"
                + "public final class R {\n"
                + "    public static final class attr {\n"
                + "        /** <p>Must be an integer value, such as \"<code>100</code>\".\n"
                + "<p>This may also be a reference to a resource (in the form\n"
                + "\"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>\") or\n"
                + "theme attribute (in the form\n"
                + "\"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>\")\n"
                + "containing a value of this type.\n"
                + "         */\n"
                + "        public static final int myAttr1=0x7f010000;\n"
                + "        /** <p>Must be a boolean value, either \"<code>true</code>\" or \"<code>false</code>\".\n"
                + "<p>This may also be a reference to a resource (in the form\n"
                + "\"<code>@[<i>package</i>:]<i>type</i>:<i>name</i></code>\") or\n"
                + "theme attribute (in the form\n"
                + "\"<code>?[<i>package</i>:][<i>type</i>:]<i>name</i></code>\")\n"
                + "containing a value of this type.\n"
                + "         */\n"
                + "        public static final int myAttr2=0x7f010001;\n"
                + "    }\n"
                + "    public static final class dimen {\n"
                + "        public static final int activity_horizontal_margin=0x7f040000;\n"
                + "        public static final int activity_vertical_margin=0x7f040001;\n"
                + "    }\n"
                + "    public static final class drawable {\n"
                + "        public static final int ic_launcher=0x7f020000;\n"
                + "        public static final int unused=0x7f020001;\n"
                + "    }\n"
                + "    public static final class id {\n"
                + "        public static final int action_settings=0x7f080000;\n"
                + "    }\n"
                + "    public static final class layout {\n"
                + "        public static final int activity_main=0x7f030000;\n"
                + "    }\n"
                + "    public static final class menu {\n"
                + "        public static final int main=0x7f070000;\n"
                + "    }\n"
                + "    public static final class string {\n"
                + "        public static final int action_settings=0x7f050000;\n"
                + "        public static final int alias=0x7f050001;\n"
                + "        public static final int app_name=0x7f050002;\n"
                + "        public static final int hello_world=0x7f050003;\n"
                + "    }\n"
                + "    public static final class style {\n"
                + "        public static final int AppTheme=0x7f060000;\n"
                + "        public static final int MyStyle=0x7f060001;\n"
                + "        public static final int MyStyle_Child=0x7f060002;\n"
                + "    }\n"
                + "}");

        File mergedManifest = createFile(dir, "app/build/manifests/release/AndroidManifest.xml", ""
                + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" android:versionCode=\"1\" android:versionName=\"1.0\" package=\"com.example.shrinkunittest.app\">\n"
                + "    <uses-sdk android:minSdkVersion=\"20\" android:targetSdkVersion=\"19\"/>\n"
                + "\n"
                + "    <application android:allowBackup=\"true\" android:icon=\"@drawable/ic_launcher\" android:label=\"@string/app_name\">\n"
                + "        <activity android:label=\"@string/app_name\" android:name=\"com.example.shrinkunittest.app.MainActivity\">\n"
                + "            <intent-filter>\n"
                + "                <action android:name=\"android.intent.action.MAIN\"/>\n"
                + "\n"
                + "                <category android:name=\"android.intent.category.LAUNCHER\"/>\n"
                + "            </intent-filter>\n"
                + "        </activity>\n"
                + "    </application>\n"
                + "\n"
                + "</manifest>");

        File resources = new File(dir, "app/build/res/all/release/values".replace('/', separatorChar));
        //noinspection ResultOfMethodCallIgnored
        resources.mkdirs();

        createFile(resources, "drawable-hdpi/ic_launcher.png", new byte[0]);
        createFile(resources, "drawable-mdpi/ic_launcher.png", new byte[0]);
        createFile(resources, "drawable-xxhdpi/ic_launcher.png", new byte[0]);
        File unusedBitmap = createFile(resources, "drawable-xxhdpi/unused.png", new byte[0]);

        createFile(resources, "layout/activity_main.xml", ""
                + "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                + "    android:layout_width=\"match_parent\"\n"
                + "    android:layout_height=\"match_parent\"\n"
                + "    android:paddingLeft=\"@dimen/activity_horizontal_margin\"\n"
                + "    android:paddingRight=\"@dimen/activity_horizontal_margin\"\n"
                + "    android:paddingTop=\"@dimen/activity_vertical_margin\"\n"
                + "    android:paddingBottom=\"@dimen/activity_vertical_margin\"\n"
                + "    tools:context=\".MainActivity\">\n"
                + "\n"
                + "    <TextView\n"
                + "        style=\"@style/MyStyle.Child\"\n"
                + "        android:text=\"@string/hello_world\"\n"
                + "        android:layout_width=\"wrap_content\"\n"
                + "        android:layout_height=\"wrap_content\" />\n"
                + "\n"
                + "</RelativeLayout>");

        createFile(resources, "menu/main.xml", ""
                + "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/tools\"\n"
                + "    tools:context=\".MainActivity\" >\n"
                + "    <item android:id=\"@+id/action_settings\"\n"
                + "        android:title=\"@string/action_settings\"\n"
                + "        android:orderInCategory=\"100\"\n"
                + "        android:showAsAction=\"never\" />\n"
                + "</menu>");

        File values = createFile(resources, "values/values.xml", ""
                + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<resources>\n"
                + "\n"
                + "    <attr name=\"myAttr1\" format=\"integer\" />\n"
                + "    <attr name=\"myAttr2\" format=\"boolean\" />\n"
                + "\n"
                + "    <dimen name=\"activity_horizontal_margin\">16dp</dimen>\n"
                + "    <dimen name=\"activity_vertical_margin\">16dp</dimen>\n"
                + "\n"
                + "    <string name=\"action_settings\">Settings</string>\n"
                + "    <string name=\"alias\"> @string/app_name </string>\n"
                + "    <string name=\"app_name\">ShrinkUnitTest</string>\n"
                + "    <string name=\"hello_world\">Hello world!</string>\n"
                + "\n"
                + "    <style name=\"AppTheme\" parent=\"android:Theme.Holo\"></style>\n"
                + "\n"
                + "    <style name=\"MyStyle\">\n"
                + "        <item name=\"myAttr1\">50</item>\n"
                + "    </style>\n"
                + "\n"
                + "    <style name=\"MyStyle.Child\">\n"
                + "        <item name=\"myAttr2\">true</item>\n"
                + "    </style>\n"
                + "\n"
                + "</resources>");

        ResourceUsageAnalyzer analyzer = new ResourceUsageAnalyzer(rDir, classesJar,
            mergedManifest, mapping, resources);
        assertEquals(""
                + "@attr/myAttr1 : reachable=false\n"
                + "@attr/myAttr2 : reachable=false\n"
                + "@dimen/activity_horizontal_margin : reachable=true\n"
                + "@dimen/activity_vertical_margin : reachable=true\n"
                + "@drawable/ic_launcher : reachable=true\n"
                + "@drawable/unused : reachable=false\n"
                + "@layout/activity_main : reachable=true\n"
                + "    @layout/activity_main\n"
                + "    @layout/activity_main\n"
                + "    @layout/activity_main\n"
                + "@menu/main : reachable=true\n"
                + "    @menu/main\n"
                + "    @menu/main\n"
                + "@string/action_settings : reachable=true\n"
                + "@string/alias : reachable=false\n"
                + "    @string/alias\n"
                + "@string/app_name : reachable=true\n"
                + "@string/hello_world : reachable=true\n"
                + "@style/AppTheme : reachable=false\n"
                + "@style/MyStyle : reachable=false\n"
                + "@style/MyStyle_Child : reachable=false\n"
                + "    @style/MyStyle_Child\n",
                analyzer.dumpResourceModel());

        assertTrue(unusedBitmap.exists());

        analyzer.removeUnused();

        assertFalse(unusedBitmap.exists());
        assertEquals(""
            + "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "\n"
            + "    <attr name=\"myAttr1\" format=\"integer\" />\n"
            + "    <attr name=\"myAttr2\" format=\"boolean\" />\n"
            + "\n"
            + "    <dimen name=\"activity_horizontal_margin\">16dp</dimen>\n"
            + "    <dimen name=\"activity_vertical_margin\">16dp</dimen>\n"
            + "\n"
            + "    <string name=\"action_settings\">Settings</string>\n"
            + "    <string name=\"app_name\">ShrinkUnitTest</string>\n"
            + "    <string name=\"hello_world\">Hello world!</string>\n"
            + "\n"
            + "</resources>",

            Files.toString(values, Charsets.UTF_8));

        deleteDir(dir);
    }

    /** Utility method to generate byte array literal dump (used by classesJarBytecode above) */
    public static void dumpBytes(File file) throws IOException {
        byte[] bytes = Files.toByteArray(file);
        int count = 0;
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            System.out.print("(byte)" + Byte.toString(b) + ", ");
            count++;
            if (count == 8) {
                count = 0;
                System.out.println();
            }
        }

        System.out.println();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected static void deleteDir(File root) {
        if (root.exists()) {
            File[] files = root.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDir(file);
                    } else {
                        file.delete();
                    }
                }
            }
            root.delete();
        }
    }

    @NonNull
    private static File createFile(File dir, String relative) throws IOException {
        File file = new File(dir, relative.replace('/', separatorChar));
        file.getParentFile().mkdirs();
        return file;
    }


    @NonNull
    private static File createFile(File dir, String relative, String contents) throws IOException {
        File file = createFile(dir, relative);
        Files.write(contents, file, Charsets.UTF_8);
        return file;
    }


    @NonNull
    private static File createFile(File dir, String relative, byte[] contents) throws IOException {
        File file = createFile(dir, relative);
        Files.write(contents, file);
        return file;
    }
}