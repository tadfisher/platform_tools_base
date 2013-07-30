package com.android.tools.perflib.viz.action;

import com.android.tools.perflib.viz.VisualItem;

import java.util.Collection;

public interface Action<D> {
    public void run(Collection<VisualItem<D>> items, double frac);
}
