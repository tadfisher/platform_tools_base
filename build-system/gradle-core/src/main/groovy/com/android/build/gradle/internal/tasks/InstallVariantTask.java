/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.build.gradle.internal.tasks;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.OutputFile;
import com.android.build.VariantOutput;
import com.android.build.gradle.internal.LoggerWrapper;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.build.gradle.internal.variant.BaseVariantOutputData;
import com.android.builder.core.SplitSelectTool;
import com.android.builder.core.VariantConfiguration;
import com.android.builder.internal.InstallUtils;
import com.android.builder.sdk.SdkInfo;
import com.android.builder.sdk.TargetInfo;
import com.android.builder.testing.ConnectedDeviceProvider;
import com.android.builder.testing.api.DeviceConfig;
import com.android.builder.testing.api.DeviceConnector;
import com.android.builder.testing.api.DeviceException;
import com.android.builder.testing.api.DeviceProvider;
import com.android.ide.common.build.SplitOutputMatcher;
import com.android.ide.common.process.ProcessException;
import com.android.sdklib.BuildToolInfo;
import com.android.utils.ILogger;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Task installing an app variant. It looks at connected device and install the best matching
 * variant output on each device.
 */
public class InstallVariantTask extends BaseTask {

    private int timeOutInMs = 0;

    private Collection<String> installOptions;

    private BaseVariantData<? extends BaseVariantOutputData> variantData;

    public InstallVariantTask() {
        this.getOutputs().upToDateWhen(new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task task) {
                getLogger().debug("Install task is always run.");
                return false;
            }
        });
    }

    @TaskAction
    void install() throws DeviceException {

        ILogger lifecycleLogger = new LoggerWrapper(getLogger(), LogLevel.LIFECYCLE);

        VariantConfiguration variantConfig = variantData.getVariantConfiguration();
        String variantName = variantConfig.getFullName();
        String projectName = getProject().getName();
        getLogger().info("Installing app: {}:{}", getProject().getName(), variantName);
        DeviceProvider deviceProvider = new ConnectedDeviceProvider(getAdbExe());
        deviceProvider.init();  // will fail if no devices found.

        int successfulInstallCount = 0;

        final List<? extends DeviceConnector> devices = deviceProvider.getDevices();

        final List<DeviceConnector> activeDevices = InstallUtils
                .keepOnlineDevices(devices, lifecycleLogger);

        final List<DeviceConnector> candidateDevices =
                Lists.newArrayListWithCapacity(activeDevices.size());

        for (DeviceConnector device : activeDevices) {
            if (InstallUtils.checkDeviceApiLevel(device, variantConfig.getMinSdkVersion(),
                    lifecycleLogger, projectName, variantName)) {
                candidateDevices.add(device);
            }
            // When InstallUtils.checkDeviceApiLevel returns false, it logs the reason.
        }

        if (candidateDevices.isEmpty()) {
            throw new DeviceException(String.format(
                    "Failed to find any devices compatible with %s:%s", projectName, variantName));
        }

        for (DeviceConnector device : candidateDevices) {
            // build the list of APKs.
            List<String> splitApksPath = new ArrayList<String>();
            OutputFile mainApk = null;
            for (VariantOutput output : variantData.getOutputs()) {
                for (OutputFile outputFile : output.getOutputs()) {
                    if (!outputFile.getOutputFile().getAbsolutePath().equals(
                            output.getMainOutputFile().getOutputFile().getAbsolutePath())) {
                        splitApksPath.add(outputFile.getOutputFile().getAbsolutePath());
                    }
                }
                mainApk = output.getMainOutputFile();
            }

            if (getSplitSelectExe() == null && !splitApksPath.isEmpty()) {
                throw new GradleException(
                        "Pure splits installation requires build tools 22 or above");
            }
            if (mainApk == null) {
                throw new GradleException(
                        "Cannot retrieve the main APK from variant outputs");
            }
            List<File> apkFiles;
            if (!splitApksPath.isEmpty()) {
                DeviceConfig deviceConfig = device.getDeviceConfig();
                Set<String> resultApksPath = new HashSet<String>();
                for (String abi : device.getAbis()) {
                    try {
                        resultApksPath.addAll(SplitSelectTool.splitSelect(
                                getBuilder().getProcessExecutor(),
                                getSplitSelectExe(),
                                deviceConfig.getConfigFor(abi),
                                mainApk.getOutputFile().getAbsolutePath(),
                                splitApksPath));
                    } catch (ProcessException e) {
                        throw new GradleException(e.getMessage());
                    }
                }
                apkFiles = new ArrayList<File>();
                for (String resultApkPath : resultApksPath) {
                    apkFiles.add(new File(resultApkPath));
                }
                // and add back the main APK.
                apkFiles.add(mainApk.getOutputFile());
            } else {
                // now look for a matching output file
                List<OutputFile> outputFiles = SplitOutputMatcher.computeBestOutput(
                        variantData.getOutputs(),
                        variantData.getVariantConfiguration().getSupportedAbis(),
                        device.getDensity(),
                        device.getLanguage(),
                        device.getRegion(),
                        device.getAbis());
                apkFiles = Lists.transform(outputFiles, new Function<OutputFile, File>() {
                    @Override
                    public File apply(OutputFile outputFile) {
                        return outputFile.getOutputFile();
                    }
                });
            }

            if (apkFiles.isEmpty()) {
                getLogger().lifecycle("Skipping devices '{}' for '{}:{}': Could not find build of"
                                + "variant which supports density {} and ABI {}",
                        device.getName(), projectName, variantName,
                        device.getDensity(), Joiner.on(" or ").join(device.getAbis()));
            } else {
                if (getLogger().isLifecycleEnabled()) {
                    List<String> apkNames = Lists.transform(
                            apkFiles, new Function<File, String>() {
                                @Override
                                public String apply(File file) {
                                    return file.getName();
                                }
                            });
                    getLogger().lifecycle("Installing '{}' from {}:{} on '{}' ({})",
                            Joiner.on(", ").join(apkNames),
                            projectName,
                            variantName,
                            device.getName(),
                            device.getSerialNumber());
                }

                List<String> extraArgs = installOptions == null ?
                        ImmutableList.<String>of() : ImmutableList.copyOf(installOptions);
                if (apkFiles.size() > 1 || device.getApiLevel() >= 21) {
                    device.installPackages(
                            apkFiles, extraArgs, getTimeOutInMs(), getILogger());
                    successfulInstallCount++;
                } else {
                    device.installPackage(
                            apkFiles.get(0), extraArgs, getTimeOutInMs(), getILogger());
                    successfulInstallCount++;
                }
            }
        }

        if (successfulInstallCount == 0) {
            throw new GradleException("Failed to install on any devices.");
        } else {
            getLogger().quiet("Installed on {} device{}",
                    successfulInstallCount, successfulInstallCount == 1 ? "" : "s");
        }
    }

    @InputFile
    public File getAdbExe() {
        SdkInfo sdkInfo = getBuilder().getSdkInfo();
        if (sdkInfo == null) {
            return null;
        }
        return sdkInfo.getAdb();
    }

    @InputFile
    @Optional
    public File getSplitSelectExe() {
        TargetInfo targetInfo = getBuilder().getTargetInfo();
        if (targetInfo == null) {
            return null;
        }
        String path = targetInfo.getBuildTools().getPath(
                BuildToolInfo.PathId.SPLIT_SELECT);
        if (path != null) {
            File splitSelectExe = new File(path);
            return splitSelectExe.exists() ? splitSelectExe : null;
        } else {
            return null;
        }
    }


    @Input
    public int getTimeOutInMs() {
        return timeOutInMs;
    }

    public void setTimeOutInMs(int timeOutInMs) {
        this.timeOutInMs = timeOutInMs;
    }

    @Input
    @Optional
    public Collection<String> getInstallOptions() {
        return installOptions;
    }

    public void setInstallOptions(@Nullable Collection<String> installOptions) {
        this.installOptions = installOptions;
    }

    public BaseVariantData<? extends BaseVariantOutputData> getVariantData() {
        return variantData;
    }

    public void setVariantData(
            @NonNull BaseVariantData<? extends BaseVariantOutputData> variantData) {
        this.variantData = variantData;
    }

}
