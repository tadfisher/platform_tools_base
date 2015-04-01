package com.android.build.gradle.internal.tasks;


import static com.android.ddmlib.IDevice.DeviceState.UNAUTHORIZED;
import static com.android.sdklib.BuildToolInfo.PathId.SPLIT_SELECT;

import com.android.build.gradle.internal.TaskManager;
import com.android.build.gradle.internal.scope.ConventionMappingHelper;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.variant.ApkVariantData;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.build.gradle.internal.variant.BaseVariantOutputData;
import com.android.builder.core.VariantConfiguration;
import com.android.builder.internal.InstallUtils;
import com.android.builder.sdk.SdkInfo;
import com.android.builder.sdk.TargetInfo;
import com.android.builder.testing.ConnectedDeviceProvider;
import com.android.builder.testing.api.DeviceConfigProviderImpl;
import com.android.builder.testing.api.DeviceConnector;
import com.android.builder.testing.api.DeviceException;
import com.android.builder.testing.api.DeviceProvider;
import com.android.ide.common.build.SplitOutputMatcher;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessExecutor;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import groovy.lang.Closure;

/**
 * Task installing an app variant. It looks at connected device and install the best matching
 * variant output on each device.
 */
public class InstallVariantTask extends BaseTask {
    private File adbExe;

    private File splitSelectExe;

    private ProcessExecutor processExecutor;

    private String projectName;

    private int timeOutInMs = 0;

    private Collection<String> installOptions;

    private BaseVariantData<? extends BaseVariantOutputData> variantData;

    @InputFile
    public File getAdbExe() {
        return adbExe;
    }

    public void setAdbExe(File adbExe) {
        this.adbExe = adbExe;
    }

    @InputFile
    @Optional
    public File getSplitSelectExe() {
        return splitSelectExe;
    }

    public void setSplitSelectExe(File splitSelectExe) {
        this.splitSelectExe = splitSelectExe;
    }

    public ProcessExecutor getProcessExecutor() {
        return processExecutor;
    }

    public void setProcessExecutor(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
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

    public void setInstallOptions(Collection<String> installOptions) {
        this.installOptions = installOptions;
    }

    public BaseVariantData<? extends BaseVariantOutputData> getVariantData() {
        return variantData;
    }

    public void setVariantData(BaseVariantData<? extends BaseVariantOutputData> variantData) {
        this.variantData = variantData;
    }


    public InstallVariantTask() {
        this.getOutputs().upToDateWhen(new Closure<Boolean>(this, this) {
            public Boolean doCall(Task it) {
                getLogger().debug("Install task is always run.");
                return false;
            }

            public Boolean doCall() {
                return doCall(null);
            }

        });
    }

    @TaskAction
    public void install() throws DeviceException, ProcessException {
        DeviceProvider deviceProvider = new ConnectedDeviceProvider(getAdbExe());
        deviceProvider.init();

        VariantConfiguration variantConfig = variantData.getVariantConfiguration();
        final String variantName = variantConfig.getFullName();

        final String serial = System.getenv("ANDROID_SERIAL");

        int successfulInstallCount = 0;

        for (DeviceConnector device : ((ConnectedDeviceProvider) deviceProvider).getDevices()) {
            if (serial != null && !serial.equals(device.getSerialNumber())) {
                continue;
            }

            if (!device.getState()
                    .equals(UNAUTHORIZED)) {
                if (InstallUtils
                        .checkDeviceApiLevel(device, variantConfig.getMinSdkVersion(), getILogger(),
                                projectName, variantName)) {

                    final List<File> apkFiles = SplitOutputMatcher
                            .computeBestOutput(processExecutor, getSplitSelectExe(),
                                    new DeviceConfigProviderImpl(device), variantData.getOutputs(),
                                    variantData.getVariantConfiguration().getSupportedAbis());

                    if (apkFiles.isEmpty()) {
                        getLogger().lifecycle(
                                "Skipping device \'" + device.getName() + "\' for \'" + projectName
                                        + ":" + variantName + "\': "
                                        + "Could not find build of variant which supports density "
                                        + device.getDensity() + " "
                                        + "and an ABI in "
                                        + Joiner.on(", ").join(device.getAbis()));
                    } else {
                        Iterable<String> apkFileNames = Iterables.transform(apkFiles,
                                new Function<File, String>() {
                                    @Override
                                    public String apply(File file) {
                                        return file.getName();
                                    }
                                });
                        getLogger().lifecycle("Installing APK \'" +
                                Joiner.on(", ").join(apkFileNames) + "\' on \'" +
                                device.getName() + "\'");

                        Collection<String> extraArgs = installOptions == null ? ImmutableList.<String>of()
                                : installOptions;
                        if (apkFiles.size() > 1 || device.getApiLevel() >= 21) {
                            device.installPackages(apkFiles, extraArgs, getTimeOutInMs(),
                                    getILogger());
                            successfulInstallCount++;
                        } else {
                            device.installPackage(apkFiles.get(0), extraArgs, getTimeOutInMs(),
                                    getILogger());
                            successfulInstallCount++;
                        }
                    }
                } // When InstallUtils.checkDeviceApiLevel returns false, it logs the reason.
            } else {
                getLogger().lifecycle(
                        "Skipping device \'" + device.getName() + "\' for \'" + projectName + ":"
                                + variantName
                                + "': Device not authorized, see "
                                + "http://developer.android.com/tools/help/adb.html#Enabling.");

            }

        }

        if (successfulInstallCount == 0) {
            if (serial != null) {
                throw new GradleException("Failed to find device with serial \'" + serial + "\'. "
                        + "Unset ANDROID_SERIAL to search for any device.");
            } else {
                throw new GradleException("Failed to install on any devices.");
            }

        } else {
            getLogger().quiet("Installed on " + successfulInstallCount + " "
                    + (successfulInstallCount == 1 ? "device" : "devices" + "."));
        }
    }

    public static class ConfigAction implements Action<InstallVariantTask> {

        private final VariantScope scope;

        public ConfigAction(VariantScope scope) {
            this.scope = scope;
        }

        @Override
        public void execute(InstallVariantTask installTask) {
            installTask.setDescription("Installs the " + scope.getVariantData().getDescription() + ".");
            installTask.setGroup(TaskManager.INSTALL_GROUP);
            installTask.setProjectName(scope.getGlobalScope().getProject().getName());
            installTask.setVariantData(scope.getVariantData());
            installTask.setTimeOutInMs(scope.getGlobalScope().getExtension().getAdbOptions().getTimeOutInMs());
            installTask.setInstallOptions(scope.getGlobalScope().getExtension().getAdbOptions().getInstallOptions());
            installTask.setProcessExecutor(scope.getGlobalScope().getAndroidBuilder().getProcessExecutor());
            ConventionMappingHelper.map(installTask, "adbExe", new Closure<File>(this, this) {
                public File doCall(Object it) {
                    final SdkInfo info = scope.getGlobalScope().getSdkHandler().getSdkInfo();
                    return (info == null ? null : info.getAdb());
                }

                public File doCall() {
                    return doCall(null);
                }

            });
            ConventionMappingHelper.map(installTask, "splitSelectExe", new Callable<File>() {
                @Override
                public File call() throws Exception {
                    final TargetInfo info = scope.getGlobalScope().getAndroidBuilder().getTargetInfo();
                    String path = info == null ? null : info.getBuildTools().getPath(SPLIT_SELECT);
                    if (path != null) {
                        File splitSelectExe = new File(path);
                        return splitSelectExe.exists() ? splitSelectExe : null;
                    } else {
                        return null;
                    }
                }
            });
            ((ApkVariantData) scope.getVariantData()).installTask = installTask;

        }
    }
}
