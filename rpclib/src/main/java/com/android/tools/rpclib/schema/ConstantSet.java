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

public final class ConstantSet implements BinaryObject {
    //<<<Start:Java.ClassBody:1>>>
    Type mType;
    Constant[] mEntries;

    // Constructs a default-initialized {@link ConstantSet}.
    public ConstantSet() {}


    public Type getType() {
        return mType;
    }

    public ConstantSet setType(Type v) {
        mType = v;
        return this;
    }

    public Constant[] getEntries() {
        return mEntries;
    }

    public ConstantSet setEntries(Constant[] v) {
        mEntries = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    public static byte[] IDBytes = {40, -113, 108, -120, 49, -47, 4, 82, -73, 90, 37, -125, 1, 78, -102, 124, 83, 3, 50, -98, };
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
        public BinaryObject create() { return new ConstantSet(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            ConstantSet o = (ConstantSet)obj;
            e.object(o.mType.unwrap());
            e.uint32(o.mEntries.length);
            for (int i = 0; i < o.mEntries.length; i++) {
                e.value(o.mEntries[i]);
            }
        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            ConstantSet o = (ConstantSet)obj;
            o.mType = Type.wrap(d.object());
            o.mEntries = new Constant[d.uint32()];
            for (int i = 0; i <o.mEntries.length; i++) {
                o.mEntries[i] = new Constant();
                d.value(o.mEntries[i]);
            }
        }
        //<<<End:Java.KlassBody:2>>>
    }
}
