/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.build.gradle.internal

import com.android.annotations.NonNull
import com.android.ide.common.res2.MergingException
import com.android.utils.ILogger
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

/**
 * Implementation of Android's {@link ILogger} over gradle's {@link Logger}.
 */
class LoggerWrapper implements ILogger {

    private final Logger logger

    private final LogLevel infoLogLevel

    LoggerWrapper(@NonNull Logger logger) {
        this(logger, LogLevel.INFO)
    }

    /* Allow info level messages to be remapped to e.g. LIFECYCLE rather than INFO.
       As Gradle has more granularity in log level than ILogger. */
    LoggerWrapper(@NonNull Logger logger, @NonNull LogLevel infoLogLevel) {
        this.logger = logger
        this.infoLogLevel = infoLogLevel
    }

    @Override
    void error(Throwable throwable, String s, Object... objects) {
        if (throwable instanceof MergingException) {
            // MergingExceptions have a known cause: they aren't internal errors, they
            // are errors in the user's code, so a full exception is not helpful (and
            // these exceptions should include a pointer to the user's error right in
            // the message).
            //
            // Furthermore, these exceptions are already caught by the MergeResources
            // and MergeAsset tasks, so don't duplicate the output
            return
        }

        if (!logger.isEnabled(LogLevel.ERROR)) {
            return
        }

        if (objects != null && objects.length > 0) {
            s = String.format(s, objects)
        }

        if (throwable == null) {
            logger.log(LogLevel.ERROR, s)

        } else {
            logger.log(LogLevel.ERROR, s, throwable)
        }
    }

    @Override
    void warning(String s, Object... objects) {
        if (!logger.isEnabled(LogLevel.WARN)) {
            return
        }
        if (objects == null || objects.length == 0) {
            logger.log(LogLevel.WARN, s)
        } else {
            logger.log(LogLevel.WARN, String.format(s, objects))
        }
    }

    @Override
    void info(String s, Object... objects) {
        if (!logger.isEnabled(infoLogLevel)) {
            return
        }
        if (objects == null || objects.length == 0) {
            logger.log(infoLogLevel, s)
        } else {
            logger.log(infoLogLevel, String.format(s, objects))
        }
    }

    @Override
    void verbose(String s, Object... objects) {
        if (!logger.isEnabled(LogLevel.DEBUG)) {
            return
        }
        if (objects == null || objects.length == 0) {
            logger.log(LogLevel.DEBUG, s)

        } else {
            logger.log(LogLevel.DEBUG, String.format(s, objects))
        }
    }
}
