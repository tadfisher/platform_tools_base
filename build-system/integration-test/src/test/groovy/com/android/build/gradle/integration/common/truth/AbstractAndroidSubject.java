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

import com.android.annotations.NonNull;
import com.android.ide.common.process.ProcessException;
import com.google.common.truth.FailureStrategy;

import java.io.File;
import java.io.IOException;
<<<<<<< HEAD
import java.util.zip.ZipFile;

/**
 * Base Truth support for android archives (aar and apk)
 */
public abstract class AbstractAndroidSubject<T extends AbstractZipSubject<T>> extends AbstractZipSubject<T> {
=======
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Truth support for aar files.
 */
public class AbstractAndroidSubject extends AbstractZipSubject<AbstractAndroidSubject> {
>>>>>>> a35fd7c... Support for provided aars.

    public AbstractAndroidSubject(FailureStrategy failureStrategy, File subject) {
        super(failureStrategy, subject);
    }

<<<<<<< HEAD
    /**
     * Returns true if the provided class is present in the file.
     * @param expectedClassName the class name in the format Lpkg1/pk2/Name;
     */
    protected abstract boolean checkForClass(
            @NonNull String expectedClassName)
            throws ProcessException, IOException;

=======
>>>>>>> a35fd7c... Support for provided aars.
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void containsClass(String className) throws IOException, ProcessException {
        if (!checkForClass(className)) {
            failWithRawMessage("'%s' does not contain '%s'", getDisplaySubject(), className);
        }
    }

    public void doesNotContainClass(String className) throws IOException, ProcessException {
        if (checkForClass(className)) {
            failWithRawMessage("'%s' unexpectedly contains '%s'", getDisplaySubject(), className);
        }
    }

<<<<<<< HEAD
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void containsResource(String name) throws IOException, ProcessException {
        if (!checkForResource(name)) {
            failWithRawMessage("'%s' does not contain resource '%s'", getDisplaySubject(), name);
        }
    }

    public void doesNotContainResource(String name) throws IOException, ProcessException {
        if (checkForResource(name)) {
            failWithRawMessage("'%s' unexpectedly contains resource '%s'", getDisplaySubject(), name);
        }
    }

=======
>>>>>>> a35fd7c... Support for provided aars.
    @Override
    protected String getDisplaySubject() {
        String name = (internalCustomName() == null) ? "" : "\"" + internalCustomName() + "\" ";
        return name + "<" + getSubject().getName() + ">";
    }

<<<<<<< HEAD
    private boolean checkForResource(String name) throws IOException {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(getSubject());
            return zipFile.getEntry("res/" + name) != null;
        } finally {
            if (zipFile != null) {
                zipFile.close();
            }
=======
    /**
     * Returns true if the provided class is present in the file.
     * @param expectedClassName the class name in the format Lpkg1/pk2/Name;
     */
    private boolean checkForClass(
            @NonNull String expectedClassName)
            throws ProcessException, IOException {
        InputStream stream = getInputStream("classes.jar");

        ZipInputStream zis = new ZipInputStream(stream);
        try {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (expectedClassName.equals(zipEntry.getName())) {
                    return true;
                }
            }

            // didn't find the class.
            return false;
        } finally {
            zis.close();
>>>>>>> a35fd7c... Support for provided aars.
        }
    }
}
