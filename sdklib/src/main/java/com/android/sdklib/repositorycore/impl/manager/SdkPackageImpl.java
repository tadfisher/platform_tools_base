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

package com.android.sdklib.repositorycore.impl.manager;

import com.android.sdklib.repository.License;
import com.android.sdklib.repository.PreciseRevision;
import com.android.sdklib.repositorycore.api.SdkPackage;
import com.android.sdklib.repositorycore.api.SdkSource;
import com.android.sdklib.repositorycore.impl.generated.ArchiveType;
import com.android.sdklib.repositorycore.impl.generated.DependencyType;
import com.android.sdklib.repositorycore.impl.generated.GenericType;
import com.android.sdklib.repositorycore.impl.generated.RevisionType;
import com.android.sdklib.repositorycore.impl.generated.TypeDetails;
import com.android.sdklib.repositorycore.impl.remote.SdkSourceImpl;

import java.util.Collection;

/**
 * Created by jbakermalone on 7/24/15.
 */
public class SdkPackageImpl implements SdkPackage {

    protected final GenericType mPackage;

    private final PreciseRevision mRevision;
    private final License mLicense;

    public SdkPackageImpl(GenericType p, License license) {
        mPackage = p;
        RevisionType revision = p.getRevision();
        mRevision = new PreciseRevision(revision.getMajor(), revision.getMinor(),
                revision.getMicro(), revision.getPreview());
        mLicense = license;
    }

    // TODO: do we need this?
    @Override
    public GenericType getMeta() {
        return mPackage;
    }

    @Override
    public TypeDetails getTypeDetails() {
        return mPackage.getTypeDetails();
    }

    @Override
    public PreciseRevision getRevision() {
        return mRevision;
    }

    @Override
    public String getUiName() {
        return mPackage.getUiName();
    }

    @Override
    public License getLicense() {
        return mLicense;
    }

    @Override
    public Collection<DependencyType> getDependencies() {
        return mPackage.getDependencies().getDependency();
    }

    @Override
    public String getPath() {
        return mPackage.getPath();
    }

    @Override
    public boolean isObsolete() {
        return Boolean.parseBoolean(mPackage.getObsolete());
    }

    @Override
    public int compareTo(SdkPackage o) {
        // TODO
        return 0;
    }
}
