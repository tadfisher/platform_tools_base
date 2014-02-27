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

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Responsible for loading XML files.
 */
public class XmlLoader {

    public final static String TOOLS_URI = "http://schemas.android.com/apk/res/android/tools";

    public XmlDocument load(File xmlFile)
            throws IOException, SAXException, ParserConfigurationException {
        InputStream inputStream = new BufferedInputStream(new FileInputStream(xmlFile));

        PositionXmlParser positionXmlParser = new PositionXmlParser();
        Document domDocument = positionXmlParser.parse(inputStream);
        // TODO(jedo): need to pass the file location to XmlDocument for proper logging.
        return domDocument != null
            ? new XmlDocument(positionXmlParser, domDocument.getDocumentElement())
            : null;
    }

    public XmlDocument load(String xml)
            throws IOException, SAXException, ParserConfigurationException {
        PositionXmlParser positionXmlParser = new PositionXmlParser();
        Document domDocument = positionXmlParser.parse(xml);
        return domDocument != null
                ? new XmlDocument(positionXmlParser, domDocument.getDocumentElement())
                : null;
    }
}
