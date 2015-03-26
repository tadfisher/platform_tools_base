/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.build.gradle.internal.LoggerWrapper;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.builder.internal.InstallUtils;
import com.android.builder.sdk.SdkInfo;
import com.android.builder.testing.ConnectedDeviceProvider;
import com.android.builder.testing.api.DeviceConnector;
import com.android.builder.testing.api.DeviceException;
import com.android.builder.testing.api.DeviceProvider;
import com.android.ddmlib.IDevice;
import com.android.utils.ILogger;

import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.List;

public class UninstallTask extends BaseTask {

    private BaseVariantData variant;

    private int mTimeOutInMs = 0;

    public UninstallTask() {
        this.getOutputs().upToDateWhen(new Spec<Task>() {
            @Override
            public boolean isSatisfiedBy(Task task) {
                getLogger().debug("Uninstall task is always run.");
                return false;
            }
        });
    }

    @TaskAction
    public void uninstall() throws DeviceException {
        ILogger lifecycleLogger = new LoggerWrapper(getLogger(), LogLevel.LIFECYCLE);
        final String applicationId = variant.getApplicationId();

        getLogger().info("Uninstalling app: " + applicationId);
        final DeviceProvider deviceProvider = new ConnectedDeviceProvider(getAdbExe());

        deviceProvider.init();
        final List<? extends DeviceConnector> devices = deviceProvider.getDevices();

        final List<DeviceConnector> onlineDevices = InstallUtils.keepOnlineDevices(devices, lifecycleLogger);

        for (DeviceConnector device : onlineDevices) {
            device.uninstallPackage(applicationId, getTimeOutInMs(), getILogger());
            logUninstall(device, applicationId);
        }

        int n = onlineDevices.size();
        getLogger().quiet("Uninstalled {} from {} device{}.",
                applicationId, n, n == 1 ? "" : "s");

    }

    private void logUninstall(DeviceConnector device, String applicationId) {
        getLogger().lifecycle(
                "Uninstalling {} (from {}:{}) from device '{}' ({}).",
                applicationId, getProject().getName(),
                variant.getVariantConfiguration().getFullName(),
                device.getName(), device.getSerialNumber());
    }

    @InputFile
    public File getAdbExe() {
        SdkInfo sdkInfo = getBuilder().getSdkInfo();
        if (sdkInfo == null) {
            return null;
        }
        return sdkInfo.getAdb();
    }

    public BaseVariantData getVariant() {
        return variant;
    }

    public void setVariant(BaseVariantData variant) {
        this.variant = variant;
    }

    @Input
    public int getTimeOutInMs() {
        return mTimeOutInMs;
    }

    public void setTimeOutInMs(int timeoutInMs) {
        mTimeOutInMs = timeoutInMs;
    }

    @Input
    @Optional
    public String getSerialFilter() {
        return System.getenv("ANDROID_SERIAL");
    }

    private void logIgnoreDevice(DeviceConnector device) {
        getLogger().lifecycle(
                "Skipping device '{}' ({}) for  uninstall '{}:{}': Device in {} state{}",
                device.getName(), device.getSerialNumber(), getProject().getName(),
                variant.getVariantConfiguration().getFullName(), device.getState(),
                device.getState().equals(IDevice.DeviceState.UNAUTHORIZED) ?
                        ", see http://developer.android.com/tools/help/adb.html#Enabling." : ".");
    }


}
