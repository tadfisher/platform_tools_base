package com.android.manifmerger;

import com.android.annotations.Nullable;
import com.android.utils.PositionXmlParser;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.OutputStream;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Represents a loaded xml document.
 */
public class XmlDocument {

    private final Element mRootElement;
    private final PositionXmlParser mPositionXmlParser;

    private final ImmutableList<XmlNode> mergeableElements;

    public XmlDocument(PositionXmlParser positionXmlParser, Element rootElement) {
        this.mPositionXmlParser = positionXmlParser;
        this.mRootElement = rootElement;
        this.mergeableElements = initMergeableElements();
    }

    public ImmutableList<XmlNode> getMergeableElements() {
        return mergeableElements;
    }

    public Optional<XmlNode> getNodeByTypeAndKey(XmlNodeTypes type, @Nullable String keyValue) {
        for (XmlNode xmlNode : mergeableElements) {
            if (xmlNode.isA(type) &&
                    (keyValue == null || xmlNode.getKey().equals(keyValue))) {
                return Optional.of(xmlNode);
            }
        }
        return Optional.absent();
    }

    private ImmutableList<XmlNode> initMergeableElements() {
        ImmutableList.Builder<XmlNode> mergeableNodes = new ImmutableList.Builder<XmlNode>();
        XmlNode parentNode = new XmlNode(mRootElement, XmlNodeTypes.MANIFEST, null, this);
        mergeableNodes.add(parentNode);
        NodeList nodeList = mRootElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                XmlNodeTypes xmlNodeTypes = XmlNodeTypes.fromXmlSimpleName(
                        node.getNodeName());
                if (xmlNodeTypes != null) {
                    XmlNode xmlNode = new XmlNode((Element) node, xmlNodeTypes, parentNode, this);
                    mergeableNodes.add(xmlNode);
                }
            }
        }
        return mergeableNodes.build();
    }

    public boolean write(OutputStream outputStream) {

        try {
            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");         //$NON-NLS-1$
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");                   //$NON-NLS-1$
            tf.setOutputProperty(OutputKeys.INDENT, "yes");                       //$NON-NLS-1$
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",     //$NON-NLS-1$
                    "4");                                            //$NON-NLS-1$
            tf.transform(new DOMSource(mRootElement), new StreamResult(outputStream));
            return true;
        } catch (TransformerException e) {
            return false;
        }
    }

        // merge this higher priority document with a lower priority document.
    public Optional<XmlDocument> merge(XmlDocument lowerPriorityXmlDocument) {
        // read all lower priority mergeable nodes.
        // if the same node is not defined in this document merge it in.
        // if the same is defined, so far, give an error message.
        for (XmlNode lowerPriorityNode : lowerPriorityXmlDocument.getMergeableElements()) {
            if (lowerPriorityNode.getType() != null &&
                    lowerPriorityNode.getType().getMergeType() == MergeType.IGNORE) {
                continue;
            }
            Optional<XmlNode> thisDocumentNode = getNodeByTypeAndKey(lowerPriorityNode.getType(),
                    lowerPriorityNode.getKey());
            if (thisDocumentNode.isPresent()) {
                // it's defined in both files
                Logger.getAnonymousLogger().severe(
                        lowerPriorityNode.getElement().toString() + " defined in both files...");
            } else {
                XmlNode parentNode = lowerPriorityNode.getParentNode();
                Node node = this.mRootElement.getOwnerDocument()
                        .adoptNode(lowerPriorityNode.getElement());
                Logger.getAnonymousLogger().info("Adopted " + node);
                if (parentNode == null) {
                    // that's not right...
                    Logger.getAnonymousLogger().severe(
                            "No parent for " + lowerPriorityNode.toString());
                    getNodeByTypeAndKey(XmlNodeTypes.MANIFEST, null).get().getElement().appendChild(node);
                } else {
                    getNodeByTypeAndKey(parentNode.getType(), parentNode.getKey()).get().getElement().appendChild(node);
                }
            }
        }
        // force re-parsing of mergeable items.
        return Optional.of(new XmlDocument(mPositionXmlParser, mRootElement));
    }

    public PositionXmlParser getPositionXmlParser() {
        return mPositionXmlParser;
    }
}
