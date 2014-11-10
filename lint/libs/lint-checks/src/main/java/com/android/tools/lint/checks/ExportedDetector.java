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
import static com.android.SdkConstants.TAG_ACTIVITY;
import static com.android.SdkConstants.TAG_PROVIDER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.SdkConstants.TAG_SERVICE;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collection;

public class ExportedDetector extends Detector implements Detector.XmlScanner {

    /** Not explicitly setting exported attribute */
    public static final Issue ISSUE = Issue.create(
            "ImplicitExported",//$NON-NLS-1$
            "Export is not explicitly set",
            "An accidentally exported component could be accessible to any third party " +
                    "applications. Specifying android:exported=false (or true if proper permissions are " +
                    "present) explicitly will avoid any such accidental leakages.",
            Category.SECURITY,
            4,
            Severity.WARNING,
            new Implementation(
               ExportedDetector.class,
                Scope.MANIFEST_SCOPE));

    // ---- Implements Detector.XmlScanner ----
    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
            TAG_SERVICE,
            TAG_PROVIDER,
            TAG_ACTIVITY,
            TAG_RECEIVER
        );
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String exported = element.getAttributeNS(ANDROID_URI, ATTR_EXPORTED);
        if (exported == null || exported.isEmpty()) {
            context.report(ISSUE, element, context.getLocation(element),
                    "Component does not explicitly set android:exported");
        }
    }

}
