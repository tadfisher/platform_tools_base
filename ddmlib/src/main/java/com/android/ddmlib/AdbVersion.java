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
package com.android.ddmlib;

import com.android.annotations.NonNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdbVersion implements Comparable<AdbVersion> {
    public static final AdbVersion UNKNOWN = new AdbVersion(-1, -1, -1);

    /** Matches e.g. ".... 1.0.32" */
    private static final Pattern ADB_VERSION_PATTERN = Pattern.compile(
            "^.*(\\d+)\\.(\\d+)\\.(\\d+).*");

    private final int mMajor;
    private final int mMinor;
    private final int mMicro;

    private AdbVersion(int major, int minor, int micro) {
        mMajor = major;
        mMinor = minor;
        mMicro = micro;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%1$d.%2$d.%3$d", mMajor, mMinor, mMicro);
    }

    @Override
    public int compareTo(AdbVersion o) {
        if (mMajor != o.mMajor) {
            return mMajor - o.mMajor;
        }

        if (mMinor != o.mMinor) {
            return mMinor - o.mMinor;
        }

        return mMicro - o.mMicro;
    }

    @NonNull
    public static AdbVersion parseFrom(@NonNull String input) {
        Matcher matcher = ADB_VERSION_PATTERN.matcher(input);
        if (matcher.matches()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int micro = Integer.parseInt(matcher.group(3));
            return new AdbVersion(major, minor, micro);
        } else {
            return UNKNOWN;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AdbVersion version = (AdbVersion) o;

        if (mMajor != version.mMajor) {
            return false;
        }
        if (mMinor != version.mMinor) {
            return false;
        }
        return mMicro == version.mMicro;

    }

    @Override
    public int hashCode() {
        int result = mMajor;
        result = 31 * result + mMinor;
        result = 31 * result + mMicro;
        return result;
    }
}
