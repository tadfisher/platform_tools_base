package com.android.tools.perflib.viz;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public class VisualizationCanvas<D> extends JComponent {
    private final Visualization<D> mViz;

    private final InputListener mInputListener = new InputListener();

    private BufferedImage mBackBuffer;

    private AffineTransform mTransform = new AffineTransform();
    private int mPanX, mPanY;
    private double mScaleX = 1;
    private double mScaleY = 1;

    public VisualizationCanvas(Visualization<D> viz) {
        mViz = viz;

        addMouseListener(mInputListener);
        addMouseMotionListener(mInputListener);
        addMouseWheelListener(mInputListener);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);

        mBackBuffer = null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        if (mBackBuffer == null) {
            mBackBuffer = createBackBuffer();
        }

        // paint back buffer back to screen
        mTransform.setToTranslation(mPanX, mPanY);
        mTransform.scale(mScaleX, mScaleY);

        setRenderingHints(g2d);
        g2d.drawImage(mBackBuffer, 0, 0, null);

        // from a list of visual items, get the list of items to be rendered at the given transform

        // then render those items
        g2d.setTransform(mTransform);
        g2d.setColor(Color.GRAY);
        for (VisualItem<D> item : mViz.getVisualItems()) {
            if (g2d.getClipBounds().intersects(item.getLayoutBounds())) {
                Renderer<D> renderer = mViz.getRenderer(item.getCategory());
                renderer.render(g2d, item, null);
            }
        }

        drawDebugGraphics(g2d);
    }

    private void drawDebugGraphics(Graphics2D g2d) {
        g2d.transform(mTransform);
        g2d.drawString("Hello World", 50, 50);
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.setColor(Color.GRAY);
        g2d.drawLine(0, 0, 400, 400);
        g2d.drawLine(0, 0, 400, 0);
        g2d.drawRect(10, 20, 50, 200);
    }

    private void setRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private BufferedImage createBackBuffer() {
        BufferedImage image;
        try {
            image = (BufferedImage) createImage(getWidth(), getHeight());
        } catch (Exception e) {
            image = null;
        }

        if (image == null) {
            image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        }

        return image;
    }

    public void addInteractor(Interactor i) {
        mInputListener.addInteractor(i);
    }

    public void pan(int deltaX, int deltaY) {
        mPanX += deltaX;
        mPanY += deltaY;
        repaint();
    }

    public void zoomBy(double scaleX, double scaleY) {
        mScaleX += scaleX;
        mScaleY += scaleY;

        // don't use a zero or negative scale
        mScaleX = Math.max(0.0001, mScaleX);
        mScaleY = Math.max(0.0001, mScaleY);

        repaint();
    }

    private class InputListener implements MouseListener, MouseMotionListener, MouseWheelListener {
        private final List<Interactor> mInteractors = new ArrayList<Interactor>();

        public void addInteractor(Interactor i) {
            mInteractors.add(i);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            for (Interactor i : mInteractors) {
                i.mousePressed(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            for (Interactor i : mInteractors) {
                i.mouseReleased(e);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            for (Interactor i : mInteractors) {
                i.mouseDragged(e);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            for (Interactor i : mInteractors) {
                i.mouseWheelMoved(e);
            }
        }
    }
}
