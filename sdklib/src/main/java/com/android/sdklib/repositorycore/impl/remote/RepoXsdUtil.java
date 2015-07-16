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

/**
 * Utilities related to the respository XSDs.
 */
// TODO: delete
public class RepoXsdUtil {
/*
    public static final String NODE_IMPORT = "import";

    public static final String NODE_INCLUDE = "include";

    public static final String NODE_SCHEMA = "schema";

    public static final String NODE_ELEMENT = "element";

    public static final String ATTR_SCHEMA_LOCATION = "schemaLocation";

    public static final String ATTR_TARGET_NAMESPACE = "targetNamespace";

    public static final String ATTR_NAME = "name";

    private static final Map<String, SdkSchemaExtension> sSourceCategoriesByNamespace = Maps
            .newHashMap();

    // TODO: delete
    private static final Map<String, SdkSchemaExtension> sSourceCategoriesByElement = Maps
            .newHashMap();

    // TODO: move?
    public static void registerSourceCategory(SdkSchemaExtension category)
            throws FileNotFoundException {

        sSourceCategoriesByNamespace.put(, category);
        sSourceCategoriesByElement
                .put(findAttrValue(category.getXsd(), ATTR_NAME, ImmutableSet.of(NODE_ELEMENT))
                        .get(0), category);
    }

    @Nullable
    public static SdkSchemaExtension getSourceCategory(File xml) {
        final AtomicReference<SdkSchemaExtension> category
                = new AtomicReference<SdkSchemaExtension>();
        try {
            // TODO: use StAX
            SAXParserFactory.newInstance().newSAXParser().parse(xml, new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String name,
                        Attributes attributes) throws SAXException {
                    if (category.get() != null) {
                        return;
                    }
                    String ns = name.substring(0, name.indexOf(':'));
                    SdkSchemaExtension result = sSourceCategoriesByNamespace.get(ns);
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
     * Gets StreamSources for the given xml file, as well as any xsds imported or included by the
     * main one.
     /
    // TODO: change parameter type to something nicer
    public static StreamSource[] getXsdStreams(SdkSchemaExtension category) {

        final List<StreamSource> streams = Lists.newArrayList();
        InputStream stream = null;

        // Parse the schema and find any imports or includes so we can return them as well.
        List<String> streamFiles = findAttrValue(category.getXsd(), ATTR_SCHEMA_LOCATION,
                ImmutableSet.of(NODE_IMPORT, NODE_INCLUDE));
        for (String fileName : streamFiles) {
            File file = new File(fileName);
            if (!file.isAbsolute()) {
                // It's relative, so must be relative to referring xsd
                file = new File(category.getXsd().getParentFile(), fileName);
            }
            streams.add(new StreamSource(file));
        }
        // create and add the first stream again, since SaxParser closes the original one
        try {
            // Closed by SAXParser
            //noinspection IOResourceOpenedButNotSafelyClosed
            stream = new FileInputStream(category.getXsd());
        } catch (FileNotFoundException e) {
            // TODO
        }
        streams.add(new StreamSource(stream));
        return streams.toArray(new StreamSource[streams.size()]);
    }

    private static List<String> findAttrValue(File input, final String attr,
            final Set<String> elems) {
        final List<String> result = Lists.newArrayList();
        try {
            InputStream stream = new FileInputStream(input);
            // Currently transitive includes are not supported.
            SAXParserFactory.newInstance().newSAXParser().parse(stream, new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String name,
                        Attributes attributes) throws SAXException {
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
    }*/
}
