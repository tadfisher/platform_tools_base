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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.androidTarget.PlatformTarget;
import com.android.sdklib.io.FileOp;
import com.android.sdklib.io.IFileOp;
import com.android.sdklib.repository.FullRevision;
import com.android.sdklib.repository.License;
import com.android.sdklib.repository.PkgProps;
import com.android.sdklib.repository.PreciseRevision;
import com.android.sdklib.repository.descriptors.IdDisplay;
import com.android.sdklib.repository.descriptors.PkgType;
import com.android.sdklib.repository.local.LocalPkgInfo;
import com.android.sdklib.repository.local.LocalPlatformPkgInfo;
import com.android.sdklib.repository.local.LocalSdk;
import com.android.sdklib.repositorycore.api.Dependency;
import com.android.sdklib.repositorycore.api.FallbackLocalSdk;
import com.android.sdklib.repositorycore.api.LocalPackage;
import com.android.sdklib.repositorycore.api.SdkPackage;
import com.android.sdklib.repositorycore.impl.generated.v1.IdDisplayType;
import com.android.sdklib.repositorycore.impl.generated.v1.ObjectFactory;
import com.android.sdklib.repositorycore.impl.generated.v1.TypeDetails;
import com.android.sdklib.repositoryv2.generated.addon.v1.AddonDetailsType;
import com.android.sdklib.repositoryv2.generated.addon.v1.ExtraDetailsType;
import com.android.sdklib.repositoryv2.generated.repository.v1.LayoutlibType;
import com.android.sdklib.repositoryv2.generated.repository.v1.PlatformDetailsType;
import com.android.sdklib.repositoryv2.generated.repository.v1.SourceDetailsType;
import com.android.sdklib.repositoryv2.generated.sysimg.v1.AbiType;
import com.android.sdklib.repositoryv2.generated.sysimg.v1.SysimgDetailsType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by jbakermalone on 7/31/15.
 */
public class LegacyLocalSdk implements FallbackLocalSdk {

    private final LocalSdk mLocalSdk;

    private Map<File, LocalPkgInfo> mPkgs = null;

    private final IFileOp mFop;

    public LegacyLocalSdk(File root) {
        this(root, new FileOp());
    }

    public LegacyLocalSdk(File root, IFileOp fop) {
        mLocalSdk = new LocalSdk(fop);
        mLocalSdk.setLocation(root);
        mFop = fop;
    }

    @Override
    public LocalPackage parseLegacyLocalPackage(File dir) {
        if (!mFop.exists(new File(dir, SdkConstants.FN_SOURCE_PROP))) {
            return null;
        }
        Logger.getLogger(getClass().getName())
                .warning(String.format("Parsing legacy package: %s", dir));
        if (mPkgs == null) {
            Map<File, LocalPkgInfo> result = Maps.newHashMap();
            for (LocalPkgInfo local : mLocalSdk.getPkgsInfos(PkgType.PKG_ALL)) {
                result.put(local.getLocalDir(), local);
            }
            mPkgs = result;
        }

        LocalPkgInfo info = mPkgs.get(dir);
        if (info == null) {
            Logger.getLogger(getClass().getName())
                    .warning(String.format("Bad legacy package found: %s", dir));
            return null;
        }

        return new LegacyLocalPackage(info);
    }

    class LegacyLocalPackage implements LocalPackage {

        private final LocalPkgInfo mWrapped;

        LegacyLocalPackage(LocalPkgInfo wrapped) {
            mWrapped = wrapped;
        }

        @Override
        public TypeDetails getTypeDetails() {
            ObjectFactory commonOf = new ObjectFactory();
            if (mWrapped.getDesc().getType() == PkgType.PKG_TOOLS) {
                com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory of
                        = new com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory();
                return of.createToolDetailsType();
            } else if (mWrapped.getDesc().getType() == PkgType.PKG_PLATFORM_TOOLS) {
                com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory of
                        = new com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory();
                return of.createPlatformToolDetailsType();
            } else if (mWrapped.getDesc().getType() == PkgType.PKG_BUILD_TOOLS) {
                com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory of
                        = new com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory();
                return of.createBuildToolDetailsType();
            } else if (mWrapped.getDesc().getType() == PkgType.PKG_DOC) {
                com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory of
                        = new com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory();
                return of.createDocDetailsType();
            } else if (mWrapped.getDesc().getType() == PkgType.PKG_PLATFORM) {
                com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory of
                        = new com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory();
                PlatformDetailsType details = of.createPlatformDetailsType();
                details.setApiLevel(mWrapped.getDesc().getAndroidVersion().getApiLevel());
                details.setCodename(mWrapped.getDesc().getAndroidVersion().getCodename());
                details.setVersion(mWrapped.getSourceProperties().getProperty(
                        PkgProps.PLATFORM_VERSION));
                LayoutlibType layoutLib = of.createLayoutlibType();
                LocalPlatformPkgInfo localPPI = (LocalPlatformPkgInfo) mWrapped;
                SdkManager.LayoutlibVersion layoutVersion =
                        ((PlatformTarget) localPPI.getAndroidTarget()).getLayoutlibVersion();
                layoutLib.setApi(layoutVersion.getApi());
                layoutLib.setRevision(layoutVersion.getRevision());
                details.setLayoutlib(layoutLib);
                return details;
            } else if (mWrapped.getDesc().getType() == PkgType.PKG_SYS_IMAGE ||
                    mWrapped.getDesc().getType() == PkgType.PKG_ADDON_SYS_IMAGE) {
                com.android.sdklib.repositoryv2.generated.sysimg.v1.ObjectFactory of =
                        new com.android.sdklib.repositoryv2.generated.sysimg.v1.ObjectFactory();
                SysimgDetailsType details = of.createSysimgDetailsType();
                details.setAbi(AbiType.fromValue(mWrapped.getDesc().getPath()));
                details.setApiLevel(mWrapped.getDesc().getAndroidVersion().getApiLevel());
                IdDisplay tagIdDisplay = mWrapped.getDesc().getTag();
                if (tagIdDisplay != null) {
                    IdDisplayType tag = commonOf.createIdDisplayType();
                    tag.setId(tagIdDisplay.getId());
                    tag.setDisplay(tagIdDisplay.getDisplay());
                    details.setTag(tag);
                }
                IdDisplay vendorIdDisplay = mWrapped.getDesc().getVendor();
                if (vendorIdDisplay != null) {
                    IdDisplayType vendor = commonOf.createIdDisplayType();
                    vendor.setId(vendorIdDisplay.getId());
                    vendor.setDisplay(vendorIdDisplay.getDisplay());
                    details.setVendor(vendor);
                }
                return details;
            } else if (mWrapped.getDesc().getType() == PkgType.PKG_ADDON) {
                com.android.sdklib.repositoryv2.generated.addon.v1.ObjectFactory of = new
                        com.android.sdklib.repositoryv2.generated.addon.v1.ObjectFactory();
                AddonDetailsType details = of.createAddonDetailsType();
                IdDisplay vendorIdDisplay = mWrapped.getDesc().getVendor();
                if (vendorIdDisplay != null) {
                    IdDisplayType vendor = commonOf.createIdDisplayType();
                    vendor.setId(vendorIdDisplay.getId());
                    vendor.setDisplay(vendorIdDisplay.getDisplay());
                    details.setVendor(vendor);
                }
                details.setApiLevel(mWrapped.getDesc().getAndroidVersion().getApiLevel());
                return details;
            } else if (mWrapped.getDesc().getType() == PkgType.PKG_SAMPLE) {
                // Obsolete, ignore
                return null;
            } else if (mWrapped.getDesc().getType() == PkgType.PKG_SOURCE) {
                com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory of
                        = new com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory();
                SourceDetailsType details = of.createSourceDetailsType();
                details.setApiLevel(mWrapped.getDesc().getAndroidVersion().getApiLevel());
                return details;
            } else if (mWrapped.getDesc().getType() == PkgType.PKG_EXTRA) {
                com.android.sdklib.repositoryv2.generated.addon.v1.ObjectFactory of = new
                        com.android.sdklib.repositoryv2.generated.addon.v1.ObjectFactory();
                ExtraDetailsType details = of.createExtraDetailsType();
                IdDisplay vendorIdDisplay = mWrapped.getDesc().getVendor();
                if (vendorIdDisplay != null) {
                    IdDisplayType vendor = commonOf.createIdDisplayType();
                    vendor.setId(vendorIdDisplay.getId());
                    vendor.setDisplay(vendorIdDisplay.getDisplay());
                    details.setVendor(vendor);
                }
                return details;
            } else if (mWrapped.getDesc().getType() == PkgType.PKG_NDK) {
                com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory of
                        = new com.android.sdklib.repositoryv2.generated.repository.v1.ObjectFactory();
                return of.createNdkDetailsType();
            } else {
                return null;
            }
        }

        @Override
        public PreciseRevision getRevision() {
            return mWrapped.getDesc().getPreciseRevision();
        }

        @Override
        public String getUiName() {
            return mWrapped.getDesc().getListDisplay();
        }

        @Override
        public License getLicense() {
            return mWrapped.getDesc().getLicense();
        }

        @Override
        public Collection<Dependency> getDependencies() {
            List<Dependency> result = Lists.newArrayList();
            FullRevision rev = mWrapped.getDesc().getMinPlatformToolsRev();
            if (rev != null) {
                result.add(
                        new Dependency(new PreciseRevision(rev), SdkConstants.FD_PLATFORM_TOOLS));
            }
            rev = mWrapped.getDesc().getMinToolsRev();
            if (rev != null) {
                result.add(new Dependency(new PreciseRevision(rev), SdkConstants.FD_TOOLS));
            }
            return result;
        }

        @NonNull
        @Override
        public String getPath() {
            return mWrapped.getLocalDir().getAbsolutePath()
                    .substring(mLocalSdk.getLocation().getAbsolutePath().length() + 1)
                    .replaceAll(File.separator, ";");
        }

        @Override
        public boolean isObsolete() {
            return mWrapped.getDesc().isObsolete();
        }

        @Override
        public int compareTo(SdkPackage o) {
            // TODO
            return 0;
        }
    }
}
