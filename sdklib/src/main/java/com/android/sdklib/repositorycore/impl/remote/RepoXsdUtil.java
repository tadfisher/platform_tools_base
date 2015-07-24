/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.sdklib.repositorycore.impl.remote;

import com.android.annotations.Nullable;
import com.android.sdklib.repositorycore.api.SdkSourceCategory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;

/**
 * Utilities related to the respository XSDs.
 */
// TODO: rename
public class RepoXsdUtil {

    public static final String NODE_IMPORT = "import";
    public static final String NODE_INCLUDE = "include";
    public static final String NODE_SCHEMA = "schema";
    public static final String NODE_ELEMENT = "element";

    public static final String ATTR_SCHEMA_LOCATION = "schemaLocation";
    public static final String ATTR_TARGET_NAMESPACE = "targetNamespace";
    public static final String ATTR_NAME = "name";

    private static final Map<String, SdkSourceCategory> sSourceCategoriesByNamespace = Maps.newHashMap();
    // TODO: delete
    private static final Map<String, SdkSourceCategory> sSourceCategoriesByElement = Maps.newHashMap();

    // TODO: move?
    public static void registerSourceCategory(SdkSourceCategory category)
            throws FileNotFoundException {

        // Closed by SaxParser
        //noinspection IOResourceOpenedButNotSafelyClosed
        InputStream xsd = new FileInputStream(category.getXsd());

        sSourceCategoriesByNamespace.put(findAttrValue(xsd, ATTR_TARGET_NAMESPACE, ImmutableSet.of(NODE_SCHEMA)).get(0), category);
        sSourceCategoriesByElement.put(findAttrValue(xsd, ATTR_NAME, ImmutableSet.of(NODE_ELEMENT)).get(0), category);
    }

    @Nullable
    public static SdkSourceCategory getSourceCategory(File xml) {
        final AtomicReference<SdkSourceCategory> category = new AtomicReference<SdkSourceCategory>();
        try {
            // TODO: use StAX
            SAXParserFactory.newInstance().newSAXParser().parse(xml, new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
                    if (category.get() != null) {
                        return;
                    }
                    String ns = name.substring(0, name.indexOf(':'));
                    SdkSourceCategory result = sSourceCategoriesByNamespace.get(ns);
                    if (result == null) {
                        ns = attributes.getValue("xmlns:" + ns);
                        result = sSourceCategoriesByNamespace.get(ns);
                    }
                    if (result != null) {
                        category.set(result);
                    }
                }
            });
        } catch (Exception e) {
            // TODO
            return null;
        }
        return category.get();
    }

    /**
     * Gets StreamSources for the given xml file, as well as any xsds imported or included by the main one.
     *
     */
    // TODO: change parameter type to something nicer
    public static StreamSource[] getXsdStreams(SdkSourceCategory category) {

        final List<StreamSource> streams = Lists.newArrayList();
        InputStream stream = null;
        try {
            // Closed by SAXParser
            //noinspection IOResourceOpenedButNotSafelyClosed
            stream = new FileInputStream(category.getXsd());
        } catch (FileNotFoundException e) {
            // TODO
        }

        // Parse the schema and find any imports or includes so we can return them as well.
        List<String> streamFiles = findAttrValue(stream, ATTR_SCHEMA_LOCATION, ImmutableSet.of(NODE_IMPORT, NODE_INCLUDE));
        for (String file : streamFiles) {
            streams.add(new StreamSource(new File(file)));
        }
        // create and add the first stream again, since SaxParser closes the original one
        try {
            // Closed by SAXParser
            //noinspection IOResourceOpenedButNotSafelyClosed
            stream = new FileInputStream(category.get().getXsd());
        } catch (FileNotFoundException e) {
            // TODO
        }
        streams.add(new StreamSource(stream));
        return streams.toArray(new StreamSource[streams.size()]);
    }

    private static List<String> findAttrValue(InputStream stream, final String attr, final Set<String> elems) {
        final List<String> result = Lists.newArrayList();
        try {
            // Currently transitive includes are not supported.
            SAXParserFactory.newInstance().newSAXParser().parse(stream, new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
                    name = name.substring(name.indexOf(':') + 1);
                    if (elems.contains(name)) {
                        String value = attributes.getValue(attr);
                        if (!Strings.isNullOrEmpty(value)) {
                            result.add(value);
                        }
                    }
                }
            });
        } catch (Exception e) {
            // Some implementations seem to return null on failure,
            // others throw an exception. We'll just return what we have so far..
            return result;
        }
        return result;
    }
}
