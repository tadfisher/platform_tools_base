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
import org.gradle.launcher.daemon.protocol.Build
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
        }

        @Mutate
        void createTopLevelTask(CollectionBuilder<Task> tasks, AndroidModel model, ManagedSet<BuildType> buildTypes) {
            println("Create top level tasks")

            DexFile dexFile = new DexFile(File.createTempFile("random", ".dex"));
            GlobalScope globalScope = new GlobalScope(dexFile, File.createTempFile("random", "txt"), model);

            AndroidTask<TopLevelTaskOne> topLevelTaskOne =
                    androidTasks.create(tasks, "topLevelTaskOne", TopLevelTaskOne.class, globalScope);
            AndroidTask<TopLevelTaskThree> topLevelTaskThree =
                    androidTasks.create(tasks, "topLevelTaskThree", TopLevelTaskThree.class, globalScope);
            AndroidTask<AggregatingTask> aggregatingTask =
                    androidTasks.create(tasks, "aggregatingTask", AggregatingTask.class, globalScope);

            AndroidTask<AggregatingTask> dexProducingTask =
                    androidTasks.create(tasks, "dexProducingTask", DexProducingTask.class, globalScope);

            dexProducingTask.produces(dexFile);

            for (BuildType buildType : buildTypes) {
                System.out.println("Build Tyype " + buildType.name);
                VariantScope variantScope = new VariantScope(globalScope, buildType.name);
                String taskName = "variant" + buildType.name + "Task";
                aggregatingTask.dependsOn(
                    androidTasks.create(tasks, taskName, PerVariantTask.class, variantScope));

            }

            topLevelTaskOne.dependsOn(topLevelTaskThree);
            topLevelTaskOne.dependsOn(aggregatingTask);
            // this should not be necessary but it is, why ?
            topLevelTaskOne.dependsOn(dexProducingTask);
        }
    }
}
