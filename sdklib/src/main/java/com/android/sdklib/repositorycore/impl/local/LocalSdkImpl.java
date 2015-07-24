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
import com.android.annotations.VisibleForTesting;
import com.android.sdklib.io.FileOp;
import com.android.sdklib.io.IFileOp;
import com.android.sdklib.repositorycore.api.FallbackLocalSdk;
import com.android.sdklib.repositorycore.api.LocalPackage;
import com.android.sdklib.repositorycore.api.LocalSdk;
import com.android.sdklib.repositorycore.api.SdkManager;
import com.android.sdklib.repositorycore.api.SdkSchemaExtension;
import com.android.sdklib.repositorycore.impl.SdkConstants;
import com.android.sdklib.repositorycore.impl.generated.v1.GenericType;
import com.android.sdklib.repositorycore.impl.generated.v1.RepositoryType;
import com.android.sdklib.repositorycore.impl.manager.SdkManagerImpl;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * LocalSdk is used in: LintDriver LintClient SdkMavenRepository ApiLookup (lint)
 */
public final class LocalSdkImpl implements LocalSdk {

    private Map<String, LocalPackage> mPackages = null;

    private final File mSdkRoot;

    private final SdkManager mSdkManager;
    private final IFileOp mFop;
    private final FallbackLocalSdk mFallback;

    private static final Logger LOG = Logger.getLogger(LocalSdkImpl.class.getName());

    private static final int MAX_SCAN_DEPTH = 10;

    // Should only be used internally by repositorycore
    public LocalSdkImpl(@NonNull File root, SdkManager manager, FallbackLocalSdk fallback) {
        this(root, manager, fallback, new FileOp());
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public LocalSdkImpl(@NonNull File root, SdkManager manager, FallbackLocalSdk fallback, IFileOp fop) {
        mSdkRoot = root;
        mSdkManager = manager;
        mFop = fop;
        mFallback = fallback;
    }

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

    private void collectPackages(Map<String, LocalPackage> packages, @NonNull File root,
            int depth) {
        if (depth > MAX_SCAN_DEPTH) {
            return;
        }
        File packageXml = new File(root, SdkConstants.PACKAGE_XML_FN);
        LocalPackage p;
        if (mFop.exists(packageXml)) {
            p = parsePackage(packageXml);
        } else if (null != (p = mFallback.parseLegacyLocalPackage(root))) {
            p = new LocalPackageImpl(p);
            writePackage((LocalPackageImpl)p, packageXml);
        } else {
            for (File f : mFop.listFiles(root)) {
                if (mFop.isDirectory(f)) {
                    collectPackages(packages, f, depth + 1);
                }
            }
        }
        if (p != null) {
            packages.put(p.getPath(), p);
        }
    }

    private void writePackage(LocalPackageImpl p, File packageXml) {
        OutputStream fos = null;
        try {
            fos = mFop.newFileOutputStream(packageXml);
            SdkSchemaExtension.marshal(p.getMeta(),
                    mSdkManager.getSchemaExtensions().values(), fos);
        } catch (FileNotFoundException e) {
            // TODO
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // TODO
                    e.printStackTrace();
                }
            }
        }

    }

    @Nullable
    private LocalPackage parsePackage(File packageXml) {
        RepositoryType repo;
        try {
            repo = SdkSchemaExtension.unmarshal(mFop.newFileInputStream(packageXml),
                    mSdkManager.getSchemaExtensions().values());
        } catch (FileNotFoundException e) {
            LOG.severe(String.format("XML file %s doesn't exist", packageXml));
            return null;
        }
        if (repo == null) {
            LOG.severe(String.format("Failed to parse %s", packageXml));
            return null;
        } else {
            GenericType generic = repo.getPackage().get(0);
            return new LocalPackageImpl(generic);
        }
    }

}
