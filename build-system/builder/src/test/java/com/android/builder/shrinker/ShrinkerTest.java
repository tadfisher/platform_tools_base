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

package com.android.builder.shrinker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.android.annotations.NonNull;
import com.android.ide.common.internal.WaitableExecutor;
import com.android.ide.common.res2.FileStatus;
import com.android.utils.FileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link Shrinker}.
 */
public class ShrinkerTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private File mTestPackageDir;

    private File mOutDir;

    private List<ClassStream> mClassStreams;

    private Shrinker<String> mShrinker;

    @Before
    public void setUp() throws Exception {
        mTestPackageDir = tmpDir.newFolder("app-classes", "test");
        File classDir = new File(tmpDir.getRoot(), "app-classes");
        mOutDir = tmpDir.newFolder("out");
        File incrementalDir = tmpDir.newFolder("incremental");
        mClassStreams = ImmutableList.<ClassStream>of(new TestClassStream(classDir, mOutDir));
        mShrinker = new Shrinker<String>(
                new WaitableExecutor<Void>(),
                new JavaSerializationShrinkerGraph(incrementalDir));
    }

    @Test
    public void oneClass() throws Exception {
        // Given:
        Files.write(TestClasses.aaa(), new File(mTestPackageDir, "Aaa.class"));

        // When:
        mShrinker.run(
                mClassStreams,
                ImmutableMap.<Shrinker.ShrinkType, Shrinker.KeepRules>of(
                        Shrinker.ShrinkType.SHRINK, new SimpleKeepRules("Aaa", "aaa")));

        // Then:
        assertMethodsLeft("Aaa", "aaa:()V", "bbb:()V");
    }

    @Test
    public void threeClasses() throws Exception {
        // Given:
        Files.write(TestClasses.aaa(), new File(mTestPackageDir, "Aaa.class"));
        Files.write(TestClasses.bbb(), new File(mTestPackageDir, "Bbb.class"));
        Files.write(TestClasses.ccc(), new File(mTestPackageDir, "Ccc.class"));

        // When:
        mShrinker.run(
                mClassStreams,
                ImmutableMap.<Shrinker.ShrinkType, Shrinker.KeepRules>of(
                        Shrinker.ShrinkType.SHRINK, new SimpleKeepRules("Bbb", "bbb")));

        // Then:
        assertMethodsLeft("Aaa", "aaa:()V", "bbb:()V");
        assertMethodsLeft("Bbb", "bbb:(Ltest/Aaa;)V");
        assertClassSkipped("Ccc");
    }

    @Test
    public void testIncrementalUpdate() throws Exception {
        // Given:
        File bbbFile = new File(mTestPackageDir, "Bbb.class");
        Files.write(TestClasses.bbb(), bbbFile);
        Files.write(TestClasses.aaa(), new File(mTestPackageDir, "Aaa.class"));
        Files.write(TestClasses.ccc(), new File(mTestPackageDir, "Ccc.class"));

        mShrinker.run(
                mClassStreams,
                ImmutableMap.<Shrinker.ShrinkType, Shrinker.KeepRules>of(
                        Shrinker.ShrinkType.SHRINK, new SimpleKeepRules("Bbb", "bbb")));

        Files.write(TestClasses.bbb2(), bbbFile);

        // When:
        mShrinker.handleFileChanges(
                ImmutableMap.of(bbbFile, FileStatus.CHANGED),
                mClassStreams,
                ImmutableMap.<Shrinker.ShrinkType, Shrinker.KeepRules>of(
                        Shrinker.ShrinkType.SHRINK, new SimpleKeepRules("Bbb", "bbb")));

        // Then:
        assertMethodsLeft("Aaa", "ccc:()V");
        assertMethodsLeft("Bbb", "bbb:(Ltest/Aaa;)V");
        assertClassSkipped("Ccc");
    }

    private void assertClassSkipped(String className) {
        assertFalse(getOutputClassFile(className).exists());
    }

    private void assertMethodsLeft(String className, String... methods) throws IOException {
        File outFile = getOutputClassFile(className);
        assertEquals(Sets.newHashSet(methods), getMethods(outFile));
    }

    @NonNull
    private File getOutputClassFile(String className) {
        return FileUtils.join(mOutDir, "test", className + ".class");
    }

    private static Set<String> getMethods(File classFile) throws IOException {
        ClassReader classReader = new ClassReader(Files.toByteArray(classFile));
        ClassNode classNode = new ClassNode(Opcodes.ASM5);
        classReader.accept(
                classNode,
                ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        Set<String> methods = Sets.newHashSet();
        //noinspection unchecked - ASM API
        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            methods.add(methodNode.name + ":" + methodNode.desc);
        }

        return methods;
    }

    private static class SimpleKeepRules implements Shrinker.KeepRules {
        private final String mClassName;
        private final String mMethodName;

        private SimpleKeepRules(String className, String methodName) {
            mClassName = className;
            mMethodName = methodName;
        }


        @Override
        public boolean keep(ClassNode classNode, MethodNode methodNode) {
            return classNode.name.endsWith(mClassName) && methodNode.name.equals(mMethodName);
        }
    }
}