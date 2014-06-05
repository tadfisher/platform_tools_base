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

package com.android.apigenerator;



import com.android.apigenerator.AndroidJarReader.FolderType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

/**
 * Main class for command line command to convert the existing API XML/TXT files into diff-based
 * simple text files.
 *
 */
public class Main {

    /**
     * @param args
     */
    public static void main(String[] args) {

        boolean error = false;
        int minApi = 1;
        FolderType type = FolderType.SDK;
        String baseFolder = null;
        String outPath = null;

        for (int i = 0; i < args.length && !error; i++) {
            String arg = args[i];

            if (arg.equals("--prebuilt")) {
                type = FolderType.PREBUILT;

            } else if (arg.equals("--min-api")) {
                i++;
                if (i < args.length) {
                    minApi = Integer.parseInt(args[i]);
                } else {
                    System.err.println("Missing number >= 1 after " + arg);
                    error = true;
                }
            } else if (baseFolder == null) {
                baseFolder = arg;

            } else if (outPath == null) {
                outPath = arg;

            } else {
                System.err.println("Unknown argument: " + arg);
                error = true;
            }
        }

        if (!error && baseFolder == null) {
            System.err.println("Missing base folder");
            error = true;
        }

        if (!error && outPath == null) {
            System.err.println("Missing out file path");
            error = true;
        }

        if (error) {
            printUsage();
        }

        AndroidJarReader reader = new AndroidJarReader(type, baseFolder, minApi);
        Map<String, ApiClass> classes = reader.getClasses();
        createApiFile(new File(outPath), classes);
    }

    private static void printUsage() {
        System.err.println("\nGenerates a single API file from the content of an SDK.\n");
        System.err.println("Usage\n");
        System.err.println("\tApiCheck [--min-api=1] [--prebuilt] SDKFOLDER OUTFILE\n");
        System.exit(1);
    }

    /**
     * Creates the simplified diff-based API level.
     * @param outFolder the out folder.
     * @param classes
     */
    private static void createApiFile(File outFile, Map<String, ApiClass> classes) {

        PrintStream ps = null;
        try {
            ps = new PrintStream(outFile);
            ps.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            ps.println("<api version=\"1\">");
            TreeMap<String, ApiClass> map = new TreeMap<String, ApiClass>(classes);
            for (ApiClass theClass : map.values()) {
                (theClass).print(ps);
            }
            ps.println("</api>");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }
}
