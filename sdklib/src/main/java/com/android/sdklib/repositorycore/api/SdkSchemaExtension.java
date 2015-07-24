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
package com.android.sdklib.repositorycore.api;

import com.android.sdklib.repositorycore.impl.generated.v1.GenericType;
import com.android.sdklib.repositorycore.impl.generated.v1.LicenseType;
import com.android.sdklib.repositorycore.impl.generated.v1.ObjectFactory;
import com.android.sdklib.repositorycore.impl.generated.v1.RepositoryType;
import com.android.sdklib.repositorycore.impl.generated.v1.UsesLicenseType;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * Created by jbakermalone on 7/22/15.
 */
// TODO: pull out interface?
public class SdkSchemaExtension {

    public static final String NODE_IMPORT = "import";

    public static final String NODE_INCLUDE = "include";

    public static final String NODE_SCHEMA = "schema";

    public static final String NODE_ELEMENT = "element";

    public static final String ATTR_SCHEMA_LOCATION = "schemaLocation";

    public static final String ATTR_TARGET_NAMESPACE = "targetNamespace";

    public static final String ATTR_NAME = "name";

    private final Class mObjectFactory;

    private final File mMyXsd;

    private final String mNamespace;

    private final List<File> mAllXsds = Lists.newArrayList();

    public SdkSchemaExtension(Class objectFactory, File xsd) {
        mObjectFactory = objectFactory;
        mMyXsd = xsd;

        // Parse the schema and find any imports or includes so we can return them as well.
        List<String> streamFiles = findAttrValue(mMyXsd, ATTR_SCHEMA_LOCATION,
                ImmutableSet.of(NODE_IMPORT, NODE_INCLUDE));
        for (String fileName : streamFiles) {
            File file = new File(fileName);
            if (!file.isAbsolute()) {
                // It's relative, so must be relative to referring xsd
                file = new File(mMyXsd.getParentFile(), fileName);
            }
            mAllXsds.add(file);
        }
        mAllXsds.add(mMyXsd);
        mNamespace = findAttrValue(mMyXsd, ATTR_TARGET_NAMESPACE,
                ImmutableSet.of(NODE_SCHEMA)).get(0);
    }

    public Class getObjectFactory() {
        return mObjectFactory;
    }

    // TODO: needed?
    public File getPrimaryXsd() {
        return mMyXsd;
    }

    public List<File> getAllXsds() {
        return mAllXsds;
    }

    public String getNamespace() {
        return mNamespace;
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
    }

    private static JAXBContext getContext(Collection<SdkSchemaExtension> possibleExtensions) {
        List<String> packages = Lists.newArrayList();
        for (SdkSchemaExtension extension : possibleExtensions) {
            packages.add(extension.getObjectFactory().getPackage().getName());
        }
        JAXBContext jc = null;
        try {
            jc = JAXBContext.newInstance(Joiner.on(":").join(packages));
        } catch (JAXBException e1) {
            e1.printStackTrace();
        }
        return jc;
    }

    private static Schema getSchema(Collection<SdkSchemaExtension> possibleExtensions) {
        Schema schema = null;
        SchemaFactory sf =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        List<StreamSource> sources = Lists.newArrayList();
        for (SdkSchemaExtension extension : possibleExtensions) {
            for (File xsd : extension.getAllXsds()) {
                try {
                    sources.add(new StreamSource(new FileInputStream(xsd)));
                } catch (FileNotFoundException e) {
                    // TODO
                    e.printStackTrace();
                }
            }
        }
        try {
            schema = sf.newSchema(sources.toArray(new StreamSource[sources.size()]));
        } catch (SAXException e) {
            // TODO
            e.printStackTrace();
        }
        return schema;
    }

    public static RepositoryType unmarshal(InputStream xml,
            Collection<SdkSchemaExtension> possibleExtensions) {
        JAXBContext context = getContext(possibleExtensions);
        Schema schema = getSchema(possibleExtensions);
        if (context == null) {
            return null;
        }
        Unmarshaller u = null;
        try {
            u = context.createUnmarshaller();
            u.setSchema(schema);
            return u.unmarshal(new StreamSource(xml), RepositoryType.class).getValue();
        } catch (JAXBException e) {
            // TODO
            e.printStackTrace();
        }
        return null;
    }

    public static void marshal(GenericType p,
            Collection<SdkSchemaExtension> possibleExtensions,
            OutputStream out) {
        ObjectFactory commonFactory = new ObjectFactory();
        RepositoryType repo = commonFactory.createRepositoryType();
        repo.getPackage().add(p);
        UsesLicenseType usesLicense = p.getUsesLicense();
        if (usesLicense != null) {
            repo.getLicense().add((LicenseType) usesLicense.getRef());
        }
        JAXBContext context = getContext(possibleExtensions);
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(commonFactory.createRepository(repo), out);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
