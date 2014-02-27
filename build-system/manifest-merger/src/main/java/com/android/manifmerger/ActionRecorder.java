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
 * Records all the actions by the merging tool.
 */
public class ActionRecorder {

    public enum ActionType {
        Added,
        Replaced,
        Rejected
    }

    public enum ActionTarget {
        Node,
        Attribute
    }

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

    private static class DecisionTreeRecord {
        // all other occurrences of the nodes decisions, in order of decisions.
        List<NodeRecord> mNodeRecords = new ArrayList<NodeRecord>();

        // all attributes decisions indexed by attribute name.
        Map<String, List<AttributeRecord>> mAttributeRecords =
                new HashMap<String, List<AttributeRecord>>();

        // this need to be enhanced to get the operation type per node and per attribute as the first
        // in the list may not contain it.
    }

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

    private final Map<String, DecisionTreeRecord> records =
            new HashMap<String, DecisionTreeRecord>();


    public void recordDefaultNodeAction(XmlNode xmlNode) {
        String storageKey = xmlNode.getId();
        if (!records.containsKey(storageKey)) {
            recordNodeAction(xmlNode, ActionType.Added);
            for (XmlAttribute xmlAttribute : xmlNode.getAttributes()) {
                AttributeOperationType attributeOperation = xmlNode
                        .getAttributeOperationType(xmlAttribute.getName().toString());
                recordAttributeAction(xmlNode, xmlAttribute.getName(), ActionType.Added, attributeOperation);
            }
            for (XmlNode childNode : xmlNode.getMergeableElements()) {
                recordDefaultNodeAction(childNode);
            }
        }
    }

    public synchronized void recordNodeAction(
            XmlNode xmlNode,
            ActionType actionType) {

        String storageKey = xmlNode.getId();
        DecisionTreeRecord nodeDecisionTree = records.get(storageKey);
        if (nodeDecisionTree == null) {
            nodeDecisionTree = new DecisionTreeRecord();
            records.put(storageKey, nodeDecisionTree);
        }
        NodeRecord record = new NodeRecord(actionType,
                new ActionLocation(
                        xmlNode.getDocument().getSourceLocation(), xmlNode.getPosition()),
                xmlNode.getOperationType());
        nodeDecisionTree.mNodeRecords.add(record);
    }

    public synchronized void recordAttributeAction(
            XmlNode targetNode,
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
