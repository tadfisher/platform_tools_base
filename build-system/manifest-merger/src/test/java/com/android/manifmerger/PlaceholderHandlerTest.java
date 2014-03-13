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

import static com.android.manifmerger.ManifestMerger2.Invoker.SystemProperty;
import static com.android.manifmerger.PlaceholderHandler.KeyBasedValueResolver;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.mock.MockLog;
import com.google.common.base.Optional;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Tests for the {@link com.android.manifmerger.PlaceholderHandler}
 */
public class PlaceholderHandlerTest  extends TestCase {

    @Mock
    ActionRecorder.Builder mActionRecorder;

    @Mock
    MergingReport.Builder mBuilder;

    MockLog mMockLog = new MockLog();

    KeyBasedValueResolver<String> nullResolver = new KeyBasedValueResolver<String>() {
        @Override
        public String getValue(@NonNull String key) {
            // not provided a placeholder value should generate an error.
            return null;
        }
    };

    KeyBasedValueResolver<SystemProperty> nullSystemResolver =
            new KeyBasedValueResolver<SystemProperty>() {
        @Nullable
        @Override
        public String getValue(@NonNull SystemProperty key) {
            return null;
        }
    };

    KeyBasedValueResolver<SystemProperty> keyBasedValueResolver =
            new KeyBasedValueResolver<SystemProperty>() {
                @Nullable
                @Override
                public String getValue(@NonNull SystemProperty key) {
                    if (key == SystemProperty.PACKAGE) {
                        return "com.bar.new";
                    }
                    return null;
                }
            };

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        when(mBuilder.getLogger()).thenReturn(mMockLog);
        when(mBuilder.getActionRecorder()).thenReturn(mActionRecorder);
    }

    public void testPlaceholders() throws ParserConfigurationException, SAXException, IOException {

        String xml = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <activity android:name=\"activityOne\"\n"
                + "         android:attr1=\"@{landscapePH}\"\n"
                + "         android:attr2=\"prefix.@{landscapePH}\"\n"
                + "         android:attr3=\"@{landscapePH}.suffix\"\n"
                + "         android:attr4=\"prefix@{landscapePH}suffix\">\n"
                + "    </activity>\n"
                + "</manifest>";

        XmlDocument refDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(), "testPlaceholders#xml"), xml);

        PlaceholderHandler handler = new PlaceholderHandler();
        handler.visit(refDocument, new KeyBasedValueResolver<String>() {
            @Override
            public String getValue(@NonNull String key) {
                return "newValue";
            }
        }, nullSystemResolver, mBuilder);

        Optional<XmlElement> activityOne = refDocument.getRootNode()
                .getNodeByTypeAndKey(ManifestModel.NodeTypes.ACTIVITY, ".activityOne");
        assertTrue(activityOne.isPresent());
        assertEquals(5, activityOne.get().getAttributes().size());
        // check substitution.
        assertEquals("newValue",
                activityOne.get().getAttribute(
                        XmlNode.fromXmlName("android:attr1")).get().getValue());
        assertEquals("prefix.newValue",
                activityOne.get().getAttribute(
                        XmlNode.fromXmlName("android:attr2")).get().getValue());
        assertEquals("newValue.suffix",
                activityOne.get().getAttribute(
                        XmlNode.fromXmlName("android:attr3")).get().getValue());
        assertEquals("prefixnewValuesuffix",
                activityOne.get().getAttribute(
                        XmlNode.fromXmlName("android:attr4")).get().getValue());

        for (XmlAttribute xmlAttribute : activityOne.get().getAttributes()) {
            // any attribute other than android:name should have been injected.
            if (!xmlAttribute.getName().toString().contains("name")) {
                verify(mActionRecorder).recordAttributeAction(
                        xmlAttribute,
                        ActionRecorder.ActionType.INJECTED,
                        null);
            }
        }
    }

    public void testPlaceHolder_notProvided()
            throws ParserConfigurationException, SAXException, IOException {
        String xml = ""
                + "<manifest\n"
                + "    xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <activity android:name=\"activityOne\"\n"
                + "         android:attr1=\"@{landscapePH}\"/>\n"
                + "</manifest>";

        XmlDocument refDocument = TestUtils.xmlDocumentFromString(
                new TestUtils.TestSourceLocation(getClass(), "testPlaceholders#xml"), xml);

        PlaceholderHandler handler = new PlaceholderHandler();
        handler.visit(refDocument, nullResolver, nullSystemResolver, mBuilder);
        // verify the error was recorded.
        verify(mBuilder).addError(anyString());
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
                new TestUtils.TestSourceLocation(getClass(), "testPlaceholders#xml"), xml);

        PlaceholderHandler handler = new PlaceholderHandler();
        handler.visit(refDocument, nullResolver, keyBasedValueResolver, mBuilder);
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
                new TestUtils.TestSourceLocation(getClass(), "testPlaceholders#xml"), xml);

        PlaceholderHandler handler = new PlaceholderHandler();
        handler.visit(refDocument, nullResolver, keyBasedValueResolver, mBuilder);
        // verify the package value was added.
        assertEquals("com.bar.new", refDocument.getRootNode().getXml().getAttribute("package"));
    }
}
