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
import com.android.sdklib.repository.License;
import com.android.sdklib.repository.PreciseRevision;
import com.android.sdklib.repositorycore.impl.generated.v1.DependencyType;
import com.android.sdklib.repositorycore.impl.generated.v1.GenericType;
import com.android.sdklib.repositorycore.impl.generated.v1.TypeDetails;

import java.util.Collection;

/**
 * Created by jbakermalone on 7/24/15.
 */
public interface SdkPackage extends Comparable<SdkPackage> {

    TypeDetails getTypeDetails();

    PreciseRevision getRevision();

    String getUiName();

    License getLicense();

    Collection<Dependency> getDependencies();

    @NonNull String getPath();

    boolean isObsolete();
}
