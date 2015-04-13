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
import static com.android.xml.AndroidManifest.ATTRIBUTE_NAME;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.collect.Sets;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;


/**
 * Check if the usage of app indexing api is correct.
 */
public class AppIndexingApiDetector extends Detector implements Detector.XmlScanner {

    public static final Issue ISSUE = Issue.create("AppIndexing", //$NON-NLS-1$
            "Wrong usage of AppIndexing.",
            "Ensure the app can correctly handle deep links and integrate with " +
                    "Google AppIndexing Api.",
            Category.USABILITY, 5, Severity.WARNING,
            new Implementation(AppIndexingApiDetector.class, Scope.MANIFEST_SCOPE))
            .addMoreInfo("http://g.co/AppIndexing");

    private boolean mHasActionView;

    private Set<Location> mActivityLocations;

    public AppIndexingApiDetector() {
        mActivityLocations = Sets.newHashSet();
    }

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(NODE_ACTIVITY);
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        mHasActionView = false;
        mActivityLocations.clear();
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (!mActivityLocations.isEmpty() && !mHasActionView) {
            for (Location location : mActivityLocations) {
                context.report(ISSUE, location,
                        "Need an <action> tag that specifies the ACTION_VIEW intent action.");
            }
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        mActivityLocations.add(context.getLocation(element));
        if (element.hasChildNodes()) {
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    if (child.getNodeName().equals(NODE_INTENT)) {
                        if (child.hasChildNodes()) {
                            NodeList grandChildren = child.getChildNodes();
                            boolean isActionView = false;

                            // Check if this activity supports action view.
                            for (int j = 0; j < grandChildren.getLength(); j++) {
                                if (checkActionView(grandChildren.item(j))) {
                                    isActionView = true;
                                    break;
                                }
                            }

                            if (isActionView) {
                                mHasActionView = true;
                                boolean isBrowsable = false;
                                for (int j = 0; j < grandChildren.getLength(); j++) {
                                    Node grandChild = grandChildren.item(j);
                                    if (grandChild.getNodeType() == Node.ELEMENT_NODE) {
                                        Element e = (Element) grandChild;
                                        if (e.getNodeName().equals(NODE_DATA)) {
                                            checkData(context, e);
                                        } else if (e.getNodeName().equals(NODE_CATEGORY)) {
                                            if (checkBrowsable(e)) {
                                                isBrowsable = true;
                                            }
                                        }
                                    }
                                }
                                if (!isBrowsable) {
                                    context.report(ISSUE, child, context.getLocation(child),
                                            "Activity supporting ACTION_VIEW "
                                                    + "is not set as browsable");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkActionView(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE && node
                .getNodeName().equals(NODE_ACTION)) {
            Element e = (Element) node;
            if (e.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                Attr attr = e.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
                if (attr.getValue().equals("android.intent.action.VIEW")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkBrowsable(Element e) {
        if (e.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
            Attr attr = e.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
            if (attr.getNodeValue().equals("android.intent.category.BROWSABLE")) {
                return true;
            }
        }
        return false;
    }

    private void checkData(XmlContext context, Element element) {
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
