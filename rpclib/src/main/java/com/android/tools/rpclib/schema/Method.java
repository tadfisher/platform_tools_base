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
 *
 * THIS WILL BE REMOVED ONCE THE CODE GENERATOR IS INTEGRATED INTO THE BUILD.
 */
package com.android.tools.rpclib.schema;

import org.jetbrains.annotations.NotNull;
import com.android.tools.rpclib.binary.Decoder;
import com.android.tools.rpclib.binary.Encoder;
import java.io.IOException;

public enum Method {
    ID(0),
    Bool(1),
    Int8(2),
    Uint8(3),
    Int16(4),
    Uint16(5),
    Int32(6),
    Uint32(7),
    Int64(8),
    Uint64(9),
    Float32(10),
    Float64(11),
    String(12);

    private final int mValue;
    Method(int value) {
        mValue = value;
    }
    public int getValue() { return mValue; }

    public void encode(@NotNull Encoder e) throws IOException {
        e.int32(mValue);
    }

    public static Method decode(@NotNull Decoder d) throws IOException {
        int value = d.int32();
        switch (value) {
        case 0:
            return ID;
        case 1:
            return Bool;
        case 2:
            return Int8;
        case 3:
            return Uint8;
        case 4:
            return Int16;
        case 5:
            return Uint16;
        case 6:
            return Int32;
        case 7:
            return Uint32;
        case 8:
            return Int64;
        case 9:
            return Uint64;
        case 10:
            return Float32;
        case 11:
            return Float64;
        case 12:
            return String;
        }
        throw new IOException("Invalid value for Method");
    }
}
