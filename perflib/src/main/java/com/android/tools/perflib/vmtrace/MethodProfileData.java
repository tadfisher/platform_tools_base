package com.android.tools.perflib.vmtrace;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MethodProfileData {
    private final MethodStats mAllThreadStats;

    private final Map<String, MethodStats> mPerThreadCumulativeStats;
    private final Table<String, Long, MethodStats> mPerThreadStatsByCallee;
    private final Table<String, Long, MethodStats> mPerThreadStatsByCaller;

    private MethodProfileData() {
        mAllThreadStats = null;
        mPerThreadCumulativeStats = null;
        mPerThreadStatsByCallee = null;
        mPerThreadStatsByCaller = null;
    }

    private static class MethodStats {
        private long mCallCount;

        private long mInclusiveThreadTime;
        private long mExclusiveThreadTime;

        private long mInclusiveGlobalTime;
        private long mExclusiveGlobalTime;

        public long getInclusiveTime(ClockType clockType) {
            return clockType == ClockType.THREAD ? mInclusiveThreadTime : mInclusiveGlobalTime;
        }

        public long getExclusiveTime(ClockType clockType) {
            return clockType == ClockType.THREAD ? mExclusiveThreadTime : mExclusiveGlobalTime;
        }

        private void addInclusiveTime(long time, ClockType clockType) {
            if (clockType == ClockType.THREAD) {
                mInclusiveThreadTime += time;
            } else {
                mInclusiveGlobalTime += time;
            }
        }

        private void addExclusiveTime(long time, ClockType clockType) {
            if (clockType == ClockType.THREAD) {
                mExclusiveThreadTime += time;
            } else {
                mExclusiveGlobalTime += time;
            }
        }

        private void incrementCallCount() {
            mCallCount++;
        }

        private long getCallCount() {
            return mCallCount;
        }
    }

    public static class Builder {
        private final MethodStats mAllThreadStats = new MethodStats();

        private final Map<Integer, MethodStats> mPerThreadCumulativeStats = Maps.newHashMap();
        private final Table<Integer, Long, MethodStats> mPerThreadStatsByCaller =
                HashBasedTable.create();
        private final Table<Integer, Long, MethodStats> mPerThreadStatsByCallee =
                HashBasedTable.create();

        private final Table<Integer, Long, Long> mPerThreadInvocationCounts =
                HashBasedTable.create();

        public void addCallTime(Call call, Call parent, ThreadInfo thread) {
            for (ClockType type: ClockType.values()) {
                addExclusiveTime(call, parent, thread, type);

                if (!call.isRecursive()) {
                    addInclusiveTime(call, parent, thread, type);
                }
            }
        }

        private void addExclusiveTime(Call call, Call parent, ThreadInfo thread, ClockType type) {
            long time = call.getExclusiveTime(type, TimeUnit.NANOSECONDS);

            mAllThreadStats.addExclusiveTime(time, type);
            getPerThreadCumulativeStats(thread).addExclusiveTime(time, type);

            if (parent != null) {
                getPerCallerTime(thread, parent).addExclusiveTime(time, type);
            }
        }

        private void addInclusiveTime(Call call, Call parent, ThreadInfo thread, ClockType type) {
            long time = call.getInclusiveTime(type, TimeUnit.NANOSECONDS);

            mAllThreadStats.addInclusiveTime(time, type);
            getPerThreadCumulativeStats(thread).addInclusiveTime(time, type);

            if (parent != null) {
                getPerCallerTime(thread, parent).addInclusiveTime(time, type);
            }

            for (Call callee: call.getCallees()) {

            }
        }

        private MethodStats getPerThreadCumulativeStats(ThreadInfo thread) {
            MethodStats stats = mPerThreadCumulativeStats.get(thread.getId());
            if (stats == null) {
                stats = new MethodStats();
                mPerThreadCumulativeStats.put(thread.getId(), stats);
            }
            return stats;
        }

        private MethodStats getPerCallerTime(ThreadInfo thread, Call parent) {
            MethodStats stats = mPerThreadStatsByCaller.get(thread.getId(), parent.getMethodId());
            if (stats == null) {
                stats = new MethodStats();
                mPerThreadStatsByCaller.put(thread.getId(), parent.getMethodId(), stats);
            }
            return stats;
        }

        public void incrementInvocationCount(Call c, ThreadInfo thread) {
            Long count = mPerThreadInvocationCounts.get(thread.getId(), c.getMethodId());
            if (count == null) {
                count = Long.valueOf(0);
            }

            count += 1;
            mPerThreadInvocationCounts.put(thread.getId(), c.getMethodId(), count);
        }
    }
}
