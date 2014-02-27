package com.android.manifmerger;

import com.android.utils.PositionXmlParser;
import com.google.common.base.Optional;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a loaded xml document.
 */
public class XmlDocument {

    private final Element mRootElement;
    private final AtomicReference<XmlNode> mRootNode = new AtomicReference<XmlNode>(null);
    private final PositionXmlParser mPositionXmlParser;
    private final XmlLoader.SourceLocation mSourceLocation;

    public XmlDocument(PositionXmlParser positionXmlParser,
            XmlLoader.SourceLocation sourceLocation,
            Element element) {
        this.mPositionXmlParser = positionXmlParser;
        this.mSourceLocation = sourceLocation;
        this.mRootElement = element;
    }

    public boolean write(OutputStream outputStream) {

        try {
            Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");         //$NON-NLS-1$
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");                   //$NON-NLS-1$
            tf.setOutputProperty(OutputKeys.INDENT, "yes");                       //$NON-NLS-1$
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",     //$NON-NLS-1$
                    "4");                                            //$NON-NLS-1$
            tf.transform(new DOMSource(getRootNode().getElement()), new StreamResult(outputStream));
            return true;
        } catch (TransformerException e) {
            return false;
        }
    }

    // merge this higher priority document with a higher priority document.
    public Optional<XmlDocument> merge(
            XmlDocument lowerPriorityDocument,
            MergingReport.Builder mergingReportBuilder) {

        mergingReportBuilder.getActionRecorder().recordDefaultNodeAction(getRootNode());

        getRootNode().mergeWithLowerPriorityNode(
                lowerPriorityDocument.getRootNode(), mergingReportBuilder);

        // force re-parsing as new nodes may have appeared.
        return Optional.of(new XmlDocument(mPositionXmlParser, mSourceLocation, mRootElement));
    }

    public boolean compareXml(
            XmlDocument other,
            MergingReport.Builder mergingReport) throws Exception {

        return getRootNode().compareXml(other.getRootNode(), mergingReport);
    }

    public PositionXmlParser getPositionXmlParser() {
        return mPositionXmlParser;
    }

    public XmlLoader.SourceLocation getSourceLocation() {
        return mSourceLocation;
    }

    public synchronized XmlNode getRootNode() {
        if (mRootNode.get() == null) {
            this.mRootNode.set(new XmlNode(mRootElement, this));
        }
        return mRootNode.get();
    }

    public String getPackageName() {
        // TODO(jedo): allow injection...
        return mRootElement.getAttribute("package");
    }
}
