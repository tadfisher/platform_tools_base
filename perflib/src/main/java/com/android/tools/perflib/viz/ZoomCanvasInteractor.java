package com.android.tools.perflib.viz;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class ZoomCanvasInteractor implements Interactor {
    /**
     * The values from {@link java.awt.event.MouseWheelEvent#getWheelRotation()} are quite high even
     * for a small amount of scrolling. This is an arbitrary scale factor used to go from the wheel
     * rotation value to a zoom by factor. The scale is negated to take care of the common
     * expectation that scrolling down should zoom out, not zoom in.
     */
    public static final double WHEEL_UNIT_SCALE = -0.1;

    private final boolean mZoomVertical;
    private final boolean mZoomHorizontal;

    private final Point mLocation = new Point();

    public ZoomCanvasInteractor(boolean zoomHorizontal, boolean zoomVertical) {
        mZoomHorizontal = zoomHorizontal;
        mZoomVertical = zoomVertical;
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) {
            return;
        }

        mLocation.x = e.getX();
        mLocation.y = e.getY();

        double scale = WHEEL_UNIT_SCALE * e.getWheelRotation();

        double scaleX = mZoomHorizontal ? scale : 0;
        double scaleY = mZoomVertical ? scale : 0;

        VisualizationCanvas canvas = (VisualizationCanvas) e.getComponent();
        canvas.zoomBy(scaleX, scaleY, mLocation);
    }
}
