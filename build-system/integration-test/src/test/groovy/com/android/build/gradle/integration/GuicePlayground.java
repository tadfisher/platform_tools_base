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

package com.android.build.gradle.integration;

import static org.junit.Assert.assertEquals;

import com.android.build.gradle.internal.tasks.MockableAndroidJarTask;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;

import org.gradle.api.Project;
import org.gradle.api.tasks.compile.JavaCompile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Example of how we could try to wire inputs and outputs using Guice.
 */
@RunWith(JUnit4.class)
public class GuicePlayground {

    private static class GlobalValuesModule extends AbstractModule {
        private final Project mProject;

        public GlobalValuesModule(Project project) {
            this.mProject = project;
        }

        @Override
        protected void configure() {
            bind(Project.class).toInstance(mProject);
            bind(File.class).annotatedWith(BuildDir.class).toInstance(mProject.getBuildDir());
        }

        @Provides @MockableAndroidJar
        File provideMockableAndroidJar(@BuildDir File buildDir) {
            return new File(buildDir, "intermediates/mockable-android.jar");
        }
    }

    private static class VariantValuesModule extends AbstractModule {
        private final String mVariantName;

        public VariantValuesModule(String variantName) {
            mVariantName = variantName;
        }

        @Override
        protected void configure() {
            // Only @Provides methods.
        }

        @Provides @ProductionClasses
        File providesProductionClasses(@BuildDir File buildDir) {
            return new File(buildDir, "/outputs/classes" + mVariantName);
        }
    }

    private static class CompileUnitTests extends JavaCompile {
        @Inject
        public void setCompileClassPath(
                Project project,
                @BuildDir File buildDir,
                @MockableAndroidJar File mockableAndroidJar) {
            this.setClasspath(project.files(buildDir, mockableAndroidJar));
        }
    }

    private static class MockableJarInjectableTask extends MockableAndroidJarTask {
        @Inject
        public void setOutput(@MockableAndroidJar File mockableJar) {
            this.setOutputFile(mockableJar);
        }
    }


    @Test
    public void wiring() throws Exception {
        Project gradleProject = Mockito.mock(Project.class);
        Mockito.when(gradleProject.getBuildDir()).thenReturn(new File("myBuildDir"));

        Injector topLevelInjector = Guice.createInjector(new GlobalValuesModule(gradleProject));

        MockableJarInjectableTask topLevelTask = new MockableJarInjectableTask();
        topLevelInjector.injectMembers(topLevelTask);

        assertEquals("myBuildDir/intermediates/mockable-android.jar", topLevelTask.getOutputFile().getPath());

        Injector perVariantInjector = topLevelInjector.createChildInjector(
                new VariantValuesModule("debug"));

        CompileUnitTests perVariantTask = new CompileUnitTests();
        perVariantInjector.injectMembers(perVariantTask);
    }


    @BindingAnnotation
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MockableAndroidJar {}

    @BindingAnnotation
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ProductionClasses {}

    @BindingAnnotation
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BuildDir {}
}
