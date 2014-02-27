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

import com.android.utils.PositionXmlParser;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Records all the actions taken by the merging tool.
 *
 * Each action generate at least one {@link com.android.manifmerger.ActionRecorder.Record}
 * containing enough information to generate a machine or human readable report.
 *
 * The records are not organized in a temporal structure as the merging tool takes such decision but
 * are organized by xml elements and attributes. For each node (elements or attributes), a linked
 * list of actions that happened on the node is recorded to display all decisions that were made
 * for that particular node.
 *
 * Such structure will permit displaying logs with co-located decisions records for each element,
 * for instance (non binding example) :
 * <pre>
 * activity:com.foo.bar.MyApp
 *     Added from manifest.xml:31
 *     Rejected from lib1_manifest.xml:65
 * </pre>
 *
 * Each record for a node (element or attribute) will contain the following metadata :
 *
 * <ul>
 *     <li>{@link com.android.manifmerger.ActionRecorder.ActionTarget} to identify the action
 *     taken by the merging tool.</li>
 *     <li>{@link com.android.manifmerger.ActionRecorder.ActionType} to identify whether the action
 *     applies to an attribute or an element.</li>
 *     <li>{@link com.android.manifmerger.ActionRecorder.ActionLocation} to identify the source xml
 *     location for the node.</li>
 * </ul>
 *
 * Elements will also contain :
 * <ul>
 *     <li>Element name : a name composed of the element type and its key. (replaced with typed)</li>
 *     <li>{@link com.android.manifmerger.NodeOperationType} the original annotation from the xml
 *     justifying the merging tool decision.</li>
 * </ul>
 *
 * While attributes will have :
 * <ul>
 *     <li>element name</li>
 *     <li>attribute name : the namespace aware xml name</li>
 *     <li>{@link AttributeOperationType} the original annotation from the source xml justifying the
 *     merging tool decision.</li>
 * </ul>
 */
public class ActionRecorder {

    // defines all the records for the merging tool activity, indexed by element name+key.
    private final Map<String, DecisionTreeRecord> records =
            new HashMap<String, DecisionTreeRecord>();

    /**
     * Defines all possible actions taken from the merging tool for an xml element or attribute.
     */
    public enum ActionType {
        /**
         * The element was added into the resulting merged manifest.
         */
        Added,
        /**
         * The element was merged with another element into the resulting merged manifest.
         */
        Merged,
        /**
         * The element was rejected.
         */
        Rejected
    }

    public enum ActionTarget {
        Node,
        Attribute
    }

    /**
     * Defines an action location which is composed of a pointer to the source location (e.g. a
     * file) and a position within that source location.
     */
    public static class ActionLocation {
        private final XmlLoader.SourceLocation mSourceLocation;
        private final PositionXmlParser.Position mPosition;

        public ActionLocation(XmlLoader.SourceLocation sourceLocation,
                PositionXmlParser.Position position) {
            mSourceLocation = sourceLocation;
            mPosition = position;
        }

        @Override
        public String toString() {
            return mSourceLocation.print(true) + ":" + mPosition.getLine();
        }
    }

    /**
     * Defines an abstract record contain common metadata for elements and attributes actions.
     */
    private abstract static class Record {
        protected final ActionType mActionType;
        protected final ActionLocation mActionLocation;

        private Record(ActionType actionType,
                ActionLocation actionLocation) {
            mActionType = actionType;
            mActionLocation = actionLocation;
        }

        abstract ActionTarget getActionTarget();

        public void print(StringBuilder stringBuilder) {
            stringBuilder.append(mActionType)
                    .append(" from ")
                    .append(mActionLocation);
        }
    }

    /**
     * Defines a merging tool action for an xml element.
     */
    private static class NodeRecord extends Record {

        private final NodeOperationType mNodeOperationType;

        private NodeRecord(ActionType actionType,
                ActionLocation actionLocation,
                NodeOperationType nodeOperationType) {
            super(actionType, actionLocation);
            this.mNodeOperationType = nodeOperationType;
        }

        @Override
        ActionTarget getActionTarget() {
            return ActionTarget.Node;
        }
    }

    /**
     * Internal structure on how records are kept for the duration of the merging activity for a
     * particular xml element.
     *
     * Each xml element should have an associated instance which keeps a list of
     * {@link com.android.manifmerger.ActionRecorder.NodeRecord} for all the node actions related
     * to this xml element.
     *
     * It will also contain a map indexed by attribute name on all the attribute actions related
     * to that particular attribute within the xml element.
     *
     */
    private static class DecisionTreeRecord {
        // all other occurrences of the nodes decisions, in order of decisions.
        List<NodeRecord> mNodeRecords = new ArrayList<NodeRecord>();

        // all attributes decisions indexed by attribute name.
        Map<String, List<AttributeRecord>> mAttributeRecords =
                new HashMap<String, List<AttributeRecord>>();

        // this need to be enhanced to get the operation type per node and per attribute as the first
        // in the list may not contain it.
    }

    /**
     * Defines a merging tool action for an xml attribute
     */
    private static class AttributeRecord extends Record {

        // first in wins which should be fine, the first
        // operation type will be the highest priority one
        private final AttributeOperationType mOperationType;

        private AttributeRecord(
                ActionType actionType,
                ActionLocation actionLocation,
                AttributeOperationType operationType) {
            super(actionType, actionLocation);
            this.mOperationType = operationType;
        }

        @Override
        ActionTarget getActionTarget() {
            return ActionTarget.Attribute;
        }
    }

    /**
     * When the first xml file is loaded, there is nothing to merge with, however, each xml element
     * and attribute added to the initial merged file need to be recorded.
     *
     * @param xmlElement xml element added to the initial merged document.
     */
    public void recordDefaultNodeAction(XmlElement xmlElement) {
        String storageKey = xmlElement.getId();
        if (!records.containsKey(storageKey)) {
            recordNodeAction(xmlElement, ActionType.Added);
            for (XmlAttribute xmlAttribute : xmlElement.getAttributes()) {
                AttributeOperationType attributeOperation = xmlElement
                        .getAttributeOperationType(xmlAttribute.getName().toString());
                recordAttributeAction(xmlElement, xmlAttribute.getName(), ActionType.Added, attributeOperation);
            }
            for (XmlElement childNode : xmlElement.getMergeableElements()) {
                recordDefaultNodeAction(childNode);
            }
        }
    }

    /**
     * Record a node action taken by the merging tool.
     * @param xmlElement the action's target xml element
     * @param actionType the action's type
     */
    public synchronized void recordNodeAction(
            XmlElement xmlElement,
            ActionType actionType) {

        String storageKey = xmlElement.getId();
        DecisionTreeRecord nodeDecisionTree = records.get(storageKey);
        if (nodeDecisionTree == null) {
            nodeDecisionTree = new DecisionTreeRecord();
            records.put(storageKey, nodeDecisionTree);
        }
        NodeRecord record = new NodeRecord(actionType,
                new ActionLocation(
                        xmlElement.getDocument().getSourceLocation(), xmlElement.getPosition()),
                xmlElement.getOperationType());
        nodeDecisionTree.mNodeRecords.add(record);
    }

    /**
     * Records an attribute action taken by the merging tool
     * @param targetNode the xml element owning the attribute
     * @param attributeName the attribute name
     * @param actionType the action's type
     * @param attributeOperationType the original tool annotation leading to the merging tool
     *                               decision.
     */
    public synchronized void recordAttributeAction(
            XmlElement targetNode,
            XmlAttribute.AttributeName attributeName,
            ActionType actionType,
            AttributeOperationType attributeOperationType) {

        String storageKey = targetNode.getId();
        DecisionTreeRecord nodeDecisionTree = records.get(storageKey);
        // by now the node should have been added for this element.
        Preconditions.checkState(nodeDecisionTree != null);
        String attributeKey = attributeName.toString();
        List<AttributeRecord> attributeRecords =
                nodeDecisionTree.mAttributeRecords.get(attributeKey);
        if (attributeRecords == null) {
            attributeRecords = new ArrayList<AttributeRecord>();
            nodeDecisionTree.mAttributeRecords.put(attributeKey, attributeRecords);
        }
        AttributeRecord attributeRecord = new AttributeRecord(
                actionType,
                new ActionLocation(
                        targetNode.getDocument().getSourceLocation(), targetNode.getPosition()),
                attributeOperationType);
        attributeRecords.add(attributeRecord);
    }

    /**
     * Initial dump of the merging tool actions, need to be refined and spec'ed out properly.
     * @param logger logger to log to at {@link Level#INFO} level.
     */
    public void log(Logger logger) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-- Merging decision tree log ---\n");
        for (Map.Entry<String, DecisionTreeRecord> record : records.entrySet()) {
            stringBuilder.append(record.getKey()).append("\n");
            for (NodeRecord nodeRecord : record.getValue().mNodeRecords) {
                nodeRecord.print(stringBuilder);
                stringBuilder.append("\n");
            }
            for (Map.Entry<String, List<AttributeRecord>> attributeRecords :
                    record.getValue().mAttributeRecords.entrySet()) {
                stringBuilder.append("\t").append(attributeRecords.getKey());
                for (AttributeRecord attributeRecord : attributeRecords.getValue()) {
                    stringBuilder.append("\t\t");
                    attributeRecord.print(stringBuilder);
                    stringBuilder.append("\n");
                }

            }
        }
        logger.log(Level.INFO, stringBuilder.toString());
    }

}
