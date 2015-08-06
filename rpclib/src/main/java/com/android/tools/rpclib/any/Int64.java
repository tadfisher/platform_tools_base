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
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.rpclib.any;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.BinaryClass;
import com.android.tools.rpclib.binary.BinaryID;
import com.android.tools.rpclib.binary.BinaryObject;
import com.android.tools.rpclib.binary.Decoder;
import com.android.tools.rpclib.binary.Encoder;
import com.android.tools.rpclib.binary.Namespace;

import java.io.IOException;

final class Int64 extends Box implements BinaryObject {
    //<<<Start:Java.ClassBody:1>>>
    long mValue;

    // Constructs a default-initialized {@link Int64}.
    public Int64() {}


    public long getValue() {
        return mValue;
    }

    public Int64 setValue(long v) {
        mValue = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    private static final byte[] IDBytes = {78, -60, 75, -85, -118, 16, -39, 7, -68, 30, 73, 113, 46, -5, 44, 102, -41, 38, 91, 106, };
    public static final BinaryID ID = new BinaryID(IDBytes);

    static {
        Namespace.register(ID, Klass.INSTANCE);
    }
    public static void register() {}
    //<<<End:Java.ClassBody:1>>>
    public enum Klass implements BinaryClass {
        //<<<Start:Java.KlassBody:2>>>
        INSTANCE;

        @Override @NotNull
        public BinaryID id() { return ID; }

        @Override @NotNull
        public BinaryObject create() { return new Int64(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            Int64 o = (Int64)obj;
            e.int64(o.mValue);
        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            Int64 o = (Int64)obj;
            o.mValue = d.int64();
        }
        //<<<End:Java.KlassBody:2>>>
    }
}