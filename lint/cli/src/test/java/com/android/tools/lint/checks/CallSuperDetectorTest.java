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
package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

public class CallSuperDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new CallSuperDetector();
    }

    public void testViewOnDetachedFromWindow() throws Exception {
        assertEquals(""
                + "src/test/pkg/ViewDetachedFromWindow.java:7: Warning: Overriding method should call super.onDetachedFromWindow [MissingSuperCall]\n"
                + "        protected void onDetachedFromWindow() {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ViewDetachedFromWindow.java:26: Warning: Overriding method should call super.onDetachedFromWindow [MissingSuperCall]\n"
                + "        protected void onDetachedFromWindow() {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ViewDetachedFromWindow.java.txt=>" +
                        "src/test/pkg/ViewDetachedFromWindow.java"));
    }

    public void testViewOnCancelPendingInputEvents() throws Exception {
        assertEquals(""
                + "src/test/pkg/ViewOnCancelPendingInputEvents.java.txt:7: Warning: Overriding method should call super.onCancelPendingInputEvents [MissingSuperCall]\n"
                + "        protected void onCancelPendingInputEvents() {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ViewOnCancelPendingInputEvents.java.txt:26: Warning: Overriding method should call super.onCancelPendingInputEvents [MissingSuperCall]\n"
                + "        protected void onCancelPendingInputEvents() {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ViewOnCancelPendingInputEvents.java.txt=>" +
                        "src/test/pkg/ViewOnCancelPendingInputEvents.java.txt"));
    }

    public void testActivityOnCreate() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityOnCreate.java.txt:8: Warning: Overriding method should call super.onCreate [MissingSuperCall]\n"
                + "        protected void onCreate(Bundle savedInstanceState) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActivityOnCreate.java.txt:27: Warning: Overriding method should call super.onCreate [MissingSuperCall]\n"
                + "        protected void onCreate(Bundle savedInstanceState) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ActivityOnCreate.java.txt=>" +
                        "src/test/pkg/ActivityOnCreate.java.txt"));
    }

    public void testActivityOnStart() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityOnStart.java.txt:7: Warning: Overriding method should call super.onStart [MissingSuperCall]\n"
                + "        protected void onStart() {\n"
                + "                       ~~~~~~~~~\n"
                + "src/test/pkg/ActivityOnStart.java.txt:26: Warning: Overriding method should call super.onStart [MissingSuperCall]\n"
                + "        protected void onStart() {\n"
                + "                       ~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ActivityOnStart.java.txt=>" +
                        "src/test/pkg/ActivityOnStart.java.txt"));
    }

    public void testActivityonRestart() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityOnStart.java.txt:7: Warning: Overriding method should call super.onRestart [MissingSuperCall]\n"
                + "        protected void onRestart() {\n"
                + "                       ~~~~~~~~~~~\n"
                + "src/test/pkg/ActivityOnStart.java.txt:26: Warning: Overriding method should call super.onRestart [MissingSuperCall]\n"
                + "        protected void onRestart() {\n"
                + "                       ~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ActivityOnRestart.java.txt=>" +
                        "src/test/pkg/ActivityOnRestart.java.txt"));
    }

    public void testActivityOnResume() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityOnResume.java.txt:7: Warning: Overriding method should call super.onResume [MissingSuperCall]\n"
                + "        protected void onResume() {\n"
                + "                       ~~~~~~~~~~\n"
                + "src/test/pkg/ActivityOnResume.java.txt:26: Warning: Overriding method should call super.onResume [MissingSuperCall]\n"
                + "        protected void onResume() {\n"
                + "                       ~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ActivityOnResume.java.txt=>" +
                        "src/test/pkg/ActivityOnResume.java.txt"));
    }

    public void testActivityOnPostResume() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityOnPostResume.java.txt:7: Warning: Overriding method should call super.onPostResume [MissingSuperCall]\n"
                + "        protected void onPostResume() {\n"
                + "                       ~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActivityOnPostResume.java.txt:26: Warning: Overriding method should call super.onPostResume [MissingSuperCall]\n"
                + "        protected void onPostResume() {\n"
                + "                       ~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ActivityOnPostResume.java.txt=>" +
                        "src/test/pkg/ActivityOnPostResume.java.txt"));
    }

    public void testActivityOnPause() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityOnPause.java.txt:7: Warning: Overriding method should call super.onPause [MissingSuperCall]\n"
                + "        protected void onPause() {\n"
                + "                       ~~~~~~~~~\n"
                + "src/test/pkg/ActivityOnPause.java.txt:26: Warning: Overriding method should call super.onPause [MissingSuperCall]\n"
                + "        protected void onPause() {\n"
                + "                       ~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ActivityOnPause.java.txt=>" +
                        "src/test/pkg/ActivityOnPause.java.txt"));
    }

    public void testActivityOnStop() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityOnStop.java.txt:7: Warning: Overriding method should call super.onStop [MissingSuperCall]\n"
                + "        protected void onStop() {\n"
                + "                       ~~~~~~~~\n"
                + "src/test/pkg/ActivityOnStop.java.txt:26: Warning: Overriding method should call super.onStop [MissingSuperCall]\n"
                + "        protected void onStop() {\n"
                + "                       ~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ActivityOnStop.java.txt=>" +
                        "src/test/pkg/ActivityOnStop.java.txt"));
    }

    public void testActivityOnPostCreate() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityOnPostCreate.java.txt:8: Warning: Overriding method should call super.onPostCreate [MissingSuperCall]\n"
                + "        protected void onPostCreate(Bundle savedInstanceState) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActivityOnPostCreate.java.txt:27: Warning: Overriding method should call super.onPostCreate [MissingSuperCall]\n"
                + "        protected void onPostCreate(Bundle savedInstanceState) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ActivityOnPostCreate.java.txt=>" +
                        "src/test/pkg/ActivityOnPostCreate.java.txt"));
    }

    public void testActivityOnDestroy() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityOnDestroy.java.txt:7: Warning: Overriding method should call super.onDestroy [MissingSuperCall]\n"
                + "        protected void onDestroy() {\n"
                + "                       ~~~~~~~~~~~\n"
                + "src/test/pkg/ActivityOnDestroy.java.txt:26: Warning: Overriding method should call super.onDestroy [MissingSuperCall]\n"
                + "        protected void onDestroy() {\n"
                + "                       ~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ActivityOnDestroy.java.txt=>" +
                        "src/test/pkg/ActivityOnDestroy.java.txt"));
    }

    public void testActivityOnConfigurationChanged() throws Exception {
        assertEquals(""
                + "src/test/pkg/ActivityOnConfigurationChanged.java.txt:8: Warning: Overriding method should call super.onConfigurationChanged [MissingSuperCall]\n"
                + "        protected void onConfigurationChanged(Configuration configuration) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/ActivityOnConfigurationChanged.java.txt:27: Warning: Overriding method should call super.onConfigurationChanged [MissingSuperCall]\n"
                + "        protected void onConfigurationChanged(Configuration configuration) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/ActivityOnConfigurationChanged.java.txt=>" +
                        "src/test/pkg/ActivityOnConfigurationChanged.java.txt"));
    }

    public void testFragmentOnAttach() throws Exception {
        assertEquals(""
                + "src/test/pkg/FragmentOnAttach.java.txt:8: Warning: Overriding method should call super.onAttach [MissingSuperCall]\n"
                + "        protected void onAttach(Activity activity) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/FragmentOnAttach.java.txt:27: Warning: Overriding method should call super.onAttach [MissingSuperCall]\n"
                + "        protected void onAttach(Activity activity) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/FragmentOnAttach.java.txt=>" +
                        "src/test/pkg/FragmentOnAttach.java.txt"));
    }

    public void testFragmentOnDetach() throws Exception {
        assertEquals(""
                + "src/test/pkg/FragmentOnDetach.java.txt:7: Warning: Overriding method should call super.onDetach [MissingSuperCall]\n"
                + "        protected void onDetach() {\n"
                + "                       ~~~~~~~~~~\n"
                + "src/test/pkg/FragmentOnDetach.java.txt:26: Warning: Overriding method should call super.onDetach [MissingSuperCall]\n"
                + "        protected void onDetach() {\n"
                + "                       ~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/FragmentOnDetach.java.txt=>" +
                        "src/test/pkg/FragmentOnDetach.java.txt"));
    }

    public void testFragmentOnViewStateRestored() throws Exception {
        assertEquals(""
                + "src/test/pkg/FragmentOnViewStateRestored.java.txt:8: Warning: Overriding method should call super.onViewStateRestored [MissingSuperCall]\n"
                + "        protected void onViewStateRestored(Bundle savedInstanceState) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/FragmentOnViewStateRestored.java.txt:27: Warning: Overriding method should call super.onViewStateRestored [MissingSuperCall]\n"
                + "        protected void onViewStateRestored(Bundle savedInstanceState) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/FragmentOnViewStateRestored.java.txt=>" +
                        "src/test/pkg/FragmentOnViewStateRestored.java.txt"));
    }

    public void testFragmentOnActivityCreated() throws Exception {
        assertEquals(""
                + "src/test/pkg/FragmentOnActivityCreated.java.txt:8: Warning: Overriding method should call super.onActivityCreated [MissingSuperCall]\n"
                + "        protected void onActivityCreated(Bundle savedInstanceState) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/FragmentOnActivityCreated.java.txt:27: Warning: Overriding method should call super.onActivityCreated [MissingSuperCall]\n"
                + "        protected void onActivityCreated(Bundle savedInstanceState) {\n"
                + "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/FragmentOnActivityCreated.java.txt=>" +
                        "src/test/pkg/FragmentOnActivityCreated.java.txt"));
    }

    public void testFragmentOnDestroyView() throws Exception {
        assertEquals(""
                + "src/test/pkg/FragmentOnDestroyView.java.txt:7: Warning: Overriding method should call super.onDestroyView [MissingSuperCall]\n"
                + "        protected void onDestroyView() {\n"
                + "                       ~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/FragmentOnDestroyView.java.txt:26: Warning: Overriding method should call super.onDestroyView [MissingSuperCall]\n"
                + "        protected void onDestroyView() {\n"
                + "                       ~~~~~~~~~~~~~~~\n"
                + "0 errors, 2 warnings\n",

                lintProject("src/test/pkg/FragmentOnDestroyView.java.txt=>" +
                        "src/test/pkg/FragmentOnDestroyView.java.txt"));
    }

}
