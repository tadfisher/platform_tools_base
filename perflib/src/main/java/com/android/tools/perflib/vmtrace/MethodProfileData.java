package com.android.tools.perflib.vmtrace;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MethodProfileData {
    private final MethodStats mAllThreadStats;

    private final Map<Integer, MethodStats> mPerThreadCumulativeStats;
    private final Table<Integer, Long, MethodStats> mPerThreadStatsByCallee;
    private final Table<Integer, Long, MethodStats> mPerThreadStatsByCaller;

    private MethodProfileData(Builder b) {
        mAllThreadStats = b.mAllThreadStats;
        mPerThreadCumulativeStats = ImmutableMap.copyOf(b.mPerThreadCumulativeStats);
        mPerThreadStatsByCallee = ImmutableTable.copyOf(b.mPerThreadStatsByCallee);
        mPerThreadStatsByCaller = ImmutableTable.copyOf(b.mPerThreadStatsByCaller);
    }

    public long getExclusiveTime(String thread, ClockType clockType) {
        return 0;
    }

    private static class MethodStats {
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

            addExclusiveTime(mAllThreadStats, time, type);
            addExclusiveTime(getPerThreadStats(thread), time, type);
            if (parent != null) {
                addExclusiveTime(getPerCallerStats(thread, parent), time, type);
            }
        }

        private void addInclusiveTime(Call call, Call parent, ThreadInfo thread, ClockType type) {
            long time = call.getInclusiveTime(type, TimeUnit.NANOSECONDS);

            addInclusiveTime(mAllThreadStats, time, type);
            addInclusiveTime(getPerThreadStats(thread), time, type);
            if (parent != null) {
                addInclusiveTime(getPerCallerStats(thread, parent), time, type);
            }
            for (Call callee: call.getCallees()) {
                addInclusiveTime(getPerCalleeStats(thread, callee), -1 /* TODO */, type);
            }
        }

        private void addInclusiveTime(MethodStats stats, long time, ClockType type) {
            if (type == ClockType.THREAD) {
                stats.mInclusiveThreadTime += time;
            } else {
                stats.mInclusiveGlobalTime += time;
            }
        }

        private void addExclusiveTime(MethodStats stats, long time, ClockType type) {
            if (type == ClockType.THREAD) {
                stats.mExclusiveThreadTime += time;
            } else {
                stats.mExclusiveGlobalTime += time;
            }
        }

        private MethodStats getPerThreadStats(ThreadInfo thread) {
            MethodStats stats = mPerThreadCumulativeStats.get(thread.getId());
            if (stats == null) {
                stats = new MethodStats();
                mPerThreadCumulativeStats.put(thread.getId(), stats);
            }
            return stats;
        }

        private MethodStats getPerCallerStats(ThreadInfo thread, Call parent) {
            return getMethodStatsFromTable(thread.getId(), parent.getMethodId(),
                    mPerThreadStatsByCaller);
        }

        private MethodStats getPerCalleeStats(ThreadInfo thread, Call callee) {
            return getMethodStatsFromTable(thread.getId(), callee.getMethodId(),
                    mPerThreadStatsByCallee);
        }

        private MethodStats getMethodStatsFromTable(Integer threadId, Long methodId,
                Table<Integer, Long, MethodStats> statsTable) {
            MethodStats stats = statsTable.get(threadId, methodId);
            if (stats == null) {
                stats = new MethodStats();
                statsTable.put(threadId, methodId, stats);
            }
            return stats;
        }

        public void incrementInvocationCount(Call c, ThreadInfo thread) {
            Long count = mPerThreadInvocationCounts.get(thread.getId(), c.getMethodId());
            if (count == null) {
                count = 0L;
            }

            count += 1;
            mPerThreadInvocationCounts.put(thread.getId(), c.getMethodId(), count);
        }

        public MethodProfileData build() {
            return new MethodProfileData(this);
        }
    }
}
