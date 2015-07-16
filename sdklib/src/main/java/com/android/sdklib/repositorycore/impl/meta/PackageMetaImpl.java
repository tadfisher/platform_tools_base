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
package com.android.sdklib.repositorycore.impl.meta;

import com.android.annotations.NonNull;
import com.android.sdklib.repository.License;
import com.android.sdklib.repository.PreciseRevision;
import com.android.sdklib.repositorycore.api.ContentDetails;
import com.android.sdklib.repositorycore.api.PackageMeta;
import com.android.sdklib.repositorycore.impl.generated.v1.GenericDetailsType;
import com.android.sdklib.repositorycore.impl.generated.v1.GenericType;
import com.android.sdklib.repositorycore.impl.installer.PackageInstaller;
import com.android.sdklib.repositorycore.api.ContentDetails;

import org.w3c.dom.Element;

import java.util.List;

/**
 * Created by jbakermalone on 7/22/15.
 */
public final class PackageMetaImpl implements PackageMeta {
    // TODO: delete this in favor of generated classes


    // TODO: use the generated classes directly? Or in place of Data?
    private final class Data {

        private final String mPath;

        private final PreciseRevision mRevision;

        private final ContentDetails mContentDetails;

        private final PackageInstaller mInstaller;

        private final String mName;

        private final List<PackageDependency> mDependencies;

        private final boolean mObsolete;

        private final License mLicense;

        private Data(String path, PreciseRevision revision, ContentDetails contentDetails,
                PackageInstaller installer, String name, List<PackageDependency> dependencies,
                boolean obsolete, License license) {
            mPath = path;
            mRevision = revision;
            mContentDetails = contentDetails;
            mInstaller = installer;
            mName = name;
            mDependencies = dependencies;
            mObsolete = obsolete;
            mLicense = license;
        }
    }

    private final Data mData;

    public PackageMetaImpl(@NonNull GenericType p) {
        mData = read(p);
    }

    @Override
    public ContentDetails getContentDetails() {
        return mData.mContentDetails;
    }

    @Override
    public List<PackageDependency> getDependencies() {
        return mData.mDependencies;
    }

    public PackageInstaller getInstaller() {
        return mData.mInstaller;
    }

    @Override
    public License getLicense() {
        return mData.mLicense;
    }

    @Override
    public String getName() {
        return mData.mName;
    }

    @Override
    public boolean isObsolete() {
        return mData.mObsolete;
    }

    @Override
    public String getPath() {
        return mData.mPath;
    }

    @Override
    public PreciseRevision getRevision() {
        return mData.mRevision;
    }

    public String getXmlSnippet() {
        // TODO format and print xml
        return null;
    }

    private Data read(GenericType p) {
/*
        return new Data(path, revision, new ContentDetails() {
        })*/
        return null;
    }

    @Override
    public int compareTo(PackageMeta o) {
        // TODO
        return 0;
    }

}
