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

package com.android.build.gradle.internal.tasks.processor;

import com.android.build.gradle.internal.TaskFactory;
import com.google.common.collect.Maps;

import org.gradle.api.Action;
import org.gradle.api.Task;

import java.util.Map;

/**
 * Facilities to create tasks and provide hooks for automatic wiring of tasks.
 */
public class AndroidTaskRegistry {

    private final Map<String, AndroidTask<? extends Task>> androidTasks = Maps.newHashMap();
    private final Map<Class, AndroidTask<? extends Task>> producers = Maps.newHashMap();

    public AndroidTask getProducer(Class<?> cls) {
        return producers.get(cls);
    }

    public <T extends Task> AndroidTask<T> create(
            TaskFactory tasks,
            String taskName,
            final Class<T> taskClass) {
        return create(tasks, taskName, taskClass, new Action<T> () {
                    @Override
                    public void execute(T t) {}
                });
    }

    public <T extends Task> AndroidTask<T> create(
            TaskFactory tasks,
            String taskName,
            final Class<T> taskClass,
            Action<T> configureAction) {

        tasks.create(taskName, taskClass, configureAction);
        final AndroidTask<T> newTask = new AndroidTask<T>(taskName, taskClass);
        androidTasks.put(taskName, newTask);

        for (Class<?> input : newTask.getBindingInputs()) {
            AndroidTask<? extends Task> producer = producers.get(input);
            if (producer != null) {
                newTask.dependsOn(producer);
            }
        }

        tasks.named(taskName,
                new Action<Task>() {
                    @Override
                    public void execute(Task task) {
                        newTask.setTask(taskClass.cast(task));
                        newTask.configure();
                    }
                });

        // Update outputs.
        for(Class<?> output : newTask.getBindingOutputs()) {
            producers.put(output, newTask);
        }
        return newTask;
    }
}
