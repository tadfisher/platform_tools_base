/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.build.gradle.integration.common.truth;

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;

import com.android.builder.model.SyncIssue;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;

import java.util.Collection;

public class CollectionIssueSubject extends Subject<CollectionIssueSubject, Collection<SyncIssue>> {

    public CollectionIssueSubject(FailureStrategy failureStrategy, Collection<SyncIssue> subject) {
        super(failureStrategy, subject);
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void hasOnly(int severity, int type) {
        Collection<SyncIssue> subject = getSubject();

        assertThat(subject).hasSize(1);

        SyncIssue issue = subject.iterator().next();
        assertThat(issue).isNotNull();
        assertThat(issue).hasSeverity(severity);
        assertThat(issue).hasType(type);
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void hasOnly(int severity, int type, String data) {
        Collection<SyncIssue> subject = getSubject();

        assertThat(subject).hasSize(1);

        SyncIssue issue = subject.iterator().next();
        assertThat(issue).isNotNull();
        assertThat(issue).hasSeverity(severity);
        assertThat(issue).hasType(type);
        assertThat(issue).hasData(data);
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void hasOnly(int severity, int type, String data, String message) {
        Collection<SyncIssue> subject = getSubject();

        assertThat(subject).hasSize(1);

        SyncIssue issue = subject.iterator().next();
        assertThat(issue).isNotNull();
        assertThat(issue).hasSeverity(severity);
        assertThat(issue).hasType(type);
        assertThat(issue).hasData(data);
        assertThat(issue).hasMessage(message);
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void has(int severity, int type) {
        Collection<SyncIssue> subject = getSubject();

        for (SyncIssue issue : subject) {
            if (severity == issue.getSeverity() &&
                    type == issue.getType()) {
                return;
            }
        }

        failWithRawMessage("'%s' does not contain <%s / %s>", getDisplaySubject(),
                severity, type);
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void has(int severity, int type, String data) {
        Collection<SyncIssue> subject = getSubject();

        for (SyncIssue issue : subject) {
            if (severity == issue.getSeverity() &&
                    type == issue.getType() &&
                    data.equals(issue.getData())) {
                return;
            }
        }

        failWithRawMessage("'%s' does not contain <%s / %s / %s>", getDisplaySubject(),
                severity, type, data);
    }
}
