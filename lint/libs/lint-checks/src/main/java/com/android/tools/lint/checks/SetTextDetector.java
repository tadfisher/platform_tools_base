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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.client.api.JavaParser.TypeDescriptor;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.JavaScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.BinaryExpression;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.StringLiteral;

/**
 * Checks for errors related to Date Formats
 */
public class SetTextDetector extends Detector implements JavaScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            SetTextDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Constructing SimpleDateFormat without an explicit locale */
    public static final Issue NUMBER_FORMAT = Issue.create(
            "NumberInSetText", //$NON-NLS-1$
            "Passing result of Integer.toString() or Float.toString() call into TextView.setText",

            "Do not call toString() to convert number into a string when displaying it to a user. " +
            "It always uses ASCII digits (0-9) even if current locale uses other numbering " +
            "system (e.g. Arabic digits) or fraction separator.\n" +
            "\n" +
            "Consider using String.format with proper format specification (%d or %f) instead.",

            Category.I18N,
            6,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Passing a string literal into TextView.setText */
    public static final Issue LITERAL_SETTEXT = Issue.create(
            "LiteralInSetText", //$NON-NLS-1$
            "Passing string literal into TextView.setText invocation.",

            "Do not pass a string literal to setText to display any text to a user. " +
            "Hardcoded text can not be properly translated to other languages.\n" +
            "\n" +
            "Use Android resources and pass resource ID into a setText call instead.",

            Category.I18N,
            6,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Concatenation of text during TextView.setText invocation */
    public static final Issue CONCATENATION_SETTEXT = Issue.create(
            "ConcatenationInSetText", //$NON-NLS-1$
            "Concatenation of text during TextView.setText invocation.",

            "Do not build messages concatenating text chunks in setText. " +
            "Concatenated messages can not be properly translated to other languages.\n" +
            "\n" +
            "Use formatting string with numbered placeholders.",

            Category.I18N,
            6,
            Severity.WARNING,
            IMPLEMENTATION);


    private static final String METHOD_NAME = "setText";
    private static final String CHAR_SEQUENCE_CLS = "java.lang.CharSequence";
    private static final String NUMBER_CLS = "java.lang.Number";
    private static final String TEXTVIEW_CLS = "android.widget.TextView";

    // Pattern to match string literal that require translation. These are those having word
    // characters in it.
    private static final String WORD_PATTERN = ".*\\w.*";
    private static final String TOSTRING_SIGNATURE = "toString";

    /** Constructs a new {@link SetTextDetector} */
    public SetTextDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(METHOD_NAME);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
        @NonNull MethodInvocation call) {
      ResolvedMethod method = (ResolvedMethod)context.resolve(call);
      if (method != null && method.getContainingClass().matches(TEXTVIEW_CLS)
          && method.matches(METHOD_NAME)
          && method.getArgumentCount() > 0
          && method.getArgumentType(0).matchesSignature(CHAR_SEQUENCE_CLS)) {
        checkNode(context, call.getChildren().get(2));
      }
    }

    private void checkNode(JavaContext context, Node node) {
      if (node instanceof StringLiteral) {
        if (((StringLiteral)node).astValue().matches(WORD_PATTERN)) {
          context.report(LITERAL_SETTEXT, node, context.getLocation(node),
              "String literal in setText can not be translated. Use Android resources instead.");
        }
      } else if (node instanceof MethodInvocation) {
        ResolvedMethod rm = (ResolvedMethod)context.resolve(node);
        if (rm != null && rm.getName().matches(TOSTRING_SIGNATURE)
            && rm.getContainingClass().getSuperClass().matches(NUMBER_CLS)) {
          context.report(NUMBER_FORMAT, node, context.getLocation(node),
              "Number formatting does not take into account locale settings. Consider using String.format instead.");
        }
      } else if (node instanceof BinaryExpression) {
        if(((BinaryExpression)node).astOperator().getSymbol().equals("+")) {
          context.report(CONCATENATION_SETTEXT, node, context.getLocation(node),
              "Do not concatenate text displayed with setText. Use resource string with placeholders.");
        }
        for (Node child : node.getChildren()) {
          checkNode(context, child);
        }
      }
    }
}
