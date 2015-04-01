/*
 * Copyright (C) 2012 The Android Open Source Project
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
import com.android.builder.model.AndroidArtifact;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.MavenCoordinates;
import com.android.ide.common.repository.ResourceVisibilityLookup;
import com.android.ide.common.resources.ResourceUrl;
import com.android.resources.ResourceType;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector.JavaScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;

import java.util.Collection;

import lombok.ast.AstVisitor;
import lombok.ast.Node;

/**
 * Check which looks for access of private resources.
 */
public class PrivateResourceDetector extends ResourceXmlDetector implements JavaScanner {
    @SuppressWarnings("unchecked")
    private static final Implementation IMPLEMENTATION = new Implementation(
            PrivateResourceDetector.class,
            Scope.JAVA_AND_RESOURCE_FILES,
            Scope.JAVA_FILE_SCOPE,
            Scope.RESOURCE_FILE_SCOPE);

    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "PrivateResource", //$NON-NLS-1$
            "Using private resources",

            "Private resources should not be referenced; the may not be present everywhere, and " +
            "even where they are they may disappear without notice.\n" +
            "\n" +
            "To fix this, copy the resource into your own project instead.",

            Category.CORRECTNESS,
            3,
            Severity.WARNING,
            IMPLEMENTATION);


    /** Constructs a new detector */
    public PrivateResourceDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements JavaScanner ----

    @Override
    public boolean appliesToResourceRefs() {
        return true;
    }

    @Override
    public void visitResourceReference(
            @NonNull JavaContext context,
            @Nullable AstVisitor visitor,
            @NonNull Node node,
            @NonNull String type,
            @NonNull String name,
            boolean isFramework) {
        if (context.getProject().isGradleProject() && !isFramework) {
            Project project = context.getProject();
            if (project.getCurrentVariant() != null) {
                ResourceType resourceType = ResourceType.getEnum(type);
                if (resourceType != null) {
                    AndroidArtifact artifact = project.getCurrentVariant().getMainArtifact();
                    LintClient client = context.getClient();
                    ResourceVisibilityLookup lookup = client.getResourceVisibility().get(artifact);
                    if (lookup.isPrivate(resourceType, name)) {
                        ResourceUrl url = ResourceUrl.create(resourceType, name, false, false);
                        String message = createErrorMessage(url, lookup);
                        context.report(ISSUE, node, context.getLocation(node), message);
                    }
                }
            }
        }
    }

    // ---- Implements XmlScanner ----

    @Override
    public Collection<String> getApplicableAttributes() {
        return ALL;
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String value = attribute.getNodeValue();
        if (context.getProject().isGradleProject()) {
            Project project = context.getProject();
            if (project.getCurrentVariant() != null) {
                ResourceUrl url = ResourceUrl.parse(value);
                if (url != null && !url.framework) {
                    AndroidArtifact artifact = project.getCurrentVariant().getMainArtifact();
                    LintClient client = context.getClient();
                    ResourceVisibilityLookup lookup = client.getResourceVisibility().get(artifact);
                    if (lookup.isPrivate(url)) {
                        String message = createErrorMessage(url, lookup);
                        context.report(ISSUE, attribute, context.getLocation(attribute), message);
                    }
                }
            }
        }
    }

    private static String createErrorMessage(@NonNull ResourceUrl url,
            @NonNull ResourceVisibilityLookup lookup) {
        String libraryName = getLibraryName(url.type, url.name, lookup);
        return String.format("The resource `%1$s` is marked as private in %2$s", url, libraryName);
    }

    /** Pick a suitable name to describe the library defining the private resource */
    @Nullable
    private static String getLibraryName(@NonNull ResourceType type, @NonNull String name,
            @NonNull ResourceVisibilityLookup visibility) {
        AndroidLibrary library = visibility.getPrivateIn(type, name);
        if (library != null) {
            String libraryName = library.getProject();
            if (libraryName != null) {
                return libraryName;
            }
            MavenCoordinates coordinates = library.getResolvedCoordinates();
            if (coordinates != null) {
                return coordinates.getGroupId() + ':' + coordinates.getArtifactId();
            }
        }
        return "the library";
    }
}
