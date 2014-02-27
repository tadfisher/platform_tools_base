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

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Tests for the {@link ActionRecorder} class
 */
public class ActionRecorderTest extends TestCase {

    private static final String reference = ""
            + "<manifest\n"
            + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
            + "    package=\"com.example.lib3\">\n"
            + "\n"
            + "    <activity android:name=\"activityOne\">\n"
            + "       <intent-filter android:label=\"@string/foo\"/>\n"
            + "    </activity>\n"
            + "\n"
            + "</manifest>";


    // this will be used as the source location for the "reference" xml string.
    private static final String REFEFENCE_DOCUMENT = "ref_doc";

    @Mock
    Logger mLoggerMock;

    ActionRecorder mActionRecorder = new ActionRecorder();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }

    public void testDoNothing() {
        mActionRecorder.log(mLoggerMock);
        Mockito.verify(mLoggerMock).log(Level.INFO, "-- Merging decision tree log ---\n");
        Mockito.verifyNoMoreInteractions(mLoggerMock);
        assertTrue(mActionRecorder.getAllRecords().isEmpty());
    }

    public void testSingleElement_withoutAttributes()
            throws ParserConfigurationException, SAXException, IOException {

        XmlDocument xmlDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(),
                        REFEFENCE_DOCUMENT), reference);

        XmlElement xmlElement = xmlDocument.getRootNode().getNodeByTypeAndKey(
                ManifestModel.NodeTypes.ACTIVITY, "com.example.lib3.activityOne").get();
        // added during the initial file loading
        mActionRecorder.recordNodeAction(xmlElement, ActionRecorder.ActionType.Added);

        ImmutableMap<String,ActionRecorder.DecisionTreeRecord> allRecords =
                mActionRecorder.getAllRecords();
        assertEquals(1, allRecords.size());
        assertEquals(1, allRecords.get(xmlElement.getId()).mNodeRecords.size());
        assertEquals(0, allRecords.get(xmlElement.getId()).mAttributeRecords.size());
        mActionRecorder.log(mLoggerMock);

        // check that output is consistent with spec.
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-- Merging decision tree log ---\n")
            .append(xmlElement.getId()).append("\n");
        appendNode(stringBuilder, ActionRecorder.ActionType.Added, REFEFENCE_DOCUMENT, 6);

        Mockito.verify(mLoggerMock).log(Level.INFO, stringBuilder.toString());
        Mockito.verifyNoMoreInteractions(mLoggerMock);
    }

    public void testSingleElement_withoutAttributes_withRejection()
            throws ParserConfigurationException, SAXException, IOException {

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

        XmlDocument xmlDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(),
                        REFEFENCE_DOCUMENT), reference);

        XmlDocument otherDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(),
                        "other_document"), other);

        XmlElement xmlElement = xmlDocument.getRootNode().getNodeByTypeAndKey(
                ManifestModel.NodeTypes.ACTIVITY, "com.example.lib3.activityOne").get();
        // added during initial document loading
        mActionRecorder.recordNodeAction(xmlElement, ActionRecorder.ActionType.Added);
        // rejected during second document merging.
        mActionRecorder.recordNodeAction(xmlElement, ActionRecorder.ActionType.Rejected,
                otherDocument.getRootNode().getNodeByTypeAndKey(
                        ManifestModel.NodeTypes.ACTIVITY, "com.example.lib3.activityOne").get());

        ImmutableMap<String,ActionRecorder.DecisionTreeRecord> allRecords =
                mActionRecorder.getAllRecords();
        assertEquals(1, allRecords.size());
        assertEquals(2, allRecords.get(xmlElement.getId()).mNodeRecords.size());
        assertEquals(ActionRecorder.ActionType.Added,
                allRecords.get(xmlElement.getId()).mNodeRecords.get(0).mActionType);
        assertEquals(ActionRecorder.ActionType.Rejected,
                allRecords.get(xmlElement.getId()).mNodeRecords.get(1).mActionType);
        assertEquals(0, allRecords.get(xmlElement.getId()).mAttributeRecords.size());
        mActionRecorder.log(mLoggerMock);

        // check that output is consistent with spec.
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-- Merging decision tree log ---\n")
                .append(xmlElement.getId()).append("\n");
        appendNode(stringBuilder, ActionRecorder.ActionType.Added, REFEFENCE_DOCUMENT, 6);
        appendNode(stringBuilder, ActionRecorder.ActionType.Rejected, "other_document", 6);

        Mockito.verify(mLoggerMock).log(Level.INFO, stringBuilder.toString());
        Mockito.verifyNoMoreInteractions(mLoggerMock);
    }

    public void testSingleElement_withNoNamespaceAttributes()
            throws ParserConfigurationException, SAXException, IOException {

        XmlDocument xmlDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(),
                        REFEFENCE_DOCUMENT), reference);

        XmlElement xmlElement = xmlDocument.getRootNode().getNodeByTypeAndKey(
                ManifestModel.NodeTypes.ACTIVITY, "com.example.lib3.activityOne").get();
        // added during the initial file loading
        mActionRecorder.recordNodeAction(xmlElement, ActionRecorder.ActionType.Added);
        mActionRecorder.recordAttributeAction(xmlElement,
                XmlNode.unwrapName(xmlElement.getXml().getAttributeNode("android:name")),
                ActionRecorder.ActionType.Added, AttributeOperationType.STRICT);

        ImmutableMap<String,ActionRecorder.DecisionTreeRecord> allRecords =
                mActionRecorder.getAllRecords();
        assertEquals(1, allRecords.size());
        assertEquals(1, allRecords.get(xmlElement.getId()).mNodeRecords.size());
        assertEquals(1, allRecords.get(xmlElement.getId()).mAttributeRecords.size());
        mActionRecorder.log(mLoggerMock);

        // check that output is consistent with spec.
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-- Merging decision tree log ---\n")
                .append(xmlElement.getId()).append("\n");
        appendNode(stringBuilder, ActionRecorder.ActionType.Added, REFEFENCE_DOCUMENT, 6);
        appendAttribute(stringBuilder,
                XmlNode.unwrapName(xmlElement.getXml().getAttributeNode("android:name")),
                ActionRecorder.ActionType.Added,
                REFEFENCE_DOCUMENT,
                6);

        Mockito.verify(mLoggerMock).log(Level.INFO, stringBuilder.toString());
        Mockito.verifyNoMoreInteractions(mLoggerMock);
    }

    public void testSingleElement_withNamespaceAttributes()
            throws ParserConfigurationException, SAXException, IOException {

        XmlDocument xmlDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(),
                        REFEFENCE_DOCUMENT), reference);

        XmlElement xmlElement = xmlDocument.getRootNode();
        // added during the initial file loading
        mActionRecorder.recordNodeAction(xmlElement, ActionRecorder.ActionType.Added);
        mActionRecorder.recordAttributeAction(xmlElement,
                XmlNode.unwrapName(xmlElement.getXml().getAttributeNode("package")),
                ActionRecorder.ActionType.Added, AttributeOperationType.STRICT);

        ImmutableMap<String,ActionRecorder.DecisionTreeRecord> allRecords =
                mActionRecorder.getAllRecords();
        assertEquals(1, allRecords.size());
        assertEquals(1, allRecords.get(xmlElement.getId()).mNodeRecords.size());
        assertEquals(1, allRecords.get(xmlElement.getId()).mAttributeRecords.size());
        mActionRecorder.log(mLoggerMock);

        // check that output is consistent with spec.
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-- Merging decision tree log ---\n")
                .append(xmlElement.getId()).append("\n");
        appendNode(stringBuilder, ActionRecorder.ActionType.Added, REFEFENCE_DOCUMENT, 1);
        appendAttribute(stringBuilder,
                XmlNode.unwrapName(xmlElement.getXml().getAttributeNode("package")),
                ActionRecorder.ActionType.Added,
                REFEFENCE_DOCUMENT,
                1);

        Mockito.verify(mLoggerMock).log(Level.INFO, stringBuilder.toString());
        Mockito.verifyNoMoreInteractions(mLoggerMock);
    }

    public void testMultipleElements_withRejection()
            throws ParserConfigurationException, SAXException, IOException {

        String other = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
                + "    xmlns:tools=\"http://schemas.android.com/apk/res/android/tools\"\n"
                + "    package=\"com.example.lib3\">\n"
                + "\n"
                + "    <activity android:name=\"activityOne\""
                + "         android:configChanges=\"locale\"/>\n"
                + "    <application android:name=\"applicationOne\"/>"
                + "\n"
                + "</manifest>";

        XmlDocument xmlDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(),
                        REFEFENCE_DOCUMENT), reference);

        XmlDocument otherDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(),
                        "other_document"), other);

        XmlElement activityElement = xmlDocument.getRootNode().getNodeByTypeAndKey(
                ManifestModel.NodeTypes.ACTIVITY, "com.example.lib3.activityOne").get();
        // added during initial document loading
        mActionRecorder.recordNodeAction(activityElement, ActionRecorder.ActionType.Added);
        // rejected during second document merging.
        mActionRecorder.recordNodeAction(activityElement, ActionRecorder.ActionType.Rejected,
                otherDocument.getRootNode().getNodeByTypeAndKey(
                        ManifestModel.NodeTypes.ACTIVITY, "com.example.lib3.activityOne").get());
        XmlElement applicationElement = otherDocument.getRootNode().getNodeByTypeAndKey(
                ManifestModel.NodeTypes.APPLICATION, null).get();
        mActionRecorder.recordNodeAction(applicationElement, ActionRecorder.ActionType.Added);

        ImmutableMap<String,ActionRecorder.DecisionTreeRecord> allRecords =
                mActionRecorder.getAllRecords();
        assertEquals(2, allRecords.size());
        assertEquals(2, allRecords.get(activityElement.getId()).mNodeRecords.size());
        assertEquals(ActionRecorder.ActionType.Added,
                allRecords.get(activityElement.getId()).mNodeRecords.get(0).mActionType);
        assertEquals(ActionRecorder.ActionType.Rejected,
                allRecords.get(activityElement.getId()).mNodeRecords.get(1).mActionType);
        assertEquals(0, allRecords.get(activityElement.getId()).mAttributeRecords.size());
        assertEquals(1, allRecords.get(applicationElement.getId()).mNodeRecords.size());
        assertEquals(0, allRecords.get(applicationElement.getId()).mAttributeRecords.size());
        mActionRecorder.log(mLoggerMock);

        // check that output is consistent with spec.
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-- Merging decision tree log ---\n")
                .append(activityElement.getId()).append("\n");
        appendNode(stringBuilder, ActionRecorder.ActionType.Added, REFEFENCE_DOCUMENT, 6);
        appendNode(stringBuilder, ActionRecorder.ActionType.Rejected, "other_document", 6);
        stringBuilder.append(applicationElement.getId()).append("\n");
        appendNode(stringBuilder, ActionRecorder.ActionType.Added, "other_document", 7);

        Mockito.verify(mLoggerMock).log(Level.INFO, stringBuilder.toString());
        Mockito.verifyNoMoreInteractions(mLoggerMock);
    }


    private void appendNode(StringBuilder out,
            ActionRecorder.ActionType actionType,
            String docString,
            int lineNumber) {

        out.append(actionType.toString())
                .append(" from ")
                .append(getClass().getSimpleName()).append('#').append(docString)
                .append(":").append(lineNumber).append("\n");
    }

    private void appendAttribute(StringBuilder out,
            XmlNode.NodeName attributeName,
            ActionRecorder.ActionType actionType,
            String docString,
            int lineNumber) {

        out.append("\t")
                .append(attributeName.toString())
                .append("\t\t")
                .append(actionType.toString())
                .append(" from ")
                .append(getClass().getSimpleName()).append('#').append(docString)
                .append(":").append(lineNumber).append("\n");
    }
}
