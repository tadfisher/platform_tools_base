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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * Defines a single operation on 1 to many attributes of one xml element.
 */
public class AttributeOperation {

    private final AttributeOperationType mOperationType;
    private final ImmutableList<String> attributeNames;
    private final XmlNode owner;

    public AttributeOperation(AttributeOperationType operationType,
            String persistedListOfAttributeNames, XmlNode owner) {
        mOperationType = operationType;
        attributeNames = ImmutableList.copyOf(
                Splitter.on(',').trimResults().split(persistedListOfAttributeNames));
        this.owner = owner;
    }

    public AttributeOperationType getOperationType() {
        return mOperationType;
    }

    public ImmutableList<String> getAttributeNames() {
        return attributeNames;
    }
}
