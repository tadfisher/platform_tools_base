package com.android.tools.perflib.viz;

import com.android.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Visualization<D> {
    private final String mName;
    private final VisualItemFactory<D> mFactory;

    private final List<VisualItem<D>> mVisualItems = new ArrayList<VisualItem<D>>();

    public Visualization(String name, @NonNull VisualItemFactory<D> f) {
        mName = name;
        mFactory = f;
    }

    public String getName() {
        return mName;
    }

    public void setData(Collection<D> dataSet) {
        for (D data : dataSet) {
            mVisualItems.addAll(mFactory.create(data));
        }
    }

    public List<VisualItem<D>> getVisualItems() {
        return mVisualItems;
    }
}
