/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.ide.common.res2;


import com.android.annotations.NonNull;
import com.android.ide.common.blame.Message;
import com.android.ide.common.blame.SourceFile;
import com.android.ide.common.blame.SourceFilePosition;
import com.android.ide.common.blame.SourcePosition;
import com.google.common.collect.ImmutableList;

import java.io.File;

/**
 * Exception when a {@link DataItem} is declared more than once in a {@link DataSet}
 */
public class DuplicateDataException extends MergingException {
    DuplicateDataException(@NonNull DataItem one, @NonNull DataItem two) {
        super(null, createMessage(one, two));
    }


    private static Message createMessage(DataItem one, DataItem two) {
        return new Message(Message.Kind.ERROR, "Duplicate resources", getPosition(one), getPosition(two));
    }


    private static SourceFilePosition getPosition(DataItem item) {
        DataFile dataFile = item.getSource();
        if (dataFile == null) {
            return new SourceFilePosition(new SourceFile(item.getKey()), SourcePosition.UNKNOWN);
        }
        File f = dataFile.getFile();
        SourcePosition sourcePosition = SourcePosition.UNKNOWN;  // TODO: find position in file.
        return new SourceFilePosition(new SourceFile(f, item.getKey()), sourcePosition);
    }
}
