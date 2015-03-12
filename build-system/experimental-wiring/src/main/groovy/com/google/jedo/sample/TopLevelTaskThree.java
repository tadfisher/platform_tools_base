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

import com.android.build.annotation.BindingParameter;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by jedo on 3/4/15.
 */
public class TopLevelTaskThree extends DefaultTask implements ScopedTask<GlobalScope> {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    @Scoping(GlobalScope.class)
    public @interface DexFile {
    }

    @Input
    public int inputNumber;

    @OutputFile
    @DexFile
    public File outputFile;

    @TaskAction
    public void apply() {
        System.out.println("TopLevelTaskThree executing : " + inputNumber);
        try {
            System.out.println("and writing to " + outputFile.getAbsolutePath());
            FileWriter fileWriter = new FileWriter(outputFile);
            fileWriter.append(String.format("%d", inputNumber));
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initializeWith(GlobalScope scope) {
        inputNumber = 7;
        // an annotation process could verify that Scoping is correct.
        outputFile = scope.getDexFile();
    }
}
