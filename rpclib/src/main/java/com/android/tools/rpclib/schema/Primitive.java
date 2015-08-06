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

public final class Primitive extends Type {
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
    String mName;
    Method mMethod;

    // Constructs a default-initialized {@link Primitive}.
    public Primitive() {}


    public String getName() {
        return mName;
    }

    public Primitive setName(String v) {
        mName = v;
        return this;
    }

    public Method getMethod() {
        return mMethod;
    }

    public Primitive setMethod(Method v) {
        mMethod = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    public static byte[] IDBytes = {69, 77, -98, 125, -26, 79, 80, 116, -107, 19, 96, -67, -22, 0, 113, -109, -98, 62, -62, 78, };
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
        public BinaryObject create() { return new Primitive(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            Primitive o = (Primitive)obj;
            e.string(o.mName);
            o.mMethod.encode(e);
        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            Primitive o = (Primitive)obj;
            o.mName = d.string();
            o.mMethod = Method.decode(d);
        }
        //<<<End:Java.KlassBody:2>>>
    }
}
