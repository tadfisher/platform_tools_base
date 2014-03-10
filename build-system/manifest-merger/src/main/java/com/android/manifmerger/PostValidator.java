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

import static com.android.manifmerger.ActionRecorder.ActionType;

import com.android.annotations.NonNull;
import com.android.utils.ILogger;
import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.Map;

/**
 * Validator that runs post merging activities and verifies that all "tools:" instructions
 * triggered an action by the merging tool.
 * <p>
 *
 * This is primarily to catch situations like a user entered a tools:remove="foo" directory on one
 * of its elements and that particular attribute was never removed during the merges possibly
 * indicating a unforeseen change of configuration.
 * <p>
 *
 * Most of the output from this validation should be warnings.
 */
public class PostValidator {

    /**
     * Post validation of the merged document. This will essentially check that all merging
     * instructions were applied at least once.
     *
     * @param xmlDocument merged document to check.
     * @param actionRecorder the actions recorded during the merging activities.
     * @param iLogger logger for errors and warnings.
     */
    public static MergingReport.Result validate(
            @NonNull XmlDocument xmlDocument,
            @NonNull ActionRecorder actionRecorder,
            @NonNull ILogger iLogger) {

        Preconditions.checkNotNull(xmlDocument);
        Preconditions.checkNotNull(actionRecorder);
        Preconditions.checkNotNull(iLogger);
        return validate(xmlDocument.getRootNode(), actionRecorder, iLogger);
    }

    /**
     * Validate an xml element and recursively its children elemnts, ensuring that all merging
     * instructions were applied.
     *
     * @param xmlElement xml element to validate.
     * @param actionRecorder the actions recorded during the merging activities.
     * @param iLogger logger for errors and warnings.
     * @return {@link MergingReport.Result#SUCCESS} if all the merging
     * instructions were applied once or {@link MergingReport.Result#WARNING} otherwise.
     */
    private static MergingReport.Result validate(
            XmlElement xmlElement,
            ActionRecorder actionRecorder,
            ILogger iLogger) {

        MergingReport.Result result = MergingReport.Result.SUCCESS;
        NodeOperationType operationType = xmlElement.getOperationType();
        switch (operationType) {
            case REPLACE:
                // we should find at least one rejected twin.
                if (!isNodeOperationPresent(xmlElement, actionRecorder, ActionType.REJECTED)) {
                    iLogger.warning(""
                                    + "%s was tagged at %s:%d to replace another declaration "
                                    + "but no other declaration present",
                            xmlElement.getId(),
                            xmlElement.getDocument().getSourceLocation().print(true),
                            xmlElement.getPosition().getLine()
                    );
                    result = MergingReport.Result.WARNING;
                }
                break;
            case REMOVE:
            case REMOVE_ALL:
                // we should find at least one rejected twin.
                if (!isNodeOperationPresent(xmlElement, actionRecorder, ActionType.REJECTED)) {
                    iLogger.warning(""
                                    + "%s was tagged at %s:%d to remove other declarations "
                                    + "but no other declaration present",
                            xmlElement.getId(),
                            xmlElement.getDocument().getSourceLocation().print(true),
                            xmlElement.getPosition().getLine()
                    );
                    result = MergingReport.Result.WARNING;
                }
                break;
        }
        validateAttributes(xmlElement, actionRecorder, iLogger);
        for (XmlElement child : xmlElement.getMergeableElements()) {
            if (validate(child, actionRecorder, iLogger) == MergingReport.Result.WARNING) {
                result = MergingReport.Result.WARNING;
            }
        }
        return result;
    }


    /**
     * Verifies that all merging attributes on a passed xml element were applied.
     */
    private static void validateAttributes(
            XmlElement xmlElement,
            ActionRecorder actionRecorder,
            ILogger iLogger) {

        Collection<Map.Entry<XmlNode.NodeName, AttributeOperationType>> attributeOperations
                = xmlElement.getAttributeOperations();
        for (Map.Entry<XmlNode.NodeName, AttributeOperationType> attributeOperation :
                attributeOperations) {
            switch (attributeOperation.getValue()) {
                case REMOVE:
                    if (!isAttributeOperationPresent(
                            xmlElement, attributeOperation, actionRecorder,
                            ActionType.REJECTED)) {
                        iLogger.warning(""
                                        + "%s@%s was tagged at %s:%d to remove other declarations "
                                        + "but no other declaration present",
                                xmlElement.getId(),
                                attributeOperation.getKey(),
                                xmlElement.getDocument().getSourceLocation().print(true),
                                xmlElement.getPosition().getLine()
                        );
                    }
                    break;
                case REPLACE:
                    if (!isAttributeOperationPresent(
                            xmlElement, attributeOperation, actionRecorder,
                            ActionType.REJECTED)) {
                        iLogger.warning(""
                                        + "%s@%s was tagged at %s:%d to replace other declarations "
                                        + "but no other declaration present",
                                xmlElement.getId(),
                                attributeOperation.getKey(),
                                xmlElement.getDocument().getSourceLocation().print(true),
                                xmlElement.getPosition().getLine()
                        );
                    }
                    break;
            }
        }

    }

    /**
     * Check in our list of applied actions that a particular {@link ActionRecorder.ActionType}
     * action was recorded on the passed element.
     * @return true if it was applied, false otherwise.
     */
    private static boolean isNodeOperationPresent(XmlElement xmlElement,
            ActionRecorder actionRecorder,
            ActionType actionType) {

        ActionRecorder.DecisionTreeRecord decisionTreeRecord = actionRecorder.getAllRecords()
                .get(xmlElement.getId());
        for (ActionRecorder.NodeRecord nodeRecord : decisionTreeRecord.getNodeRecords()) {
            if (nodeRecord.getActionType() == actionType) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check in our list of attribute actions that a particular {@link ActionRecorder.ActionType}
     * action was recorded on the passed element.
     * @return true if it was applied, false otherwise.
     */
    private static boolean isAttributeOperationPresent(XmlElement xmlElement,
            Map.Entry<XmlNode.NodeName, AttributeOperationType> attributeOperation,
            ActionRecorder actionRecorder,
            ActionType actionType) {

        ActionRecorder.DecisionTreeRecord decisionTreeRecord = actionRecorder.getAllRecords()
                .get(xmlElement.getId());
        for (ActionRecorder.AttributeRecord attributeRecord : decisionTreeRecord
                .getAttributeRecords(attributeOperation.getKey())) {
            if (attributeRecord.getActionType() == actionType) {
                return true;
            }
        }
        return false;
    }
}
