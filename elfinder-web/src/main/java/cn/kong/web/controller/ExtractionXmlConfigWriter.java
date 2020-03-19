package cn.kong.web.controller;

import org.w3c.dom.Document;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class ExtractionXmlConfigWriter {
    public static void write(Document document, String path) throws TransformerException {
        TransformerFactory tff = TransformerFactory.newInstance();
        Transformer tf = tff.newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        tf.transform(new DOMSource(document), new StreamResult(path));
    }
}
