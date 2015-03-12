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

import org.gradle.api.BuildableModelElement;
import org.gradle.api.internal.AbstractBuildableModelElement;

import java.io.File;

/**
 * Created by jedo on 3/25/15.
 */
public class DexFile extends AbstractBuildableModelElement implements BuildableModelElement {

    private final File dexFile;

    public DexFile(File dexFile) {
        this.dexFile = dexFile;
    }


    public File get() {
        return dexFile;
    }
}
