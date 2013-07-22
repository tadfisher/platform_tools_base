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

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/** Reconstructs call stacks from a sequence of trace events (method entry/exit events) */
public class CallStackReconstructor {
    private List<Call> mTopLevelCallees = new ArrayList<Call>();
    private Stack<Call> mCallStack = new Stack<Call>();

    public void addTraceAction(long methodId, TraceAction action, int threadTime, int globalTime) {
        if (action == TraceAction.METHOD_ENTER) {
            enterMethod(methodId, threadTime, globalTime);
        } else {
            exitMethod(methodId, threadTime, globalTime);
        }
    }

    private void enterMethod(long methodId, int threadTime, int globalTime) {
        Call c = new Call(methodId);
        c.setMethodEntryTime(threadTime, globalTime);

        if (mCallStack.isEmpty()) {
            mTopLevelCallees.add(c);
        } else {
            Call caller = mCallStack.peek();
            caller.addCallee(c);
        }

        mCallStack.push(c);
    }

    private void exitMethod(long methodId, int threadTime, int globalTime) {
        if (!mCallStack.isEmpty()) {
            Call c = mCallStack.pop();
            if (c.getMethodId() != methodId) {
                String msg = String
                        .format("Error during call stack reconstruction. Attempt to exit from method 0x%1$x while in method 0x%2$x",
                                c.getMethodId(), methodId);
                throw new RuntimeException(msg);
            }

            c.setMethodExitTime(threadTime, globalTime);
        }
    }

    public void reconstruct() {
        // fixup whatever needs fixing up

        // make things final
        mTopLevelCallees = new ImmutableList.Builder<Call>().addAll(mTopLevelCallees).build();
    }

    public List<Call> getTopLevelCallees() {
        return mTopLevelCallees;
    }
}
