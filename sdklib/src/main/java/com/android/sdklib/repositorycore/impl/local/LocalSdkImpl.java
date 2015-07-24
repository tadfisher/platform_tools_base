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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.repository.License;
import com.android.sdklib.repositorycore.api.LocalPackage;
import com.android.sdklib.repositorycore.api.LocalSdk;
import com.android.sdklib.repositorycore.api.SdkSourceCategory;
import com.android.sdklib.repositorycore.impl.SdkConstants;
import com.android.sdklib.repositorycore.impl.generated.GenericType;
import com.android.sdklib.repositorycore.impl.generated.RepositoryType;
import com.android.sdklib.repositorycore.impl.remote.RepoXsdUtil;
import com.android.sdklib.repositoryv2.generated.repository.PlatformDetailsType;
import com.google.common.collect.Maps;

import org.xml.sax.SAXException;

import java.io.File;
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

    private Map<String, LocalPackage> mPackages = null;

    private final File mSdkRoot;

    private static Map<File, LocalSdkImpl> sInstances = Maps.newHashMap();

    private static final Logger LOG = Logger.getLogger(LocalSdkImpl.class.getName());

    private static final int MAX_SCAN_DEPTH = 10;

    // Should only be used internally by repositorycore
    public LocalSdkImpl(@NonNull File root) {
        mSdkRoot = root;
    }

    // TODO: delete in favor of caching in SdkManager
    /*
    static LocalSdkImpl getInstance(File root) {
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
    }*/

    @Override
    public File getLocation() {
        return mSdkRoot;
    }

    @Override
    public Map<String, LocalPackage> getPackages() {
        if (mPackages == null) {
            Map<String, LocalPackage> packages = Maps.newHashMap();
            collectPackages(packages, mSdkRoot, 0);
            mPackages = packages;
        }
        return Collections.unmodifiableMap(mPackages);
    }

    @Override
    public void invalidate() {

    }

    private void collectPackages(Map<String, LocalPackage> packages, @NonNull File root, int depth) {
        if (depth > MAX_SCAN_DEPTH) {
            return;
        }
        for (File f : root.listFiles()) {
            if (f.getName().equals(SdkConstants.SOURCE_PROPERTIES_FN)) {
                LocalPackage p = parseLegacyPackage(f);
                packages.put(p.getPath(), p);
            }
            else if (f.getName().equals(SdkConstants.PACKAGE_XML_FN)) {
                LocalPackage p = parsePackage(f);
                packages.put(p.getPath(), p);
            }
            else if (f.isDirectory()) {
                collectPackages(packages, f, depth + 1);
            }
        }
    }

    private LocalPackageImpl parseLegacyPackage(File sourceProperties) {
        return null;
    }

    // TODO: refactor into common place with remote
    @Nullable
    private LocalPackage parsePackage(File packageXml) {
        Schema mySchema;
        SchemaFactory sf =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            SdkSourceCategory category = RepoXsdUtil.getSourceCategory(packageXml);
            if (category == null) {
                return null;
            }
            mySchema = sf.newSchema(RepoXsdUtil.getXsdStreams(category));
            JAXBContext jc = JAXBContext.newInstance(category.getObjectFactory());
            Unmarshaller u = jc.createUnmarshaller();
            u.setSchema(mySchema);
            RepositoryType repo = u.unmarshal(new StreamSource(packageXml), RepositoryType.class)
                    .getValue();
            List<GenericType> packages = repo.getPackage();
            if (packages.size() != 1) {
                // TODO: logging
                return null;
            }

            GenericType generic = packages.get(0);
            // TODO: move stuff out of try {}
            return new LocalPackageImpl(generic);
        }
        catch (SAXException e) {
            // TODO
        }
        catch (JAXBException e) {
            // TODO
        }
        return null;
    }
}
