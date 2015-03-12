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
public class TopLevelTaskOne extends DefaultTask implements ScopedTask<GlobalScope> {

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

    @Override
    public void initializeWith(GlobalScope scope) {
        this.inputFile = scope.thirdFile;
        this.androidModel = scope.androidModel;
    }
}
