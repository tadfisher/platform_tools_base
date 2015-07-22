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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * State the {@link Shrinker} needs to keep between invocations.
 *
 * @param <T> Reference to a class member.
 */
interface ShrinkerGraph<T> {

    File getClassFile(T klass);

    Iterable<T> getClassesToKeep(Shrinker.ShrinkType shrinkType);

    Set<Shrinker.MemberId> getMembersToKeep(T klass, Shrinker.ShrinkType shrinkType);

    Set<T> getDependencies(T member);

    Set<T> getMembers(T klass);

    T addClass(ClassNode classNode, File classFile);

    T addMethod(ClassNode classNode, MethodNode methodNode);

    T getClassForMember(T member);

    T getClassReference(String className);

    T getMethodReference(String className, String methodName, String methodDesc);

    T getTypeReference(Type type);

    int getAndIncrement(T member, Shrinker.ShrinkType shrinkType);

    void addDependency(T from, T to);

    void addVirtualMethod(T method);

    void loadState() throws IOException;

    void removeStoredState() throws IOException;

    void saveState() throws IOException;

    int getCounter(T member, Shrinker.ShrinkType shrinkType);

    void removeDependency(T from, T to);

    int decrementAndGet(T member, Shrinker.ShrinkType shrinkType);
}
