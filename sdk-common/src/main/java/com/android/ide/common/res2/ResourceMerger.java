/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.ide.common.res2;

import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.TAG_DECLARE_STYLEABLE;
import static com.android.ide.common.res2.DataFile.FileType;
import static com.android.ide.common.res2.ResourceFile.ATTR_QUALIFIER;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Implementation of {@link DataMerger} for {@link ResourceSet}, {@link ResourceItem}, and
 * {@link ResourceFile}.
 */
public class ResourceMerger extends DataMerger<ResourceItem, ResourceFile, ResourceSet> {

    /**
     * Override of the normal ResourceItem to handle merged item cases.
     * This is mostly to deal with items that do not have a matching source file.
     * This override the
     */
    private static class MergedResourceItem extends ResourceItem {

        @NonNull
        private final String mQualifiers;

        /**
         * Constructs the object with a name, type and optional value.
         *
         * Note that the object is not fully usable as-is. It must be added to a ResourceFile first.
         *
         * @param name  the name of the resource
         * @param type  the type of the resource
         * @param qualifiers the qualifiers of the resource
         * @param value an optional Node that represents the resource value.
         */
        public MergedResourceItem(
                @NonNull String name,
                @NonNull ResourceType type,
                @NonNull String qualifiers,
                @Nullable Node value) {
            super(name, type, value);
            mQualifiers = qualifiers;
        }

        @NonNull
        @Override
        public String getQualifiers() {
            return mQualifiers;
        }

        @Override
        @NonNull
        public FileType getSourceType() {
            return FileType.MULTI;
        }
    }

    /**
     * Map of items that are purely results of merges (ie item that made up of several
     * original items). The first map key is the associated qualifier for the items,
     * the second map key is the item name.
     */
    protected final Map<String, Map<String, ResourceItem>> mMergedItems = Maps.newHashMap();


    @Override
    protected ResourceSet createFromXml(Node node) {
        ResourceSet set = new ResourceSet("");
        return (ResourceSet) set.createFromXml(node);
    }

    @Override
    protected boolean requiresMerge(@NonNull String dataItemKey) {
        return dataItemKey.startsWith("declare-styleable/");
    }

    @Override
    protected void mergeItems(
            @NonNull String dataItemKey,
            @NonNull List<ResourceItem> items,
            @NonNull MergeConsumer<ResourceItem> consumer) throws MergingException {
        boolean mustCompute = false;
        for (ResourceItem item : items) {
            mustCompute |= item.getStatus() != 0;
        }

        if (mustCompute) {
            ResourceItem sourceItem = items.get(0);

            try {
                DocumentBuilder builder = mFactory.newDocumentBuilder();
                Document document = builder.newDocument();
                Node declareStyleableNode = document.createElement(TAG_DECLARE_STYLEABLE);

                Attr nameAttr = document.createAttribute(ATTR_NAME);
                String itemName = sourceItem.getName();
                nameAttr.setValue(itemName);
                declareStyleableNode.getAttributes().setNamedItem(nameAttr);

                // keep track of attr added to it.
                Set<String> attrs = Sets.newHashSet();

                for (ResourceItem item : items) {
                    if (!item.isRemoved()) {
                        Node oldDeclareStyleable = item.getValue();
                        if (oldDeclareStyleable != null) {
                            NodeList children = oldDeclareStyleable.getChildNodes();
                            for (int i = 0; i < children.getLength(); i++) {
                                Node attrNode = children.item(i);
                                if (attrNode.getNodeType() != Node.ELEMENT_NODE) {
                                    continue;
                                }

                                // get the name
                                NamedNodeMap attributes = attrNode.getAttributes();
                                nameAttr = (Attr) attributes.getNamedItemNS(null, ATTR_NAME);
                                if (nameAttr == null) {
                                    continue;
                                }

                                String name = nameAttr.getNodeValue();
                                if (attrs.contains(name)) {
                                    continue;
                                }

                                // duplicate the node.
                                attrs.add(name);
                                Node newAttrNode = NodeUtils.duplicateNode(document, attrNode);
                                declareStyleableNode.appendChild(newAttrNode);
                            }
                        }
                    }
                }

                String qualifier = sourceItem.getQualifiers();

                // always write it for now.
                MergedResourceItem newItem = new MergedResourceItem(
                        itemName,
                        sourceItem.getType(),
                        qualifier,
                        declareStyleableNode);

                // get the matching mergedItem
                ResourceItem previouslyWrittenItem = getMergedItem(qualifier, itemName);

                if (previouslyWrittenItem == null ||
                        !newItem.compareValueWith(previouslyWrittenItem)) {
                    newItem.setTouched();
                    addMergedItem(qualifier, newItem);
                    consumer.addItem(newItem);
                }
            } catch (ParserConfigurationException e) {
                throw new MergingException(e);
            }
        }
    }

    @Nullable
    private ResourceItem getMergedItem(@NonNull String qualifiers, @NonNull String name) {
        Map<String, ResourceItem> map = mMergedItems.get(qualifiers);
        if (map != null) {
            return map.get(name);
        }

        return null;
    }

    @Override
    protected void loadMergedItems(@NonNull Node mergedItemsNode) {
        // loop on the qualifiers.
        NodeList configurationList = mergedItemsNode.getChildNodes();

        for (int j = 0, n2 = configurationList.getLength(); j < n2; j++) {
            Node configuration = configurationList.item(j);

            if (configuration.getNodeType() != Node.ELEMENT_NODE ||
                    !NODE_CONFIGURATION.equals(configuration.getLocalName())) {
                continue;
            }

            // get the qualifier value.
            Attr qualifierAttr = (Attr) configuration.getAttributes().getNamedItem(
                    ATTR_QUALIFIER);
            if (qualifierAttr == null) {
                continue;
            }

            String qualifier = qualifierAttr.getValue();

            // get the resource items
            NodeList itemList = mergedItemsNode.getChildNodes();

            for (int k = 0, n3 = itemList.getLength(); k < n3; k++) {
                Node itemNode = itemList.item(k);

                if (configuration.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                ResourceItem item = ValueResourceParser2.getResource(itemNode, null);
                if (item != null) {
                    addMergedItem(qualifier, item);
                }
            }
        }
    }

    @Override
    protected void writeMergedItems(Document document, Node rootNode) {
        Node mergedItemsNode = document.createElement(NODE_MERGED_ITEMS);
        rootNode.appendChild(mergedItemsNode);

        for (String qualifier : mMergedItems.keySet()) {
            Map<String, ResourceItem> itemMap = mMergedItems.get(qualifier);

            Node qualifierNode = document.createElement(NODE_CONFIGURATION);
            NodeUtils.addAttribute(document, qualifierNode, null, ATTR_QUALIFIER,
                    qualifier);

            mergedItemsNode.appendChild(qualifierNode);

            for (ResourceItem item : itemMap.values()) {
                Node adoptedNode = item.getAdoptedNode(document);
                if (adoptedNode != null) {
                    qualifierNode.appendChild(adoptedNode);
                }
            }
        }
    }

    private void addMergedItem(@NonNull String qualifier, @NonNull ResourceItem item) {
        Map<String, ResourceItem> map = mMergedItems.get(qualifier);
        if (map == null) {
            map = Maps.newHashMap();
            mMergedItems.put(qualifier, map);
        }

        map.put(item.getName(), item);
    }

}
