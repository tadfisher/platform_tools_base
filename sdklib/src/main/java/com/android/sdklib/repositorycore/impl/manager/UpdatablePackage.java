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
import com.android.annotations.Nullable;
import com.android.sdklib.repository.PreciseRevision;
import com.android.sdklib.repositorycore.api.LocalPackage;
import com.android.sdklib.repositorycore.api.PackageMeta;
import com.android.sdklib.repositorycore.api.SdkPackage;
import com.android.sdklib.repositorycore.impl.remote.RemotePackage;
import com.android.utils.ILogger;
import com.android.utils.NullLogger;


import java.util.LinkedList;
import java.util.List;

/**
 * Represents a (revisionless) package, either local, remote, or both. If both a local and remote package are specified,
 * they should represent exactly the same package, excepting the revision. That is, the result of installing the remote package
 * should be (a possibly updated version of) the local package.
 */
public class UpdatablePackage implements Comparable<UpdatablePackage> {
  private LocalPackage myLocalInfo;
  private RemotePackage myRemoteInfo;
  private RemotePackage myRemotePreviewInfo;

  // TODO: figure out logging
  private final ILogger LOG = new NullLogger();

  public UpdatablePackage(@NonNull LocalPackage localInfo) {
    init(localInfo, null);
  }

  public UpdatablePackage(@NonNull RemotePackage remoteInfo) {
    init(null, remoteInfo);
  }

  public UpdatablePackage(@NonNull LocalPackage localInfo, @NonNull RemotePackage remoteInfo) {
    init(localInfo, remoteInfo);
  }

  private void init(@Nullable LocalPackage localPkg, @Nullable RemotePackage remotePkg) {
    assert localPkg != null || remotePkg != null;
    myLocalInfo = localPkg;
    if (remotePkg != null) {
      addRemote(remotePkg);
    }
  }

  /**
   * Adds the given remote package if this package doesn't already have a remote, or if the given remote is more recent.
   * If it is a preview, it will be returned by {@link #getRemote()} only if it is specified that preview packages are desired.
   *
   * @param remote The remote package.
   */
  public void addRemote(@NonNull RemotePackage remote) {
    if (remote.getRevision().isPreview()) {
      if (myRemotePreviewInfo == null ||
          remote.getRevision().compareTo(myRemotePreviewInfo.getRevision(), PreciseRevision.PreviewComparison.IGNORE) > 0) {
        myRemotePreviewInfo = remote;
      }
    }
    else {
      if (myRemoteInfo == null || remote.getRevision().compareTo(myRemoteInfo.getRevision(), PreciseRevision.PreviewComparison.IGNORE) > 0) {
        myRemoteInfo = remote;
      }
    }
  }

  @Nullable
  public LocalPackage getLocalInfo() {
    return myLocalInfo;
  }

  public RemotePackage getRemote(boolean includePreview) {
    // If includePreview is true, and we don't have a non-preview remote or the preview is newer than
    // the non-preview, return the preview.
    if (includePreview &&
        (!hasRemote(false) ||
         (hasPreview() &&
          myRemotePreviewInfo.getRevision().compareTo(myRemoteInfo.getRevision(), PreciseRevision.PreviewComparison.IGNORE) > 0))) {
      return myRemotePreviewInfo;
    }
    // Else return the non-preview, possibly null.
    return myRemoteInfo;
  }

  public boolean hasPreview() {
    return myRemotePreviewInfo != null;
  }

  public boolean hasRemote(boolean includePreview) {
    return myRemoteInfo != null || (includePreview && myRemotePreviewInfo != null);
  }

  public boolean hasLocal() {
    return myLocalInfo != null;
  }

  @Override
  public int compareTo(UpdatablePackage o) {
    return getRepresentative(true).compareTo(o.getRepresentative(true));
  }

  /**
   * Gets a IPkgDesc corresponding to this updatable package. This will be:
   * - The local pkg desc if the package is installed
   * - The remote preview package if there is a remote preview and includePreview is true
   * - The remote package otherwise, or null if there is no non-preview remote.
   * @param includePreview
   */
  public SdkPackage getRepresentative(boolean includePreview) {
    if (hasLocal()) {
      return myLocalInfo;
    }
    if (includePreview && hasPreview()) {
      return myRemotePreviewInfo;
    }
    if (hasRemote(false)) {
      return getRemote(false);
    }
    return null;
  }

  // TODO: note that remotes are always updates
  public boolean isUpdate(boolean includePreview) {
    RemotePackage remote = getRemote(includePreview);
    return myLocalInfo != null && remote != null;
  }

  public List<RemotePackage> getAllRemotes() {
    List<RemotePackage> result = new LinkedList<RemotePackage>();
    if (myRemoteInfo != null) {
      result.add(myRemoteInfo);
    }
    if (myRemotePreviewInfo != null) {
      result.add(myRemotePreviewInfo);
    }
    return result;
  }
}
