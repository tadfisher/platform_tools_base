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
import com.google.common.collect.Lists;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;

public class TraceViewVisualizationTestApplication {
    public static void main(String[] args) {
        VmTraceData traceData = getVmTraceData("/play.dalvik.trace");
        PanCanvasInteractor panCanvasInteractor = new PanCanvasInteractor(true, true);
        ZoomCanvasInteractor zoomCanvasInteractor = new ZoomCanvasInteractor(true, false);

//        VisualizationCanvas timeLine = createTimeLine(traceData.getLowestMethodEntryTime(),
//                traceData.getHighestMethodExitTime());
        VisualizationCanvas chartCanvas = createFlameChartVisualization(traceData, panCanvasInteractor,
                zoomCanvasInteractor);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        //Button button = new Button("Test");
        //panel.add(button);
        panel.add(chartCanvas);

        JFrame frame = new JFrame("TraceViewTestApplication");
        frame.setLayout(new BorderLayout());
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.setSize(1500, 1000);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private static VisualizationCanvas createFlameChartVisualization(VmTraceData traceData,
            PanCanvasInteractor panCanvasInteractor, ZoomCanvasInteractor zoomCanvasInteractor) {
        TraceCallVisualItemFactory factory = new TraceCallVisualItemFactory(traceData);
        Visualization<Call> vis = new Visualization<Call>(factory);

        int threadId = findThreadId("main", traceData);
        assert threadId >= 0;
        Call c = traceData.getTopLevelCall(threadId);
        assert c != null;

        vis.setData(Collections.singletonList(c));

        TraceViewFlameChartLayout layout = new TraceViewFlameChartLayout(c.getEntryGlobalTime());
        vis.addAction(layout);
        vis.runActions();

        vis.addRenderer(TraceCallVisualItemFactory.CATEGORY_METHODCALL, new CallRenderer(traceData));

        VisualizationCanvas canvas = new VisualizationCanvas<Call>(vis);
        canvas.addInteractor(panCanvasInteractor);
        canvas.addInteractor(zoomCanvasInteractor);
        return canvas;
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
        private static final int MARGIN = 1;
        private final long mStart;

        public TraceViewFlameChartLayout(long start) {
            mStart = start;
        }

        @Override
        protected int getWidth(VisualItem<Call> item) {
            Call c = item.getSource();
            return c == null ? 0 : (int) c.getInclusiveGlobalTime() - 2 * MARGIN;
        }

        @Override
        protected int getStartLocation(VisualItem<Call> item) {
            Call c = item.getSource();
            return c == null ? 0 : (int) (c.getEntryGlobalTime() - mStart) + MARGIN;
        }

        @Override
        protected int getStackLevel(VisualItem<Call> item) {
            Call c = item.getSource();
            return c == null ? 0 : c.getDepth();
        }
    }
}
