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

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.model.collection.CollectionBuilder;

/**
 * Helper class to create Tasks' factories. Tasks should only be created through factories to
 * provide a clean configuration API.
 */
public abstract class AbstractTaskFactory<T extends Task> {


    protected abstract void configure(T instance);

    protected AndroidTask<T> build(
            Class<T> taskType,
            AndroidTasks androidTasks,
            CollectionBuilder<Task> tasks,
            AndroidTask<Task>... upstreamTasks) {

        System.out.println(taskType.getSimpleName() + " creation");
        AndroidTask<T> newTask = androidTasks
                .create(tasks, taskType.getSimpleName(), taskType,
                        new Action<T>() {
                            @Override
                            public void execute(T task) {
                                System.out.println(task.getName() + " configuration");
                                configure(task);
                            }
                        });

        for (AndroidTask<Task> upstreamTask : upstreamTasks) {
            newTask.dependsOn(upstreamTask);
        }
        return newTask;
    }
}
