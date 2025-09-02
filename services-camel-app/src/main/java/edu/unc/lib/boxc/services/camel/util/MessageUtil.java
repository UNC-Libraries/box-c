package edu.unc.lib.boxc.services.camel.util;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;

import edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants;
import org.apache.camel.Message;
import org.fcrepo.camel.FcrepoHeaders;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utilities for working with event messages
 *
 * @author bbpennel
 *
 */
public class MessageUtil {

    private static final Logger log = LoggerFactory.getLogger(MessageUtil.class);

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

        try {
            return switch (body) {
                case Document docBody -> docBody;
                case InputStream streamBody -> createSAXBuilder().build(streamBody);
                case String stringBody -> createSAXBuilder().build(new ByteArrayInputStream((stringBody).getBytes()));
                default -> null;
            };
        } catch (JDOMException e) {
            // Received messages may be json since that is the default format in activemq 6+, so we ignore those here
            log.debug("Message body of type {} is not a valid XML document: {}", body.getClass(), e.getMessage());
            return null;
        }
    }

    /**
     * Returns the fcrepo URI for the given message, either from the FcrepoHeaders.FCREPO_URI header or by
     * constructing it from the FcrepoJmsConstants.BASE_URL and FcrepoJmsConstants.IDENTIFIER headers.
     *
     * @param msg
     * @return
     */
    public static String getFcrepoUri(Message msg) {
        String fcrepoUri = msg.getHeader(FcrepoHeaders.FCREPO_URI, String.class);
        if (fcrepoUri == null) {
            String fcrepoId = msg.getHeader(FcrepoJmsConstants.IDENTIFIER, String.class);
            String fcrepoBaseUrl = msg.getHeader(FcrepoJmsConstants.BASE_URL, String.class);
            if (fcrepoBaseUrl == null || fcrepoId == null) {
                return null;
            }
            return fcrepoBaseUrl + fcrepoId;
        }
        return fcrepoUri;
    }
}
