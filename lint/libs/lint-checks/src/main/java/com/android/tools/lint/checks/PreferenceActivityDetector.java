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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_EXPORTED;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PACKAGE;
import static com.android.SdkConstants.TAG_ACTIVITY;
import static com.android.SdkConstants.TAG_INTENT_FILTER;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.objectweb.asm.tree.ClassNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Ensures that PreferenceActivity and its subclasses are never exported.
 */
public class PreferenceActivityDetector extends Detector
        implements Detector.XmlScanner, Detector.ClassScanner {
    public static final Issue ISSUE = Issue.create(
            "ExportedPreferenceActivity", //$NON-NLS-1$
            "PreferenceActivity should not be exported",
            "Checks that PreferenceActivity and its subclasses are never exported",
            "Fragment injection gives anyone who can send your PreferenceActivity an intent the "
                + "ability to load any fragment, with any arguments, in your process.",
            Category.SECURITY,
            8,
            Severity.WARNING,
            new Implementation(
                    PreferenceActivityDetector.class,
                    EnumSet.of(Scope.MANIFEST, Scope.CLASS_FILE)));
    private static final String PREFERENCE_ACTIVITY = "android.preference.PreferenceActivity";
    private static final String PREFERENCE_ACTIVITY_VM = "android/preference/PreferenceActivity"; //$NON-NLS-1$

    private final Map<String, Location> mExportedActivities = new HashMap<String, Location>();
    private String mPackage = null;

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements XmlScanner ----
    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_PACKAGE);
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        mPackage = attribute.getValue();
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(TAG_ACTIVITY);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (isExported(element)) {
            String fqcn = getFQCN(element);
            if (fqcn != null) {
                if (fqcn.equals(PREFERENCE_ACTIVITY)) {
                    String message = "PreferenceActivity should not be exported";
                    context.report(ISSUE, context.getLocation(element), message, null);
                }
                mExportedActivities.put(fqcn, context.getLocation(element));
            }
        }
    }

    private String getFQCN(@NonNull Element activityElement) {
        String activityClassName = activityElement.getAttributeNS(ANDROID_URI, ATTR_NAME);

        if (activityClassName == null) {
            return null;
        }

        // If the activity class name starts with a '.', it is shorthand for prepending the
        // package name specified int he manifest.
        if (activityClassName.startsWith(".")) {
            if (mPackage != null) {
                return mPackage + activityClassName;
            } else {
                return null;
            }
        }

        return activityClassName;
    }

    private boolean isExported(Element element) {
        String exportValue = element.getAttributeNS(ANDROID_URI, ATTR_EXPORTED);
        if (exportValue != null && !exportValue.isEmpty()) {
            return Boolean.valueOf(exportValue);
        } else {
            for (Element child : LintUtils.getChildren(element)) {
                if (child.getTagName().equals(TAG_INTENT_FILTER)) {
                    return true;
                }
            }
        }

        return false;
    }

    // ---- Implements ClassScanner ----);
    @Override
    public void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode) {
        if (context.getDriver().isSubclassOf(classNode, PREFERENCE_ACTIVITY_VM)) {
            String javaName = toJavaName(classNode.name);
            if (mExportedActivities.containsKey(javaName)) {
            String message = String.format(
                    "PreferenceActivity subclass %1$s should not be exported",
                    javaName);
            context.report(ISSUE, mExportedActivities.get(javaName), message, null);
            }
        }
    }

    private String toJavaName(String fqcn) {
        return fqcn.replace('/', '.');
    }
}
