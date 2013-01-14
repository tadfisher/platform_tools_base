/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmlib;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final public class HandleViewDebug extends ChunkHandler {
    /** Enable/Disable tracing of OpenGL calls. */
    private static final int CHUNK_VUGL = type("VUGL");

    /** List {@link ViewRootImpl}'s of this process. */
    private static final int CHUNK_VULW = type("VULW");

    /** Dump view hierarchy. */
    private static final int CHUNK_VUDP = type("VUDP");

    /** Capture View Layers. */
    private static final int CHUNK_VUCL = type("VUCL");

    /** Capture View Layers. */
    private static final int CHUNK_VUCV = type("VUCV");

    /** Obtain the Display List corresponding to the view. */
    private static final int CHUNK_VUDL = type("VUDL");

    /** Invalidate View. */
    private static final int CHUNK_VUIV = type("VUIV");

    /** Re-layout given view. */
    private static final int CHUNK_VULT = type("VULT");

    /** Profile a view. */
    private static final int CHUNK_VUPR = type("VUPR");

    private static final HandleViewDebug sInst = new HandleViewDebug();

    private HandleViewDebug() {}

    public static void register(MonitorThread mt) {
        mt.registerChunkHandler(CHUNK_VUCL, sInst);
        mt.registerChunkHandler(CHUNK_VUDL, sInst);
        mt.registerChunkHandler(CHUNK_VUIV, sInst);
        mt.registerChunkHandler(CHUNK_VULT, sInst);
        mt.registerChunkHandler(CHUNK_VUPR, sInst);
    }

    @Override
    public void clientReady(Client client) throws IOException {}

    @Override
    public void clientDisconnected(Client client) {}

    public static void listViewRoots(Client client, ListViewRootsHandler replyHandler)
            throws IOException {
        ByteBuffer buf = allocBuffer(4);
        JdwpPacket packet = new JdwpPacket(buf);
        ByteBuffer chunkBuf = getChunkDataBuf(buf);
        chunkBuf.putInt(1);
        finishChunkPacket(packet, CHUNK_VULW, chunkBuf.position());
        client.sendAndConsume(packet, replyHandler);
    }

    private static abstract class ViewDumpHandler extends ChunkHandler {
        private final CountDownLatch mLatch = new CountDownLatch(1);

        @Override
        void clientReady(Client client) throws IOException {
        }

        @Override
        void clientDisconnected(Client client) {
        }

        protected void resultReceived() {
            mLatch.countDown();
        }

        protected void waitForResult(long timeout, TimeUnit unit) {
            try {
                mLatch.await(timeout, unit);
            } catch (InterruptedException e) {
                // pass
            }
        }
    }

    public static class ListViewRootsHandler extends ViewDumpHandler {
        private List<String> mViewRoots = Collections.synchronizedList(new ArrayList<String>(10));

        @Override
        void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId) {
            if (type != CHUNK_VULW) {
                handleUnknownChunk(client, type, data, isReply, msgId);
                return;
            }

            int nWindows = data.getInt();

            for (int i = 0; i < nWindows; i++) {
                int len = data.getInt();
                mViewRoots.add(getString(data, len));
            }

            resultReceived();
        }

        public List<String> getViewRoots(long timeout, TimeUnit unit) {
            waitForResult(timeout, unit);
            return mViewRoots;
        }
    }

    public static void dumpViewHierarchy(Client client, String viewRoot,
            boolean skipChildren, boolean includeProperties, DumpViewHierarchyHandler handler)
                    throws IOException {
        ByteBuffer buf = allocBuffer(4      // view root length
                + viewRoot.length() * 2     // view root
                + 4                         // skip children
                + 4);                       // include view properties
        JdwpPacket packet = new JdwpPacket(buf);
        ByteBuffer chunkBuf = getChunkDataBuf(buf);

        chunkBuf.putInt(viewRoot.length());
        putString(chunkBuf, viewRoot);
        chunkBuf.putInt(skipChildren ? 1 : 0);
        chunkBuf.putInt(includeProperties ? 1 : 0);

        finishChunkPacket(packet, CHUNK_VUDP, chunkBuf.position());
        client.sendAndConsume(packet, handler);
    }

    public static class DumpViewHierarchyHandler extends ViewDumpHandler {
        private AtomicReference<String> mViewHierarchyRef = new AtomicReference<String>("");
        @Override
        void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId) {
            if (type != CHUNK_VUDP) {
                handleUnknownChunk(client, type, data, isReply, msgId);
                return;
            }

            byte[] b = new byte[data.remaining()];
            data.get(b);
            String s = new String(b, Charset.forName("UTF-8"));
            mViewHierarchyRef.set(s);
            resultReceived();
        }

        public String getViewHierarchy(long timeout, TimeUnit unit) {
            waitForResult(timeout, unit);
            return mViewHierarchyRef.get();
        }
    }

    private static void sendJdwpPacket(Client client, int type, String viewRoot, String view,
            ViewDumpHandler handler) throws IOException {
        int bufLen = 4 + viewRoot.length() * 2 +
                     4 + view.length() * 2;

        ByteBuffer buf = allocBuffer(bufLen);
        JdwpPacket packet = new JdwpPacket(buf);
        ByteBuffer chunkBuf = getChunkDataBuf(buf);

        chunkBuf.putInt(viewRoot.length());
        putString(chunkBuf, viewRoot);

        chunkBuf.putInt(view.length());
        putString(chunkBuf, view);

        finishChunkPacket(packet, type, chunkBuf.position());
        if (handler != null) {
            client.sendAndConsume(packet, handler);
        } else {
            client.sendAndConsume(packet);
        }
    }

    public static void captureView(Client client, String viewRoot, String view,
            CaptureViewHandler handler) throws IOException {
        sendJdwpPacket(client, CHUNK_VUCV, viewRoot, view, handler);
    }

    public static class CaptureViewHandler extends ViewDumpHandler {
        private AtomicReference<byte[]> mData = new AtomicReference<byte[]>();

        @Override
        void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId) {
            if (type != CHUNK_VUCV) {
                handleUnknownChunk(client, type, data, isReply, msgId);
                return;
            }

            byte[] b = new byte[data.remaining()];
            data.get(b);
            mData.set(b);
            resultReceived();
        }

        public byte[] getData(long timeout, TimeUnit unit) {
            waitForResult(timeout, unit);
            return mData.get();
        }
    }

    public static void invalidateView(Client client, String viewRoot, String view)
            throws IOException {
        sendJdwpPacket(client, CHUNK_VUIV, viewRoot, view, null);
    }

    public static void requestLayout(Client client, String viewRoot, String view)
            throws IOException {
        sendJdwpPacket(client, CHUNK_VULT, viewRoot, view, null);
    }

    public static void dumpDisplayList(Client client, String viewRoot, String view)
            throws IOException {
        sendJdwpPacket(client, CHUNK_VUDL, viewRoot, view, null);
    }

    @Override
    public void handleChunk(Client client, int type, ByteBuffer data,
            boolean isReply, int msgId) {
    }
}

