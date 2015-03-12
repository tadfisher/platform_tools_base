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

package com.google.jedo.sample

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.model.Model
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.model.collection.CollectionBuilder
import org.gradle.model.collection.ManagedSet

/**
 * Playground plugin.
 */
class NewModelPlugin implements Plugin<Project> {

    private final static AndroidTasks androidTasks = new AndroidTasks()

    @Override
    void apply(Project project) {
        println "New Model Plugin running"
    }

    static class Rules extends RuleSource {

        @Model("android")
        void createAndroidModel(AndroidModel model) {
            model.setNumber(1)
        }

        @Model
        void createBuildTypes(ManagedSet<BuildType> buildTypes, AndroidModel model) {
            buildTypes.create() {
                name = "DEBUG"
            }
            buildTypes.create() {
                name = "RELEASE"
            }
            model.setBuildTypes(buildTypes)
        }

        @Mutate
        void createTopLevelTask(CollectionBuilder<Task> tasks, AndroidModel model) {
            println("Create top level tasks")

            // so far I am still declaring upstream and downstream tasks, eventually the annotation
            // processor code can do that for me.
            TopLevelTaskOne.factory()
                    .setModel(model)
                    .build(androidTasks, tasks,
                    TopLevelTaskThree.factory()
                            .setOutputFile(File.createTempFile("random", "txt"))
                            .build(androidTasks, tasks));

            // old way of creating tasks mix well.
            androidTasks.create(tasks, "topLevelTwo", TopLevelTaskTwo.class) {
                        println("TopLevelTwo configuration using model ${model.getNumber()}")
                    }
        }
    }
}
