package com.android.tools.perflib.viz;

import com.android.tools.perflib.vmtrace.Call;

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

        // setOpaque(true);

        if (mBackBuffer == null) {
            mBackBuffer = createBackBuffer();
        }

        // paint back buffer back to screen
        mTransform.setToTranslation(mPanX + getX(), mPanY + getY());
        mTransform.scale(mScaleX, mScaleY);

        setRenderingHints(g2d);
        g2d.drawImage(mBackBuffer, 0, 0, null);

        // from a list of visual items, get the list of items to be rendered at the given transform

        // then render those items
        g2d.setTransform(mTransform);
        g2d.setColor(Color.GRAY);
        int count = 0;

        long minDisplayed = 0, minActual = 0;
        long maxDisplayed = 0, maxActual = 0;

        long startTime = System.currentTimeMillis();

        Rectangle itemBounds = new Rectangle();
        for (VisualItem<D> item : mViz.getVisualItems()) {
            Call c = (Call) item.getSource();
            if (c.getEntryThreadTime() < minActual) {
                minActual = c.getEntryThreadTime();
            } else if (c.getExitThreadTime() > maxActual) {
                maxActual = c.getExitThreadTime();
            }

            item.fillLayoutBounds(itemBounds);
            if (g2d.getClipBounds().intersects(itemBounds)) {
                Renderer<D> renderer = mViz.getRenderer(item.getCategory());
                if (renderer.render(g2d, item, null)) {
                    count++;
                }

                if (c.getEntryThreadTime() < minDisplayed) {
                    minDisplayed = c.getEntryThreadTime();
                } else if (c.getExitThreadTime() > maxDisplayed) {
                    maxDisplayed = c.getExitThreadTime();
                }
            }
        }

        //System.out.println("render time = " + (System.currentTimeMillis() - startTime));
        //System.out.println("# of items rendered = " + count);
        //System.out.printf("Displayed: %d - %d out of %d - %d\n", minDisplayed, maxDisplayed, minActual, maxActual);
    }

    private void setRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(300, 300);
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

        mPanY = Math.min(mPanY, 0);

        repaint();
    }

    public void zoomBy(double scaleX, double scaleY, Point location) {
        double origPanX = (mPanX - location.x) / mScaleX;
        double origPanY = (mPanY - location.y) / mScaleY;

        mScaleX *= scaleX;
        mScaleY *= scaleY;

        // don't use a zero or negative scale
        mScaleX = Math.max(0.00001, mScaleX);
        mScaleY = Math.max(0.00001, mScaleY);

        mPanX = (int)(origPanX * mScaleX) + location.x;
        mPanY = (int)(origPanY * mScaleY) + location.y;

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
