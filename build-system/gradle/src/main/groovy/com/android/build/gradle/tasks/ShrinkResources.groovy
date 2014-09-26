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
import com.android.build.gradle.internal.variant.BaseVariantOutputData
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

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
 * <li> Look through all resources and produce a graph of reachable resources
 * <li> Compute unused resources by visiting all resources and ignoring those that
 *      were reachable
 * <li> In addition, if we find a call to Resources#getIdentifier(), we collect all
 *      strings in the class files, and also mark as used any resources that match
 *      potential string lookups
 * </ul>
 */
public class ShrinkResources extends BaseTask {
    /** This is not a constant; can be set by BuildType#shrinkResources("verbose") */
    public static boolean verbose = false;

    /** This is not a constant; can be set by BuildType#shrinkResources("debug") */
    public static boolean debug = false;

    /**
     * Associated variant data that the strip task will be run against. Used to locate
     * not only locations the task needs (e.g. for resources and generated R classes)
     * but also to obtain the resource merging task, since we will run it a second time
     * here to generate a new .ap_ file with fewer resources
     */
    public BaseVariantOutputData variantOutputData

    /**
     * The location of the stripped classes. This is provided as a separate property since
     * the ProguardTask does not expose a getter for it
     */
    @InputFile
    public File classesJar

    /**
     * The proguard mapping file, if any. Also provided as a separate property since the
     * ProGuard task does not expose a getter for it.
     */
    @InputFile
    public File proguardMapFile

    @InputFile
    File uncompressedResources

    @OutputFile
    File compressedResources

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    void shrink(IncrementalTaskInputs inputs) {
        // TODO: Check extension.getUseOldManifestMerger()
        def variantData = variantOutputData.variantData
        try {
            def processResourcesTask = variantData.generateRClassTask
            File sourceDir = processResourcesTask.sourceOutputDir
            File resourceDir = variantData.mergeResourcesTask.outputDir
            File mergedManifest = variantOutputData.manifestProcessorTask.manifestOutputFile

            // Analyze resources and usages and strip out unused
            def analyzer = new ResourceUsageAnalyzer(sourceDir, classesJar, mergedManifest,
                    proguardMapFile, resourceDir)
            analyzer.verbose = project.logger.isEnabled(LogLevel.INFO)
            analyzer.debug = project.logger.isEnabled(LogLevel.DEBUG)
            analyzer.analyze();

            //noinspection GroovyConstantIfStatement
            if (ResourceUsageAnalyzer.TWO_PASS_AAPT) {
                def destination = new File(resourceDir.parentFile, resourceDir.name + "-stripped")
                analyzer.removeUnused(destination)

                File sourceOutputs = processResourcesTask.getSourceOutputDir();
                sourceOutputs = new File(sourceOutputs.getParentFile(),
                        sourceOutputs.getName() + "-stripped")
                sourceOutputs.mkdirs()

                // We don't need to emit R files again, but we can do this here such that
                // we can *verify* that the R classes generated in the second aapt pass
                // matches those we saw the first time around.
                //String sourceOutputPath = sourceOutputs?.getAbsolutePath();
                String sourceOutputPath = null

                // Repackage the resources:
                getBuilder().processResources(
                        processResourcesTask.getManifestFile(),
                        destination,
                        processResourcesTask.getAssetsDir(),
                        processResourcesTask.getLibraries(),
                        processResourcesTask.getPackageForR(),
                        sourceOutputPath,
                        null,
                        getCompressedResources().absolutePath,
                        null,
                        processResourcesTask.getType(),
                        processResourcesTask.getDebuggable(),
                        processResourcesTask.getAaptOptions(),
                        processResourcesTask.getResourceConfigs(),
                        processResourcesTask.getEnforceUniquePackageName(),
                        processResourcesTask.getSplits()
                )
            } else {
                // Just rewrite the .ap_ file to strip out the res/ files for unused resources
                analyzer.rewriteResourceZip(getUncompressedResources(), getCompressedResources())
            }

            // Dump some stats
            int unused = analyzer.getUnusedResourceCount()
            if (unused > 0) {
                StringBuilder sb = new StringBuilder(200);
                sb.append("Removed unused resources")

                // This is a bit misleading until we can strip out all resource types:
                //int total = analyzer.getTotalResourceCount()
                //sb.append("(" + unused + "/" + total + ")")

                int before = getUncompressedResources().length()
                int after = getCompressedResources().length()
                int percent = (before - after) * 100 / before
                sb.append(": Binary resource data reduced from ").
                        append(toKbString(before)).
                        append("KB to ").
                        append(toKbString(after)).
                        append("KB: Removed " + percent + "%\n");
                sb.append("Note: If necessary, you can disable resource shrinking by adding\n" +
                          "android {\n" +
                          "    buildTypes {\n" +
                          "        " + variantData.variantConfiguration.buildType.name + " {\n" +
                          "            shrinkResources false\n" +
                          "        }\n" +
                          "    }\n" +
                          "}")

                println sb.toString();
            }

        } catch (Exception e) {
            println 'Failed to shrink resources: ' + e.toString() + '; ignoring'
            logger.quiet("Failed to shrink resources: ignoring", e)
        }
    }

    private static String toKbString(long size) {
        return Integer.toString((int)size/1024);
    }
}
