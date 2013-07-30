package com.android.tools.perflib.viz;

import com.android.annotations.NonNull;
import com.android.tools.perflib.viz.action.Action;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Visualization<D> {
    private final VisualItemFactory<D> mFactory;

    private List<Action<D>> mActions = Lists.newArrayList();
    private List<VisualItem<D>> mVisualItems = new ArrayList<VisualItem<D>>();
    private final Map<String, Renderer<D>> mRenderersByCategory = Maps.newHashMap();

    public Visualization(@NonNull VisualItemFactory<D> f) {
        mFactory = f;
    }

    public void setData(Collection<D> dataSet) {
        mVisualItems = new ArrayList<VisualItem<D>>();
        for (D data : dataSet) {
            mVisualItems.addAll(mFactory.create(data));
        }
        mVisualItems = new ImmutableList.Builder<VisualItem<D>>().addAll(mVisualItems).build();
    }

    public List<VisualItem<D>> getVisualItems() {
        return mVisualItems;
    }

    public void addAction(Action<D> action) {
        mActions.add(action);
    }

    public void runActions() {
        for (Action<D> a : mActions) {
            a.run(mVisualItems, 1.0f);
        }
    }

    public void addRenderer(String visualItemCategory, Renderer<D> renderer) {
        mRenderersByCategory.put(visualItemCategory, renderer);
    }

    public Renderer<D> getRenderer(String category) {
        return mRenderersByCategory.get(category);
    }
}
