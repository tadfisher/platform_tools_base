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
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.Collection;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PERMISSION;
import static com.android.xml.AndroidManifest.NODE_ACTION;
import static com.android.xml.AndroidManifest.NODE_CATEGORY;
import static com.android.xml.AndroidManifest.NODE_INTENT;
import static com.android.xml.AndroidManifest.NODE_MANIFEST;
import static com.android.xml.AndroidManifest.NODE_METADATA;
import static com.android.xml.AndroidManifest.NODE_SERVICE;
import static com.android.xml.AndroidManifest.NODE_USES_FEATURE;
import static com.android.xml.AndroidManifest.NODE_USES_LIBRARY;
import static com.android.xml.AndroidManifest.NODE_USES_PERMISSION;

/**
 * Detector for Android Wear issues.
 */
public class WearDetector extends ResourceXmlDetector implements Detector.XmlScanner {
    public static final Implementation IMPL = new Implementation(
        WearDetector.class,
        Scope.MANIFEST_SCOPE);

    /** Watch Face Missing Permissions */
    public static final Issue WEAR_APP_WATCH_FACE_MISSING_PERMISSIONS_ISSUE = Issue.create(
        "WearAppWatchFaceMissingPermissions", //$NON-NLS-1$
        "Watch face missing permissions",
        "The wear app manifest must have the following permission tags:\n" + "" +
        "<uses-permission android:name=\"com.google.android.permission" +
        ".PROVIDE_BACKGROUND\" />\n" +
        "<uses-permission android:name=\"android.permission.WAKE_LOCK\" />",
        Category.CORRECTNESS,
        7,
        Severity.ERROR,
        IMPL).addMoreInfo(
        "https://developer.android.com/training/wearables/watch-faces/service.html");

    /** Watch Face Missing Service */
    public static final Issue WEAR_APP_WATCH_FACE_MISSING_SERVICE_ISSUE = Issue.create(
        "WearAppWatchFaceMissingService", //$NON-NLS-1$
        "Watch face missing watch face service with correct intent",
        "The watch face service must have the following intent filter:\n" +
        "`<intent-filter>\n" +
        "  <action android:name=\"android.service.wallpaper" +
        ".WallpaperService\" />\n" +
        "  <category android:name=\"com.google.android.wearable.watchface" +
        ".category.WATCH_FACE\" />\n" +
        "</intent-filter>`",
        Category.CORRECTNESS,
        7,
        Severity.ERROR,
        IMPL).addMoreInfo(
        "https://developer.android.com/training/wearables/watch-faces/service.html");

    /** Deprecated Watch Face Category */
    public static final Issue WEAR_APP_DEPRECATED_WATCH_FACE_ISSUE = Issue.create(
        "WearAppDeprecatedWatchFace", //$NON-NLS-1$
        "Watch face using deprecated API",
        "The watch face service is using a deprecated API. Please use the `com.google.android" +
        ".wearable.watchface.category.WATCH_FACE` category instead.",
        Category.CORRECTNESS,
        8,
        Severity.ERROR,
        IMPL).addMoreInfo(
        "https://developer.android.com/training/wearables/watch-faces/service.html");

    private static final String WEAR_LIB = "com.google.android.wearable"; //$NON-NLS-1$
    private static final String WEAR_LIB_WEARABLE_ARTIFACT = WEAR_LIB + ":wearable"; //$NON-NLS-1$
    private static final String WEAR_METADATA_NAME = "com.google.android.wearable.beta.app";
        //$NON-NLS-1$
    public static final String PERMISSION_PROVIDE_BACKGROUND =
        "com.google.android.permission.PROVIDE_BACKGROUND"; //$NON-NLS-1$
    public static final String PERMISSION_WAKE_LOCK = "android.permission.WAKE_LOCK"; //$NON-NLS-1$
    private static final String PERMISSION_BIND_WALLPAPER = "android.permission.BIND_WALLPAPER";
        //$NON-NLS-1$
    private static final String ACTION_WALLPAPER_SERVICE =
        "android.service.wallpaper.WallpaperService"; //$NON-NLS-1$
    public static final String CATEGORY_WATCH_FACE =
        "com.google.android.wearable.watchface.category.WATCH_FACE"; //$NON-NLS-1$
    private static final String WEAR_METADATA_PREVIEW =
        "com.google.android.wearable.watchface.preview"; //$NON-NLS-1$
    private static final String WEAR_METADATA_PREVIEW_CIRCULAR =
        "com.google.android.wearable.watchface.preview_circular"; //$NON-NLS-1$
    private static final String CATEGORY_HOME_BACKGROUND =
        "com.google.android.clockwork.home.category.HOME_BACKGROUND"; //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_WATCH = "android.hardware.type.watch";
        //$NON-NLS-1$

    /**
     * Used for {@link #WEAR_APP_WATCH_FACE_MISSING_PERMISSIONS_ISSUE}.
     */
    private boolean mHasUsesPermissionProvideBackground;
    private boolean mHasUsesPermissionWakeLock;

    /**
     * Used for {@link #WEAR_APP_WATCH_FACE_MISSING_SERVICE_ISSUE}.
     */
    private boolean mHasWatchFaceService;

    /**
     * Records setting from triggers for detecting wear apps and watch faces.
     */
    private boolean mIsWearApp;
    private boolean mHasWatchFace;

    private boolean mHasWearDependency;
    private Location mManifestLocation;

    /** Constructs a new {@link WearDetector} */
    public WearDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
            NODE_USES_PERMISSION,
            NODE_CATEGORY,
            NODE_METADATA,
            NODE_USES_LIBRARY,
            NODE_USES_FEATURE,
            NODE_ACTION,
            NODE_SERVICE,
            NODE_MANIFEST
        );
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        if (context.getPhase() != 1) {
            return;
        }

        mHasUsesPermissionProvideBackground = false;
        mHasUsesPermissionWakeLock = false;
        mHasWatchFaceService = false;
        mHasWatchFace = false;
        mManifestLocation = null;

        // Trigger: Check for wear gradle dependencies
        Project mainProject = context.getMainProject();
        if (mainProject.isGradleProject()
            && (Boolean.TRUE.equals(mainProject.dependsOn(WEAR_LIB_WEARABLE_ARTIFACT)))) {
            mHasWearDependency = true;
        }
        mIsWearApp = mHasWearDependency;
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        // Report more issues at the very end of linting
        if (!mIsWearApp || context.getPhase() != 2 || context.getMainProject().isLibrary()) {
            return;
        }

        if (mHasWatchFace && mManifestLocation != null) {
            if (!mHasUsesPermissionWakeLock &&
                context.isEnabled(WEAR_APP_WATCH_FACE_MISSING_PERMISSIONS_ISSUE)) {
                context.report(
                    WEAR_APP_WATCH_FACE_MISSING_PERMISSIONS_ISSUE,
                    mManifestLocation,
                    "Expecting watch face to have permission `" + PERMISSION_WAKE_LOCK + "`.");
            }
            if (!mHasUsesPermissionProvideBackground &&
                context.isEnabled(WEAR_APP_WATCH_FACE_MISSING_PERMISSIONS_ISSUE)) {
                context.report(
                    WEAR_APP_WATCH_FACE_MISSING_PERMISSIONS_ISSUE,
                    mManifestLocation,
                    "Expecting watch face to have permission `" + PERMISSION_PROVIDE_BACKGROUND +
                    "`.");
            }
            if (!mHasWatchFaceService &&
                context.isEnabled(WEAR_APP_WATCH_FACE_MISSING_SERVICE_ISSUE)) {
                context.report(
                    WEAR_APP_WATCH_FACE_MISSING_SERVICE_ISSUE,
                    mManifestLocation,
                    "Expecting existence of watch face service.");
            }
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String elementName = element.getTagName();
        if (context.getPhase() == 1) {
            // Check if the app is a Wear app
            // An app determined to be a Wear app if ANY of the following is true:
            // - Manifest contains `<meta-data android:name="com.google.android.wearable.beta
            // .app"/>`
            // - Manifest contains `<uses-feature android:name="android.hardware.type.watch"/>`
            // - Manifest contains `<uses-library android:name="com.google.android.wearable"/>`
            // We don't want to recheck any issues that we've already checked, so we stop all
            // trigger checks that might request a repeat immediately.

            // We don't know if the app is a Wear app, so check all possibilities with this
            // tag.
            Node elementAttrName =
                element.getAttributes().getNamedItemNS(ANDROID_URI, ATTR_NAME);

            // Kill trigger checks if we already know the app has a watch face.
            if (mHasWatchFace) {
                return;
            }

            // Check all triggers to see if the app has a watch face (and therefore is a wear
            // app)
            // All these triggers require `android:name` tag attributes
            if (elementAttrName != null) {
                if (NODE_ACTION.equals(elementName)) {
                    mHasWatchFace =
                        (ACTION_WALLPAPER_SERVICE.equals(elementAttrName.getNodeValue()));
                } else if (NODE_CATEGORY.equals(elementName)) {
                    mHasWatchFace =
                        (CATEGORY_WATCH_FACE.equals(elementAttrName.getNodeValue()));
                } else if (NODE_METADATA.equals(elementName)) {
                    mHasWatchFace =
                        (WEAR_METADATA_PREVIEW.equals(elementAttrName.getNodeValue()) ||
                         WEAR_METADATA_PREVIEW_CIRCULAR.equals(
                             elementAttrName.getNodeValue()));
                }
            }
            // Watch face implies the wear app. Only called once.
            if (mHasWatchFace) {
                if (!mIsWearApp) {
                    // Ask for another phase if we haven't already from the wear app trigger.
                    context.requestRepeat(this, Scope.MANIFEST_SCOPE);
                }
                mIsWearApp = true;
            }

            if (mIsWearApp) {
                // Kill rest of trigger checks if we already know the app is a wear app.
                return;
            }
            // Check all triggers to see if the app is a Wear app
            if (NODE_USES_FEATURE.equals(elementName)) {
                if (hasWatchSupport(element)) {
                    mIsWearApp = true;
                }
            } else if (NODE_USES_LIBRARY.equals(elementName)) {
                mIsWearApp = (elementAttrName != null &&
                              WEAR_LIB.equals(elementAttrName.getNodeValue()));
            } else if (NODE_METADATA.equals(elementName)) {
                mIsWearApp = (elementAttrName != null &&
                              WEAR_METADATA_NAME.equals(elementAttrName.getNodeValue()));
            }

            // If we discover the app is a Wear app, request next phase. Only called once.
            if (mIsWearApp) {
                context.requestRepeat(this, Scope.MANIFEST_SCOPE);
            }
        } else if (context.getPhase() == 2) {
            assert mIsWearApp;

            if (!mHasWatchFace) {
                return;
            }
            if (NODE_MANIFEST.equals(elementName)) {
                mManifestLocation = context.getLocation(element);
            } else if (NODE_USES_PERMISSION.equals(elementName)) {
                // <uses-permission>
                Attr name = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
                if (name != null) {
                    String nameValue = name.getValue();
                    if (PERMISSION_PROVIDE_BACKGROUND.equals(nameValue)) {
                        mHasUsesPermissionProvideBackground = true;
                    } else if (PERMISSION_WAKE_LOCK.equals(nameValue)) {
                        mHasUsesPermissionWakeLock = true;
                    }
                }
            } else if (NODE_SERVICE.equals(elementName)) {
                // <service>
                String permission = element.getAttributeNS(ANDROID_URI, ATTR_PERMISSION);
                if (PERMISSION_BIND_WALLPAPER.equals(permission)) {
                    NodeList serviceChildren = element.getChildNodes();
                    for (int i = 0; i < serviceChildren.getLength(); i++) {
                        Node serviceChild = serviceChildren.item(i);
                        if (NODE_INTENT.equals(serviceChild.getNodeName())) {
                            NodeList intentChildren = serviceChild.getChildNodes();

                            // Check for both
                            // <action android:name="android.service.wallpaper
                            // .WallpaperService" />
                            // <category android:name= "com.google.android.wearable
                            // .watchface.category.WATCH_FACE" />
                            boolean hasWallpaperServiceAction = false;
                            boolean hasWatchFaceCategory = false;
                            for (Element intentChild : LintUtils.getChildren(serviceChild)) {
                                String intentChildNameAttr =
                                    intentChild.getAttributeNS(ANDROID_URI, ATTR_NAME);
                                String tagName = intentChild.getNodeName();
                                if (NODE_ACTION.equals(tagName) && ACTION_WALLPAPER_SERVICE
                                    .equals(intentChildNameAttr)) {
                                    hasWallpaperServiceAction = true;
                                } else if (NODE_CATEGORY.equals(tagName) &&
                                           CATEGORY_WATCH_FACE.equals(intentChildNameAttr)) {
                                    hasWatchFaceCategory = true;
                                }
                            }

                            if (hasWallpaperServiceAction && hasWatchFaceCategory) {
                                mHasWatchFaceService = true;
                            }
                        }
                    }
                }
            } else if (NODE_CATEGORY.equals(elementName)) {
                // <category>
                String categoryName = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
                if (CATEGORY_HOME_BACKGROUND.equals(categoryName)
                    && context.isEnabled(WEAR_APP_DEPRECATED_WATCH_FACE_ISSUE)) {
                    context.report(
                        WEAR_APP_DEPRECATED_WATCH_FACE_ISSUE,
                        context.getLocation(
                            element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME)),
                        "This is a deprecated API. Use `" + CATEGORY_WATCH_FACE + "` instead.");
                }
            }
        }
    }

    /**
     * Returns <code>true</code> if the element is
     * <code>&lt;uses-feature android:name="android.hardware.type.watch"/&gt;</code>
     *
     * @param element The tag
     * @return If the tag is the watch hardware uses-feature tag
     */
    private boolean hasWatchSupport(Element element) {
        assert NODE_USES_FEATURE.equals(element.getTagName());
        Node usesFeatureName = element.getAttributes().getNamedItemNS(ANDROID_URI, ATTR_NAME);
        return (usesFeatureName != null &&
                HARDWARE_FEATURE_WATCH.equals(usesFeatureName.getNodeValue()));
    }
}

