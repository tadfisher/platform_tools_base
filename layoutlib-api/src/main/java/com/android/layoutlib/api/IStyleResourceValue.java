/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.layoutlib.api;

import com.android.ide.common.rendering.api.StyleResourceValue;

/**
 * Represents an android style resources with a name and a list of children {@link IResourceValue}.
 * @deprecated Use {@link StyleResourceValue}.
 */
@Deprecated
public interface IStyleResourceValue extends IResourceValue {

    /**
     * Returns the parent style name or <code>null</code> if unknown.
     */
    String getParentStyle();

    /**
     * Find an item in the list by name
     * @param name the name of the resource
     *
     * @deprecated use {@link StyleResourceValue#findItem(String, boolean)}
     */
    @Deprecated
    IResourceValue findItem(String name);
}
