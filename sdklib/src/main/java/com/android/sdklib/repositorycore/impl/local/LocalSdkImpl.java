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
package com.android.sdklib.repositorycore.impl.local;

import com.android.sdklib.repositorycore.api.LocalSdk;
import com.android.sdklib.repositorycore.impl.SdkConstants;
import com.android.sdklib.repositorycore.impl.SdkConstants;
import com.android.sdklib.repositorycore.impl.generated.RepositoryType;
import com.android.sdklib.repositorycore.impl.remote.RepoXsdUtil;
import com.android.sdklib.repositoryv2.generated.repository.PlatformDetailsType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * LocalSdk is used in: LintDriver LintClient SdkMavenRepository ApiLookup (lint)
 */
public final class LocalSdkImpl implements LocalSdk {

    private List<LocalPackageImpl> mPackages = null;

    private File mSdkRoot;

    private static Map<File, LocalSdkImpl> sInstances = Maps.newHashMap();

    private static final Logger LOG = Logger.getLogger(LocalSdkImpl.class.getName());

    private static final int MAX_SCAN_DEPTH = 10;

    private LocalSdkImpl(File root) {
        mSdkRoot = root;
    }

    public static LocalSdkImpl getInstance(File root) {
        root = root.getAbsoluteFile();
        LocalSdkImpl result = sInstances.get(root);
        if (result == null) {
            result = new LocalSdkImpl(root);
            if (!sInstances.isEmpty()) {
                StringBuilder warning =
                        new StringBuilder("Creating Local SDK when one already exists in:\n");
                for (File existing : sInstances.keySet()) {
                    warning.append(existing);
                    warning.append("\n");
                }
                LOG.warning(warning.toString());
            }
            sInstances.put(root, result);
        }
        return result;
    }

    @Override
    public File getLocation() {
        return mSdkRoot;
    }

    @Override
    public List<LocalPackageImpl> getPackages() {
        if (mPackages == null) {
            List<LocalPackageImpl> packages = Lists.newArrayList();
            collectPackages(packages, mSdkRoot, 0);
            mPackages = packages;
        }
        return Collections.unmodifiableList(mPackages);
    }

    private void collectPackages(List<LocalPackageImpl> packages, File root, int depth) {
        if (depth > MAX_SCAN_DEPTH) {
            return;
        }
        for (File f : root.listFiles()) {
            if (f.getName().equals(SdkConstants.SOURCE_PROPERTIES_FN)) {
                packages.add(parseLegacyPackage(f));
            }
            else if (f.getName().equals(SdkConstants.PACKAGE_XML_FN)) {
                packages.add(parsePackage(f));
            }
            else if (f.isDirectory()) {
                collectPackages(packages, f, depth + 1);
            }
        }
    }

    private LocalPackageImpl parseLegacyPackage(File sourceProperties) {
        return null;
    }

    private LocalPackageImpl parsePackage(File packageXml) {
        Schema mySchema;
        SchemaFactory sf =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            mySchema = sf.newSchema(RepoXsdUtil.getXsdStream("sdk-repository", 12));
            JAXBContext jc = JAXBContext.newInstance(PlatformDetailsType.class.getPackage().getName());
            Unmarshaller u = jc.createUnmarshaller();
            u.setSchema(mySchema);
            RepositoryType repo = u.unmarshal(new StreamSource(packageXml), RepositoryType.class)
                    .getValue();
        }
        catch (SAXException e) {
            // TODO
        }
        catch (JAXBException e) {
            // TODO
        }
    }
}
