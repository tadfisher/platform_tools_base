/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.android.build.gradle.internal.model
import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.builder.model.SigningConfig
import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * Implementation of SigningConfig that is serializable. Objects used in the DSL cannot be
 * serialized.
 */
@CompileStatic
@Immutable(knownImmutableClasses = [File])
class SigningConfigImpl implements SigningConfig, Serializable {
    private static final long serialVersionUID = 2L

    @NonNull final String name
    @Nullable final File storeFile
    @Nullable final String storePassword
    @Nullable final String keyAlias
    @Nullable final String keyPassword
    @Nullable final String storeType
    final boolean signingReady

    @NonNull
    static SigningConfig createSigningConfig(@NonNull SigningConfig signingConfig) {
        return new SigningConfigImpl(
                signingConfig.name,
                signingConfig.storeFile,
                signingConfig.storePassword,
                signingConfig.keyAlias,
                signingConfig.keyPassword,
                signingConfig.storeType,
                signingConfig.signingReady)
    }
}
