package com.android.build.gradle.internal;

import com.android.utils.ILogger;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

/**
 * Utilities for logging with pre-formatted message.
 */
public class LoggingUtil {

    public static void displayWarning(ILogger logger, Project project, String message) {
        logger.warning(createWarning(project.getPath(), message));
    }

    public static void displayWarning(Logger logger, Project project, String message) {
        logger.warn(createWarning(project.getPath(), message));
    }

    public static void displayDeprecationWarning(Logger logger, Project project, String message) {
        displayWarning(logger, project, message);
    }

    private static String createWarning(String projectName, String message) {
        return "WARNING [Project: " + projectName + "] " + message;
    }

}
