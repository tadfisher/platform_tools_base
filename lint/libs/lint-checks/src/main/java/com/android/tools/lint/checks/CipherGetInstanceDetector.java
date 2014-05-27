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

import static com.android.tools.lint.client.api.JavaParser.TYPE_OBJECT;
import static com.android.tools.lint.client.api.JavaParser.TYPE_STRING;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.Expression;
import lombok.ast.MethodInvocation;
import lombok.ast.Select;
import lombok.ast.StrictListAccessor;
import lombok.ast.StringLiteral;
import lombok.ast.VariableReference;

/**
 * Ensures that Cipher.getInstance is not called with AES as the parameter.
 */
public class CipherGetInstanceDetector extends Detector implements Detector.JavaScanner {
    public static final Issue ISSUE = Issue.create(
            "GetInstance", //$NON-NLS-1$
            "getInstance Called",
            "Checks that `Cipher#getInstance` is not called with AES as the parameter",
            "`Cipher#getInstance` should not be called with AES as the parameter because the "
                    + "default mode of vanilla AES on android is ECB, which is insecure.",
            Category.SECURITY,
            9,
            Severity.WARNING,
            new Implementation(
                    CipherGetInstanceDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    private static final String CIPHER = "javax.crypto.Cipher"; //$NON-NLS-1$
    private static final String AES = "AES"; //$NON-NLS-1$
    private static final String GET_INSTANCE = "getInstance"; //$NON-NLS-1$

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(GET_INSTANCE);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        // Ignore if the method doesn't fit our description.
        JavaParser.ResolvedNode resolved = context.resolve(node);
        if (!(resolved instanceof JavaParser.ResolvedMethod)) {
            return;
        }
        JavaParser.ResolvedMethod method = (JavaParser.ResolvedMethod) resolved;
        if (!method.getContainingClass().isSubclassOf(CIPHER, false)) {
            return;
        }
        StrictListAccessor<Expression, MethodInvocation> argumentList = node.astArguments();
        if (argumentList != null && argumentList.size() == 1) {
            Expression expression = argumentList.first();
            if (expression instanceof StringLiteral) {
                StringLiteral argument = (StringLiteral)expression;
                if (AES.equals(argument.astValue())) {
                    String message = "Cipher.getInstance(\"AES\") should not be called";
                    context.report(ISSUE, node, context.getLocation(argument), message, null);
                }
            }
        }
    }

}
