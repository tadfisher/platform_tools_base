/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.chartlib;

import com.android.annotations.VisibleForTesting;
import com.android.annotations.concurrency.GuardedBy;

import java.util.List;

/**
 * A group of streams of data sampled over time. This object is thread safe as it can be
 * read/modified from any thread. It uses itself as the mutex object so it is possible to
 * synchronize on it if modifications from other threads want to be prevented.
 */
public class TimelineData {

    private final int myStreams;

    @GuardedBy("this")
    private final List<Sample> mSamples;

    @GuardedBy("this")
    private long mStart;

    @GuardedBy("this")
    private float mMaxTotal;

    @GuardedBy("this")
    private float mMinTotal;

    @GuardedBy("this")
    private final byte[] mDataSigns;

    public TimelineData(int streams, int capacity) {
        this(streams, capacity, new byte[streams]);
    }

    public TimelineData(int streams, int capacity, byte[] dataSigns) {
        myStreams = streams;
        mSamples = new CircularArrayList<Sample>(capacity);
        mDataSigns = dataSigns;
        clear();
    }

    @VisibleForTesting
    public synchronized long getStartTime() {
        return mStart;
    }

    public int getStreamCount() {
        return myStreams;
    }

    public byte[] getDataSigns() {
        return mDataSigns;
    }

    public boolean existsNegativeDataSign() {
        for (byte sign : mDataSigns) {
            if (sign < 0) {
                return true;
            }
        }
        return false;
    }

    public synchronized float getMaxTotal() {
        return mMaxTotal;
    }

    public synchronized float getMinTotal() {
        return mMinTotal;
    }

    public synchronized void add(long time, int type, float... values) {
        assert values.length == myStreams;
        float positiveTotal = 0.0f;
        float negativeTotal = 0.0f;
        for (int i = 0; i < myStreams; i++) {
            if (mDataSigns[i] >= 0) {
                positiveTotal += values[i];
            } else {
                negativeTotal -= values[i];
            }
        }
        mMaxTotal = Math.max(mMaxTotal, positiveTotal);
        mMinTotal = Math.min(mMinTotal, negativeTotal);
        mSamples.add(new Sample((time - mStart) / 1000.0f, type, values));
    }

    public synchronized void clear() {
        mSamples.clear();
        mMaxTotal = 0.0f;
        mMinTotal = 0.0f;
        mStart = System.currentTimeMillis();
    }

    public int size() {
        return mSamples.size();
    }

    public Sample get(int index) {
        return mSamples.get(index);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public synchronized float getEndTime() {
        return (mSamples.isEmpty() ? 0.0f : (System.currentTimeMillis() - mStart)) / 1000.f;
    }

    /**
     * A sample of all the streams at a given moment in time.
     */
    public static class Sample {

        /**
         * The time of the sample. In seconds since the start of the sampling.
         */
        public final float time;

        public final float[] values;

        public final int type;

        public Sample(float time, int type, float[] values) {
            this.time = time;
            this.values = values;
            this.type = type;
        }
    }
}
