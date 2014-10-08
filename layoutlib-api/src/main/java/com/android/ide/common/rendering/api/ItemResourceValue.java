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
package com.android.ide.common.rendering.api;

import com.android.util.Pair;

/**
 * Represents each item in the android style resource.
 */
public class ItemResourceValue extends ResourceValue {

    private final boolean mIsFrameworkAttr;

    /**
     * @see #ItemResourceValue(String, boolean, String, boolean)
     */
    public ItemResourceValue(String name, boolean isFrameworkAttr, boolean isFramework) {
        this(name, isFrameworkAttr, null, isFramework);
    }

    /**
     * For {@code <item name="foo">bar</item>}, item in a style resource, the values of the
     * parameters will be as follows:
     *
     * @param attributeName foo
     * @param isFrameworkAttr is foo in framework namespace.
     * @param value bar
     * @param isFramework if the style is a framework file or project file.
     */
    public ItemResourceValue(String attributeName, boolean isFrameworkAttr, String value,
            boolean isFramework) {
        super(null, attributeName, value, isFramework);
        mIsFrameworkAttr = isFrameworkAttr;
    }

    @SuppressWarnings("deprecation")  // For Pair
    Pair<String, Boolean> getAttribute() {
        return Pair.of(getName(), mIsFrameworkAttr);
    }

    static ItemResourceValue fromResourceValue(ResourceValue res, boolean isFrameworkAttr) {
        assert res.getResourceType() == null;
        return new ItemResourceValue(res.getName(), isFrameworkAttr, res.getValue(),
                res.isFramework());
    }
}
