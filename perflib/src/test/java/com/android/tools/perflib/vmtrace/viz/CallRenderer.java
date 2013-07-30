package com.android.tools.perflib.vmtrace.viz;

import com.android.tools.perflib.viz.Renderer;
import com.android.tools.perflib.viz.VisualItem;
import com.android.tools.perflib.vmtrace.Call;
import com.android.tools.perflib.vmtrace.MethodInfo;
import com.android.tools.perflib.vmtrace.VmTraceData;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class CallRenderer implements Renderer<Call> {
    private final VmTraceData mTraceData;

    public CallRenderer(VmTraceData traceData) {
        mTraceData = traceData;
    }

    @Override
    public void render(Graphics2D g, VisualItem<Call> item, AffineTransform tx) {
        Rectangle r = item.getLayoutBounds();
        g.drawRoundRect(r.x, r.y, r.width, r.height, 3, 3);

        Call c = item.getSource();
        if (c == null) {
            return;
        }

        long methodId = c.getMethodId();
        MethodInfo info = mTraceData.getMethod(methodId);

        String s = info.getName();
        if (r.width > g.getFontMetrics().stringWidth(s)) {
            g.setFont(g.getFont().deriveFont(8.0f));
            g.drawString(info.getName(), r.x + 10, r.y + 10);
        }
    }
}
