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

public final class Field implements BinaryObject {
    //<<<Start:Java.ClassBody:1>>>
    String mDeclared;
    Type mType;

    // Constructs a default-initialized {@link Field}.
    public Field() {}


    public String getDeclared() {
        return mDeclared;
    }

    public Field setDeclared(String v) {
        mDeclared = v;
        return this;
    }

    public Type getType() {
        return mType;
    }

    public Field setType(Type v) {
        mType = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    public static byte[] IDBytes = {-116, 84, 61, -104, -61, 126, 56, -88, -86, 86, -68, -124, 39, 73, 93, 66, -35, 33, 36, -8, };
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
        public BinaryObject create() { return new Field(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            Field o = (Field)obj;
            e.string(o.mDeclared);
            e.object(o.mType.unwrap());
        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            Field o = (Field)obj;
            o.mDeclared = d.string();
            o.mType = Type.wrap(d.object());
        }
        //<<<End:Java.KlassBody:2>>>
    }
}
