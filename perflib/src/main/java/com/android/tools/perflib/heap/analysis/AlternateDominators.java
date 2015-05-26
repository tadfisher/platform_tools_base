
package com.android.tools.perflib.heap.analysis;

import com.android.annotations.NonNull;
import com.android.tools.perflib.heap.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.List;

/**
 * Alternative algorithm for computing dominators.
 *
 */
public class AlternateDominators {
    @NonNull
    private final Snapshot mSnapshot;

    private static class PathElem {
        public Instance object;
        public String field;

        public PathElem(Instance obj, String fld) {
            object = obj;
        field = fld;
      }
    }

    public AlternateDominators(@NonNull Snapshot snapshot) {
        mSnapshot = snapshot;
    }

    // Path: ArrayList<PathElem>, the canonical path from the root to the
    // object.
    private final static String PATH_ATTRIBUTE = "AlternateDominator.path";

    static private void putPath(Instance inst, ArrayList<PathElem> path) {
        inst.putAttribute(PATH_ATTRIBUTE, path);
    }

    static private ArrayList<PathElem> getPath(Instance inst) {
        return (ArrayList<PathElem>)inst.getAttribute(PATH_ATTRIBUTE);
    }

    // Compute for each object:
    //  * Path: A canonical path from the root to the object.
    //  * Dominator: The immediate dominator of this object.
    //
    // This visitor assumes all instances have a single common root that is a
    // dominator for all other objects.
    private static class DominatorVisitor extends BreadthFirstVisitor {
        @Override
        public void visitRootObj(RootObj root) {
            processReference(root, root.getReferredInstance(), ".");
        }

        @Override
        public void visitClassInstance(ClassInstance instance) {
            for (Map.Entry<Field, Object> entry : instance.getValues().entrySet()) {
                if (entry.getValue() instanceof Instance) {
                    processReference(instance,
                            (Instance)entry.getValue(), entry.getKey().getName());
                }
            }
        }

        @Override
        public void visitArrayInstance(ArrayInstance instance) {
            Object[] values = instance.getValues();
            for (int i = 0; i < values.length; i++) {
                if (values[i] instanceof Instance) {
                    processReference(instance, (Instance)values[i], Integer.toString(i));
                }
            }
        }

        @Override
        public void visitClassObj(ClassObj instance) {
            for (Map.Entry<Field, Object> entry : instance.getStaticFieldValues().entrySet()) {
                if (entry.getValue() instanceof Instance) {
                    processReference(instance,
                            (Instance)entry.getValue(), entry.getKey().getName());
                }
            }
        }

        private void processReference(Instance src, Instance dst, String field) {
            if (dst == null) {
                return;
            }

            ArrayList<PathElem> srcpath = getPath(src);
            if (srcpath == null) {
                String err = String.format("srcpath is null for @%x #%x with references:\n",
                        src.getId(), src.hashCode());
                for (Instance ref : src.getReferences()) {
                    err += String.format("@%x #%x\n", ref.getId(), ref.hashCode());
                }
                throw new RuntimeException(err);
            }

            if (srcpath.isEmpty()) {
                String err = String.format("srcpath is empty for @%x #%x with references:\n",
                        src.getId(), src.hashCode());
                for (Instance ref : src.getReferences()) {
                    err += String.format("@%x #%x\n", ref.getId(), ref.hashCode());
                }
                throw new RuntimeException(err);
            }

            ArrayList<PathElem> dstpath = getPath(dst);
            if (dstpath == null) {
                // We haven't seen this dst node before. Set its path and its
                // dominator.
                dstpath = new ArrayList<PathElem>();
                for (PathElem elem : srcpath) {
                    dstpath.add(elem);
                }
                dstpath.add(new PathElem(src, field));
                putPath(dst, dstpath);
                dst.setImmediateDominator(src);
            } else {
                // We have seen this dst node before. No need to set its path, but we
                // need to update its dominator.
                Instance olddom = dst.getImmediateDominator();
                ArrayList<PathElem> dompath = getPath(olddom);
                if (dompath == null) {
                    throw new RuntimeException(
                            String.format("dompath is null for @%x from @%x with path %s",
                                olddom.getId(), dst.getId(), dstpath.toString()));
                }

                Instance dominator = srcpath.get(0).object;
                for (int i = 0; i < dompath.size() && i < srcpath.size()
                        && dompath.get(i) == srcpath.get(i); i++) {
                    dominator = dompath.get(i).object;
                }
                dst.setImmediateDominator(dominator);
            }
        }
    }

    public void computeDominators() {
        Collection<RootObj> roots = mSnapshot.getGCRoots();

        // Run over the snapshot to initialize the references.
        NonRecursiveVisitor visitor = new NonRecursiveVisitor();
        visitor.doVisit(roots);

        // roots contains a bunch of dummy RootObj objects, but these clutter the
        // display, so get rid of them ahead of time.
        List<Instance> realroots = new ArrayList<Instance>();
        for (RootObj root : roots) {
            Instance realroot = root.getReferredInstance();
            if (realroot != null) {
                realroots.add(realroot);
            }
        }

        Instance sroot = Snapshot.SENTINEL_ROOT;
        putPath(sroot, new ArrayList<PathElem>());
        for (Instance root : realroots) {
            ArrayList<PathElem> path = new ArrayList<PathElem>();
            path.add(new PathElem(sroot, String.format("ROOT@%x", root.getId())));
            putPath(root, path);
            root.setImmediateDominator(sroot);
        }

        DominatorVisitor dominatorVisitor = new DominatorVisitor();
        dominatorVisitor.doVisit(realroots);
    }

    /**
     * Kicks off the computation of dominators and retained sizes.
     */
    public void computeRetainedSizes() {
        computeDominators();

        // Initialize retained sizes for all classes and objects, including unreachable ones.
        for (Heap heap : mSnapshot.getHeaps()) {
            for (Instance instance : Iterables.concat(heap.getClasses(), heap.getInstances())) {
                instance.resetRetainedSize();
            }
        }

        // Update the retained size based on retainers.
        for (Heap heap : mSnapshot.getHeaps()) {
            for (Instance node : Iterables.concat(heap.getClasses(), heap.getInstances())) {
                int heapIndex = mSnapshot.getHeapIndex(node.getHeap());
                // Add the size of the current node to the retained size of
                // every dominator up to the root, in the same heap.
                for (Instance dom = node.getImmediateDominator();
                        dom != null && dom != Snapshot.SENTINEL_ROOT;
                        dom = dom.getImmediateDominator()) {
                    dom.addRetainedSize(heapIndex, node.getSize());
                }
            }
        }
    }
}

