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

package com.android.manifmerger;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Tests for {@link com.android.manifmerger.XmlDocument}
 */
public class XmlDocumentTest extends TestCase {

    public void testMergeableElementsIdentification()
            throws ParserConfigurationException, SAXException, IOException {
        String input = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <application android:label=\"@string/lib_name\" />\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" />\n"
                + "\n"
                + "</manifest>";

        XmlDocument xmlDocument = xmlDocumentFromString(input);
        ImmutableList<XmlNode> mergeableElements = xmlDocument.getMergeableElements();
        assertEquals(3, mergeableElements.size());
    }

    public void testGetXmlNodeByTypeAndKey()
            throws ParserConfigurationException, SAXException, IOException {
        String input = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <application android:label=\"@string/lib_name\" />\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" />\n"
                + "\n"
                + "</manifest>";

        XmlDocument xmlDocument = xmlDocumentFromString(input);
        assertTrue(xmlDocument.getNodeByTypeAndKey(
                XmlNodeTypes.ACTIVITY, "activityOne").isPresent());
        assertFalse(xmlDocument.getNodeByTypeAndKey(
                XmlNodeTypes.ACTIVITY, "noName").isPresent());
    }

    public void testSimpleMerge()
            throws ParserConfigurationException, SAXException, IOException {
        String main = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <application android:label=\"@string/lib_name\" />\n"
                + "\n"
                + "</manifest>";
        String library = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" />\n"
                + "\n"
                + "</manifest>";

        XmlDocument mainDocument = xmlDocumentFromString(main);
        XmlDocument libraryDocument = xmlDocumentFromString(library);
        Optional<XmlDocument> mergedDocument = mainDocument.merge(libraryDocument);
        assertTrue(mergedDocument.isPresent());
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        mergedDocument.get().write(byteArrayOutputStream);
        Logger.getAnonymousLogger().info(byteArrayOutputStream.toString());
        assertTrue(mergedDocument.get().getNodeByTypeAndKey(
                XmlNodeTypes.APPLICATION, null).isPresent());
        Optional<XmlNode> activityOne = mergedDocument.get()
                .getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne");
        assertTrue(activityOne.isPresent());

    }

    public void testDiff1()
            throws Exception {
        String main = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <application android:label=\"@string/lib_name\" />\n"
                + "\n"
                + "</manifest>";
        String library = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" />\n"
                + "\n"
                + "</manifest>";

        XmlDocument mainDocument = xmlDocumentFromString(main);
        XmlDocument libraryDocument = xmlDocumentFromString(library);
        assertFalse(mainDocument.getNodeByTypeAndKey(XmlNodeTypes.MANIFEST, null)
                .get()
                .deepCompare(
                        libraryDocument.getNodeByTypeAndKey(XmlNodeTypes.MANIFEST, null).get()));
    }

    public void testDiff2()
            throws Exception {
        String main = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <application android:label=\"@string/lib_name\" />\n"
                + "    <activity android:name=\"activityOne\" />\n"
                + "\n"
                + "\n"
                + "</manifest>";
        String library = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" />\n"
                + "    <application android:label=\"@string/lib_name\" />\n"
                + "\n"
                + "</manifest>";

        XmlDocument mainDocument = xmlDocumentFromString(main);
        XmlDocument libraryDocument = xmlDocumentFromString(library);
        assertTrue(mainDocument.getNodeByTypeAndKey(XmlNodeTypes.MANIFEST, null)
                .get().deepCompare(
                        libraryDocument.getNodeByTypeAndKey(XmlNodeTypes.MANIFEST, null).get()));
    }

    public void testDiff3()
            throws Exception {
        String main = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <application android:label=\"@string/lib_name\" />\n"
                + "    <!-- some comment that should be ignored -->\n"
                + "    <activity android:name=\"activityOne\" />\n"
                + "\n"
                + "\n"
                + "</manifest>";
        String library = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <!-- some comment that should also be ignored -->\n"
                + "    <activity android:name=\"activityOne\" />\n"
                + "    <application android:label=\"@string/lib_name\" />\n"
                + "\n"
                + "</manifest>";

        XmlDocument mainDocument = xmlDocumentFromString(main);
        XmlDocument libraryDocument = xmlDocumentFromString(library);
        assertTrue(mainDocument.getNodeByTypeAndKey(XmlNodeTypes.MANIFEST, null)
                .get().deepCompare(
                        libraryDocument.getNodeByTypeAndKey(XmlNodeTypes.MANIFEST, null).get()));
    }

    public void testWriting() throws ParserConfigurationException, SAXException, IOException {
        String input = ""
                + "<manifest"
                + " xmlns:x=\"http://schemas.android.com/apk/res/android\""
                + " xmlns:y=\"http://schemas.android.com/apk/res/android/tools\""
                + " package=\"com.example.lib3\">\n"
                + "\n"
                + "    <application x:label=\"@string/lib_name\" y:node=\"replace\"/>\n"
                + "\n"
                + "</manifest>\n";

        XmlDocument xmlDocument = xmlDocumentFromString(input);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        xmlDocument.write(byteArrayOutputStream);
        assertEquals(input, byteArrayOutputStream.toString());
    }

    private static XmlDocument xmlDocumentFromString(String input)
            throws IOException, SAXException, ParserConfigurationException {
        XmlLoader xmlLoader = new XmlLoader();
        return xmlLoader.load(input);
    }
}
