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

import static com.android.tools.lint.client.api.JavaParser.TYPE_STRING;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedField;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ClassDeclaration;
import lombok.ast.Expression;
import lombok.ast.If;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.StringLiteral;

/**
 * Detector for finding inefficiencies in logging calls.
 * <p>
 * Investigate:
 * <ul>
 * <li> Perhaps limit the isLogging call check
 * <li> I should see if you're doing work (string parameter is an expression);
 * if so, you should be at least guarding it with an "isLoggable" to avoid doing
 * work.
 * <li>Using ProGuard to strip these out: If you have ProGuard rules to strip
 * them out, then you don't need to do this, right? Make sure that actually
 * strips out any computation related to the parameters too!
 * <li>There were some comments on StackOverflow indicating that they "will be
 * stripped at runtime"; find out if that's true and "who" does it; is it the dx
 * compiler? See
 * http://stackoverflow.com/questions/2018263/android-logging/2019002#2019002
 * and http://developer.android.com/reference/android/util/Log.html :
 * "Verbose should never be compiled into an application except during
 * development. Debug logs are compiled in but stripped at runtime. Error,
 * warning and info logs are always kept."
 */
public class LogDetector extends Detector implements Detector.JavaScanner {
    private static final Implementation IMPLEMENTATION = new Implementation(
          LogDetector.class, Scope.JAVA_FILE_SCOPE);


    /** Log call missing surrounding if */
    public static final Issue CONDITIONAL = Issue.create(
            "LogConditional", //$NON-NLS-1$
            "Unconditional Logging Calls",
            "The BuildConfig class (available in Tools 17) provides a constant, \"DEBUG\", " +
            "which indicates whether the code is being built in release mode or in debug " +
            "mode. In release mode, you typically want to strip out all the logging calls. " +
            "Since the compiler will automatically remove all code which is inside a " +
            "\"if (false)\" check, surrounding your logging calls with a check for " +
            "BuildConfig.DEBUG is a good idea.\n" +
            "\n" +
            "If you *really* intend for the logging to be present in release mode, you can " +
            "suppress this warning with a @SuppressLint annotation for the intentional " +
            "logging calls.",

            Category.PERFORMANCE,
            5,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Mismatched tags between isLogging and log calls within it */
    public static final Issue WRONG_TAG = Issue.create(
            "LogTagMismatch", //$NON-NLS-1$
            "Mismatched Log Tags",
            "When guarding a `Log.v(tag, ...)` call with `Log.isLoggable(tag)`, the " +
            "tag passed to both calls should be the same.",

            Category.CORRECTNESS,
            5,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Log tag is too long */
    public static final Issue LONG_TAG = Issue.create(
            "LongLogTag", //$NON-NLS-1$
            "Too Long Log Tags",
            "Log tags are only allowed to be at most 23 tag characters long.",

            Category.CORRECTNESS,
            5,
            Severity.ERROR,
            IMPLEMENTATION);

    @SuppressWarnings("SpellCheckingInspection")
    private static final String IS_LOGGABLE = "isLoggable";       //$NON-NLS-1$
    private static final String LOG_CLS = "android.util.Log";     //$NON-NLS-1$
    private static final String PRINTLN = "println";              //$NON-NLS-1$

    // ---- Implements Detector.JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList(
                "d",           //$NON-NLS-1$
                "e",           //$NON-NLS-1$
                "i",           //$NON-NLS-1$
                "v",           //$NON-NLS-1$
                PRINTLN,
                IS_LOGGABLE);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor, @NonNull MethodInvocation node) {
        ResolvedNode resolved = context.resolve(node);
        if (!(resolved instanceof ResolvedMethod)) {
            return;
        }

        ResolvedMethod method = (ResolvedMethod) resolved;
        if (!method.getContainingClass().matches(LOG_CLS)) {
            return;
        }

        String name = node.astName().astValue();
        boolean withinConditional = IS_LOGGABLE.equals(name) ||
                checkWithinConditional(context, node.getParent(), node);

        // See if it's surrounded by an if statement (and it's one of the non-error, spammy
        // log methods (info, verbose, etc))
        if (("i".equals(name) || "d".equals(name) || "v".equals(name) || PRINTLN.equals(name))
                && !withinConditional
                && context.isEnabled(CONDITIONAL)) {
            String message = String.format("The log call Log.%1$s(...) should be " +
                            "conditional: surround with `if (Log.isLoggable(...))` or " +
                            "`if (BuildConfig.DEBUG) { ... }`",
                    node.astName().toString());
            context.report(CONDITIONAL, node, context.getLocation(node), message);
        }

        // Check tag length
        if (context.isEnabled(LONG_TAG)) {
            int tagArgumentIndex = PRINTLN.equals(name) ? 1 : 0;
            if (method.getArgumentCount() > tagArgumentIndex
                    && method.getArgumentType(tagArgumentIndex).matchesSignature(TYPE_STRING)
                    && node.astArguments().size() == method.getArgumentCount()) {
                Iterator<Expression> iterator = node.astArguments().iterator();
                if (tagArgumentIndex == 1) {
                    iterator.next();
                }
                Node argument = iterator.next();
                String tag = findLiteralValue(context, argument);
                if (tag != null && tag.length() > 23) {
                    String message = String.format(
                        "The logging tag can be at most 23 characters, was %1$d (%2$s)",
                        tag.length(), tag);
                    context.report(LONG_TAG, node, context.getLocation(node), message);
                }
            }
        }
    }

    @Nullable
    private static String findLiteralValue(@NonNull JavaContext context, @NonNull Node argument) {
        if (argument instanceof StringLiteral) {
            return ((StringLiteral)argument).astValue();
        } else {
            ResolvedNode resolved = context.resolve(argument);
            if (resolved instanceof ResolvedField) {
                ResolvedField field = (ResolvedField) resolved;
                Object value = field.getValue();
                if (value instanceof String) {
                    return (String)value;
                }
            }
        }

        return null;
    }

    private static boolean checkWithinConditional(
            @NonNull JavaContext context,
            @Nullable Node curr,
            @NonNull MethodInvocation logCall) {
        while (curr != null) {
            if (curr instanceof If) {
                If ifNode = (If) curr;
                if (ifNode.astCondition() instanceof MethodInvocation) {
                    MethodInvocation call = (MethodInvocation) ifNode.astCondition();
                    if (IS_LOGGABLE.equals(call.astName().astValue())) {
                        checkTagConsistent(context, logCall, call);
                    }
                }

                return true;
            } else if (curr instanceof MethodInvocation
                    || curr instanceof ClassDeclaration) { // static block
                break;
            }
            curr = curr.getParent();
        }
        return false;
    }

    /** Checks that the tag passed to Log.s and Log.isLoggable match */
    private static void checkTagConsistent(JavaContext context, MethodInvocation logCall,
            MethodInvocation call) {
        Expression isLoggableTag = call.astArguments().first();
        Expression logTag = logCall.astArguments().first();
        if (logTag != null) {
            ResolvedNode resolved1 = context.resolve(isLoggableTag);
            ResolvedNode resolved2 = context.resolve(logTag);
            boolean differ;
            if (resolved1 != null && resolved2 != null) {
                differ = !resolved1.equals(resolved2);
            } else {
                differ = !isLoggableTag.toString().equals(logTag.toString());
            }
            if (differ && context.isEnabled(WRONG_TAG)) {
                Location location = context.getLocation(logTag);
                Location alternate = context.getLocation(isLoggableTag);
                alternate.setMessage("Conflicting tag");
                location.setSecondary(alternate);
                String logCallName = call.astName().astValue();
                String isLoggableDescription = resolved1 != null ? resolved1
                        .getName()
                        : isLoggableTag.toString();
                String logCallDescription = resolved2 != null ? resolved2.getName()
                        : logTag.toString();
                String message = String.format(
                        "Mismatched tags: the %1$s() and isLoggable() calls typically " +
                                "should pass the same tag: %2$s versus %3$s",
                        logCallName,
                        isLoggableDescription,
                        logCallDescription);
                context.report(WRONG_TAG, call, location, message);
            }
        }
    }
}
