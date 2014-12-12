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

package com.android.build.gradle.integration.application
import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.build.gradle.integration.common.utils.ModelHelper
import com.android.builder.model.AndroidArtifact
import com.android.builder.model.AndroidProject
import com.android.builder.model.Dependencies
import com.android.builder.model.JavaLibrary
import com.android.builder.model.Variant
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

/**
 * Assemble tests for multiproject.
 */
class MultiProjectTest {
    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromSample("multiproject")
            .create()
    static Map<String, AndroidProject> models

    @BeforeClass
    static void setUp() {
        models = project.getAllModels();
    }

    @AfterClass
    static void cleanUp() {
        project = null
        models = null
    }

    @Test
    void testModel() {

        AndroidProject baseLibModel = models.get(":baseLibrary");
        Assert.assertNotNull("Module app null-check", baseLibModel);

        Collection<Variant> variants = baseLibModel.getVariants();
        Assert.assertEquals("Variant count", 2, variants.size());

        Variant variant = ModelHelper.getVariant(variants, "release");
        Assert.assertNotNull("release variant null-check", variant);

        AndroidArtifact mainInfo = variant.getMainArtifact();
        Assert.assertNotNull("Main Artifact null-check", mainInfo);

        Dependencies dependencies = mainInfo.getDependencies();
        Assert.assertNotNull("Dependencies null-check", dependencies);

        Collection<String> projects = dependencies.getProjects();
        Assert.assertNotNull("project dep list null-check", projects);
        Assert.assertEquals("project dep count", 1, projects.size());
        Assert.assertEquals("dep on :util check", ":util", projects.iterator().next());

        Collection<JavaLibrary> javaLibraries = dependencies.getJavaLibraries();
        Assert.assertNotNull("jar dep list null-check", javaLibraries);
        // TODO these are jars coming from ':util' They shouldn't be there.
        Assert.assertEquals("jar dep count", 2, javaLibraries.size());
    }
}
