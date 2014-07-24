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

import com.android.annotations.NonNull;
import com.android.build.gradle.api.AndroidSourceDirectorySet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.language.base.FunctionalSourceSet;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.api.internal.AbstractBuildableModelElement;
import org.gradle.language.base.internal.AbstractLanguageSourceSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import groovy.lang.Closure;

/**
 * Default implementation of the AndroidSourceDirectorySet.
 */
public class AndroidLanguageSourceSet extends AbstractLanguageSourceSet implements LanguageSourceSet {
    @Inject
    public AndroidLanguageSourceSet(String name, FunctionalSourceSet parent, FileResolver fileResolver) {
        super(name, parent, "Android source", new DefaultSourceDirectorySet("source", fileResolver));
    }

}
