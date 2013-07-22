package com.android.tools.perflib.vmtrace;

import com.android.utils.SparseArray;
import com.google.common.base.Charsets;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;

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
                + "1\tmain\n"
                + "11\tAsyncTask #1\n"
                + "*methods\n"
                + "0x62830738\tandroid/graphics/Bitmap\taccess$100\t(I)V\tBitmapF.java\t29\n"
                + "0x6282b408\tFinalizerDaemon\taccess$100\t()Ljava/lang/Daemons$FinalizerDaemon;\tDaemons.java\t160\n"
                + "0x6282b440\tjava/lang/Daemons$FinalizerDaemon\taccess$400\t(Ljava/lang/Daemons$FinalizerDaemon;)Ljava/lang/Object;\tDaemons.java\t160\n"
                + "0x6282b4b0\tjava/lang/Daemons$FinalizerDaemon\tdoFinalize\t(Ljava/lang/ref/FinalizerReference;)V\tDaemons.java\t-1\n"
                + "*end\n"
                + "data section follows..";

        VmTraceParser parser = new VmTraceParser();
        parser.parseHeader(new ByteArrayInputStream(header.getBytes(Charsets.US_ASCII)));
        VmTraceData traceData = parser.getTraceData();

        assertEquals(3, traceData.getVersion());
        assertTrue(traceData.isDataFileOverflow());
        assertEquals(VmTraceData.ClockType.DUAL, traceData.getClockType());
        assertEquals("dalvik", traceData.getVm());

        SparseArray<String> threads = traceData.getThreads();
        assertEquals(2, threads.size());
        assertEquals("main", threads.get(1));
        assertEquals("AsyncTask #1", threads.get(11));

        Collection<MethodInfo> methods = traceData.getMethods();
        assertEquals(4, methods.size());

        MethodInfo info = traceData.getMethod(0x62830738);
        assertNotNull(info);
        assertEquals("android/graphics/Bitmap", info.className);
        assertEquals("access$100", info.methodName);
        assertEquals("(I)V", info.signature);
        assertEquals("android/graphics/BitmapF.java", info.srcPath);
        assertEquals(29, info.srcLineNumber);

        info = traceData.getMethod(0x6282b4b0);
        assertNotNull(info);
        assertEquals(-1, info.srcLineNumber);
    }
}
