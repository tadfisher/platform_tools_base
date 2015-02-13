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

package com.android.builder.profile;

/**
 * Defines a type of processing.
 */
public enum ExecutionType {

    SOME_RANDOM_PROCESSING(1),
    BASEPLUGIN_PROJECT_CONFIGURE(2),
    BASEPLUGIN_PROJECT_BASE_EXTENSTION_CREATION(3),
    BASEPLUGIN_PROJECT_TASKS_CREATION(4),
    BASEPLUGIN_BUILD_FINISHED(5);

    // LAST ID = 5,
    // increment for each new execution type;

    int getId() {
        return id;
    }


    private final int id;
    ExecutionType(int id) {
        this.id = id;
    }

}
