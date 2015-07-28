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

package com.android.build.gradle.internal.coverage;
import com.android.utils.FileUtils;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Simple Jacoco report task that calls the Ant version.
 */
public class JacocoReportTask extends DefaultTask {

    private File coverageFile;

    private File reportDir;

    private File classDir;

    private List<File> sourceDir;

    private String reportName;

    private FileCollection jacocoClasspath;

    @TaskAction
    public void report() throws IOException {
        File reportOutDir = getReportDir();
        FileUtils.deleteFolder(reportOutDir);
        reportOutDir.mkdirs();

        getAnt().taskdef(name: 'reportWithJacoco',
                         classname: 'org.jacoco.ant.ReportTask',
                         classpath: getJacocoClasspath().asPath)
        getAnt().reportWithJacoco {
            executiondata {
                file(file: getCoverageFile())
            }
            structure(name: getReportName()) {
                sourcefiles {
                    for (File source : getSourceDir()) {
                        fileset(dir: source)
                    }
                }
                classfiles {
                    fileset(
                            dir: getClassDir(),
                            excludes: "**/R.class,**/R\$*.class,**/Manifest.class,**/Manifest\$*.class,**/BuildConfig.class")
                }
            }

            html(destdir: reportOutDir)
            xml(destfile: new File(reportOutDir, "report.xml"))
        }
    }

    @InputFile
    public File getCoverageFile() {
        return coverageFile;
    }

    public void setCoverageFile(File coverageFile) {
        this.coverageFile = coverageFile;
    }

    @OutputDirectory
    public File getReportDir() {
        return reportDir;
    }

    public void setReportDir(File reportDir) {
        this.reportDir = reportDir;
    }

    @InputDirectory
    public File getClassDir() {
        return classDir;
    }

    public void setClassDir(File classDir) {
        this.classDir = classDir;
    }

    @InputFiles
    public List<File> getSourceDir() {
        return sourceDir;
    }

    public void setSourceDir(List<File> sourceDir) {
        this.sourceDir = sourceDir;
    }

    @Input
    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    @InputFiles
    public FileCollection getJacocoClasspath() {
        return jacocoClasspath;
    }

    public void setJacocoClasspath(FileCollection jacocoClasspath) {
        this.jacocoClasspath = jacocoClasspath;
    }
}
