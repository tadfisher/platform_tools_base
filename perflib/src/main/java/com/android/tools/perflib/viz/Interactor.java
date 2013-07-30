package com.android.tools.perflib.viz;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public interface Interactor {
    void mousePressed(MouseEvent e);
    void mouseDragged(MouseEvent e);
    void mouseReleased(MouseEvent e);
    void mouseWheelMoved(MouseWheelEvent e);
}
