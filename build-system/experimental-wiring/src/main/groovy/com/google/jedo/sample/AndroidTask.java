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

import com.android.annotations.NonNull;
import com.android.build.annotation.BindingParameter;
import com.android.build.annotation.TaskSetter;
import com.google.common.collect.ImmutableMap;

import org.gradle.api.Task;
import org.gradle.model.collection.CollectionBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper for a {@link Task}, will allow us to keep interesting information like what tasks
 * depend on this task, the which ones this task depends on. Eventually we can tight things further
 * to make sure tasks are never retrieved using string based APIs.
 */
public class AndroidTask<T extends Task> {

    private String taskName;
    private final Class<T> taskType;
    private final CollectionBuilder<Task> collectionBuilder;
    private T task;
    private final List<AndroidTask<Task>> upstreamTasks;
    private final List<AndroidTask<? extends Task>> downstreamTasks;
    private boolean configured = false;
    private boolean userConfigured = false;

    public AndroidTask(String taskName, Class<T> taskType, CollectionBuilder<Task> collectionBuilder) {
        this.taskName = taskName;
        this.taskType = taskType;
        this.collectionBuilder = collectionBuilder;
        upstreamTasks = new ArrayList<AndroidTask<Task>>();
        downstreamTasks = new ArrayList<AndroidTask<? extends Task>>();
    }

    /**
     * this should be called directly by the annotation processor code. As you can see, right now,
     * the dependency is not declared to gradle, just in this internal model. The gradle
     * dependency is added during field wiring.
     *
     * @param other an upstream task.
     */


    public void dependsOn(@NonNull AndroidTask<Task> other) {
        upstreamTasks.add(other);
        other.addDependent(this);
    }

    private void addDependent(AndroidTask<? extends Task> tAndroidTask) {
        downstreamTasks.add(tAndroidTask);
    }

    public T getTask() {
        if (task == null) {
            task = taskType.cast(collectionBuilder.get(taskName));
        }
        return task;
    }

    private Map<Class<?>, ?> getUpstreamTasks() {
        ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
        for (AndroidTask<Task> upstreamTask : upstreamTasks) {
            builder.put(upstreamTask.taskType, upstreamTask.task);
            System.out.println("Adding upstream " + upstreamTask.taskType);
        }
        return builder.build();
    }

    /**
     * Loads the annotation processor generated setter class and invoke it.
     */
    public void configureWithSetter() {
        System.out.println("Configuring " + taskName);
        try {
            Class setterClass = task.getClass().getClassLoader()
                    .loadClass(taskType.getName() + "___Setter");
            TaskSetter<Task> taskSetter = (TaskSetter<Task>) setterClass.newInstance();
            System.out.println("Using setter " + taskSetter);
            taskSetter.inject(task, getUpstreamTasks());
            System.out.println("Done configuring " + taskName);
            return;
        } catch (ClassNotFoundException e) {
            // not setter, no need for injection..
            return;
        } catch (Exception e) {
            e.printStackTrace();
            // it's ok, just revert to slow instrospection based mechanism.
        }
        configureWithIntrospection();
    }

    /**
     * Slow alternative to generated code, it's using java introspection which
     * is notoriously slow.
     */
    private void configureWithIntrospection() {

        for (AndroidTask<Task> upstreamTask : upstreamTasks) {

            Map<Class<? extends Annotation>, Field> fieldsToWire
                    = new HashMap<Class<? extends Annotation>, Field>();
            for (Field f : task.getClass().getFields()) {
                System.out.println(" input " + f.getName());
                for (Annotation a : f.getAnnotations()) {
                    BindingParameter bindingParameter =
                            a.annotationType().getAnnotation(BindingParameter.class);
                    if (bindingParameter != null) {
                        fieldsToWire.put(a.annotationType(), f);
                    }
                }
            }

            for (Field f : upstreamTask.taskType.getFields()) {
                System.out.println(" output " + f.getName());
                for (Annotation a : f.getAnnotations()) {
                    if (fieldsToWire.containsKey(a.annotationType())) {
                        Field targetField = fieldsToWire.get(a.annotationType());
                        System.out.println("Wire " + f.getName() + " to " + targetField);
                        try {
                            task.dependsOn(upstreamTask.getTask());
                            targetField.set(task, f.get(upstreamTask.getTask()));
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    public synchronized void configure() {
        if (configured) {
            return;
        }
        userConfigured = true;
        // if all my upstream tasks are configured, I can configure myself.
        for (AndroidTask<Task> upstreamTask : upstreamTasks) {
            if (!upstreamTask.configured) {
                return;
            }
        }
        configureWithSetter();
        configured = true;
        // now that I am configured, let's give a chance to my dependents.
        for (AndroidTask<? extends Task> downstreamTask : downstreamTasks) {
            if (downstreamTask.userConfigured) {
                downstreamTask.configure();
            }
        }
    }

    public void setTask(Task task) {
        this.task = taskType.cast(task);
    }
}
