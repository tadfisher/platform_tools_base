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

package com.android.build.gradle.internal.dsl;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.core.NdkConfig;
import com.android.build.gradle.tasks.Dex;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.core.BuilderConstants;
import com.android.builder.model.BaseConfig;
import com.android.builder.model.BuildType;
import com.android.builder.model.ClassField;

import org.gradle.api.Action;
import org.gradle.internal.reflect.Instantiator;

import java.util.Map;

/**
 * A build type with addition properties for building with Gradle plugin.
 */
public interface CoreBuildType extends BuildType {

    NdkConfig getNdkConfig();

    Boolean getUseJack();

    boolean getShrinkResources();
}
