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

package com.android.build.gradle.ndk.internal;

import com.google.common.collect.Lists;

import org.gradle.nativebinaries.platform.Platform;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Compiler flags to configure STL.
 */
public class StlNativeToolSpecification extends AbstractNativeToolSpecification {
    private NdkBuilder ndkBuilder;
    private String stl;
    private Platform platform;

    StlNativeToolSpecification(NdkBuilder ndkBuilder, String stl, Platform platform) {
        this.ndkBuilder = ndkBuilder;
        this.stl = stl;
        this.platform = platform;
    }


    @Override
    public Iterable<String> getCFlags() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<String> getCppFlags() {

        List<String> cppFlags = Lists.newArrayList();

        List<String> includeDirs = Lists.newArrayList();
        if (stl.equals("system")) {
            includeDirs.add("system/include");
        } else if (stl.equals("stlport_static") || stl.equals("stlport_shared")) {
            includeDirs.add("stlport/stlport");
            includeDirs.add("gabi++/include");
        } else if (stl.equals("gnustl_static") || stl.equals("gnustl_shared")) {
            includeDirs.add("gnu-libstdc++/4.8/include");
            includeDirs.add("gnu-libstdc++/4.8/libs/armeabi/include");
            includeDirs.add("gnu-libstdc++/4.8/include/backward");
        } else if (stl.equals("gabi++_static") || stl.equals("gabi++_shared")) {
            includeDirs.add("gabi++/include");
        } else if (stl.equals("c++_static") || stl.equals("c++_shared")) {
            includeDirs.add("llvm-libc++/libcxx/include");
            includeDirs.add("gabi++/include");
            includeDirs.add("../android/support/include");
            cppFlags.add("-std=c++11");
        }

        for (String dir : includeDirs) {
            cppFlags.add("-I" + new File(getStlBaseDirectory(), dir).toString());
        }
        return cppFlags;
    }

    @Override
    public Iterable<String> getLdFlags() {
        if (stl.equals("system")) {
            return Collections.emptyList();
        }
        List<String> flags = Lists.newArrayList();
        flags.add(getStlLib().toString());
        return flags;
    }

    public File getStlBaseDirectory() {
        return new File(ndkBuilder.getNdkDirectory(), "sources/cxx-stl/");
    }

    public File getStlLib() {
        String stlLib;
        if (stl.equals("stlport_static") || stl.equals("stlport_shared")) {
            stlLib = "stlport";
        } else if (stl.equals("gnustl_static") || stl.equals("gnustl_shared")) {
            stlLib = "gnu-libstdc++/4.8";
        } else if (stl.equals("gabi++_static") || stl.equals("gabi++_shared")) {
            stlLib = "gabi++";
        } else if (stl.equals("c++_static") || stl.equals("c++_shared")) {
            stlLib = "llvm-libc++";
        } else {
            stlLib = "";
        }
        return new File(getStlBaseDirectory(), stlLib + "/libs/" + platform.getName() + "/lib" + stl + (stl.endsWith("_shared") ? ".so" : ".a"));
    }
}
