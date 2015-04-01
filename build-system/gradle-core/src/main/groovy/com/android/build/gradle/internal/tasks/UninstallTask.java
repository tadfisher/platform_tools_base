package com.android.build.gradle.internal.tasks;

import com.android.build.gradle.internal.TaskManager;
import com.android.build.gradle.internal.scope.ConventionMappingHelper;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.variant.ApkVariantData;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.builder.sdk.SdkInfo;

import org.gradle.api.Action;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;

import java.io.File;
import java.util.concurrent.Callable;

import groovy.lang.Closure;

public class UninstallTask extends BaseTask {

    private File adbExe;

    private BaseVariantData variant;

    public BaseVariantData getVariant() {
        return variant;
    }

    @InputFile
    public File getAdbExe() {
        return adbExe;
    }

    public void setAdbExe(File adbExe) {
        this.adbExe = adbExe;
    }

    public void setVariant(BaseVariantData variant) {
        this.variant = variant;
    }

    @TaskAction
    public void uninstall() {
        final String applicationId = variant.getApplicationId();
        getLogger().info("Uninstalling app: " + applicationId);
        getProject().exec(new Action<ExecSpec>() {
            @Override
            public void execute(ExecSpec execSpec) {
                execSpec.executable(getAdbExe());
                execSpec.args("uninstall");
                execSpec.args(applicationId);
            }
        });
    }

    public static class ConfigAction implements Action<UninstallTask> {

        private final VariantScope scope;

        public ConfigAction(VariantScope scope) {
            this.scope = scope;
        }

        @Override
        public void execute(UninstallTask uninstallTask) {
            uninstallTask.setDescription(
                    "Uninstalls the " + scope.getVariantData().getDescription() + ".");
            uninstallTask.setGroup(TaskManager.INSTALL_GROUP);
            uninstallTask.setVariant(scope.getVariantData());
            ConventionMappingHelper.map(uninstallTask, "adbExe", new Callable<File>() {
                @Override
                public File call() throws Exception {
                    final SdkInfo info = scope.getGlobalScope().getSdkHandler().getSdkInfo();
                    return (info == null ? null : info.getAdb());
                }
            });

            ((ApkVariantData) scope.getVariantData()).uninstallTask = uninstallTask;
        }
    }
}
