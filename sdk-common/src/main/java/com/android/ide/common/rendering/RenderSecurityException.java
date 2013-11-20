/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.ide.common.rendering;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

/** Exception thrown when custom view code makes an illegal code while rendering under layoutlib */
public class RenderSecurityException extends SecurityException {

    public RenderSecurityException(@NonNull String resource, @Nullable String context) {
        super(computeLabel(resource, context));
    }

    private static String computeLabel(@NonNull String resource, @Nullable String context) {
        StringBuilder sb = new StringBuilder(40);
        sb.append(resource);
        sb.append(" access not allowed during rendering");
        if (context != null) {
            sb.append(" (").append(context).append(")");
        }
        return sb.toString();
    }
}
