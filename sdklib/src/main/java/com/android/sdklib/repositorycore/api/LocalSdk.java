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

import com.android.sdklib.repositorycore.impl.local.LocalPackageImpl;
import com.android.sdklib.repositorycore.impl.local.LocalPackageImpl;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by jbakermalone on 7/23/15.
 */
// TODO: is interface this actually needed, or just go through the manager and keep it internal?
public interface LocalSdk {

    File getLocation();

    // key is path
    Map<String, LocalPackageImpl> getPackages();

    void invalidate();
}
