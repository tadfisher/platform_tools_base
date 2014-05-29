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

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Tests for {@link com.android.manifmerger.ManifestTask} class.
 */
public class ManifestTaskTest extends TestCase {

    @Mock
    ActionRecorder mActionRecorder;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }

    public void testPackageOverride()
            throws ParserConfigurationException, SAXException, IOException {
        String xml = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\""
                + "    package=\"com.foo.old\" >\n"
                + "    <activity android:name=\"activityOne\"/>\n"
                + "</manifest>";

        XmlDocument refDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(), "testPackageOverride#xml"), xml);

        ManifestTask.SystemProperty.PACKAGE.addTo(mActionRecorder, refDocument, "com.bar.new");
        // verify the package value was overriden.
        assertEquals("com.bar.new", refDocument.getRootNode().getXml().getAttribute("package"));
    }

    public void testMissingPackageOverride()
            throws ParserConfigurationException, SAXException, IOException {
        String xml = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <activity android:name=\"activityOne\"/>\n"
                + "</manifest>";

        XmlDocument refDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(), "testMissingPackageOverride#xml"), xml);

        ManifestTask.SystemProperty.PACKAGE.addTo(mActionRecorder, refDocument, "com.bar.new");
        // verify the package value was added.
        assertEquals("com.bar.new", refDocument.getRootNode().getXml().getAttribute("package"));
    }

    public void testAddingSystemProperties()
            throws ParserConfigurationException, SAXException, IOException {
        String xml = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <activity android:name=\"activityOne\"/>\n"
                + "</manifest>";

        XmlDocument document = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(),
                        "testAddingSystemProperties#xml"), xml);

        ManifestTask.SystemProperty.VERSION_CODE.addTo(mActionRecorder, document, "101");
        assertEquals("101",
                document.getXml().getDocumentElement().getAttribute("android:versionCode"));

        ManifestTask.SystemProperty.VERSION_NAME.addTo(mActionRecorder, document, "1.0.1");
        assertEquals("1.0.1",
                document.getXml().getDocumentElement().getAttribute("android:versionName"));

        ManifestTask.SystemProperty.MIN_SDK_VERSION.addTo(mActionRecorder, document, "10");
        Element usesSdk = (Element) document.getXml().getElementsByTagName("uses-sdk").item(0);
        assertNotNull(usesSdk);
        assertEquals("10", usesSdk.getAttribute("android:minSdkVersion"));

        ManifestTask.SystemProperty.TARGET_SDK_VERSION.addTo(mActionRecorder, document, "14");
        usesSdk = (Element) document.getXml().getElementsByTagName("uses-sdk").item(0);
        assertNotNull(usesSdk);
        assertEquals("14", usesSdk.getAttribute("android:targetSdkVersion"));
    }

    public void testAddingSystemProperties_withDifferentPrefix()
            throws ParserConfigurationException, SAXException, IOException {
        String xml = ""
                + "<manifest\n"
                + "    xmlns:t=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <activity t:name=\"activityOne\"/>\n"
                + "</manifest>";

        XmlDocument document = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(),
                        "testAddingSystemProperties#xml"), xml
        );

        ManifestTask.SystemProperty.VERSION_CODE.addTo(mActionRecorder, document, "101");
        // using the non namespace aware API to make sure the prefix is the expected one.
        assertEquals("101",
                document.getXml().getDocumentElement().getAttribute("t:versionCode"));
    }

    public void testOverridingSystemProperties()
            throws ParserConfigurationException, SAXException, IOException {
        String xml = ""
                + "<manifest versionCode=\"34\" versionName=\"3.4\"\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <uses-sdk minSdkVersion=\"9\" targetSdkVersion=\".9\"/>\n"
                + "    <activity android:name=\"activityOne\"/>\n"
                + "</manifest>";

        XmlDocument document = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(),
                        "testAddingSystemProperties#xml"), xml);
        // check initial state.
        assertEquals("34", document.getXml().getDocumentElement().getAttribute("versionCode"));
        assertEquals("3.4", document.getXml().getDocumentElement().getAttribute("versionName"));
        Element usesSdk = (Element) document.getXml().getElementsByTagName("uses-sdk").item(0);
        assertNotNull(usesSdk);
        assertEquals("9", usesSdk.getAttribute("minSdkVersion"));
        assertEquals(".9", usesSdk.getAttribute("targetSdkVersion"));


        ManifestTask.SystemProperty.VERSION_CODE.addTo(mActionRecorder, document, "101");
        assertEquals("101",
                document.getXml().getDocumentElement().getAttribute("android:versionCode"));

        ManifestTask.SystemProperty.VERSION_NAME.addTo(mActionRecorder, document, "1.0.1");
        assertEquals("1.0.1",
                document.getXml().getDocumentElement().getAttribute("android:versionName"));

        ManifestTask.SystemProperty.MIN_SDK_VERSION.addTo(mActionRecorder, document, "10");
        usesSdk = (Element) document.getXml().getElementsByTagName("uses-sdk").item(0);
        assertNotNull(usesSdk);
        assertEquals("10", usesSdk.getAttribute("android:minSdkVersion"));

        ManifestTask.SystemProperty.TARGET_SDK_VERSION.addTo(mActionRecorder, document, "14");
        usesSdk = (Element) document.getXml().getElementsByTagName("uses-sdk").item(0);
        assertNotNull(usesSdk);
        assertEquals("14", usesSdk.getAttribute("android:targetSdkVersion"));
    }
}
