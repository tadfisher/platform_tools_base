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
package com.android.sdklib.repository.descriptors;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.repository.License;
import com.android.sdklib.repository.PreciseRevision;

import java.util.Map;

/**
 * Created by jbakermalone on 4/28/15.
 */
public class PkgDescGeneric extends PkgDesc {

    private final String mId;

    protected PkgDescGeneric(@NonNull PkgType type,
            @Nullable License license,
            @Nullable String listDisplay,
            @Nullable String descriptionShort,
            boolean isObsolete,
            @Nullable PreciseRevision revision,
            @Nullable AndroidVersion androidVersion,
            @Nullable String path,
            @Nullable IdDisplay tag,
            @Nullable IdDisplay vendor,
            @NonNull Map<String, PreciseRevision> dependencies,
            @NonNull String id,
            @Nullable IdDisplay name) {
        super(type, license, listDisplay, descriptionShort, null, isObsolete, revision,
                androidVersion, path, tag, vendor, PreciseRevision.NOT_SPECIFIED, null,
                new UpdateChecker(), null, name);
        mId = id;
    }

    public String getId() {
        return mId;
    }

  @Override
  public int compareTo(@NonNull IPkgDesc o) {
    if (!(o instanceof PkgDescGeneric)) {
      return super.compareTo(o);
    }
    PkgDescGeneric other = (PkgDescGeneric)o;
    int res = getId().compareTo(other.getId());
    if (res == 0) {
      res = getPreciseRevision().compareTo(other.getPreciseRevision());
    }
    return res;
  }

  private static class UpdateChecker implements IIsUpdateFor {

        @Override
        public boolean isUpdateFor(@NonNull PkgDesc thisPkgDesc, @NonNull IPkgDesc existingDesc) {
            if (thisPkgDesc instanceof PkgDescGeneric) {
                if (existingDesc instanceof PkgDescGeneric) {
                    return ((PkgDescGeneric) thisPkgDesc).getId()
                            .equals(((PkgDescGeneric) existingDesc).getId()) &&
                            thisPkgDesc.getPreciseRevision()
                                    .compareTo(existingDesc.getPreciseRevision()) > 0;
                }
                if (existingDesc.getType().equals(thisPkgDesc.getType())) {
                    // Below checks are for backward compatibility with non-generic packages.
                    // Once generic packages are fully rolled out they should be able to be removed.
                    if (existingDesc.getType().equals(PkgType.PKG_PLATFORM)
                            || existingDesc.getType().equals(PkgType.PKG_SOURCE)) {
                        return thisPkgDesc.getAndroidVersion().getApiLevel() == existingDesc
                                .getAndroidVersion().getApiLevel()
                                && thisPkgDesc.getPreciseRevision()
                                .compareTo(existingDesc.getPreciseRevision()) > 0;
                    } else if (existingDesc.getType().equals(PkgType.PKG_BUILD_TOOLS)) {
                        return thisPkgDesc.getPreciseRevision()
                                .equals(existingDesc.getPreciseRevision());
                    } else if (existingDesc.getType().equals(PkgType.PKG_DOC)) {
                        return thisPkgDesc.getPreciseRevision()
                                .compareTo(existingDesc.getPreciseRevision()) >= 0;
                    } else if (existingDesc.getType().equals(PkgType.PKG_PLATFORM_TOOLS)) {

                    } else if (existingDesc.getType().equals(PkgType.PKG_TOOLS)) {

                    } else if (existingDesc.getType().equals(PkgType.PKG_ADDON_SYS_IMAGE)) {

                    } else if (existingDesc.getType().equals(PkgType.PKG_ADDON)) {

                    } else if (existingDesc.getType().equals(PkgType.PKG_SYS_IMAGE)) {

                    } else if (existingDesc.getType().equals(PkgType.PKG_EXTRA)) {

                    }
                }
            }
            return false;
        }
    }
}
