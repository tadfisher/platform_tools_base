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

package com.android.build.gradle.integration.application

import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.build.gradle.integration.common.fixture.app.HelloWorldApp
import com.android.build.gradle.integration.common.fixture.app.TestSourceFile
import com.google.common.base.Joiner
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

import static org.junit.Assert.assertFalse
import static org.junit.Assert.fail

/**
 * Created by cmw on 11/14/14.
 */
class GenerateAnnotationsClassPathTest {
    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromSample("regular/extractAnnotations").create()

    @BeforeClass
    static public void setUpProject() {
        File use = project.file(
                "src/main/java/com/android/tests/extractannotations/HelloWorld.java")

        use.parentFile.mkdirs()
        use << """
import com.example.helloworld.GeneratedClass;

public class HelloWorld {

    public void go() {
        GeneratedClass genC = new GeneratedClass();
        genC.method();
    }
}"""

        project.getBuildFile() << '''

import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.google.common.base.Joiner


class JavaGeneratingPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        System.err.println('Plugin applied')

        project.plugins.withType(LibraryPlugin) {
            def androidPlugin = project.plugins.getPlugin(LibraryPlugin)
            def variants = ((LibraryExtension) androidPlugin.extension).libraryVariants
            variants.all { BaseVariant variant -> registerTask(project, variant) }
        }

    }

    private void registerTask(Project project, BaseVariant variant) {
        def outDir = project.file("$project.buildDir/generated/source/testplugin/$variant.name");
        def task = project.task(
                "generateJavaFromPlugin${variant.name.capitalize()}",
                dependsOn: [variant.mergeResources],
                type: JavaGeneratingTask) {
            outputDirectory = outDir
        }
        variant.registerJavaGeneratingTask(task, outDir)
    }
}



public class JavaGeneratingTask extends DefaultTask {
    @OutputDirectory
    File outputDirectory

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        System.err.println('Plugin executed on ' + inputs)
        File outputFile = new File(outputDirectory, Joiner.on(File.separatorChar).join(
                "com", "example", "helloworld", "GeneratedClass.java"))
                System.err.println("creating file " + outputFile)
        outputFile.getParentFile().mkdirs()

        outputFile << """
package com.example.helloworld;

public class GeneratedClass {
    public void method() {
        System.out.println("Executed generated method");
    }
}

    """
    }
}
apply plugin: JavaGeneratingPlugin

'''
    }

    /**
     * Check variant.registerJavaGeneratingTask() adds output directory to the class path of
     * the generate annotations task
     */
    @Test
    public void "check javaGeneratingTask adds output dir to generate annotations classpath"() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        project.execute(outputStream, "clean", "assembleDebug")
        String output = outputStream.toString("UTF-8")
        assertFalse(
                "Extract annotation should get generated class on path.",
                output.contains("Not extracting annotations (compilation problems encountered)"))
    }

}
