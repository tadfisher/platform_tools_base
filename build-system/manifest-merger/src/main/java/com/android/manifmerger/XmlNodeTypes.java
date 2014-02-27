package com.android.manifmerger;

/**
 * Describes a mergeable xml element.
 */
public enum XmlNodeTypes {

    ACTIVITY(MergeType.MERGE, "name"),
    APPLICATION(MergeType.MERGE, "name"),
    MANIFEST(MergeType.IGNORE, "name"),
    SUPPORTS_SCREENS(MergeType.MERGE, null),
    USE_LIBRARY(MergeType.CONFLICT, "name"),
    USES_FEATURE(MergeType.MERGE, "name"),
    USES_PERMISSION(MergeType.MERGE, "name"),
    USES_SDK(MergeType.CONFLICT, null);


    private final MergeType mergeType;

    private final String keyAttributeName;

    XmlNodeTypes(MergeType mergeType, String keyAttributeName) {
        this.mergeType = mergeType;
        this.keyAttributeName = keyAttributeName;
    }

    String toXmlName() {
        return XmlStringUtils.constantNameToXmlName(name());
    }

    String getKey(XmlNode xmlNode) {
        return xmlNode.getAndroidAttribute(keyAttributeName);
    }

    static XmlNodeTypes fromXmlSimpleName(String xmlSimpleName) {
        String constantName = XmlStringUtils.xmlNameToConstantName(xmlSimpleName);

        return XmlNodeTypes.valueOf(constantName);
    }

    public MergeType getMergeType() {
        return mergeType;
    }
}
