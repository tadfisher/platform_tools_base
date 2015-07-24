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
package com.android.sdklib.repositoryv2;

import com.android.sdklib.repositorycore.impl.generated.GenericType;
import com.android.sdklib.repositorycore.impl.generated.LicenseType;
import com.android.sdklib.repositorycore.impl.generated.RepositoryType;
import com.android.sdklib.repositorycore.impl.remote.RepoXsdUtil;
import com.android.sdklib.repositoryv2.generated.repository.PlatformDetailsType;
import com.google.common.collect.Maps;

import junit.framework.TestCase;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * Created by jbakermalone on 7/23/15.
 */
public class UnmarshalTest extends TestCase {

    public void testLoadRepo12() throws Exception {
        String filename = "/com/android/sdklib/testdata/repository_sample_12.xml";
        InputStream xmlStream = getClass().getResourceAsStream(filename);
        assertNotNull("Missing test file: " + filename, xmlStream);

        Schema mySchema;
        SchemaFactory sf =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        mySchema = sf.newSchema(RepoXsdUtil.getXsdStream("sdk-repository", 12));
        JAXBContext jc = JAXBContext.newInstance(PlatformDetailsType.class.getPackage().getName());
        Unmarshaller u = jc.createUnmarshaller();
        u.setSchema(mySchema);
        RepositoryType repo = u.unmarshal(new StreamSource(xmlStream), RepositoryType.class)
                .getValue();
        List<LicenseType> licenses = repo.getLicense();
        assertEquals(licenses.size(), 2);
        Map<String, String> licenseMap = Maps.newHashMap();
        for (LicenseType license : licenses) {
            licenseMap.put(license.getId(), license.getValue());
        }
        assertEquals(licenseMap.get("license1").trim(), "This is the license\n        for this platform.");
        assertEquals(licenseMap.get("license2").trim(),
                "Licenses are only of type 'text' right now, so this is implied.");

        List<GenericType> packages = repo.getPackage();
        assertEquals(packages.size(), 2);
        Map<String, GenericType> packageMap = Maps.newHashMap();
        for (GenericType p : packages) {
            packageMap.put(p.getPath(), p);
        }

        GenericType platform22 = packageMap.get("platforms;android-22");
        assertEquals(platform22.getUiName(), "The first Android platform ever");
        assertTrue(platform22.getTypeDetails() instanceof PlatformDetailsType);
        PlatformDetailsType details = (PlatformDetailsType)platform22.getTypeDetails();
        assertEquals(details.getApiLevel(), 1);
        assertEquals(details.getLayoutlib().getApi(), 5);
        assertEquals((int)details.getLayoutlib().getRevision(), 0);
    }


    public void testSchemaResolver() throws Exception {
        String filename = "/com/android/sdklib/testdata/repository_sample_12.xml";
        InputStream xmlStream = getClass().getResourceAsStream(filename);
        assertNotNull("Missing test file: " + filename, xmlStream);

        Schema mySchema;
        SchemaFactory sf =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        sf.setResourceResolver(new LSResourceResolver() {
            @Override
            public LSInput resolveResource(String type, String namespaceURI, String publicId,
                    String systemId, String baseURI) {
                return null;
            }
        });
        mySchema = sf.newSchema();
        JAXBContext jc = JAXBContext.newInstance(PlatformDetailsType.class.getPackage().getName());
        Unmarshaller u = jc.createUnmarshaller();
        u.setSchema(mySchema);
        RepositoryType repo = u.unmarshal(new StreamSource(xmlStream), RepositoryType.class)
                .getValue();

    }
}
