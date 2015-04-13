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
import static com.android.SdkConstants.ATTR_PATH_PREFIX;
import static com.android.SdkConstants.ATTR_SCHEME;

import static com.android.xml.AndroidManifest.NODE_ACTION;
import static com.android.xml.AndroidManifest.NODE_ACTIVITY;
import static com.android.xml.AndroidManifest.NODE_CATEGORY;
import static com.android.xml.AndroidManifest.NODE_DATA;
import static com.android.xml.AndroidManifest.NODE_INTENT;
import static com.android.xml.AndroidManifest.NODE_APPLICATION;
import static com.android.xml.AndroidManifest.ATTRIBUTE_NAME;

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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Collections;


/**
 * Check if the usage of app indexing api is correct.
 */
public class AppIndexingApiDetector extends Detector implements Detector.XmlScanner {

    public static final Issue ISSUE = Issue.create("AppIndexing", //$NON-NLS-1$
            "Wrong Usage of AppIndexing",
            "Ensure the app can correctly handle deep links and integrate with " +
                    "Google AppIndexing Api.",
            Category.USABILITY, 5, Severity.WARNING,
            new Implementation(AppIndexingApiDetector.class, Scope.MANIFEST_SCOPE))
            .addMoreInfo("http://g.co/AppIndexing");

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(NODE_APPLICATION);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element application) {
        boolean hasActionView = false;
        boolean hasActivity = false;
        if (application.hasChildNodes()) {
            NodeList children = application.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (child.getNodeName().equals(NODE_ACTIVITY)) {
                        hasActivity = true;
                        Element activity = (Element) child;
                        if (isActionView(activity)) {
                            hasActionView = true;
                            checkActivity(context, activity);
                        }
                    }
                }
            }
        }
        if (hasActivity && !hasActionView) {
            context.report(ISSUE, application, context.getLocation(application),
                    "Need an <action> tag that specifies the ACTION_VIEW intent action.");
        }
    }

    private static boolean isActionView(Element activity) {
        if (activity.hasChildNodes()) {
            NodeList children = activity.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName()
                        .equals(NODE_INTENT)) {
                    Element intent = (Element) child;
                    if (intent.hasChildNodes()) {
                        NodeList grandChildren = intent.getChildNodes();
                        for (int j = 0; j < grandChildren.getLength(); j++) {
                            Node grandChild = grandChildren.item(j);
                            if (grandChild.getNodeType() == Node.ELEMENT_NODE && grandChild
                                    .getNodeName().equals(NODE_ACTION)) {
                                Element action = (Element) grandChild;
                                if (action.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                                    Attr attr = action
                                            .getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
                                    if (attr.getValue().equals("android.intent.action.VIEW")) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void checkActivity(XmlContext context, Element activity) {
        if (activity.hasChildNodes()) {
            NodeList children = activity.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName()
                        .equals(NODE_INTENT)) {
                    Element intent = (Element) child;
                    if (intent.hasChildNodes()) {
                        NodeList grandChildren = intent.getChildNodes();
                        boolean browsable = false;
                        for (int j = 0; j < grandChildren.getLength(); j++) {
                            Node grandChild = grandChildren.item(j);
                            if (grandChild.getNodeType() == Node.ELEMENT_NODE) {
                                Element e = (Element) grandChild;
                                if (e.getNodeName().equals(NODE_DATA)) {
                                    checkData(context, e);
                                } else if (e.getNodeName().equals(NODE_CATEGORY)) {
                                    if (isBrowsable(e)) {
                                        browsable = true;
                                    }
                                }
                            }
                        }
                        if (!browsable) {
                            context.report(ISSUE, intent, context.getLocation(intent),
                                    "Activity supporting ACTION_VIEW "
                                            + "is not set as browsable");
                        }
                    }
                }
            }
        }
    }

    private static boolean isBrowsable(Element e) {
        if (e.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
            Attr attr = e.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
            if (attr.getNodeValue().equals("android.intent.category.BROWSABLE")) {
                return true;
            }
        }
        return false;
    }

    private static void checkData(XmlContext context, Element element) {
        if (!element.hasAttributeNS(ANDROID_URI, ATTR_SCHEME)) {
            context.report(ISSUE, element, context.getLocation(element),
                    "At least one android:scheme attribute should be set for the intent filter");
        }

        if (!element.hasAttributeNS(ANDROID_URI, ATTR_HOST)) {
            context.report(ISSUE, element, context.getLocation(element),
                    "android:host attribute must be set for the intent filter");
        }

        if (element.hasAttributeNS(ANDROID_URI, ATTR_PATH_PREFIX)) {
            Attr pathPrefix = element.getAttributeNodeNS(ANDROID_URI, ATTR_PATH_PREFIX);
            if (!pathPrefix.getValue().startsWith("/")) {
                context.report(ISSUE, pathPrefix, context.getLocation(pathPrefix),
                        "android:pathPrefix attribute should start with /");
            }
        }
    }
}
