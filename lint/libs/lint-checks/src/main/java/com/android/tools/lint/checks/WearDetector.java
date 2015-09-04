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
    public static final Issue MISSING_WATCH_FACE_PERMISSIONS = Issue.create(
        "WatchfaceMissingPermissions", //$NON-NLS-1$
        "Missing necessary permissions for Watch faces",
        "The wear app manifest must have the following permission tags:\n" + "" +
        "`<uses-permission android:name=\"" +
        "com.google.android.permission.PROVIDE_BACKGROUND\" />`\n" +
        "`<uses-permission android:name=\"android.permission.WAKE_LOCK\" />`",
        Category.CORRECTNESS,
        7,
        Severity.ERROR,
        IMPL).addMoreInfo(
        "https://developer.android.com/training/wearables/watch-faces/service.html");

    /** Watch Face Missing Service */
    public static final Issue WATCH_FACE_MISSING_SERVICE_ISSUE = Issue.create(
        "WatchfaceMissingService", //$NON-NLS-1$
        "Missing watch face service with correct intent",
        "The watch face service must have the following intent filter:\n" +
        "`<intent-filter>`\n" +
        "   `<action android:name=\"android.service.wallpaper.WallpaperService\" />`\n" +
        "   `<category android:name=\"" +
        "com.google.android.wearable.watchface.category.WATCH_FACE\" />`\n" +
        "`</intent-filter>`",
        Category.CORRECTNESS,
        7,
        Severity.ERROR,
        IMPL).addMoreInfo(
        "https://developer.android.com/training/wearables/watch-faces/service.html");

    /** Deprecated Watch Face Category */
    public static final Issue DEPRECATED_WATCH_FACE_ISSUE = Issue.create(
        "DeprecatedWatchfaceCategory", //$NON-NLS-1$
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

    private static final String WEAR_METADATA_NAME =
            "com.google.android.wearable.beta.app"; //$NON-NLS-1$

    private static final String PERMISSION_PROVIDE_BACKGROUND =
            "com.google.android.permission.PROVIDE_BACKGROUND"; //$NON-NLS-1$

    private static final String PERMISSION_WAKE_LOCK =
            "android.permission.WAKE_LOCK"; //$NON-NLS-1$

    private static final String PERMISSION_BIND_WALLPAPER =
            "android.permission.BIND_WALLPAPER"; //$NON-NLS-1$

    private static final String ACTION_WALLPAPER_SERVICE =
            "android.service.wallpaper.WallpaperService"; //$NON-NLS-1$

    private static final String CATEGORY_WATCH_FACE =
            "com.google.android.wearable.watchface.category.WATCH_FACE"; //$NON-NLS-1$

    private static final String WEAR_METADATA_PREVIEW =
            "com.google.android.wearable.watchface.preview"; //$NON-NLS-1$

    private static final String WEAR_METADATA_PREVIEW_CIRCULAR =
            "com.google.android.wearable.watchface.preview_circular"; //$NON-NLS-1$

    private static final String CATEGORY_HOME_BACKGROUND =
            "com.google.android.clockwork.home.category.HOME_BACKGROUND"; //$NON-NLS-1$

    private static final String HARDWARE_FEATURE_WATCH =
            "android.hardware.type.watch"; //$NON-NLS-1$

    /**
     * Used for {@link #MISSING_WATCH_FACE_PERMISSIONS}.
     */
    private boolean mHasUsesPermissionProvideBackground;
    private boolean mHasUsesPermissionWakeLock;

    /**
     * Used for {@link #WATCH_FACE_MISSING_SERVICE_ISSUE}.
     */
    private boolean mHasWatchFaceService;

    private boolean mHasWatchFace;

    private Location.Handle mManifestLocationHandle;

    /** Constructs a new {@link WearDetector} */
    public WearDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.SLOW;
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
    public void beforeCheckProject(@NonNull Context context) {
        if (context.getPhase() == 1) {
            mHasUsesPermissionProvideBackground = false;
            mHasUsesPermissionWakeLock = false;
            mHasWatchFaceService = false;
            mHasWatchFace = false;
            mManifestLocationHandle = null;
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        // Report more issues at the very end of linting
        if (!mHasWatchFace || context.getPhase() != 2 || context.getMainProject().isLibrary()) {
            return;
        }

        if (mHasWatchFace && mManifestLocationHandle != null) {

            if (!mHasUsesPermissionWakeLock
                && context.isEnabled(MISSING_WATCH_FACE_PERMISSIONS)) {
                context.report(MISSING_WATCH_FACE_PERMISSIONS,
                    mManifestLocationHandle.resolve(),
                    "Expecting watch face to have permission `" + PERMISSION_WAKE_LOCK + "`.");
            }
            if (!mHasUsesPermissionProvideBackground
                && context.isEnabled(MISSING_WATCH_FACE_PERMISSIONS)) {
                context.report(MISSING_WATCH_FACE_PERMISSIONS,
                    mManifestLocationHandle.resolve(),
                    "Expecting watch face to have permission `" + PERMISSION_PROVIDE_BACKGROUND +
                    "`.");
            }
            if (!mHasWatchFaceService
                && context.isEnabled(WATCH_FACE_MISSING_SERVICE_ISSUE)) {
                context.report(WATCH_FACE_MISSING_SERVICE_ISSUE,
                    mManifestLocationHandle.resolve(),
                    "Expecting existence of watch face service.");
            }
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String elementName = element.getTagName();
        if (context.getPhase() == 1 && !mHasWatchFace) {
            // In the first phase, check if the app has a watch face.
            String nameAttrValue = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
            // Check all triggers to see if the app has a watch face (and therefore is a wear app)
            // All these triggers require `android:name` tag attributes
            if (NODE_ACTION.equals(elementName)) {
                mHasWatchFace = ACTION_WALLPAPER_SERVICE.equals(nameAttrValue);
            } else if (NODE_CATEGORY.equals(elementName)) {
                mHasWatchFace = CATEGORY_WATCH_FACE.equals(nameAttrValue);
            } else if (NODE_METADATA.equals(elementName)) {
                mHasWatchFace = WEAR_METADATA_PREVIEW.equals(nameAttrValue)
                  || WEAR_METADATA_PREVIEW_CIRCULAR.equals(nameAttrValue);
            }

            if (mHasWatchFace) {
                context.requestRepeat(this, Scope.MANIFEST_SCOPE);
            }

        } else if (context.getPhase() == 2 && mHasWatchFace) {
            if (NODE_MANIFEST.equals(elementName)
                    && context.getMainProject() == context.getProject()) {
                // We display the errors at the manifest element level.
                // To allow for developers to suppress the issues, we check and disable any
                // reporting that occurs due to a particular issue.
                if (context.getDriver().isSuppressed(
                        context, MISSING_WATCH_FACE_PERMISSIONS, element)) {
                    mHasUsesPermissionWakeLock = true;
                    mHasUsesPermissionProvideBackground = true;
                }
                if (context.getDriver().isSuppressed(
                        context, WATCH_FACE_MISSING_SERVICE_ISSUE, element)) {
                    mHasWatchFaceService = true;
                }
                mManifestLocationHandle = context.createLocationHandle(element);
            } else if (NODE_USES_PERMISSION.equals(elementName)) {
                // <uses-permission>
                String nameValue = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
                if (PERMISSION_PROVIDE_BACKGROUND.equals(nameValue)) {
                    mHasUsesPermissionProvideBackground = true;
                } else if (PERMISSION_WAKE_LOCK.equals(nameValue)) {
                    mHasUsesPermissionWakeLock = true;
                }
            } else if (NODE_SERVICE.equals(elementName)) {
                // <service>
                String permission = element.getAttributeNS(ANDROID_URI, ATTR_PERMISSION);
                if (PERMISSION_BIND_WALLPAPER.equals(permission)) {
                    for (Node serviceChild : LintUtils.getChildren(element)) {
                        if (NODE_INTENT.equals(serviceChild.getNodeName())) {
                            // Check for both
                            // <action android:name="android.service.wallpaper.WallpaperService" />
                            // <category android:name=
                            // "com.google.android.wearable.watchface.category.WATCH_FACE" />
                            boolean hasWallpaperServiceAction = false;
                            boolean hasWatchFaceCategory = false;
                            for (Element filterChild : LintUtils.getChildren(serviceChild)) {
                                String filterChildName =
                                    filterChild.getAttributeNS(ANDROID_URI, ATTR_NAME);
                                String filterTag = filterChild.getNodeName();
                                if (NODE_ACTION.equals(filterTag)
                                        && ACTION_WALLPAPER_SERVICE.equals(filterChildName)) {
                                    hasWallpaperServiceAction = true;
                                } else if (NODE_CATEGORY.equals(filterTag)
                                        && CATEGORY_WATCH_FACE.equals(filterChildName)) {
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
                Attr categoryAttr = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
                if (categoryAttr != null
                    && CATEGORY_HOME_BACKGROUND.equals(categoryAttr.getValue())
                    && context.isEnabled(DEPRECATED_WATCH_FACE_ISSUE)) {
                    context.report(DEPRECATED_WATCH_FACE_ISSUE, categoryAttr,
                            context.getLocation(categoryAttr),
                            "This is a deprecated API. Use `" + CATEGORY_WATCH_FACE + "` instead.");
                }
            }
        }
    }

    // Used by lint inspections.
    @SuppressWarnings("unused")
    @NonNull
    public static String[] getRequiredWatchfacePermissions() {
        return new String[]{PERMISSION_WAKE_LOCK, PERMISSION_PROVIDE_BACKGROUND};
    }

    @SuppressWarnings("unused")
    public static String getDeprecatedWatchFaceCategory() {
        return CATEGORY_HOME_BACKGROUND;
    }

    @SuppressWarnings("unused")
    public static String getCategoryWatchFace() {
        return CATEGORY_WATCH_FACE;
    }
}
