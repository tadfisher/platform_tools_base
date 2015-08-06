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

public final class Slice extends Type {
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
    String mAlias;
    Type mValueType;

    // Constructs a default-initialized {@link Slice}.
    public Slice() {}


    public String getAlias() {
        return mAlias;
    }

    public Slice setAlias(String v) {
        mAlias = v;
        return this;
    }

    public Type getValueType() {
        return mValueType;
    }

    public Slice setValueType(Type v) {
        mValueType = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    public static byte[] IDBytes = {59, 117, -18, 89, 24, 26, 77, 116, 80, -14, 80, 127, -7, 79, 121, -103, 56, 88, -106, -46, };
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
        public BinaryObject create() { return new Slice(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            Slice o = (Slice)obj;
            e.string(o.mAlias);
            e.object(o.mValueType.unwrap());
        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            Slice o = (Slice)obj;
            o.mAlias = d.string();
            o.mValueType = Type.wrap(d.object());
        }
        //<<<End:Java.KlassBody:2>>>
    }
}
