package com.android.tools.perflib.vmtrace;

import com.google.common.base.Charsets;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class VmTraceParserTest extends TestCase {
    public void testParseHeader() throws IOException {
        String header = "*version\n"
                + "3\n"
                + "data-file-overflow=true\n"
                + "clock=dual\n"
                + "elapsed-time-usec=4713089\n"
                + "clock-call-overhead-nsec=1974\n"
                + "vm=dalvik\n"
                + "*threads\n"
                + "1   main\n"
                + "11  AsyncTask #1\n"
                + "2   GC\n"
                + "*methods\n"
                + "0x62830738  android/graphics/Bitmap access$100  (I)V    Bitmap.java 29\n"
                + "0x6282b408  java/lang/Daemons$FinalizerDaemon   access$100  ()Ljava/lang/Daemons$FinalizerDaemon;   Daemons.java    160\n"
                + "0x6282b440  java/lang/Daemons$FinalizerDaemon   access$400  (Ljava/lang/Daemons$FinalizerDaemon;)Ljava/lang/Object; Daemons.java    160\n"
                + "0x6282b4b0  java/lang/Daemons$FinalizerDaemon   doFinalize  (Ljava/lang/ref/FinalizerReference;)V   Daemons.java    -1\n"
                + "*end\n"
                + "data section follows..";

        VmTraceParser parser = new VmTraceParser();
        parser.parseKeys(new ByteArrayInputStream(header.getBytes(Charsets.US_ASCII)));
        VmTraceData traceData = parser.getTraceData();

        assertEquals(3, traceData.getVersion());
        assertTrue(traceData.isDataFileOverflow());
        assertEquals(VmTraceData.ClockType.DUAL, traceData.getClockType());
        assertEquals("dalvik", traceData.getVm());
    }
}
