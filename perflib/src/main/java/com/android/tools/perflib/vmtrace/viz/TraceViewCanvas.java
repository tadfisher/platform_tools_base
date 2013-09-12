/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.tools.perflib.vmtrace.viz;

import com.android.annotations.NonNull;
import com.android.tools.perflib.vmtrace.Call;
import com.android.tools.perflib.vmtrace.ClockType;
import com.android.tools.perflib.vmtrace.ThreadInfo;
import com.android.tools.perflib.vmtrace.VmTraceData;
import com.google.common.collect.ImmutableList;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

/**
 * A canvas that displays the call hierarchy for a single thread. The trace and the the thread to be
 * displayed are specified using {@link #setTrace(com.android.tools.perflib.vmtrace.VmTraceData,
 * String, ClockType)} and {@link #displayThread(String)} methods.
 */
public class TraceViewCanvas extends JComponent {
    private static final Color BACKGROUND_COLOR =
            UIManager.getLookAndFeelDefaults().getColor("EditorPane.background");
    private static final int TOOLTIP_OFFSET = 10;

    /**
     * Interactor that listens to mouse events, interprets them as zoom/pan events, and provides the
     * resultant viewport transform.
     */
    private final ZoomPanInteractor mZoomPanInteractor;

    /** The viewport transform takes into account the current zoom and translation/pan values. */
    private AffineTransform mViewPortTransform;

    /** Inverse of {@link #mViewPortTransform}. */
    private AffineTransform mViewPortInverseTransform;

    private VmTraceData mTraceData;
    private Call mTopLevelCall;

    private TimeScaleRenderer mTimeScaleRenderer;
    private CallHierarchyRenderer mCallHierarchyRenderer;

    private final Point mTmpPoint = new Point();

    private ClockType mRenderClock;

    public TraceViewCanvas() {
        mViewPortTransform = new AffineTransform();
        mViewPortInverseTransform = new AffineTransform();

        mZoomPanInteractor = new ZoomPanInteractor();
        addMouseListener(mZoomPanInteractor);
        addMouseMotionListener(mZoomPanInteractor);
        addMouseWheelListener(mZoomPanInteractor);

        mZoomPanInteractor.addViewTransformListener(new ZoomPanInteractor.ViewTransformListener() {
            @Override
            public void transformChanged(@NonNull AffineTransform transform) {
                updateViewPortTransform(transform);
            }
        });

        addMouseMotionListener(ToolTipManager.sharedInstance());

        // Listen for the first hierarchy bounds change so as to get the initial width.
        // Zoom fit if possible once we know the width.
        addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            @Override
            public void ancestorMoved(HierarchyEvent e) {
            }

            @Override
            public void ancestorResized(HierarchyEvent e) {
                removeHierarchyBoundsListener(this);
                zoomFit(TimeUnit.NANOSECONDS);
                //zoomFit(getHighestResolutionTimeUnit(mTopLevelCall));
            }
        });
    }

    public void setTrace(@NonNull VmTraceData traceData, @NonNull String threadName,
            ClockType renderClock) {
        mTraceData = traceData;
        displayThread(threadName);
        setRenderClock(renderClock);
    }

    public void displayThread(@NonNull String threadName) {
        mCallHierarchyRenderer = null;
        mTimeScaleRenderer = null;

        if (mTraceData == null) {
            return;
        }

        ThreadInfo thread = mTraceData.getThread(threadName);
        if (thread == null) {
            return;
        }

        mTopLevelCall = thread.getTopLevelCall();
        if (mTopLevelCall == null) {
            return;
        }

        TimeUnit u = getHighestResolutionTimeUnit(mTopLevelCall);
        u = TimeUnit.NANOSECONDS;

        mTimeScaleRenderer = new TimeScaleRenderer(
                mTopLevelCall.getEntryTime(ClockType.GLOBAL, u), u);
        int yOffset = mTimeScaleRenderer.getLayoutHeight();
        mCallHierarchyRenderer = new CallHierarchyRenderer(mTraceData, threadName, yOffset,
                mRenderClock);
        mCallHierarchyRenderer.setLayoutTimeUnit(u);

        zoomFit(u);
    }

    public void setRenderClock(ClockType clock) {
        mRenderClock = clock;
        if (mCallHierarchyRenderer != null) {
            mCallHierarchyRenderer.setRenderClock(clock);
            repaint();
        }
    }

    /**
     * Returns the highest resolution {@link TimeUnit} to use so that the time values still
     * fit within an integer range.
     *
     * All the layout and rendering operations are performed using integers. Ideally, we want to use
     * the highest resolution time units (nanoseconds) wherever possible since that would allow for
     * smooth panning and zooming even when you are viewing a trace of just a few microseconds.
     *
     * However, when viewing larger traces, use of nano seconds would cause an overflow if the
     * layout operations store values in integer types. So this method returns the highest
     * resolution time unit to use given the start and end times of the top level call.
     *
     * TODO Currently, we don't dynamically change the time unit used as the user zooms in or out.
     * Ideally, if the initial display was in milliseconds, and then we zoom in to view just a few
     * microseconds, the display should change the timeunits from ms to ns.
     */
    private TimeUnit getHighestResolutionTimeUnit(Call c) {
        final int MAX_ZOOM_FACTOR = 10;

        List<TimeUnit> timeunits = ImmutableList.of(TimeUnit.NANOSECONDS, TimeUnit.MICROSECONDS,
                TimeUnit.MILLISECONDS);

        for (TimeUnit unit : timeunits) {
            long start = c.getEntryTime(ClockType.GLOBAL, unit);
            long end = c.getExitTime(ClockType.GLOBAL, unit);
            long diff = end - start;
            long max = Math.max(end, diff);

            if (max * MAX_ZOOM_FACTOR < Integer.MAX_VALUE) {
                return unit;
            }
        }

        return TimeUnit.MILLISECONDS;
    }

    private void zoomFit(TimeUnit u) {
        if (mTopLevelCall == null) {
            return;
        }

        long start = mTopLevelCall.getEntryTime(ClockType.GLOBAL, u);
        long end = mTopLevelCall.getExitTime(ClockType.GLOBAL, u);

        // Scale so that the full trace occupies 90% of the screen width.
        double width = getWidth();
        double sx = width * .9f / (end - start);

        // Guard against trying to zoom when the component doesn't know its width yet. Width is
        // usually 0 in such cases, but we just make it slightly general and check for width < 10.
        if (width < 10) {
            sx = Math.max(sx, 0.2);
        }

        // Initialize display so that the full trace is visible and takes up most of the view.
        mZoomPanInteractor.setToScaleX(sx, 1); // make everything visible
        mZoomPanInteractor.translateBy(50, 0); // shift over the start of the trace
        updateViewPortTransform(mZoomPanInteractor.getTransform());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        setRenderingHints(g2d);

        // fill with background color
        g2d.setColor(BACKGROUND_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (mTraceData == null) {
            return;
        }

        // Obtain the current screen transformation for this component. This transformation
        // could be changed without any events being generated (e.g. screen transformation is
        // changed when a tooltip is displayed inside a container
        AffineTransform screenTransform = g2d.getTransform();

        // set the viewport * screen space transform
//        AffineTransform transform = new AffineTransform(screenTransform);
//        transform.concatenate(mViewPortTransform);
//        g2d.setTransform(transform);

        // paint stack layout view
        if (mCallHierarchyRenderer != null) {
            mCallHierarchyRenderer.render(g2d, mViewPortTransform);
        }

        // paint timeline at top
        if (mTimeScaleRenderer != null) {
            mTimeScaleRenderer.paint(g2d, screenTransform, mViewPortTransform, getWidth());
        }
    }

    private void setRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private final Point2D mTmpPoint2 = new Point2D.Double();

    @Override
    public String getToolTipText(MouseEvent event) {
        if (mTraceData == null || mCallHierarchyRenderer == null) {
            return null;
        }

        mTmpPoint2.setLocation(event.getPoint());
        mViewPortInverseTransform.transform(mTmpPoint2, mTmpPoint2);
        return mCallHierarchyRenderer.getToolTipFor(mTmpPoint2.getX(), mTmpPoint2.getY());
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        return new Point(event.getX() + TOOLTIP_OFFSET, event.getY() + TOOLTIP_OFFSET);
    }

    private void updateViewPortTransform(AffineTransform tx) {
        mViewPortTransform = new AffineTransform(tx);

        try {
            mViewPortInverseTransform = mViewPortTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            // This can't occur since we just do scale or pan, both of which are invertible
            mViewPortInverseTransform = new AffineTransform();
        }

        repaint();
    }
}
