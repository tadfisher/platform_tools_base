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

package com.android.assetstudiolib.vectordrawable;

import com.android.assetstudiolib.vectordrawable.vg.SVGGroupNode;
import com.android.assetstudiolib.vectordrawable.vg.SVGLeaveNode;
import com.android.assetstudiolib.vectordrawable.vg.SVGTree;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Converts SVG to VectorDrawable's XML
 */
public class Svg2Vector {
    private static Logger logger = Logger.getLogger(Svg2Vector.class.getSimpleName());

    private static final String SVG_D = "d";
    private static final String SVG_STROKE_COLOR = "stroke";
    private static final String SVG_STROKE_OPACITY = "stroke-opacity";
    private static final String SVG_STROKE_LINEJOINE = "stroke-linejoin";
    private static final String SVG_STROKE_LINECAP = "stroke-linecap";
    private static final String SVG_STROKE_WIDTH = "stroke-width";
    private static final String SVG_FILL_COLOR = "fill";
    private static final String SVG_FILL_OPACITY = "fill-opacity";
    private static final String SVG_OPACITY = "opacity";
    private static final String SVG_CLIP = "clip";
    private static final String SVG_POINTS = "points";

    private static final HashMap<String, String> presentationMap = new HashMap<String, String>();

    static {
        presentationMap.put(SVG_STROKE_COLOR, "android:strokeColor");
        presentationMap.put(SVG_STROKE_OPACITY, "android:strokeAlpha");
        presentationMap.put(SVG_STROKE_LINEJOINE, "android:strokeLinejoin");
        presentationMap.put(SVG_STROKE_LINECAP, "android:strokeLinecap");
        presentationMap.put(SVG_STROKE_WIDTH, "android:strokeWidth");
        presentationMap.put(SVG_FILL_COLOR, "android:fillColor");
        presentationMap.put(SVG_FILL_OPACITY, "android:fillAlpha");
        presentationMap.put(SVG_CLIP, "android:clip");
        presentationMap.put(SVG_OPACITY, "android:fillAlpha");
    }

    private static void fillPresentationAttributes(SVGLeaveNode path, String name, String value) {
        logger.log(Level.FINE, ">>>> PROP " + name + " = " + value);
        path.prop.put(name, value);
    }

    private static SVGTree parse(File f) throws Exception {
        Document doc = parseDocument(f);
        SVGTree svgTree = new SVGTree(f.getName());
        NodeList nSVGNode;

        // Parse svg elements
        nSVGNode = doc.getElementsByTagName("svg");
        if (nSVGNode.getLength() != 1) {
            throw new IllegalStateException("Not a proper SVG file");
        }
        for (int temp = 0; temp < nSVGNode.getLength(); temp++) {
            Node nNode = nSVGNode.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                parseDimension(svgTree, nNode);
            }
        }

        if ((svgTree.w == 0 || svgTree.h == 0) && svgTree.viewBox[2] > 0 && svgTree.viewBox[3] > 0) {
            svgTree.w = svgTree.viewBox[2];
            svgTree.h = svgTree.viewBox[3];
        }

        // Parse transformation information.
        // TODO: Properly handle transformation in the group level. In the "use" case, we treat
        // it as global for now.
        NodeList nUseTags;
        svgTree.matrix = new float[6];
        svgTree.matrix[0] = 1;
        svgTree.matrix[3] = 1;

        nUseTags = doc.getElementsByTagName("use");
        for (int temp = 0; temp < nUseTags.getLength(); temp++) {
            Node nNode = nUseTags.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                parseTransformation(svgTree, nNode);
            }
        }

        SVGGroupNode root = new SVGGroupNode("root");
        svgTree.setRoot(root);

        // Parse all the group and path node recursively.
        traverseSVGAndExtract(svgTree, root, nSVGNode.item(0));

        svgTree.print(root);

        return svgTree;
    }

    private static void traverseSVGAndExtract(SVGTree svgTree, SVGGroupNode currentGroup, Node item) {
        // Recursively traverse all the group and path nodes
        NodeList allChildren = item.getChildNodes();

        for (int i = 0; i < allChildren.getLength(); i++) {
            Node currentNode = allChildren.item(i);
            String nodeName = currentNode.getNodeName();
            if ("path".equals(nodeName) || "rect".equals(nodeName) || "circle".equals(nodeName)
                    || "polygon".equals(nodeName) ||"line".equals(nodeName) ) {
                SVGLeaveNode child = new SVGLeaveNode(nodeName + i);

                extractAllItemsAs(svgTree, child, currentNode);

                currentGroup.addChildren(child);
            } else if ("g".equals(nodeName)) {
                SVGGroupNode childGroup = new SVGGroupNode("child" + i);
                currentGroup.addChildren(childGroup);
                traverseSVGAndExtract(svgTree, childGroup, currentNode);
            } else {
                // For other fancy tags, like <refs>, they can contain children too.
                traverseSVGAndExtract(svgTree, currentGroup, currentNode);
            }
        }

    }

    private static Document parseDocument(File f) throws ParserConfigurationException, SAXException,
            IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        // Skip the XML grammar validation for much faster parsing.
        dbFactory.setNamespaceAware(false);
        dbFactory.setValidating(false);
        dbFactory.setFeature("http://xml.org/sax/features/namespaces", false);
        dbFactory.setFeature("http://xml.org/sax/features/validation", false);
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                false);
        dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false);

        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(f);
        return doc;
    }

    private static void parseTransformation(SVGTree avg, Node nNode) {
        NamedNodeMap a = nNode.getAttributes();
        int len = a.getLength();

        for (int i = 0; i < len; i++) {
            Node n = a.item(i);
            String name = n.getNodeName();
            String value = n.getNodeValue();
            if ("transform".equals(name)) {
                if (value.startsWith("matrix(")) {
                    value = value.substring("matrix(".length(), value.length() - 1);
                    String[] sp = value.split(" ");
                    for (int j = 0; j < sp.length; j++) {
                        avg.matrix[j] = Float.parseFloat(sp[j]);
                    }
                }
            } else if (name.equals("y")) {
                Float.parseFloat(value);
            } else if (name.equals("x")) {
                Float.parseFloat(value);
            }

        }
    }

    private static void parseDimension(SVGTree avg, Node nNode) {
        NamedNodeMap a = nNode.getAttributes();
        int len = a.getLength();

        for (int i = 0; i < len; i++) {
            Node n = a.item(i);
            String name = n.getNodeName();
            String value = n.getNodeValue();
            int subStringSize = value.length();
            if (subStringSize > 2) {
                if (value.endsWith("px")) {
                    subStringSize = subStringSize - 2;
                }
            }

            if ("width".equals(name)) {
                avg.w = Float.parseFloat(value.substring(0, subStringSize));
            } else if ("height".equals(name)) {
                avg.h = Float.parseFloat(value.substring(0, subStringSize));
            } else if ("viewBox".equals(name)) {
                avg.viewBox = new float[4];
                String[] strbox = value.split(" ");
                for (int j = 0; j < avg.viewBox.length; j++) {
                    avg.viewBox[j] = Float.parseFloat(strbox[j]);
                }
            }

        }
    }

    // Read the content from currentItem, and fill into "child"
    private static void extractAllItemsAs(SVGTree avg, SVGLeaveNode child, Node currentItem) {
        Node currentGroup = currentItem.getParentNode();

        boolean hasNodeAttr = false;
        String styleContent = "";
        boolean nothingToDisplay = false;

        while (currentGroup != null && currentGroup.getNodeName().equals("g")) {
            // Parse the group's attributes.
            logger.log(Level.FINE, "Printing current parent");
            printlnCommon(currentGroup);

            NamedNodeMap attr = currentGroup.getAttributes();
            Node nodeAttr = attr.getNamedItem("style");
            // Search for the "display:none", if existed, then skip this item.
            if (nodeAttr != null) {
                styleContent += nodeAttr.getTextContent() + ";";
                logger.log(Level.FINE,
                        "styleContent is :" + styleContent + "at number group ");
                if (styleContent.contains("display:none")) {
                    logger.log(Level.FINE, "Found none style, skip the whole group");
                    nothingToDisplay = true;
                    break;
                } else {
                    hasNodeAttr = true;
                }
            }

            Node displayAttr = attr.getNamedItem("display");
            if (displayAttr != null && "none".equals(displayAttr.getNodeValue())) {
                logger.log(Level.FINE, "Found display:none style, skip the whole group");
                nothingToDisplay = true;
                break;
            }
            currentGroup = currentGroup.getParentNode();
        }

        if (nothingToDisplay) {
            // Skip this current whole item.
            return;
        }

        logger.log(Level.FINE, "Print current item");
        printlnCommon(currentItem);

        if (hasNodeAttr && styleContent != null) {
            addStyleToPath(child, styleContent);
        }

        Node currentGroupNode = currentItem;

        if ("path".equals(currentGroupNode.getNodeName())) {
            extractPathForAVG(avg, child, currentGroupNode);
        }

        if ("rect".equals(currentGroupNode.getNodeName())) {
            extractRectForAVG(avg, child, currentGroupNode);
        }

        if ("circle".equals(currentGroupNode.getNodeName())) {
            extractCircleForAVG(avg, child, currentGroupNode);
        }

        if ("polygon".equals(currentGroupNode.getNodeName())) {
            extractPolyForAVG(avg, child, currentGroupNode);
        }

        if ("line".equals(currentGroupNode.getNodeName())) {
            extractLineForAVG(avg, child, currentGroupNode);
        }
    }

    private static void printlnCommon(Node n) {
        logger.log(Level.FINE, " nodeName=\"" + n.getNodeName() + "\"");

        String val = n.getNamespaceURI();
        if (val != null) {
            logger.log(Level.FINE, " uri=\"" + val + "\"");
        }

        val = n.getPrefix();

        if (val != null) {
            logger.log(Level.FINE, " pre=\"" + val + "\"");
        }

        val = n.getLocalName();
        if (val != null) {
            logger.log(Level.FINE, " local=\"" + val + "\"");
        }

        val = n.getNodeValue();
        if (val != null) {
            logger.log(Level.FINE, " nodeValue=");
            if (val.trim().equals("")) {
                // Whitespace
                logger.log(Level.FINE, "[WS]");
            }
            else {
                logger.log(Level.FINE, "\"" + n.getNodeValue() + "\"");
            }
        }
    }

    /**
     * Convert polygon element into a path.
     */
    private static void extractPolyForAVG(SVGTree avg, SVGLeaveNode child, Node currentGroupNode) {
        logger.log(Level.FINE, "Rect found" + currentGroupNode.getTextContent());
        if (currentGroupNode.getNodeType() == Node.ELEMENT_NODE) {

            NamedNodeMap a = currentGroupNode.getAttributes();
            int len = a.getLength();

            for (int itemIndex = 0; itemIndex < len; itemIndex++) {
                Node n = a.item(itemIndex);
                String name = n.getNodeName();
                String value = n.getNodeValue();
                if (name.equals("style")) {
                    addStyleToPath(child, value);
                } else if (presentationMap.containsKey(name)) {
                    fillPresentationAttributes(child, name, value);
                } else if (name.equals(SVG_POINTS)) {
                    String[] split = value.split("[\\s,]+");
                    String pathdes = "M";
                    float baseX = Float.parseFloat(split[0]);
                    float baseY = Float.parseFloat(split[1]);
                    pathdes += baseX + "," + baseY + "l";

                    for (int j = 2; j < split.length; j += 2) {
                        float x = Float.parseFloat(split[j]);
                        float y = Float.parseFloat(split[j + 1]);
                        if (j != 2) {
                            pathdes += " ";
                        }
                        pathdes += String.format("%f", x - baseX) + ","
                                + String.format("%f", y - baseY);
                        baseX = x;
                        baseY = y;
                    }
                    pathdes += "z";
                    child.setPathData(pathdes);
                }

            }
        }
    }

    /**
     * Convert rectangle element into a path.
     */
    private static void extractRectForAVG(SVGTree avg, SVGLeaveNode child, Node currentGroupNode) {
        logger.log(Level.FINE, "Rect found" + currentGroupNode.getTextContent());

        if (currentGroupNode.getNodeType() == Node.ELEMENT_NODE) {
            float x = 0;
            float y = 0;
            float width = Float.NaN;
            float height = Float.NaN;

            NamedNodeMap a = currentGroupNode.getAttributes();
            int len = a.getLength();
            boolean pureTransparent = false;
            for (int j = 0; j < len; j++) {
                Node n = a.item(j);
                String name = n.getNodeName();
                String value = n.getNodeValue();
                if (name.equals("style")) {
                    addStyleToPath(child, value);
                    if (value.contains("opacity:0;")) {
                        pureTransparent = true;
                    }
                } else if (presentationMap.containsKey(name)) {
                    fillPresentationAttributes(child, name, value);
                } else if (name.equals("clip-path") && value.startsWith("url(#SVGID_")) {

                } else if (name.equals("x")) {
                    x = Float.parseFloat(value);
                } else if (name.equals("y")) {
                    y = Float.parseFloat(value);
                } else if (name.equals("width")) {
                    width = Float.parseFloat(value);
                } else if (name.equals("height")) {
                    height = Float.parseFloat(value);
                } else if (name.equals("style")) {

                }

            }

            if (!pureTransparent && avg != null && !Float.isNaN(x) && !Float.isNaN(y)
                    && !Float.isNaN(width)
                    && !Float.isNaN(height)) {

                String pathData = "M" + x + "," + y + " h" +
                        width + " v" + height + " h" + (-width) + "z";
                child.setPathData(pathData);


                child.rectW = width;
                child.rectH = height;
            }

        }
    }

    /**
     * Convert circle element into a path.
     */
    private static void extractCircleForAVG(SVGTree avg, SVGLeaveNode child, Node currentGroupNode) {
        logger.log(Level.FINE, "circle found" + currentGroupNode.getTextContent());

        if (currentGroupNode.getNodeType() == Node.ELEMENT_NODE) {
            float cx = 0;
            float cy = 0;
            float radius = 0;

            NamedNodeMap a = currentGroupNode.getAttributes();
            int len = a.getLength();
            boolean pureTransparent = false;
            for (int j = 0; j < len; j++) {
                Node n = a.item(j);
                String name = n.getNodeName();
                String value = n.getNodeValue();
                if (name.equals("style")) {
                    addStyleToPath(child, value);
                    if (value.contains("opacity:0;")) {
                        pureTransparent = true;
                    }
                } else if (presentationMap.containsKey(name)) {
                    fillPresentationAttributes(child, name, value);
                } else if (name.equals("clip-path") && value.startsWith("url(#SVGID_")) {

                } else if (name.equals("cx")) {
                    cx = Float.parseFloat(value);
                } else if (name.equals("cy")) {
                    cy = Float.parseFloat(value);
                } else if (name.equals("r")) {
                    radius = Float.parseFloat(value);
                }

            }

            /*
             * M cx cy m -r, 0 a r,r 0 1,1 (r * 2),0 a r,r 0 1,1 -(r * 2),0
             */
            if (!pureTransparent && avg != null && !Float.isNaN(cx) && !Float.isNaN(cy)) {

                String pathData = "M" + cx + "," + cy + " m" + "-" + radius + ", 0 a " +
                        radius + "," + radius + " 0 1,1 " + 2 * radius + ",0 a" +
                        radius + "," + radius + " 0 1,1 " + (-2 * radius) + ",0";

                child.setPathData(pathData);
            }

        }
    }

    /**
     * Convert line element into a path.
     */
    private static void extractLineForAVG(SVGTree avg, SVGLeaveNode child, Node currentGroupNode) {
        logger.log(Level.FINE, "line found" + currentGroupNode.getTextContent());

        if (currentGroupNode.getNodeType() == Node.ELEMENT_NODE) {
            float x1 = 0;
            float y1 = 0;
            float x2 = 0;
            float y2 = 0;

            NamedNodeMap a = currentGroupNode.getAttributes();
            int len = a.getLength();
            boolean pureTransparent = false;
            for (int j = 0; j < len; j++) {
                Node n = a.item(j);
                String name = n.getNodeName();
                String value = n.getNodeValue();
                if (name.equals("style")) {
                    addStyleToPath(child, value);
                    if (value.contains("opacity:0;")) {
                        pureTransparent = true;
                    }
                } else if (presentationMap.containsKey(name)) {
                    fillPresentationAttributes(child, name, value);
                } else if (name.equals("clip-path") && value.startsWith("url(#SVGID_")) {
                    // TODO: Handle clip path here.
                } else if (name.equals("x1")) {
                    x1 = Float.parseFloat(value);
                } else if (name.equals("y1")) {
                    y1 = Float.parseFloat(value);
                } else if (name.equals("x2")) {
                    x2 = Float.parseFloat(value);
                } else if (name.equals("y2")) {
                    y2 = Float.parseFloat(value);
                }
            }

            if (!pureTransparent && avg != null && !Float.isNaN(x1) && !Float.isNaN(y1)
                    && !Float.isNaN(x2) && !Float.isNaN(y2)) {
                String pathData = "M" + x1 + "," + y1 + " L " + x2 + ", " + y2;
                child.setPathData(pathData);
            }
        }

    }

    private static void extractPathForAVG(SVGTree avg, SVGLeaveNode child, Node currentGroupNode) {
        logger.log(Level.FINE, "Path found " + currentGroupNode.getTextContent());

        if (currentGroupNode.getNodeType() == Node.ELEMENT_NODE) {
            Element eElement = (Element) currentGroupNode;

            NamedNodeMap a = currentGroupNode.getAttributes();
            int len = a.getLength();

            for (int j = 0; j < len; j++) {
                Node n = a.item(j);
                String name = n.getNodeName();
                String value = n.getNodeValue();
                if (name.equals("style")) {
                    addStyleToPath(child, value);
                } else if (presentationMap.containsKey(name)) {
                    fillPresentationAttributes(child, name, value);
                } else if (name.equals(SVG_D)) {
                    String pathData = value.replaceAll("(\\d)-", "$1,-");
                    child.setPathData(pathData);
                }

            }
        }
    }

    private static void addStyleToPath(SVGLeaveNode path, String value) {
        logger.log(Level.FINE, "Style found is " + value);
        if (value != null) {
            String[] parts = value.split(";");
            for (int k = parts.length - 1; k >= 0; k--) {
                String subStyle = parts[k];
                String[] nameValue = subStyle.split(":");
                if (nameValue.length == 2 && nameValue[0] != null && nameValue[1] != null) {
                    if (presentationMap.containsKey(nameValue[0])) {
                        fillPresentationAttributes(path, nameValue[0], nameValue[1]);
                    } else if (nameValue[0].equals(SVG_OPACITY)) {
                        // TODO: This is hacky, since we don't have a group level
                        // android:opacity. This only works when the path didn't overlap.
                        fillPresentationAttributes(path, SVG_FILL_OPACITY, nameValue[1]);
                    }
                }
            }
        }
    }

    private final static String copyright = "<!--\n" +
            "Copyright (C) 2015 The Android Open Source Project\n\n" +
            "   Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
            "    you may not use this file except in compliance with the License.\n" +
            "    You may obtain a copy of the License at\n\n" +

            "         http://www.apache.org/licenses/LICENSE-2.0\n\n" +

            "    Unless required by applicable law or agreed to in writing, software\n" +
            "    distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
            "    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
            "    See the License for the specific language governing permissions and\n" +
            "    limitations under the License.\n" +
            "-->\n";

    private final static String head = "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n";

    private static String getSizeString(float w, float h, float scaleFactor) {
        String size = "        android:width=\"" + (w * scaleFactor) + "dp\"\n"
                + "        android:height=\"" + (h * scaleFactor) + "dp\"\n";
        return size;
    }

    private static void writeFile(OutputStream outStream, SVGTree svgTree) throws IOException {

        OutputStreamWriter fw = new OutputStreamWriter(outStream);
        fw.write(copyright);
        fw.write(head);
        fw.write(getSizeString(svgTree.w, svgTree.h, svgTree.mScaleFactor));

        fw.write("        android:viewportWidth=\"" + svgTree.w + "\"\n");
        fw.write("        android:viewportHeight=\"" + svgTree.h + "\">\n");

        svgTree.normalize();
        // TODO: this has to happen in the tree mode!!!
        writeXML(svgTree, fw);
        fw.write("</vector>\n");

        fw.close();
    }

    private static void writeXML(SVGTree svgTree, OutputStreamWriter fw) throws IOException {
        writeXMLRecursively(svgTree.getRoot(), fw);
    }

    private static void writeXMLRecursively(SVGGroupNode currentNode, OutputStreamWriter fw) throws IOException {
        int size = currentNode.getSize();
        for (int i = 0; i < size; i++) {
            Object child = currentNode.getChildAt(i);
            if (child instanceof SVGLeaveNode) {
                SVGLeaveNode path = (SVGLeaveNode) child;

                String fillColor = path.prop.get(SVG_FILL_COLOR);
                String strokeColor = path.prop.get(SVG_STROKE_COLOR);
                logger.log(Level.FINE, "fill color " + fillColor);
                boolean emptyFill = fillColor != null && ("none".equals(fillColor) || "#0000000".equals(fillColor));
                boolean emptyStroke = strokeColor == null || "none".equals(strokeColor);
                boolean emptyPath = path.d == null;
                boolean nothingToDraw = emptyPath || emptyFill && emptyStroke;
                if (nothingToDraw) {
                    continue;
                }

                fw.write("    <path\n");
                if (!path.prop.containsKey(SVG_FILL_COLOR)) {
                    logger.log(Level.FINE, "ADDING FILL SVG_FILL_COLOR");
                    fw.write("        android:fillColor=\"#FF000000\"\n");
                }
                fw.write("        android:pathData=\"" + path.d + "\"");
                fw.write(path.propString(presentationMap));
            } else if (child instanceof SVGGroupNode){
                writeXMLRecursively((SVGGroupNode) child, fw);
            }
        }

    }

    public static void parseSVGToXML(File inputSVG, OutputStream outStream) throws Exception {
        SVGTree svgTree = parse(inputSVG);
        writeFile(outStream, svgTree);
    }
}
