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

package com.android.build.gradle.tasks.factory;

import static com.android.sdklib.BuildToolInfo.PathId.ZIP_ALIGN;

import com.android.build.gradle.internal.TaskFactory;
import com.android.build.gradle.internal.scope.AndroidTask;
import com.android.build.gradle.internal.scope.ConventionMappingHelper;
import com.android.build.gradle.internal.scope.VariantOutputScope;
import com.android.build.gradle.internal.variant.ApkVariantOutputData;
import com.android.build.gradle.tasks.ZipAlign;

import org.gradle.api.Action;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * Action to configure a ZipAlign task.
 */
public class ZipAlignConfigAction implements Action<ZipAlign> {

    private final VariantOutputScope scope;

    public ZipAlignConfigAction(VariantOutputScope scope) {
        this.scope = scope;
    }

    @Override
    public void execute(ZipAlign zipAlign) {
        ((ApkVariantOutputData)scope.getVariantOutputData()).zipAlignTask = zipAlign;
        ConventionMappingHelper.map(zipAlign, "inputFile", new Callable<File>() {
            @Override
            public File call() throws Exception {
                return scope.getPackageApk();
            }
        });
        ConventionMappingHelper.map(zipAlign, "outputFile", new Callable<File>() {
            @Override
            public File call() throws Exception {
                return scope.getProject().file(
                        scope.getApkLocation() + "/" + scope.getProjectBaseName() + "-" +
                                scope.getVariantOutputData().getBaseName() + ".apk");
            }
        });
        ConventionMappingHelper.map(zipAlign, "zipAlignExe", new Callable<File>() {
            @Override
            public File call() throws Exception {
                String path = scope.getAndroidBuilder().getTargetInfo().getBuildTools()
                        .getPath(ZIP_ALIGN);
                if (path != null) {
                    return new File(path);
                }
                return null;
            }
        });
    }
}
