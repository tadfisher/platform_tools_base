package com.android.tools.perflib.viz;

import java.awt.*;
import java.awt.geom.AffineTransform;

public interface Renderer<D> {
    boolean render(Graphics2D g, VisualItem<D> item, AffineTransform tx);
}
