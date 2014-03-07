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

package com.android.ide.common.res2;

import static com.android.SdkConstants.STRING_PREFIX;
import static com.android.SdkConstants.TAG_ITEM;

import com.android.annotations.NonNull;
import com.android.resources.ResourceType;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * A class to wrap string resource as
 * {message_start_tag}{message_id}{message_separator}{message_text}{message_end_tag},
 * where except the {message_text}, all other components are a sequence of LRM/RLM characters.
 * The purpose is to save message id inside message text while not interfere with message's display.
 *
 * Depending on the message's directionality, {message_start_tag} and {message_end_tag} are
 * choose separately. {message_id} is the hash code of string resource name in
 * binary presentation where LRM represents binary value 0 and RLM represents binary value 1.
 */
class StrResWrapper {
    private static final char[] L_SCHEME_MSG_START_TAG_CHARS = {
            '\u200e', '\u200f', '\u200e', '\u200e', '\u200e'
    };

    private static final char[] L_SCHEME_MSG_END_TAG_CHARS = {
            '\u200e', '\u200f', '\u200e', '\u200e', '\u200f', '\u200e'
    };

    private static final char[] L_SCHEME_MSG_SEPARATOR_CHARS = {'\u200e'};

    private static final char[] R_SCHEME_MSG_START_TAG_CHARS = {
            '\u200f', '\u200e', '\u200f', '\u200f', '\u200f'
    };

    private static final char[] R_SCHEME_MSG_END_TAG_CHARS = {
            '\u200f', '\u200e', '\u200f', '\u200f', '\u200e', '\u200f'
    };

    private static final char[] R_SCHEME_MSG_SEPARATOR_CHARS = {'\u200f'};

    private static final String L_SCHEME_MSG_START_TAG = new String(L_SCHEME_MSG_START_TAG_CHARS);
    private static final String L_SCHEME_MSG_END_TAG = new String(L_SCHEME_MSG_END_TAG_CHARS);;
    private static final String L_SCHEME_MSG_SEPARATOR = new String(L_SCHEME_MSG_SEPARATOR_CHARS);;
    private static final String R_SCHEME_MSG_START_TAG = new String(R_SCHEME_MSG_START_TAG_CHARS);;
    private static final String R_SCHEME_MSG_END_TAG = new String(R_SCHEME_MSG_END_TAG_CHARS);;
    private static final String R_SCHEME_MSG_SEPARATOR = new String(R_SCHEME_MSG_SEPARATOR_CHARS);;

    private static enum Directionality {
        LTR,
        RTL,
    };

    private StrResIdUtil mStringResIdUtil;

    StrResWrapper() {
        mStringResIdUtil = new StrResIdUtil();
    }

    void wrapResNode(@NonNull Node node, @NonNull ResourceItem item, @NonNull Document document) {
        String name = item.getName();
        ResourceType type = item.getType();
        if (type != ResourceType.STRING && type != ResourceType.ARRAY && type != ResourceType.PLURALS) {
            return;
        }

        if (type == ResourceType.STRING) {
            wrapResString(node, name, document);
        } else {
            NodeList itemList = node.getChildNodes();
            for (int j = 0; j < itemList.getLength(); ++j) {
                Node itemNode = itemList.item(j);
                if (itemNode.getNodeName().equals(TAG_ITEM)) {
                    // TODO: handle array index and plural quantity.
                    wrapResString(itemNode, name, document);
                }
            }
        }
    }

    private void wrapResString(@NonNull Node node, @NonNull String name, @NonNull Document document) {
        String text = node.getTextContent();
        if (isWrappable(text)) {
            if (node.hasChildNodes()) {
                Directionality dir = getDirectionality(text);
                Node firstChild = document.createTextNode(getPrefix(name, dir).toString());
                Node lastChild = document.createTextNode(getSuffix(dir));
                node.insertBefore(firstChild, node.getFirstChild());
                node.appendChild(lastChild);
            } else{
                node.setTextContent(wrapText(name, text));
            }
        }
    }

    private StringBuilder getPrefix(@NonNull String name, Directionality dir) {
        StringBuilder buf = new StringBuilder();
        String encoding = mStringResIdUtil.encode(name);
        if (dir == Directionality.LTR) {
            buf.append(L_SCHEME_MSG_START_TAG);
            buf.append(encoding);
            buf.append(L_SCHEME_MSG_SEPARATOR);
        } else {
            buf.append(R_SCHEME_MSG_START_TAG);
            buf.append(encoding);
            buf.append(R_SCHEME_MSG_SEPARATOR);
        }
        return buf;
    }

    private String getSuffix(Directionality dir) {
        if (dir == Directionality.LTR) {
            return L_SCHEME_MSG_END_TAG;
        }
        return R_SCHEME_MSG_END_TAG;
    }

    private String wrapText(@NonNull String name, @NonNull String text) {
        Directionality dir = getDirectionality(text);
        StringBuilder buf = getPrefix(name, dir);
        buf.append(text);
        buf.append(getSuffix(dir));
        return buf.toString();
    }

    // TODO return RTL if text contains strong RTL characters.
    private Directionality getDirectionality(@NonNull String text) {
        return Directionality.LTR;
    }

    private boolean isWrappable(@NonNull String text) {
        return !text.isEmpty() && !text.startsWith(STRING_PREFIX) && !isNumeric(text);
    }

    private boolean isNumeric(@NonNull String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }
}

