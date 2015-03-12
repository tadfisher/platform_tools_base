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
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by jedo on 3/25/15.
 */
public class DexProducingTask extends DefaultTask implements ScopedTask<GlobalScope> {

    @OutputFile
    DexFile dexFile;

    @TaskAction
    public void doIt() {
        System.out.println("Executing DexProducingTask");
        try {
            FileWriter fileWriter = new FileWriter(dexFile.get());
            fileWriter.append("This is great !");
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void initializeWith(GlobalScope scope) {
        dexFile = scope.getDexBuildableElement();
    }
}
