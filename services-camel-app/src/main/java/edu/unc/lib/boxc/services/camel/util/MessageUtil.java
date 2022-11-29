package edu.unc.lib.boxc.services.camel.util;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;

import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.Message;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.jdom2.Document;
import org.jdom2.JDOMException;

/**
 * Utilities for working with event messages
 *
 * @author bbpennel
 *
 */
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
                return createSAXBuilder().build((InputStream) body);
            } else if (body instanceof String) {
                return createSAXBuilder().build(
                        new ByteArrayInputStream(((String) body).getBytes()));
            }
        }

        return null;
    }
}
