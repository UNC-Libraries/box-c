package edu.unc.lib.cdr.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.Message;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

public class MessageUtil {

    private MessageUtil() {
    }

    /**
     * Returns the body of the given message represented as a XML Document
     *
     * @param msg
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    public static Document getDocumentBody(Message msg) throws JDOMException, IOException {
        Object body = msg.getBody();

        if (body != null) {
            if (body instanceof Document) {
                return (Document) body;
            } else if (body instanceof InputStream) {
                SAXBuilder bodyBuilder = new SAXBuilder();
                return bodyBuilder.build((InputStream) body);
            } else if (body instanceof String) {
                SAXBuilder bodyBuilder = new SAXBuilder();
                return bodyBuilder.build(
                        new ByteArrayInputStream(((String) body).getBytes()));
            }
        }

        return null;
    }
}
