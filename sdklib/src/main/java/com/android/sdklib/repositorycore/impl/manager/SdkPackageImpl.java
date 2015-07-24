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

import com.android.annotations.NonNull;
import com.android.sdklib.repository.License;
import com.android.sdklib.repository.PreciseRevision;
import com.android.sdklib.repositorycore.api.Dependency;
import com.android.sdklib.repositorycore.api.SdkPackage;
import com.android.sdklib.repositorycore.impl.generated.v1.DependencyType;
import com.android.sdklib.repositorycore.impl.generated.v1.GenericType;
import com.android.sdklib.repositorycore.impl.generated.v1.LicenseType;
import com.android.sdklib.repositorycore.impl.generated.v1.ObjectFactory;
import com.android.sdklib.repositorycore.impl.generated.v1.RevisionType;
import com.android.sdklib.repositorycore.impl.generated.v1.TypeDetails;
import com.android.sdklib.repositorycore.impl.generated.v1.UsesLicenseType;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

/**
 * Created by jbakermalone on 7/24/15.
 */
public class SdkPackageImpl implements SdkPackage {

    protected final GenericType mPackage;

    private final PreciseRevision mRevision;

    private final License mLicense;

    private final Collection<Dependency> mDependencies;

    public SdkPackageImpl(GenericType p) {
        mPackage = p;
        RevisionType revision = p.getRevision();

        mRevision = new PreciseRevision(revision.getMajor(), revision.getMinor(),
                revision.getMicro(), revision.getPreview());
        UsesLicenseType usesLicense = mPackage.getUsesLicense();
        if (usesLicense != null) {
            LicenseType licenseXml = (LicenseType) usesLicense.getRef();
            mLicense = new License(licenseXml.getValue(), licenseXml.getId());
        } else {
            mLicense = null;
        }

        ImmutableList.Builder<Dependency> depBuilder = ImmutableList.builder();
        GenericType.Dependencies dependencies = mPackage.getDependencies();
        if (dependencies != null) {
            for (DependencyType d : dependencies.getDependency()) {
                PreciseRevision depRev = new PreciseRevision(d.getMinRevision().getMajor(),
                        d.getMinRevision().getMinor(), d.getMinRevision().getMicro(),
                        d.getMinRevision().getPreview());
                depBuilder.add(new Dependency(depRev, d.getId()));
            }
            mDependencies = depBuilder.build();
        } else {
            mDependencies = null;
        }
    }

    public SdkPackageImpl(SdkPackage p) {
        ObjectFactory of = new ObjectFactory();
        GenericType generic = of.createGenericType();
        // TODO: archives
        GenericType.Dependencies dependencies = of.createGenericTypeDependencies();
        List<DependencyType> depsList = dependencies.getDependency();
        for (Dependency d : p.getDependencies()) {
            DependencyType dep = of.createDependencyType();
            dep.setId(d.getPath());
            RevisionType rev = of.createRevisionType();
            rev.setMajor(d.getRevision().getMajor());
            rev.setMinor(d.getRevision().getMinor());
            rev.setMicro(d.getRevision().getMicro());
            rev.setPreview(d.getRevision().getPreview());
            dep.setMinRevision(rev);
            depsList.add(dep);
        }
        if (!depsList.isEmpty()) {
            generic.setDependencies(dependencies);
        }
        generic.setObsolete(p.isObsolete());
        generic.setPath(p.getPath());
        RevisionType rev = of.createRevisionType();
        rev.setMajor(p.getRevision().getMajor());
        rev.setMinor(p.getRevision().getMinor());
        rev.setMicro(p.getRevision().getMicro());
        rev.setPreview(p.getRevision().getPreview());
        generic.setRevision(rev);
        generic.setTypeDetails(p.getTypeDetails());
        generic.setUiName(p.getUiName());
        License l = p.getLicense();
        if (l != null) {
            UsesLicenseType usesLicense = of.createUsesLicenseType();
            LicenseType license = of.createLicenseType();
            String ref = l.getLicenseRef();
            if (ref == null) {
                // Get a unique string if one isn't present.
                // TODO: consider de-duping when writing out multiple packages into one file
                ref = String.format("license-%X", hashCode());
            }
            license.setId(ref);
            license.setType("text");
            license.setValue(l.getLicense());
            usesLicense.setRef(license);
            generic.setUsesLicense(usesLicense);
        }
        mPackage = generic;
        mRevision = p.getRevision();
        mLicense = p.getLicense();
        mDependencies = p.getDependencies();
    }


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
    public Collection<Dependency> getDependencies() {
        return mDependencies;
    }

    @NonNull
    @Override
    public String getPath() {
        return mPackage.getPath();
    }

    @Override
    public boolean isObsolete() {
        return mPackage.isObsolete();
    }

    @Override
    public int compareTo(SdkPackage o) {
        // TODO
        return 0;
    }
}
