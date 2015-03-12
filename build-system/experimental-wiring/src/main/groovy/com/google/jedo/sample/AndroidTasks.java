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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facilities to create tasks and provide hooks for automatic wiring of tasks.
 */
public class AndroidTasks {

    private final Map<String, AndroidTask> tasks = new HashMap<String, AndroidTask>();
    private final Set<CollectionBuilder<Task>> collectionBuilders =
            new HashSet<CollectionBuilder<Task>>();

    public synchronized <T extends Task> AndroidTask<T> create(
            CollectionBuilder<Task> collectionBuilder,
            String taskName,
            Class<T> taskClass,
            Action<T> configureAction) {

        collectionBuilder.create(taskName, taskClass, configureAction);
        final AndroidTask<T> newTask = new AndroidTask<T>(taskName, taskClass, collectionBuilder);
        tasks.put(taskName, newTask);

        if (!collectionBuilders.contains(collectionBuilder)) {
            collectionBuilders.add(collectionBuilder);
            // add a single afterEach listener to be notified after each tasks' configuration, this
            // will allow to retrieve the results of upstream's task configuration.
            collectionBuilder.afterEach(new Action<Task>() {

                @Override
                public void execute(Task task) {

                    AndroidTask androidTask = tasks.get(task.getName());
                    if (androidTask == null) {
                        // not one of our task, just return.
                        return;
                    }
                    androidTask.setTask(task);
                    System.out.println("After each " + task.getName());
                    androidTask.configure();

                }
            });

            // todo : experiment with a way get a hook after a task execution so there would be
            // a way to set further things on downstream tasks.
        }

        return newTask;
    }



}
