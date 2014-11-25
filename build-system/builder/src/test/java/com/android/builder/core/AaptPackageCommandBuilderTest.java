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

package com.android.builder.core;

import com.android.annotations.NonNull;
import com.android.builder.dependency.SymbolFileProvider;
import com.android.builder.model.AaptOptions;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkManager;
import com.android.sdklib.repository.FullRevision;
import com.android.utils.ILogger;
import com.android.utils.StdLogger;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.List;

/**
 * Tests for {@link com.android.builder.core.AaptPackageCommandBuilder} class
 */
public class AaptPackageCommandBuilderTest extends TestCase {

    @Mock
    AaptOptions mAaptOptions;

    BuildToolInfo mBuildToolInfo;
    IAndroidTarget mIAndroidTarget;

    ILogger mLogger = new StdLogger(StdLogger.Level.VERBOSE);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        SdkManager sdkManager = SdkManager.createManager(getSdkDir().getAbsolutePath(), mLogger);
        assert sdkManager != null;
        mBuildToolInfo = sdkManager.getBuildTool(FullRevision.parseRevision("21"));
        if (mBuildToolInfo == null) {
            throw new RuntimeException("Test requires build-tools 21");
        }
        for (IAndroidTarget iAndroidTarget : sdkManager.getTargets()) {
            if (iAndroidTarget.getVersion().getApiLevel() == 21) {
                mIAndroidTarget = iAndroidTarget;
            }
        }
        if (mIAndroidTarget == null) {
            throw new RuntimeException("Test requires android-21");
        }
    }

    public void testAndroidManifestPackaging() {
        File virtualAndroidManifestFile = new File("/path/to/non/existent/file");

        AaptPackageCommandBuilder aaptPackageCommandBuilder =
                new AaptPackageCommandBuilder(virtualAndroidManifestFile, mAaptOptions);
        aaptPackageCommandBuilder.setResPackageOutput("/path/to/non/existent/dir");

        List<String> command = aaptPackageCommandBuilder
                .build(mBuildToolInfo, mIAndroidTarget, mLogger);

        assertTrue("/path/to/non/existent/file".equals(command.get(command.indexOf("-M") + 1)));
        assertTrue("/path/to/non/existent/dir".equals(command.get(command.indexOf("-F") + 1)));
        assertTrue(command.get(command.indexOf("-I") + 1).contains("android.jar"));
    }

    public void testResourcesPackaging() {
        File virtualAndroidManifestFile = new File("/path/to/non/existent/file");
        File assetsFolder = Mockito.mock(File.class);
        Mockito.when(assetsFolder.isDirectory()).thenReturn(true);
        Mockito.when(assetsFolder.getAbsolutePath()).thenReturn("/path/to/assets/folder");
        File resFolder = Mockito.mock(File.class);
        Mockito.when(resFolder.isDirectory()).thenReturn(true);
        Mockito.when(resFolder.getAbsolutePath()).thenReturn("/path/to/res/folder");

        AaptPackageCommandBuilder aaptPackageCommandBuilder =
                new AaptPackageCommandBuilder(virtualAndroidManifestFile, mAaptOptions);
        aaptPackageCommandBuilder.setResPackageOutput("/path/to/non/existent/dir")
                .setAssetsFolder(assetsFolder)
                .setResFolder(resFolder)
                .setPackageForR("com.example.package.forR")
                .setSourceOutputDir("path/to/source/output/dir")
                .setLibraries(ImmutableList.of(Mockito.mock(SymbolFileProvider.class)))
                .setType(VariantConfiguration.Type.DEFAULT);

        List<String> command = aaptPackageCommandBuilder
                .build(mBuildToolInfo, mIAndroidTarget, mLogger);

        assertTrue("/path/to/non/existent/file".equals(command.get(command.indexOf("-M") + 1)));
        assertTrue("/path/to/non/existent/dir".equals(command.get(command.indexOf("-F") + 1)));
        assertTrue(command.get(command.indexOf("-I") + 1).contains("android.jar"));
        assertTrue("/path/to/res/folder".equals(command.get(command.indexOf("-S") + 1)));
        assertTrue("/path/to/assets/folder".equals(command.get(command.indexOf("-A") + 1)));
        assertTrue("path/to/source/output/dir".equals(command.get(command.indexOf("-J") + 1)));
        assertTrue("com.example.package.forR".equals(command.get(command.indexOf("--custom-package") + 1)));

        assertTrue(command.indexOf("-f") != -1);
        assertTrue(command.indexOf("--no-crunch") != -1);
        assertTrue(command.indexOf("-0") != -1);
        assertTrue(command.indexOf("apk") != -1);
    }

    public void testResourcesPackagingForTest() {
        File virtualAndroidManifestFile = new File("/path/to/non/existent/file");
        File assetsFolder = Mockito.mock(File.class);
        Mockito.when(assetsFolder.isDirectory()).thenReturn(true);
        Mockito.when(assetsFolder.getAbsolutePath()).thenReturn("/path/to/assets/folder");
        File resFolder = Mockito.mock(File.class);
        Mockito.when(resFolder.isDirectory()).thenReturn(true);
        Mockito.when(resFolder.getAbsolutePath()).thenReturn("/path/to/res/folder");

        AaptPackageCommandBuilder aaptPackageCommandBuilder =
                new AaptPackageCommandBuilder(virtualAndroidManifestFile, mAaptOptions);
        aaptPackageCommandBuilder.setResPackageOutput("/path/to/non/existent/dir")
                .setAssetsFolder(assetsFolder)
                .setResFolder(resFolder)
                .setPackageForR("com.example.package.forR")
                .setSourceOutputDir("path/to/source/output/dir")
                .setLibraries(ImmutableList.of(Mockito.mock(SymbolFileProvider.class)))
                .setType(VariantConfiguration.Type.TEST);

        List<String> command = aaptPackageCommandBuilder
                .build(mBuildToolInfo, mIAndroidTarget, mLogger);

        assertTrue("/path/to/non/existent/file".equals(command.get(command.indexOf("-M") + 1)));
        assertTrue("/path/to/non/existent/dir".equals(command.get(command.indexOf("-F") + 1)));
        assertTrue(command.get(command.indexOf("-I") + 1).contains("android.jar"));
        assertTrue("/path/to/res/folder".equals(command.get(command.indexOf("-S") + 1)));
        assertTrue("/path/to/assets/folder".equals(command.get(command.indexOf("-A") + 1)));
        assertTrue("path/to/source/output/dir".equals(command.get(command.indexOf("-J") + 1)));
        assertTrue(command.indexOf("--custom-package") == -1);

        assertTrue(command.indexOf("-f") != -1);
        assertTrue(command.indexOf("--no-crunch") != -1);
        assertTrue(command.indexOf("-0") != -1);
        assertTrue(command.indexOf("apk") != -1);
    }

    public void testResourcesPackagingForLibrary() {
        File virtualAndroidManifestFile = new File("/path/to/non/existent/file");
        File assetsFolder = Mockito.mock(File.class);
        Mockito.when(assetsFolder.isDirectory()).thenReturn(true);
        Mockito.when(assetsFolder.getAbsolutePath()).thenReturn("/path/to/assets/folder");
        File resFolder = Mockito.mock(File.class);
        Mockito.when(resFolder.isDirectory()).thenReturn(true);
        Mockito.when(resFolder.getAbsolutePath()).thenReturn("/path/to/res/folder");

        AaptPackageCommandBuilder aaptPackageCommandBuilder =
                new AaptPackageCommandBuilder(virtualAndroidManifestFile, mAaptOptions);
        aaptPackageCommandBuilder.setResPackageOutput("/path/to/non/existent/dir")
                .setAssetsFolder(assetsFolder)
                .setResFolder(resFolder)
                .setPackageForR("com.example.package.forR")
                .setSourceOutputDir("path/to/source/output/dir")
                .setLibraries(ImmutableList.of(Mockito.mock(SymbolFileProvider.class)))
                .setType(VariantConfiguration.Type.LIBRARY);

        List<String> command = aaptPackageCommandBuilder
                .build(mBuildToolInfo, mIAndroidTarget, mLogger);

        assertTrue("/path/to/non/existent/file".equals(command.get(command.indexOf("-M") + 1)));
        assertTrue("/path/to/non/existent/dir".equals(command.get(command.indexOf("-F") + 1)));
        assertTrue(command.get(command.indexOf("-I") + 1).contains("android.jar"));
        assertTrue("/path/to/res/folder".equals(command.get(command.indexOf("-S") + 1)));
        assertTrue("/path/to/assets/folder".equals(command.get(command.indexOf("-A") + 1)));
        assertTrue("path/to/source/output/dir".equals(command.get(command.indexOf("-J") + 1)));

        assertTrue(command.indexOf("--non-constant-id") != -1);

        assertTrue(command.indexOf("-f") != -1);
        assertTrue(command.indexOf("--no-crunch") != -1);
        assertTrue(command.indexOf("-0") != -1);
        assertTrue(command.indexOf("apk") != -1);
    }


    public void testSplitResourcesPackaging() {
        File virtualAndroidManifestFile = new File("/path/to/non/existent/file");
        File assetsFolder = Mockito.mock(File.class);
        Mockito.when(assetsFolder.isDirectory()).thenReturn(true);
        Mockito.when(assetsFolder.getAbsolutePath()).thenReturn("/path/to/assets/folder");
        File resFolder = Mockito.mock(File.class);
        Mockito.when(resFolder.isDirectory()).thenReturn(true);
        Mockito.when(resFolder.getAbsolutePath()).thenReturn("/path/to/res/folder");

        AaptPackageCommandBuilder aaptPackageCommandBuilder =
                new AaptPackageCommandBuilder(virtualAndroidManifestFile, mAaptOptions);
        aaptPackageCommandBuilder.setResPackageOutput("/path/to/non/existent/dir")
                .setAssetsFolder(assetsFolder)
                .setResFolder(resFolder)
                .setPackageForR("com.example.package.forR")
                .setSourceOutputDir("path/to/source/output/dir")
                .setLibraries(ImmutableList.of(Mockito.mock(SymbolFileProvider.class)))
                .setType(VariantConfiguration.Type.DEFAULT)
                .setSplits(ImmutableList.of("mdpi", "hdpi"));

        List<String> command = aaptPackageCommandBuilder
                .build(mBuildToolInfo, mIAndroidTarget, mLogger);

        assertTrue("/path/to/non/existent/file".equals(command.get(command.indexOf("-M") + 1)));
        assertTrue("/path/to/non/existent/dir".equals(command.get(command.indexOf("-F") + 1)));
        assertTrue(command.get(command.indexOf("-I") + 1).contains("android.jar"));
        assertTrue("/path/to/res/folder".equals(command.get(command.indexOf("-S") + 1)));
        assertTrue("/path/to/assets/folder".equals(command.get(command.indexOf("-A") + 1)));
        assertTrue("path/to/source/output/dir".equals(command.get(command.indexOf("-J") + 1)));
        assertTrue("com.example.package.forR".equals(command.get(command.indexOf("--custom-package") + 1)));

        assertTrue(command.indexOf("-f") != -1);
        assertTrue(command.indexOf("--no-crunch") != -1);
        assertTrue(command.indexOf("-0") != -1);
        assertTrue(command.indexOf("apk") != -1);

        assertTrue("--split".equals(command.get(command.indexOf("mdpi") - 1)));
        assertTrue("--split".equals(command.get(command.indexOf("hdpi") - 1)));
        assertTrue(command.indexOf("--preferred-density") == -1);
    }

    public void testPre21ResourceConfigsAndPreferredDensity() {
        File virtualAndroidManifestFile = new File("/path/to/non/existent/file");
        File assetsFolder = Mockito.mock(File.class);
        Mockito.when(assetsFolder.isDirectory()).thenReturn(true);
        Mockito.when(assetsFolder.getAbsolutePath()).thenReturn("/path/to/assets/folder");
        File resFolder = Mockito.mock(File.class);
        Mockito.when(resFolder.isDirectory()).thenReturn(true);
        Mockito.when(resFolder.getAbsolutePath()).thenReturn("/path/to/res/folder");

        AaptPackageCommandBuilder aaptPackageCommandBuilder =
                new AaptPackageCommandBuilder(virtualAndroidManifestFile, mAaptOptions);
        aaptPackageCommandBuilder.setResPackageOutput("/path/to/non/existent/dir")
                .setAssetsFolder(assetsFolder)
                .setResFolder(resFolder)
                .setPackageForR("com.example.package.forR")
                .setSourceOutputDir("path/to/source/output/dir")
                .setLibraries(ImmutableList.of(Mockito.mock(SymbolFileProvider.class)))
                .setType(VariantConfiguration.Type.DEFAULT)
                .setResourceConfigs(ImmutableList.of("res1", "res2"))
                .setPreferredDensity("xhdpi");


        SdkManager sdkManager = SdkManager.createManager(getSdkDir().getAbsolutePath(), mLogger);
        assert sdkManager != null;
        BuildToolInfo buildToolInfo = sdkManager.getBuildTool(FullRevision.parseRevision("20"));
        if (buildToolInfo == null) {
            throw new RuntimeException("Test requires build-tools 20");
        }
        IAndroidTarget androidTarget = null;
        for (IAndroidTarget iAndroidTarget : sdkManager.getTargets()) {
            if (iAndroidTarget.getVersion().getApiLevel() < 20) {
                androidTarget = iAndroidTarget;
                break;
            }
        }
        if (androidTarget == null) {
            throw new RuntimeException("Test requires pre android-21");
        }

        List<String> command = aaptPackageCommandBuilder
                .build(buildToolInfo, androidTarget, mLogger);

        assertTrue("res1,res2,xhdpi,nodpi".equals(command.get(command.indexOf("-c") + 1)));
        assertTrue(command.indexOf("--preferred-density") == -1);
    }

    public void testPost21ResourceConfigsAndPreferredDensity() {
        File virtualAndroidManifestFile = new File("/path/to/non/existent/file");
        File assetsFolder = Mockito.mock(File.class);
        Mockito.when(assetsFolder.isDirectory()).thenReturn(true);
        Mockito.when(assetsFolder.getAbsolutePath()).thenReturn("/path/to/assets/folder");
        File resFolder = Mockito.mock(File.class);
        Mockito.when(resFolder.isDirectory()).thenReturn(true);
        Mockito.when(resFolder.getAbsolutePath()).thenReturn("/path/to/res/folder");

        AaptPackageCommandBuilder aaptPackageCommandBuilder =
                new AaptPackageCommandBuilder(virtualAndroidManifestFile, mAaptOptions);
        aaptPackageCommandBuilder.setResPackageOutput("/path/to/non/existent/dir")
                .setAssetsFolder(assetsFolder)
                .setResFolder(resFolder)
                .setPackageForR("com.example.package.forR")
                .setSourceOutputDir("path/to/source/output/dir")
                .setLibraries(ImmutableList.of(Mockito.mock(SymbolFileProvider.class)))
                .setType(VariantConfiguration.Type.DEFAULT)
                .setResourceConfigs(ImmutableList.of("res1", "res2"))
                .setPreferredDensity("xhdpi");


        SdkManager sdkManager = SdkManager.createManager(getSdkDir().getAbsolutePath(), mLogger);
        assert sdkManager != null;
        BuildToolInfo buildToolInfo = sdkManager.getBuildTool(FullRevision.parseRevision("21"));
        if (buildToolInfo == null) {
            throw new RuntimeException("Test requires build-tools 21");
        }
        IAndroidTarget androidTarget = null;
        for (IAndroidTarget iAndroidTarget : sdkManager.getTargets()) {
            if (iAndroidTarget.getVersion().getApiLevel() < 21) {
                androidTarget = iAndroidTarget;
                break;
            }
        }
        if (androidTarget == null) {
            throw new RuntimeException("Test requires pre android-21");
        }

        List<String> command = aaptPackageCommandBuilder
                .build(buildToolInfo, androidTarget, mLogger);

        assertEquals("res1,res2", command.get(command.indexOf("-c") + 1));
        assertEquals("xhdpi", command.get(command.indexOf("--preferred-density") + 1));
    }

    public void testResConfigAndSplitConflict() {
        File virtualAndroidManifestFile = new File("/path/to/non/existent/file");
        File assetsFolder = Mockito.mock(File.class);
        Mockito.when(assetsFolder.isDirectory()).thenReturn(true);
        Mockito.when(assetsFolder.getAbsolutePath()).thenReturn("/path/to/assets/folder");
        File resFolder = Mockito.mock(File.class);
        Mockito.when(resFolder.isDirectory()).thenReturn(true);
        Mockito.when(resFolder.getAbsolutePath()).thenReturn("/path/to/res/folder");

        AaptPackageCommandBuilder aaptPackageCommandBuilder =
                new AaptPackageCommandBuilder(virtualAndroidManifestFile, mAaptOptions);
        aaptPackageCommandBuilder.setResPackageOutput("/path/to/non/existent/dir")
                .setAssetsFolder(assetsFolder)
                .setResFolder(resFolder)
                .setPackageForR("com.example.package.forR")
                .setSourceOutputDir("path/to/source/output/dir")
                .setLibraries(ImmutableList.of(Mockito.mock(SymbolFileProvider.class)))
                .setType(VariantConfiguration.Type.DEFAULT)
                .setResourceConfigs(ImmutableList.of("nodpi", "en", "fr", "mdpi", "hdpi", "xxhdpi", "xxxhdpi"))
                .setSplits(ImmutableList.of("xhdpi"))
                .setPreferredDensity("xhdpi");


        SdkManager sdkManager = SdkManager.createManager(getSdkDir().getAbsolutePath(), mLogger);
        assert sdkManager != null;
        BuildToolInfo buildToolInfo = sdkManager.getBuildTool(FullRevision.parseRevision("21"));
        if (buildToolInfo == null) {
            throw new RuntimeException("Test requires build-tools 21");
        }
        IAndroidTarget androidTarget = null;
        for (IAndroidTarget iAndroidTarget : sdkManager.getTargets()) {
            if (iAndroidTarget.getVersion().getApiLevel() < 21) {
                androidTarget = iAndroidTarget;
                break;
            }
        }
        if (androidTarget == null) {
            throw new RuntimeException("Test requires pre android-21");
        }

        try {
            aaptPackageCommandBuilder.build(buildToolInfo, androidTarget, mLogger);
        } catch(Exception expected) {
            assertEquals("Splits for densities \"xhdpi\" were configured, yet the resConfigs settings does not include such splits. The resulting split APKs would be empty.\n"
                    + "Suggestion : exclude those splits in your build.gradle : \n"
                    + "splits {\n"
                    + "     density {\n"
                    + "         enable true\n"
                    + "         exclude \"xhdpi\"\n"
                    + "     }\n"
                    + "}\n"
                    + "OR add them to the resConfigs list.", expected.getMessage());
        }
    }

    public void testResConfigAndSplitConflict2() {
        File virtualAndroidManifestFile = new File("/path/to/non/existent/file");
        File assetsFolder = Mockito.mock(File.class);
        Mockito.when(assetsFolder.isDirectory()).thenReturn(true);
        Mockito.when(assetsFolder.getAbsolutePath()).thenReturn("/path/to/assets/folder");
        File resFolder = Mockito.mock(File.class);
        Mockito.when(resFolder.isDirectory()).thenReturn(true);
        Mockito.when(resFolder.getAbsolutePath()).thenReturn("/path/to/res/folder");

        AaptPackageCommandBuilder aaptPackageCommandBuilder =
                new AaptPackageCommandBuilder(virtualAndroidManifestFile, mAaptOptions);
        aaptPackageCommandBuilder.setResPackageOutput("/path/to/non/existent/dir")
                .setAssetsFolder(assetsFolder)
                .setResFolder(resFolder)
                .setPackageForR("com.example.package.forR")
                .setSourceOutputDir("path/to/source/output/dir")
                .setLibraries(ImmutableList.of(Mockito.mock(SymbolFileProvider.class)))
                .setType(VariantConfiguration.Type.DEFAULT)
                .setResourceConfigs(ImmutableList.of("xxxhdpi"))
                .setSplits(ImmutableList.of("hdpi", "mdpi", "xxhdpi"))
                .setPreferredDensity("xhdpi");


        SdkManager sdkManager = SdkManager.createManager(getSdkDir().getAbsolutePath(), mLogger);
        assert sdkManager != null;
        BuildToolInfo buildToolInfo = sdkManager.getBuildTool(FullRevision.parseRevision("21"));
        if (buildToolInfo == null) {
            throw new RuntimeException("Test requires build-tools 21");
        }
        IAndroidTarget androidTarget = null;
        for (IAndroidTarget iAndroidTarget : sdkManager.getTargets()) {
            if (iAndroidTarget.getVersion().getApiLevel() < 21) {
                androidTarget = iAndroidTarget;
                break;
            }
        }
        if (androidTarget == null) {
            throw new RuntimeException("Test requires pre android-21");
        }

        try {
            aaptPackageCommandBuilder.build(buildToolInfo, androidTarget, mLogger);
        } catch(Exception expected) {
            assertEquals("Splits for densities \"hdpi,mdpi,xxhdpi\" were configured, yet the "
                    + "resConfigs settings does not include such splits. The resulting split APKs "
                    + "would be empty.\n"
                    + "Suggestion : exclude those splits in your build.gradle : \n"
                    + "splits {\n"
                    + "     density {\n"
                    + "         enable true\n"
                    + "         exclude \"hdpi\",\"mdpi\",\"xxhdpi\"\n"
                    + "     }\n"
                    + "}\n"
                    + "OR add them to the resConfigs list.", expected.getMessage());
        }
    }

    public void testResConfigAndSplitNoConflict() {
        File virtualAndroidManifestFile = new File("/path/to/non/existent/file");
        File assetsFolder = Mockito.mock(File.class);
        Mockito.when(assetsFolder.isDirectory()).thenReturn(true);
        Mockito.when(assetsFolder.getAbsolutePath()).thenReturn("/path/to/assets/folder");
        File resFolder = Mockito.mock(File.class);
        Mockito.when(resFolder.isDirectory()).thenReturn(true);
        Mockito.when(resFolder.getAbsolutePath()).thenReturn("/path/to/res/folder");

        AaptPackageCommandBuilder aaptPackageCommandBuilder =
                new AaptPackageCommandBuilder(virtualAndroidManifestFile, mAaptOptions);
        aaptPackageCommandBuilder.setResPackageOutput("/path/to/non/existent/dir")
                .setAssetsFolder(assetsFolder)
                .setResFolder(resFolder)
                .setPackageForR("com.example.package.forR")
                .setSourceOutputDir("path/to/source/output/dir")
                .setLibraries(ImmutableList.of(Mockito.mock(SymbolFileProvider.class)))
                .setType(VariantConfiguration.Type.DEFAULT)
                .setResourceConfigs(ImmutableList.of( "en", "fr", "es", "de", "it", "mdpi", "hdpi", "xhdpi", "xxhdpi"))
                .setSplits(ImmutableList.of("mdpi", "hdpi", "xhdpi", "xxhdpi"))
                .setPreferredDensity("xhdpi");


        SdkManager sdkManager = SdkManager.createManager(getSdkDir().getAbsolutePath(), mLogger);
        assert sdkManager != null;
        BuildToolInfo buildToolInfo = sdkManager.getBuildTool(FullRevision.parseRevision("21"));
        if (buildToolInfo == null) {
            throw new RuntimeException("Test requires build-tools 21");
        }
        IAndroidTarget androidTarget = null;
        for (IAndroidTarget iAndroidTarget : sdkManager.getTargets()) {
            if (iAndroidTarget.getVersion().getApiLevel() < 21) {
                androidTarget = iAndroidTarget;
                break;
            }
        }
        if (androidTarget == null) {
            throw new RuntimeException("Test requires pre android-21");
        }

        List<String> command = aaptPackageCommandBuilder.build(buildToolInfo, androidTarget, mLogger);
        assertEquals("en,fr,es,de,it,mdpi,hdpi,xhdpi,xxhdpi",
                command.get(command.indexOf("-c") + 1));
        assertTrue("--split".equals(command.get(command.indexOf("mdpi") - 1)));
        assertTrue("--split".equals(command.get(command.indexOf("hdpi") - 1)));
        assertTrue("--split".equals(command.get(command.indexOf("xhdpi") - 1)));
        assertTrue("--split".equals(command.get(command.indexOf("xxhdpi") - 1)));
        assertEquals(-1, command.indexOf("xxxhdpi"));
    }

    /**
     * Returns the SDK folder as built from the Android source tree.
     * @return the SDK
     */
    @NonNull
    protected File getSdkDir() {
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome != null) {
            File f = new File(androidHome);
            if (f.isDirectory()) {
                return f;
            }
        }

        throw new IllegalStateException("SDK not defined with ANDROID_HOME");
    }
}
