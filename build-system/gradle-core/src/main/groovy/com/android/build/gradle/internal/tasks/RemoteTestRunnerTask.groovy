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
package com.android.build.gradle.internal.tasks

import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.test.TestDataImpl
import com.android.build.gradle.internal.test.report.ReportType
import com.android.build.gradle.internal.test.report.TestReport
import com.android.builder.internal.testing.CustomTestRunListener
import com.android.builder.internal.testing.SimpleTestCallable
import com.android.builder.testing.TestData
import com.android.builder.testing.api.RemoteTestRunner
import com.android.ddmlib.testrunner.InstrumentationResultParser
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.ConsoleRenderer
/**
 * Runs instrumentation tests for a given variant.
 */
public class RemoteTestRunnerTask extends BaseTask implements AndroidTestTask {

    File reportsDir
    File resultsDir
    File coverageDir

    String flavorName

    TestData testData

    RemoteTestRunner remoteTestRunner

    boolean ignoreFailures
    boolean testFailed

    @TaskAction
    protected void runTests() {

        File resultsOutDir = getResultsDir()
        emptyFolder(resultsOutDir)

        File coverageOutDir = getCoverageDir()
        emptyFolder(coverageOutDir)

        boolean success = false;
        // If there are tests to run, and the test runner returns with no results, we fail (since
        // this is most likely a problem with the device setup). If no, the task will succeed.
        if (!testsFound()) {
            logger.info("No tests found, nothing to do.")
            // If we don't create the coverage file, createXxxCoverageReport task will fail.
            File emptyCoverageFile = new File(coverageOutDir, SimpleTestCallable.FILE_COVERAGE_EC)
            emptyCoverageFile.createNewFile()
            success = true;
        } else {

            CustomTestRunListener runListener = new CustomTestRunListener(
                    "remote", project.name, getFlavorName(), getILogger());
            runListener.setReportDir(resultsOutDir);

            remoteTestRunner.init()
            Map<String, List<String>> testResults = remoteTestRunner.runTests(testData)
            for (Map.Entry<String, List<String>> testResult: testResults) {

                def parser = new InstrumentationResultParser(testResults.key, runListener);
                parser.processNewLines(testResult.value.toArray(new String[testResult.value.size()]))
                success = true
                parser.flush()
            }
            remoteTestRunner.close()
        }


        // run the report from the results.
        File reportOutDir = getReportsDir()
        emptyFolder(reportOutDir)

        TestReport report = new TestReport(ReportType.SINGLE_FLAVOR, resultsOutDir, reportOutDir)
        report.generateReport()

        if (!success) {
            testFailed = true
            String reportUrl = new ConsoleRenderer().asClickableFileUrl(
                    new File(reportOutDir, "index.html"));
            String message = "There were failing tests. See the report at: " + reportUrl;
            if (getIgnoreFailures()) {
                logger.warn(message)
                return
            } else {
                throw new GradleException(message)
            }
        }

        testFailed = false
    }

    /**
     * Determines if there are any tests to run.
     *
     * @return true if there are some tests to run, false otherwise
     */
    private boolean testsFound() {
        // For now we check if there are any test sources. We could inspect the test classes and
        // apply JUnit logic to see if there's something to run, but that would not catch the case
        // where user makes a typo in a test name or forgets to inherit from a JUnit class
        !project.files(testData.getTestDirectories()).asFileTree.empty
    }
}
