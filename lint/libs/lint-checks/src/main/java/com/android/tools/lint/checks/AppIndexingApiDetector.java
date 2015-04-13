/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.lint.checks;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_HOST;
import static com.android.SdkConstants.ATTR_PATH;
import static com.android.SdkConstants.ATTR_PATH_PATTERN;
import static com.android.SdkConstants.ATTR_PATH_PREFIX;
import static com.android.SdkConstants.ATTR_SCHEME;

import static com.android.xml.AndroidManifest.ATTRIBUTE_MIME_TYPE;
import static com.android.xml.AndroidManifest.ATTRIBUTE_NAME;
import static com.android.xml.AndroidManifest.ATTRIBUTE_PORT;
import static com.android.xml.AndroidManifest.NODE_ACTION;
import static com.android.xml.AndroidManifest.NODE_ACTIVITY;
import static com.android.xml.AndroidManifest.NODE_CATEGORY;
import static com.android.xml.AndroidManifest.NODE_DATA;
import static com.android.xml.AndroidManifest.NODE_INTENT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Collections;


/**
 * Check if the usage of App Indexing is correct.
 */
public class AppIndexingApiDetector extends Detector implements Detector.XmlScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            AppIndexingApiDetector.class, Scope.MANIFEST_SCOPE);

    public static final Issue ISSUE_ERROR = Issue.create("AppIndexingError", //$NON-NLS-1$
            "Wrong Usage of App Indexing",
            "Ensures the app can correctly handle deep links and integrate with " +
                    "App Indexing for Google search.",
            Category.USABILITY, 5, Severity.ERROR, IMPLEMENTATION)
            .addMoreInfo("https://g.co/AppIndexing");

    public static final Issue ISSUE_WARNING = Issue.create("AppIndexingWarning", //$NON-NLS-1$
            "Missing App Indexing Support",
            "Ensures the app can correctly handle deep links and integrate with " +
                    "App Indexing for Google search.",
            Category.USABILITY, 5, Severity.WARNING, IMPLEMENTATION)
            .addMoreInfo("https://g.co/AppIndexing");

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(NODE_ACTIVITY);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element activity) {
        boolean actionView = isActionView(activity);
        NodeList children = activity.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    child.getNodeName().equals(NODE_INTENT)) {
                Element intent = (Element) child;
                boolean browsable = isBrowsable(intent);
                boolean isHttp = false;
                boolean hasData = false;
                NodeList intentChildren = intent.getChildNodes();
                for (int j = 0; j < intentChildren.getLength(); j++) {
                    Node intentChild = intentChildren.item(j);
                    if (intentChild.getNodeType() == Node.ELEMENT_NODE) {
                        Element e = (Element) intentChild;
                        if (e.getNodeName().equals(NODE_DATA)) {
                            hasData = true;
                            if (isHttpSchema(e)) {
                                isHttp = true;
                            }
                            checkData(context, e);

                            // If this activity is an action view, is browsable, but has neither a
                            // URL nor mimeType, it may be a mistake and we will report warning.
                            if (actionView && browsable &&
                                    !e.hasAttributeNS(ANDROID_URI, ATTR_SCHEME) &&
                                    !e.hasAttributeNS(ANDROID_URI, ATTRIBUTE_MIME_TYPE)) {
                                context.report(ISSUE_ERROR, e, context.getLocation(e),
                                        "URL should be set for the intent filter");
                            }
                        }
                    }
                }

                // If this activity is an ACTION_VIEW action, has a http URL but doesn't have
                // BROWSABLE, it may be a mistake and and we will report warning.
                if (actionView && isHttp && !browsable) {
                    context.report(ISSUE_WARNING, intent, context.getLocation(intent),
                            "Activity supporting ACTION_VIEW is not set as BROWSABLE");
                }

                // If this activity is an ACTION_VIEW action with category BROWSABLE, but doesn't
                // have data node, it may be a mistake and we will report error.
                if (actionView && browsable && !hasData) {
                    context.report(ISSUE_ERROR, intent, context.getLocation(intent),
                            "Missing data node");
                }
            }
        }
    }

    private static boolean isActionView(Element activity) {
        NodeList children = activity.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    child.getNodeName().equals(NODE_INTENT)) {
                Element intent = (Element) child;
                NodeList intentChildren = intent.getChildNodes();
                for (int j = 0; j < intentChildren.getLength(); j++) {
                    Node intentChild = intentChildren.item(j);
                    if (intentChild.getNodeType() == Node.ELEMENT_NODE &&
                            intentChild.getNodeName().equals(NODE_ACTION)) {
                        Element action = (Element) intentChild;
                        if (action.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                            Attr attr = action.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
                            if (attr.getValue().equals("android.intent.action.VIEW")) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isBrowsable(Element intent) {
        NodeList children = intent.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) child;
                if (e.getNodeName().equals(NODE_CATEGORY)) {
                    if (e.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                        Attr attr = e.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
                        if (attr.getNodeValue().equals("android.intent.category.BROWSABLE")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isHttpSchema(Element data) {
        if (data.hasAttributeNS(ANDROID_URI, ATTR_SCHEME)) {
            String value = data.getAttributeNodeNS(ANDROID_URI, ATTR_SCHEME).getValue();
            if (value.equals("http") || value.equals("https")) {
                return true;
            }
        }
        return false;
    }

    private static final String[] PATH_ATTR_TO_CHECK = new String[]{ATTR_PATH_PREFIX, ATTR_PATH,
            ATTR_PATH_PATTERN};

    private static void checkData(XmlContext context, Element data) {
        boolean hasScheme = false;
        boolean hasHost = false;
        boolean hasPort = false;
        boolean hasPath = false;

        // In data field, a URL is consisted by
        // <scheme>://<host>:<port>[<path>|<pathPrefix>|<pathPattern>]
        // Each part of the URL should not has illegal character.

        // path, pathPrefix and pathPattern should starts with /.
        for (String name : PATH_ATTR_TO_CHECK) {
            if (data.hasAttributeNS(ANDROID_URI, name)) {
                hasPath = true;
                Attr attr = data.getAttributeNodeNS(ANDROID_URI, name);
                if (!attr.getValue().startsWith("/")) {
                    context.report(ISSUE_ERROR, attr, context.getLocation(attr),
                            "android:" + name + " attribute should start with /");
                }
            }
        }

        if (data.hasAttributeNS(ANDROID_URI, ATTR_SCHEME)) {
            hasScheme = true;
        }

        if (data.hasAttributeNS(ANDROID_URI, ATTR_HOST)) {
            hasHost = true;
        }

        // port should be a legal number.
        if (data.hasAttributeNS(ANDROID_URI, ATTRIBUTE_PORT)) {
            hasPort = true;
            Attr attr = data.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_PORT);
            try {
                Integer.parseInt(attr.getValue());
            } catch (NumberFormatException e) {
                context.report(ISSUE_ERROR, attr, context.getLocation(attr),
                        "android:port is not a legal number");
            }
        }

        if ((hasPath || hasHost || hasPort) && !hasScheme) {
            context.report(ISSUE_ERROR, data, context.getLocation(data),
                    "android:scheme missing");
        }

        if ((hasPath || hasPort) && !hasHost) {
            context.report(ISSUE_ERROR, data, context.getLocation(data),
                    "android:host missing");
        }

        // Each field should be non empty.
        NamedNodeMap attrs = data.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node item = attrs.item(i);
            if (item.getNodeType() == Node.ATTRIBUTE_NODE) {
                Attr attr = (Attr) attrs.item(i);
                if (attr.getValue().isEmpty()) {
                    context.report(ISSUE_ERROR, attr, context.getLocation(attr),
                            attr.getName() + " cannot be empty");
                }
            }
        }
    }
}
