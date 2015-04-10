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

package com.android.build.gradle.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import com.android.annotations.NonNull;
import com.android.build.gradle.internal.tasks.IncrementalTask;
import com.android.builder.png.VectorDrawableRenderer;
import com.android.ide.common.res2.FileStatus;
import com.android.resources.Density;
import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 * Generates PNGs from an Android vector drawables.
 */
public class GeneratePngsFromVectorDrawablesTask extends IncrementalTask {
    private static final Type TYPE_TOKEN = new TypeToken<Map<String, Collection<String>>>() {}.getType();

    private final VectorDrawableRenderer renderer = new VectorDrawableRenderer();
    private File outputResDirectory;
    private File generatedResDirectory;
    private File mergedResDirectory;
    private Collection<Density> densitiesToGenerate;

    @Override
    protected boolean isIncremental() {
        return true;
    }

    @Override
    protected void doIncrementalTaskAction(Map<File, FileStatus> changedInputs) {
        try {
            File stateFile = getStateFile();
            if (!stateFile.exists()) {
                doFullTaskAction();
            }

            String stateString = Files.toString(stateFile, Charsets.UTF_8);
            Map<String, Collection<String>> state = new Gson().fromJson(stateString, TYPE_TOKEN);

            for (Map.Entry<File, FileStatus> entry : changedInputs.entrySet()) {
                switch (entry.getValue()) {
                    case REMOVED:
                        for (String path : state.get(entry.getKey().getAbsolutePath())) {
                            File file = new File(path);
                            System.out.println("deleting " + path);
                            file.delete();
                        }
                        break;
                    default:
                        throw new RuntimeException("Unsupported operation " + entry.getValue());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doFullTaskAction() {
        SetMultimap<String, String> state = HashMultimap.create();

        try {
            for (File resourceFile : getProject().fileTree(getMergedResDirectory())) {
                if (resourceFile.isDirectory()) {
                    continue;
                }
                if (renderer.isVectorDrawable(resourceFile)) {
                    Collection<File> generatedFiles = renderer.createPngFiles(
                            resourceFile,
                            getGeneratedResDirectory(),
                            getDensitiesToGenerate());

                    for (File generatedFile : generatedFiles) {
                        copyFile(generatedFile, resourceFile, getGeneratedResDirectory(),
                                state);
                    }
                } else {
                    copyFile(resourceFile, resourceFile, getMergedResDirectory(), state);
                }
            }

            File stateFile = getStateFile();
            stateFile.delete();
            Files.write(new Gson().toJson(state.asMap(), TYPE_TOKEN), stateFile, Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private File getStateFile() {
        return new File(getIncrementalFolder(), "state.json");
    }

    private void copyFile(
            File resourceFile,
            File input,
            File resDir,
            SetMultimap<String, String> createdFiles) throws IOException {
        checkNotNull(resDir);
        String relativePath =
                resDir.toURI().relativize(resourceFile.toURI()).getPath();

        File finalFile = new File(getOutputResDirectory(), relativePath);
        Files.createParentDirs(finalFile);
        Files.copy(resourceFile, finalFile);
        createdFiles.put(input.getAbsolutePath(), finalFile.getAbsolutePath());
    }

    @OutputDirectory
    public File getGeneratedResDirectory() {
        return generatedResDirectory;
    }

    public void setGeneratedResDirectory(File generatedResDirectory) {
        this.generatedResDirectory = generatedResDirectory;
    }

    @InputDirectory
    public File getMergedResDirectory() {
        return mergedResDirectory;
    }

    public void setMergedResDirectory(File mergedResDirectory) {
        this.mergedResDirectory = mergedResDirectory;
    }

    /**
     * res directory where the generated PNGs should be put.
     */
    @OutputDirectory
    public File getOutputResDirectory() {
        return outputResDirectory;
    }

    public void setOutputResDirectory(File outputResDirectory) {
        this.outputResDirectory = outputResDirectory;
    }

    @Input
    public Collection<Density> getDensitiesToGenerate() {
        return densitiesToGenerate;
    }

    public void setDensitiesToGenerate(
            Collection<Density> densitiesToGenerate) {
        this.densitiesToGenerate = densitiesToGenerate;
    }

}
