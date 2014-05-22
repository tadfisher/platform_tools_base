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

import static com.android.manifmerger.Actions.ActionType;

import com.android.annotations.NonNull;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Validator that runs post merging activities and verifies that all "tools:" instructions
 * triggered an action by the merging tool.
 * <p>
 *
 * This is primarily to catch situations like a user entered a tools:remove="foo" directory on one
 * of its elements and that particular attribute was never removed during the merges possibly
 * indicating an unforeseen change of configuration.
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
     * @param mergingReport report for errors and warnings.
     */
    public static void validate(
            @NonNull XmlDocument xmlDocument,
            @NonNull MergingReport.Builder mergingReport) {

        Preconditions.checkNotNull(xmlDocument);
        Preconditions.checkNotNull(mergingReport);
        reOrderElements(xmlDocument.getRootNode());
        validate(xmlDocument.getRootNode(),
                mergingReport.getActionRecorder().build(),
                mergingReport);
    }

    /**
     * Reorder child elements. So far, only <application> can be moved last in the list of children
     * of the <manifest> element.
     *
     * @param xmlElement the root element of the manifest document.
     */
    private static void reOrderElements(XmlElement xmlElement) {

        // look up application element first.
        Optional<XmlElement> element = xmlElement
                .getNodeByTypeAndKey(ManifestModel.NodeTypes.APPLICATION, null);
        if (!element.isPresent()) {
            return;
        }
        XmlElement applicationElement = element.get();

        // remove the application element and add it back, it will be automatically placed last.
        List<Node> comments = XmlElement.getLeadingComments(applicationElement.getXml());

        // also move the application's comments if any.
        for (Node comment : comments) {
            xmlElement.getXml().removeChild(comment);
            xmlElement.getXml().appendChild(comment);
        }
        // remove from its current location and add it back right before the
        // application element.
        xmlElement.getXml().removeChild(applicationElement.getXml());
        xmlElement.getXml().appendChild(applicationElement.getXml());
    }

    /**
     * Validate an xml element and recursively its children elements, ensuring that all merging
     * instructions were applied.
     *
     * @param xmlElement xml element to validate.
     * @param actions the actions recorded during the merging activities.
     * @param mergingReport report for errors and warnings.
     * instructions were applied once or {@link MergingReport.Result#WARNING} otherwise.
     */
    private static void validate(
            XmlElement xmlElement,
            Actions actions,
            MergingReport.Builder mergingReport) {

        NodeOperationType operationType = xmlElement.getOperationType();
        switch (operationType) {
            case REPLACE:
                // we should find at least one rejected twin.
                if (!isNodeOperationPresent(xmlElement, actions, ActionType.REJECTED)) {
                    xmlElement.addMessage(mergingReport, MergingReport.Record.Severity.WARNING,
                            String.format(
                                    "%1$s was tagged at %2$s:%3$d to replace another declaration "
                                            + "but no other declaration present",
                                    xmlElement.getId(),
                                    xmlElement.getDocument().getSourceLocation().print(true),
                                    xmlElement.getPosition().getLine()
                            ));
                }
                break;
            case REMOVE:
            case REMOVE_ALL:
                // we should find at least one rejected twin.
                if (!isNodeOperationPresent(xmlElement, actions, ActionType.REJECTED)) {
                    xmlElement.addMessage(mergingReport, MergingReport.Record.Severity.WARNING,
                            String.format(
                                    "%1$s was tagged at %2$s:%3$d to remove other declarations "
                                            + "but no other declaration present",
                                    xmlElement.getId(),
                                    xmlElement.getDocument().getSourceLocation().print(true),
                                    xmlElement.getPosition().getLine()
                            ));
                }
                break;
        }
        validateAttributes(xmlElement, actions, mergingReport);
        validateAndroidAttributes(xmlElement, mergingReport);
        for (XmlElement child : xmlElement.getMergeableElements()) {
            validate(child, actions, mergingReport);
        }
    }


    /**
     * Verifies that all merging attributes on a passed xml element were applied.
     */
    private static void validateAttributes(
            XmlElement xmlElement,
            Actions actions,
            MergingReport.Builder mergingReport) {

        Collection<Map.Entry<XmlNode.NodeName, AttributeOperationType>> attributeOperations
                = xmlElement.getAttributeOperations();
        for (Map.Entry<XmlNode.NodeName, AttributeOperationType> attributeOperation :
                attributeOperations) {
            switch (attributeOperation.getValue()) {
                case REMOVE:
                    if (!isAttributeOperationPresent(
                            xmlElement, attributeOperation, actions, ActionType.REJECTED)) {
                        xmlElement.addMessage(mergingReport, MergingReport.Record.Severity.WARNING,
                                String.format(
                                        "%1$s@%2$s was tagged at %3$s:%4$d to remove other"
                                                + " declarations but no other declaration present",
                                        xmlElement.getId(),
                                        attributeOperation.getKey(),
                                        xmlElement.getDocument().getSourceLocation().print(true),
                                        xmlElement.getPosition().getLine()
                                ));
                    }
                    break;
                case REPLACE:
                    if (!isAttributeOperationPresent(
                            xmlElement, attributeOperation, actions, ActionType.REJECTED)) {
                        xmlElement.addMessage(mergingReport, MergingReport.Record.Severity.WARNING,
                                String.format(
                                        "%1$s@%2$s was tagged at %3$s:%4$d to replace other"
                                                + " declarations but no other declaration present",
                                        xmlElement.getId(),
                                        attributeOperation.getKey(),
                                        xmlElement.getDocument().getSourceLocation().print(true),
                                        xmlElement.getPosition().getLine()
                                ));
                    }
                    break;
            }
        }

    }

    /**
     * Check in our list of applied actions that a particular {@link com.android.manifmerger.Actions.ActionType}
     * action was recorded on the passed element.
     * @return true if it was applied, false otherwise.
     */
    private static boolean isNodeOperationPresent(XmlElement xmlElement,
            Actions actions,
            ActionType action) {

        for (Actions.NodeRecord nodeRecord : actions.getNodeRecords(xmlElement.getId())) {
            if (nodeRecord.getActionType() == action) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check in our list of attribute actions that a particular {@link com.android.manifmerger.Actions.ActionType}
     * action was recorded on the passed element.
     * @return true if it was applied, false otherwise.
     */
    private static boolean isAttributeOperationPresent(XmlElement xmlElement,
            Map.Entry<XmlNode.NodeName, AttributeOperationType> attributeOperation,
            Actions actions,
            ActionType action) {

        for (Actions.AttributeRecord attributeRecord : actions.getAttributeRecords(
                xmlElement.getId(), attributeOperation.getKey())) {
            if (attributeRecord.getActionType() == action) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates all {@link com.android.manifmerger.XmlElement} attributes belonging to the
     * {@link com.android.SdkConstants#ANDROID_URI} namespace.
     *
     * @param xmlElement xml element to check the attributes from.
     * @param mergingReport report for errors and warnings.
     */
    private static void validateAndroidAttributes(XmlElement xmlElement,
            MergingReport.Builder mergingReport) {

        for (XmlAttribute xmlAttribute : xmlElement.getAttributes()) {
            if (xmlAttribute.getModel() != null) {
                AttributeModel.Validator onWriteValidator = xmlAttribute.getModel()
                        .getOnWriteValidator();
                if (onWriteValidator != null) {
                    onWriteValidator.validates(
                            mergingReport, xmlAttribute, xmlAttribute.getValue());
                }
            }
        }
    }
}
