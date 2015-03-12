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

import com.google.common.collect.ImmutableMap;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.model.collection.CollectionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for a {@link Task}, will allow us to keep interesting information like what tasks
 * depend on this task, the which ones this task depends on. Eventually we can tight things further
 * to make sure tasks are never retrieved using string based APIs.
 */
public class AndroidTask<T extends ScopedTask> {

    private String taskName;
    private final Class<T> taskType;
    private final CollectionBuilder<Task> collectionBuilder;
    private T task;
    private final List<AndroidTask<ScopedTask>> upstreamTasks;
    private final List<AndroidTask<? extends Task>> downstreamTasks;

    public AndroidTask(String taskName, Class<T> taskType, CollectionBuilder<Task> collectionBuilder) {
        this.taskName = taskName;
        this.taskType = taskType;
        this.collectionBuilder = collectionBuilder;
        upstreamTasks = new ArrayList<AndroidTask<ScopedTask>>();
        downstreamTasks = new ArrayList<AndroidTask<? extends Task>>();
    }

    /**
     * this should be called directly by the annotation processor code. As you can see, right now,
     * the dependency is not declared to gradle, just in this internal model. The gradle
     * dependency is added during field wiring.
     *
     * @param other an upstream task.
     */


    public void dependsOn(final AndroidTask<ScopedTask> other) {
        collectionBuilder.named(taskName, new Action<Task>() {
            @Override
            public void execute(Task task) {
                task.dependsOn(collectionBuilder.get(other.taskName));
            }
        });
        upstreamTasks.add(other);
        other.addDependent(this);
    }

    private void addDependent(AndroidTask<? extends Task> tAndroidTask) {
        downstreamTasks.add(tAndroidTask);
    }
}
