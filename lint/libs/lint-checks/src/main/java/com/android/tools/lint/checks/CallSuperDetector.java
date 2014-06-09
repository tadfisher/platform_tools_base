/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import static com.android.tools.lint.client.api.JavaParser.ResolvedNode;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.ast.AstVisitor;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Super;

/**
 * Makes sure that methods call super when overriding methods
 */
public class CallSuperDetector extends Detector implements Detector.JavaScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            CallSuperDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Missing call to super */
    public static final Issue ISSUE = Issue.create(
            "MissingSuperCall", //$NON-NLS-1$
            "Missing Super Call",
            "Looks for overriding methods that should also invoke the parent method",

            "Some methods, such as `View#onDetachedFromWindow`, require that you also " +
            "call the super implementation as part of your method.",

            Category.CORRECTNESS,
            9,
            Severity.WARNING,
            IMPLEMENTATION);

    static final String BUNDLE = "android.os.Bundle";   //$NON-NLS-1$
    static final String CONFIGURATION = "android.content.res.Configuration";   //$NON-NLS-1$
    static final String ACTIVITY = "android.app.Activity";   //$NON-NLS-1$

    private static class ApplicableMethod {

        // View
        static final ApplicableMethod ON_DETACHED_FROM_WINDOW = new ApplicableMethod("onDetachedFromWindow");   //$NON-NLS-1$
        static final ApplicableMethod ON_CANCEL_PENDING_INPUT_EVENTS = new ApplicableMethod("onCancelPendingInputEvents");   //$NON-NLS-1$

        // Activity
        static final ApplicableMethod ON_CREATE= new ApplicableMethod("onCreate",   //$NON-NLS-1$
                                                                       new String[] {BUNDLE});
        static final ApplicableMethod ON_START = new ApplicableMethod("onStart");   //$NON-NLS-1$
        static final ApplicableMethod ON_RESTART = new ApplicableMethod("onRestart");   //$NON-NLS-1$
        static final ApplicableMethod ON_RESUME = new ApplicableMethod("onResume");   //$NON-NLS-1$
        static final ApplicableMethod ON_POST_RESUME = new ApplicableMethod("onPostResume");   //$NON-NLS-1$
        static final ApplicableMethod ON_PAUSE = new ApplicableMethod("onPause");   //$NON-NLS-1$
        static final ApplicableMethod ON_STOP = new ApplicableMethod("onStop");   //$NON-NLS-1$
        static final ApplicableMethod ON_POST_CREATE = new ApplicableMethod("onPostCreate",   //$NON-NLS-1$
                                                                            new String[] {BUNDLE});
        static final ApplicableMethod ON_DESTROY = new ApplicableMethod("onDestroy");   //$NON-NLS-1$
        static final ApplicableMethod ON_CONFIGURATION_CHANGED = new ApplicableMethod("onConfigurationChanged",   //$NON-NLS-1$
                                                                                      new String[] {CONFIGURATION});

        // Fragment
        static final ApplicableMethod ON_ATTACH = new ApplicableMethod("onAttach",
                                                                       new String[] {ACTIVITY});   //$NON-NLS-1$
        static final ApplicableMethod ON_DETACH = new ApplicableMethod("onDetach");   //$NON-NLS-1$
        static final ApplicableMethod ON_VIEW_STATE_RESTORED = new ApplicableMethod("onViewStateRestored",   //$NON-NLS-1$
                                                                                    new String[] {BUNDLE});
        static final ApplicableMethod ON_ACTIVITY_CREATED = new ApplicableMethod("onActivityCreated",
                                                                                 new String[] {BUNDLE});   //$NON-NLS-1$
        static final ApplicableMethod ON_DESTROY_VIEW = new ApplicableMethod("onDestroyView");   //$NON-NLS-1$

        static final List<ApplicableMethod> ALL = Arrays.asList(
                ON_DETACHED_FROM_WINDOW,
                ON_CANCEL_PENDING_INPUT_EVENTS,
                ON_CREATE,
                ON_START,
                ON_RESTART,
                ON_RESUME,
                ON_POST_RESUME,
                ON_PAUSE,
                ON_STOP,
                ON_POST_CREATE,
                ON_DESTROY,
                ON_CONFIGURATION_CHANGED,
                ON_ATTACH,
                ON_DETACH,
                ON_VIEW_STATE_RESTORED,
                ON_ACTIVITY_CREATED,
                ON_DESTROY_VIEW
        );

        final String name;
        final String[] arguments;

        private ApplicableMethod(String name, String[] arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        private ApplicableMethod(String name) {
            this(name, new String[0]);
        }

        public boolean isAppricable(String name, ResolvedMethod resolved) {
            if (!this.name.equals(name)) {
                return false;
            }

            int size = resolved.getArgumentCount();
            if (size != arguments.length) {
                return false;
            }

            for (int i=0; i < size; i++) {
                if (!resolved.getArgumentType(i).matchesName(arguments[i])) {
                    return false;
                }
            }

            return true;
        }
    }

    /** Constructs a new {@link CallSuperDetector} check */
    public CallSuperDetector() {
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList(
            ApplicableMethod.ON_DETACHED_FROM_WINDOW.name,
            ApplicableMethod.ON_CANCEL_PENDING_INPUT_EVENTS.name,
            ApplicableMethod.ON_CREATE.name,
            ApplicableMethod.ON_START.name,
            ApplicableMethod.ON_RESTART.name,
            ApplicableMethod.ON_RESUME.name,
            ApplicableMethod.ON_POST_RESUME.name,
            ApplicableMethod.ON_PAUSE.name,
            ApplicableMethod.ON_STOP.name,
            ApplicableMethod.ON_POST_CREATE.name,
            ApplicableMethod.ON_DESTROY.name,
            ApplicableMethod.ON_CONFIGURATION_CHANGED.name,
            ApplicableMethod.ON_ATTACH.name,
            ApplicableMethod.ON_DETACH.name,
            ApplicableMethod.ON_VIEW_STATE_RESTORED.name,
            ApplicableMethod.ON_ACTIVITY_CREATED.name,
            ApplicableMethod.ON_DESTROY_VIEW.name
        );
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements JavaScanner ----

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return Collections.<Class<? extends Node>>singletonList(MethodDeclaration.class);
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return new PerformanceVisitor(context);
    }

    private static class PerformanceVisitor extends ForwardingAstVisitor {
        private final JavaContext mContext;

        public PerformanceVisitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitMethodDeclaration(MethodDeclaration node) {
            for (ApplicableMethod method : ApplicableMethod.ALL) {
                String name = node.astMethodName().astValue();

                // Ignore if the method doesn't fit our description.
                ResolvedNode resolved = mContext.resolve(node);
                if (!(resolved instanceof ResolvedMethod)) {
                    continue;
                }

                if (method.isAppricable(name, (ResolvedMethod)resolved)) {
                    if (!callsSuper(node, name)) {
                        String message = "Overriding method should call super."
                                + name;
                        Location location = mContext.getLocation(node.astMethodName());
                        mContext.report(ISSUE, node, location, message, null);
                    }
                }
            }
            return super.visitMethodDeclaration(node);
        }

        private boolean callsSuper(MethodDeclaration node, final String methodName) {
            final AtomicBoolean result = new AtomicBoolean();
            node.accept(new ForwardingAstVisitor() {
                @Override
                public boolean visitMethodInvocation(MethodInvocation node) {
                    if (node.astName().astValue().equals(methodName) &&
                            node.astOperand() instanceof Super) {
                        result.set(true);
                    }
                    return super.visitMethodInvocation(node);
                }
            });

            return result.get();
        }
    }
}
