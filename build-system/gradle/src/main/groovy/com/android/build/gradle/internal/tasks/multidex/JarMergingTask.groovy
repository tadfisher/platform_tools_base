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



package com.android.build.gradle.internal.tasks.multidex
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Custom Jar task that can merge other jars.
 * This ignores all non .class files since this is strictly to
 * handle code.
 */
class JarMergingTask extends DefaultTask {

    // could be files (jar) or folders of classes.
    @InputFiles
    Iterable<File> inputFiles

    @OutputFile
    File jarFile

    @TaskAction
    void createJar() {
        jarFile.delete()

        FileOutputStream fos = new FileOutputStream(jarFile)
        JarOutputStream jos = new JarOutputStream(fos)

        final byte[] buffer = new byte[8192]

        for (File file : getInputFiles()) {
            if (file.isDirectory()) {
                processFolder(jos, "", file, buffer)
            } else if (file.isFile() && file.getPath().endsWith(".jar")) {
                processJarFile(jos, file, buffer)
            }
        }

        jos.close()
    }

    private void processFolder(JarOutputStream jos, String path, File folder, byte[] buffer) {
        for (File file : folder.listFiles()) {
            if (file.isFile()) {
                // new entry
                jos.putNextEntry(new JarEntry(path + file.getName()))

                // put the file content
                FileInputStream fis = new FileInputStream(file)
                int count
                while ((count = fis.read(buffer)) != -1) {
                    jos.write(buffer, 0, count)
                }
                fis.close()

                // close the entry
                jos.closeEntry()
            } else if (file.isDirectory()) {
                processFolder(jos, path + file.getName() + "/", file, buffer)
            }
        }
    }

    private static void processJarFile(JarOutputStream jos, File file, byte[] buffer) {
        FileInputStream fis = new FileInputStream(file)
        ZipInputStream zis = new ZipInputStream(fis)

        try {
            // loop on the entries of the jar file package and put them in the final jar
            ZipEntry entry
            while ((entry = zis.getNextEntry()) != null) {
                // do not take directories or anything inside a potential META-INF folder.
                if (entry.isDirectory()) {
                    continue
                }

                String name = entry.getName()
                if (!name.endsWith(".class")) {
                    continue
                }

                JarEntry newEntry

                // Preserve the STORED method of the input entry.
                if (entry.getMethod() == JarEntry.STORED) {
                    newEntry = new JarEntry(entry)
                } else {
                    // Create a new entry so that the compressed len is recomputed.
                    newEntry = new JarEntry(name)
                }

                // add the entry to the jar archive
                jos.putNextEntry(newEntry)

                // read the content of the entry from the input stream, and write it into the archive.
                int count
                while ((count = zis.read(buffer)) != -1) {
                    jos.write(buffer, 0, count)
                }

                // close the entries for this file
                jos.closeEntry()
                zis.closeEntry()
            }
        } finally {
            zis.close()
        }

        fis.close()
    }
}
