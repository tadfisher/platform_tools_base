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

import com.android.tools.rpclib.binary.BinaryClass;
import com.android.tools.rpclib.binary.BinaryID;
import com.android.tools.rpclib.binary.BinaryObject;
import com.android.tools.rpclib.binary.Decoder;
import com.android.tools.rpclib.binary.Encoder;
import com.android.tools.rpclib.binary.Namespace;

import java.io.IOException;

public final class Pointer extends Type {
    @Override
    public void encodeValue(@NotNull Encoder e, Object value) throws IOException {
        // TODO: implement variant encode
    }

    @Override
    public Object decodeValue(@NotNull Decoder d) throws IOException {
        // TODO: implement variant decode
        return null;
    }

    //<<<Start:Java.ClassBody:1>>>
    Type mType;

    // Constructs a default-initialized {@link Pointer}.
    public Pointer() {}


    public Type getType() {
        return mType;
    }

    public Pointer setType(Type v) {
        mType = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    public static byte[] IDBytes = {19, -87, -45, 17, -95, -115, -67, 91, 24, -25, -101, 67, 59, -33, -59, 73, -56, -91, 53, -49, };
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
        public BinaryObject create() { return new Pointer(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            Pointer o = (Pointer)obj;
            e.object(o.mType.unwrap());
        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            Pointer o = (Pointer)obj;
            o.mType = Type.wrap(d.object());
        }
        //<<<End:Java.KlassBody:2>>>
    }
}
