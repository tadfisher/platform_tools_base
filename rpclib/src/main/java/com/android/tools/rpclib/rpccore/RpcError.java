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
package com.android.tools.rpclib.rpccore;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.BinaryClass;
import com.android.tools.rpclib.binary.BinaryID;
import com.android.tools.rpclib.binary.BinaryObject;
import com.android.tools.rpclib.binary.Decoder;
import com.android.tools.rpclib.binary.Encoder;
import com.android.tools.rpclib.binary.Namespace;

import java.io.IOException;

public final class RpcError implements BinaryObject {
    //<<<Start:Java.ClassBody:1>>>
    String mMessage;

    // Constructs a default-initialized {@link RpcError}.
    public RpcError() {}


    public String getMessage() {
        return mMessage;
    }

    public RpcError setMessage(String v) {
        mMessage = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }

    public static byte[] IDBytes = {-2, 118, -32, 58, 68, -93, -64, 56, -37, 98, 46, -29, -13, -28, -7, -121, -7, 25, -66, -3, };
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
        public BinaryObject create() { return new RpcError(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            RpcError o = (RpcError)obj;
            e.string(o.mMessage);
        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            RpcError o = (RpcError)obj;
            o.mMessage = d.string();
        }
        //<<<End:Java.KlassBody:2>>>
    }
}
