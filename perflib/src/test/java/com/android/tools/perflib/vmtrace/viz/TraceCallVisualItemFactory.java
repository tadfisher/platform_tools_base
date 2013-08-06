package com.android.tools.perflib.vmtrace.viz;

import com.android.tools.perflib.viz.VisualItem;
import com.android.tools.perflib.viz.VisualItemFactory;
import com.android.tools.perflib.vmtrace.Call;
import com.android.tools.perflib.vmtrace.MethodInfo;
import com.android.tools.perflib.vmtrace.VmTraceData;
import com.google.common.collect.Lists;

import java.awt.*;
import java.util.Collection;
import java.util.List;

public class TraceCallVisualItemFactory extends VisualItemFactory<Call> {
    public static final String CATEGORY_METHODCALL = "methodcall";

    private final VmTraceData mTraceData;

    public TraceCallVisualItemFactory(VmTraceData traceData) {
        mTraceData = traceData;
    }

    @Override
    public Collection<VisualItem<Call>> create(Call item) {
        List<VisualItem<Call>> visualItems = Lists.newArrayList();
        createVisualItems(item, visualItems);
        return visualItems;
    }

    /**
     * Traverses through the entire call hierarchy rooted at the given call,
     * creates a {@link VisualItem} corresponding to each of the methods in the hierarchy,
     * and accumulates them in the provided list of {@link VisualItem}s.
     */
    private void createVisualItems(Call call, List<VisualItem<Call>> visualItems) {
        MethodInfo info = mTraceData.getMethod(call.getMethodId());
        VisualItem<Call> visualItem = new VisualItem<Call>(call, CATEGORY_METHODCALL);
        int percent = quantize(info.getInclusiveThreadPercent());
        visualItem.setFill(getFill(percent));
        visualItems.add(visualItem);
        for (Call c : call.getCallees()) {
            createVisualItems(c, visualItems);
        }
    }

    private static final Color[] QUANTIZED_COLORS = {
        new Color(247,251,255),
        new Color(222,235,247),
        new Color(198,219,239),
        new Color(158,202,225),
        new Color(107,174,214),
        new Color(66,146,198),
        new Color(33,113,181),
        new Color(8,81,156),
        new Color(8,48,107),
    };

    private Color getFill(int percent) {
        int i = percent / 10;
        if (i >= QUANTIZED_COLORS.length) {
            i = QUANTIZED_COLORS.length - 1;
        }
        return QUANTIZED_COLORS[i];
    }

    private int quantize(float inclusiveThreadPercent) {
        return ((int)(inclusiveThreadPercent + 9) / 10) * 10;
    }
}
