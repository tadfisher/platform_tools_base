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

/**
 * Defines an operation on 1 to many nodes within one element.
 */
public class NodeOperation {

    private final NodeOperationType mNodeOperationType;
    private final XmlNode owner;

    public NodeOperation(NodeOperationType nodeOperationType, XmlNode owner) {
        mNodeOperationType = nodeOperationType;
        this.owner = owner;
    }

    public NodeOperationType getNodeOperationType() {
        return mNodeOperationType;
    }
}
