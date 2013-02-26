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

package com.android.build.gradle.buildsrc

import com.google.common.base.Charsets
import com.google.common.collect.Sets
import com.google.common.hash.HashCode
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedModuleVersionResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.tasks.TaskAction

class CloneArtifactsTask extends DefaultTask {

    private static final String URL_MAVEN_CENTRAL = "http://repo1.maven.org/maven2"

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml"
    private static final String DOT_MD5 = ".md5"
    private static final String DOT_SHA1 = ".sha1"
    private static final String DOT_POM = ".pom"
    private static final String DOT_JAR = ".jar"
    private static final String SOURCES_JAR = "-sources.jar"

    Project project

    File repo

    @TaskAction
    public void cloneArtifacts() {
        // add maven to the project.

        project.repositories {
            mavenCentral()
        }

        Set<ModuleVersionIdentifier> list = Sets.newHashSet()

        ResolutionResult resolutionResult = project.configurations.compile.getIncoming().getResolutionResult()
        buildArtifactList(resolutionResult.getRoot(), list)

        resolutionResult = project.configurations.testCompile.getIncoming().getResolutionResult()
        buildArtifactList(resolutionResult.getRoot(), list)

        for (ModuleVersionIdentifier id : list) {
            // ignore all android artifact
            if (id.getGroup().startsWith("com.android.tools")) {
                continue
            }
            pullArtifact(id, getRepo())
        }
    }

    private void buildArtifactList(ResolvedModuleVersionResult module,
                                   Set<ModuleVersionIdentifier> list) {
        list.add(module.id)

        for (DependencyResult d : module.getDependencies()) {
            if (d instanceof UnresolvedDependencyResult) {
                UnresolvedDependencyResult dependency = (UnresolvedDependencyResult) d
                ModuleVersionSelector attempted = dependency.getAttempted();
                list.add(DefaultModuleVersionIdentifier.newId(
                        attempted.getGroup(), attempted.getName(), attempted.getVersion()))
            } else {
                buildArtifactList(((ResolvedDependencyResult) d).getSelected(), list)
            }
        }
    }

    private void pullArtifact(ModuleVersionIdentifier artifact, File rootDestination) {
        String folder = getFolder(artifact)

        // download the artifact metadata file.
        downloadFile(folder, MAVEN_METADATA_XML, rootDestination)

        // move to the folder of the required the version
        folder = folder + "/" + artifact.getVersion()

        // create name base
        String baseName = artifact.getName() + "-" + artifact.getVersion()

        // download the pom
        File pomFile = downloadFile(folder, baseName + DOT_POM, rootDestination)
        System.out.println("Downloaded: " + pomFile.absoluteFile);

        // read the pom to figure out parents, relocation and packaging
        if (!handlePom(pomFile, rootDestination)) {
            // pom said there's no jar to download: abort
            return
        }

        // download the jar artifact
        downloadFile(folder, baseName + DOT_JAR, rootDestination)

        // download the source if available
        try {
            downloadFile(folder, baseName + SOURCES_JAR, rootDestination)
        } catch (IOException e) {
            // ignore if missing
        }
    }

    private static String getFolder(ModuleVersionIdentifier artifact) {
        return artifact.getGroup().replaceAll("\\.", "/") + "/" + artifact.getName()
    }

    private static File downloadFile(String folder, String name, File rootDestination) {
        File destinationFolder = new File(rootDestination, folder)
        destinationFolder.mkdirs()

        URL fileURL = new URL(URL_MAVEN_CENTRAL + "/" + folder + "/" + name)
        File destinationFile = new File(destinationFolder, name)
        FileUtils.copyURLToFile(fileURL, destinationFile)

        // get the checksums
        URL md5URL = new URL(URL_MAVEN_CENTRAL + "/" + folder + "/" + name + DOT_MD5)
        File md5File = new File(destinationFolder, name + DOT_MD5)
        FileUtils.copyURLToFile(md5URL, md5File)

        checksum(destinationFile, md5File, Hashing.md5());

        URL sha15URL = new URL(URL_MAVEN_CENTRAL + "/" + folder + "/" + name + DOT_SHA1)
        File sha1File = new File(destinationFolder, name + DOT_SHA1)
        FileUtils.copyURLToFile(sha15URL, sha1File)

        checksum(destinationFile, sha1File, Hashing.sha1());

        return destinationFile
    }

    /**
     * Handles a pom and return true if there is a jar package to download.
     *
     * @param pomFile the pom file
     * @param rootDestination where the download happens, in case parent pom must be downloaded.
     *
     * @return true if jar packaging must be downloaded
     */
    private boolean handlePom(File pomFile, File rootDestination) {
        PomHandler pomHandler = new PomHandler(pomFile)

        if (pomHandler.isRelocated()) {
            return false
        }

        ModuleVersionIdentifier parentPom = pomHandler.getParentPom()
        if (parentPom != null) {
            pullArtifact(parentPom, rootDestination)
        }

        String packaging = pomHandler.getPackaging();

        // default packaging is jar so missing data is ok.
        return packaging == null || "jar".equals(packaging) || "bundle".equals(packaging)
    }

    private static void checksum(File file, File checksumFile, HashFunction hashFunction)
            throws IOException {
        // get the checksum value
        List<String> lines = Files.readLines(checksumFile, Charsets.UTF_8)
        if (lines.isEmpty()) {
            throw new IOException("Empty file: " + checksumFile)
        }

        // read the first line only.
        String checksum = lines.get(0).trim()
        // it is possible that the line also contains the file for which this checksum applies:
        // <checksum> <file>
        // remove it
        int pos = checksum.indexOf(' ')
        if (pos != -1) {
            checksum = checksum.substring(0, pos)
        }

        // hash the file.
        HashCode hashCode = Files.asByteSource(file).hash(hashFunction);
        String hashCodeString = hashCode.toString()

        if (!checksum.equals(hashCodeString)) {
            throw new IOException(String.format("Wrong checksum!\n\t%s computed for %s\n\t%s read from %s",
                hashCodeString, file,
                checksum, checksumFile))
        }
    }
}
