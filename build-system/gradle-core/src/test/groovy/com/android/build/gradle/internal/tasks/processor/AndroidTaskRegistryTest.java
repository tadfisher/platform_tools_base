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

import static com.google.common.truth.Truth.assertThat;

import com.android.build.annotation.BindingParameter;
import com.android.build.gradle.internal.TaskContainerAdaptor;
import com.android.build.gradle.internal.TaskFactory;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test for AndroidTaskRegistry.
 */
public class AndroidTaskRegistryTest {

    @BindingParameter
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface Type1 {};

    @BindingParameter
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD})
    @interface Type2 {};

    public static class Producer extends DefaultTask {
        @OutputFile
        @Type1
        public String output;
    }

    public static class MiddleMan extends DefaultTask {
        @InputFile
        @Type1
        public String input;

        @OutputFile
        @Type2
        public String output;
    }

    public static class Consumer extends DefaultTask {
        @InputFile
        @Type2
        @Type1
        public String input;
    }

    @Rule
    public TemporaryFolder projectDir = new TemporaryFolder();

    Project project;

    TaskFactory tasks;

    AndroidTaskRegistry registry;

    @Before
    public void setup() throws Exception {
        project = ProjectBuilder.builder().withProjectDir(projectDir.newFolder()).build();
        tasks = new TaskContainerAdaptor(project.getTasks());
        registry = new AndroidTaskRegistry();
    }

    @Test
    public void simpleDependencies() {
        AndroidTask task1 = registry.create(tasks, "task1", Producer.class);
        AndroidTask task2 = registry.create(tasks, "task2", Consumer.class);

        assertThat(task1.getUpstreamTasks()).isEmpty();
        assertThat(task1.getDownstreamTasks()).containsExactly(task2);
        assertThat(task2.getUpstreamTasks()).containsExactly(task1);
        assertThat(task2.getDownstreamTasks()).isEmpty();
    }

    @Test
    public void chainDependencies() {
        AndroidTask<Producer> task1 = registry.create(tasks, "task1", Producer.class,
                new Action<Producer>() {
                    @Override
                    public void execute(Producer producer) {
                        producer.output = "output1";
                    }
                });
        AndroidTask<MiddleMan> task2 = registry.create(tasks, "task2", MiddleMan.class,
                new Action<MiddleMan>() {
                    @Override
                    public void execute(MiddleMan middleMan) {
                        middleMan.output = "output2";
                    }
                });
        AndroidTask<Consumer> task3 = registry.create(tasks, "task3", Consumer.class);

        assertThat(task1.getUpstreamTasks()).isEmpty();
        assertThat(task1.getDownstreamTasks()).containsExactly(task2, task3);
        assertThat(task2.getUpstreamTasks()).containsExactly(task1);
        assertThat(task2.getDownstreamTasks()).containsExactly(task3);
        assertThat(task3.getUpstreamTasks()).containsExactly(task1, task2);
        assertThat(task3.getDownstreamTasks()).isEmpty();

        assertThat(task3.getTask()).isNotNull();
        assertThat(task3.getTask().input).isEqualTo("output2");
    }
}
