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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import com.android.annotations.concurrency.Immutable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains the result of 2 files merging.
 */
@Immutable
public class MergingReport {

    public void log(Logger logger) {
        for (Record record : mRecords) {
            switch(record.mType) {
                case Warning:
                    logger.log(Level.WARNING, record.mLog);
                    break;
                case Error:
                    logger.log(Level.SEVERE, record.mLog);
                    break;
                case Info:
                    logger.log(Level.INFO, record.mLog);
                default:
                    logger.log(Level.SEVERE, "Unhandled record type " + record.mType);
            }
        }
        mActionRecorder.log(logger);
    }

    public ImmutableList<String> getmIntermediaryStages() {
        return mIntermediaryStages;
    }


    public enum Result {
        SUCCESS,

        WARNING,

        ERROR
    }

    public Result getResult() {
        return mResult;
    }

    private static class Record {

        private Record(Type type, String mLog) {
            this.mType = type;
            this.mLog = mLog;
        }

        enum Type { Warning, Error, Info }

        private final Type mType;
        private final String mLog;
    }

    private final Optional<XmlDocument> mMergedDocument;
    private final Result mResult;
    // list of logging events, ordered by their recording time.
    private final ImmutableList<Record> mRecords;
    private final ImmutableList<String> mIntermediaryStages;
    private final ActionRecorder mActionRecorder;

    private MergingReport(Optional<XmlDocument> mergedDocument, Result result,
            ImmutableList<Record> records,
            ImmutableList<String> intermediaryStages, ActionRecorder actionRecorder) {
        mMergedDocument = mergedDocument;
        mResult = result;
        mRecords = records;
        mIntermediaryStages = intermediaryStages;
        mActionRecorder = actionRecorder;
    }

    public Optional<XmlDocument> getMergedDocument() {
        return mMergedDocument;
    }

    static class Builder {

        Optional<XmlDocument> mergedDocument = Optional.absent();
        ImmutableList.Builder<Record> mRecordBuilder = new ImmutableList.Builder<Record>();
        ImmutableList.Builder<String> mIntermediaryStages = new ImmutableList.Builder<String>();
        boolean hasWarnings = false;
        boolean hasErrors = false;
        ActionRecorder mActionRecorder = new ActionRecorder();

        void setMergedDocument(XmlDocument mergedDocument) {
            this.mergedDocument = Optional.of(mergedDocument);
        }

        void addWarning(String warning) {
            hasWarnings = true;
            mRecordBuilder.add(new Record(Record.Type.Warning, warning));
        }

        void addError(String error) {
            hasErrors = true;
            mRecordBuilder.add(new Record(Record.Type.Error, error));
        }

        void addMergingStage(String xml) {
            mIntermediaryStages.add(xml);
        }

        ActionRecorder getActionRecorder() {
            return mActionRecorder;
        }

        MergingReport build() {
            Result result = hasErrors
                    ? Result.ERROR
                    : hasWarnings
                            ? Result.WARNING
                            : Result.SUCCESS;

            return new MergingReport(
                    mergedDocument,
                    result,
                    mRecordBuilder.build(),
                    mIntermediaryStages.build(),
                    mActionRecorder);
        }
    }
}
