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
 * Defines attributes operations as it can be provided by users through attributes on the target
 * xml element.
 *
 * For instance :
 * <pre>
 *     <uses-permission
 *         android:name="android.permission.CAMERA"
 *         android:maxSdkVersion=25
 *         tools:replace="android:maxSdkVersion"/>
 * </pre>
 *
 * The operation type is provided as part of the attribute name itself, so you can find
 * tools:remove, tools:replace, tools:strict. The value of the attribute is a comma separated list
 * of attribute names on which the operation applies.
 */
public enum AttributeOperationType {

    /**
     * Removes the attributes from all further merging activity/
     */
    REMOVE,

    /**
     * Replace the attributes values with the provided one. (Will generate a merging error if no
     * new value is provided).
     */
    REPLACE,

    /**
     * The attributes should not be specified by any other similar xml elements declaration.
     */
    STRICT
}
