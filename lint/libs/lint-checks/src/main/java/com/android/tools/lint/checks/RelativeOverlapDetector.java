package com.android.tools.lint.checks;

import com.android.SdkConstants;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * Check for potential item overlaps in a RelativeLayout when
 * left- and right-aligned text items are used.
 */
public class RelativeOverlapDetector extends LayoutDetector {
  public static final Issue ISSUE = Issue.create(
      "RelativeOverlap",
      "Overlapping items in RelativeLayout",
      "Looks for potential overlap of left and right-aligned items in RelativeLayout",
      "If relative layout has text or button items aligned to left and right " +
      "sides they can overlap each other due to localized text expansion " +
      "unless they have mutual constraints like toEndOf/toStartOf.", 
      Category.I18N, 3, Severity.WARNING,
      new Implementation(
          RelativeOverlapDetector.class,
          Scope.RESOURCE_FILE_SCOPE));

  static class LayoutNode {
    private static enum Bucket {
      TOP, BOTTOM, SKIP
    }
    private int index;
    private boolean processed;
    private Node node;
    private Bucket bucket;
    private LayoutNode toLeft;
    private LayoutNode toRight;
    private boolean lastLeft;
    private boolean lastRight;

    public LayoutNode(Node node, int index) {
      this.node = node;
      this.index = index;
      processed = false;
      lastLeft = true;
      lastRight = true;
    }

    public String getNodeId() {
      String nodeid = getAttr(SdkConstants.ATTR_ID);
      if (nodeid == null) {
        return String.format("%s-%d", node.getNodeName(), index);
      } else {
        return uniformId(nodeid);
      }
    }

    public String getNodeTextId() {
      String text = getAttr(SdkConstants.ATTR_TEXT);
      if (text == null) {
        return getNodeId();
      } else {
        return uniformId(text);
      }
    }

    public boolean isInvisible() {
      String visibility = getAttr(SdkConstants.ATTR_VISIBILITY);
      return (visibility != null &&
          (visibility.equals("gone") || visibility.equals("invisible")));
    }

    /**
     * Determine if not can grow due to localization or not.
     */
    public boolean fixedWidth() {
      String width = getAttr(SdkConstants.ATTR_LAYOUT_WIDTH);
      if (width!= null && width.equals(SdkConstants.VALUE_WRAP_CONTENT)) {
        // First check child nodes. If at least one of them is not fixed-width,
        // treat whole layout as non-fixed-width
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
          Node child = childNodes.item(i);
          if (child.getNodeType() == Node.ELEMENT_NODE) {
            LayoutNode childLayout = new LayoutNode(child, i);
            if (!childLayout.fixedWidth()) {
              return false;
            }
          }
        }
        // If node contains text attribute, consider if fixed-width if text is
        // hard-coded and non-empty, otherwise it is not fixed-width.
        String text = getAttr(SdkConstants.ATTR_TEXT);
        if (text != null) {
          return text.length() != 0 && text.charAt(0) != '@';
        }

        String nodeName = node.getNodeName();
        if (nodeName.contains("Image") ||
            nodeName.contains("Progress") ||
            nodeName.contains("Radio")) {
          return true;
        } else if (nodeName.contains("Button") || nodeName.contains("Text")) {
          return false;
        }
      }
      return true;
    }

    public Node getNode() {
      return node;
    }

    /**
     * Process a node of a layouyt. Put it into one of three processing units and
     * determine its right and left neighbours.
     */
    public void processNode(Map<String, LayoutNode> nodes) {
      if (processed) {
        return;
      }
      processed = true;

      if (isInvisible()) {
        bucket = Bucket.SKIP;
      } else if(hasAttr(SdkConstants.ATTR_LAYOUT_ALIGN_RIGHT) ||
                hasAttr(SdkConstants.ATTR_LAYOUT_ALIGN_END) ||
                hasAttr(SdkConstants.ATTR_LAYOUT_ALIGN_LEFT) ||
                hasAttr(SdkConstants.ATTR_LAYOUT_ALIGN_START)) {
        bucket = Bucket.SKIP;
      } else if(hasTrueAttr(SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_TOP)) {
        bucket = Bucket.TOP;
      } else if(hasTrueAttr(SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_BOTTOM)) {
        bucket = Bucket.BOTTOM;
      } else {
        if (hasAttr(SdkConstants.ATTR_LAYOUT_ABOVE) ||
            hasAttr(SdkConstants.ATTR_LAYOUT_BELOW)) {
          bucket = Bucket.SKIP;
        } else {
          String[] checkAlignment = {
              SdkConstants.ATTR_LAYOUT_ALIGN_TOP,
              SdkConstants.ATTR_LAYOUT_ALIGN_BOTTOM,
              SdkConstants.ATTR_LAYOUT_ALIGN_BASELINE};
          for (String alignment : checkAlignment) {
            String value = getAttr(alignment);
            if (value != null) {
              LayoutNode otherNode = nodes.get(uniformId(value));
              if (otherNode != null) {
                otherNode.processNode(nodes);
                bucket = otherNode.bucket;
              }
            }
          }
        }
      }
      if (bucket == null) {
        bucket = Bucket.TOP;
      }

      // Check relative placement
      toLeft = findNodeByAttr(nodes, SdkConstants.ATTR_LAYOUT_TO_START_OF);
      if (toLeft == null) {
        toLeft = findNodeByAttr(nodes, SdkConstants.ATTR_LAYOUT_TO_LEFT_OF);
      }
      if (toLeft != null) {
        toLeft.lastLeft = false;
        lastRight = false;
      }
      toRight = findNodeByAttr(nodes, SdkConstants.ATTR_LAYOUT_TO_END_OF);
      if (toRight == null) {
        toRight = findNodeByAttr(nodes, SdkConstants.ATTR_LAYOUT_TO_RIGHT_OF);
      }
      if (toRight != null) {
        toRight.lastRight = false;
        lastRight = false;
      }

      if (hasTrueAttr(SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_RIGHT)) {
        lastRight = false;
      }
      if (hasTrueAttr(SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_LEFT)) {
        lastLeft = false;
      }
      if (toLeft == null && toRight == null && lastRight && lastLeft) {
        lastLeft = false;
      }
    }

    public Set<LayoutNode> canGrowLeft() {
      Set<LayoutNode> nodes;
      if (toRight != null) {
         nodes = toRight.canGrowLeft();
      } else {
        nodes = new LinkedHashSet<LayoutNode>();
      }
      if (!fixedWidth()) {
        nodes.add(this);
      }
      return nodes;
    }

    public Set<LayoutNode> canGrowRight() {
      Set<LayoutNode> nodes;
      if (toLeft != null) {
         nodes = toLeft.canGrowRight();
      } else {
        nodes = new LinkedHashSet<LayoutNode>();
      }
      if (!fixedWidth()) {
        nodes.add(this);
      }
      return nodes;
    }

    /**
     * Determines if not should be skipped from checking.
     */
    public boolean skip() {
      if (bucket == Bucket.SKIP) {
        return true;
      }

      // Skip all includes and Views
      if (node.getNodeName().equals("include") ||
          node.getNodeName().equals("View")) {
        return true;
      }
      return false;
    }

    public boolean sameBucket(LayoutNode node) {
      return bucket == node.bucket;
    }

    private String getAttr(String key) {
      Node attrNode = node.getAttributes().getNamedItem(
          String.format("%s:%s",
              SdkConstants.ANDROID_PKG, key));
      if (attrNode != null) {
        return attrNode.getNodeValue();
      } else {
        return null;
      }
    }

    private LayoutNode findNodeByAttr(Map<String, LayoutNode> nodes, String attrName) {
      String value = getAttr(attrName);
      if (value != null) {
        return nodes.get(uniformId(value));
      } else {
        return null;
      }
    }

    private boolean hasAttr(String key) {
      return getAttr(key) != null;
    }

    private boolean hasTrueAttr(String key) {
      String value = getAttr(key);
      return value != null && value.equals("true");
    }

    private static String uniformId(String value) {
      return value.replaceFirst("@\\+", "@");
    }
  }

  public RelativeOverlapDetector() {
  }

  @Override
  public Collection<String> getApplicableElements() {
      return Arrays.asList(
              SdkConstants.RELATIVE_LAYOUT
      );
  }

  @Override
  public void visitElement(XmlContext context, Element element) {
    // Traverse all child elements
    NodeList childNodes = element.getChildNodes();
    int count = childNodes.getLength();
    HashMap<String, LayoutNode> nodes = Maps.newHashMap();
    for (int i = 0; i < count; i++) {
      Node node = childNodes.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        LayoutNode ln = new LayoutNode(node, i);
        nodes.put(ln.getNodeId(), ln);
      }
    }

    // Node map is populated, recalculate nodes sizes
    for (LayoutNode ln: nodes.values()) {
      ln.processNode(nodes);
    }
    for (LayoutNode right: nodes.values()) {
      if (!right.lastLeft || right.skip()) {
        continue;
      }
      Set<LayoutNode> canGrowLeft = right.canGrowLeft();
      for (LayoutNode left: nodes.values()) {
        if (left == right || !left.lastRight || left.skip() || !left.sameBucket(right)) {
          continue;
        }
        Set<LayoutNode> canGrowRight = left.canGrowRight();
        if (canGrowLeft.size() > 0 || canGrowRight.size() > 0) {
          canGrowRight.addAll(canGrowLeft);
          StringBuffer growNodes = new StringBuffer();
          for (LayoutNode n : canGrowRight) {
            if (growNodes.length() > 0) {
              growNodes.append(", ");
            }
            growNodes.append(n.getNodeTextId());
          }
          LayoutNode nodeToBlame = right;
          LayoutNode otherNode = left;
          if (!canGrowRight.contains(right) && canGrowRight.contains(left)) {
            nodeToBlame = left;
            otherNode = right;
          }
          context.report(ISSUE, nodeToBlame.getNode(),
              context.getLocation(nodeToBlame.getNode()),
              String.format(
                  "%s can overlap %s if %s grow due to localized text expansion",
                  nodeToBlame.getNodeId(),
                  otherNode.getNodeId(),
                  growNodes), null);
        }
      }
    }
  }
}
