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

import static com.android.SdkConstants.SUPPORT_ANNOTATIONS_PREFIX;
import static com.android.resources.ResourceType.COLOR;
import static com.android.resources.ResourceType.DRAWABLE;
import static com.android.resources.ResourceType.MIPMAP;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedAnnotation;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.util.Locale;

import lombok.ast.AstVisitor;
import lombok.ast.Expression;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Select;

/**
 * Looks for mismatched resource types
 */
public class ResourceTypeDetector extends Detector implements Detector.JavaScanner {

    /**
     * Attempting to set a resource id as a color
     */
    public static final Issue ISSUE = Issue.create(
            "ResourceType", //$NON-NLS-1$
            "Wrong Resource Type",

            "Ensures that resource id's passed to APIs are of the right type; for example, " +
            "calling `Resources.getColor(R.string.name)` is wrong.",

            Category.CORRECTNESS,
            7,
            Severity.FATAL,
            new Implementation(
                    ResourceTypeDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /**
     * Constructs a new {@link ResourceTypeDetector} check
     */
    public ResourceTypeDetector() {
    }

    // ---- Implements JavaScanner ----

    @Override
    public boolean appliesToResourceRefs() {
        return true;
    }

    @Override
    public void visitResourceReference(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull Node select, @NonNull String type, @NonNull String name, boolean isFramework) {
        while (select.getParent() instanceof Select) {
            select = select.getParent();
        }

        // TODO: Check returns in the current method,
        //  @ColorRes int getFoo() { return R.layout.bar; } <== Wrong
        //
        // TODO: check return value comparisons, etc
        //  @ColorRes abstract int getColor(); ... if (getColor() == R.layout.foo)  <== Wrong

        Node parameter = select;
        Node current = select.getParent();
        while (current != null) {
            if (current.getClass() == MethodInvocation.class) {
                MethodInvocation call = (MethodInvocation) current;
                ResolvedNode resolved = context.resolve(call);
                if (resolved instanceof ResolvedMethod) {
                    ResolvedMethod method = (ResolvedMethod) resolved;
                    int index = 0;
                    for (Expression expression : call.astArguments()) {
                        if (expression == parameter) {
                            for (ResolvedAnnotation annotation : method.getParameterAnnotations(index)) {
                                String mismatchedType = getMismatchedType(type, annotation);
                                if (mismatchedType != null) {
                                    // TODO: Unify with error message in the IDE
                                    String message = String.format(
                                            "Wrong resource type; expected %1$s but received %2$s",
                                            mismatchedType, type);
                                    context.report(ISSUE, select, context.getLocation(select),
                                            message);
                                }
                            }
                            break;
                        }
                        index++;
                    }
                }
                break;
            } else if (current.getClass() == MethodDeclaration.class) {
                break;
            }
            parameter = current;
            current = current.getParent();
        }
    }

    @Nullable
    private static String getMismatchedType(@NonNull String type,
            ResolvedAnnotation annotation) {
        String mismatchedType = null;
        String signature = annotation.getSignature();
        if (!signature.endsWith("Res") || !signature.startsWith(SUPPORT_ANNOTATIONS_PREFIX)) {
            return null;
        }
        String t = signature.substring(SUPPORT_ANNOTATIONS_PREFIX.length());
        t = t.substring(0, t.length() - 3); // remove "Res" suffix
        if (!t.equalsIgnoreCase(type)) {
            if (DRAWABLE.getName().equalsIgnoreCase(t)
                    && (COLOR.getName().equals(type)
                    || MIPMAP.getName().equals(type))) {
                // You can pass colors and mipmaps as well
                return null;
            }
            mismatchedType = t.toLowerCase(Locale.US);
        }
        return mismatchedType;
    }
}
