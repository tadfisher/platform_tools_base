package com.android.tools.perflib.vmtrace;

import com.android.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.io.Closeables;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class VmTraceParser {
    private static final String HEADER_SECTION_VERSION = "*version";
    private static final String HEADER_SECTION_THREADS = "*threads";
    private static final String HEADER_SECTION_METHODS = "*methods";
    private static final String HEADER_END = "*end";

    private static final String KEY_CLOCK = "clock";
    private static final String KEY_DATA_OVERFLOW = "data-file-overflow";
    private static final String KEY_VM = "vm";

    private final File mTraceFile;

    private VmTraceData mTraceData;

    public VmTraceParser(File traceFile) {
        if (!traceFile.exists()) {
            throw new IllegalArgumentException(
                    "Trace file " + traceFile.getAbsolutePath() + " does not exist.");
        }
        mTraceFile = traceFile;
        mTraceData = new VmTraceData();
    }

    @VisibleForTesting
    VmTraceParser() {
        mTraceFile = null;
        mTraceData = new VmTraceData();
    }

    public void parse() throws IOException {
        FileInputStream is = new FileInputStream(mTraceFile);
        parseHeader(is);
    }

    public VmTraceData getTraceData() {
        return mTraceData;
    }

    static final int PARSE_VERSION = 0;
    static final int PARSE_THREADS = 1;
    static final int PARSE_METHODS = 2;
    static final int PARSE_OPTIONS = 4;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    void parseHeader(InputStream is) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(is, Charsets.US_ASCII));

            int mode = PARSE_VERSION;
            String line;
            while (true) {
                line = in.readLine();
                if (line == null) {
                    throw new IOException("Key section does not have an *end marker");
                }

                if (line.startsWith("*")) {
                    if (line.equals(HEADER_SECTION_VERSION)) {
                        mode = PARSE_VERSION;
                        continue;
                    }
                    if (line.equals(HEADER_SECTION_THREADS)) {
                        mode = PARSE_THREADS;
                        continue;
                    }
                    if (line.equals(HEADER_SECTION_METHODS)) {
                        mode = PARSE_METHODS;
                        continue;
                    }
                    if (line.equals(HEADER_END)) {
                        break;
                    }
                }

                switch (mode) {
                    case PARSE_VERSION:
                        mTraceData.setVersion(Integer.decode(line));
                        mode = PARSE_OPTIONS;
                        break;
                    case PARSE_THREADS:
                        parseThread(line);
                        break;
                    case PARSE_METHODS:
                        parseMethod(line);
                        break;
                    case PARSE_OPTIONS:
                        parseOption(line);
                        break;
                }
            }
        } finally {
            if (in != null) {
                Closeables.closeQuietly(in);
            }
        }
    }

    /** Parses trace option formatted as a key value pair. */
    private void parseOption(String line) {
        String[] tokens = line.split("=");
        if (tokens.length == 2) {
            String key = tokens[0];
            String value = tokens[1];

            if (key.equals(KEY_CLOCK)) {
                if (value.equals("thread-cpu")) {
                    mTraceData.setClockType(VmTraceData.ClockType.THREAD_CPU);
                } else if (value.equals("wall")) {
                    mTraceData.setClockType(VmTraceData.ClockType.WALL);
                } else if (value.equals("dual")) {
                    mTraceData.setClockType(VmTraceData.ClockType.DUAL);
                }
            } else if (key.equals(KEY_DATA_OVERFLOW)) {
                mTraceData.setDataFileOverflow(Boolean.parseBoolean(value));
            } else if (key.equals(KEY_VM)) {
                mTraceData.setVm(value);
            } else {
                mTraceData.setProperty(key, value);
            }
        }
    }

    /** Parses thread information comprising an integer id and the thread name */
    private void parseThread(String line) {
        int index = line.indexOf('\t');
        if (index < 0) {
            return;
        }

        try {
            int id = Integer.decode(line.substring(0, index));
            String name = line.substring(index).trim();
            mTraceData.addThread(id, name);
        } catch (NumberFormatException e) {
            return;
        }
    }

    void parseMethod(String line) {
        String[] tokens = line.split("\t");
        long id;
        try {
            id = Long.decode(tokens[0]);
        } catch (NumberFormatException e) {
            return;
        }

        String className = tokens[1];
        String methodName = null;
        String signature = null;
        String pathname = null;
        int lineNumber = -1;
        if (tokens.length == 6) {
            methodName = tokens[2];
            signature = tokens[3];
            pathname = tokens[4];
            lineNumber = Integer.decode(tokens[5]);
            pathname = constructPathname(className, pathname);
        } else if (tokens.length > 2) {
            if (tokens[3].startsWith("(")) {
                methodName = tokens[2];
                signature = tokens[3];
            } else {
                pathname = tokens[2];
                lineNumber = Integer.decode(tokens[3]);
            }
        }

        mTraceData.addMethod(id, new MethodInfo(id, className, methodName, signature,
                pathname, lineNumber));
    }

    private String constructPathname(String className, String pathname) {
        int index = className.lastIndexOf('/');
        if (index > 0 && index < className.length() - 1 && pathname.endsWith(".java")) {
            pathname = className.substring(0, index + 1) + pathname;
        }
        return pathname;
    }
}
