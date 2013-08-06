package com.android.tools.perflib.vmtrace.viz;

import com.android.tools.perflib.viz.Renderer;
import com.android.tools.perflib.viz.VisualItem;
import com.android.tools.perflib.vmtrace.Call;
import com.android.tools.perflib.vmtrace.MethodInfo;
import com.android.tools.perflib.vmtrace.VmTraceData;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class CallRenderer implements Renderer<Call> {
    private static final int FONT_HEIGHT = 10;
    private static final int LEFT_MARGIN = 5;

    private final VmTraceData mTraceData;

    Point2D.Float mSrc = new Point2D.Float();
    Point2D.Float mDst = new Point2D.Float();

    @SuppressWarnings("FieldCanBeLocal") // Make it a field to avoid allocation
    private final Rectangle mBounds = new Rectangle();

    public CallRenderer(VmTraceData traceData) {
        mTraceData = traceData;
    }

    @Override
    public boolean render(Graphics2D g, VisualItem<Call> item, AffineTransform tx) {
        item.fillLayoutBounds(mBounds);

        double widthOnScreen = g.getTransform().getScaleX() * mBounds.width;
        if (widthOnScreen < 1) {
            return false;
        }

        Call c = item.getSource();
        g.setColor(item.getFill());

        g.fillRect(mBounds.x, mBounds.y, mBounds.width, mBounds.height);

        g.setColor(Color.BLACK);
        g.drawRect(mBounds.x, mBounds.y, mBounds.width, mBounds.height);

        if (c == null) {
            return true;
        }

        AffineTransform origTx = g.getTransform();

        mSrc.x = mBounds.x + LEFT_MARGIN;
        mSrc.y = mBounds.y + FONT_HEIGHT;
        origTx.transform(mSrc, mDst);

        g.setTransform(new AffineTransform());
        g.setFont(g.getFont().deriveFont(8.0f));

        long methodId = c.getMethodId();
        MethodInfo info = mTraceData.getMethod(methodId);
        String s = info.getShortName();
        if (widthOnScreen > g.getFontMetrics().stringWidth(s)) {
            g.drawString(s, mDst.x, mDst.y);
        }

        g.setTransform(origTx);
        return true;
    }
}
