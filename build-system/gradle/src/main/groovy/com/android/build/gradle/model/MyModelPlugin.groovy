/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.build.gradle.model

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.language.base.plugins.ComponentModelBasePlugin
import org.gradle.model.Model
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.platform.base.ComponentSpec
import org.gradle.platform.base.ComponentSpecContainer
import org.gradle.platform.base.ComponentType
import org.gradle.platform.base.ComponentTypeBuilder
import org.gradle.platform.base.component.BaseComponentSpec

/**
 * For experiments only!
 */
class MyModelPlugin implements Plugin<Project> {

    public void apply(Project project) {
        project.getPlugins().apply(ComponentModelBasePlugin.class);
    }


    static public class MyModel extends BaseComponentSpec implements ComponentSpec {
        List<String> tasks = []
    }

    @RuleSource
    static class Rules {
        @ComponentType
        void defineModelType(ComponentTypeBuilder<MyModel> builder) {
//            println "registering MyModel."
            builder.defaultImplementation(MyModel)
        }

        @Model("myModel")
        MyModel myModel(ComponentSpecContainer specContainer) {
//            println "modeling myModel"
//            specContainer.each {
//                println it
//            }
            return specContainer.getByName("myModel")
        }

        @Mutate
        void createMyModel(ComponentSpecContainer specContainer) {
            specContainer.create("myModel", MyModel.class)
        }

        @Mutate
        void addTasks(TaskContainer tasks, MyModel myModel) {
//            println "Adding myModel tasks"
            myModel.tasks.each { n ->
                tasks.create(n) {
                    it.description = "task \$n"
                }
            }
        }
    }
}

