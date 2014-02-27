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
 *
 * TODO(jedo): more work necessary, this is pretty raw as it stands.
 */
@Immutable
public class MergingReport {

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

    /**
     * dumps all logging records to a logger.
     */
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

    /**
     * Return the resulting merged document.
     */
    public Optional<XmlDocument> getMergedDocument() {
        return mMergedDocument;
    }

    /**
     * Returns all the merging intermediary stages if
     * {@link com.android.manifmerger.ManifestMerger2.Builder.Features#KEEP_INTERMEDIARY_STAGES)}
     * is set.
     */
    public ImmutableList<String> getIntermediaryStages() {
        return mIntermediaryStages;
    }

    /**
     * Overall result of the merging process.
     */
    public enum Result {
        SUCCESS,

        WARNING,

        ERROR
    }

    public Result getResult() {
        return mResult;
    }

    /**
     * Log record. This is used to give users some information about what is happening and
     * what might have gone wrong.
     *
     * TODO(jedo): need to enhance to add SourceLocation, and make this more machine readable.
     */
    private static class Record {

        private Record(Type type, String mLog) {
            this.mType = type;
            this.mLog = mLog;
        }

        enum Type { Warning, Error, Info }

        private final Type mType;
        private final String mLog;
    }

    /**
     * This builder is used to accumulate logging, action recording and intermediary results as
     * well as final result of the merging activity.
     *
     * Once the merging is finished, the {@see #build()} is called to return an immutable version
     * of itself with all the logging, action recordings and xml files obtainable.
     *
     */
    static class Builder {

        Optional<XmlDocument> mMergedDocument = Optional.absent();
        ImmutableList.Builder<Record> mRecordBuilder = new ImmutableList.Builder<Record>();
        ImmutableList.Builder<String> mIntermediaryStages = new ImmutableList.Builder<String>();
        boolean mHasWarnings = false;
        boolean mHasErrors = false;
        ActionRecorder mActionRecorder = new ActionRecorder();

        Builder setMergedDocument(XmlDocument mergedDocument) {
            mMergedDocument = Optional.of(mergedDocument);
            return this;
        }

        Builder addInfo(String info) {
            mRecordBuilder.add(new Record(Record.Type.Info, info));
            return this;
        }

        Builder addWarning(String warning) {
            mHasWarnings = true;
            mRecordBuilder.add(new Record(Record.Type.Warning, warning));
            return this;
        }

        Builder addError(String error) {
            mHasErrors = true;
            mRecordBuilder.add(new Record(Record.Type.Error, error));
            return this;
        }

        Builder addMergingStage(String xml) {
            mIntermediaryStages.add(xml);
            return this;
        }

        ActionRecorder getActionRecorder() {
            return mActionRecorder;
        }

        MergingReport build() {
            Result result = mHasErrors
                    ? Result.ERROR
                    : mHasWarnings
                            ? Result.WARNING
                            : Result.SUCCESS;

            return new MergingReport(
                    mMergedDocument,
                    result,
                    mRecordBuilder.build(),
                    mIntermediaryStages.build(),
                    mActionRecorder);
        }
    }
}
