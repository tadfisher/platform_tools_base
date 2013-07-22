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
        return toString(STRING_FORMATTER);
    }

    public String toString(MethodFormatter f) {
        StringBuilder sb = new StringBuilder(100);
        printCalleeInformation(sb, f);
        return sb.toString();
    }

    public interface MethodFormatter {
        String format(long methodId);
    }

    public static MethodFormatter STRING_FORMATTER = new MethodFormatter() {
        @Override
        public String format(long methodId) {
            return Long.toString(methodId);
        }
    };

    private void printCalleeInformation(@NonNull StringBuilder sb, MethodFormatter formatter) {
        sb.append(" -> ");
        sb.append(formatter.format(mMethodId));

        List<Call> callees = getCallees();
        if (callees == null) {
            return;
        }

        int lineStart = sb.lastIndexOf("\n");
        int depth = sb.length() - (lineStart + 1);

        for (int i = 0; i < callees.size(); i++) {
            if (i != 0) {
                sb.append("\n");
                sb.append(Strings.repeat(" ", depth));
            }

            Call callee = callees.get(i);
            callee.printCalleeInformation(sb, formatter);
        }
    }
}
