package com.android.manifmerger;

import com.android.utils.PositionXmlParser;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Defines an XML attribute inside a {@link XmlElement}.
 *
 * Basically a facade object on {@link Attr} objects with some added features like automatic
 * namespace handling, manifest merger friendly identifiers and smart replacement of shortened
 * full qualified class names using manifest node's package setting from the the owning Android's
 * document.
 */
public class XmlAttribute {

    private static final String UNKNOWN_POSITION = "Unknown position";

    private final XmlElement mXmlElement;
    private final Attr mXml;

    /**
     * Creates a new facade object to a {@link Attr} xml attribute in a
     * {@link XmlElement}.
     *
     * @param xmlElement the xml node object owning this attribute.
     * @param xml the xml definition of the attribute.
     */
    public XmlAttribute(XmlElement xmlElement, Attr xml) {
        this.mXmlElement = Preconditions.checkNotNull(xmlElement);
        this.mXml = Preconditions.checkNotNull(xml);
        if (mXmlElement.getType().isAttributePackageDependent(mXml)) {
            String value = mXml.getNodeValue();
            String pkg = mXmlElement.getDocument().getPackageName();
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

    /**
     * Returns the attribute's name, providing isolation from details like namespaces handling.
     */
    public AttributeName getName() {
        return unwrap(mXml);
    }

    /**
     * Returns the attribute's value
     */
    public String getValue() {
        return mXml.getNodeValue();
    }

    /**
     * Returns a display friendly identification string that can be used in machine and user
     * readable messages.
     */
    public String getAttrID() {
        return mXmlElement.getId() + "@" + (mXml.getNamespaceURI() == null
                ? mXml.getNodeName()
                : mXml.getPrefix() + ":" + mXml.getLocalName());
    }

    /**
     * Returns the position of this attribute in the original xml file. This may return an invalid
     * location as this xml fragment does not exist in any xml file but is the temporary result
     * of the merging process.
     * @return a human readable position or {@link #UNKNOWN_POSITION}
     */
    public String printPosition() {
        PositionXmlParser.Position position =
                mXmlElement.getDocument().getPositionXmlParser().getPosition(mXml);
        if (position == null) {
            return UNKNOWN_POSITION;
        }
        StringBuilder stringBuilder = new StringBuilder();
        dumpPosition(stringBuilder, position);
        return stringBuilder.toString();
    }

    private void dumpPosition(StringBuilder stringBuilder, PositionXmlParser.Position position) {
        stringBuilder
                .append("(").append(position.getLine())
                .append(",").append(position.getColumn()).append(") ")
                .append(mXmlElement.getDocument().getSourceLocation().print(true))
                .append(":").append(position.getLine());
    }


    /**
     * Abstraction to an attribute name and services associated to adding an attribute of a name to
     * an existing xml node.
     */
    public interface AttributeName {

        /**
         * Returns true if this attribute name has a namespace declaration and that namespapce is
         * the same as provided, false otherwise.
         */
        boolean isInNamespace(String namespaceURI);

        /**
         * Adds a new attribute of this name to a xml element with a value.
         * @param to the xml element to add the attribute to.
         * @param withValue the new attribute's value.
         */
        void addToNode(Element to, String withValue);
    }

    /**
     * Factory method to create an instance of {@link AttributeName} for an existing attribute on an
     * xml element.
     * @param attribute the attribute's xml definition.
     * @return an instance of {@link com.android.manifmerger.XmlAttribute.AttributeName} providing
     * namespace handling.
     */
    public static AttributeName unwrap(Attr attribute) {
        return attribute.getNamespaceURI() == null
                ? new Name(attribute.getNodeName())
                : new NamespaceAwareName(attribute);
    }

    /**
     * Implementation of {@link com.android.manifmerger.XmlAttribute.AttributeName} for an
     * attribute's declaration not using a namespace.
     */
    private static final class Name implements AttributeName {
        private final String mName;

        private Name(String name) {
            this.mName = Preconditions.checkNotNull(name);
        }

        @Override
        public boolean isInNamespace(String namespaceURI) {
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

    /**
     * Implementation of the {@link AttributeName} for a namespace aware attribute.
     */
    private static final class NamespaceAwareName implements AttributeName {
        private final String mNamespaceURI;
        // ignore for comparison and hashcoding since different documents can use different
        // prefixes for the same namespace URI.
        private final String mPrefix;
        private final String mLocalName;

        private NamespaceAwareName(Node node) {
            this.mNamespaceURI = Preconditions.checkNotNull(node.getNamespaceURI());
            this.mPrefix = Preconditions.checkNotNull(node.getPrefix());
            this.mLocalName = Preconditions.checkNotNull(node.getLocalName());
        }


        @Override
        public boolean isInNamespace(String namespaceURI) {
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
