package com.android.tools.perflib.viz;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class PanCanvasInteractor implements Interactor {
    private final boolean mPanHorizontal;
    private final boolean mPanVertical;

    private int mLastX;
    private int mLastY;

    public PanCanvasInteractor(boolean panHorizontal, boolean panVertical) {
        mPanHorizontal = panHorizontal;
        mPanVertical = panVertical;
    }

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
        canvas.pan(mPanHorizontal ? deltaX : 0, mPanVertical ? deltaY : 0);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mLastX = mLastY = 0;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }
}
