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

package com.google.jedo.sample;

import java.io.File;

/**
 * a scope example.
 */
public class GlobalScope {

    // no need for annotation, there is only one instance in that scope.
    public final AndroidModel androidModel;

    private final File dexFile;

    @TopLevelTaskThree.DexFile
    File getDexFile() {
        return dexFile;
    }

    // these values should be created lazily on demand to avoid pre-calculating scope data which
    // may not be useful.
    public GlobalScope(File dexFile, AndroidModel model) {
        this.dexFile = dexFile;
        this.androidModel = model;
    }
}
