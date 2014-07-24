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

package com.android.build.gradle.model;

import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.language.base.FunctionalSourceSet;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.internal.AbstractLanguageSourceSet;

import javax.inject.Inject;

/**
 * Implementation of LanguageSourceSet for Android's sources.
 */
public class AndroidLanguageSourceSet extends AbstractLanguageSourceSet implements LanguageSourceSet {
    @Inject
    public AndroidLanguageSourceSet(String name, FunctionalSourceSet parent, FileResolver fileResolver) {
        super(name, parent, "Android source", new DefaultSourceDirectorySet("source", fileResolver));
    }
}
