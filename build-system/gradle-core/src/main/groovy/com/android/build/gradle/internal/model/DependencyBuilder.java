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

package com.android.build.gradle.internal.model;

import static com.android.SdkConstants.DOT_JAR;
import static com.android.SdkConstants.PLATFORM_DARWIN;

import com.android.annotations.NonNull;
import com.android.builder.dependency.LibraryDependency;
import com.android.builder.model.AndroidLibrary;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 */
public class DependencyBuilder {

    private static DependencyBuilder sBuilder = new DependencyBuilder();

    public static DependencyBuilder builder() {
        return sBuilder;
    }

    Map<File, AndroidLibrary> cache = Maps.newHashMap();

    @NonNull
    public synchronized AndroidLibrary getAndroidLibrary(@NonNull LibraryDependency libraryDependency) {
        File bundle = libraryDependency.getBundle();
        AndroidLibrary androidLibrary = cache.get(bundle);

        if (androidLibrary == null) {
            List<LibraryDependency> deps = libraryDependency.getDependencies();
            List<AndroidLibrary> clonedDeps = Lists.newArrayListWithCapacity(deps.size());
            for (LibraryDependency child : deps) {
                AndroidLibrary clonedLib = getAndroidLibrary(child);
                clonedDeps.add(clonedLib);
            }

            // compute local jar even if the bundle isn't exploded.
            Collection<File> localJarOverride = findLocalJar(libraryDependency);

            androidLibrary = new AndroidLibraryImpl(
                    libraryDependency,
                    clonedDeps,
                    localJarOverride,
                    libraryDependency.getProject(),
                    libraryDependency.getProjectVariant(),
                    libraryDependency.getRequestedCoordinates(),
                    libraryDependency.getResolvedCoordinates());
            cache.put(bundle, androidLibrary);
        }

        return androidLibrary;
    }

    /**
     * Finds the local jar for an aar.
     *
     * Since the model can be queried before the aar are exploded, we attempt to get them
     * from inside the aar.
     *
     * @param library the library.
     * @return its local jars.
     */
    @NonNull
    private static Collection<File> findLocalJar(LibraryDependency library) {
        // if the library is exploded, just use the normal method.
        File explodedFolder = library.getFolder();
        if (explodedFolder.isDirectory()) {
            return library.getLocalJars();
        }

        // if the aar file is present, search inside it for jar files under libs/
        File aarFile = library.getBundle();
        if (aarFile.isFile()) {
            List<File> jarList = Lists.newArrayList();

            ZipFile zipFile = null;
            try {
                //noinspection IOResourceOpenedButNotSafelyClosed
                zipFile = new ZipFile(aarFile);

                for (Enumeration<? extends ZipEntry> e = zipFile.entries(); e.hasMoreElements();) {
                    ZipEntry zipEntry = e.nextElement();
                    String name = zipEntry.getName();
                    if (name.startsWith("libs/") && name.endsWith(DOT_JAR)) {
                        jarList.add(new File(explodedFolder, name.replace('/', File.separatorChar)));
                    }
                }

                return jarList;
            } catch (FileNotFoundException ignored) {
                // should not happen since we check ahead of time
            } catch (IOException e) {
                // we'll return an empty list below
            } finally {
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        return Collections.emptyList();
    }

}
