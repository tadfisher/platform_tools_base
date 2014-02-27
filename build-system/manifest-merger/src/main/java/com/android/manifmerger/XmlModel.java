package com.android.manifmerger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import com.android.SdkConstants;
import com.android.annotations.Nullable;

import org.w3c.dom.Node;

/**
 * Describes a mergeable xml element.
 */
public class XmlModel {

    public interface NodeKeyResolver {
        String getKey(XmlNode xmlNode);
    }

    public static class NoKeyNodeResolver implements NodeKeyResolver {

        @Override
        public String getKey(XmlNode xmlNode) {
            return null;
        }
    }

    public static class AttributeBasedNodeKeyResolver implements NodeKeyResolver {

        @Nullable private final String namespaceUri;
        private final String attributeName;

        public AttributeBasedNodeKeyResolver(@Nullable String namespaceUri, String attributeName) {
            this.namespaceUri = namespaceUri;
            this.attributeName = attributeName;
        }

        @Override
        public String getKey(XmlNode xmlNode) {
            return namespaceUri == null
                ? xmlNode.getElement().getAttribute(attributeName)
                : xmlNode.getElement().getAttributeNS(namespaceUri, attributeName);
        }
    }

    public static class NameAttributeNodeKeyResolver extends AttributeBasedNodeKeyResolver {

        public NameAttributeNodeKeyResolver() {
            super(SdkConstants.ANDROID_URI, "name");
        }
    }

    private static final NameAttributeNodeKeyResolver defaultNameAttributeResolver =
            new NameAttributeNodeKeyResolver();

    private static final NoKeyNodeResolver defaultNoKeyNodeResolver = new NoKeyNodeResolver();

    public enum NodeTypes {

// FQCN Attributes for reference.
//    "application/name",
//            "application/backupAgent",
//            "activity/name",
//            "activity/parentActivityName",
//            "activity-alias/name",
//            "activity-alias/targetActivity",
//            "receiver/name",
//            "service/name",
//            "provider/name",
//            "instrumentation/name"

        ACTION(MergeType.CONFLICT, defaultNameAttributeResolver),
        ACTIVITY(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver, "parentActivityName", "name"),
        ACTIVITY_ALIAS(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver, "targetActivity", "name"),
        APPLICATION(MergeType.MERGE, defaultNoKeyNodeResolver, "backupAgent", "name"),
        CATEGORY(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver),
        INSTRUMENTATION(MergeType.RESPECT_TOOLS_INSTRUCTIONS, defaultNameAttributeResolver, "name"),
        INTENT_FILTER(MergeType.RESPECT_TOOLS_INSTRUCTIONS, new NoKeyNodeResolver()), // key is provided by sub-elements.
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

        NodeTypes(MergeType mergeType, NodeKeyResolver nodeKeyResolver, String... fqcnAttributes) {
            this.mergeType = mergeType;
            this.mNodeKeyResolver = nodeKeyResolver;
            this.fqcnAttributes = ImmutableList.copyOf(fqcnAttributes);
        }

        // TODO(jedo): we need to support cases where the key is actually provided by a sub-element
        // like intent-filter.
        String getKey(XmlNode xmlNode) {
            return mNodeKeyResolver.getKey(xmlNode);
        }

        boolean isAttributePackageDependent(Node attribute) {
            Preconditions.checkArgument(attribute.getNodeType() == Node.ATTRIBUTE_NODE);
            return fqcnAttributes != null
                    && SdkConstants.ANDROID_URI.equals(attribute.getNamespaceURI())
                    && fqcnAttributes.contains(attribute.getLocalName());
        }

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
