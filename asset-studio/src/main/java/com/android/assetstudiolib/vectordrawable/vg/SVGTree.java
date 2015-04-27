/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.assetstudiolib.vectordrawable.vg;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represent the SVG file in an internal data structure as a tree.
 */
public class SVGTree {
    private static Logger logger = Logger.getLogger(SVGTree.class.getSimpleName());

    public float w;
    public float h;
    public float[] matrix;
    public float[] viewBox;
    public float mScaleFactor = 1;
    private String mFileName;

    private SVGGroupNode mRoot;

    public SVGTree(String name) {
        mFileName = name;
    }

    public void normalize() {
        if (!(matrix[0] == 1 && matrix[3] == 1 && matrix[1] == 0 && matrix[2] == 0
                && matrix[4] == 0 && matrix[5] == 0)) {
            transform(matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
        }

        if (viewBox != null && (viewBox[0] != 0 || viewBox[1] != 0)) {
            transform(1, 0, 0, 1, -viewBox[0], -viewBox[1]);
        }
        logger.log(Level.FINE, "matrix=" + Arrays.toString(matrix));
    }

    void transform(float a, float b, float c, float d, float e, float f) {
        transformRecursively(mRoot, a, b, c, d, e, f);
    }

    private void transformRecursively(SVGGroupNode groupNode, float a, float b,
                                      float c, float d, float e, float f) {
        for (Object p : groupNode.mChildren) {
            if (p instanceof SVGLeaveNode) {
                SVGLeaveNode leave = (SVGLeaveNode) p;

                if ("none".equals(leave.prop.get("fill")) || leave.d == null) {
                    continue;
                }
                // TODO: We need to just apply the transformation to group.
                VDPath.Node[] n = VDParser.parsePath(leave.d);
                VDPath.Node.transform(
                        a, b, c, d, e, f,
                        n);
                leave.d = VDPath.Node.NodeListToString(n);
            }
        }
    }

    public void setScaleFactor(float scaleFactor) {
        mScaleFactor = scaleFactor;
    }

    public void print(SVGGroupNode root) {
        printSVGTree(root, 0);
    }

    private static void printSVGTree(SVGGroupNode currentGroup, int level) {
        String indent = "";
        for (int i = 0; i < level; i++) {
            indent += "    ";
        }
        // Print the current node
        logger.log(Level.FINE, indent + "current group is :" + currentGroup.getName());

        // Then print all the children groups
        for (int i = 0; i < currentGroup.mChildren.size(); i++) {
            Object child = currentGroup.mChildren.get(i);
            if (child instanceof SVGGroupNode) {
                printSVGTree((SVGGroupNode) child, level + 1);
            } else if (child instanceof SVGLeaveNode) {
                ((SVGLeaveNode) child).print(indent);
            }
        }
    }

    public void setRoot(SVGGroupNode root) {
        mRoot = root;
    }

    public SVGGroupNode getRoot() {
        return mRoot;
    }

}
