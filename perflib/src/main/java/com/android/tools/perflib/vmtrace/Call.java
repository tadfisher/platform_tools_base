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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

public class Call {
    private final long mMethodId;

    private int mEntryThreadTime;
    private int mEntryGlobalTime;
    private int mExitGlobalTime;
    private int mExitThreadTime;

    private List<Call> mCallees;

    public Call(long methodId) {
        mMethodId = methodId;
    }

    public long getMethodId() {
        return mMethodId;
    }

    public void setMethodEntryTime(int threadTime, int globalTime) {
        mEntryThreadTime = threadTime;
        mEntryGlobalTime = globalTime;
    }

    public void setMethodExitTime(int threadTime, int globalTime) {
        mExitThreadTime = threadTime;
        mExitGlobalTime = globalTime;
    }

    public void addCallee(Call c) {
        if (mCallees == null) {
            mCallees = new ArrayList<Call>();
        }

        mCallees.add(c);
    }

    @Nullable
    public List<Call> getCallees() {
        return mCallees;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        printCalleeInformation(sb);
        return sb.toString();
    }

    private void printCalleeInformation(@NonNull StringBuilder sb) {
        sb.append(" -> ");
        sb.append(mMethodId);

        if (mCallees == null) {
            return;
        }

        int lineStart = sb.lastIndexOf("\n");
        int depth = sb.length() - (lineStart + 1);

        for (int i = 0; i < mCallees.size(); i++) {
            if (i != 0) {
                sb.append("\n");
                sb.append(Strings.repeat(" ", depth));
            }

            Call callee = mCallees.get(i);
            callee.printCalleeInformation(sb);
        }
    }
}
