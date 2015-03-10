package com.android.build.gradle.ndk;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.managed.ManagedPattern;
import com.android.build.gradle.managed.ManagedString;
import com.google.common.collect.Sets;

import org.gradle.api.Action;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.model.Managed;
import org.gradle.model.collection.ManagedSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Configuration model for android-ndk plugin.
 */
@Managed
public interface ManagedNdkConfig {

    String getModuleName();
    void setModuleName(@NonNull String moduleName);

    String getCompileSdkVersion();
    void setCompileSdkVersion(@NonNull String target);

    String getToolchain();
    void setToolchain(@NonNull String toolchain);

    /**
     * The toolchain version.
     */
    String getToolchainVersion();
    void setToolchainVersion(@NonNull String toolchainVersion);

    ManagedSet<ManagedString> getAbiFilters();

    String getCFlags();
    void setCFlags(@NonNull String cFlags);

    String getCppFlags();
    void setCppFlags(@NonNull String cppFlags);

    ManagedSet<ManagedString> getLdLibs();

    String getStl();
    void setStl(@NonNull String stl);

    Boolean getRenderscriptNdkMode();
    void setRenderscriptNdkMode(Boolean renderscriptNdkMode);

    @NonNull
    ManagedPattern getCFilePattern();

    @NonNull
    ManagedPattern getCppFilePattern();
}
