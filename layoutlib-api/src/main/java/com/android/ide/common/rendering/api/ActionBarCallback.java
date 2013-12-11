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

import java.util.Collections;
import java.util.List;

public class ActionBarCallback {

    public static final int NAVIGATION_MODE_STANDARD = 0;
    public static final int NAVIGATION_MODE_LIST = 1;
    public static final int NAVIGATION_MODE_TABS = 2;

    public enum HomeButtonStyle{
        NONE,SHOW_HOME_AS_UP
    }

    /**
     * Returns a list of resource ids for menus to add to the action bar. Layoutlib then calls
     * {@link #resolveResourceId(int)} with the menu ids passed and then gets the parser by
     * calling {@link #getParser(ResourceValue)}.
     *
     * @return the list of menu ids.
     *
     * @since API 11
     */
    public List<String> getMenuIdNames() {
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns if the Action Bar should be split for narrow screens.
     * @since API 11
     */
    public boolean getSplitActionBarWhenNarrow() {
        return false;
    }

    /**
     * Returns which navigation mode the action bar should use.
     *
     * @since API 11
     */
    public int getNavigationMode() {
        return NAVIGATION_MODE_STANDARD;
    }

    /**
     * Returns the subtitle to be used with the action bar.
     */
    public String getSubTitle() {
        return null;
    }

    /**
     * Returns the type of navigation for home button to be used in the action bar.
     * <p/>
     * For example, for showHomeAsUp, an arrow is shown alongside the "home" icon.
     */
    public HomeButtonStyle getHomeButtonStyle() {
        return HomeButtonStyle.NONE;
    }
}
