package com.android.tools.perflib.viz;

import java.util.Collection;

public abstract class VisualItemFactory<D> {
    public abstract Collection<VisualItem<D>> create(D item);
}
