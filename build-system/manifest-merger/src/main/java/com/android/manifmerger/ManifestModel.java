package com.android.manifmerger;

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.google.common.collect.ImmutableList;

import org.w3c.dom.Attr;

/**
 * Model for the manifest file merging activities.
 *
 * This model will describe each element that is eligible for merging and associated merging
 * policies. It is not reusable as most of its interfaces are private but a future enhancement
 * could easily make this more generic/reusable if we need to merge more than manifest files.
 *
 */
public class ManifestModel {

    /**
     * Interface responsible for providing a key extraction capability from a xml element.
     * Some elements store their keys as an attribute, some as a sub-element attribute, some don't
     * have any key.
     */
    interface NodeKeyResolver {

        /**
         * Returns the key associated with this xml element.
         * @param xmlElement the xml element to get the key from
         * @return the key as a string to uniquely identify xmlElement from similarly typed elements
         * in the xml document or null if there is no key.
         */
        @Nullable String getKey(XmlElement xmlElement);
    }

    /**
     * Implementation of {@link com.android.manifmerger.ManifestModel.NodeKeyResolver} that do not
     * provide any key (the element has to be unique in the xml document).
     */
    private static class NoKeyNodeResolver implements NodeKeyResolver {

        @Override
        @Nullable
        public String getKey(XmlElement xmlElement) {
            return null;
        }
    }

    /**
     * Implementation of {@link com.android.manifmerger.ManifestModel.NodeKeyResolver} that uses an
     * attribute to resolve the key value.
     */
    private static class AttributeBasedNodeKeyResolver implements NodeKeyResolver {

        @Nullable private final String namespaceUri;
        private final String attributeName;

        /**
         * Build a new instance capable of resolving an xml element key from the passed attribute
         * namespace and local name.
         * @param namespaceUri optional namespace for the attribute name.
         * @param attributeName attribute name
         */
        public AttributeBasedNodeKeyResolver(@Nullable String namespaceUri, String attributeName) {
            this.namespaceUri = namespaceUri;
            this.attributeName = attributeName;
        }

        @Override
        @Nullable
        public String getKey(XmlElement xmlElement) {
            return namespaceUri == null
                ? xmlElement.getElement().getAttribute(attributeName)
                : xmlElement.getElement().getAttributeNS(namespaceUri, attributeName);
        }
    }

    /**
     * Subclass of {@link com.android.manifmerger.ManifestModel.AttributeBasedNodeKeyResolver} that
     * uses "android:name" as the attribute.
     */
    private static class NameAttributeNodeKeyResolver extends AttributeBasedNodeKeyResolver {

        public NameAttributeNodeKeyResolver() {
            super(SdkConstants.ANDROID_URI, "name");
        }
    }

    private static final NameAttributeNodeKeyResolver defaultNameAttributeResolver =
            new NameAttributeNodeKeyResolver();

    private static final NoKeyNodeResolver defaultNoKeyNodeResolver = new NoKeyNodeResolver();

    /**
     * Definitions of the support node types in the Android Manifest file.
     * {@see http://developer.android.com/guide/topics/manifest/manifest-intro.html} for more
     * details about the xml format.
     *
     * There is no DTD or schema associated with the file type so this is best effort in providing
     * some metadata on the elements of the Android's xml file.
     *
     * Each xml element is defined as an enum value and for each node, extra metadata is added
     * <ul>
     *     <li>{@link com.android.manifmerger.MergeType} to identify how the merging engine
     *     should process this element.</li>
     *     <li>{@link com.android.manifmerger.ManifestModel.NodeKeyResolver} to resolve the
     *     element's key. Elements can have an attribute like "android:name", others can use
     *     a sub-element, and finally some do not have a key and are meant to be unique.</li>
     *     <li>List of attributes that support smart substitution of class names to fully qualified
     *     class names using the document's package declaration. The list's size can be 0..n</li>
     * </ul>
     *
     * It is of the outermost importance to keep this model correct as it is used by the merging
     * engine to make all its decisions. There should not be special casing in the engine, all
     * decisions must be represented here.
     *
     * If you find yourself needing to extend the model to support future requirements, do it here
     * and modify the engine to make proper decision based on the added metadata.
     */
    public enum NodeTypes {

        ACTION(MergeType.CONFLICT, defaultNameAttributeResolver),
        ACTIVITY(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver,
                "parentActivityName", "name"),
        ACTIVITY_ALIAS(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver,
                "targetActivity", "name"),
        APPLICATION(MergeType.MERGE, defaultNoKeyNodeResolver, "backupAgent", "name"),
        CATEGORY(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver),
        INSTRUMENTATION(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver, "name"),
        // TODO(jedo): key is provided by sub elements...
        INTENT_FILTER(MergeType.RESPECT_TOOLS_INSTRUCTIONS, new NoKeyNodeResolver()),
        MANIFEST(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNoKeyNodeResolver),
        META_DATA(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver),
        PROVIDER(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver, "name"),
        RECEIVER(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver, "name"),
        SERVICE(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver, "name"),
        SUPPORTS_SCREENS(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNoKeyNodeResolver), // TODO
        USE_LIBRARY(MergeType.CONFLICT, defaultNameAttributeResolver),
        USES_FEATURE(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver),
        USES_PERMISSION(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver),
        USES_SDK(MergeType.CONFLICT, defaultNoKeyNodeResolver);


        private final MergeType mergeType;
        private final NodeKeyResolver mNodeKeyResolver;
        private final ImmutableList<String> fqcnAttributes;

        private NodeTypes(MergeType mergeType, NodeKeyResolver nodeKeyResolver, String... fqcnAttributes) {
            this.mergeType = mergeType;
            this.mNodeKeyResolver = nodeKeyResolver;
            this.fqcnAttributes = ImmutableList.copyOf(fqcnAttributes);
        }

        // TODO(jedo): we need to support cases where the key is actually provided by a sub-element
        // like intent-filter.
        String getKey(XmlElement xmlElement) {
            return mNodeKeyResolver.getKey(xmlElement);
        }

        /**
         * Return true if the attribute support smart substitution of partially fully qualified
         * class names with package settings as provided by the manifest node's package attribute
         * {@see http://developer.android.com/guide/topics/manifest/manifest-element.html}
         *
         * @param attribute the xml attribute definition.
         * @return true if this name supports smart substitution or false if not.
         */
        boolean isAttributePackageDependent(Attr attribute) {
            return fqcnAttributes != null
                    && SdkConstants.ANDROID_URI.equals(attribute.getNamespaceURI())
                    && fqcnAttributes.contains(attribute.getLocalName());
        }

        /**
         * Returns the {@link NodeTypes} instance from an xml element name (without namespace
         * decoration). For instance, an xml element
         * <pre>
         *     <activity android:name="foo">
         *         ...
         *     </activity>
         * </pre>
         * has a xml simple name of "activity" which will make to {@link NodeTypes#ACTIVITY} value.
         *
         * Note : a runtime exception will be generated if no mapping from the simple name to a
         * {@link com.android.manifmerger.ManifestModel.NodeTypes} exists.
         *
         * @param xmlSimpleName the xml (lower-hyphen separated words) simple name.
         * @return the {@link NodeTypes} associated with that element name.
         */
        static NodeTypes fromXmlSimpleName(String xmlSimpleName) {
            String constantName = XmlStringUtils.xmlNameToConstantName(xmlSimpleName);

            // TODO(jedo): is legal to have non standard xml elements in manifest files ? if yes
            // consider adding a CUSTOM NodeTypes and not generate exception here.
            return NodeTypes.valueOf(constantName);
        }

        public MergeType getMergeType() {
            return mergeType;
        }
    }
}
