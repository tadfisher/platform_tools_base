package com.android.build.gradle.internal;

import com.android.build.gradle.ndk.NdkPlugin;
import com.android.builder.core.VariantConfiguration;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.runtime.base.Binary;

import java.io.File;
import java.util.Collection;

/**
 * A dependency on a NDK library within the build.
 */
public class NdkLibrarySpecification {
    private Project currentProject;

    private String targetProjectName;

    private NdkPlugin getNdkPlugin() {
        Project targetProject = targetProjectName.isEmpty()
                    ? currentProject
                    : currentProject.getRootProject().project(targetProjectName);
        NdkPlugin plugin = targetProject.getPlugins().findPlugin(NdkPlugin.class);
        if (plugin == null) {
            throw new InvalidUserDataException(
                    "Unable to find NdkPlugin in project '" + targetProjectName + "'.  You may " +
                    "need define your compile dependencies such that " + targetProject + " is " +
                    "configured before " + currentProject + ".");
        }
        return plugin;
    }

    public NdkLibrarySpecification(Project currentProject, String targetProjectName) {
        this.currentProject = currentProject;
        this.targetProjectName = targetProjectName;
    }

    public Collection<? extends Binary> getBinaries(VariantConfiguration variantConfig) {
        return getNdkPlugin().getBinaries(variantConfig);
    }

    public Collection<File> getOutputDirectories(VariantConfiguration variantConfig) {
        return getNdkPlugin().getOutputDirectories(variantConfig);
    }
}
