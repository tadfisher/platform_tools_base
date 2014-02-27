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

import com.android.utils.PositionXmlParser;
import com.google.common.collect.ImmutableList;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests for the {@link com.android.manifmerger.MergingReport} class
 */
public class MergingReportTest extends TestCase {

    @Mock Logger mLoggerMock;
    @Mock Element mElement;
    @Mock XmlLoader.SourceLocation mSourceLocation;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }

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


    public void testJustInfoAndWarning() {
        MergingReport mergingReport = new MergingReport.Builder()
                .addInfo("merging info")
                .addWarning("Something weird happened")
                .build();

        assertEquals(MergingReport.Result.WARNING, mergingReport.getResult());
    }

    public void testJustInfoAndError() {
        MergingReport mergingReport = new MergingReport.Builder()
                .addInfo("merging info")
                .addError("something bad happened")
                .build();

        assertEquals(MergingReport.Result.ERROR, mergingReport.getResult());
    }

    public void testJustWarningAndError() {
        MergingReport mergingReport = new MergingReport.Builder()
                .addWarning("something weird happened")
                .addError("something bad happened")
                .build();

        assertEquals(MergingReport.Result.ERROR, mergingReport.getResult());
    }
    public void testAllTypes() {
        MergingReport mergingReport = new MergingReport.Builder()
                .addInfo("merging info")
                .addWarning("something weird happened")
                .addError("something bad happened")
                .build();

        assertEquals(MergingReport.Result.ERROR, mergingReport.getResult());
    }

    public void testLogging() {
        MergingReport mergingReport = new MergingReport.Builder()
                .addInfo("merging info")
                .addWarning("something weird happened")
                .addError("something bad happened")
                .build();

        mergingReport.log(mLoggerMock);
        Mockito.verify(mLoggerMock).log(Level.INFO, "merging info");
        Mockito.verify(mLoggerMock).log(Level.WARNING,"something weird happened");
        Mockito.verify(mLoggerMock).log(Level.SEVERE, "something bad happened");
        Mockito.verify(mLoggerMock).log(Level.INFO, ActionRecorder.HEADER);
        Mockito.verifyNoMoreInteractions(mLoggerMock);
    }

    public void testItermediaryMerges() {
        MergingReport mergingReport = new MergingReport.Builder()
                .addMergingStage("<first/>")
                .addMergingStage("<second/>")
                .addMergingStage("<third/>")
                .build();

        ImmutableList<String> intermediaryStages = mergingReport.getIntermediaryStages();
        assertEquals(3, intermediaryStages.size());
        assertEquals("<first/>", intermediaryStages.get(0));
        assertEquals("<second/>", intermediaryStages.get(1));
        assertEquals("<third/>", intermediaryStages.get(2));
    }

    public void testGetMergedDocument() {
        XmlDocument xmlDocument =
                new XmlDocument(new PositionXmlParser(), mSourceLocation, mElement);

        MergingReport mergingReport = new MergingReport.Builder()
                .setMergedDocument(xmlDocument)
                .build();

        assertTrue(mergingReport.getMergedDocument().isPresent());
        assertEquals(xmlDocument, mergingReport.getMergedDocument().get());
    }
}
