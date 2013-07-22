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
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class VmTraceParser {
    private static final int TRACE_MAGIC = 0x574f4c53; // 'SLOW'

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

    public void parse() throws IOException {
        long headerLength = parseHeader(mTraceFile);
        MappedByteBuffer buffer = mapFile(mTraceFile, headerLength);
        parseData(buffer);
    }

    public VmTraceData getTraceData() {
        return mTraceData;
    }

    static final int PARSE_VERSION = 0;
    static final int PARSE_THREADS = 1;
    static final int PARSE_METHODS = 2;
    static final int PARSE_OPTIONS = 4;

    /** Parses the trace file header and returns the offset in the file where the header ends. */
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    long parseHeader(File f) throws IOException {
        long offset = 0;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(f), Charsets.US_ASCII));

            int mode = PARSE_VERSION;
            String line;
            while (true) {
                line = in.readLine();
                if (line == null) {
                    throw new IOException("Key section does not have an *end marker");
                }

                // Calculate how much we have read from the file so far.  The
                // extra byte is for the line ending not included by readLine().
                offset += line.length() + 1;

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

        return offset;
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

    /**
     * Parses the data section of the trace. The data section is formatted as below:
     * File format:
     *  header
     *  record 0
     *  record 1
     *  ...
     *
     * Header format:
     *  u4  magic ('SLOW')
     *  u2  version
     *  u2  offset to data
     *  u8  start date/time in usec
     *  u2  record size in bytes (version >= 2 only)
     *  ... padding to 32 bytes
     *
     * Record format v1:
     *  u1  thread ID
     *  u4  method ID | method action
     *  u4  time delta since start, in usec
     *
     * Record format v2:
     *  u2  thread ID
     *  u4  method ID | method action
     *  u4  time delta since start, in usec
     *
     * Record format v3:
     *  u2  thread ID
     *  u4  method ID | method action
     *  u4  time delta since start, in usec
     *  u4  wall time since start, in usec (when clock == "dual" only)
     *
     * 32 bits of microseconds is 70 minutes.
     *
     * All values are stored in little-endian order.
     */
    private void parseData(MappedByteBuffer buffer) {
        int recordSize = readDataFileHeader(buffer);
        System.out.println(recordSize);
    }

    private int readDataFileHeader(MappedByteBuffer buffer) {
        int magic = buffer.getInt();
        if (magic != TRACE_MAGIC) {
            String msg = String.format("Error: magic number mismatch; got 0x%x, expected 0x%x\n",
                    magic, TRACE_MAGIC);
            throw new RuntimeException(msg);
        }

        // read version
        int version = buffer.getShort();
        if (version != mTraceData.getVersion()) {
            String msg = String.format(
                    "Error: version number mismatch; got %d in data header but %d in options\n",
                    version, mTraceData.getVersion());
            throw new RuntimeException(msg);
        }
        if (version < 1 || version > 3) {
            String msg = String.format(
                    "Error: unsupported trace version number %d.  "
                            + "Please use a newer version of TraceView to read this file.",
                    version);
            throw new RuntimeException(msg);
        }

        // read offset
        int offsetToData = buffer.getShort() - 16;

        // read startWhen
        buffer.getLong();

        // read record size
        int recordSize;
        switch (version) {
            case 1:
                recordSize = 9;
                break;
            case 2:
                recordSize = 10;
                break;
            default:
                recordSize = buffer.getShort();
                offsetToData -= 2;
                break;
        }

        // Skip over offsetToData bytes
        while (offsetToData-- > 0) {
            buffer.get();
        }

        return recordSize;
    }

    private MappedByteBuffer mapFile(File f, long offset) throws IOException {
        FileInputStream dataFile = new FileInputStream(f);
        try {
            FileChannel fc = dataFile.getChannel();
            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, offset,
                    f.length() - offset);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer;
        } finally {
            dataFile.close(); // this *also* closes the associated channel, fc
        }
    }
}
