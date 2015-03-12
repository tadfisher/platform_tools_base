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

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.model.collection.CollectionBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 * Class that has field that should be populated after other task's configuration.
 *
 * TODO: experitment with late binding objects like SdkInfo and associated.
 */
public class TopLevelTaskOne extends DefaultTask {

    @Input
    public AndroidModel androidModel;

    @InputFile
    @TopLevelTaskThree.ThirdFile
    public File inputFile;

    @TaskAction
    public void apply() {
        System.out.println("TopLevelTaskOne execution with " + androidModel.getNumber());
        try {
            FileReader fileReader = new FileReader(inputFile);
            char[] charBuffer = new char[1024];
            System.out.println("I read " + fileReader.read(charBuffer) + " characters");
            fileReader.close();
            System.out.println("I got " + new String(charBuffer));
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Factory factory() {
        return new Factory();
    }

    public static class Factory extends AbstractTaskFactory<TopLevelTaskOne> {

        private AndroidModel model;

        public Factory setModel(AndroidModel model) {
            this.model = model;
            return this;
        }

        @Override
        protected void configure(TopLevelTaskOne instance) {
            instance.androidModel = model;
        }

        public AndroidTask<TopLevelTaskOne> build(
                AndroidTasks androidTasks,
                CollectionBuilder<Task> tasks,
                AndroidTask<Task>... upstreamTasks) {

            return super.build(TopLevelTaskOne.class, androidTasks, tasks, upstreamTasks);
        }

    }

    public static class TopLevelOneInputSetter {

        static <T> T getTask(Class<T> taskType, Map<Class<?>, ?> tasks) {
            return taskType.cast(tasks.get(taskType));
        }

        public static void set(TopLevelTaskOne topLevelTaskOne, Map<Class<?>, ?> otherTasks) {
            topLevelTaskOne.inputFile =
                    getTask(TopLevelTaskThree.class, otherTasks).createdFile;
        }
    }
}
