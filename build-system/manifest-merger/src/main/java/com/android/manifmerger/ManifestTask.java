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

package com.android.manifmerger;

import static com.android.manifmerger.PlaceholderHandler.KeyBasedValueResolver;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.ILogger;
import com.android.utils.SdkUtils;
import com.android.utils.XmlUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Super class for all tasks involving manifest files handling.
 */
public class ManifestTask {

    @NonNull
    protected final File mManifestFile;

    @NonNull
    protected final KeyBasedValueResolver<String> mPlaceHolderValueResolver;

    @NonNull
    protected final KeyBasedValueResolver<SystemProperty> mSystemPropertyResolver;

    protected final ILogger mLogger;

    public ManifestTask(
            @NonNull KeyBasedValueResolver<SystemProperty> systemPropertiesResolver,
            @NonNull KeyBasedValueResolver<String> placeHolderValueResolver,
            @NonNull File manifestFile, @NonNull ILogger logger) {
        this.mSystemPropertyResolver = systemPropertiesResolver;
        this.mPlaceHolderValueResolver = placeHolderValueResolver;
        this.mManifestFile = manifestFile;
        this.mLogger = logger;
    }

    /**
     * List of manifest files properties that can be directly overridden without using a
     * placeholder.
     */
    public static enum SystemProperty implements AutoAddingProperty {

        /**
         * Allow setting the merged manifest file package name.
         */
        PACKAGE {
            @Override
            public void addTo(@NonNull ActionRecorder actionRecorder,
                    @NonNull XmlDocument document,
                    @NonNull String value) {
                addToElement(this, actionRecorder, value, document.getRootNode());
            }
        },
        /**
         * http://developer.android.com/guide/topics/manifest/manifest-element.html#vcode
         */
        VERSION_CODE {
            @Override
            public void addTo(@NonNull ActionRecorder actionRecorder,
                    @NonNull XmlDocument document,
                    @NonNull String value) {
                addToElementInAndroidNS(this, actionRecorder, value, document.getRootNode());
            }
        },
        /**
         * http://developer.android.com/guide/topics/manifest/manifest-element.html#vname
         */
        VERSION_NAME {
            @Override
            public void addTo(@NonNull ActionRecorder actionRecorder,
                    @NonNull XmlDocument document,
                    @NonNull String value) {
                addToElementInAndroidNS(this, actionRecorder, value, document.getRootNode());
            }
        },
        /**
         * http://developer.android.com/guide/topics/manifest/uses-sdk-element.html#min
         */
        MIN_SDK_VERSION {
            @Override
            public void addTo(@NonNull ActionRecorder actionRecorder,
                    @NonNull XmlDocument document,
                    @NonNull String value) {
                addToElementInAndroidNS(this, actionRecorder, value,
                        createOrGetUseSdk(actionRecorder, document));
            }
        },
        /**
         * http://developer.android.com/guide/topics/manifest/uses-sdk-element.html#target
         */
        TARGET_SDK_VERSION {
            @Override
            public void addTo(@NonNull ActionRecorder actionRecorder,
                    @NonNull XmlDocument document,
                    @NonNull String value) {
                addToElementInAndroidNS(this, actionRecorder, value,
                        createOrGetUseSdk(actionRecorder, document));
            }
        };

        public String toCamelCase() {
            return SdkUtils.constantNameToCamelCase(name());
        }

        // utility method to add an attribute which name is derived from the enum name().
        private static void addToElement(
                SystemProperty systemProperty,
                ActionRecorder actionRecorder,
                String value,
                XmlElement to) {

            to.getXml().setAttribute(systemProperty.toCamelCase(), value);
            XmlAttribute xmlAttribute = new XmlAttribute(to,
                    to.getXml().getAttributeNode(systemProperty.toCamelCase()), null);
            actionRecorder.recordAttributeAction(xmlAttribute, new Actions.AttributeRecord(
                    Actions.ActionType.INJECTED,
                    new Actions.ActionLocation(
                            to.getSourceLocation(),
                            PositionImpl.UNKNOWN),
                    xmlAttribute.getId(),
                    null, /* reason */
                    null /* attributeOperationType */));
        }

        // utility method to add an attribute in android namespace which local name is derived from
        // the enum name().
        private static void addToElementInAndroidNS(
                SystemProperty systemProperty,
                ActionRecorder actionRecorder,
                String value,
                XmlElement to) {

            String toolsPrefix = XmlUtils.lookupNamespacePrefix(
                    to.getXml(), SdkConstants.ANDROID_URI, SdkConstants.ANDROID_NS_NAME, false);
            to.getXml().setAttributeNS(SdkConstants.ANDROID_URI,
                    toolsPrefix + XmlUtils.NS_SEPARATOR + systemProperty.toCamelCase(),
                    value);
            Attr attr = to.getXml().getAttributeNodeNS(SdkConstants.ANDROID_URI,
                    systemProperty.toCamelCase());

            XmlAttribute xmlAttribute = new XmlAttribute(to, attr, null);
            actionRecorder.recordAttributeAction(xmlAttribute,
                    new Actions.AttributeRecord(
                            Actions.ActionType.INJECTED,
                            new Actions.ActionLocation(
                                    to.getSourceLocation(),
                                    PositionImpl.UNKNOWN),
                            xmlAttribute.getId(),
                            null, /* reason */
                            null /* attributeOperationType */
                    )
            );

        }

        // utility method to create or get an existing use-sdk xml element under manifest.
        // this could be made more generic by adding more metadata to the enum but since there is
        // only one case so far, keep it simple.
        private static XmlElement createOrGetUseSdk(
                ActionRecorder actionRecorder, XmlDocument document) {

            Element manifest = document.getXml().getDocumentElement();
            NodeList usesSdks = manifest
                    .getElementsByTagName(ManifestModel.NodeTypes.USES_SDK.toXmlName());
            if (usesSdks.getLength() == 0) {
                // create it first.
                Element useSdk = manifest.getOwnerDocument().createElement(
                        ManifestModel.NodeTypes.USES_SDK.toXmlName());
                manifest.appendChild(useSdk);
                XmlElement xmlElement = new XmlElement(useSdk, document);
                Actions.NodeRecord nodeRecord = new Actions.NodeRecord(
                        Actions.ActionType.INJECTED,
                        new Actions.ActionLocation(xmlElement.getSourceLocation(),
                                PositionImpl.UNKNOWN),
                        xmlElement.getId(),
                        "use-sdk injection requested",
                        NodeOperationType.STRICT);
                actionRecorder.recordNodeAction(xmlElement, nodeRecord);
                return xmlElement;
            } else {
                return new XmlElement((Element) usesSdks.item(0), document);
            }
        }
    }

    /**
     * Defines a property that can add or override itself into an XML document.
     */
    public interface AutoAddingProperty {

        /**
         * Add itself (possibly just override the current value) with the passed value
         * @param actionRecorder to record actions.
         * @param document the xml document to add itself to.
         * @param value the value to set of this property.
         */
        void addTo(@NonNull ActionRecorder actionRecorder,
                @NonNull XmlDocument document,
                @NonNull String value);
    }

    /**
     * Helper class for map based placeholders key value pairs.
     */
    public static class MapBasedKeyBasedValueResolver<T> implements
            KeyBasedValueResolver<T> {

        private final ImmutableMap<T, String> keyValues;

        public MapBasedKeyBasedValueResolver(Map<T, String> keyValues) {
            this.keyValues = ImmutableMap.copyOf(keyValues);
        }

        @Nullable
        @Override
        public String getValue(@NonNull T key) {
            return keyValues.get(key);
        }
    }

    public abstract static class Invoker<T extends Invoker<T>> {

        protected final File mMainManifestFile;

        protected final ImmutableMap.Builder<SystemProperty, String> mSystemProperties =
                new ImmutableMap.Builder<SystemProperty, String>();

        protected final ILogger mLogger;

        protected final ImmutableMap.Builder<String, String> mPlaceHolders =
                new ImmutableMap.Builder<String, String>();

        public Invoker(
                @NonNull File mainManifestFile, @NonNull ILogger logger) {
            this.mMainManifestFile = Preconditions.checkNotNull(mainManifestFile);
            this.mLogger = logger;
        }

        /**
         * Sets a value for a {@link com.android.manifmerger.ManifestTask.SystemProperty}
         * @param override the property to set
         * @param value the value for the property
         * @return itself.
         */
        public T setOverride(SystemProperty override, String value) {
            mSystemProperties.put(override, value);
            return thisAsT();
        }

        /**
         * Adds a new placeholder name and value for substitution.
         * @return itself.
         */
        public T setPlaceHolderValue(String placeHolderName, String value) {
            mPlaceHolders.put(placeHolderName, value);
            return thisAsT();
        }

        @SuppressWarnings("unchecked")
        private T thisAsT() {
            return (T) this;
        }
    }

    /**
     * Implementation a {@link KeyResolver} capable of resolving all
     * selectors value in the context of the passed libraries to this merging activities.
     */
    static class SelectorResolver implements KeyResolver<String> {

        private final Map<String, String> mSelectors = new HashMap<String, String>();

        protected void addSelector(String key, String value) {
            mSelectors.put(key, value);
        }

        @Nullable
        @Override
        public String resolve(String key) {
            return mSelectors.get(key);
        }

        @Override
        public Iterable<String> getKeys() {
            return mSelectors.keySet();
        }
    }

    // a wrapper exception to all sorts of failure exceptions that can be thrown during merging.
    public static class MergeFailureException extends Exception {

        protected MergeFailureException(Exception cause) {
            super(cause);
        }
    }

    /**
     * Perform {@link com.android.manifmerger.ManifestTask.SystemProperty} injection.
     * @param mergingReport to log actions and errors.
     * @param xmlDocument the xml document to inject into.
     */
    protected void performSystemPropertiesInjection(
            MergingReport.Builder mergingReport,
            XmlDocument xmlDocument) {
        for (SystemProperty systemProperty : SystemProperty.values()) {
            String propertyOverride = mSystemPropertyResolver.getValue(systemProperty);
            if (propertyOverride != null) {
                systemProperty.addTo(mergingReport.getActionRecorder(), xmlDocument, propertyOverride);
            }
        }
    }
}
