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
import com.google.common.collect.ImmutableMap;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.PREFIX_ANDROID;
import static com.android.xml.AndroidManifest.ATTRIBUTE_REQUIRED;
import static com.android.xml.AndroidManifest.NODE_ACTIVITY;
import static com.android.xml.AndroidManifest.NODE_APPLICATION;
import static com.android.xml.AndroidManifest.NODE_CATEGORY;
import static com.android.xml.AndroidManifest.NODE_INTENT;
import static com.android.xml.AndroidManifest.NODE_MANIFEST;
import static com.android.xml.AndroidManifest.NODE_USES_FEATURE;
import static com.android.xml.AndroidManifest.NODE_USES_PERMISSION;

/**
 * Detects various issues for Android TV.
 * <p/>
 * First detects if the app is a TV app, then scans for issues.
 */
public class TVDetector extends ResourceXmlDetector implements Detector.XmlScanner {
    private static final Implementation IMPLEMENTATION = new Implementation(
        TVDetector.class, Scope.MANIFEST_SCOPE);

    private static final String LEANBACK_LIB_ARTIFACT = "com.android.support:leanback-v17";
        //$NON-NLS-1$
    private static final String CATEGORY_LEANBACK_LAUNCHER =
        "android.intent.category.LEANBACK_LAUNCHER"; //$NON-NLS-1$

    // Features
    private static final String HARDWARE_FEATURE_TOUCHSCREEN = "android.hardware.touchscreen";
        //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_FAKETOUCH = "android.hardware.faketouch";
        //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_TELEPHONY = "android.hardware.telephony";
        //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_CAMERA = "android.hardware.camera"; //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_BLUETOOTH = "android.hardware.bluetooth";
        //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_BLUETOOTHLE = "android.hardware.bluetooth_le";
        //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_NFC = "android.hardware.nfc"; //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_LOCATION = "android.hardware.location";
        //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_LOCATION_GPS = "android.hardware.location.gps";
        //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_MICROPHONE = "android.hardware.microphone";
        //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_SENSORS = "android.hardware.sensors"; //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_SCREEN_PORTRAIT =
        "android.hardware.screen.portrait"; //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_INFRARED = "android.hardware.consumerir";
        //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_DEPRECATED_GOOGLE_ANDROID_TV =
        "com.google.android.tv"; //$NON-NLS-1$
    private static final String HARDWARE_FEATURE_DEPRECATED_ANDROID_TYPE_TELEVISION =
        "android.hardware.type.television"; //$NON-NLS-1$
    public static final String SOFTWARE_FEATURE_LEANBACK = "android.software.leanback";
        //$NON-NLS-1$

    // Permissions
    private static final String PERMISSION_BLUETOOTH = "android.permission.BLUETOOTH"; //$NON-NLS-1$
    private static final String PERMISSION_BLUETOOTH_ADMIN = "android.permission.BLUETOOTH_ADMIN";
        //$NON-NLS-1$
    private static final String PERMISSION_CAMERA = "android.permission.CAMERA"; //$NON-NLS-1$
    private static final String PERMISSION_ACCESS_MOCK_LOCATION =
        "android.permission.ACCESS_MOCK_LOCATION"; //$NON-NLS-1$
    private static final String PERMISSION_ACCESS_LOCATION_EXTRA_COMMANDS =
        "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS"; //$NON-NLS-1$
    private static final String PERMISSION_INSTALL_LOCATION_PROVIDER =
        "android.permission.INSTALL_LOCATION_PROVIDER"; //$NON-NLS-1$
    private static final String PERMISSION_ACCESS_FINE_LOCATION =
        "android.permission.ACCESS_FINE_LOCATION"; //$NON-NLS-1$
    private static final String PERMISSION_RECORD_AUDIO = "android.permission.RECORD_AUDIO";
        //$NON-NLS-1$
    private static final String PERMISSION_CALL_PHONE = "android.permission.CALL_PHONE";
        //$NON-NLS-1$
    private static final String PERMISSION_CALL_PRIVILEGED = "android.permission.CALL_PRIVILEGED";
        //$NON-NLS-1$
    private static final String PERMISSION_PROCESS_OUTGOING_CALLS =
        "android.permission.PROCESS_OUTGOING_CALLS"; //$NON-NLS-1$
    private static final String PERMISSION_READ_SMS = "android.permission.READ_SMS"; //$NON-NLS-1$
    private static final String PERMISSION_RECEIVE_SMS = "android.permission.RECEIVE_SMS";
        //$NON-NLS-1$
    private static final String PERMISSION_RECEIVE_MMS = "android.permission.RECEIVE_MMS";
        //$NON-NLS-1$
    private static final String PERMISSION_RECEIVE_WAP_PUSH = "android.permission.RECEIVE_WAP_PUSH";
        //$NON-NLS-1$
    private static final String PERMISSION_SEND_SMS = "android.permission.SEND_SMS"; //$NON-NLS-1$
    private static final String PERMISSION_WRITE_APN_SETTINGS =
        "android.permission.WRITE_APN_SETTINGS"; //$NON-NLS-1$
    private static final String PERMISSION_WRITE_SMS = "android.permission.WRITE_SMS"; //$NON-NLS-1$

    /** Using hardware unsupported by TV */
    public static final Issue UNSUPPORTED_TV_HARDWARE = Issue.create(
        "UnsupportedTVHardware", //$NON-NLS-1$
        "Unsupported TV Hardware Feature",
        "The <" + NODE_USES_FEATURE +
        "> element should not require this unsupported hardware feature.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        IMPLEMENTATION).addMoreInfo(
        "https://developer.android.com/training/tv/start/hardware.html#unsupported-features");

    /** Using deprecated TV hardware */
    public static final Issue DEPRECATED_TV_HARDWARE = Issue.create(
        "DeprecatedTVHardware", //$NON-NLS-1$
        "Deprecated TV Hardware Name",
        "The <" + NODE_USES_FEATURE + "> elements are deprecated.",
        Category.CORRECTNESS,
        6,
        Severity.ERROR,
        IMPLEMENTATION);

    /** Missing leanback launcher intent filter */
    public static final Issue MISSING_LEANBACK_LAUNCHER = Issue.create(
        "MissingLeanbackLauncher", //$NON-NLS-1$
        "TV Missing Leanback Launcher Intent Filter.",
        "The application should have an activity with a leanback launcher intent filter.",
        Category.CORRECTNESS,
        8,
        Severity.ERROR,
        IMPLEMENTATION)
        .addMoreInfo("https://developer.android.com/training/tv/start/start.html#tv-activity");

    /** Missing leanback support */
    public static final Issue MISSING_LEANBACK_SUPPORT = Issue.create(
        "MissingLeanbackSupport", //$NON-NLS-1$
        "TV Missing Leanback Support.",
        "The manifest should declare the use of the Leanback user interface.",
        Category.CORRECTNESS,
        8,
        Severity.ERROR,
        IMPLEMENTATION)
        .addMoreInfo("https://developer.android.com/training/tv/start/start.html#leanback-req");

    /** Permission implies required hardware unsupported by TV */
    public static final Issue PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE = Issue.create(
        "PermissionImpliesUnsupportedHardware", //$NON-NLS-1$
        "TV Permission Implies Unsupported Hardware",
        "The <" +
        NODE_USES_PERMISSION +
        "> element should not require a permission that implies an unsupported hardware feature.",
        Category.CORRECTNESS,
        3,
        Severity.WARNING,
        IMPLEMENTATION).addMoreInfo(
        "http://developer.android.com/guide/topics/manifest/uses-feature-element" +
        ".html#permissions");

    /** Missing banner */
    public static final Issue MISSING_BANNER = Issue.create(
        "MissingBanner", //$NON-NLS-1$
        "TV Missing Banner",
        "The TV app must provide a home screen banner for each localization.",
        Category.CORRECTNESS,
        5,
        Severity.WARNING,
        IMPLEMENTATION).addMoreInfo(
        "http://developer.android.com/training/tv/start/start.html#banner");

    private static final Set<String> UNSUPPORTED_HARDWARE_FEATURES = new HashSet<String>(
        Arrays.asList(
            HARDWARE_FEATURE_TOUCHSCREEN,
            HARDWARE_FEATURE_FAKETOUCH,
            HARDWARE_FEATURE_TELEPHONY,
            HARDWARE_FEATURE_CAMERA,
            HARDWARE_FEATURE_BLUETOOTH,
            HARDWARE_FEATURE_BLUETOOTHLE,
            HARDWARE_FEATURE_NFC,
            HARDWARE_FEATURE_LOCATION_GPS,
            HARDWARE_FEATURE_MICROPHONE,
            HARDWARE_FEATURE_SENSORS,
            HARDWARE_FEATURE_SCREEN_PORTRAIT,
            HARDWARE_FEATURE_INFRARED));

    private static final Set<String> DEPRECATED_HARDWARE_FEATURES = new HashSet<String>(
        Arrays.asList(
            HARDWARE_FEATURE_DEPRECATED_GOOGLE_ANDROID_TV,
            HARDWARE_FEATURE_DEPRECATED_ANDROID_TYPE_TELEVISION));

    public static final Map<String, String> PERMISSIONS_TO_IMPLIED_UNSUPPORTED_HARDWARE =
        ImmutableMap.<String, String>builder()
            .put(PERMISSION_BLUETOOTH, HARDWARE_FEATURE_BLUETOOTH)
            .put(PERMISSION_BLUETOOTH_ADMIN, HARDWARE_FEATURE_BLUETOOTH)

            .put(PERMISSION_CAMERA, HARDWARE_FEATURE_CAMERA)
            .put(PERMISSION_RECORD_AUDIO, HARDWARE_FEATURE_CAMERA)

            .put(PERMISSION_ACCESS_MOCK_LOCATION, HARDWARE_FEATURE_LOCATION)
            .put(PERMISSION_ACCESS_LOCATION_EXTRA_COMMANDS, HARDWARE_FEATURE_LOCATION)
            .put(PERMISSION_INSTALL_LOCATION_PROVIDER, HARDWARE_FEATURE_LOCATION)
            .put(PERMISSION_ACCESS_FINE_LOCATION, HARDWARE_FEATURE_LOCATION)

            .put(PERMISSION_CALL_PHONE, HARDWARE_FEATURE_TELEPHONY)
            .put(PERMISSION_CALL_PRIVILEGED, HARDWARE_FEATURE_TELEPHONY)
            .put(PERMISSION_PROCESS_OUTGOING_CALLS, HARDWARE_FEATURE_TELEPHONY)
            .put(PERMISSION_READ_SMS, HARDWARE_FEATURE_TELEPHONY)
            .put(PERMISSION_RECEIVE_SMS, HARDWARE_FEATURE_TELEPHONY)
            .put(PERMISSION_RECEIVE_MMS, HARDWARE_FEATURE_TELEPHONY)
            .put(PERMISSION_RECEIVE_WAP_PUSH, HARDWARE_FEATURE_TELEPHONY)
            .put(PERMISSION_SEND_SMS, HARDWARE_FEATURE_TELEPHONY)
            .put(PERMISSION_WRITE_APN_SETTINGS, HARDWARE_FEATURE_TELEPHONY)
            .put(PERMISSION_WRITE_SMS, HARDWARE_FEATURE_TELEPHONY).build();

    public static final String ATTR_BANNER = "banner"; //$NON-NLS-1$

    /** Used for {@link #MISSING_LEANBACK_LAUNCHER}. */
    private Location mActivityWithLeanbackLauncherLocation;
    /** Used for {@link #MISSING_LEANBACK_SUPPORT}. */
    private boolean mHasLeanbackSupport;
    /** Used for checking if the app is a TV app. */
    private boolean mIsTVApp;
    private boolean mHasLeanbackDependency;
    /** Used for {@link #MISSING_BANNER}. */
    private boolean mHasApplicationBanner;
    private Location mApplicationLocation;
    private int mNumberLeanbackLauncherActivityLocationsWithoutBanners;
    /** Used for {@link #PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE}. */
    private List<Location> mPotentiallyBadPermissions;
    private Map<String, Location> mPotentiallyBadUsesFeature;
    private Location mManifestLocation;

    /** Constructs a new {@link TVDetector} */
    public TVDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
            NODE_APPLICATION,
            NODE_ACTIVITY,
            NODE_USES_FEATURE,
            NODE_USES_PERMISSION,
            NODE_MANIFEST
        );
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        if (context.getPhase() == 1) {
            // Setup
            mActivityWithLeanbackLauncherLocation = null;
            mHasLeanbackSupport = false;
            mHasApplicationBanner = false;
            mApplicationLocation = null;
            mNumberLeanbackLauncherActivityLocationsWithoutBanners = 0;
            mPotentiallyBadPermissions = new ArrayList<Location>();
            mPotentiallyBadUsesFeature = new HashMap<String, Location>();
            mManifestLocation = null;

            // Check gradle dependency
            Project mainProject = context.getMainProject();
            mHasLeanbackDependency = (mainProject.isGradleProject() && Boolean.TRUE.equals(
                mainProject.dependsOn(LEANBACK_LIB_ARTIFACT)));
            mIsTVApp = mHasLeanbackDependency;
        }
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        if (!context.getMainProject().isLibrary()
            && mHasLeanbackDependency || (mIsTVApp && context.getPhase() == 2)) {
            // Report an error if there's not at least one leanback launcher intent filter activity
            if (mActivityWithLeanbackLauncherLocation == null
                && context.isEnabled(MISSING_LEANBACK_LAUNCHER)
                && mManifestLocation != null) {
                // No launch activity
                context.report(
                    MISSING_LEANBACK_LAUNCHER,
                    mManifestLocation,
                    "Expecting an activity to have `" + CATEGORY_LEANBACK_LAUNCHER +
                    "` intent filter.");
            }

            // Report an issue if there's no leanback <uses-feature> tag.
            if (!mHasLeanbackSupport && context.isEnabled(MISSING_LEANBACK_SUPPORT)
                && mManifestLocation != null) {
                context.report(
                    MISSING_LEANBACK_SUPPORT,
                    mManifestLocation,
                    "Expecting <uses-feature android:name=\"android.software.leanback\" " +
                    "android:required=\"false\" /> tag.");
            }

            // Report missing banners
            if (context.isEnabled(MISSING_BANNER) && !mHasApplicationBanner
                && mNumberLeanbackLauncherActivityLocationsWithoutBanners != 0
                && mApplicationLocation != null) {
                context.report(
                    MISSING_BANNER,
                    mApplicationLocation,
                    "Expecting `android:banner` with the `<application>` tag or each Leanback " +
                    "launcher activity.");
            }

            // Report permissions implying unsupported hardware
            if (context.isEnabled(PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE)) {
                for (Location potentiallyBadPermissionLocation : mPotentiallyBadPermissions) {
                    Element potentiallyBadPermissionElement =
                        (Element)potentiallyBadPermissionLocation.getClientData();
                    assert potentiallyBadPermissionElement != null;
                    String permissionName =
                        potentiallyBadPermissionElement.getAttributeNS(ANDROID_URI, ATTR_NAME);
                    String unsupportedHardwareName =
                        PERMISSIONS_TO_IMPLIED_UNSUPPORTED_HARDWARE.get(permissionName);
                    if (mPotentiallyBadUsesFeature.containsKey(unsupportedHardwareName)) {
                        // Ensure the <uses-feature> tag has required="false"
                        Location usesFeatureLocation =
                            mPotentiallyBadUsesFeature.get(unsupportedHardwareName);
                        Element impliedUsesFeatureTag =
                            (Element)usesFeatureLocation
                                .getClientData();
                        assert impliedUsesFeatureTag != null;
                        String required =
                            impliedUsesFeatureTag.getAttributeNS(ANDROID_URI, ATTRIBUTE_REQUIRED);
                        if (required.isEmpty() || Boolean.parseBoolean(required)) {
                            context.report(
                                PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE,
                                usesFeatureLocation,
                                "Expecting `required=\"false\"` for this hardware that is " +
                                "unsupported by TV.");
                        }
                    } else {
                        // We have a permission that implies a hardware feature not found.
                        context.report(
                            PERMISSION_IMPLIES_UNSUPPORTED_HARDWARE,
                            potentiallyBadPermissionLocation,
                            "Permission exists without corresponding hardware `<uses-feature " +
                            "android:name=\"" +
                            unsupportedHardwareName +
                            "\" required=\"false\">` tag.");
                    }
                }
            }
        }
    }

    /**
     * Visits applicable elements and reports all possible issues found in the manifest file.
     * <p/>
     * Reports the following issues:
     * <ul>
     * <li>Unsupported hardware required</li>
     * <li>Deprecated hardware</li>
     * <li>Not at least one leanback launcher activity</li>
     * <li>Permissions that:
     * <ul>
     * <li>are required</li>
     * <li>imply an unsupported hardware feature</li>
     * </ul>
     * </li>
     * </ul>
     */
    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        // If the app has a leanback gradle dependency, skip the trigger check and check for issues.
        if (mHasLeanbackDependency) {
            checkTVIssues(context, element);
        } else {
            // First check to see if the app is a TV app, if so, check for TV issues
            if (context.getPhase() == 1) {
                checkIfManifestSupportsTV(context, element);
            } else {
                assert context.getPhase() == 2;
                checkTVIssues(context, element);
            }
        }
    }

    /**
     * Check if the app is a TV app.
     * <p/>
     * An app determined to be a TV app if <em>ANY</em> of the following is true:
     * <ul>
     * <li>Manifest contains <code>&lt;uses-feature android:name="android.software.leanback"/&gt;
     * </code></li>
     * <li>Manifest contains a leanback launcher activity</li>
     * <li>Gradle depends on <code>com.android.support:leanback-v17</code></li>
     * </ul>
     * We also don't want to recheck any issues that we've already checked.
     */
    private void checkIfManifestSupportsTV(@NonNull XmlContext context, @NonNull Element element) {
        String elementName = element.getTagName();

        // Kill phase 1 if we already know it's a TV app.
        if (mIsTVApp) {
            return;
        }

        // We don't know if the app is a TV app, so check all possibilities with this tag.
        if (NODE_USES_FEATURE.equals(elementName)) {
            if (hasLeanbackSupport(element)) {
                mHasLeanbackSupport = true;
            }
        } else if (NODE_ACTIVITY.equals(elementName)) {
            if (activityNodeHasLeanbackIntentFilter(element)) {
                mActivityWithLeanbackLauncherLocation = context.getLocation(element);
            }
        }

        // If we discover the app is a TV app, request next phase
        mIsTVApp = mIsTVApp
                   || mHasLeanbackSupport
                   || (mActivityWithLeanbackLauncherLocation != null);
        if (mIsTVApp) {
            context.requestRepeat(this, Scope.MANIFEST_SCOPE);
        }
    }

    private void checkTVIssues(@NonNull XmlContext context, @NonNull Element element) {
        assert mIsTVApp;

        String elementName = element.getTagName();
        Location elementLocation = context.getLocation(element);
        elementLocation.setClientData(element);

        if (NODE_MANIFEST.equals(elementName)) {
            mManifestLocation = elementLocation;
        } else if (NODE_APPLICATION.equals(elementName)) {
            mHasApplicationBanner = element.hasAttributeNS(ANDROID_URI, ATTR_BANNER);
            mApplicationLocation = elementLocation;
        } else if (NODE_USES_FEATURE.equals(elementName)) {
            // <uses-feature>

            // Ensures that unsupported hardware features aren't required.
            Attr name = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
            if (name != null) {
                String featureName = name.getValue();
                mPotentiallyBadUsesFeature.put(featureName, elementLocation);
                if (isUnsupportedHardwareFeature(featureName)
                    && context.isEnabled(UNSUPPORTED_TV_HARDWARE)) {
                    Attr required =
                        element.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_REQUIRED);
                    if (required == null || Boolean.parseBoolean(required.getValue())) {
                        Location location =
                            context.getLocation((required == null) ? element : required);
                        context.report(
                            UNSUPPORTED_TV_HARDWARE,
                            location,
                            "Expecting `" +
                            PREFIX_ANDROID +
                            ATTRIBUTE_REQUIRED +
                            "=\"" +
                            false +
                            "\"` for this hardware feature that is unsupported by TV.");
                    }
                } else if (isDeprecatedHardwareFeature(featureName)
                           && context.isEnabled(DEPRECATED_TV_HARDWARE)) {
                    context.report(
                        DEPRECATED_TV_HARDWARE,
                        context.getLocation(name),
                        "`" + featureName + "` is deprecated and should be removed.");
                }
            }

            if (!mHasLeanbackSupport && hasLeanbackSupport(element)) {
                mHasLeanbackSupport = true;
            }
        } else if (NODE_ACTIVITY.equals(elementName)) {
            // <activity>
            if (mActivityWithLeanbackLauncherLocation == null &&
                activityNodeHasLeanbackIntentFilter(element)) {
                mActivityWithLeanbackLauncherLocation = elementLocation;

                // Since this activity has a leanback launcher intent filter,
                // Make sure it has a home screen banner
                if (!element.hasAttributeNS(ANDROID_URI, ATTR_BANNER)) {
                    ++mNumberLeanbackLauncherActivityLocationsWithoutBanners;
                }
            }
        } else if (NODE_USES_PERMISSION.equals(elementName)) {
            // <uses-permission>
            Attr nameNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);

            // Store all <uses-permission> tags that imply unsupported hardware)
            if (nameNode != null
                && PERMISSIONS_TO_IMPLIED_UNSUPPORTED_HARDWARE.containsKey(nameNode.getValue())) {
                elementLocation.setClientData(element);
                mPotentiallyBadPermissions.add(elementLocation);
            }
        }
    }

    /**
     * Returns <code>true</code> if the element is <code>&lt;uses-feature
     * android:name="android.software.leanback"/&gt;</code>
     *
     * @param element The tag
     * @return If the tag is the leanback uses-feature tag
     */
    private boolean hasLeanbackSupport(Element element) {
        assert NODE_USES_FEATURE.equals(element.getTagName());
        return SOFTWARE_FEATURE_LEANBACK.equals(element.getAttributeNS(ANDROID_URI, ATTR_NAME));
    }

    /**
     * Returns <code>true</code> if the <code>featureName</code> is an unsupported hardware feature
     * for Android TV.
     *
     * @param featureName The name attribute of the <code>&lt;uses-feature&gt;</code> tag.
     * @return <code>true</code> if the feature is unsupported.
     */
    private boolean isUnsupportedHardwareFeature(@NonNull String featureName) {
        return containsStringThatStartsWith(featureName, UNSUPPORTED_HARDWARE_FEATURES);
    }

    /**
     * Returns <code>true</code> if the <code>featureName</code> is a deprecated hardware feature
     * for Android TV.
     *
     * @param featureName The name attribute of the <code>&lt;uses-feature&gt;</code> tag.
     * @return <code>true</code> if the feature is unsupported.
     */
    private boolean isDeprecatedHardwareFeature(@NonNull String featureName) {
        return containsStringThatStartsWith(featureName, DEPRECATED_HARDWARE_FEATURES);
    }

    /**
     * Tests if the <code>prefix</code> starts one of the strings in the <code>stringSet</code>.
     *
     * @param prefix    The prefix of a feature
     * @param stringSet The set of strings to test the prefix
     * @return <code>true</code> if the <code>prefix</code> starts any <code>stringSet</code> word.
     */
    private boolean containsStringThatStartsWith(@NonNull String prefix,
                                                 @NonNull Set<String> stringSet) {
        for (String string : stringSet) {
            if (prefix.startsWith(string)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns <code>true</code> if the <code>&lt;activity&gt;</code> node contains <code>&lt;
     * intent-filter&gt;</code> with a
     * <pre>
     *   <category android:name="android.intent.category.LAUNCHER" />
     * </pre>
     *
     * @param activityNode The <code>&lt;activity&gt;</code> node to check.
     * @return <code>true</code> if the activity declares the launcher intent filter.
     */
    private boolean activityNodeHasLeanbackIntentFilter(@NonNull Node activityNode) {
        assert NODE_ACTIVITY.equals(activityNode.getNodeName());

        // Visit every intent filter
        for (Element activityChild : LintUtils.getChildren(activityNode)) {
            if (NODE_INTENT.equals(activityChild.getNodeName())) {
                for (Element intentFilterChild : LintUtils.getChildren(activityChild)) {
                    // Check to see if the category is the leanback launcher
                    if (NODE_CATEGORY.equals(intentFilterChild.getNodeName())
                        && CATEGORY_LEANBACK_LAUNCHER
                            .equals(intentFilterChild.getAttributeNS(ANDROID_URI, ATTR_NAME))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
