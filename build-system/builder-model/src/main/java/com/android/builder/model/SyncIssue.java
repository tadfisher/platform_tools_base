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

package com.android.builder.model;

import com.android.annotations.NonNull;

/**
 * Class representing a sync issue.
 * The goal is to make these issues not fail the sync but instead report them at the end
 * of a successful sync.
 */
public interface SyncIssue {
    public static final int SEVERITY_WARNING = 1;
    public static final int SEVERIRT_ERROR = 2;

    public static final int TYPE_UNKNOWN                  = 0;
    public static final int TYPE_UNRESOLVED_DEPENDENCY    = 1;
    public static final int TYPE_DEPENDENCY_IS_APK        = 2;
    public static final int TYPE_DEPENDENCY_IS_APKLIB     = 3;
    public static final int TYPE_MAX                      = 4; // increment when adding new types.

    int getSeverity();

    int getType();

    @NonNull
    String getData();

    @NonNull
    String getMessage();
}
