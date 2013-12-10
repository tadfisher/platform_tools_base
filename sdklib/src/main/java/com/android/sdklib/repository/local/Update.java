/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.sdklib.repository.local;

import com.android.annotations.NonNull;
import com.android.sdklib.repository.descriptors.IPkgDesc;
import com.android.sdklib.repository.descriptors.PkgType;
import com.android.sdklib.repository.remote.RemotePkgInfo;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.util.Set;



/**
 * Computes updates available for local packages.
 */
public class Update {

    public static class Result {
        final Set<LocalPkgInfo> mUpdatedPkgs = Sets.newTreeSet();
        final Set<RemotePkgInfo> mNewPkgs = Sets.newTreeSet();

        /**
         * Returns the
         * @return
         */
        public Set<LocalPkgInfo> getUpdatedPkgs() {
            return mUpdatedPkgs;
        }

        public Set<RemotePkgInfo> getNewPkgs() {
            return mNewPkgs;
        }
    }

    public static Result computeUpdates(@NonNull LocalPkgInfo[] localPkgs,
                                        @NonNull Multimap<PkgType, RemotePkgInfo> remotePkgs) {

        Result result = new Result();

        for (LocalPkgInfo local : localPkgs) {
            findUpdate(local, remotePkgs, result);
        }

        return result;
    }

    private static void findUpdate(@NonNull LocalPkgInfo local,
                                   @NonNull Multimap<PkgType, RemotePkgInfo> remotePkgs,
                                   @NonNull Result result) {
        local.setUpdate(null);

        IPkgDesc localDesc = local.getDesc();
        for (RemotePkgInfo remote: remotePkgs.get(localDesc.getType())) {

        }

    }

}
