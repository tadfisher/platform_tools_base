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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.internal.WaitableExecutor;
import com.android.ide.common.res2.FileStatus;
import com.android.utils.FileUtils;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * TODO: Document.
 */
public class Shrinker<T> {
    private static final int EMPTY_FLAGS = 0;

    private final WaitableExecutor<Void> mExecutor;
    private final ShrinkerGraph<T> mGraph;

    public Shrinker(WaitableExecutor<Void> executor, ShrinkerGraph<T> graph) {
        mExecutor = executor;
        mGraph = graph;
    }

    private static ClassStream findSource(File classFile, Collection<ClassStream> streams) {
        for (ClassStream stream : streams) {
            if (stream.contains(classFile)) {
                return stream;
            }
        }

        throw new IllegalStateException("Can't find the source of " + classFile.getAbsolutePath());
    }

    private static List<MethodNode> getMethodNodes(ClassNode classNode) {
        //noinspection unchecked - ASM doesn't use generics.
        return classNode.methods;
    }

    @NonNull
    private static ClassNode readClassNode(File classFile) throws IOException {
        ClassReader classReader = new ClassReader(Files.toByteArray(classFile));
        ClassNode classNode = new ClassNode(Opcodes.ASM5);
        classReader.accept(classNode, EMPTY_FLAGS);
        return classNode;
    }

    private static byte[] rewrite(File classFile, Set<MemberId> membersToKeep) throws IOException {
        // TODO: Use visitor API.
        ClassNode classNode = readClassNode(classFile);

        Set<MethodNode> methodsToKeep = Sets.newHashSet();
        List<MethodNode> methodNodes = getMethodNodes(classNode);
        for (MethodNode methodNode : methodNodes) {
            MemberId memberId = new MemberId(methodNode.name, methodNode.desc);
            if (membersToKeep.contains(memberId)) {
                methodsToKeep.add(methodNode);
            }
        }
        methodNodes.retainAll(methodsToKeep);

        ClassWriter classWriter = new ClassWriter(EMPTY_FLAGS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    @NonNull
    private static UnsupportedOperationException todo(String message) {
        return new UnsupportedOperationException(message);
    }

    private ImmutableMap<ShrinkType, Set<T>> buildMapPerShrinkType(
            ImmutableMap<ShrinkType, KeepRules> keepRules) {
        ImmutableMap.Builder<ShrinkType, Set<T>> builder = ImmutableMap.builder();
        for (ShrinkType shrinkType : keepRules.keySet()) {
            builder.put(shrinkType, Sets.<T>newConcurrentHashSet());
        }

        return builder.build();
    }

    private void buildGraph(
            Collection<ClassStream> streams,
            final Map<ShrinkType, KeepRules> keepRules,
            final ImmutableMap<ShrinkType, Set<T>> entryPoints) {

        for (ClassStream stream : streams) {
            for (final File classFile : stream.getClassFiles()) {
                mExecutor.execute(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        processNewClassFile(classFile, keepRules, entryPoints);
                        return null;
                    }
                });
            }
        }

        try {
            mExecutor.waitForAllTasks();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void decrementCounter(T member, ShrinkType shrinkType, @Nullable ImmutableMap<ShrinkType, Set<T>> modifiedClasses) {
        // TODO: Avoid cycles.
        int newValue = mGraph.decrementAndGet(member, shrinkType);
        if (newValue == 0) {
            if (modifiedClasses != null) {
                modifiedClasses.get(shrinkType).add(mGraph.getClassForMember(member));
            }
            for (T dependency : mGraph.getDependencies(member)) {
                decrementCounter(dependency, shrinkType, modifiedClasses);
            }
        }
    }

    @NonNull
    private Set<T> getDependencies(MethodNode methodNode) {
        Set<T> deps = Sets.newHashSet();

        Type methodType = Type.getMethodType(methodNode.desc);
        for (Type argType : methodType.getArgumentTypes()) {
            // TODO: skip primitive types, handle arrays etc.
            T argTypeRef = mGraph.getTypeReference(argType);
            deps.add(argTypeRef);
        }
        // TODO: process return type.

        for (AbstractInsnNode instruction : methodNode.instructions.toArray()) {
            if (instruction.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode methodInstruction = (MethodInsnNode) instruction;
                T target =
                        mGraph.getMethodReference(
                                methodInstruction.owner,
                                methodInstruction.name,
                                methodInstruction.desc);
                deps.add(target);
            }
        }
        return deps;
    }

    public void handleFileChanges(
            Map<File, FileStatus> changedFiles,
            Collection<ClassStream> streams,
            final ImmutableMap<ShrinkType, KeepRules> keepRules)
            throws IOException, InterruptedException {
        mGraph.loadState();

        final ImmutableMap<ShrinkType, Set<T>> newEntryPoints = buildMapPerShrinkType(keepRules);
        final ImmutableMap<ShrinkType, Set<T>> modifiedClasses = buildMapPerShrinkType(keepRules);

        for (final Map.Entry<File, FileStatus> entry : changedFiles.entrySet()) {
            mExecutor.execute(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    switch (entry.getValue()) {
                        case NEW:
                            throw todo("new file");
                        case REMOVED:
                            throw todo("removed file");
                        case CHANGED:
                            processChangedClassFile(
                                    entry.getKey(),
                                    keepRules,
                                    newEntryPoints,
                                    modifiedClasses);
                            break;
                    }
                    return null;
                }
            });
        }

        mExecutor.waitForAllTasks();

        for (ShrinkType shrinkType : keepRules.keySet()) {
            updateClassFiles(modifiedClasses.get(shrinkType), shrinkType, streams);
        }

        mExecutor.waitForAllTasks();
    }

    private void incrementCounter(T member, ShrinkType shrinkType, @Nullable ImmutableMap<ShrinkType, Set<T>> modifiedClasses) {
        // TODO: Avoid cycles.
        int previous = mGraph.getAndIncrement(member, shrinkType);
        if (previous == 0) {
            if (modifiedClasses != null) {
                modifiedClasses.get(shrinkType).add(mGraph.getClassForMember(member));
            }
            for (T dependency : mGraph.getDependencies(member)) {
                incrementCounter(dependency, shrinkType, modifiedClasses);
            }
        }
    }

    private void processChangedClassFile(
            File classFile,
            Map<ShrinkType, KeepRules> keepRules,
            ImmutableMap<ShrinkType, Set<T>> entryPoints,
            ImmutableMap<ShrinkType, Set<T>> modifiedClasses) throws IOException {
        // TODO: Use the visitor API to save memory.
        ClassNode classNode = readClassNode(classFile);
        T klass = mGraph.getClassReference(classNode.name);

        for (ShrinkType shrinkType : keepRules.keySet()) {
            // If the class is in the output, it needs to be rewritten, to reflect changes.
            if (mGraph.getCounter(klass, shrinkType) > 0) {
                modifiedClasses.get(shrinkType).add(klass);
            }
        }

        Set<T> oldMembers = mGraph.getMembers(klass);

        for (MethodNode methodNode : getMethodNodes(classNode)) {
            T method = mGraph.getMethodReference(classNode.name, methodNode.name, methodNode.desc);

            if (!oldMembers.contains(method)) {
                throw todo("added method");
            } else {
                Set<T> oldDeps = mGraph.getDependencies(method);
                Set<T> currentDeps = getDependencies(methodNode);

                for (T addedDep : Sets.difference(currentDeps, oldDeps)) {
                    mGraph.addDependency(method, addedDep);
                    for (ShrinkType shrinkType : keepRules.keySet()) {
                        if (mGraph.getCounter(method, shrinkType) > 0) {
                            incrementCounter(addedDep, shrinkType, modifiedClasses);
                        }
                    }
                }

                for (T removedDep : Sets.difference(oldDeps, currentDeps)) {
                    mGraph.removeDependency(method, removedDep);
                    for (ShrinkType shrinkType : keepRules.keySet()) {
                        if (mGraph.getCounter(method, shrinkType) > 0) {
                            decrementCounter(removedDep, shrinkType, modifiedClasses);
                        }
                    }
                }

                // Keep only unprocessed members, so we know which ones were deleted.
                oldMembers.remove(method);
            }
        }

        // TODO: process fields

        for (T deletedMember : oldMembers) {
            throw todo("deleted member");
        }
    }

    private void processNewClassFile(
            File classFile,
            Map<ShrinkType, KeepRules> keepRules,
            ImmutableMap<ShrinkType, Set<T>> entryPoints) throws IOException {
        // TODO: Use the visitor API to save memory.
        ClassNode classNode = readClassNode(classFile);

        mGraph.addClass(classNode, classFile);

        for (MethodNode methodNode : getMethodNodes(classNode)) {
            T method = mGraph.addMethod(classNode, methodNode);

            // TODO: check if it's private or final
            mGraph.addVirtualMethod(method);

            for (T dep : getDependencies(methodNode)) {
                mGraph.addDependency(method, dep);
            }

            for (Map.Entry<ShrinkType, KeepRules> entry : keepRules.entrySet()) {
                if (entry.getValue().keep(classNode, methodNode)) {
                    entryPoints.get(entry.getKey()).add(method);
                }
            }
        }

        // TODO: handle fields
        // TODO: handle superclasses
        // TODO: handle annotations
    }

    public void run(
            Collection<ClassStream> streams,
            ImmutableMap<ShrinkType, KeepRules> keepRules) throws IOException {
        mGraph.removeStoredState();

        for (ClassStream stream : streams) {
            for (ShrinkType shrinkType : keepRules.keySet()) {
                FileUtils.emptyFolder(stream.getOutputDir(shrinkType));
            }
        }

        ImmutableMap<ShrinkType, Set<T>> entryPoints = buildMapPerShrinkType(keepRules);

        buildGraph(streams, keepRules, entryPoints);

        // TODO: Parallelize.
        for (ShrinkType shrinkType : keepRules.keySet()) {
            Set<T> classesToKeep = Sets.newHashSet();
            for (T entryPoint : entryPoints.get(shrinkType)) {
                incrementCounter(entryPoint, shrinkType, null);

                // TODO: Document the potential cycle here.
                T klass = mGraph.getClassForMember(entryPoint);
                classesToKeep.add(klass);
            }

            Iterables.addAll(classesToKeep, mGraph.getClassesToKeep(shrinkType));

            updateClassFiles(classesToKeep, shrinkType, streams);
        }

        mGraph.saveState();
    }

    private void updateClassFiles(Set<T> classesToKeep, ShrinkType shrinkType,
            Collection<ClassStream> streams) throws IOException {
        for (T klass : classesToKeep) {
            File classFile = mGraph.getClassFile(klass);
            ClassStream source = findSource(classFile, streams);
            String path = FileUtils.relativePath(classFile, source.getClassDir());
            File outputFile = new File(source.getOutputDir(shrinkType), path);
            Files.createParentDirs(outputFile);
            Files.write(rewrite(classFile, mGraph.getMembersToKeep(klass, shrinkType)),
                    outputFile);
        }
    }

    enum ShrinkType {
        SHRINK, LEGACY_MULTIDEX
    }

    interface KeepRules {
        boolean keep(ClassNode classNode, MethodNode methodNode);
    }

    public static class MemberId {
        public String name;
        public String desc;

        public MemberId(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MemberId memberId = (MemberId) o;
            return Objects.equal(name, memberId.name) &&
                    Objects.equal(desc, memberId.desc);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(name, desc);
        }
    }

}
