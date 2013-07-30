package com.android.tools.perflib.viz;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class PanCanvasInteractor implements Interactor {
    private int mLastX;
    private int mLastY;

    @Override
    public void mousePressed(MouseEvent e) {
        mLastX = e.getX();
        mLastY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int deltaX = e.getX() - mLastX;
        int deltaY = e.getY() - mLastY;

        mLastX = e.getX();
        mLastY = e.getY();

        VisualizationCanvas canvas = (VisualizationCanvas) e.getComponent();
        canvas.pan(deltaX, deltaY);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mLastX = mLastY = 0;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }
}
