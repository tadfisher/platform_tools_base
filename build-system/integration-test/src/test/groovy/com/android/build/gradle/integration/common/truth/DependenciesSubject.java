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

import com.android.annotations.NonNull;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.Dependencies;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;


public class DependenciesSubject extends Subject<DependenciesSubject, Dependencies> {

    public enum LibraryProperty {
        PATH, RESOLVED_COORD, REQUESTED_COORD
    }

    static class Factory extends
            SubjectFactory<DependenciesSubject, Dependencies> {
        @NonNull
        public static Factory get() {
            return new Factory();
        }

        private Factory() {}

        @Override
        public DependenciesSubject getSubject(
                @NonNull FailureStrategy failureStrategy,
                @NonNull Dependencies subject) {
            return new DependenciesSubject(failureStrategy, subject);
        }
    }

    public DependenciesSubject(
            @NonNull FailureStrategy failureStrategy,
            @NonNull Dependencies subject) {
        super(failureStrategy, subject);
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void hasOneLibrary() {
        hasOneLibrary(Collections.<String, String>emptyMap());
    }

    /**
     * Checks that the dependencies has a single library, and optionally checks one or more values
     * of the library through a map of (property, value).
     *
     * Through Groovy, it can look like this:
     *
     * assertThat(dependencies).hasOneLibrary(Path: ":library")
     *
     * or
     *
     * assertThat(dependencies).hasOneLibrary(Resolved_Coord: "group:artifact:version")
     *
     * @param filters the filters.
     */
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void hasOneLibrary(@NonNull Map<String, String> filters) {
        Collection<AndroidLibrary> libs = getSubject().getLibraries();

        assertThat(libs).hasSize(1);

        AndroidLibrary lib = libs.iterator().next();

        for (Map.Entry<String, String> filter : filters.entrySet()) {
            LibraryProperty property = LibraryProperty.valueOf(filter.getKey().toUpperCase(
                    Locale.getDefault()));
            if (property == null) {
                fail("Unrecognized library property:" + filter.getKey());
            }

            //noinspection ConstantConditions
            switch (property) {
                case PATH:
                    assertThat(lib.getProject()).isEqualTo(filter.getValue());
                    break;
                case RESOLVED_COORD:
                    assertThat(lib.getResolvedCoordinates()).isNotNull();
                    //noinspection ConstantConditions
                    assertThat(lib.getResolvedCoordinates().toString()).isEqualTo(filter.getValue());
                    break;
                case REQUESTED_COORD:
                    assertThat(lib.getRequestedCoordinates()).isNotNull();
                    //noinspection ConstantConditions
                    assertThat(lib.getRequestedCoordinates().toString()).isEqualTo(filter.getValue());
                    break;
                default:
                    fail("Unsupported LibraryProperty value: " + filter.getKey());
            }
        }
    }
}
