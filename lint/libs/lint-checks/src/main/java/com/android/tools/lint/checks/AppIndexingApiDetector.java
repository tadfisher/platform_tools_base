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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.*;
import com.google.common.collect.Lists;
import org.w3c.dom.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.android.SdkConstants.*;
import static com.android.xml.AndroidManifest.*;


/**
 * Check if the usage of App Indexing is correct.
 * Will report explicit issues as error, and possible issues as warning to promote app indexing.
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

    private static final String[] PATH_ATTR_LIST = new String[]{ATTR_PATH_PREFIX, ATTR_PATH,
            ATTR_PATH_PATTERN};

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(NODE_APPLICATION);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element application) {
        List<Element> activities = extractChildrenByName(application, NODE_ACTIVITY);
        boolean applicationHasActionView = false;
        for (Element activity : activities) {
            List<Element> intents = extractChildrenByName(activity, NODE_INTENT);
            boolean activityHasActionView = false;
            for (Element intent : intents) {
                boolean actionView = hasActionView(intent);
                boolean browsable = isBrowsable(intent);
                checkIntentFilter(context, intent, actionView, browsable);
                if (actionView) {
                    activityHasActionView = true;
                }
            }
            if (activityHasActionView) {
                applicationHasActionView = true;
                if (activity.hasAttributeNS(ANDROID_URI, ATTR_EXPORTED)) {
                    Attr exported = activity.getAttributeNodeNS(ANDROID_URI, ATTR_EXPORTED);
                    if (!exported.getValue().equals("true")) {
                        // Report error if the activity supporting action view is not exported.
                        context.report(ISSUE_ERROR, activity, context.getLocation(activity),
                                "Activity supporting ACTION_VIEW isn't exported correctly");
                    }
                }
            }
        }
        if (!applicationHasActionView) {
            // Report warning if there're no
            context.report(ISSUE_WARNING, application, context.getLocation(application),
                    "Application should has at least one Activity supporting ACTION_VIEW");
        }
    }

    private static void checkIntentFilter(@NonNull XmlContext context, @NonNull Element intent,
            boolean actionView, boolean browsable) {
        boolean hasScheme = false;
        boolean hasHost = false;
        boolean hasPort = false;
        boolean hasPath = false;
        boolean hasMimeType = false;
        List<Element> datas = extractChildrenByName(intent, NODE_DATA);
        Element firstData = datas.size() > 0 ? datas.get(0) : null;
        for (Element data : datas) {
            checkSingleData(context, data);

            for (String name : PATH_ATTR_LIST) {
                if (data.hasAttributeNS(ANDROID_URI, name)) {
                    hasPath = true;
                }
            }

            if (data.hasAttributeNS(ANDROID_URI, ATTR_SCHEME)) {
                hasScheme = true;
            }

            if (data.hasAttributeNS(ANDROID_URI, ATTR_HOST)) {
                hasHost = true;
            }

            if (data.hasAttributeNS(ANDROID_URI, ATTRIBUTE_PORT)) {
                hasPort = true;
            }

            if (data.hasAttributeNS(ANDROID_URI, ATTRIBUTE_MIME_TYPE)) {
                hasMimeType = true;
            }
        }

        // In data field, a URL is consisted by
        // <scheme>://<host>:<port>[<path>|<pathPrefix>|<pathPattern>]
        // Each part of the URL should not have illegal character.
        if ((hasPath || hasHost || hasPort) && !hasScheme) {
            context.report(ISSUE_ERROR, firstData, context.getLocation(firstData),
                    "android:scheme missing");
        }

        if ((hasPath || hasPort) && !hasHost) {
            context.report(ISSUE_ERROR, firstData, context.getLocation(firstData),
                    "android:host missing");
        }

        if (actionView) {
            if (browsable) {
                if (firstData == null) {
                    // If this activity is an ACTION_VIEW action with category BROWSABLE, but
                    // doesn't have data node, it may be a mistake and we will report error.
                    context.report(ISSUE_ERROR, intent, context.getLocation(intent),
                            "Missing data node?");
                } else if (!hasScheme && !hasMimeType) {
                    // If this activity is an action view, is browsable, but has neither a
                    // URL nor mimeType, it may be a mistake and we will report error.
                    context.report(ISSUE_ERROR, firstData, context.getLocation(firstData),
                            "Missing URL for the intent filter?");
                }
            } else {
                // If this activity is an ACTION_VIEW action, has a http URL but doesn't have
                // BROWSABLE, it may be a mistake and and we will report warning.
                context.report(ISSUE_WARNING, intent, context.getLocation(intent),
                        "Activity supporting ACTION_VIEW is not set as BROWSABLE");
            }
        }
    }

    /**
     * Check if the intent filter supports action view.
     * @param intent the intent filter
     * @return true if it does
     */
    private static boolean hasActionView(Element intent) {
        List<Element> actions = extractChildrenByName(intent, NODE_ACTION);
        for (Element action : actions) {
            if (action.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                Attr attr = action.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
                if (attr.getValue().equals("android.intent.action.VIEW")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the intent filter is set as browsable.
     *
     * @param intent the intent filter.
     * @return true if it is.
     */
    private static boolean isBrowsable(@NonNull Element intent) {
        List<Element> categories = extractChildrenByName(intent, NODE_CATEGORY);
        for (Element e : categories) {
            if (e.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                Attr attr = e.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
                if (attr.getNodeValue().equals("android.intent.category.BROWSABLE")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the attributes in data is legal.
     *
     * @param context the Xml content.
     * @param data    the data node.
     */
    private static void checkSingleData(@NonNull XmlContext context, @NonNull Element data) {
        // path, pathPrefix and pathPattern should starts with /.
        for (String name : PATH_ATTR_LIST) {
            if (data.hasAttributeNS(ANDROID_URI, name)) {
                Attr attr = data.getAttributeNodeNS(ANDROID_URI, name);
                if (!attr.getValue().startsWith("/")) {
                    context.report(ISSUE_ERROR, attr, context.getLocation(attr),
                            "android:" + name + " attribute should start with /");
                }
            }
        }

        // port should be a legal number.
        if (data.hasAttributeNS(ANDROID_URI, ATTRIBUTE_PORT)) {
            Attr attr = data.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_PORT);
            try {
                Integer.parseInt(attr.getValue());
            } catch (NumberFormatException e) {
                context.report(ISSUE_ERROR, attr, context.getLocation(attr),
                        "android:port is not a legal number");
            }
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

    private static List<Element> extractChildrenByName(@NonNull Element node,
            @NonNull String name) {
        List<Element> result = Lists.newArrayList();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(name)) {
                result.add((Element) child);
            }
        }
        return result;
    }
}
