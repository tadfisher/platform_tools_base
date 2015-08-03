/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.build.gradle.internal.transforms;

import static com.android.builder.model.AndroidProject.FD_OUTPUTS;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.internal.core.GradleVariantConfiguration;
import com.android.build.gradle.internal.pipeline.InputStream;
import com.android.build.gradle.internal.pipeline.OutputStream;
import com.android.build.gradle.internal.pipeline.StreamScope;
import com.android.build.gradle.internal.pipeline.StreamType;
import com.android.build.gradle.internal.pipeline.Transform;
import com.android.build.gradle.internal.pipeline.TransformException;
import com.android.build.gradle.internal.pipeline.TransformType;
import com.android.build.gradle.internal.scope.GlobalScope;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.build.gradle.internal.variant.BaseVariantOutputData;
import com.android.build.gradle.internal.variant.LibraryVariantData;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.gradle.tooling.BuildException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import proguard.ClassPath;
import proguard.ClassPathEntry;
import proguard.ClassSpecification;
import proguard.Configuration;
import proguard.ConfigurationParser;
import proguard.KeepClassSpecification;
import proguard.ParseException;
import proguard.ProGuard;
import proguard.classfile.util.ClassUtil;
import proguard.util.ListUtil;

/**
 * ProGuard support as a transform
 */
public class ProGuardTransform implements Transform {

    private final VariantScope variantScope;
    private final boolean isLibrary;
    private final boolean isTest;
    private final File mappingFile;
    private final File testedAppMappingFile;

    private final Configuration configuration = new Configuration();
    private final List<Object> configurationFiles = Lists.newArrayListWithExpectedSize(3);

    public ProGuardTransform(
            @NonNull VariantScope variantScope,
            @NonNull File mappingFile,
            @Nullable File testedAppMappingFile) {
        this.variantScope = variantScope;
        isLibrary = variantScope.getVariantData() instanceof LibraryVariantData;
        isTest = variantScope.getTestedVariantData() != null;
        this.mappingFile = mappingFile;
        this.testedAppMappingFile = testedAppMappingFile;

        configuration.useMixedCaseClassNames = false;
    }

    @NonNull
    public ProGuardTransform keep(@NonNull String keep) throws ParseException {
        if (configuration.keep == null) {
            configuration.keep = Lists.newArrayList();
        }

        ClassSpecification classSpecification;
        try {
            ConfigurationParser parser = new ConfigurationParser(new String[] { keep }, null);

            try {
                classSpecification = parser.parseClassSpecificationArguments();
            } finally {
                parser.close();
            }
        } catch (IOException e) {
            throw new ParseException(e.getMessage());
        }

        //noinspection unchecked
        configuration.keep.add(new KeepClassSpecification(
                true  /*markClasses*/,
                false /*markConditionally*/,
                false /*includedescriptorclasses */,
                false /*allowshrinking*/,
                false /*allowoptimization*/,
                false /*allowobfuscation*/,
                classSpecification));
        return this;
    }

    @NonNull
    public ProGuardTransform dontshrink() {
        configuration.shrink = false;
        return this;
    }

    @NonNull
    public ProGuardTransform dontoptimize() {
        configuration.optimize = false;
        return this;
    }

    @NonNull
    public ProGuardTransform keepattributes() {
        configuration.keepAttributes = Lists.newArrayListWithExpectedSize(0);
        return this;
    }

    @NonNull
    public ProGuardTransform configurationFiles(Object configFiles) {
        configurationFiles.add(configFiles);
        return this;
    }

    @NonNull
    public ProGuardTransform dontwarn(@NonNull String dontwarn) {
        if (configuration.warn == null) {
            configuration.warn = Lists.newArrayList();
        }

        dontwarn = ClassUtil.internalClassName(dontwarn);

        configuration.warn = ListUtil.commaSeparatedList(dontwarn);
        return this;
    }

    @NonNull
    @Override
    public String getName() {
        return "proguard";
    }

    @NonNull
    @Override
    public Set<StreamType> getInputTypes() {
        return Sets.immutableEnumSet(StreamType.CLASSES, StreamType.RESOURCES);
    }

    @NonNull
    @Override
    public Set<StreamType> getOutputTypes() {
        return Sets.immutableEnumSet(StreamType.CLASSES, StreamType.RESOURCES);
    }

    @NonNull
    @Override
    public Set<StreamScope> getScopes() {
        if (isLibrary) {
            return Sets.immutableEnumSet(StreamScope.PROJECT);
        }

        return Sets.immutableEnumSet(
                StreamScope.PROJECT,
                StreamScope.SUB_PROJECTS,
                StreamScope.EXTERNAL_LIBRARIES);
    }

    @NonNull
    @Override
    public Set<StreamScope> getReferencedScope() {
        if (isLibrary) {
            if (isTest) {
                return Sets.immutableEnumSet(
                        StreamScope.SUB_PROJECTS,
                        StreamScope.EXTERNAL_LIBRARIES,
                        StreamScope.TESTED_CODE);
            }

            return Sets.immutableEnumSet(
                    StreamScope.SUB_PROJECTS,
                    StreamScope.EXTERNAL_LIBRARIES);
        }

        if (isTest) {
            return Sets.immutableEnumSet(StreamScope.TESTED_CODE);
        }

        return ImmutableSet.copyOf(EnumSet.noneOf(StreamScope.class));
    }

    @NonNull
    @Override
    public TransformType getTransformType() {
        return TransformType.COMBINED_AS_FILE;
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileInputs() {
        List<File> files = Lists.newArrayList();

        // the config files
        if (testedAppMappingFile != null) {
            files.add(testedAppMappingFile);
        }

        // if an app, the
        if (!isLibrary) {
            files.addAll(
                    variantScope.getVariantData().getVariantConfiguration().getProvidedOnlyJars());
        }

        return files;
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileOutputs() {
        return Collections.singletonList(mappingFile);
    }

    @NonNull
    @Override
    public Map<String, Object> getParameterInputs() {
        return Collections.emptyMap();
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(@NonNull List<InputStream> inputs, @NonNull List<OutputStream> outputs,
            boolean isIncremental) throws TransformException {

        try {
            final BaseVariantData<? extends BaseVariantOutputData> variantData = variantScope
                    .getVariantData();
            final GradleVariantConfiguration variantConfig = variantData.getVariantConfiguration();
            GlobalScope globalScope = variantScope.getGlobalScope();

            configuration.programJars = new ClassPath();
            configuration.libraryJars = new ClassPath();

            // --- InJars / LibraryJars ---
            if (isLibrary) {
                handleLibraryCase(variantConfig, inputs);
            } else {
                handleAppCase(inputs);
            }

            // libraryJars: the runtime jars.
            for (String runtimeJar : globalScope.getAndroidBuilder().getBootClasspathAsStrings()) {
                ClassPathEntry classPathEntry = new ClassPathEntry(new File(runtimeJar), false /*output*/);
                configuration.libraryJars.add(classPathEntry);
            }

            // --- Out files ---

            OutputStream outputStream = Iterables.getOnlyElement(outputs);

            ClassPathEntry classPathEntry = new ClassPathEntry(outputStream.getFile(), true /*output*/);
            configuration.programJars.add(classPathEntry);

            final File proguardOut = new File(
                    String.valueOf(globalScope.getBuildDir()) + "/" + FD_OUTPUTS
                            + "/mapping/" + variantConfig.getDirName());

            configuration.dump = new File(proguardOut, "dump.txt");
            configuration.printSeeds = new File(proguardOut, "seeds.txt");
            configuration.printUsage = new File(proguardOut, "usage.txt");
            configuration.printMapping = new File(proguardOut, "mapping.txt");

            // proguard doesn't verify that the seed/mapping/usage folders exist and will fail
            // if they don't so create them.
            proguardOut.mkdirs();

            applyConfigFiles();

            configuration.lastModified = Long.MAX_VALUE;

            new ProGuard(configuration).execute();
        } catch (Exception e) {
            throw new TransformException(e);
        }
    }

    private void applyConfigFiles() throws Exception {
        for (Object configObject : configurationFiles) {
            handleConfigObject(configObject);
        }
    }

    private void handleConfigObject(@NonNull Object configObject)
            throws Exception {
        if (configObject instanceof File) {
            processConfigFile((File) configObject);
        } else if (configObject instanceof List) {
            List list = (List) configObject;
            for (Object child : list) {
                handleConfigObject(child);
            }
        } else if (configObject instanceof Callable) {
            handleConfigObject(((Callable) configObject).call());
        } else {
            throw new RuntimeException("Unsupported config object: " + configObject);
        }
    }

    private void processConfigFile(@NonNull File file) throws IOException, ParseException {
        ConfigurationParser parser =
                new ConfigurationParser(file, System.getProperties());
        try {
            parser.parse(configuration);
        } finally {
            parser.close();
        }
    }

    private void handleAppCase(@NonNull List<InputStream> inputs) {
        List<String> jarFilter = Lists.newArrayList("!META-INF/MANIFEST.MF");

        for (InputStream inputStream : inputs) {
            for (File file : inputStream.getFiles()) {
                if (inputStream.isReferencedOnly()) {
                    ClassPathEntry classPathEntry = new ClassPathEntry(file, false /*output*/);
                    classPathEntry.setFilter(jarFilter);
                    configuration.libraryJars.add(classPathEntry);
                } else {
                    ClassPathEntry classPathEntry = new ClassPathEntry(file, false /*output*/);
                    configuration.programJars.add(classPathEntry);
                }
            }
        }

        for (File file : variantScope.getVariantData().getVariantConfiguration().getProvidedOnlyJars()) {
            ClassPathEntry classPathEntry = new ClassPathEntry(file, false /*output*/);
            classPathEntry.setFilter(jarFilter);
            configuration.libraryJars.add(classPathEntry);
        }
    }

    private void handleLibraryCase(
            @NonNull GradleVariantConfiguration variantConfig,
            @NonNull List<InputStream> inputs) {

        String packageName = variantConfig.getPackageFromManifest();
        if (packageName == null) {
            throw new BuildException("Failed to read manifest", null);
        }

        packageName = packageName.replace(".", "/");

        // For inJars, we exclude a bunch of classes, but make the reverse
        // filter for libraryJars so that the classes aren't missing.
        List<String> excludeList = Lists.newArrayListWithExpectedSize(5);
        List<String> includeList = Lists.newArrayListWithExpectedSize(5);
        excludeList.add("!" + packageName + "/R.class");
        includeList.add(      packageName + "/R.class");
        excludeList.add("!" + packageName + "/R$*.class");
        includeList.add(      packageName + "/R$*.class");
        excludeList.add("!META-INF/MANIFEST.MF");
        if (!variantScope.getGlobalScope().getExtension().getPackageBuildConfig()) {
            excludeList.add("!" + packageName + "/Manifest.class");
            includeList.add(      packageName + "/Manifest.class");
            excludeList.add("!" + packageName + "/Manifest$*.class");
            includeList.add(      packageName + "/Manifest$*.class");
            excludeList.add("!" + packageName + "/BuildConfig.class");
            includeList.add(      packageName + "/BuildConfig.class");
        }

        List<String> jarFilter = Lists.newArrayList("!META-INF/MANIFEST.MF");

        for (InputStream inputStream : inputs) {
            Set<StreamScope> scopes = inputStream.getScopes();

            // in this case scopes should only be one, unless we have a stream with both
            // SUB_PROJECT and EXTERNAL_LIBRARIES
            // TODO: Support sub_project + External libraries case.
            if (scopes.size() > 1) {
                throw new RuntimeException("Library cannot handle multi-scope streams");
            }

            StreamScope scope = Iterables.getOnlyElement(scopes);

            switch (scope) {
                // this is consumed as inJars
                case PROJECT:
                    for (File file : inputStream.getFiles()) {
                        ClassPathEntry classPathEntry = new ClassPathEntry(file, false /*output*/);
                        classPathEntry.setFilter(excludeList);
                        configuration.programJars.add(classPathEntry);

                        ClassPathEntry classPathEntry2 = new ClassPathEntry(file, false /*output*/);
                        classPathEntry2.setFilter(includeList);
                        configuration.libraryJars.add(classPathEntry2);
                    }
                    break;
                // there are referenced only as libraryJars
                case SUB_PROJECTS:
                case EXTERNAL_LIBRARIES:
                    for (File file : inputStream.getFiles()) {
                        ClassPathEntry classPathEntry = new ClassPathEntry(file, false /*output*/);
                        classPathEntry.setFilter(jarFilter);
                        configuration.libraryJars.add(classPathEntry);
                    }
                    break;
            }
        }

        // ensure local jars keep their package names
        if (configuration.keepPackageNames == null) {
            configuration.keepPackageNames = Lists.newArrayListWithExpectedSize(0);
        }
    }
}
