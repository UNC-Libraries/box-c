package edu.unc.lib.boxc.operations.jms.destroy;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;

/**
 * Helper methods for destroy objects messages
 *
 * @author lfarrell
 */
public class DestroyObjectsMessageHelpers {
    private DestroyObjectsMessageHelpers() {
    }

    /**
     * Sends a remove object from the repository message
     *
     * @param  userid user making request
     * @param contentUri uri of object removed
     * @param metadata metadata for object removed
     * @return id of operation message
     */
    public static Document makeDestroyOperationBody(String userid, List<URI> contentUris,
            Map<String, String> metadata) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);

        entry.addContent(new Element("author", ATOM_NS)
                .addContent(new Element("name", ATOM_NS).setText(userid)));

        Element objToDestroyEl = new Element("objToDestroy", CDR_MESSAGE_NS);
        entry.addContent(objToDestroyEl);

        if (contentUris != null) {
            for (URI contentUri: contentUris) {
                Element contentUriValue = new Element("contentUri", CDR_MESSAGE_NS).setText(contentUri.toString());
                objToDestroyEl.addContent(contentUriValue);
            }
        }
        Element objType = new Element("objType", CDR_MESSAGE_NS).setText(metadata.get("objType"));
        objToDestroyEl.addContent(objType);

        String mimetype = metadata.get("mimeType");
        if (mimetype != null) {
            Element mimetypeValue = new Element("mimeType", CDR_MESSAGE_NS).setText(mimetype);
            objToDestroyEl.addContent(mimetypeValue);
        }

        objToDestroyEl.addContent(new Element("pidId", CDR_MESSAGE_NS).setText(metadata.get("pid")));

        msg.addContent(entry);

        return msg;
    }
}
