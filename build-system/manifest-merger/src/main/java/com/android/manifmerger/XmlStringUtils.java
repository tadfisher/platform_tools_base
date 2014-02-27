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

import com.google.common.base.CaseFormat;

/**
 * Xml to constants and java centric names conversion utilities.
 */
public class XmlStringUtils {

    private XmlStringUtils(){}

    public static String constantNameToXmlName(String constantName) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, constantName);
    }

    public static String xmlNameToConstantName(String xmlName) {
        return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_UNDERSCORE, xmlName);
    }

    public static String camelCaseToXmlName(String camelCaseName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, camelCaseName);
    }

    public static String camelCaseToConstantName(String camelCaseName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, camelCaseName);
    }
}
