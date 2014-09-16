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

package com.android.builder.tasks;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by jedo on 9/15/14.
 */
public class Job<T> {

    private final String mJobTitle;
    private final Task<T> mTask;
    private final BooleanLatch mBooleanLatch;
    private final AtomicBoolean mResult = new AtomicBoolean(false);

    public Job(String jobTile, Task<T> task) {
        mJobTitle = jobTile;
        mTask = task;
        mBooleanLatch = new BooleanLatch();
    }

    public String getJobTitle() {
        return mJobTitle;
    }

    public void runTask(JobContext<T> jobContext) throws IOException {
        mTask.run(this, jobContext);
    }

    public void finished() {
        mResult.set(true);
        mBooleanLatch.signal();
    }

    public void error() {
        mResult.set(false);
        mBooleanLatch.signal();
    }

    public boolean await() throws InterruptedException {
        mBooleanLatch.await();
        return mResult.get();
    }
}
