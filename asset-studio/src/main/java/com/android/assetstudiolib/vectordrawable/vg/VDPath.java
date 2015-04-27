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

import java.awt.geom.Path2D;
import java.util.Arrays;

/**
 * Used to represent one VectorDrawble's path element.
 */
public class VDPath {
    Node[] mNode = null;
    String mId;
    int mStrokeColor = 0;
    int mFillColor = 0;
    float mStrokeWidth = 0;
    float mRotate = 0;
    float mShiftX = 0;
    float mShiftY = 0;
    public float mRotateX = 0;
    public float mRotateY = 0;
    public float trimPathStart = 0;
    public float trimPathEnd = 1;
    public float trimPathOffset = 0;
    int mStrokeLineCap = -1;
    int mStrokeLineJoin = -1;
    float mStrokeMiterlimit;
    int fill_rule;
    boolean mClip = false;
    float mStrokeOpacity = Float.NaN;
    float mFillOpacity = Float.NaN;
    float mTrimPathStart = 0;
    float mTrimPathEnd = 1;
    float mTrimPathOffset = 0;

    /**
     * @return the name of the path
     */
    public String getName() {
        return mId;
    }

    public void toPath(Path2D path) {
        path.reset();
        if (mNode != null) {
            VDNodeRender.creatPath(mNode, path);
        }
    }

    public static class Node {
        char type;
        float[] params;

        public Node(char type, float[] params) {
            this.type = type;
            this.params = params;
        }

        public Node(Node n) {
            this.type = n.type;
            this.params = Arrays.copyOf(n.params, n.params.length);
        }

        public static String NodeListToString(Node[] nodes) {
            String s = "";
            for (int i = 0; i < nodes.length; i++) {
                Node n = nodes[i];
                s += n.type;
                int len = n.params.length;
                for (int j = 0; j < len; j++) {
                    if (j > 0) {
                        s += ((j & 1) == 1) ? "," : " ";
                    }
                    s += String.valueOf(n.params[j]);
                }
            }
            return s;
        }

        public static void transform(float a,
                float b,
                float c,
                float d,
                float e,
                float f,
                Node[] nodes) {
            float[] pre = new float[2];
            for (int i = 0; i < nodes.length; i++) {
                nodes[i].transform(a, b, c, d, e, f, pre);
            }
        }

        public void transform(float a,
                float b,
                float c,
                float d,
                float e,
                float f,
                float[] pre) {
            int incr = 0;
            float[] tempParams;
            float[] origParams;
            switch (type) {

                case 'z':
                case 'Z':
                    return;
                case 'M':
                case 'L':
                case 'T':
                    incr = 2;
                    pre[0] = params[params.length - 2];
                    pre[1] = params[params.length - 1];
                    for (int i = 0; i < params.length; i += incr) {
                        matrix(a, b, c, d, e, f, i, i + 1);
                    }
                    break;
                case 'm':
                case 'l':
                case 't':
                    incr = 2;
                    pre[0] += params[params.length - 2];
                    pre[1] += params[params.length - 1];
                    for (int i = 0; i < params.length; i += incr) {
                        matrix(a, b, c, d, 0, 0, i, i + 1);
                    }
                    break;
                case 'h':
                    type = 'l';
                    pre[0] += params[params.length - 1];

                    tempParams = new float[params.length * 2];
                    origParams = params;
                    params = tempParams;
                    for (int i = 0; i < params.length; i += 2) {
                        params[i] = origParams[i / 2];
                        params[i + 1] = 0;
                        matrix(a, b, c, d, 0, 0, i, i + 1);
                    }

                    break;
                case 'H':
                    type = 'L';
                    pre[0] = params[params.length - 1];
                    tempParams = new float[params.length * 2];
                    origParams = params;
                    params = tempParams;
                    for (int i = 0; i < params.length; i += 2) {
                        params[i] = origParams[i / 2];
                        params[i + 1] = pre[1];
                        matrix(a, b, c, d, e, f, i, i + 1);
                    }
                    break;
                case 'v':
                    pre[1] += params[params.length - 1];
                    type = 'l';
                    tempParams = new float[params.length * 2];
                    origParams = params;
                    params = tempParams;
                    for (int i = 0; i < params.length; i += 2) {
                        params[i] = 0;
                        params[i + 1] = origParams[i / 2];
                        matrix(a, b, c, d, 0, 0, i, i + 1);
                    }
                    break;
                case 'V':
                    type = 'L';
                    pre[1] = params[params.length - 1];
                    tempParams = new float[params.length * 2];
                    origParams = params;
                    params = tempParams;
                    for (int i = 0; i < params.length; i += 2) {
                        params[i] = pre[0];
                        params[i + 1] = origParams[i / 2];
                        matrix(a, b, c, d, e, f, i, i + 1);
                    }
                    break;
                case 'C':
                case 'S':
                case 'Q':
                    pre[0] = params[params.length - 2];
                    pre[1] = params[params.length - 1];
                    for (int i = 0; i < params.length; i += 2) {
                        matrix(a, b, c, d, e, f, i, i + 1);
                    }
                    break;
                case 's':
                case 'q':
                case 'c':
                    pre[0] += params[params.length - 2];
                    pre[1] += params[params.length - 1];
                    for (int i = 0; i < params.length; i += 2) {
                        matrix(a, b, c, d, 0, 0, i, i + 1);
                    }
                    break;
                case 'a':
                    incr = 7;
                    pre[0] += params[params.length - 2];
                    pre[1] += params[params.length - 1];
                    for (int i = 0; i < params.length; i += incr) {
                        matrix(a, b, c, d, 0, 0, i, i + 1);
                        double ang = Math.toRadians(params[i + 2]);
                        params[i + 2] = (float) Math.toDegrees(ang + Math.atan2(b, d));
                        matrix(a, b, c, d, 0, 0, i + 5, i + 6);
                    }
                    break;
                case 'A':
                    incr = 7;
                    pre[0] = params[params.length - 2];
                    pre[1] = params[params.length - 1];
                    for (int i = 0; i < params.length; i += incr) {
                        matrix(a, b, c, d, e, f, i, i + 1);
                        double ang = Math.toRadians(params[i + 2]);
                        params[i + 2] = (float) Math.toDegrees(ang + Math.atan2(b, d));
                        matrix(a, b, c, d, e, f, i + 5, i + 6);
                    }
                    break;

            }
        }

        void matrix(float a,
                float b,
                float c,
                float d,
                float e,
                float f,
                int offx,
                int offy) {
            float inx = (offx < 0) ? 1 : params[offx];
            float iny = (offy < 0) ? 1 : params[offy];
            float x = inx * a + iny * c + e;
            float y = inx * b + iny * d + f;
            if (offx >= 0) {
                params[offx] = x;
            }
            if (offy >= 0) {
                params[offy] = y;
            }
        }
    }

    public VDPath() {
        mId = this.toString(); // to ensure paths have unique names
    }

    public void copyFrom(VDPath p1) {
        mNode = new VDPath.Node[p1.mNode.length];
        for (int i = 0; i < mNode.length; i++) {
            mNode[i] = new VDPath.Node(p1.mNode[i]);
        }
        mId = p1.mId;
        mStrokeColor = p1.mStrokeColor;
        mFillColor = p1.mFillColor;
        mStrokeWidth = p1.mStrokeWidth;
        mRotate = p1.mRotate;
        mShiftX = p1.mShiftX;
        mShiftY = p1.mShiftY;
        mRotateX = p1.mRotateX;
        mRotateY = p1.mRotateY;
        trimPathStart = p1.trimPathStart;
        trimPathEnd = p1.trimPathEnd;
        trimPathOffset = p1.trimPathOffset;
        mStrokeLineCap = p1.mStrokeLineCap;
        mStrokeLineJoin = p1.mStrokeLineJoin;
        mStrokeMiterlimit = p1.mStrokeMiterlimit;
        fill_rule = p1.fill_rule;

        mClip = p1.mClip;
        mStrokeOpacity = p1.mStrokeOpacity;
        mFillOpacity = p1.mFillOpacity;
        mTrimPathStart = p1.mTrimPathStart;
        mTrimPathEnd = p1.mTrimPathEnd;
        mTrimPathOffset = p1.mTrimPathOffset;
    }

    /**
     * TODO: support rotation attribute for stroke width
     */
    public void transform(float a, float b, float c, float d, float e, float f) {
        mStrokeWidth *= Math.hypot(a + b, c + d);
        Node.transform(a, b, c, d, e, f, mNode);
    }
}
