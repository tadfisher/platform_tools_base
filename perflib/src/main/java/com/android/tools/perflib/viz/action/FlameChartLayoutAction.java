package com.android.tools.perflib.viz.action;

import com.android.tools.perflib.viz.VisualItem;

import java.awt.*;
import java.util.Collection;

public abstract class FlameChartLayoutAction<D> implements Action<D> {
    private static final int PER_LEVEL_HEIGHT_PX = 15;

    @Override
    public void run(Collection<VisualItem<D>> items, double frac) {
        for (VisualItem<D> item : items) {
            int depth = getStackLevel(item);
            int start = getStartLocation(item);
            int width = getWidth(item);

            // stack going down, item at depth 0 is at the top
            int y = depth * PER_LEVEL_HEIGHT_PX;

            item.setLayoutBounds(start, y, width, PER_LEVEL_HEIGHT_PX);
        }
    }

    protected abstract int getWidth(VisualItem<D> item);
    protected abstract int getStartLocation(VisualItem<D> item);
    protected abstract int getStackLevel(VisualItem<D> item);
}
