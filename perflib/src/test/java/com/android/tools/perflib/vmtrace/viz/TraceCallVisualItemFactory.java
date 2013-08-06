package com.android.tools.perflib.vmtrace.viz;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.perflib.viz.VisualItem;
import com.android.tools.perflib.viz.VisualItemFactory;
import com.android.tools.perflib.vmtrace.Call;
import com.android.tools.perflib.vmtrace.MethodInfo;
import com.android.tools.perflib.vmtrace.VmTraceData;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

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
        info.getInclusiveThreadTimes();
        visualItems.add(visualItem);
        for (Call c : call.getCallees()) {
            createVisualItems(c, visualItems);
        }
    }

    @NonNull
    private String formatMethodName(@Nullable MethodInfo info, long methodId) {
        if (info == null) {
            return Long.toString(methodId);
        }

        return String.format(Locale.US, "%s : %s", info.methodName, info.signature);
    }
}
