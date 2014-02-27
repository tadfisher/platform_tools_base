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

import com.android.SdkConstants;
import com.android.annotations.Nullable;
import com.google.common.collect.ImmutableList;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Xml {@link org.w3c.dom.Element} which is mergeable.
 */
public class XmlNode {

    private final Element mXml;
    @Nullable private final XmlNodeTypes mType;
    private final XmlDocument mOwner;
    @Nullable private final XmlNode mParentNode;

    private final ImmutableList<NodeOperation> mNodeOperations;
    private final ImmutableList<AttributeOperation> mAttributeOperations;

    public XmlNode(Element xml, @Nullable XmlNodeTypes type, @Nullable XmlNode parentNode, XmlDocument owner) {

        this.mXml = xml;
        this.mType = type;
        this.mParentNode = parentNode;
        this.mOwner = owner;

        ImmutableList.Builder<NodeOperation> nodeOperationTypeBuilder =
                new ImmutableList.Builder<NodeOperation>();
        ImmutableList.Builder<AttributeOperation> attributeOperationTypeBuilder =
                new ImmutableList.Builder<AttributeOperation>();
        NamedNodeMap namedNodeMap = mXml.getAttributes();
        for (int i = 0; i < namedNodeMap.getLength(); i++) {
            Node attribute = namedNodeMap.item(i);
            if (attribute.getNamespaceURI() != null &&
                    attribute.getNamespaceURI().equals(XmlLoader.TOOLS_URI)) {
                String instruction = attribute.getLocalName();
                if (instruction.equals("node")) {
                    NodeOperationType nodeOperationType =
                            NodeOperationType.valueOf(
                                    XmlStringUtils.camelCaseToConstantName(
                                            attribute.getNodeValue()));
                    nodeOperationTypeBuilder.add(
                            new NodeOperation(nodeOperationType, this));
                } else {
                    AttributeOperationType attributeOperationType =
                            AttributeOperationType.valueOf(
                                    XmlStringUtils.xmlNameToConstantName(instruction));
                    attributeOperationTypeBuilder.add(
                            new AttributeOperation(attributeOperationType,
                                    attribute.getNodeValue(),
                                    this));
                }
            }
        }
        mNodeOperations = nodeOperationTypeBuilder.build();
        mAttributeOperations = attributeOperationTypeBuilder.build();
    }

    public boolean isA(XmlNodeTypes type) {
        return this.mType == type;
    }

    public Element getElement() {
        return mXml;
    }

    @Nullable
    public XmlNodeTypes getType() {
        return mType;
    }

    @Nullable
    public String getKey() {
        return mType != null
                ? mType.getKey(this)
                : null;
    }

    public ImmutableList<NodeOperation> getNodeOperations() {
        return mNodeOperations;
    }

    public ImmutableList<AttributeOperation> getAttributeOperations() {
        return mAttributeOperations;
    }

    public String getAndroidAttribute(String attributeName) {
        return mXml.getAttributeNS(SdkConstants.ANDROID_URI, attributeName);
    }

    @Nullable
    public XmlNode getParentNode() {
        return mParentNode;
    }

    public boolean deepCompare(XmlNode otherNode) throws Exception {

        // compare element names
        String elementName;
        if (mXml.getNamespaceURI() != null) {
            if (!mXml.getLocalName().equals(otherNode.mXml.getLocalName())) {
                throw new Exception("Element names do not match: " + mXml.getLocalName() + " "
                        + otherNode.mXml.getLocalName());
            }
            // compare element ns
            String thisNS = mXml.getNamespaceURI();
            String otherNS = otherNode.mXml.getNamespaceURI();
            if ((thisNS == null && otherNS != null)
                    || (thisNS != null && !thisNS.equals(otherNS))) {
                throw new Exception("Element namespaces names do not match: " + thisNS + " " + otherNS);
            }
            elementName = "{" + mXml.getNamespaceURI() + "}"
                    + mXml.getLocalName();
        } else {
            if (!mXml.getNodeName().equals(otherNode.mXml.getNodeName())) {
                throw new Exception("Element names do not match: " + mXml.getNodeName() + " "
                        + otherNode.mXml.getNodeName());
            }
            elementName = "{" + mXml.getNodeName() + "}";
        }

        // compare attributes, we do it twice to identify added/missing elements in both lists.
        checkAttributes(elementName, mXml.getAttributes(), otherNode.mXml.getAttributes());
        checkAttributes(elementName, otherNode.mXml.getAttributes(), mXml.getAttributes());

        // compare children
        List<Node> expectedChildren = filterUninterestingNodes(mXml.getChildNodes());
        List<Node> actualChildren = filterUninterestingNodes(otherNode.mXml.getChildNodes());
        if (expectedChildren.size() != actualChildren.size()) {
            throw new Exception(elementName + ": Number of children do not match up: "
                    + expectedChildren.size() + " " + actualChildren.size());
        }
        for (Node expectedChild : expectedChildren) {
            if (expectedChild.getNodeType() == Node.ELEMENT_NODE) {
                XmlNode childNode = XmlNode.fromXml((Element) expectedChild, this, mOwner);
                if (!findAndCompareNode(actualChildren, childNode)) {
                    // bail out.
                    return false;
                }
            }
        }
        return true;
    }

    private boolean findAndCompareNode(List<Node> actualChildren, XmlNode childNode) throws Exception {
        for (Node potentialNode : actualChildren) {
            if (potentialNode.getNodeType() == Node.ELEMENT_NODE) {
                XmlNode otherChildNode = XmlNode
                        .fromXml((Element) potentialNode, this, mOwner);
                if (childNode.getType() == otherChildNode.getType()
                        && ((childNode.getKey() == null && otherChildNode.getKey() == null)
                        || childNode.getKey().equals(otherChildNode.getKey()))) {
                    return childNode.deepCompare(otherChildNode);
                }
            }
        }
        return false;
    }

    private static List<Node> filterUninterestingNodes(NodeList nodeList) {
        List<Node> interestingNodes = new ArrayList<Node>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                Text t = (Text) node;
                if (!t.getData().trim().isEmpty()) {
                    interestingNodes.add(node);
                }
            } else if (node.getNodeType() != Node.COMMENT_NODE) {
                interestingNodes.add(node);
            }

        }
        return interestingNodes;
    }

    private static void checkAttributes(
            String elementName,
            NamedNodeMap thisAttrs,
            NamedNodeMap otherAttrs) throws Exception {

        for (int i = 0; i < thisAttrs.getLength(); i++) {
            Attr expectedAttr = (Attr) thisAttrs.item(i);
            // ignore namespace declarations.
            if (expectedAttr.getName().startsWith("xmlns")) {
                continue;
            }
            Attr actualAttr = expectedAttr.getNamespaceURI() == null
                    ? (Attr) otherAttrs.getNamedItem(expectedAttr.getName())
                    : (Attr) otherAttrs.getNamedItemNS(expectedAttr.getNamespaceURI(),
                            expectedAttr.getLocalName());

            if (actualAttr == null) {
                throw new Exception(elementName + ": No attribute found:" + expectedAttr);
            }
            if (!expectedAttr.getValue().equals(actualAttr.getValue())) {
                throw new Exception(elementName + ": Attribute values do not match: "
                        + expectedAttr.getValue() + " " + actualAttr.getValue());
            }
        }
    }

    public static XmlNode fromXml(
            Element element,
            @Nullable XmlNode parentNode,
            XmlDocument owner) {

        XmlNodeTypes xmlNodeTypes = XmlNodeTypes.fromXmlSimpleName(element.getNodeName());
        return new XmlNode(element, xmlNodeTypes, parentNode, owner);
    }
}
