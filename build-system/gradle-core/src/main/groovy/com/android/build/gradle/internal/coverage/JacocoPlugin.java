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

package com.android.build.gradle.internal.coverage;
import com.android.build.gradle.AndroidConfig;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvableDependencies;

/**
 * Jacoco plugin. This is very similar to the built-in support for Jacoco but we dup it in order
 * to control it as we need our own offline instrumentation.
 *
 * This may disappear if we can ever reuse the built-in support.
 *
 */
public class JacocoPlugin implements Plugin<Project> {
    public static final String ANT_CONFIGURATION_NAME = "androidJacocoAnt";
    public static final String AGENT_CONFIGURATION_NAME = "androidJacocoAgent";

    private Project project;

    @Override
    public void apply(Project project) {
        this.project = project;

        addJacocoConfigurations();
        configureAgentDependencies();
        configureTaskClasspathDefaults();
    }

    /**
     * Creates the configurations used by plugin.
     */
    private void addJacocoConfigurations() {
        this.project.getConfigurations().create(
                AGENT_CONFIGURATION_NAME,
                new Action<Configuration>() {
                    @Override
                    public void execute(Configuration files) {
                        files.setVisible(false);
                        files.setTransitive(true);
                        files.setDescription("The Jacoco agent to use to get coverage data.");
                    }
                });

        this.project.getConfigurations().create(
                ANT_CONFIGURATION_NAME,
                new Action<Configuration>() {
                    @Override
                    public void execute(Configuration files) {
                        files.setVisible(false);
                        files.setTransitive(true);
                        files.setDescription("The Jacoco ant tasks to use to get execute Gradle tasks.");
                    }
                });
    }

    /**
     * Configures the agent dependencies using the 'jacocoAnt' configuration.
     * Uses the version declared in 'toolVersion' of the Jacoco extension if no dependencies are explicitly declared.
     */
    private void configureAgentDependencies() {
        final Configuration config =
                project.getConfigurations().getByName(AGENT_CONFIGURATION_NAME);
        config.getIncoming().beforeResolve(new Action<ResolvableDependencies>() {
            @Override
            public void execute(ResolvableDependencies resolvableDependencies) {
                if (config.getDependencies().isEmpty()) {
                    config.getDependencies().add(project.getDependencies().create(
                            "org.jacoco:org.jacoco.agent:" +
                                    ((AndroidConfig) project.getExtensions().getByName("android"))
                                                    .getJacoco().getVersion()));
                }
            }
        });
    }

    /**
     * Configures the classpath for Jacoco tasks using the 'jacocoAnt' configuration.
     * Uses the version information declared in 'toolVersion' of the Jacoco extension if no dependencies are explicitly declared.
     */
    private void configureTaskClasspathDefaults() {
        final Configuration config = project.getConfigurations().getByName(ANT_CONFIGURATION_NAME);
        config.getIncoming().beforeResolve(new Action<ResolvableDependencies>() {
            @Override
            public void execute(ResolvableDependencies resolvableDependencies) {
                if (config.getDependencies().isEmpty()) {
                    config.getDependencies().add(project.getDependencies().create(
                            "org.jacoco:org.jacoco.ant:" +
                                    ((AndroidConfig) project.getExtensions().getByName("android"))
                                            .getJacoco().getVersion()));
                }
            }
        });
    }
}
