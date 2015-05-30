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

package com.android.build.gradle.internal;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Class to hold data to setup the many optional post-compilation steps.
 */
public class PostCompilationData {

    private List<?> classGeneratingTask;

    private List<?> libraryGeneratingTask;

    private Callable<List<File>> inputFiles;

    private Callable<File> inputDir;

    private Callable<File> javaResourcesInputDir;

    private Callable<List<File>> inputLibraries;

    private static <T> Callable<T> wrapValue(final T value) {
        return new Callable<T>() {
            @Override
            public T call() throws Exception {
                return value;
            }
        };
    }

    public List<?> getClassGeneratingTask() {
        return classGeneratingTask;
    }

    public void setClassGeneratingTask(List<?> classGeneratingTask) {
        this.classGeneratingTask = classGeneratingTask;
    }

    public List<?> getLibraryGeneratingTask() {
        return libraryGeneratingTask;
    }

    public void setLibraryGeneratingTask(List<?> libraryGeneratingTask) {
        this.libraryGeneratingTask = libraryGeneratingTask;
    }

    public Callable<List<File>> getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(List<File> inputFiles) {
        this.inputFiles = wrapValue(inputFiles);
    }

    public void setInputFiles(Callable<List<File>> inputFiles) {
        this.inputFiles = inputFiles;
    }

    public Callable<File> getInputDir() {
        return inputDir;
    }

    public void setInputDir(File inputDir) {
        this.inputDir = wrapValue(inputDir);
    }

    public void setInputDir() {
        this.inputDir = null;
    }

    public void setInputDir(Callable<File> inputDir) {
        this.inputDir = inputDir;
    }

    public Callable<File> getJavaResourcesInputDir() {
        return javaResourcesInputDir;
    }

    public void setJavaResourcesInputDir(File javaResourcesInputDir) {
        this.javaResourcesInputDir = wrapValue(javaResourcesInputDir);
    }

    public void setJavaResourcesInputDir(Callable<File> javaResourcesInputDir) {
        this.javaResourcesInputDir = javaResourcesInputDir;
    }

    public Callable<List<File>> getInputLibraries() {
        return inputLibraries;
    }

    public void setInputLibraries(List<File> inputLibraries) {
        this.inputLibraries = wrapValue(inputLibraries);
    }

    public void setInputLibraries(Callable<List<File>> inputLibraries) {
        this.inputLibraries = inputLibraries;
    }
}
