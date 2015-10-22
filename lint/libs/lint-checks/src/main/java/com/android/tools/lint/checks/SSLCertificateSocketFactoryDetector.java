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
import com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.client.api.JavaParser.TypeDescriptor;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.Expression;
import lombok.ast.Identifier;
import lombok.ast.MethodInvocation;
import lombok.ast.StrictListAccessor;

public class SSLCertificateSocketFactoryDetector extends Detector
        implements Detector.JavaScanner {

    private static final Implementation IMPLEMENTATION_JAVA = new Implementation(
            SSLCertificateSocketFactoryDetector.class,
            Scope.JAVA_FILE_SCOPE);

    public static final Issue CREATESOCKET = Issue.create(
            "SSLCertificateSocketFactoryCreateSocket", //$NON-NLS-1$
            "Insecure call to `SSLCertificateSocketFactory.createSocket()`",
            "When `SSLCertificateSocketFactory.createSocket()` is called with an InetAddress " +
            "as the first parameter, TLS/SSL hostname verification is not performed, which " +
            "could result in insecure network traffic caused by trusting arbitrary " +
            "hostnames in TLS/SSL certificates presented by peers.",
            Category.SECURITY,
            6,
            Severity.WARNING,
            IMPLEMENTATION_JAVA);

    public static final Issue GETINSECURE = Issue.create(
            "SSLCertificateSocketFactoryGetInsecure", //$NON-NLS-1$
            "Call to `SSLCertificateSocketFactory.getInsecure()`",
            "The `SSLCertificateSocketFactory.getInstance()` method returns " +
            "an SSLSocketFactory with all TLS/SSL security checks disabled, which " +
            "could result in insecure network traffic caused by trusting arbitrary " +
            "TLS/SSL certificates presented by peers.",
            Category.SECURITY,
            6,
            Severity.WARNING,
            IMPLEMENTATION_JAVA);

    private static final String INETADDRESS_CLASS =
            "java.net.InetAddress";

    private static final String SSLCERTIFICATESOCKETFACTORY_CLASS =
            "android.net.SSLCertificateSocketFactory";

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements Detector.JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        List<String> values = new ArrayList<String>(2);
        // Detect calls to potentially insecure SSLCertificateSocketFactory methods
        values.add("createSocket"); //$NON-NLS-1$
        values.add("getInsecure"); //$NON-NLS-1$
        return values;
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        ResolvedNode resolved = context.resolve(node);
        if (resolved instanceof ResolvedMethod) {
            String methodName = node.astName().astValue();
            ResolvedClass resolvedClass = ((ResolvedMethod) resolved).getContainingClass();
            if (resolvedClass.isSubclassOf(SSLCERTIFICATESOCKETFACTORY_CLASS, false)) {
                if ("createSocket".equals(methodName)) {
                    StrictListAccessor<Expression, MethodInvocation> argumentList =
                            node.astArguments();
                    if (argumentList != null) {
                        TypeDescriptor firstParameterType = context.getType(argumentList.first());
                        if (firstParameterType != null) {
                            ResolvedClass firstParameterClass = firstParameterType.getTypeClass();
                            if (firstParameterClass != null &&
                                    firstParameterClass.isSubclassOf(INETADDRESS_CLASS, false)) {
                                context.report(CREATESOCKET, node, context.getLocation(node),
                                        "Use of `SSLCertificateSocketFactory.createSocket()` " +
                                        "with an InetAddress parameter can cause insecure " +
                                        "network traffic due to trusting arbitrary hostnames in " +
                                        "TLS/SSL certificates presented by peers");
                            }
                        }
                    }
                } else if ("getInsecure".equals(methodName)) {
                    context.report(GETINSECURE, node, context.getLocation(node),
                            "Use of `SSLCertificateSocketFactory.getInsecure()` can cause " +
                            "insecure network traffic due to trusting arbitrary TLS/SSL " +
                            "certificates presented by peers");
                }
            }
        }
    }
}
