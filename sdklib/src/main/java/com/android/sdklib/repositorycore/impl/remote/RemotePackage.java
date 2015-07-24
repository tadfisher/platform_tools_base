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
package com.android.sdklib.repositorycore.impl.remote;

import com.android.sdklib.repository.License;
import com.android.sdklib.repositorycore.api.SdkSource;
import com.android.sdklib.repositorycore.impl.generated.ArchiveType;
import com.android.sdklib.repositorycore.impl.generated.GenericType;
import com.android.sdklib.repositorycore.impl.manager.SdkPackageImpl;

import java.util.Collection;

/**
 * Created by jbakermalone on 7/22/15.
 */
public final class RemotePackage extends SdkPackageImpl {
    private final SdkSource mSource;

    public RemotePackage(GenericType p, SdkSourceImpl source) {
        super(p);
        mSource = source;
        // TODO: assert archives compatible
    }

    public Collection<ArchiveType> getArchives() {
        return mPackage.getArchives().getArchive();
    }

    public SdkSource getSource() {
        return mSource;
    }
}
