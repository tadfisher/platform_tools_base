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

import com.android.annotations.Nullable;
import com.android.utils.PositionXmlParser;
import com.android.utils.PositionXmlParser.Position;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Xml {@link org.w3c.dom.Element} which is mergeable.
 */
public class XmlNode {

    private static final Logger logger = Logger.getLogger(Logger.class.getName());

    private final Element mXml;
    private final XmlModel.NodeTypes mType;
    private final XmlDocument mDocument;

    private final NodeOperationType mNodeOperationType;
    // list of non tools related attributes.
    private final ImmutableList<XmlAttribute> mAttributes;
    // map of all tools related attributes keyed by target attribute name
    private final Map<String, AttributeOperationType> mAttributesOperationTypes;
    // list of mergeable children elements.
    private final ImmutableList<XmlNode> mMergeableChildren;

    public XmlNode(Element xml, XmlDocument document) {

        this.mXml = Preconditions.checkNotNull(xml);
        this.mType = XmlModel.NodeTypes.fromXmlSimpleName(mXml.getNodeName());
        this.mDocument = Preconditions.checkNotNull(document);

        ImmutableMap.Builder<String, AttributeOperationType> attributeOperationTypeBuilder =
                ImmutableMap.builder();
        ImmutableList.Builder<XmlAttribute> attributesListBuilder = ImmutableList.builder();
        NamedNodeMap namedNodeMap = mXml.getAttributes();
        NodeOperationType lastNodeOperationType = null;
        for (int i = 0; i < namedNodeMap.getLength(); i++) {
            Node attribute = namedNodeMap.item(i);
            if (XmlLoader.TOOLS_URI.equals(attribute.getNamespaceURI())) {
                String instruction = attribute.getLocalName();
                if (instruction.equals("node")) {
                    // should we flag an error when there are more than one operation type on a node ?
                    lastNodeOperationType = NodeOperationType.valueOf(
                            XmlStringUtils.camelCaseToConstantName(
                                    attribute.getNodeValue()));
                } else {
                    AttributeOperationType attributeOperationType =
                            AttributeOperationType.valueOf(
                                    XmlStringUtils.xmlNameToConstantName(instruction));
                    for (String attributeName : Splitter.on(',').trimResults()
                            .split(attribute.getNodeValue())) {
                        attributeOperationTypeBuilder.put(attributeName, attributeOperationType);
                    }
                }
            }
        }
        mAttributesOperationTypes = attributeOperationTypeBuilder.build();
        for (int i = 0; i < namedNodeMap.getLength(); i++) {
            Node attribute = namedNodeMap.item(i);
            if (!XmlLoader.TOOLS_URI.equals(attribute.getNamespaceURI())) {

                XmlAttribute xmlAttribute = new XmlAttribute(this, (Attr) attribute);
                attributesListBuilder.add(xmlAttribute);
            }

        }
        mNodeOperationType = lastNodeOperationType;
        mAttributes = attributesListBuilder.build();
        mMergeableChildren = initMergeableChildren();
    }

    public boolean isA(XmlModel.NodeTypes type) {
        return this.mType == type;
    }

    public Element getElement() {
        return mXml;
    }

    public XmlDocument getDocument() {
        return mDocument;
    }

    public XmlModel.NodeTypes getType() {
        return mType;
    }

    @Nullable
    public String getKey() {
        return mType.getKey(this);
    }

    public String getId() {
      return getKey() == null
          ? getElementName()
          : getElementName() + "#" + getKey();
    }

    public NodeOperationType getOperationType() {
        return mNodeOperationType != null
                ? mNodeOperationType
                : NodeOperationType.STRICT;
    }

    public AttributeOperationType getAttributeOperationType(String attributeName) {
        return mAttributesOperationTypes.containsKey(attributeName)
                ? mAttributesOperationTypes.get(attributeName)
                : AttributeOperationType.STRICT;
    }

    public Collection<AttributeOperationType> getAttributeOperations() {
        return mAttributesOperationTypes.values();
    }

    public List<XmlAttribute> getAttributes() {
        return mAttributes;
    }

    public Optional<XmlAttribute> getAttribute(XmlAttribute.AttributeName attributeName) {
        for (XmlAttribute xmlAttribute : mAttributes) {
            if (xmlAttribute.getName().equals(attributeName)) {
                return Optional.of(xmlAttribute);
            }
        }
        return Optional.absent();
    }

    public PositionXmlParser.Position getPosition() {
        return mDocument.getPositionXmlParser().getPosition(mXml);
    }

    public void printPosition(StringBuilder stringBuilder) {
        PositionXmlParser.Position position = getPosition();
        if (position == null) {
            logger.severe("Cannot determine location for " + mXml.getNodeName()
                    + " in " + mDocument.getSourceLocation().toString());
            stringBuilder.append("Unknown position");
            return;
        }
        dumpPosition(stringBuilder, position);
    }

    public String printPosition() {
        StringBuilder stringBuilder = new StringBuilder();
        printPosition(stringBuilder);
        return stringBuilder.toString();
    }

    private void dumpPosition(StringBuilder stringBuilder, Position position) {
      stringBuilder
          .append("(").append(position.getLine())
          .append(",").append(position.getColumn()).append(") ")
          .append(mDocument.getSourceLocation().print(true))
          .append(":").append(position.getLine());
    }

    public void mergeWithLowerPriorityNode(
            XmlNode lowerPriorityNode,
            MergingReport.Builder mergingReport) {

        logger.info("Merging " + getId()
            + " with lower " + lowerPriorityNode.printPosition());

        // remove all comments associated with this node since we are merging
        // with a higher priority version.
        // TODO(jedo): fix before checking in...
        removeLeadingComments();

        // merge attributes.
        for (XmlAttribute lowerPriorityAttribute : lowerPriorityNode.getAttributes()) {
            Optional<XmlAttribute> myOptionalAttribute = getAttribute(lowerPriorityAttribute.getName());
            if (myOptionalAttribute.isPresent()) {
                XmlAttribute myAttribute = myOptionalAttribute.get();
                // this is conflict, depending on tools:replace, tools:strict
                // for now we keep the higher priority value and log it.
                String error = "Attribute " + myAttribute.getAttrID()
                        + " is also present at " + lowerPriorityAttribute.printPosition()
                        + " use tools:replace to override it.";
                mergingReport.addWarning(error);
                mergingReport.getActionRecorder().recordAttributeAction(lowerPriorityNode,
                        myAttribute.getName(),
                        ActionRecorder.ActionType.Rejected,
                        AttributeOperationType.REMOVE);
            } else {
                // cool, does not exist, just add it.
                // TODO(jedo): handle tools:remove when removing attributes
                // from lower priority files
                lowerPriorityAttribute.getName().addToNode(mXml, lowerPriorityAttribute.getValue());
            }
        }
        // merge children.
        mergeChildren(lowerPriorityNode, mergingReport);

    }

    public ImmutableList<XmlNode> getMergeableElements() {
        return mMergeableChildren;
    }

    public Optional<XmlNode> getNodeByTypeAndKey(XmlModel.NodeTypes type, @Nullable String keyValue) {
        for (XmlNode xmlNode : mMergeableChildren) {
            if (xmlNode.isA(type) &&
                    (keyValue == null || keyValue.equals(xmlNode.getKey()))) {
                return Optional.of(xmlNode);
            }
        }
        return Optional.absent();
    }

    // merge this higher priority node with a lower priority node.
    public boolean mergeChildren(XmlNode lowerPriorityNode, MergingReport.Builder mergingReport) {
        // read all lower priority mergeable nodes.
        // if the same node is not defined in this document merge it in.
        // if the same is defined, so far, give an error message.
        for (XmlNode lowerPriorityChild : lowerPriorityNode.getMergeableElements()) {
            if (lowerPriorityChild.getType() != null &&
                    lowerPriorityChild.getType().getMergeType() == MergeType.IGNORE) {
                continue;
            }
            Optional<XmlNode> thisChildNode = getNodeByTypeAndKey(lowerPriorityChild.getType(),
                    lowerPriorityChild.getKey());
            if (thisChildNode.isPresent()) {
                // it's defined in both files
                logger.fine(lowerPriorityChild.getElement().toString() + " defined in both files...");
                // are we merging no matter what or the two nodes equals ?
                if (thisChildNode.get().getType().getMergeType() != MergeType.MERGE
                        && !thisChildNode.get().compareXml(lowerPriorityChild, mergingReport)) {

                    String info = "Node abandoned : " + lowerPriorityChild.printPosition();
                    mergingReport.addError(info);
                    mergingReport.getActionRecorder().recordNodeAction(
                            lowerPriorityChild, ActionRecorder.ActionType.Rejected);
                    return false;
                }
                // this is a sophisticated xml element which attributes and children need be
                // merged modulo tools instructions.
                thisChildNode.get().mergeWithLowerPriorityNode(lowerPriorityChild, mergingReport);
            } else {
                // only in the new file, just import it.
                // TODO(jedo):need to check the prefixes...
                Node node = mXml.getOwnerDocument().adoptNode(lowerPriorityChild.getElement());
                mXml.appendChild(node);
                mergingReport.getActionRecorder().recordNodeAction(lowerPriorityChild,
                        ActionRecorder.ActionType.Added);
                logger.fine("Adopted " + node);
            }
        }
        return true;
    }

    public boolean compareXml(XmlNode otherNode, MergingReport.Builder mergingReport) {

        // compare element names
        if (mXml.getNamespaceURI() != null) {
            if (!mXml.getLocalName().equals(otherNode.mXml.getLocalName())) {
                logger.severe("Element names do not match: " + mXml.getLocalName() + " "
                        + otherNode.mXml.getLocalName());
                return false;
            }
            // compare element ns
            String thisNS = mXml.getNamespaceURI();
            String otherNS = otherNode.mXml.getNamespaceURI();
            if ((thisNS == null && otherNS != null)
                    || (thisNS != null && !thisNS.equals(otherNS))) {
                logger.severe("Element namespaces names do not match: " + thisNS + " " + otherNS);
                return false;
            }
        } else {
            if (!mXml.getNodeName().equals(otherNode.mXml.getNodeName())) {
                logger.severe("Element names do not match: " + mXml.getNodeName() + " "
                        + otherNode.mXml.getNodeName());
                return false;
            }
        }

        // compare attributes, we do it twice to identify added/missing elements in both lists.
        if (!checkAttributes(this, otherNode, mergingReport)) {
            return false;
        }
        if (!checkAttributes(otherNode, this, mergingReport)) {
            return false;
        }

        // compare children
        List<Node> expectedChildren = filterUninterestingNodes(mXml.getChildNodes());
        List<Node> actualChildren = filterUninterestingNodes(otherNode.mXml.getChildNodes());
        if (expectedChildren.size() != actualChildren.size()) {
            // TODO(jedo): i18n
            String error = getId() + ": Number of children do not match up: "
                + expectedChildren.size() + " versus " + actualChildren.size()
                + " in " + otherNode.getId();
            mergingReport.addError(error);
            return false;
        }
        for (Node expectedChild : expectedChildren) {
            if (expectedChild.getNodeType() == Node.ELEMENT_NODE) {
                XmlNode expectedChildNode = new XmlNode((Element) expectedChild, mDocument);
                if (!findAndCompareNode(actualChildren, expectedChildNode, mergingReport)) {
                    // bail out.
                    return false;
                }
            }
        }
        return true;
    }

    private boolean findAndCompareNode(
            List<Node> actualChildren,
            XmlNode childNode,
            MergingReport.Builder mergingReport) {

        for (Node potentialNode : actualChildren) {
            if (potentialNode.getNodeType() == Node.ELEMENT_NODE) {
                XmlNode otherChildNode = new XmlNode((Element) potentialNode, mDocument);
                if (childNode.getType() == otherChildNode.getType()
                        && ((childNode.getKey() == null && otherChildNode.getKey() == null)
                        || childNode.getKey().equals(otherChildNode.getKey()))) {
                    return childNode.compareXml(otherChildNode, mergingReport);
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

    private static boolean checkAttributes(
            XmlNode expected,
            XmlNode actual,
            MergingReport.Builder mergingReport) {

        for (XmlAttribute expectedAttr : expected.getAttributes()) {
            XmlAttribute.AttributeName attributeName = expectedAttr.getName();
            if (attributeName.isInNamespace(XmlLoader.TOOLS_URI)) {
                continue;
            }
            Optional<XmlAttribute> actualAttr = actual.getAttribute(attributeName);
            if (actualAttr.isPresent()) {
                if (!expectedAttr.getValue().equals(actualAttr.get().getValue())) {
                    mergingReport.addError(
                            "Attribute " + expectedAttr.getAttrID()
                                    + " do not match:" + expectedAttr.getValue()
                                    + " versus " + actualAttr.get().getValue() + " at " + actual.printPosition());
                    return false;
                }
            } else {
                mergingReport.addError(
                        "Attribute " + expectedAttr.getAttrID() + " not found at " + actual.printPosition());
                return false;
            }
        }
        return true;
    }

    private ImmutableList<XmlNode> initMergeableChildren() {
        ImmutableList.Builder<XmlNode> mergeableNodes = new ImmutableList.Builder<XmlNode>();
        NodeList nodeList = mXml.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                XmlNode xmlNode = new XmlNode((Element) node, mDocument);
                mergeableNodes.add(xmlNode);
            }
        }
        return mergeableNodes.build();
    }

    /**
     * Removes all leading comments from the this node location.
     */
    private void removeLeadingComments() {
        List<Node> nodesToRemove = new ArrayList<Node>();
        Node previousSibling = mXml.getPreviousSibling();
        while (previousSibling != null
            && (previousSibling.getNodeType() == Node.COMMENT_NODE
                || previousSibling.getNodeType() == Node.TEXT_NODE)) {
            nodesToRemove.add(previousSibling);
            previousSibling = previousSibling.getPreviousSibling();
        }
        // It's tempting to fold the two loops but don't do it. when you remove a child from
        // its parent, the next and previous siblings are reset to null preventing from backing up
        // further.
        for (Node nodeToRemove : nodesToRemove) {
            mXml.getParentNode().removeChild(nodeToRemove);
        }
    }

    private String getElementName() {
        return mXml.getNamespaceURI() == null
                ? mXml.getNodeName()
                : mXml.getPrefix() + ":" + mXml.getLocalName();
    }
}