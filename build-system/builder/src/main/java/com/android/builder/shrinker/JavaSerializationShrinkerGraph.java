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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.android.annotations.NonNull;
import com.android.utils.FileUtils;
import com.google.common.base.Splitter;
import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple {@link ShrinkerGraph} implementation that uses strings, maps and Java serialization.
 */
public class JavaSerializationShrinkerGraph implements ShrinkerGraph<String> {

    private final File mStateDir;

    private Set<String> mVirtualMethods = Sets.newConcurrentHashSet();

    private Map<String, File> mClasses = Maps.newConcurrentMap();

    private SetMultimap<String, String> mMembers = HashMultimap.create();

    private EnumMap<Shrinker.ShrinkType, Multiset<String>> mReferenceCounters;

    private SetMultimap<String, String> mDependencies = HashMultimap.create();


    public JavaSerializationShrinkerGraph(File stateDir) {
        mStateDir = checkNotNull(stateDir);
        mReferenceCounters =
                new EnumMap<Shrinker.ShrinkType, Multiset<String>>(Shrinker.ShrinkType.class);
        for (Shrinker.ShrinkType shrinkType : Shrinker.ShrinkType.values()) {
            mReferenceCounters.put(shrinkType, ConcurrentHashMultiset.<String>create());
        }
    }

    @Override
    public String addMethod(ClassNode classNode, MethodNode methodNode) {
        String methodName = getFullMethodName(classNode.name, methodNode.name, methodNode.desc);
        mMembers.put(classNode.name, methodName);
        return methodName;
    }

    @Override
    public String getMethodReference(String className, String methodName, String methodDesc) {
        return getFullMethodName(className, methodName, methodDesc);
    }

    @NonNull
    private static String getFullMethodName(String className, String methodName, String typeDesc) {
        return className + "." + methodName + ":" + typeDesc;
    }

    @Override
    public void addVirtualMethod(String fullName) {
        mVirtualMethods.add(fullName);
    }

    @Override
    public void addDependency(String from, String to) {
        mDependencies.put(from, to);
    }

    @Override
    public Set<String> getDependencies(String member) {
        return mDependencies.get(member);
    }

    @Override
    public Set<String> getMembers(String klass) {
        return mMembers.get(klass);
    }

    @Override
    public int getAndIncrement(String member, Shrinker.ShrinkType shrinkType) {
        return mReferenceCounters.get(shrinkType).add(member, 1);
    }

    @Override
    public void saveState() throws IOException {
        ObjectOutputStream stream =
                new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(getStateFile())));
        try {
            stream.writeObject(mVirtualMethods);
            stream.writeObject(mClasses);
            stream.writeObject(mMembers);
            stream.writeObject(mReferenceCounters);
            stream.writeObject(mDependencies);
        } finally {
            stream.close();
        }
    }

    @Override
    public int getCounter(String member, Shrinker.ShrinkType shrinkType) {
        return mReferenceCounters.get(shrinkType).count(member);
    }

    @Override
    public void removeDependency(String from, String to) {
        mDependencies.remove(from, to);
    }

    @Override
    public int decrementAndGet(String member, Shrinker.ShrinkType shrinkType) {
        int oldValue = mReferenceCounters.get(shrinkType).remove(member, 1);
        checkState(oldValue > 0);
        return oldValue - 1;
    }

    @NonNull
    private File getStateFile() {
        return new File(mStateDir, "shrinker.bin");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void loadState() throws IOException {
        ObjectInputStream stream =
                new ObjectInputStream(new BufferedInputStream(new FileInputStream(getStateFile())));

        try {
            mVirtualMethods = (Set<String>) stream.readObject();
            mClasses = (Map<String, File>) stream.readObject();
            mMembers = (SetMultimap<String, String>) stream.readObject();
            mReferenceCounters =
                    (EnumMap<Shrinker.ShrinkType, Multiset<String>>) stream.readObject();
            mDependencies = (SetMultimap<String, String>) stream.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            stream.close();
        }
    }

    @Override
    public void removeStoredState() throws IOException {
        FileUtils.emptyFolder(mStateDir);
    }

    @Override
    public Collection<String> getClassesToKeep(Shrinker.ShrinkType shrinkType) {
        List<String> classesToKeep = Lists.newArrayList();
        for (String klass : mClasses.keySet()) {
            if (mReferenceCounters.get(shrinkType).contains(klass)) {
                classesToKeep.add(klass);
            }
        }

        return classesToKeep;
    }

    @Override
    public File getClassFile(String klass) {
        return mClasses.get(klass);
    }

    @Override
    public Set<Shrinker.MemberId> getMembersToKeep(
            String klass,
            Shrinker.ShrinkType shrinkType) {
        Set<Shrinker.MemberId> memberIds = Sets.newHashSet();
        for (String member : mMembers.get(klass)) {
            if (mReferenceCounters.get(shrinkType).contains(member)) {
                member = member.substring(member.indexOf('.') + 1);
                List<String> parts = Splitter.on(':').splitToList(member);
                memberIds.add(new Shrinker.MemberId(parts.get(0), parts.get(1)));
            }
        }

        return memberIds;
    }

    @Override
    public String getTypeReference(Type type) {
        return type.getInternalName();
    }

    @Override
    public String getClassForMember(String member) {
        return member.substring(0, member.indexOf('.'));
    }

    @Override
    public String getClassReference(String className) {
        return className;
    }

    @Override
    public String addClass(ClassNode classNode, File classFile) {
        mClasses.put(classNode.name, classFile);
        return classNode.name;
    }
}
