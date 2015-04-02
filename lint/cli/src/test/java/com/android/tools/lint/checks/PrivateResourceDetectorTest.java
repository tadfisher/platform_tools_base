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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.FN_PUBLIC_TXT;
import static com.android.SdkConstants.FN_RESOURCE_TEXT;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.AndroidArtifact;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.Dependencies;
import com.android.builder.model.Variant;
import com.android.testutils.TestUtils;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Project;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.easymock.IExpectationSetters;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("javadoc")
public class PrivateResourceDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new PrivateResourceDetector();
    }

    public void testPrivateInXml() throws Exception {
        assertEquals(""
                + "res/layout/private.xml:11: Warning: The resource @string/my_private_string is marked as private in the library [PrivateResource]\n"
                + "            android:text=\"@string/my_private_string\" />\n"
                + "                          ~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
                lintProject("res/layout/private.xml"));
    }

    public void testPrivateInJava() throws Exception {
        assertEquals(""
                + ""
                + "src/test/pkg/Private.java:3: Warning: The resource @string/my_private_string is marked as private in the library [PrivateResource]\n"
                + "        int x = R.string.my_private_string; // ERROR\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",
                lintProject("src/test/pkg/Private.java.txt=>src/test/pkg/Private.java"));
    }

    @Override
    protected TestLintClient createClient() {
        return new TestLintClient() {
            @NonNull
            @Override
            protected Project createProject(@NonNull File dir, @NonNull File referenceDir) {
                return new Project(this, dir, referenceDir) {
                    @Override
                    public boolean isGradleProject() {
                        return true;
                    }

                    @Nullable
                    @Override
                    public AndroidProject getGradleProjectModel() {
                        return createMockProject("1.3.0-alpha1", 3);
                    }

                    @Nullable
                    @Override
                    public Variant getCurrentVariant() {
                        try {
                            AndroidLibrary library = createMockLibrary(
                                    ""
                                            + "int string my_private_string 0x7f040000\n"
                                            + "int string my_public_string 0x7f040001\n",
                                    ""
                                            + ""
                                            + "string my_public_string\n",
                                    Collections.<AndroidLibrary>emptyList()
                            );
                            AndroidArtifact artifact = createMockArtifact(
                                    Collections.singletonList(library));

                            Variant variant = createNiceMock(Variant.class);
                            expect(variant.getMainArtifact()).andReturn(artifact).anyTimes();
                            replay(variant);
                            return variant;
                        } catch (Exception e) {
                            fail(e.toString());
                            return null;
                        }
                    }
                };
            }
        };
    }

    public static AndroidProject createMockProject(String modelVersion, int apiVersion) {
        AndroidProject project = createNiceMock(AndroidProject.class);
        expect(project.getApiVersion()).andReturn(apiVersion).anyTimes();
        expect(project.getModelVersion()).andReturn(modelVersion).anyTimes();
        replay(project);

        return project;
    }

    public static AndroidArtifact createMockArtifact(List<AndroidLibrary> libraries) {
        Dependencies dependencies = createNiceMock(Dependencies.class);
        expect(dependencies.getLibraries()).andReturn(libraries).anyTimes();
        replay(dependencies);

        AndroidArtifact artifact = createNiceMock(AndroidArtifact.class);
        expect(artifact.getDependencies()).andReturn(dependencies).anyTimes();
        replay(artifact);

        return artifact;
    }

    public static AndroidLibrary createMockLibrary(String allResources, String publicResources,
            List<AndroidLibrary> dependencies)
            throws IOException {
        final File tempDir = TestUtils.createTempDirDeletedOnExit();

        Files.write(allResources, new File(tempDir, FN_RESOURCE_TEXT), Charsets.UTF_8);
        File publicTxtFile = new File(tempDir, FN_PUBLIC_TXT);
        if (publicResources != null) {
            Files.write(publicResources, publicTxtFile, Charsets.UTF_8);
        }
        AndroidLibrary library = createNiceMock(AndroidLibrary.class);
        expect(library.getPublicResources()).andReturn(publicTxtFile).anyTimes();

        // Work around wildcard capture
        //expect(mock.getLibraryDependencies()).andReturn(dependencies).anyTimes();
        IExpectationSetters setter = expect(library.getLibraryDependencies());
        //noinspection unchecked
        setter.andReturn(dependencies);
        setter.anyTimes();

        replay(library);
        return library;
    }
}
