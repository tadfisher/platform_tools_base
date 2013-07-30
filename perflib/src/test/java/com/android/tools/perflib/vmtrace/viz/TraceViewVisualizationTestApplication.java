package com.android.tools.perflib.vmtrace.viz;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import com.android.tools.perflib.viz.PanCanvasInteractor;
import com.android.tools.perflib.viz.VisualItem;
import com.android.tools.perflib.viz.Visualization;
import com.android.tools.perflib.viz.VisualizationCanvas;
import com.android.tools.perflib.viz.ZoomCanvasInteractor;
import com.android.tools.perflib.viz.action.FlameChartLayoutAction;
import com.android.tools.perflib.vmtrace.Call;
import com.android.tools.perflib.vmtrace.VmTraceData;
import com.android.tools.perflib.vmtrace.VmTraceParser;
import com.android.utils.SparseArray;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.swing.*;

public class TraceViewVisualizationTestApplication {
    public static void main(String[] args) {
        VmTraceData traceData = getVmTraceData("/play.dalvik.trace");
        TraceCallVisualItemFactory factory = new TraceCallVisualItemFactory(traceData);

        Visualization<Call> vis = new Visualization<Call>("flamechart", factory);

        TraceViewFlameChartLayout layout = new TraceViewFlameChartLayout();
        vis.addAction(layout);

        int threadId = findThreadId("main", traceData);
        if (threadId != -1) {
            vis.setData(traceData.getCalls(threadId));
        }
        vis.runActions();

        vis.addRenderer(TraceCallVisualItemFactory.CATEGORY_METHODCALL, new CallRenderer(traceData));

        VisualizationCanvas canvas = new VisualizationCanvas(vis);
        canvas.addInteractor(new PanCanvasInteractor());
        canvas.addInteractor(new ZoomCanvasInteractor(true, true));

        JFrame frame = new JFrame("TraceViewTestApplication");
        frame.setLayout(new BorderLayout());
        frame.getContentPane().add(canvas, BorderLayout.CENTER);
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    private static int findThreadId(String threadName, VmTraceData traceData) {
        SparseArray<String> threads = traceData.getThreads();
        for (int i = 0; i < threads.size(); i++) {
            if (threadName.equals(threads.valueAt(i))) {
                return threads.keyAt(i);
            }
        }
        return -1;
    }

    private static VmTraceData getVmTraceData(String tracePath) {
        VmTraceParser parser = new VmTraceParser(getFile(tracePath));
        try {
            parser.parse();
        } catch (IOException e) {
            fail("Unexpected error while reading tracing file: " + tracePath);
        }

        return parser.getTraceData();
    }

    private static File getFile(String path) {
        URL resource = TraceViewVisualizationTestApplication.class.getResource(path);
        // Note: When running from an IntelliJ, make sure the IntelliJ compiler settings treats
        // *.trace files as resources, otherwise they are excluded from compiler output
        // resulting in a NPE.
        assertNotNull(path + " not found", resource);
        return new File(resource.getFile());
    }

    private static class TraceViewFlameChartLayout extends FlameChartLayoutAction<Call> {
        @Override
        protected int getWidth(VisualItem<Call> item) {
            Call c = item.getSource();
            return c == null ? 0 : c.getExitThreadTime() - c.getEntryThreadTime();
        }

        @Override
        protected int getStartLocation(VisualItem<Call> item) {
            Call c = item.getSource();
            return c == null ? 0 : c.getEntryThreadTime();
        }

        @Override
        protected int getStackLevel(VisualItem<Call> item) {
            Call c = item.getSource();
            return c == null ? 0 : c.getDepth();
        }
    }
}
