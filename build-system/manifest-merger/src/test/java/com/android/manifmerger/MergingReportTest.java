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

package com.android.manifmerger;

import junit.framework.TestCase;

import org.mockito.Mock;

import java.util.logging.Logger;

/**
 * Tests for the {@link com.android.manifmerger.MergingReport} class
 */
public class MergingReportTest extends TestCase {

    @Mock
    Logger mLoggerMock;

    public void testJustError() {
        MergingReport mergingReport = new MergingReport.Builder()
                .addError("Something bad happened")
                .build();

        assertEquals(MergingReport.Result.ERROR, mergingReport.getResult());
    }

    public void testJustWarning() {
        MergingReport mergingReport = new MergingReport.Builder()
                .addWarning("Something weird happened")
                .build();

        assertEquals(MergingReport.Result.WARNING, mergingReport.getResult());
    }

    public void testJustInfo() {
        MergingReport mergingReport = new MergingReport.Builder()
                .addInfo("merging info")
                .build();

        assertEquals(MergingReport.Result.SUCCESS, mergingReport.getResult());
    }




}
