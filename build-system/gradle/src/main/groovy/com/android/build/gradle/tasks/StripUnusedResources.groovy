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

package com.android.build.gradle.tasks

import com.android.build.gradle.internal.tasks.BaseTask
import com.android.build.gradle.internal.variant.BaseVariantData
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Task which strips out unused resources
 * <p>
 * The process works as follows:
 * <ul>
 * <li> Collect R id <b>values</b> from the final merged R class, which incorporates
 *      the final id's of all the libraries (if ProGuard hasn't inlined these,
 *      we don't need to do this; we can look for actual R.id's instead!)
 * <li> Collect <b>used<b> R values from all the .class files, and R.x.y references too!
 * <li> Compute the set of remaining/used id’s
 * <li> Add in any found in the manifest
 * <li> Look through all resources and produce a reachability graph
 * <li> White-list everything there (graph algorithm to find connected components)
 * <li> The remainder is dead and can be deleted. Consider moving to a new directory.
 * <li> Dump Report listing savings
 * </ul>
 */
public class StripUnusedResources extends BaseTask {
    @InputFile
    public File classesJar

    @InputFile
    public File rDir

    @InputFile
    public File mergedManifest

    @InputFile
    public File resources

    // Do *NOT* attempt to call this property "mapping" (since it points to ProGuard's
    // mapping.txt).
    @InputFile
    public File proguardMapFile

    @Input
    public boolean enabled = true

    // TODO: Get rid of this
    public BaseVariantData variantData

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    public void strip() {
        try {
            def analyzer = new ResourceUsageAnalyzer(rDir, classesJar, mergedManifest,
                    proguardMapFile, resources)
            analyzer.dryRun = false
            analyzer.verbose = false
            analyzer.analyze();
            analyzer.removeUnused()

            def processResourcesTask = variantData.processResourcesTask

            // Repackage the .ap_ resources:
            int sizeBefore = 0;
            String packageOutputPath =
                    new File(processResourcesTask.getPackageOutputFile().absolutePath);
            File packageOutputFile = new File(packageOutputPath);
            if (packageOutputFile.exists()) {
                sizeBefore = packageOutputFile.length()/1024
                packageOutputFile.delete()
            }

            println "Repackaging: " + processResourcesTask.getResDir()

            getBuilder().processResources(
                    processResourcesTask.getManifestFile(),
                    processResourcesTask.getResDir(),
                    processResourcesTask.getAssetsDir(),
                    processResourcesTask.getLibraries(),
                    processResourcesTask.getPackageForR(),
                    null,
                    processResourcesTask.getTextSymbolOutputDir()?.absolutePath,
                    // This is the important part: consider pointing to a different file here!
                    processResourcesTask.getPackageOutputFile()?.absolutePath,
                    null,
                    processResourcesTask.getType(),
                    processResourcesTask.getDebuggable(),
                    processResourcesTask.getAaptOptions(),
                    processResourcesTask.getResourceConfigs(),
                    processResourcesTask.getEnforceUniquePackageName()
            )


            int sizeAfter = packageOutputFile.length()/1024

            // ZipFile is returning incorrect sizes for uncompressed data. Only display
            // compressed statistics for now.
            //int uncompressedSizeAfter = getUncompressedSize(packageOutputFile)/1024
            //println "Binary XML data reduced from " +
            //        uncompressedSizeBefore + "KB to " + uncompressedSizeAfter +
            //        "KB (compressed size from " +
            //        sizeBefore + "KB to " + sizeAfter + "KB)"
            println "Binary compressed XML data reduced from " +
                    sizeBefore + "KB to " + sizeAfter + "KB"

        } catch (Exception e) {
            println 'Failed to shrink resources: ' + e.toString()
        }
    }

    static long getUncompressedSize(File jarFile) {
        def zipFile = new ZipFile(jarFile);
        long size = 0
        Enumeration<? extends ZipEntry> e = zipFile.entries()
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement()
            size += entry.size
        }
        zipFile.close()
        return size
    }
}
