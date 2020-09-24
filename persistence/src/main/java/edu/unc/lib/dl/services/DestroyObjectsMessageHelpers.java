/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.services;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import java.net.URI;
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
     * Sends a remove object message from the repository message
     *
     * @param  userid user making request
     * @param contentUri uri of object removed
     * @param metadata metadata for object removed
     * @return id of operation message
     */
    public static Document makeDestroyOperationBody(String userid, URI contentUri, Map<String, String> metadata) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);

        entry.addContent(new Element("author", ATOM_NS)
                .addContent(new Element("name", ATOM_NS).setText(userid)));

        Element objToDestroyEl = new Element("objToDestroy", CDR_MESSAGE_NS);
        entry.addContent(objToDestroyEl);

        Element contentUriValue= new Element("contentUri").setText(contentUri.toString());
        objToDestroyEl.addContent(contentUriValue);

        Element metadataValues = new Element(("metadata"));
        metadataValues.addContent(new Element("objType").setText(metadata.get("objType")));
        metadataValues.addContent(new Element("mimetype").setText(metadata.get("mimeType")));
        metadataValues.addContent(new Element("pidId").setText(metadata.get("pid")));
        objToDestroyEl.addContent(metadataValues);

        msg.addContent(entry);

        return msg;
    }
}
