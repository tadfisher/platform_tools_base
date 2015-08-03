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
 */

package com.android.build.gradle.internal.pipeline;

/**
 * The type of inputs handled by a transform.
 * PROJECT: only the project files.
 *
 */
public enum StreamScope {
    /** Only the project content */
    PROJECT(0x0001),
    /** Only the sub-projects content (the local dependencies). */
    SUB_PROJECTS(0x0002),
    /** All the local projects' content) */
    ALL_PROJECTS(0x0003),
    /** Only the external libraries */
    EXTERNAL_LIBRARIES(0x0004),
    /** all but the main project */
    ALL_NON_PROJECT(0x0006),
    /** All the content, local and remote. */
    ALL(0x0007);

    private final byte bitMask;

    StreamScope(int bitMask) {
        this.bitMask = (byte)bitMask;
    }

    public boolean match(StreamScope other) {
        return (bitMask & other.bitMask) != 0;
    }
}
