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

package com.android.sdklib.repositorycore.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.repositorycore.impl.manager.SdkManagerImpl;
import com.android.sdklib.repositorycore.impl.remote.SdkSourceProvider;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jbakermalone on 7/31/15.
 */
public abstract class SdkManager {

    public abstract void registerSourceProvider(SdkSourceProvider provider);

    public abstract Set<SdkSourceProvider> getSourceProviders();

    public abstract Map<String, SdkSchemaExtension> getSchemaExtensions();

    public abstract SdkSchemaExtension getSchemaExtension(String namespace);

    public abstract void registerSchemaExtension(SdkSchemaExtension extension);

    public static SdkManager getInstance(@Nullable File localPath,
            @Nullable SettingsController settings, @Nullable Downloader downloader) {
        return SdkManagerImpl.getInstance(localPath, settings, downloader);
    }

    // return value: whether a reload was actually done
    public abstract boolean load(long timeoutMs,
            boolean canBeCancelled,
            @NonNull List<SdkLoadedCallback> onLocalComplete,
            @NonNull List<SdkLoadedCallback> onSuccess,
            @NonNull List<Runnable> onError,
            boolean forceRefresh,
            ProgressRunner runner,
            boolean sync);
}
