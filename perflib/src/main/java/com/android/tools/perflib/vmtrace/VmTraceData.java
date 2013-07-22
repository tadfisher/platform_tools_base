package com.android.tools.perflib.vmtrace;

import java.util.HashMap;
import java.util.Map;

public class VmTraceData {
    public static enum ClockType { THREAD_CPU, WALL, DUAL };

    private int mVersion;
    private boolean mDataFileOverflow;
    private ClockType mClockType = ClockType.THREAD_CPU;
    private String mVm = "";
    private final Map<String, String> mProperties = new HashMap<String, String>(10);

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

    }
}
