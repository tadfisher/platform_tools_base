package com.android.build.gradle.internal;

import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.FD_RES_VALUES;
import static com.android.SdkConstants.STRING_PREFIX;
import static com.android.SdkConstants.TAG_ARRAY;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_PLURALS;
import static com.android.SdkConstants.TAG_STRING;

import com.android.annotations.NonNull;

import org.gradle.api.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


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
public class StrResWrapper {
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

    private static enum DIRECTIONALITY {
        LTR,
        RTL,
    };

    private StrResIdUtil mStringResIdUtil;
    private Logger mLogger;

    private final Map<Node, String> mNode2Name = new HashMap<Node, String>();
    private Document mXmlDoc = null;

    public StrResWrapper(Logger logger) {
        mStringResIdUtil = new StrResIdUtil();
        mLogger = logger;
    }

    public void wrapStringRes(File resDir) throws IOException {
        File resValueDir = new File(resDir, FD_RES_VALUES);
        File[] files = resValueDir.listFiles();
        for (File file : files) {
            wrapStringResInFile(file);
        }
    }


    private boolean wrapStringResInFile(File file) {
        try {
            parse(file);
            wrapString();
            printXmlFile(mXmlDoc, file);
            return true;
        } catch (ParserConfigurationException e) {
            mLogger.error("parse config error", e);
        } catch (SAXException e) {
            mLogger.error("parse error", e);
        } catch (IOException e) {
            mLogger.error("parse IO exception", e);
        } catch (TransformerException e) {
            mLogger.error("could not write xml file", e);
        }
        return false;
    }

    private void parse(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        mXmlDoc = builder.parse(file);
        NodeList androidStrings = mXmlDoc.getElementsByTagName(TAG_STRING);
        for (int i = 0; i < androidStrings.getLength(); ++i) {
            Node stringNode = androidStrings.item(i);
            String name = stringNode.getAttributes().getNamedItem(ATTR_NAME).getNodeValue();
            mNode2Name.put(stringNode, name);
        }

        NodeList stringArrays = mXmlDoc.getElementsByTagName(TAG_ARRAY);
        for (int i = 0; i < stringArrays.getLength(); ++i) {
            Node stringArray = stringArrays.item(i);
            String name = stringArray.getAttributes().getNamedItem(ATTR_NAME).getNodeValue();
            NodeList itemList = stringArray.getChildNodes();
            for (int j = 0, k = 0; j < itemList.getLength(); ++j) {
                Node itemNode = itemList.item(j);
                if (itemNode.getNodeName().equals(TAG_ITEM)) {
                    // TODO: handle array index.
                    mNode2Name.put(itemNode, name);
                }
            }
        }

        NodeList allPlurals = mXmlDoc.getElementsByTagName(TAG_PLURALS);
        for (int i = 0; i < allPlurals.getLength(); ++i) {
            Node onePlurals = allPlurals.item(i);
            String name = onePlurals.getAttributes().getNamedItem(ATTR_NAME).getNodeValue();
            NodeList itemList = onePlurals.getChildNodes();
            for (int j = 0, k = 0; j < itemList.getLength(); ++j) {
                Node itemNode = itemList.item(j);
                if (itemNode.getNodeName().equals(TAG_ITEM)) {
                    // TODO: handle plural quantity.
                    mNode2Name.put(itemNode, name);
                }
            }
        }
    }

    private void wrapString() {
        for (Node node : mNode2Name.keySet()) {
            String name = mNode2Name.get(node);
            String text = node.getTextContent();
            if (isWrappable(text)) {
                if (node.hasChildNodes()) {
                    DIRECTIONALITY dir = getDirectionality(text);
                    Node firstChild = mXmlDoc.createTextNode(getPrefix(name, dir).toString());
                    Node lastChild = mXmlDoc.createTextNode(getSuffix(dir));
                    node.insertBefore(firstChild, node.getFirstChild());
                    node.appendChild(lastChild);
                } else{
                    node.setTextContent(wrapText(name, text));
                }
            }
        }
    }

    private StringBuilder getPrefix(String name, DIRECTIONALITY dir) {
        StringBuilder buf = new StringBuilder();
        String encoding = mStringResIdUtil.encode(name);
        if (dir == DIRECTIONALITY.LTR) {
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

    private String getSuffix(DIRECTIONALITY dir) {
        if (dir == DIRECTIONALITY.LTR) {
            return L_SCHEME_MSG_END_TAG;
        }
        return R_SCHEME_MSG_END_TAG;
    }

    private String wrapText(String name, String text) {
        DIRECTIONALITY dir = getDirectionality(text);
        StringBuilder buf = getPrefix(name, dir);
        buf.append(text);
        buf.append(getSuffix(dir));
        return buf.toString();
    }

    // TODO return RTL if text contains strong RTL characters.
    private DIRECTIONALITY getDirectionality(String text) {
        return DIRECTIONALITY.LTR;
    }

    private boolean isWrappable(String text) {
        return !text.isEmpty() && !text.startsWith(STRING_PREFIX) && !isNumeric(text);
    }

    private boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    /**
     * Outputs the given XML {@link org.w3c.dom.Document} to the file {@code outFile}.
     *
     * @param doc The document to output. Must not be null.
     * @param outFile The {@link java.io.File} where to write the document.
     * @return True if the file was written, false in case of error.
     */
    private void printXmlFile(@NonNull Document doc, @NonNull File outFile)
            throws TransformerException {
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");          //$NON-NLS-1$
        tf.setOutputProperty(OutputKeys.METHOD, "xml");                       //$NON-NLS-1$
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");                   //$NON-NLS-1$
        tf.setOutputProperty(OutputKeys.INDENT, "yes");                       //$NON-NLS-1$
        tf.transform(new DOMSource(doc), new StreamResult(outFile));
    }
}
