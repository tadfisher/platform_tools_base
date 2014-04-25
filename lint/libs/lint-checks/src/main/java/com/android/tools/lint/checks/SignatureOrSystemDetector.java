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


import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_PERMISSION;
import static com.android.SdkConstants.TAG_GRANT_PERMISSION;
import static com.android.SdkConstants.TAG_PERMISSION;
import static com.android.SdkConstants.TAG_PROVIDER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.SdkConstants.TAG_SERVICE;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * Checks if signatureOrSystem level permissions are set.
 */
public class SignatureOrSystemDetector extends Detector implements Detector.XmlScanner {
    public static final Issue ISSUE = Issue.create(
            "SignatureOrSystemPermissions", //$NON-NLS-1$
            "signatureOrSystem permissions declared",
            "Checks for signatureOrSystem level permissions",
            "The `signature` protection level should probably be sufficient for most needs and "
                    + "works regardless of where applications are installed. The "
                    + "`signatureOrSystem` level is used for certain situations where "
                    + "multiple vendors have applications built into a system image and "
                    + "need to share specific features explicitly because they are being built "
                    + "together. An app with `signatureOrSystem` permissions has the same "
                    + "access to data as any system app.",
            Category.SECURITY,
            5,
            Severity.WARNING,
            new Implementation(
                    SignatureOrSystemDetector.class,
                    Scope.MANIFEST_SCOPE
            ));
    private static final String SIGNATURE_OR_SYSTEM = "signatureOrSystem"; //$NON-NLS-1$
    private static final String PROTECTION_LEVEL = "protectionLevel"; //$NON-NLS-1$

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return file.getName().equals(ANDROID_MANIFEST_XML);
    }

    // ---- Implements Detector.XmlScanner ---- TAG_PERMISSION

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(TAG_PERMISSION);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        Attr protectionLevelAttr = element.getAttributeNodeNS(ANDROID_URI, PROTECTION_LEVEL);
        if (protectionLevelAttr != null) {
            String protectionLevel = protectionLevelAttr.getValue();
            if (protectionLevel != null
                && protectionLevel.equals(SIGNATURE_OR_SYSTEM)) {
                String message = "protectionLevel should probably not be set to signatureOrSystem";
                context.report(ISSUE, context.getLocation(protectionLevelAttr), message, null);
            }
        }
    }
}
