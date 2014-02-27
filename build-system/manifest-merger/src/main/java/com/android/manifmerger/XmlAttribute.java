package com.android.manifmerger;

import com.android.utils.PositionXmlParser;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Defines an XML attribute inside a {@link com.android.manifmerger.XmlNode}.
 *
 * Basically a facade object on {@link Attr} objects with some added features like automatic
 * namespace handling, manifest merger friendly identifiers and
 */
public class XmlAttribute {

    private final XmlNode mXmlNode;
    private final Attr mXml;

    public XmlAttribute(XmlNode xmlNode, Attr xml) {
        this.mXmlNode = Preconditions.checkNotNull(xmlNode);
        this.mXml = Preconditions.checkNotNull(xml);
        if (mXmlNode.getType().isAttributePackageDependent(mXml)) {
            String value = mXml.getNodeValue();
            String pkg = mXmlNode.getDocument().getPackageName();
            // We know it's a shortened FQCN if it starts with a dot
            // or does not contain any dot.
            if (value != null && !value.isEmpty() &&
                    (value.indexOf('.') == -1 || value.charAt(0) == '.')) {
                if (value.charAt(0) == '.') {
                    value = pkg + value;
                } else {
                    value = pkg + '.' + value;
                }
                mXml.setNodeValue(value);
            }
        }
    }

    public AttributeName getName() {
        return unwrap(mXml);
    }

    public String getValue() {
        return mXml.getNodeValue();
    }

    public String getAttrID() {
        return mXmlNode.getId() + "@" + (mXml.getNamespaceURI() == null
                ? mXml.getNodeName()
                : mXml.getPrefix() + ":" + mXml.getLocalName());
    }

    public String printPosition() {
        PositionXmlParser.Position position =
                mXmlNode.getDocument().getPositionXmlParser().getPosition(mXml);
        if (position == null) {
            return "Unknown Position";
        }
        StringBuilder stringBuilder = new StringBuilder();
        dumpPosition(stringBuilder, position);
        return stringBuilder.toString();
    }

    private void dumpPosition(StringBuilder stringBuilder, PositionXmlParser.Position position) {
        stringBuilder
                .append("(").append(position.getLine())
                .append(",").append(position.getColumn()).append(") ")
                .append(mXmlNode.getDocument().getSourceLocation().print(true))
                .append(":").append(position.getLine());
    }



    public interface AttributeName {
        boolean isInNameSpace(String namespaceURI);

        void addToNode(Element to, String withValue);
    }

    public static AttributeName unwrap(Node node) {
        return node.getNamespaceURI() == null
                ? new Name(node.getNodeName())
                : new NamespaceAwareName(node);
    }

    private static final class Name implements AttributeName {
        private final String mName;

        private Name(String name) {
            this.mName = Preconditions.checkNotNull(name);
        }

        @Override
        public boolean isInNameSpace(String namespaceURI) {
            return false;
        }

        @Override
        public void addToNode(Element to, String withValue) {
            to.setAttribute(mName, withValue);
        }

        @Override
        public boolean equals(Object o) {
            return (o != null && o instanceof Name && ((Name) o).mName.equals(this.mName));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(mName);
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    private static final class NamespaceAwareName implements AttributeName {
        private final String mNamespaceURI;
        // ignore for comparison and hashcoding
        private final String mPrefix;
        private final String mLocalName;

        private NamespaceAwareName(Node node) {
            this.mNamespaceURI = Preconditions.checkNotNull(node.getNamespaceURI());
            this.mPrefix = Preconditions.checkNotNull(node.getPrefix());
            this.mLocalName = Preconditions.checkNotNull(node.getLocalName());
        }


        @Override
        public boolean isInNameSpace(String namespaceURI) {
            return mNamespaceURI.equals(namespaceURI);
        }

        @Override
        public void addToNode(Element to, String withValue) {
            // TODO(jedo): consider standardizing everything on "android:"
            to.setAttributeNS(mNamespaceURI, mPrefix + ":" + mLocalName, withValue);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(mNamespaceURI, mLocalName);
        }

        @Override
        public boolean equals(Object o) {
            return (o != null && o instanceof NamespaceAwareName
                    && ((NamespaceAwareName) o).mLocalName.equals(this.mLocalName)
                    && ((NamespaceAwareName) o).mNamespaceURI.equals(this.mNamespaceURI));
        }

        @Override
        public String toString() {
            return mPrefix + ":" + mLocalName;
        }
    }

}
