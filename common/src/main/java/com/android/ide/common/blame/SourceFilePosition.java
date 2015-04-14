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
import com.android.annotations.concurrency.Immutable;
import com.google.common.base.Objects;

@Immutable
public final class SourceFilePosition {

    public static final com.android.ide.common.blame.SourceFilePosition UNKNOWN =
            new SourceFilePosition(SourceFile.UNKNOWN, SourcePosition.UNKNOWN);

    @NonNull
    private final SourceFile mSourceFile;

    @NonNull
    private final SourcePosition mSourcePosition;

    public SourceFilePosition(@NonNull SourceFile sourceFile,
            @NonNull SourcePosition sourcePosition) {
        mSourceFile = sourceFile;
        mSourcePosition = sourcePosition;
    }

    @NonNull
    public SourcePosition getPosition() {
        return mSourcePosition;
    }

    @NonNull
    public SourceFile getFile() {
        return mSourceFile;
    }

    @Override
    public String toString() {
        return print(false);
    }

    public String print (boolean shortFormat) {
        if (mSourcePosition.equals(SourcePosition.UNKNOWN)) {
            return mSourceFile.print(shortFormat);
        } else {
            return mSourceFile.print(shortFormat) + ':' + mSourcePosition.toString();
        }
    }

    @Override
    public int hashCode() {
        return  Objects.hashCode(mSourceFile, mSourcePosition);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SourceFilePosition)) {
            return false;
        }
        SourceFilePosition other = (SourceFilePosition) obj;
        return mSourceFile.equals(other.mSourceFile) && mSourcePosition.equals(other.mSourcePosition);
    }
}
