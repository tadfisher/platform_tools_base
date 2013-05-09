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

package com.android.sdklib.internal.repository.packages;


/**
 * Interface used to decorate a {@link Package} that has a dependency
 * on a specific system-image (a specific ABI) which itself has a dependency
 * of a specific platform (API level and/or code name).
 * <p/>
 * A package that has this dependency can only be installed if a system-image matching
 * the requested ABI and API level is present or installed at the same time.
 */
public interface ISystemImageDependency extends IPlatformDependency {

    /**
     * Returns the ABIs of the system-image(s) the package dependency.
     * Cannot be null. May be empty.
     */
    public String[] getAbis();
}
