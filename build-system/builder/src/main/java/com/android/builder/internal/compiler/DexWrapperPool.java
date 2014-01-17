/*
 * Copyright (C) 2014 The Android Open Source Project
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
 */

package com.android.builder.internal.compiler;

import com.android.annotations.NonNull;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.util.List;

/**
 * A Pool of {@link com.android.builder.internal.compiler.DexWrapper}.
 *
 * This loads dx.jar into a {@link com.android.builder.internal.compiler.DexWrapper}
 * on demand. Once the wrapper object is used, it is put back in the pool
 * for other threads to use.
 *
 * The is because dx is not reentrant and cannot be used in parallel to
 * do bytecode conversion.
 *
 * It is currently unbounded because the threads that are acquiring wrappers
 * are bounded themselves in numbers.
 *
 * Hopefully this is temporary and we can improve dx to be reentrant so that
 * we don't have to load dx.jar multiple times.
 *
 */
public class DexWrapperPool {

    private final Multimap<File, DexWrapper> sMap = ArrayListMultimap.create();
    private int count = 0;

    @NonNull
    public DexWrapper acquire(@NonNull File dxJar) {
        DexWrapper wrapper = null;

        // look for an available wrapper to use
        synchronized (sMap) {
            List<DexWrapper> wrappers = (List<DexWrapper>) sMap.get(dxJar);
            if (!wrappers.isEmpty()) {
                // use the last item
                wrapper = wrappers.remove(wrappers.size() - 1);
            }
        }

        // no available wrapper, create one.
        if (wrapper == null) {
            System.out.println("NEW DEX! " + (++count));
            wrapper = new DexWrapper();
            wrapper.loadDex(dxJar);
        }
        return wrapper;
    }

    public void release(@NonNull DexWrapper wrapper, @NonNull File dxJar) {
        synchronized (sMap) {
            sMap.put(dxJar, wrapper);
        }
    }
}
