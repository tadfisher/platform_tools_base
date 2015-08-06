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

public final class Factory {
    public static void register() {
        //<<<Start:Java.FactoryBody:2>>>
        Array.register();
        Field.register();
        SchemaClass.register();
        Constant.register();
        ConstantSet.register();
        Interface.register();
        Map.register();
        Pointer.register();
        Primitive.register();
        Slice.register();
        Struct.register();
        //<<<End:Java.FactoryBody:2>>>
    }
}
