/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.manifmerger;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.PositionXmlParser;
import com.google.common.base.Preconditions;

import org.w3c.dom.Element;

/**
* Created by jedo on 4/8/14.
*/
final class PositionImpl implements PositionXmlParser.Position {

    static final PositionXmlParser.Position UNKNOWN_POSITION =
            new PositionImpl(0, 0, 0);

    private final int mLine;
    private final int mColumn;
    private final int mOffset;

    PositionImpl(Element xml) {
        Preconditions.checkArgument(xml.getNodeName().equals("position"));
        mLine = Integer.parseInt(xml.getAttribute("line"));
        mColumn = Integer.parseInt(xml.getAttribute("col"));
        mOffset = Integer.parseInt(xml.getAttribute("offset"));
    }

    private PositionImpl(int line, int column, int offset) {
        mLine = line;
        mColumn = column;
        mOffset = offset;
    }

    @Nullable
    @Override
    public PositionXmlParser.Position getEnd() {
        return null;
    }

    @Override
    public void setEnd(@NonNull PositionXmlParser.Position end) {

    }

    @Override
    public int getLine() {
        return mLine;
    }

    @Override
    public int getOffset() {
        return mOffset;
    }

    @Override
    public int getColumn() {
        return mColumn;
    }
}
