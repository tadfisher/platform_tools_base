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
import com.android.tools.lint.client.api.JavaParser;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.MethodInvocation;

/**
 * Lint checks for the Google Maps Android API v2.
 */
public class GoogleMapDetector extends Detector implements Detector.JavaScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            GoogleMapDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /**
     * Use of deprecated getMap or getStreetViewPanorama call.
     */
    public static final Issue DEPRECATED_GETMAP = Issue.create(
            "GoogleMapDeprecatedCall", //$NON-NLS-1$
            "Use of deprecated Google Maps Android API v2 method",
            "The methods getMap() and getStreetViewPanorama() are deprecated and will be removed soon.\n"
                    + "Implement the OnMapReadyCallback or OnStreetViewPanoramaReadyCallback instead.",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo("https://developers.google.com/maps/documentation/android-api/map#add_map_code");

    private static final String MESSAGE =
            "Replace the call to %s() with an implementation "
                    + "of %s.";

    private static final String CLASS_MAP_FRAGMENT = "com.google.android.gms.maps.MapFragment";

    private static final String CLASS_SUPPORTMAP_FRAGMENT
            = "com.google.android.gms.maps.SupportMapFragment";

    private static final String CLASS_SUPPORTSTREETVIEW_FRAGMENT
            = "com.google.android.gms.maps.SupportStreetViewPanoramaFragment";

    private static final String CLASS_STREETVIEW_FRAGMENT
            = "com.google.android.gms.maps.StreetViewPanoramaFragment";

    private static final String METHOD_GETMAP = "getMap";

    private static final String METHOD_GETSTREETVIEW = "getStreetViewPanorama";


    /**
     * Set of accepted classes from the com.google.android.gms.maps package.
     */
    private static HashSet<String> ACCEPTED_CLASSES = new HashSet(
            Arrays.asList(CLASS_MAP_FRAGMENT, CLASS_SUPPORTMAP_FRAGMENT, CLASS_STREETVIEW_FRAGMENT,
                    CLASS_SUPPORTSTREETVIEW_FRAGMENT));


    public GoogleMapDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    // ---- Implements JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList(
                METHOD_GETMAP,
                METHOD_GETSTREETVIEW
        );
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {

        // Get the method name and resolve its call
        String name = node.astName().astValue();
        JavaParser.ResolvedNode resolved = context.resolve(node);

        if (resolved instanceof JavaParser.ResolvedMethod) {
            // Get the class on which the method was called.
            JavaParser.ResolvedMethod resolvedMethod = (JavaParser.ResolvedMethod) resolved;
            final String className = resolvedMethod.getContainingClass().getName();

            // Only report an issue on one of the accepted classes from Google Play Services.
            if (ACCEPTED_CLASSES.contains(className)) {
                // Assemble the message based on the name of the method and its new replacement.
                String message;
                if (METHOD_GETMAP.equals(name)) {
                    // getMap()
                    message = String.format(MESSAGE, name, "getMapAsync(OnMapReadyCallback)");
                } else {
                    // getStreetViewPanorama()
                    message = String.format(MESSAGE, name,
                            "getStreetViewPanoramaAsync(OnStreetViewPanoramaReadyCallback)");
                }
                context.report(DEPRECATED_GETMAP, context.getLocation(node), message);
            }
        }

    }

}
