/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.tools.perflib.vmtrace;

import com.android.utils.SparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VmTraceData {
    public static enum ClockType { THREAD_CPU, WALL, DUAL }

    private int mVersion;
    private boolean mDataFileOverflow;
    private ClockType mClockType = ClockType.THREAD_CPU;
    private String mVm = "";
    private final Map<String, String> mProperties = new HashMap<String, String>(10);

    private final SparseArray<String> mThreads = new SparseArray<String>(10);

    private final Map<Long,MethodInfo> mMethods = new HashMap<Long, MethodInfo>(100);

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        mVersion = version;
    }

    public boolean isDataFileOverflow() {
        return mDataFileOverflow;
    }

    public void setDataFileOverflow(boolean dataFileOverflow) {
        mDataFileOverflow = dataFileOverflow;
    }

    public ClockType getClockType() {
        return mClockType;
    }

    public void setClockType(ClockType clockType) {
        mClockType = clockType;
    }

    public void setProperty(String key, String value) {
        mProperties.put(key, value);
    }

    public void setVm(String vm) {
        mVm = vm;
    }

    public String getVm() {
        return mVm;
    }

    public void addThread(int id, String name) {
        mThreads.put(id, name);
    }

    public SparseArray<String> getThreads() {
        return mThreads;
    }

    public void addMethod(long id, MethodInfo info) {
        mMethods.put(id, info);
    }

    public Collection<MethodInfo> getMethods() {
        return mMethods.values();
    }

    public MethodInfo getMethod(long methodId) {
        return mMethods.get(methodId);
    }

    public void addMethodAction(int threadId, long methodId, TraceAction methodAction,
            int threadTime, int globalTime) {
        MethodInfo methodInfo = mMethods.get(methodId);

        // create thread info if it doesn't exist
        if (mThreads.get(threadId) == null) {
            mThreads.put(threadId, String.format("Thread id: %1$d", threadId));
        }

        // create method info if it doesn't exist
        if (mMethods.get(methodId) == null) {
            MethodInfo info = new MethodInfo(methodId, "unknown", "unknown", "unknown", "unknown", -1);
            mMethods.put(methodId, info);
        }

        System.out.println("Adding method action: " + methodAction + ": thread: " + mThreads.get(threadId)  + ", method: "
                + methodInfo.className + "/" + methodInfo.methodName + ":" + methodInfo.signature);
        return;
    }
}
