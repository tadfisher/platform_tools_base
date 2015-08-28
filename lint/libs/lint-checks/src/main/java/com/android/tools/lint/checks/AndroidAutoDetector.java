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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.FN_ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.TAG_APPLICATION;
import static com.android.SdkConstants.TAG_INTENT_FILTER;
import static com.android.SdkConstants.TAG_SERVICE;
import static com.android.xml.AndroidManifest.NODE_ACTION;
import static com.android.xml.AndroidManifest.NODE_METADATA;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import lombok.ast.ClassDeclaration;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodDeclaration;
import lombok.ast.Node;
import lombok.ast.StrictListAccessor;
import lombok.ast.TypeReferencePart;
import lombok.ast.VariableDefinition;

/**
 * Detector for Android Auto issues.
 * <p> Uses a {@code <meta-data>} tag with a {@code name="com.google.android.gms.car.application"}
 * as a trigger for validating Automotive specific issues.
 */
public class AndroidAutoDetector extends ResourceXmlDetector
        implements Detector.XmlScanner, Detector.JavaScanner {

    public static final Implementation IMPL = new Implementation(
            AndroidAutoDetector.class,
            EnumSet.of(Scope.RESOURCE_FILE, Scope.MANIFEST, Scope.JAVA_FILE));

    /** Invalid attribute for uses tag.*/
    public static final Issue INVALID_USES_TAG_ISSUE = Issue.create(
            "InvalidUsesTagAttribute", //$NON-NLS-1$
            "Invalid `name` attribute for `uses` element.",
            "The <uses> element in `<automotiveApp>` should contain a " +
            "valid value for the `name` attribute.\n" +
            "Valid values are `media` or `notification`.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPL).addMoreInfo(
            "https://developer.android.com/training/auto/start/index.html#auto-metadata");

    /** Missing MediaBrowserService action */
    public static final Issue MISSING_MEDIA_BROWSER_SERVICE_ACTION_ISSUE = Issue.create(
            "MissingMediaBrowserServiceIntentFilter", //$NON-NLS-1$
            "Missing intent-filter with action `android.media.browse.MediaBrowserService`.",
            "An Automotive Media App requires an exported service that extends " +
            "`android.service.media.MediaBrowserService` with an " +
            "`intent-filter` for the action `android.media.browse.MediaBrowserService` " +
            "to be able to browse and play media.\n" +
            "To do this, add\n" +
            "`<intent-filter>`\n" +
            "    `<action android:name=\"android.media.browse.MediaBrowserService\" />`\n" +
            "`</intent-filter>`\n to the service that extends " +
            "`android.service.media.MediaBrowserService`",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPL).addMoreInfo(
            "https://developer.android.com/training/auto/audio/index.html#config_manifest");

    /** Invalid meta-data tag. */
    public static final Issue INVALID_AUTO_METADATA_ISSUE = Issue.create(
            "InvalidAutoMetadata", //$NON-NLS-1$
            "meta-data tag does not appear under the application element.",
            "The meta-data tag should appear as a child of the application " +
            "tag for the app to be considered a valid automotive enabled app.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPL);

    /** Missing intent-filter for Media Search. */
    public static final Issue MISSING_INTENT_FILTER_FOR_MEDIA_SEARCH = Issue.create(
            "MissingIntentFilterForMediaSearch", //$NON-NLS-1$
            "Missing intent-filter with action `android.media.action.MEDIA_PLAY_FROM_SEARCH`",
            "To support voice searches on Android Auto, you should also register an " +
            "`intent-filter` for the action `android.media.action.MEDIA_PLAY_FROM_SEARCH`" +
            ".\nTo do this, add\n" +
            "`<intent-filter>`\n" +
            "    `<action android:name=\"android.media.action.MEDIA_PLAY_FROM_SEARCH\" />`\n" +
            "`</intent-filter>`\n" +
            "to your `<activity>` or `<service>`.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPL).addMoreInfo(
            "https://developer.android.com/training/auto/audio/index.html#support_voice");

    /** Missing implementation of MediaSession.Callback#onPlayFromSearch*/
    public static final Issue MISSING_ON_PLAY_FROM_SEARCH = Issue.create(
            "MissingOnPlayFromSearch", //$NON-NLS-1$
            "Missing `onPlayFromSearch`.",
            "To support voice searches on Android Auto, in addition to adding an " +
            "`intent-filter` for the action `onPlayFromSearch`," +
            " you also need to override and implement " +
            "`MediaSession.Callback.onPlayFromSearch(String query, Bundle bundle)`",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPL).addMoreInfo(
            "https://developer.android.com/training/auto/audio/index.html#support_voice");

    private static final String CAR_APPLICATION_METADATA_NAME =
            "com.google.android.gms.car.application"; //$NON-NLS-1$
    private static final String VAL_NAME_MEDIA = "media"; //$NON-NLS-1$
    private static final String VAL_NAME_NOTIFICATION = "notification"; //$NON-NLS-1$
    private static final String TAG_AUTOMOTIVE_APP = "automotiveApp"; //$NON-NLS-1$
    private static final String ATTR_RESOURCE = "resource"; //$NON-NLS-1$
    private static final String TAG_USES = "uses"; //$NON-NLS-1$

    private static final String ACTION_MEDIA_BROWSER_SERVICE =
            "android.media.browse.MediaBrowserService"; //$NON-NLS-1$
    private static final String ACTION_MEDIA_PLAY_FROM_SEARCH =
            "android.media.action.MEDIA_PLAY_FROM_SEARCH"; //$NON-NLS-1$

    // Java methods, identifiers and classes used by the JavaScanner
    private static final String CLASS_MEDIA_SESSION_CALLBACK =
            "android.media.session.MediaSession.Callback"; //$NON-NLS-1$
    private static final String METHOD_MEDIA_SESSION_PLAY_FROM_SEARCH =
            "onPlayFromSearch"; //$NON-NLS-1$
    private static final String STRING_ARG = "String"; //$NON-NLS-1$
    private static final String BUNDLE_ARG = "Bundle"; //$NON-NLS-1$

    /**
     * Indicates whether we identified that the current app is an automotive app and
     * that we should validate all the automotive specific issues.
     */
    private boolean mDoAutomotiveAppCheck;

    /** Indicates that a {@code ACTION_MEDIA_BROWSER_SERVICE} intent-filter action was found.*/
    private boolean mMediaIntentFilterFound;

    /** Indicates that a {@code ACTION_MEDIA_PLAY_FROM_SEARCH} intent-filter action was found.*/
    private boolean mMediaSearchIntentFilterFound;

    /** The resource file name deduced by the meta-data resource value */
    private String mAutomotiveResourceFileName;

    private boolean mIsAutomotiveMediaApp;

    /** Location of the main AndroidManifest.xml */
    private Location mMainManifestLocation;

    /** Constructs a new {@link AndroidAutoDetector} check */
    public AndroidAutoDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        // We only need to check the meta data resource file in res/xml if any.
        return folderType == ResourceFolderType.XML;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                TAG_AUTOMOTIVE_APP, // Root element of a declared automotive descriptor.
                NODE_METADATA,      // meta-data from AndroidManifest.xml
                TAG_SERVICE,        // service from AndroidManifest.xml
                TAG_INTENT_FILTER); // Any declared intent-filter from AndroidManifest.xml
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        if (context.getPhase() == 1) {
            mIsAutomotiveMediaApp = false;
            mAutomotiveResourceFileName = null;
            mMediaIntentFilterFound = false;
            mMediaSearchIntentFilterFound = false;
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String tagName = element.getTagName();
        if (context.getPhase() == 1) {
            // In the first phase we look at the AndroidManifest.xml meta-data tag
            // and if necessary request a second phase for processing the service tags.
            if (NODE_METADATA.equals(tagName) && !mDoAutomotiveAppCheck) {
                checkAutoMetadataTag(context, element);
            } else if (TAG_AUTOMOTIVE_APP.equals(tagName)
                    && mDoAutomotiveAppCheck
                    && mAutomotiveResourceFileName != null
                    && mAutomotiveResourceFileName.equals(context.file.getName())) {
                checkAutomotiveAppElement(context, element);
            }
        } else {
            assert context.getPhase() == 2;

            if (mIsAutomotiveMediaApp) {
                if (TAG_SERVICE.equals(tagName)) {
                    checkServiceForBrowserServiceIntentFilter(context, element);
                } else if (TAG_INTENT_FILTER.equals(tagName)) {
                    checkForMediaSearchIntentFilter(context, element);
                }
            }
        }
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        if (context.getPhase() == 2
                && context.getMainProject() == context.getProject()
                && mDoAutomotiveAppCheck
                && FN_ANDROID_MANIFEST_XML.equals(context.file.getName())
                && mIsAutomotiveMediaApp) {

            mMainManifestLocation = Location.create(context.file);
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mMainManifestLocation != null
                && mDoAutomotiveAppCheck
                && mIsAutomotiveMediaApp) {

            if (!mMediaIntentFilterFound
                    && context.isEnabled(MISSING_MEDIA_BROWSER_SERVICE_ACTION_ISSUE)) {
                context.report(MISSING_MEDIA_BROWSER_SERVICE_ACTION_ISSUE,
                        mMainManifestLocation,
                        "Missing `intent-filter` for action " +
                        "`android.media.browse.MediaBrowserService` that is required for " +
                        "android auto support");
            }
            if (!mMediaSearchIntentFilterFound
                    && context.isEnabled(MISSING_INTENT_FILTER_FOR_MEDIA_SEARCH)) {
                context.report(MISSING_INTENT_FILTER_FOR_MEDIA_SEARCH,
                        mMainManifestLocation,
                        "Missing `intent-filter` for action " +
                        "`android.media.action.MEDIA_PLAY_FROM_SEARCH`.");
            }
        }
    }

    private void checkAutoMetadataTag(XmlContext context, Element element) {
        assert context.getPhase() == 1;
        String name = element.getAttributeNS(ANDROID_URI, ATTR_NAME);

        if (CAR_APPLICATION_METADATA_NAME.equals(name)) {
            // Ensure that the meta-data element appears as a child of application
            if (element.getParentNode() == null
                    || !TAG_APPLICATION.equals(element.getParentNode().getNodeName())) {
                context.report(INVALID_AUTO_METADATA_ISSUE, context.getLocation(element),
                        "`<" + NODE_METADATA + ">` tag must appear as a child of the " +
                        "`<application>` tag.");
            } else {

                String autoFileName = element.getAttributeNS(ANDROID_URI, ATTR_RESOURCE);

                if (autoFileName != null && autoFileName.startsWith("@xml/")) { //$NON-NLS-1$
                    // Store the fact that we need to check all the auto issues.
                    mDoAutomotiveAppCheck = true;
                    mAutomotiveResourceFileName =
                            autoFileName.substring("@xml/".length()) + DOT_XML; //$NON-NLS-1$
                    // Request another phase. Based on the value in this file,
                    // we need to validate the manifest file again.
                    context.requestRepeat(this, Scope.MANIFEST_SCOPE);
                }
            }
        }
    }

    private void checkAutomotiveAppElement(XmlContext context, Element element) {
        assert context.getPhase() == 1;

        for (Element child : LintUtils.getChildren(element)) {

            if (TAG_USES.equals(child.getTagName())) {
                String attrValue = child.getAttribute(SdkConstants.ATTR_NAME);
                if (VAL_NAME_MEDIA.equals(attrValue)) {
                    mIsAutomotiveMediaApp = true;
                } else if (!VAL_NAME_NOTIFICATION.equals(attrValue)
                        && context.isEnabled(INVALID_USES_TAG_ISSUE)) {
                    // Error invalid value for attribute.
                    context.report(INVALID_USES_TAG_ISSUE,
                            context.getLocation(child.getAttributeNode(ATTR_NAME)),
                            "Expecting one of `" + VAL_NAME_MEDIA + "` or `" +
                            VAL_NAME_NOTIFICATION + "` for the name " +
                            "attribute in " + TAG_USES + " tag.");
                }
            }
        }
    }

    private void checkServiceForBrowserServiceIntentFilter(XmlContext context, Element element) {
        assert context.getPhase() == 2;

        if (TAG_SERVICE.equals(element.getTagName())
                && mDoAutomotiveAppCheck
                && !mMediaIntentFilterFound) {

            for (Element child : LintUtils.getChildren(element)) {
                String tagName = child.getTagName();
                if (TAG_INTENT_FILTER.equals(tagName)) {
                    for (Element filterChild : LintUtils.getChildren(child)) {
                        if (NODE_ACTION.equals(filterChild.getTagName())) {
                            String actionValue = filterChild.getAttributeNS(ANDROID_URI, ATTR_NAME);
                            if (ACTION_MEDIA_BROWSER_SERVICE.equals(actionValue)) {
                                mMediaIntentFilterFound = true;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkForMediaSearchIntentFilter(XmlContext context, Element element) {
        assert context.getPhase() == 2;

        if (mIsAutomotiveMediaApp && !mMediaSearchIntentFilterFound) {

            for (Element filterChild : LintUtils.getChildren(element)) {
                if (NODE_ACTION.equals(filterChild.getTagName())) {
                    String actionValue = filterChild.getAttributeNS(ANDROID_URI, ATTR_NAME);
                    if (ACTION_MEDIA_PLAY_FROM_SEARCH.equals(actionValue)) {
                        mMediaSearchIntentFilterFound = true;
                        break;
                    }
                }
            }
        }
    }

    // Implementation of the JavaScanner

    @Override
    @Nullable
    public List<String> applicableSuperClasses() {
        // We currently enable scanning only for media apps.
        return mIsAutomotiveMediaApp ?
                Collections.singletonList(CLASS_MEDIA_SESSION_CALLBACK) : null;
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @Nullable ClassDeclaration declaration,
            @NonNull Node node, @NonNull JavaParser.ResolvedClass resolvedClass) {
        if (declaration != null) {
            MediaSessionCallbackVisitor visitor = new MediaSessionCallbackVisitor();
            declaration.accept(visitor);
            if (!visitor.isPlayFromSearchMethodFound()
                    && context.isEnabled(MISSING_ON_PLAY_FROM_SEARCH)) {

                context.report(MISSING_ON_PLAY_FROM_SEARCH,
                        context.getLocation(declaration.astName()),
                        "This class does not override `" +
                        METHOD_MEDIA_SESSION_PLAY_FROM_SEARCH + "` from `MediaSession.Callback`" +
                        " The method should be overridden and implemented to support " +
                        "Voice search on Android Auto.");
            }
        }
    }

    @Override
    @Nullable
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(METHOD_MEDIA_SESSION_PLAY_FROM_SEARCH);
    }

    /**
     * A Visitor class to search for {@code MediaSession.Callback#onPlayFromSearch(..)}
     * method declaration.
     */
    private static class MediaSessionCallbackVisitor extends ForwardingAstVisitor {

        private boolean mOnPlayFromSearchFound;

        public boolean isPlayFromSearchMethodFound() {
            return mOnPlayFromSearchFound;
        }

        @Override
        public boolean visitMethodDeclaration(MethodDeclaration node) {
            String astValue = node.astMethodName().astValue();
            if (node.astMethodName() != null
                    && METHOD_MEDIA_SESSION_PLAY_FROM_SEARCH.equals(astValue)) {
                // Check the method parameters #onPlayFromSearch(String query, Bundle extras)
                StrictListAccessor<VariableDefinition, MethodDeclaration> params =
                        node.astParameters();
                if (params != null && params.size() == 2) {
                    VariableDefinition query = params.first();
                    VariableDefinition bundle = params.last();
                    TypeReferencePart strReference = query.astTypeReference().astParts().last();
                    String strReferenceTypeName = strReference.getTypeName();
                    String bundleRefTypeName = bundle.astTypeReference().astParts().last()
                            .getTypeName();
                    if (STRING_ARG.equals(strReferenceTypeName)
                            && BUNDLE_ARG.equals(bundleRefTypeName)) {
                        mOnPlayFromSearchFound = true;
                    }
                }
            }
            return super.visitMethodDeclaration(node);
        }
    }
}
