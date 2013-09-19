package com.android.tools.perflib.vmtrace;

import com.android.annotations.Nullable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MethodProfileData {
    /** {@link TimeUnit} for all time values stored in this model. */
    private static final TimeUnit DATA_TIME_UNITS = TimeUnit.NANOSECONDS;

    private final Map<Integer, MethodStats> mPerThreadCumulativeStats;
    private final Table<Integer, Long, MethodStats> mPerThreadStatsByCallee;
    private final Table<Integer, Long, MethodStats> mPerThreadStatsByCaller;
    private final Map<Integer, Long> mPerThreadInvocationCounts;
    private final boolean mIsRecursive;

    private MethodProfileData(Builder b) {
        mPerThreadCumulativeStats = ImmutableMap.copyOf(b.mPerThreadCumulativeStats);
        mPerThreadStatsByCallee = ImmutableTable.copyOf(b.mPerThreadStatsByCallee);
        mPerThreadStatsByCaller = ImmutableTable.copyOf(b.mPerThreadStatsByCaller);
        mPerThreadInvocationCounts = ImmutableMap.copyOf(b.mPerThreadInvocationCounts);
        mIsRecursive = b.mRecursive;
    }

    public long getInvocationCount(ThreadInfo thread) {
        Long value = mPerThreadInvocationCounts.get(thread.getId());
        return value != null ? value : 0;
    }

    public boolean isRecursive() {
        return mIsRecursive;
    }

    public long getExclusiveTime(ThreadInfo thread, ClockType clockType, TimeUnit unit) {
        MethodStats stats = mPerThreadCumulativeStats.get(thread.getId());
        return getExclusiveTime(stats, clockType, unit);
    }

    public long getInclusiveTime(ThreadInfo thread, ClockType clockType, TimeUnit unit) {
        MethodStats stats = mPerThreadCumulativeStats.get(thread.getId());
        return getInclusiveTime(stats, clockType, unit);
    }

    public Set<Long> getCallers(ThreadInfo thread) {
        Map<Long, MethodStats> perCallerStats = mPerThreadStatsByCaller.row(thread.getId());
        return perCallerStats.keySet();
    }

    public Set<Long> getCallees(ThreadInfo thread) {
        Map<Long, MethodStats> perCalleeStats = mPerThreadStatsByCallee.row(thread.getId());
        return perCalleeStats.keySet();
    }

    public long getExclusiveTimeByCaller(ThreadInfo thread, Long callerId,
            ClockType clockType, TimeUnit unit) {
        MethodStats stats = mPerThreadStatsByCaller.get(thread.getId(), callerId);
        return getExclusiveTime(stats, clockType, unit);
    }

    public long getInclusiveTimeByCaller(ThreadInfo thread, Long callerId,
            ClockType clockType, TimeUnit unit) {
        MethodStats stats = mPerThreadStatsByCaller.get(thread.getId(), callerId);
        return getInclusiveTime(stats, clockType, unit);
    }

    public long getInclusiveTimeByCallee(ThreadInfo thread, Long calleeId,
            ClockType clockType, TimeUnit unit) {
        MethodStats stats = mPerThreadStatsByCallee.get(thread.getId(), calleeId);
        return getInclusiveTime(stats, clockType, unit);
    }

    private long getExclusiveTime(@Nullable MethodStats stats, ClockType clockType, TimeUnit unit) {
        return stats != null ? stats.getExclusiveTime(clockType, unit) : 0;
    }

    private long getInclusiveTime(MethodStats stats, ClockType clockType,
            TimeUnit unit) {
        return stats != null ? stats.getInclusiveTime(clockType, unit) : 0;
    }

    private static class MethodStats {
        private long mInclusiveThreadTime;
        private long mExclusiveThreadTime;

        private long mInclusiveGlobalTime;
        private long mExclusiveGlobalTime;

        public long getInclusiveTime(ClockType clockType, TimeUnit unit) {
            long time = clockType == ClockType.THREAD ? mInclusiveThreadTime : mInclusiveGlobalTime;
            return unit.convert(time, DATA_TIME_UNITS);
        }

        public long getExclusiveTime(ClockType clockType, TimeUnit unit) {
            long time = clockType == ClockType.THREAD ? mExclusiveThreadTime : mExclusiveGlobalTime;
            return unit.convert(time, DATA_TIME_UNITS);
        }
    }

    public static class Builder {
        private final Map<Integer, MethodStats> mPerThreadCumulativeStats = Maps.newHashMap();
        private final Table<Integer, Long, MethodStats> mPerThreadStatsByCaller =
                HashBasedTable.create();
        private final Table<Integer, Long, MethodStats> mPerThreadStatsByCallee =
                HashBasedTable.create();

        private final Map<Integer, Long> mPerThreadInvocationCounts = Maps.newHashMap();

        private boolean mRecursive;

        public void addCallTime(Call call, Call parent, ThreadInfo thread) {
            for (ClockType type: ClockType.values()) {
                addExclusiveTime(call, parent, thread, type);

                if (!call.isRecursive()) {
                    addInclusiveTime(call, parent, thread, type);
                }
            }
        }

        private void addExclusiveTime(Call call, Call parent, ThreadInfo thread, ClockType type) {
            long time = call.getExclusiveTime(type, DATA_TIME_UNITS);

            addExclusiveTime(getPerThreadStats(thread), time, type);
            if (parent != null) {
                addExclusiveTime(getPerCallerStats(thread, parent), time, type);
            }
        }

        private void addInclusiveTime(Call call, Call parent, ThreadInfo thread, ClockType type) {
            long time = call.getInclusiveTime(type, DATA_TIME_UNITS);

            addInclusiveTime(getPerThreadStats(thread), time, type);
            if (parent != null) {
                addInclusiveTime(getPerCallerStats(thread, parent), time, type);
            }
            for (Call callee: call.getCallees()) {
                addInclusiveTime(getPerCalleeStats(thread, callee),
                        callee.getInclusiveTime(type, DATA_TIME_UNITS), type);
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

        public void incrementInvocationCount(ThreadInfo thread) {
            Long count = mPerThreadInvocationCounts.get(thread.getId());
            if (count == null) {
                count = 0L;
            }

            count += 1;
            mPerThreadInvocationCounts.put(thread.getId(), count);
        }

        public MethodProfileData build() {
            return new MethodProfileData(this);
        }

        public void setRecursive() {
            mRecursive = true;
        }
    }
}
