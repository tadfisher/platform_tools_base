/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.ant;

import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.SdkManager;
import com.android.sdklib.repository.FullRevision;
import com.android.utils.NullLogger;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class GetBuildToolsTask extends Task {

    private String mName;

    public void setName(String name) {
        mName = name;
    }

    @Override
    public void execute() throws BuildException {
        Project antProject = getProject();
        String buildToolsVersion = antProject.getProperty("sdk.buildtools");

        if (buildToolsVersion == null) {
            throw new BuildException("Missing sdk.buildtools in project.properties");
        }

        SdkManager sdkManager = SdkManager.createManager(antProject.getProperty("sdk.dir"),
                NullLogger.getLogger());

        if (sdkManager == null) {
            throw new BuildException("Unable to parse the SDK!");
        }

        BuildToolInfo buildTools = sdkManager.getBuildTool(
                FullRevision.parseRevision(buildToolsVersion));

        if (buildTools == null) {
            throw new BuildException(
                    String.format("Build Tools version '%s' not found in SDK.", buildToolsVersion));
        }

        antProject.setProperty(mName, buildTools.getLocation().getAbsolutePath());
    }
}
