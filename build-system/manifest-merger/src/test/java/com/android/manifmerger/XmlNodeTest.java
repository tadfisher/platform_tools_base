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

import junit.framework.TestCase;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Tests for the {@link XmlNode}
 */
public class XmlNodeTest extends TestCase {

    public void testToolsNodeInstructions()
            throws ParserConfigurationException, SAXException, IOException {
        String input = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         tools:node=\"remove\"/>\n"
                + "\n"
                + "    <activity android:name=\"activityTwo\" "
                + "         tools:node=\"removeAll\"/>\n"
                + "\n"
                + "    <activity android:name=\"activityThree\" "
                + "         tools:node=\"removeChildren\"/>\n"
                + "\n"
                + "</manifest>";

        XmlDocument xmlDocument = xmlDocumentFromString(input);
        Optional<XmlNode> activity = xmlDocument.getNodeByTypeAndKey(
                XmlNodeTypes.ACTIVITY, "activityOne");
        assertTrue(activity.isPresent());
        assertEquals(1, activity.get().getNodeOperations().size());
        assertEquals(NodeOperationType.REMOVE,
                activity.get().getNodeOperations().get(0).getNodeOperationType());
        activity = xmlDocument.getNodeByTypeAndKey(
                XmlNodeTypes.ACTIVITY, "activityTwo");
        assertTrue(activity.isPresent());
        assertEquals(1, activity.get().getNodeOperations().size());
        assertEquals(NodeOperationType.REMOVE_ALL,
                activity.get().getNodeOperations().get(0).getNodeOperationType());
        activity = xmlDocument.getNodeByTypeAndKey(
                XmlNodeTypes.ACTIVITY, "activityThree");
        assertTrue(activity.isPresent());
        assertEquals(1, activity.get().getNodeOperations().size());
        assertEquals(NodeOperationType.REMOVE_CHILDREN,
                activity.get().getNodeOperations().get(0).getNodeOperationType());
    }

    public void testInvalidNodeInstruction()
            throws ParserConfigurationException, SAXException, IOException {

        String input = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         tools:node=\"funkyValue\"/>\n"
                + "\n"
                + "</manifest>";

        try {
            xmlDocumentFromString(input);
            fail("Exception not thrown");
        } catch (IllegalArgumentException expected) {
            // expected.
        }
    }

    public void testAttributeInstructions()
            throws ParserConfigurationException, SAXException, IOException {
        String input = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         tools:remove=\"android:theme\"/>\n"
                + "\n"
                + "    <activity android:name=\"activityTwo\" "
                + "         android:theme=\"@theme1\"\n"
                + "         tools:replace=\"android:theme\"/>\n"
                + "\n"
                + "    <activity android:name=\"activityThree\" "
                + "         tools:strict=\"android:exported\"/>\n"
                + "\n"
                + "    <activity android:name=\"activityFour\" "
                + "         android:theme=\"@theme1\"\n"
                + "         android:exported=\"true\"\n"
                + "         android:windowSoftInputMode=\"stateUnchanged\"\n"
                + "         tools:replace="
                + "\"android:theme, android:exported,android:windowSoftInputMode\"/>\n"
                + "\n"
                + "    <activity android:name=\"activityFive\" "
                + "         android:theme=\"@theme1\"\n"
                + "         android:exported=\"true\"\n"
                + "         android:windowSoftInputMode=\"stateUnchanged\"\n"
                + "         tools:remove=\"android:exported\"\n"
                + "         tools:replace=\"android:theme\"\n"
                + "         tools:strict=\"android:windowSoftInputMode\"/>\n"
                + "\n"
                + "</manifest>";

        // ActivityOne, remove operation.
        XmlDocument xmlDocument = xmlDocumentFromString(input);
        Optional<XmlNode> activityOptional = xmlDocument.getNodeByTypeAndKey(
                XmlNodeTypes.ACTIVITY, "activityOne");
        assertTrue(activityOptional.isPresent());
        XmlNode activity = activityOptional.get();
        assertEquals(1, activity.getAttributeOperations().size());
        assertEquals(AttributeOperationType.REMOVE,
                activity.getAttributeOperations().get(0).getOperationType());
        assertEquals(1, activity.getAttributeOperations().get(0).getAttributeNames().size());
        assertEquals("android:theme",
                activity.getAttributeOperations().get(0).getAttributeNames().get(0));

        // ActivityTwo, replace operation.
        activityOptional = xmlDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityTwo");
        assertTrue(activityOptional.isPresent());
        activity = activityOptional.get();
        assertEquals(1, activity.getAttributeOperations().size());
        assertEquals(AttributeOperationType.REPLACE,
                activity.getAttributeOperations().get(0).getOperationType());
        assertEquals(1, activity.getAttributeOperations().get(0).getAttributeNames().size());
        assertEquals("android:theme",
                activity.getAttributeOperations().get(0).getAttributeNames().get(0));

        // ActivityThree, strict operation.
        activityOptional = xmlDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityThree");
        assertTrue(activityOptional.isPresent());
        activity = activityOptional.get();
        assertEquals(1, activity.getAttributeOperations().size());
        assertEquals(AttributeOperationType.STRICT,
                activity.getAttributeOperations().get(0).getOperationType());
        assertEquals(1, activity.getAttributeOperations().get(0).getAttributeNames().size());
        assertEquals("android:exported",
                activity.getAttributeOperations().get(0).getAttributeNames().get(0));

        // ActivityFour, multiple target fields.
        activityOptional = xmlDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityFour");
        assertTrue(activityOptional.isPresent());
        activity = activityOptional.get();
        assertEquals(1, activity.getAttributeOperations().size());
        assertEquals(AttributeOperationType.REPLACE,
                activity.getAttributeOperations().get(0).getOperationType());
        assertEquals(3, activity.getAttributeOperations().get(0).getAttributeNames().size());
        assertEquals("android:theme",
                activity.getAttributeOperations().get(0).getAttributeNames().get(0));
        assertEquals("android:exported",
                activity.getAttributeOperations().get(0).getAttributeNames().get(1));
        assertEquals("android:windowSoftInputMode",
                activity.getAttributeOperations().get(0).getAttributeNames().get(2));

        // ActivityFive, multiple operations.
        activityOptional = xmlDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityFive");
        assertTrue(activityOptional.isPresent());
        activity = activityOptional.get();
        assertEquals(3, activity.getAttributeOperations().size());

        assertEquals(AttributeOperationType.REMOVE,
                activity.getAttributeOperations().get(0).getOperationType());
        assertEquals(1, activity.getAttributeOperations().get(0).getAttributeNames().size());
        assertEquals("android:exported",
                activity.getAttributeOperations().get(0).getAttributeNames().get(0));

        assertEquals(AttributeOperationType.REPLACE,
                activity.getAttributeOperations().get(1).getOperationType());
        assertEquals(1, activity.getAttributeOperations().get(0).getAttributeNames().size());
        assertEquals("android:theme",
                activity.getAttributeOperations().get(1).getAttributeNames().get(0));

        assertEquals(AttributeOperationType.STRICT,
                activity.getAttributeOperations().get(2).getOperationType());
        assertEquals(1, activity.getAttributeOperations().get(0).getAttributeNames().size());
        assertEquals("android:windowSoftInputMode",
                activity.getAttributeOperations().get(2).getAttributeNames().get(0));
    }

    public void testInvalidAttributeInstruction()
            throws ParserConfigurationException, SAXException, IOException {

        String input = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         tools:bad-name=\"android:theme\"/>\n"
                + "\n"
                + "</manifest>";

        try {
            xmlDocumentFromString(input);
            fail("Exception not thrown");
        } catch (IllegalArgumentException expected) {
            // expected.
        }
    }

    public void testDiff1()
            throws Exception {

        String reference = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\"/>\n"
                + "\n"
                + "</manifest>";

        String other = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\"/>\n"
                + "\n"
                + "</manifest>";

        XmlDocument refDocument = xmlDocumentFromString(reference);
        XmlDocument otherDocument = xmlDocumentFromString(other);
        assertTrue(refDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne").get()
                .deepCompare(
                        otherDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne")
                                .get()));
    }

    public void testDiff2()
            throws Exception {

        String reference = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\"/>\n"
                + "\n"
                + "</manifest>";

        String other = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"mcc\"/>\n"
                + "\n"
                + "</manifest>";

        XmlDocument refDocument = xmlDocumentFromString(reference);
        XmlDocument otherDocument = xmlDocumentFromString(other);
        try {
            assertFalse(refDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne").get()
                    .deepCompare(
                            otherDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne")
                                    .get()));
            fail("Not thrown.");
        } catch (Exception e) {
            // expected
            Logger.getAnonymousLogger().info(e.getMessage());
        }
    }

    public void testDiff3()
            throws Exception {

        String reference = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\"/>\n"
                + "\n"
                + "</manifest>";

        String other = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\" android:exported=\"true\"/>\n"
                + "\n"
                + "</manifest>";

        XmlDocument refDocument = xmlDocumentFromString(reference);
        XmlDocument otherDocument = xmlDocumentFromString(other);
        try {
            assertFalse(refDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne").get()
                    .deepCompare(
                            otherDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne")
                                    .get()));
            fail("Not thrown.");
        } catch (Exception e) {
            // expected
            Logger.getAnonymousLogger().info(e.getMessage());
        }
    }

    public void testDiff4()
            throws Exception {

        String reference = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\" android:exported=\"false\"/>\n"
                + "\n"
                + "</manifest>";

        String other = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\"/>\n"
                + "\n"
                + "</manifest>";

        XmlDocument refDocument = xmlDocumentFromString(reference);
        XmlDocument otherDocument = xmlDocumentFromString(other);
        try {
            assertFalse(refDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne").get()
                    .deepCompare(
                            otherDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne")
                                    .get()));
            fail("Not thrown.");
        } catch (Exception e) {
            // expected
            Logger.getAnonymousLogger().info(e.getMessage());
        }
    }

    public void testDiff5()
            throws Exception {

        String reference = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\">\n"
                + "\n"
                + "    </activity>\n"
                + "</manifest>";

        String other = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\"/>\n"
                + "\n"
                + "</manifest>";

        XmlDocument refDocument = xmlDocumentFromString(reference);
        XmlDocument otherDocument = xmlDocumentFromString(other);
        assertTrue(refDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne").get()
                .deepCompare(
                        otherDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne")
                                .get()));
    }

    public void testDiff6()
            throws Exception {

        String reference = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\">\n"
                + "\n"
                + "       <intent-filter android:label=\"@string/foo\"/>\n"
                + "\n"
                + "    </activity>\n"
                + "</manifest>";

        String other = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\" "
                + "         android:configChanges=\"locale\"/>\n"
                + "\n"
                + "</manifest>";

        XmlDocument refDocument = xmlDocumentFromString(reference);
        XmlDocument otherDocument = xmlDocumentFromString(other);
        try {
        assertFalse(refDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne").get()
                .deepCompare(
                        otherDocument.getNodeByTypeAndKey(XmlNodeTypes.ACTIVITY, "activityOne")
                                .get()));
        } catch (Exception e) {
            // expected for now...
        }
    }


    private static XmlDocument xmlDocumentFromString(String input)
            throws IOException, SAXException, ParserConfigurationException {
        XmlLoader xmlLoader = new XmlLoader();
        return xmlLoader.load(input);
    }
}
