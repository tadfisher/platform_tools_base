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

import com.android.sdklib.repositorycore.api.Downloader;
import com.android.sdklib.repositorycore.api.ProgressIndicator;
import com.android.sdklib.repositorycore.api.SettingsController;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.List;

/**
 * Created by jbakermalone on 7/22/15.
 */
public class RemoteSdk {
    private SettingsController mSettings;
    private Downloader mDownloader;

    private List<Runnable> mSourcesChangeListeners;
    // key is url
    private List<SdkSourceProvider> mSourcesSource = Lists.newArrayList();

    // TODO: cache here or just in SdkPackages? I think not here.
    //private List<RemotePackage> mPackages;

    public RemoteSdk(SettingsController settings, Downloader downloader) {
        mSettings = settings;
        mDownloader = downloader;
    }

    public Multimap<String, ? extends RemotePackage> fetchPackages(ProgressIndicator logger) {
        // TODO
        return null;
    }


}
