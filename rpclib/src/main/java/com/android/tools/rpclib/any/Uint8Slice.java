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
package com.android.tools.rpclib.any;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.BinaryClass;
import com.android.tools.rpclib.binary.BinaryID;
import com.android.tools.rpclib.binary.BinaryObject;
import com.android.tools.rpclib.binary.Decoder;
import com.android.tools.rpclib.binary.Encoder;
import com.android.tools.rpclib.binary.Namespace;

import java.io.IOException;

final class Uint8Slice extends Box implements BinaryObject {
    @Override
    public Object unwrap() {
        return getValue();
    }

    //<<<Start:Java.ClassBody:1>>>
    byte[] mValue;

    // Constructs a default-initialized {@link Uint8Slice}.
    public Uint8Slice() {}


    public byte[] getValue() {
        return mValue;
    }

    public Uint8Slice setValue(byte[] v) {
        mValue = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    public static byte[] IDBytes = {56, -97, -48, -78, 31, -107, 4, 70, -96, -112, 5, 5, 98, -21, -67, -120, 83, 69, 28, -112, };
    public static BinaryID ID = new BinaryID(IDBytes);

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
        public BinaryObject create() { return new Uint8Slice(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            Uint8Slice o = (Uint8Slice)obj;
            e.uint32(o.mValue.length);
            e.write(o.mValue, o.mValue.length);

        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            Uint8Slice o = (Uint8Slice)obj;
            o.mValue = new byte[d.uint32()];
            d.read(o.mValue, o.mValue.length);

        }
        //<<<End:Java.KlassBody:2>>>
    }
}
