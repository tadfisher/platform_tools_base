/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.ide.common.blame;


import com.android.annotations.NonNull;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.util.List;

public class MergingLogRewriter implements MessageReceiver {

    private final MessageReceiver mMessageReceiver;
    private final MergingLog mMergingLog;
    private final Function<SourceFilePosition, SourceFilePosition> mGetOriginalPosition;


    public MergingLogRewriter(MergingLog mergingLog, MessageReceiver messageReceiver) {
        mMessageReceiver = messageReceiver;
        mMergingLog = mergingLog;
        mGetOriginalPosition = new Function<SourceFilePosition, SourceFilePosition>() {
            @Override
            public SourceFilePosition apply(SourceFilePosition input) {
                return mMergingLog.find(input);
            }
        };
    }

    @Override
    public void receiveMessage(@NonNull Message m) {
        List<SourceFilePosition> originalPositions = m.getSourceFilePositions();

        Iterable<SourceFilePosition> positions =
                Iterables.transform(originalPositions, mGetOriginalPosition);

        mMessageReceiver.receiveMessage(
                new Message(
                        m.getKind(),
                        m.getText(),
                        m.getRawMessage(),
                        ImmutableList.copyOf(positions)));
    }
}
