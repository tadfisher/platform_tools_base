/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.build.gradle.internal.dsl
/**
 * DSL object for configuring aapt options.
 */
@GradleStyleSetters
public class AaptOptions implements com.android.builder.model.AaptOptions {

    /**
     * Pattern describing assets to be ignore.
     *
     * <p>See <code>aapt --help</code>
     */
    String ignoreAssets

    /**
     * Whether to use the new cruncher.
     *
     * <p>TODO: Document.
     */
    boolean useNewCruncher = false;

    /**
     * Forces aapt to return an error if it fails to find an entry for a configuration.
     */
    boolean failOnMissingConfigEntry = false;

    /**
     * Extensions of files that will not be stored compressed in the APK.
     */
    Collection<String> noCompress = []

    /**
     * @see #noCompress
     */
    void setNoCompress(String... noCompress) {
        this.@noCompress = noCompress
    }

    /**
     * Alias for backwards compatibility.
     *
     * @see #ignoreAssets
     */
    void setIgnoreAssetsPattern(String ignoreAssetsPattern) {
        this.ignoreAssets = ignoreAssetsPattern
    }

}
