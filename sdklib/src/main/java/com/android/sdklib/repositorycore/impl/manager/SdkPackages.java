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
import com.android.sdklib.repositorycore.api.LocalPackage;
import com.android.sdklib.repositorycore.impl.remote.RemotePackage;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;

import java.util.Map;
import java.util.Set;


/**
 * Store of current local and remote packages, in convenient forms.
 */
public final class SdkPackages {

    private final Set<UpdatablePackage> myUpdatedPkgs = Sets.newTreeSet();

    private final Set<RemotePackage> mNewPkgs = Sets.newTreeSet();

    private final long myTimestampMs;

    // String is path
    private Map<String, UpdatablePackage> mConsolidatedPkgs = Maps.newTreeMap();

    // String is path
    private Map<String, ? extends LocalPackage> mLocalPackages = Maps.newHashMap();

    // String is path
    private Multimap<String, ? extends RemotePackage> mRemotePackages = TreeMultimap.create();

    SdkPackages() {
        myTimestampMs = System.currentTimeMillis();
    }

    public SdkPackages(Map<String, LocalPackage> localPkgs,
            Multimap<String, RemotePackage> remotePkgs) {
        this();
        setLocalPkgInfos(localPkgs);
        setRemotePkgInfos(remotePkgs);
    }

    /**
     * Returns the timestamp (in {@link System#currentTimeMillis()} time) when this object was
     * created.
     */
    public long getTimestampMs() {
        return myTimestampMs;
    }

    /**
     * Returns the set of packages that have local updates available.
     *
     * @return A non-null, possibly empty Set of update candidates.
     */
    @NonNull
    public Set<UpdatablePackage> getUpdatedPkgs() {
        return myUpdatedPkgs;
    }

    /**
     * Returns the set of new remote packages that are not locally present and that the user could
     * install.
     *
     * @return A non-null, possibly empty Set of new install candidates.
     */
    @NonNull
    public Set<RemotePackage> getNewPkgs() {
        return mNewPkgs;
    }

    /**
     * Returns a map of package install ids to {@link UpdatablePackage}s representing all known
     * local and remote packages. Remote packages corresponding to local packages will be
     * represented by a single item containing both the local and remote info. {@see
     * IPkgDesc#getInstallId()}
     */
    @NonNull
    public Map<String, UpdatablePackage> getConsolidatedPkgs() {
        return mConsolidatedPkgs;
    }

    @NonNull
    public Map<String, ? extends LocalPackage> getLocalPkgInfos() {
        return mLocalPackages;
    }

    public Multimap<String, ? extends RemotePackage> getRemotePkgInfos() {
        return mRemotePackages;
    }

    void setLocalPkgInfos(Map<String, ? extends LocalPackage> packages) {
        mLocalPackages = packages;
        computeUpdates();
    }

    // TODO: note that packages must be compatible
    void setRemotePkgInfos(Multimap<String, ? extends RemotePackage> packages) {
        mRemotePackages = packages;
        computeUpdates();
    }

    private void computeUpdates() {
        Map<String, UpdatablePackage> newConsolidatedPkgs = Maps.newTreeMap();
        for (String path : mLocalPackages.keySet()) {
            LocalPackage local = mLocalPackages.get(path);
            UpdatablePackage updatable = new UpdatablePackage(local);
            newConsolidatedPkgs.put(path, updatable);
            if (mRemotePackages.containsKey(path)) {
                myUpdatedPkgs.add(updatable);
                for (RemotePackage remote : mRemotePackages.get(path)) {
                    updatable.addRemote(remote);
                }
            }
        }
        for (String path : mRemotePackages.keySet()) {
            if (!newConsolidatedPkgs.containsKey(path)) {
                UpdatablePackage updatable = null;
                for (RemotePackage remote : mRemotePackages.get(path)) {
                    mNewPkgs.add(remote);
                    if (updatable == null) {
                        updatable = new UpdatablePackage(remote);
                    } else {
                        updatable.addRemote(remote);
                    }
                }
                newConsolidatedPkgs.put(path, updatable);
            }
        }
        mConsolidatedPkgs = newConsolidatedPkgs;
    }
}
