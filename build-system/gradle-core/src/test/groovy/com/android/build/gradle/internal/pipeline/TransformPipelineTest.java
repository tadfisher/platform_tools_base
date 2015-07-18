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

package com.android.build.gradle.internal.pipeline;

import com.android.annotations.Nullable;
import com.android.build.gradle.internal.TaskFactory;
import com.google.common.collect.Maps;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.junit.Test;

import java.util.Map;

public class TransformPipelineTest {

    private static final class MockTaskFactory implements TaskFactory {

        private final Map<String, Task> tasks = Maps.newHashMap();

        @Override
        public boolean containsKey(String name) {
            return tasks.containsKey(name);
        }

        @Override
        public void create(String name) {

        }

        @Override
        public void create(String name, Action<? super Task> configAction) {

        }

        @Override
        public <S extends Task> void create(String name, Class<S> type) {

        }

        @Override
        public <S extends Task> void create(String name, Class<S> type,
                Action<? super S> configAction) {

        }

        @Override
        public void named(String name, Action<? super Task> configAction) {

        }

        @Nullable
        @Override
        public Task named(String name) {
            return tasks.get(name);
        }
    }

    @Test
    void testAddTransform() {
        //TransformPipeline sPipeline = TransformPipeline.builder()
        //        .setTaskFactory(new MockTaskFactory())
        //        .setTaskRegistry(new AndroidTaskRegistry())
        //        .setVariantScope(new VariantScopeImpl())
        //        .build();

    }
}