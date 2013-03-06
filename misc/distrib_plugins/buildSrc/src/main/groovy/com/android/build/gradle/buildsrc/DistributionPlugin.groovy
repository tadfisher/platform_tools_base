/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.build.gradle.buildsrc
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar

class DistributionPlugin implements org.gradle.api.Plugin<Project> {

    private Project project
    private DistributionExtension extension

    @Override
    void apply(Project project) {
        this.project = project

        extension = project.extensions.create('distribution', DistributionExtension)

        // put some tasks on the project.
        Task pushDistribution = project.tasks.create("pushDistribution")
        pushDistribution.group = "Upload"
        pushDistribution.description = "Push the distribution artifacts into the prebuilt folder"

        // if this is the top project.
        if (project.rootProject == project) {
            // deal with NOTICE files from all the sub projects
        } else {
            Jar buildTask = project.tasks.add("buildDistributionJar", Jar)
            buildTask.from(project.sourceSets.main.output)
            buildTask.conventionMapping.destinationDir = { project.file(extension.destinationPath + "/lib") }
            buildTask.conventionMapping.archiveName = { project.archivesBaseName + ".jar" }

            pushDistribution.dependsOn buildTask

            // delay computing the manifest classpath only if the
            // prebuiltJar task is set to run.
            project.gradle.taskGraph.whenReady { taskGraph ->
                if (taskGraph.hasTask(project.tasks.buildDistributionJar)) {
                    project.tasks.buildDistributionJar.manifest.attributes("Class-Path": getClassPath())
                }
            }

            // also copy the dependencies
            CopyDependenciesTask copyDependenciesTask = project.tasks.add(
                    "copyDependencies", CopyDependenciesTask)
            copyDependenciesTask.project = project
            copyDependenciesTask.conventionMapping.distributionDir =  { project.file(extension.destinationPath + "/lib") }

            pushDistribution.dependsOn copyDependenciesTask
        }
    }

    def getClassPath() {
        String classPath = ""
        project.configurations.runtime.files.each { file ->
            String name = file.name
            String suffix = "-" + project.version + ".jar"
            if (name.endsWith(suffix)) {
                name = name.substring(0, name.size() - suffix.size()) + ".jar"
            }
            classPath = classPath + " " + name
        }
        return classPath
    }
}
