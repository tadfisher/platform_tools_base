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

import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.annotations.concurrency.GuardedBy;

import java.util.*;

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

    public TimelineData(int streams, int capacity) {
        myStreams = streams;
        mSamples = new CircularArrayList<Sample>(capacity);
        clear();
    }

    @VisibleForTesting
    public synchronized long getStartTime() {
        return mStart;
    }

    public int getStreamCount() {
        return myStreams;
    }

    public synchronized float getMaxTotal() {
        return mMaxTotal;
    }

    public synchronized void add(long time, int type, float... values) {
        add(new Sample((time - mStart) / 1000.0f, type, values));
    }

    /**
     * Converts stream area values to stream samples. The converting may break into multiple sample
     * time points to ensure the value multiple time amount is correct. For example, a stream flow is a triangle when not stacked with
     * other streams, and there are four time points; then the triangle is split into four parts when stacked, each part's shape may
     * be changed while the area size is the same.
     *
     * @param time The current time in seconds from the start timestamp.
     * @param areas The streams' area sizes.
     * @param lastSample The last recent sample, which may be null.
     * @param type The timeline data type.
     */
    private static Sample[] convertAreasToSamples(float time, int type, float[] areas, @Nullable Sample lastSample) {
        int streamSize = areas.length;
        float lastSampleTime = lastSample != null ? lastSample.time : 0.0f;
        float[] lastValues = lastSample != null ? lastSample.values : new float[streamSize];
        assert streamSize == lastValues.length;
        float period = time - lastSampleTime;

        // Gets every stream's ending time point and ending value.
        float[] endTimes = new float[streamSize];
        float[] endValues = new float[streamSize];
        for (int i = 0; i < streamSize; i++) {
            if (Float.compare(lastValues[i] * period / 2, areas[i]) <= 0) {
                endTimes[i] = period;
                endValues[i] = areas[i] * 2 / period - lastValues[i];
            }
            else if (areas[i] == 0) {
                endTimes[i] = period;
                endValues[i] = 0;
            }
            else {
                endTimes[i] = areas[i] * 2 / lastValues[i];
                endValues[i] = 0;
            }
        }

        // Gets every stream's value at every time point.
        float[] ascendingSampleTimes = Arrays.copyOf(endTimes, streamSize);
        Arrays.sort(ascendingSampleTimes);
        // Creates a list to hold the samples of unknown size, then converts to an array at last.
        List<Sample> sampleList = new ArrayList<Sample>();
        float lastTime = -1.0f;
        for (float sampleTime : ascendingSampleTimes) {
            if (Float.compare(sampleTime, lastTime) == 0) {
                continue;
            }
            float[] sampleValues = new float[streamSize];
            for (int j = 0; j < streamSize; j++) {
                sampleValues[j] = lastValues[j] - (lastValues[j] - endValues[j]) * sampleTime / endTimes[j];
                if (sampleValues[j] < 0) {
                    sampleValues[j] = 0.0f;
                }
            }
            sampleList.add(new Sample(sampleTime + lastSampleTime, type, sampleValues));
            lastTime = sampleTime;
        }
        if (Float.compare(lastTime, period) < 0) {
            sampleList.add(new Sample(time, type, new float[streamSize]));
        }
        Sample[] samples = new Sample[sampleList.size()];
        return sampleList.toArray(samples);
    }


    /**
     * Adds the stream values which are values converted from the areas values. The values depends on both last sample values and
     * the current areas' sizes. It should be a synchronized method to let the last recent sample be accurate.
     *
     * @param timeMills The current time in mills.
     * @param type Sample data type.
     * @param areas Value multiple time area sizes for all streams.
     */
    public synchronized void addFromArea(long timeMills, int type, float... areas) {
        float timeForStart = (timeMills - mStart) / 1000.0f;
        Sample lastSample = mSamples.isEmpty() ? null : mSamples.get(mSamples.size() - 1);
        for (Sample sample : convertAreasToSamples(timeForStart, type, areas, lastSample)) {
            add(sample);
        }
    }

    private void add(Sample sample) {
        float[] values = sample.values;
        assert values.length == myStreams;
        float total = 0.0f;
        for (float value : values) {
            total += value;
        }
        mMaxTotal = Math.max(mMaxTotal, total);
        mSamples.add(sample);
    }

    public synchronized void clear() {
        mSamples.clear();
        mMaxTotal = 0.0f;
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
