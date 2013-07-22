package com.android.tools.perflib.vmtrace;

import com.android.utils.SparseArray;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VmTraceData {
    public static enum ClockType { THREAD_CPU, WALL, DUAL };

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
}
