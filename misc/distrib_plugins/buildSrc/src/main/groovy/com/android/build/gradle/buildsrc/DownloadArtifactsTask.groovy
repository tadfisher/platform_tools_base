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
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ModuleVersionSelector
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedModuleVersionResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class DownloadArtifactsTask extends DefaultTask {

    private static final String URL_MAVEN_CENTRAL = "http://repo1.maven.org/maven2"

    private static final String MAVEN_METADATA_XML = "maven-metadata.xml"
    private static final String DOT_MD5 = ".md5"
    private static final String DOT_SHA1 = ".sha1"
    private static final String DOT_POM = ".pom"
    private static final String DOT_JAR = ".jar"
    private static final String SOURCES_JAR = "-sources.jar"

    Project project

    @OutputDirectory
    File mainRepo

    @OutputDirectory
    File secondaryRepo

    @TaskAction
    public void downloadArtifacts() {

        Set<ModuleVersionIdentifier> mainList = Sets.newHashSet()
        Set<ModuleVersionIdentifier> secondaryList = Sets.newHashSet()

        // gather the main and secondary dependencies for all the subprojects.
        for (Project subProject : project.subprojects) {
            ResolutionResult resolutionResult = subProject.configurations.compile.getIncoming().getResolutionResult()
            // if the sub project doesn't ship then we put it's main dependencies in the secondary
            // list.
            buildArtifactList(resolutionResult.getRoot(),
                    subProject.cloneArtifacts.isShipped ? mainList : secondaryList)

            resolutionResult = subProject.configurations.testCompile.getIncoming().getResolutionResult()
            buildArtifactList(resolutionResult.getRoot(), secondaryList)

            // manually add some artifacts that aren't detected because they are added dynamically
            // when their task run.
            try {
                resolutionResult = subProject.configurations.getByName("cloneArtifacts").getIncoming().getResolutionResult()
                buildArtifactList(resolutionResult.getRoot(), secondaryList)
            } catch (UnknownDomainObjectException e) {
                // ignore
            }
        }

        File mainRepoFile = getMainRepo()
        File secondaryRepoFile = getSecondaryRepo()

        Set<ModuleVersionIdentifier> downloadedSet = Sets.newHashSet()
        for (ModuleVersionIdentifier id : mainList) {
            pullArtifact(id, mainRepoFile, downloadedSet)
        }

        for (ModuleVersionIdentifier id : secondaryList) {
            pullArtifact(id, secondaryRepoFile, downloadedSet)
        }
    }

    protected void buildArtifactList(ResolvedModuleVersionResult module,
                                   Set<ModuleVersionIdentifier> list) {
        list.add(module.id)

        for (DependencyResult d : module.getDependencies()) {
            if (d instanceof UnresolvedDependencyResult) {
                UnresolvedDependencyResult dependency = (UnresolvedDependencyResult) d
                ModuleVersionSelector attempted = dependency.getAttempted();
                ModuleVersionIdentifier id = DefaultModuleVersionIdentifier.newId(
                        attempted.getGroup(), attempted.getName(), attempted.getVersion())
                list.add(id)
            } else {
                buildArtifactList(((ResolvedDependencyResult) d).getSelected(), list)
            }
        }
    }

    private void pullArtifact(ModuleVersionIdentifier artifact, File rootDestination,
                              Set<ModuleVersionIdentifier> downloadedSet) {
        // ignore all android artifacts and already downloaded artifacts
        if (artifact.group.startsWith("com.android.tools") || artifact.group == "base") {
            return
        }

        if (downloadedSet.contains(artifact)) {
            System.out.println("DUPLCTE " + artifact);
            return
        }

        downloadedSet.add(artifact)

        String folder = getFolder(artifact)

        // download the artifact metadata file.
        downloadFile(folder, MAVEN_METADATA_XML, rootDestination, true, false)

        // move to the folder of the required version
        folder = folder + "/" + artifact.getVersion()

        // create name base
        String baseName = artifact.getName() + "-" + artifact.getVersion()

        // download the pom
        File pomFile = downloadFile(folder, baseName + DOT_POM, rootDestination, false, true)

        // read the pom to figure out parents, relocation and packaging
        if (!handlePom(pomFile, rootDestination, downloadedSet)) {
            // pom said there's no jar to download: abort
            return
        }

        // download the jar artifact
        downloadFile(folder, baseName + DOT_JAR, rootDestination, false, false)

        // download the source if available
        try {
            downloadFile(folder, baseName + SOURCES_JAR, rootDestination, false, false)
        } catch (IOException e) {
            // ignore if missing
        }
    }

    private static String getFolder(ModuleVersionIdentifier artifact) {
        return artifact.getGroup().replaceAll("\\.", "/") + "/" + artifact.getName()
    }

    private static File downloadFile(String folder, String name, File rootDestination,
                                     boolean force, boolean printDownload) {
        File destinationFolder = new File(rootDestination, folder)
        destinationFolder.mkdirs()

        URL fileURL = new URL(URL_MAVEN_CENTRAL + "/" + folder + "/" + name)
        File destinationFile = new File(destinationFolder, name)

        if (force || !destinationFile.isFile()) {
            if (printDownload) {
                System.out.println("DWNLOAD " + destinationFile.absolutePath);
            }
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
        } else if (printDownload) {
            System.out.println("SKIPPED " + destinationFile.absolutePath);
        }

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
    private boolean handlePom(File pomFile, File rootDestination,
                              Set<ModuleVersionIdentifier> downloadedSet) {
        PomHandler pomHandler = new PomHandler(pomFile)

        ModuleVersionIdentifier relocation = pomHandler.getRelocation();
        if (relocation != null) {
            pullArtifact(relocation, rootDestination, downloadedSet);
            return false
        }

        ModuleVersionIdentifier parentPom = pomHandler.getParentPom()
        if (parentPom != null) {
            pullArtifact(parentPom, rootDestination, downloadedSet)
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
